/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.transport.http.asyncclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509KeyManager;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.jsse.SSLUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CopyingOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.Headers;
import org.apache.cxf.transport.http.URLConnectionHTTPConduit;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduitFactory.UseAsyncPolicy;
import org.apache.cxf.transport.https.AliasedX509ExtendedKeyManager;
import org.apache.cxf.transport.https.CertificateHostnameVerifier;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.version.Version;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.concurrent.BasicFuture;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.nio.client.DefaultHttpAsyncClient;
import org.apache.http.nio.conn.scheme.AsyncScheme;
import org.apache.http.nio.conn.scheme.AsyncSchemeRegistry;
import org.apache.http.nio.conn.ssl.SSLLayeringStrategy;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.BasicHttpContext;

/**
 * 
 */
public class AsyncHTTPConduit extends URLConnectionHTTPConduit {
    public static final String USE_ASYNC = "use.async.http.conduit";

    final AsyncHTTPConduitFactory factory;
    volatile int lastTlsHash = -1;
    volatile Object sslState; 
    volatile SSLContext sslContext;
    volatile DefaultHttpAsyncClient client;
    
    public AsyncHTTPConduit(Bus b, 
                            EndpointInfo ei, 
                            EndpointReferenceType t,
                            AsyncHTTPConduitFactory factory) throws IOException {
        super(b, ei, t);
        this.factory = factory;
    }

    public synchronized DefaultHttpAsyncClient getHttpAsyncClient() throws IOException {
        if (client == null) {
            client = factory.createClient(this);
        }
        return client;
    }
    public AsyncHTTPConduitFactory getAsyncHTTPConduitFactory() {
        return factory;
    }
    
    protected void setupConnection(Message message, URI uri, HTTPClientPolicy csPolicy) throws IOException {
        if (factory.isShutdown()) {
            message.put(USE_ASYNC, Boolean.FALSE);
            super.setupConnection(message, uri, csPolicy);
            return;
        }
        
        String s = uri.getScheme();
        if (!"http".equals(s) && !"https".equals(s)) {
            throw new MalformedURLException("unknown protocol: " + s);
        }
        
        Object o = message.getContextualProperty(USE_ASYNC);
        if (o == null) {
            o = factory.getUseAsyncPolicy();
        }
        if (o instanceof UseAsyncPolicy) {
            switch ((UseAsyncPolicy)o) {
            case ALWAYS:
                o = true;
                break;
            case NEVER:
                o = false;
                break;
            case ASYNC_ONLY:
            default:
                o = !message.getExchange().isSynchronous();
                break;
            }
            
        } 
        if (!MessageUtils.isTrue(o)) {
            message.put(USE_ASYNC, Boolean.FALSE);
            super.setupConnection(message, uri, csPolicy);
            return;
        }
        if (StringUtils.isEmpty(uri.getPath())) {
            //hc needs to have the path be "/" 
            uri = uri.resolve("/");
        }

        message.put(USE_ASYNC, Boolean.TRUE);
        message.put("http.scheme", uri.getScheme());
        String httpRequestMethod = 
            (String)message.get(Message.HTTP_REQUEST_METHOD);
        if (httpRequestMethod == null) {
            httpRequestMethod = "POST";
            message.put(Message.HTTP_REQUEST_METHOD, httpRequestMethod);
        }
        final CXFHttpRequest e = new CXFHttpRequest(httpRequestMethod);
        BasicHttpEntity entity = new BasicHttpEntity() {
            public boolean isRepeatable() {
                return e.getOutputStream().retransmitable();
            }
        };
        entity.setChunked(true);
        entity.setContentType((String)message.get(Message.CONTENT_TYPE));
        e.setURI(uri);
        
        e.setEntity(entity);
        
        // Set socket timeout
        e.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 
                Integer.valueOf((int) csPolicy.getReceiveTimeout()));
        
        Proxy p = proxyFactory.createProxy(csPolicy , uri);
        if (p != null) {
            InetSocketAddress isa = (InetSocketAddress)p.address();
            HttpHost proxy = new HttpHost(isa.getHostName(), isa.getPort());
            ConnRouteParams.setDefaultProxy(e.getParams(), proxy);
        }
        message.put(CXFHttpRequest.class, e);
    }
    
    
    protected OutputStream createOutputStream(Message message, 
                                              boolean needToCacheRequest, 
                                              boolean isChunking,
                                              int chunkThreshold) throws IOException {
        if (Boolean.TRUE.equals(message.get(USE_ASYNC))) {
            CXFHttpRequest entity = message.get(CXFHttpRequest.class);
            AsyncWrappedOutputStream out = new AsyncWrappedOutputStream(message,
                                                needToCacheRequest, 
                                                isChunking,
                                                chunkThreshold,
                                                getConduitName(),
                                                entity.getURI());
            entity.setOutputStream(out);
            return out;
        }
        return super.createOutputStream(message, needToCacheRequest, isChunking, chunkThreshold);
    }
    
    
    public class AsyncWrappedOutputStream extends WrappedOutputStream 
        implements CopyingOutputStream, WritableByteChannel {
        final HTTPClientPolicy csPolicy;

        CXFHttpRequest entity;
        BasicHttpEntity basicEntity;

        boolean isAsync;
        SharedInputBuffer inbuf;
        SharedOutputBuffer outbuf;
        
        // Objects for the response
        volatile HttpResponse httpResponse;
        volatile Exception exception;
        volatile SSLSession session;

        private Future<Boolean> connectionFuture;

        private Object sessionLock = new Object();
        
        public AsyncWrappedOutputStream(Message message,
                                        boolean needToCacheRequest, 
                                        boolean isChunking,
                                        int chunkThreshold, 
                                        String conduitName,
                                        URI uri) {
            super(message, needToCacheRequest, isChunking,
                  chunkThreshold, conduitName,
                  uri);
            csPolicy = getClient(message);
            entity = message.get(CXFHttpRequest.class);
            basicEntity = (BasicHttpEntity)entity.getEntity();
            HeapByteBufferAllocator allocator = new HeapByteBufferAllocator();
            int bufSize = csPolicy.getChunkLength() > 0 ? csPolicy.getChunkLength() : 16320;
            inbuf = new SharedInputBuffer(bufSize, allocator);
            outbuf = new SharedOutputBuffer(bufSize, allocator);
        }
        
        public boolean retransmitable() {
            return cachedStream != null;
        }
        public CachedOutputStream getCachedStream() {
            return cachedStream;
        }

        
        protected void setProtocolHeaders() throws IOException {
            Headers h = new Headers(outMessage);
            basicEntity.setContentType(h.determineContentType());
            boolean addHeaders = MessageUtils.isTrue(outMessage.getContextualProperty(Headers.ADD_HEADERS_PROPERTY));
            
            for (Map.Entry<String, List<String>> header : h.headerMap().entrySet()) {
                if (HttpHeaderHelper.CONTENT_TYPE.equalsIgnoreCase(header.getKey())) {
                    continue;
                }
                if (addHeaders || HttpHeaderHelper.COOKIE.equalsIgnoreCase(header.getKey())) {
                    for (String s : header.getValue()) {
                        entity.addHeader(HttpHeaderHelper.COOKIE, s);
                    }
                } else if (!"Content-Length".equalsIgnoreCase(header.getKey())) {
                    StringBuilder b = new StringBuilder();
                    for (int i = 0; i < header.getValue().size(); i++) {
                        b.append(header.getValue().get(i));
                        if (i + 1 < header.getValue().size()) {
                            b.append(',');
                        }
                    }
                    entity.setHeader(header.getKey(), b.toString());
                }        
                if (!entity.containsHeader("User-Agent")) {
                    entity.setHeader("User-Agent", Version.getCompleteVersionString());
                }
            }
        }
        
        protected void setFixedLengthStreamingMode(int i) {
            basicEntity.setChunked(false);
            basicEntity.setContentLength(i);
        }
        public void thresholdReached() throws IOException {
            basicEntity.setChunked(true);
        }

        protected void handleNoOutput() throws IOException {
            connect(false);
            outbuf.writeCompleted();
        }
        
        public boolean isOpen() {
            return true;
        }

        public int write(ByteBuffer src) throws IOException {
            int total = 0;
            if (buffer != null) {
                int pos = buffer.size();
                int len = this.threshold - pos;
                if (len > src.remaining()) {
                    len = src.remaining();
                }
                src.get(buffer.getRawBytes(), pos, len);
                buffer.setSize(buffer.size() + len);
                total += len;
                if (buffer.size() >= threshold) {
                    thresholdReached();
                    unBuffer();
                }
            }
            if (cachingForRetransmission) {
                wrappedStream.write(src.array(), src.position(), src.remaining());
                return src.remaining() + total;
            }
            return outbuf.write(src) + total;
        }

        public int copyFrom(InputStream in) throws IOException {
            int count = 0;
            while (buffer != null) {
                int pos = buffer.size();
                int i = in.read(buffer.getRawBytes(), pos,
                                this.threshold - pos);
                if (i > 0) {
                    buffer.setSize(pos + i);
                    if (buffer.size() >= threshold) {
                        thresholdReached();
                        unBuffer();
                    }
                    count += i;
                } else {
                    return count;
                }
            }
           
            if (cachingForRetransmission) {
                count += IOUtils.copy(in, wrappedStream);
            } else {
                count += outbuf.copy(in);
            }
            return count;
        }
        
        @Override
        public void close() throws IOException {
            super.close();
        }
        protected void setupWrappedStream() throws IOException {
            connect(true);
            wrappedStream = new OutputStream() {
                public void write(byte b[], int off, int len) throws IOException {
                    outbuf.write(b, off, len);
                }
                public void write(int b) throws IOException {
                    outbuf.write(b);
                }
                public void close() throws IOException {
                    outbuf.writeCompleted();
                }
            };
                        
            // If we need to cache for retransmission, store data in a
            // CacheAndWriteOutputStream. Otherwise write directly to the output stream.
            if (cachingForRetransmission) {
                cachedStream = new CacheAndWriteOutputStream(wrappedStream);
                wrappedStream = cachedStream;
            }
        }
        protected void connect(boolean output) throws IOException {
            if (connectionFuture != null) {
                return;
            }
                       
            CXFResponseCallback responseCallback = new CXFResponseCallback() {
                @Override
                public void responseReceived(HttpResponse response) {
                    setHttpResponse(response);
                }
                
            };
            
            FutureCallback<Boolean> callback = new FutureCallback<Boolean>() {

                public void completed(Boolean result) {
                }
                
                public void failed(Exception ex) {
                    setException(ex);
                    inbuf.shutdown();
                    outbuf.shutdown();
                }
                public void cancelled() {
                    inbuf.shutdown();
                    outbuf.shutdown();
                }
                
            };
            
            if (!output) {
                entity.removeHeaders("Transfer-Encoding");
                entity.removeHeaders("Content-Type");
                entity.setEntity(null);
            }
            if (url.getScheme().equals("https") && tlsClientParameters == null) {
                tlsClientParameters = new TLSClientParameters();
            }
            
            BasicHttpContext ctx = new BasicHttpContext();
            if (AsyncHTTPConduit.this.proxyAuthorizationPolicy != null
                && AsyncHTTPConduit.this.proxyAuthorizationPolicy.getUserName() != null) {
                ctx.setAttribute(ClientContext.CREDS_PROVIDER, new CredentialsProvider() {
                    public void setCredentials(AuthScope authscope, Credentials credentials) {
                    }
                    public Credentials getCredentials(AuthScope authscope) {
                        return new UsernamePasswordCredentials(AsyncHTTPConduit.this
                                                               .proxyAuthorizationPolicy.getUserName(),
                                               AsyncHTTPConduit.this.proxyAuthorizationPolicy.getPassword());
                    }
                    public void clear() {
                    }
                });
            }
            if (tlsClientParameters != null && tlsClientParameters.hashCode() == lastTlsHash && sslState != null) {
                ctx.setAttribute(ClientContext.USER_TOKEN , sslState);
            }
            
            final AsyncSchemeRegistry reg = new AsyncSchemeRegistry();
            reg.register(new AsyncScheme("http", 80, null));
            if ("https".equals(url.getScheme())) {
                try {
                    final SSLContext sslcontext = getSSLContext();
                    reg.register(new AsyncScheme("https", 443, new SSLLayeringStrategy(sslcontext) {
                        @Override
                        protected void initializeEngine(SSLEngine engine) {
                            initializeSSLEngine(sslcontext, engine);
                        }
                        @Override
                        protected void verifySession(final IOSession iosession,
                                              final SSLSession sslsession) throws SSLException {
                            super.verifySession(iosession, sslsession);
                            iosession.setAttribute("cxf.handshake.done", Boolean.TRUE);
                            CXFHttpRequest req = (CXFHttpRequest)iosession
                                .removeAttribute(CXFHttpRequest.class.getName());
                            if (req != null) {
                                req.getOutputStream().setSSLSession(sslsession);
                            }
                        }
                    }));
                } catch (GeneralSecurityException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            ctx.setAttribute(ClientContext.SCHEME_REGISTRY, reg);
            connectionFuture = new BasicFuture<Boolean>(callback);
            DefaultHttpAsyncClient c = getHttpAsyncClient();
            CredentialsProvider credProvider = c.getCredentialsProvider();
            Credentials creds = (Credentials)outMessage.getContextualProperty(Credentials.class.getName());
            if (creds != null && credProvider != null) {
                credProvider.setCredentials(AuthScope.ANY, creds);
            }
            if (credProvider != null && credProvider.getCredentials(AuthScope.ANY) != null) {
                ctx.setAttribute(ClientContext.USER_TOKEN,
                                 credProvider.getCredentials(AuthScope.ANY).getUserPrincipal());
            }
            
            c.execute(new CXFHttpAsyncRequestProducer(entity, outbuf),
                      new CXFHttpAsyncResponseConsumer(this, inbuf, responseCallback),
                      ctx,
                      callback);
        }
        
        protected void retrySetHttpResponse(HttpResponse r) {
            if (httpResponse == null && isAsync) {
                setHttpResponse(r);
            }
        }
        protected synchronized void setHttpResponse(HttpResponse r) {
            httpResponse = r;
            if (isAsync) {
                //got a response, need to start the response processing now
                try {
                    handleResponseOnWorkqueue(false, true);
                    isAsync = false; // don't trigger another start on next block. :-)
                } catch (Exception ex) {
                    //ignore, we'll try again on the next consume;
                }
            }
            notifyAll();
        }
        protected synchronized void setException(Exception ex) {
            exception = ex;
            if (isAsync) {
                //got a response, need to start the response processing now
                try {
                    handleResponseOnWorkqueue(false, true);
                    isAsync = false; // don't trigger another start on next block. :-)
                } catch (Exception ex2) {
                    ex2.printStackTrace();
                }
            }
            notifyAll();
        }

        protected synchronized HttpResponse getHttpResponse() throws IOException {
            while (httpResponse == null) {
                if (exception == null) { //already have an exception, skip waiting
                    try {
                        wait(csPolicy.getReceiveTimeout());
                    } catch (InterruptedException e) {
                        throw new IOException(e);
                    }
                }
                if (httpResponse == null) {
                    outbuf.shutdown();
                    inbuf.shutdown();
                    //outbuf = null;
                    //inbuf = null;
                    
                    if (exception != null) {
                        if (exception instanceof IOException) {
                            throw (IOException)exception;
                        }
                        if (exception instanceof RuntimeException) {
                            throw (RuntimeException)exception;
                        }
                        throw new IOException(exception);
                    }
                    
                    throw new SocketTimeoutException("Read Timeout");
                }
            }
            return httpResponse;
        }
        
        protected void handleResponseAsync() throws IOException {
            isAsync = true;
        }
        
        protected void closeInputStream() throws IOException {
            byte bytes[] = new byte[1024];
            while (inbuf.read(bytes) > 0) {
                //nothing
            }
            inbuf.close();
            inbuf.shutdown();
        }
        
        protected synchronized InputStream getInputStream() throws IOException {
            return new InputStream() {
                public int read() throws IOException {
                    return inbuf.read();
                }
                public int read(byte[] b) throws IOException {
                    return inbuf.read(b);
                }
                public int read(byte[] b, int off, int len) throws IOException {
                    return inbuf.read(b, off, len);
                }
                public int available() throws IOException {
                    return inbuf.available();
                }
                public void close() throws IOException {
                    inbuf.close();
                }
            };
        }
        
        protected boolean usingProxy() {
            //FIXME - need to get the Proxy stuff from the connection
            return false;
        }
        
        protected HttpsURLConnectionInfo getHttpsURLConnectionInfo() throws IOException {
            if ("http".equals(outMessage.get("http.scheme"))) {
                return null;
            }
            connect(true);
            synchronized (sessionLock) {
                if (session == null) {
                    try {
                        sessionLock.wait(csPolicy.getConnectionTimeout());
                    } catch (InterruptedException e) {
                        throw new IOException(e);
                    }
                }
                if (session == null) {
                    throw new IOException("No SSLSession detected");
                }
            }
            HostnameVerifier verifier;
            if (tlsClientParameters.isUseHttpsURLConnectionDefaultHostnameVerifier()) {
                verifier = HttpsURLConnection.getDefaultHostnameVerifier();
            } else if (tlsClientParameters.isDisableCNCheck()) {
                verifier = CertificateHostnameVerifier.ALLOW_ALL;
            } else {
                verifier = CertificateHostnameVerifier.DEFAULT;
            }
            if (!verifier.verify(url.getHost(), session)) {
                throw new IOException("Could not verify host " + url.getHost());
            }
            
            String method = (String)outMessage.get(Message.HTTP_REQUEST_METHOD);
            String cipherSuite = null;
            Certificate[] localCerts = null;
            Principal principal = null;
            Certificate[] serverCerts = null;
            Principal peer = null;
            if (session != null) {
                cipherSuite = session.getCipherSuite();
                localCerts = session.getLocalCertificates();
                principal = session.getLocalPrincipal();
                serverCerts = session.getPeerCertificates();
                peer = session.getPeerPrincipal();
            }
            
            return new HttpsURLConnectionInfo(url, method, cipherSuite, localCerts, principal, serverCerts, peer);
        }
        
        protected int getResponseCode() throws IOException {
            return getHttpResponse().getStatusLine().getStatusCode();
        }
        
        protected String getResponseMessage() throws IOException {
            return getHttpResponse().getStatusLine().getReasonPhrase();
        }
        
        private String readHeaders(Headers h) throws IOException {
            Header headers[] = getHttpResponse().getAllHeaders();
            h.headerMap().clear();
            String ct = null;
            for (Header header : headers) {
                List<String> s = h.headerMap().get(header.getName());
                if (s == null) {
                    s = new ArrayList<String>(1);
                    h.headerMap().put(header.getName(), s);
                }
                s.add(header.getValue());
                if ("Content-Type".equalsIgnoreCase(header.getName())) {
                    ct = header.getValue();
                }
            } 
            return ct;
        }
        
        protected void updateResponseHeaders(Message inMessage) throws IOException {
            Headers h = new Headers(inMessage);
            inMessage.put(Message.CONTENT_TYPE, readHeaders(h));
            cookies.readFromHeaders(h);
        }
        
        protected InputStream getPartialResponse() throws IOException {
            InputStream in = null;
            int responseCode = getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_ACCEPTED
                || responseCode == HttpURLConnection.HTTP_OK) {
                
                Header head = httpResponse.getFirstHeader(HttpHeaderHelper.CONTENT_LENGTH);
                int cli = 0;
                if (head != null) {
                    cli = Integer.parseInt(head.getValue());
                }
                head = httpResponse.getFirstHeader(HttpHeaderHelper.TRANSFER_ENCODING);
                boolean isChunked = head != null &&  HttpHeaderHelper.CHUNKED.equalsIgnoreCase(head.getValue());
                head = httpResponse.getFirstHeader(HttpHeaderHelper.CONNECTION);
                boolean isEofTerminated = head != null &&  HttpHeaderHelper.CLOSE.equalsIgnoreCase(head.getValue());
                if (cli > 0) {
                    in = getInputStream();
                } else if (isChunked || isEofTerminated) {
                    // ensure chunked or EOF-terminated response is non-empty
                    try {
                        @SuppressWarnings("resource")
                        PushbackInputStream pin = 
                            new PushbackInputStream(getInputStream());
                        int c = pin.read();
                        if (c != -1) {
                            pin.unread((byte)c);
                            in = pin;
                        }
                    } catch (IOException ioe) {
                        // ignore
                    }    
                }
            }
            return in;
        }
        
        protected void updateCookiesBeforeRetransmit() throws IOException {
            Headers h = new Headers();
            readHeaders(h);
            cookies.readFromHeaders(h);
        }
        
        protected boolean authorizationRetransmit() throws IOException {
            boolean b = super.authorizationRetransmit();
            if (!b) {
                //HTTPClient may be handling the authorization things instead of us, we
                //just need to make sure we set the cookies and proceed and HC 
                //will do the negotiation and such.
                try {
                    closeInputStream();
                } catch (Throwable t) {
                    //ignore
                }
                cookies.writeToMessageHeaders(outMessage);
                retransmit(url.toString());
                return true;
            }
            return b;
        }        
        
        protected void retransmitStream() throws IOException {
            cachingForRetransmission = false; //already cached
            setupWrappedStream();
            cachedStream.writeCacheTo(wrappedStream);
            wrappedStream.flush();
            wrappedStream.close();
        }

        protected void setupNewConnection(String newURL) throws IOException {
            httpResponse = null;
            isAsync = false;
            exception = null;
            connectionFuture = null;
            session = null;
            
            //reset the buffers
            HeapByteBufferAllocator allocator = new HeapByteBufferAllocator();
            int bufSize = csPolicy.getChunkLength() > 0 ? csPolicy.getChunkLength() : 16320;
            inbuf = new SharedInputBuffer(bufSize, allocator);
            outbuf = new SharedOutputBuffer(bufSize, allocator);
            try {
                this.url = new URI(newURL);
                setupConnection(outMessage, this.url, csPolicy);
                entity = outMessage.get(CXFHttpRequest.class);
                basicEntity = (BasicHttpEntity)entity.getEntity();
                entity.setOutputStream(this);
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }
        
    
        public void setSSLSession(SSLSession sslsession) {            
            session = sslsession;
            synchronized (sessionLock) {
                sslState = sslsession.getLocalPrincipal();
                sessionLock.notifyAll();
            }
        }

    }


    public synchronized SSLContext getSSLContext() throws GeneralSecurityException {
        TLSClientParameters tlsClientParameters = getTlsClientParameters();
        if (tlsClientParameters == null) {
            tlsClientParameters = new TLSClientParameters();
        }
        int hash = tlsClientParameters.hashCode();
        if (hash == lastTlsHash) {
            return sslContext;
        }
        
        String provider = tlsClientParameters.getJsseProvider();

        String protocol = tlsClientParameters.getSecureSocketProtocol() != null ? tlsClientParameters
            .getSecureSocketProtocol() : "TLS";

        SSLContext ctx = provider == null ? SSLContext.getInstance(protocol) : SSLContext
            .getInstance(protocol, provider);
        ctx.getClientSessionContext().setSessionTimeout(tlsClientParameters.getSslCacheTimeout());
        KeyManager[] keyManagers = tlsClientParameters.getKeyManagers();
        if (tlsClientParameters.getCertAlias() != null) {
            getKeyManagersWithCertAlias(tlsClientParameters, keyManagers);
        }
        ctx.init(keyManagers, tlsClientParameters.getTrustManagers(),
                 tlsClientParameters.getSecureRandom());

        sslContext = ctx;
        lastTlsHash = hash;
        sslState = null;
        return ctx;
    }

    public void initializeSSLEngine(SSLContext sslcontext, SSLEngine sslengine) {
        TLSClientParameters tlsClientParameters = getTlsClientParameters();
        if (tlsClientParameters == null) {
            tlsClientParameters = new TLSClientParameters();
        }
        String[] cipherSuites = SSLUtils.getCiphersuites(tlsClientParameters.getCipherSuites(),
                                                         SSLUtils.getSupportedCipherSuites(sslcontext), 
                                                         tlsClientParameters.getCipherSuitesFilter(), LOG, false);
        sslengine.setEnabledCipherSuites(cipherSuites);
    }

    protected static KeyManager[] getKeyManagersWithCertAlias(TLSClientParameters tlsClientParameters,
                                                      KeyManager[] keyManagers) throws GeneralSecurityException {
        if (tlsClientParameters.getCertAlias() != null) {
            KeyManager ret[] = new KeyManager[keyManagers.length];  
            for (int idx = 0; idx < keyManagers.length; idx++) {
                if (keyManagers[idx] instanceof X509KeyManager) {
                    try {
                        ret[idx] = new AliasedX509ExtendedKeyManager(tlsClientParameters.getCertAlias(),
                                                                             (X509KeyManager)keyManagers[idx]);
                    } catch (Exception e) {
                        throw new GeneralSecurityException(e);
                    }
                } else {
                    ret[idx] = keyManagers[idx]; 
                }
            }
            return ret;
        }
        return keyManagers;
    }


}
