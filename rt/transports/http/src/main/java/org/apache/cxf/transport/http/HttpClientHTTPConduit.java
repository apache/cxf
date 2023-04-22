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
package org.apache.cxf.transport.http;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PushbackInputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.UnresolvedAddressException;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;
import org.apache.cxf.transport.https.SSLUtils;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;


public class HttpClientHTTPConduit extends URLConnectionHTTPConduit {

    volatile HttpClient client;
    volatile int lastTlsHash = -1;
    volatile URI sslURL;
    
    public HttpClientHTTPConduit(Bus b, EndpointInfo ei) throws IOException {
        super(b, ei);
    }

    public HttpClientHTTPConduit(Bus b, EndpointInfo ei, EndpointReferenceType t) throws IOException {
        super(b, ei, t);
    }
    
    
    private boolean isSslTargetDifferent(URI lastURL, URI url) {
        return !lastURL.getScheme().equals(url.getScheme())
                || !lastURL.getHost().equals(url.getHost())
                || lastURL.getPort() != url.getPort();
    }
    
    @Override
    protected void setupConnection(Message message, Address address, HTTPClientPolicy csPolicy) throws IOException {
        URI uri = address.getURI();
        message.put("http.scheme", uri.getScheme());
        // check tlsClientParameters from message header
        TLSClientParameters clientParameters = message.get(TLSClientParameters.class);
        if (clientParameters == null) {
            clientParameters = tlsClientParameters;
        }
        if (clientParameters == null) {
            clientParameters = new TLSClientParameters();
        }
        Object o = message.getContextualProperty("force.urlconnection.http.conduit");
        //o = true;
        if ("https".equals(uri.getScheme()) && clientParameters != null) {
            if (clientParameters.getSSLSocketFactory() != null) {
                //if they configured in an SSLSocketFactory, we cannot do anything
                //with it as the NIO based transport cannot use socket created from
                //the SSLSocketFactory.
                o = Boolean.TRUE;
            }
            if (clientParameters.getSslContext() != null 
                && clientParameters.isDisableCNCheck()) {
                // If they specify their own SSLContext, we cannot handle the
                // HostnameVerifier so we'll need to use the URLConnection
                o = Boolean.TRUE;
            }
            
        }
        if (Boolean.TRUE.equals(o)) {
            message.put("USING_URLCONNECTION", Boolean.TRUE);
            super.setupConnection(message, address, csPolicy);
            return;
        }
        
        if (sslURL != null && isSslTargetDifferent(sslURL, uri)) {
            sslURL = null;
            client = null;
        }
        // If the HTTP_REQUEST_METHOD is not set, the default is "POST".
        String httpRequestMethod =
            (String)message.get(Message.HTTP_REQUEST_METHOD);
        if (httpRequestMethod == null) {
            httpRequestMethod = "POST";
            message.put(Message.HTTP_REQUEST_METHOD, "POST");
        }

        HttpClient cl = client;
        if (cl == null) {
            int ctimeout = determineConnectionTimeout(message, csPolicy);        
            ProxySelector ps = new ProxySelector() {
                public List<Proxy> select(URI uri) {
                    Proxy proxy = proxyFactory.createProxy(csPolicy, uri);
                    if (proxy !=  null) {
                        return Arrays.asList(proxy);
                    }
                    return ProxySelector.getDefault().select(uri);
                }
                public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                }
            };
            
            HttpClient.Builder cb = HttpClient.newBuilder()
                .proxy(ps)
                .followRedirects(Redirect.NEVER);
            
            if (ctimeout > 0) {
                cb.connectTimeout(Duration.ofMillis(ctimeout));
            }
            
            if ("https".equals(uri.getScheme())) {
                sslURL = uri;
                try {
                    SSLContext sslContext = clientParameters.getSslContext();
                    if (sslContext == null) {
                        sslContext = SSLUtils.getSSLContext(clientParameters, true);                    
                        cb.sslContext(sslContext);
                    }
                    if (sslContext != null) {
                        String[] supportedCiphers =  org.apache.cxf.configuration.jsse.SSLUtils
                                .getSupportedCipherSuites(sslContext);
                        String[] cipherSuites = org.apache.cxf.configuration.jsse.SSLUtils
                                .getCiphersuitesToInclude(clientParameters.getCipherSuites(),
                                                          clientParameters.getCipherSuitesFilter(),
                                                          sslContext.getSocketFactory().getDefaultCipherSuites(),
                                                          supportedCiphers,
                                                          LOG);
                        
                        if (clientParameters.getSecureSocketProtocol() != null) {
                            String protocol = clientParameters.getSecureSocketProtocol();
                            SSLParameters params = new SSLParameters(cipherSuites, new String[] {protocol});
                            cb.sslParameters(params);
                        } else {
                            SSLParameters params = new SSLParameters(cipherSuites, 
                                                                     new String[] {"TLSv1", "TLSv1.1", "TLSv1.2"});
                            cb.sslParameters(params);
                        }
                    }
                } catch (GeneralSecurityException e) {
                    throw new IOException(e);
                }
            }
            String verc = (String)message.getContextualProperty(FORCE_HTTP_VERSION);
            if (verc == null) {
                verc = csPolicy.getVersion();
            }
            if ("1.1".equals(HTTP_VERSION) || "1.1".equals(verc)) {
                cb.version(Version.HTTP_1_1);  
            }

            cl = cb.build();
            if (!"https".equals(uri.getScheme()) 
                && !KNOWN_HTTP_VERBS_WITH_NO_CONTENT.contains(httpRequestMethod)
                && cl.version() == Version.HTTP_2
                && ("2".equals(verc) || ("auto".equals(verc) && "2".equals(HTTP_VERSION)))) {
                try {
                    // We specifically want HTTP2, but we're using a request
                    // that won't trigger an upgrade to HTTP/2 so we'll
                    // call OPTIONS on the URI which may trigger HTTP/2 upgrade.
                    // Not needed for methods that don't have a body (GET/HEAD/etc...) 
                    // or for https (negotiated at the TLS level)
                    HttpRequest.Builder rb = HttpRequest.newBuilder()
                        .uri(uri)
                        .method("OPTIONS", BodyPublishers.noBody());
                    cl.send(rb.build(), BodyHandlers.ofByteArray());
                } catch (IOException | InterruptedException e) {
                    //
                }
            } 
            client = cl;
        }        
        message.put(HttpClient.class, cl);
        
        message.put(KEY_HTTP_CONNECTION_ADDRESS, address);        
    }

    @Override
    protected OutputStream createOutputStream(Message message, boolean needToCacheRequest, boolean isChunking,
                                              int chunkThreshold)
        throws IOException {
        
        Object o = message.get("USING_URLCONNECTION");
        if (Boolean.TRUE == o) {
            return super.createOutputStream(message, needToCacheRequest, isChunking, chunkThreshold);
        }
        return new HttpClientWrappedOutputStream(message,
                                                 needToCacheRequest,
                                                 isChunking,
                                                 chunkThreshold,
                                                 getConduitName());
    }


    class HttpClientWrappedOutputStream extends WrappedOutputStream {
        List<Flow.Subscriber<? super ByteBuffer>> subscribers = new LinkedList<>();
        CompletableFuture<HttpResponse<InputStream>> future;
        long contentLen = -1;
        int rtimeout;
        volatile Throwable exception;
        volatile boolean connectionComplete;
        PipedInputStream pin;
        PipedOutputStream pout;
        HttpRequest request;
        
        
        HttpClientWrappedOutputStream(Message message,
                                      boolean needToCacheRequest, boolean isChunking,
                                      int chunkThreshold, String conduitName) {
            super(message, needToCacheRequest, isChunking,
                  chunkThreshold, conduitName, ((Address)message.get(KEY_HTTP_CONNECTION_ADDRESS)).getURI());
        }

        
        void addSubscriber(Flow.Subscriber<? super ByteBuffer> subscriber) {
            subscribers.add(subscriber);
        }

        @Override
        protected void setFixedLengthStreamingMode(int i) {
            contentLen = i;
        }
        
        @Override
        protected void handleNoOutput() throws IOException {
            contentLen = 0;
            pout.close();
            if (exception != null) {
                if (exception instanceof IOException) {
                    throw (IOException)exception;
                } else {
                    throw new IOException(exception);
                }
            }
        }

        public void setProtocolHeadersInBuilder(HttpRequest.Builder rb) throws IOException {
            boolean addHeaders = MessageUtils.getContextualBoolean(outMessage, Headers.ADD_HEADERS_PROPERTY, false);
            Headers h = new Headers(outMessage);
            boolean hasCT = false;
            for (Map.Entry<String, List<String>>  head : h.headerMap().entrySet()) {
                List<String> headerList = head.getValue();
                String header = head.getKey();
                if ("Connection".equals(header)) {
                    //HttpClient does not allow the Connection header
                    continue;
                }
                if (HttpHeaderHelper.CONTENT_TYPE.equalsIgnoreCase(header)) {
                    hasCT = true;
                    continue;
                }
                if (addHeaders || HttpHeaderHelper.COOKIE.equalsIgnoreCase(header)) {
                    headerList.forEach(s -> rb.header(header, s));
                } else {
                    rb.header(header, String.join(",", headerList));
                }
            }
            if (!h.headerMap().containsKey("User-Agent")) {
                rb.header("User-Agent", Headers.USER_AGENT);
            }   
            if (hasCT || !KNOWN_HTTP_VERBS_WITH_NO_CONTENT.contains(outMessage.get(Message.HTTP_REQUEST_METHOD))) {
                rb.header(HttpHeaderHelper.CONTENT_TYPE, h.determineContentType());
            }            
        }
        
        @Override
        protected void setProtocolHeaders() throws IOException {
            HttpClient cl = outMessage.get(HttpClient.class);
            Address address = (Address)outMessage.get(KEY_HTTP_CONNECTION_ADDRESS);
            HTTPClientPolicy csPolicy = getClient(outMessage);
            String httpRequestMethod =
                (String)outMessage.get(Message.HTTP_REQUEST_METHOD);

            pin = new PipedInputStream(csPolicy.getChunkLength() <= 0
                                        ? 4096 : csPolicy.getChunkLength());
            pout = new PipedOutputStream(pin);
            
            
            
            if (KNOWN_HTTP_VERBS_WITH_NO_CONTENT.contains(httpRequestMethod)
                || PropertyUtils.isTrue(outMessage.get(Headers.EMPTY_REQUEST_PROPERTY))) {
                contentLen = 0;
            }

            BodyPublisher bp = new BodyPublisher() {
                @Override
                public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
                    connectionComplete = true;
                    BodyPublishers.ofInputStream(new Supplier<InputStream>() {
                        public InputStream get() {
                            return pin;
                        }                
                    }).subscribe(subscriber);
                }

                @Override
                public long contentLength() {
                    return contentLen;
                }                
            };

            HttpRequest.Builder rb = HttpRequest.newBuilder()
                .method(httpRequestMethod, bp);  
            String verc = (String)outMessage.getContextualProperty(FORCE_HTTP_VERSION);
            if (verc == null) {
                verc = csPolicy.getVersion();
            }
            if ("1.1".equals(HTTP_VERSION) || "1.1".equals(verc)) {
                rb.version(Version.HTTP_1_1);  
            }            
            try {
                rb.uri(address.getURI());
            } catch (IllegalArgumentException iae) {
                MalformedURLException mex = new MalformedURLException(iae.getMessage());
                mex.initCause(iae);
                throw mex;
            }
            
            rtimeout = determineReceiveTimeout(outMessage, csPolicy);
            if (rtimeout > 0) {
                rb.timeout(Duration.ofMillis(rtimeout));
            }

            setProtocolHeadersInBuilder(rb);
                
            request = rb.build();
            
            
            final BodyHandler<InputStream> handler =  BodyHandlers.ofInputStream();
            
            future = cl.sendAsync(request, handler);
        }
        @Override
        protected void setupWrappedStream() throws IOException {
            if (cachingForRetransmission) {
                cachedStream =
                    new CacheAndWriteOutputStream(pout);
                wrappedStream = cachedStream;
            } else {
                wrappedStream = pout;
            }
            if (exception != null) {
                if (exception instanceof IOException) {
                    throw (IOException)exception;
                } else {
                    throw new IOException(exception);
                }
            }
        }
        @Override
        protected String getExceptionMessage(Throwable t) {
            if (t instanceof ConnectException && t.getMessage() == null) {
                return "Connection refused";
            }
            return t.getMessage();
        }

        HttpResponse<InputStream> getResponse() throws IOException {
            try {
                if (rtimeout > 0) {
                    return future.get(rtimeout, TimeUnit.MILLISECONDS);
                }
                return future.get();
            } catch (ExecutionException e) {
                Throwable t = e.getCause();
                if (t instanceof ConnectException) {
                    Throwable cause = t.getCause();
                    if (cause instanceof UnresolvedAddressException) {
                        UnknownHostException uhe = new UnknownHostException();
                        uhe.initCause(cause);
                        throw uhe;
                    }
                        
                }
                if (t instanceof IOException) {
                    IOException iot = (IOException)t;
                    throw iot;
                }
                throw new IOException(t);
            } catch (InterruptedException e) {
                throw new IOException(e);
            } catch (TimeoutException e) {
                throw (IOException)(new HttpTimeoutException("Timeout").initCause(e));
            }
            
        }
        
        @Override
        protected int getResponseCode() throws IOException {
            return getResponse().statusCode();
        }
        @Override
        protected void updateResponseHeaders(Message inMessage) throws IOException {
            Headers h = new Headers(inMessage);
            HttpResponse<InputStream> rsp = getResponse();
            h.readFromConnection(rsp.headers().map());
            if (rsp.headers().map().containsKey(Message.CONTENT_TYPE)) {
                List<String> s = rsp.headers().allValues(Message.CONTENT_TYPE);
                inMessage.put(Message.CONTENT_TYPE, String.join(",", s));                
            } else {
                inMessage.put(Message.CONTENT_TYPE, null);
            }
            cookies.readFromHeaders(h);
        }
        
        @Override
        protected InputStream getInputStream() throws IOException {
            HttpResponse<InputStream> resp = getResponse();
            String method = (String)outMessage.get(Message.HTTP_REQUEST_METHOD);
            int sc = resp.statusCode();
            if ("HEAD".equals(method)) {
                return null;
            }
            if (sc == 204) {
                //no content
                return null;
            }
            if ("OPTIONS".equals(method) || (sc >= 300 && sc < 500)) {
                Optional<String> f = resp.headers().firstValue("content-length");
                Optional<String> fChunk = resp.headers().firstValue("transfer-encoding");
                if (f.isPresent()) {
                    long l = Long.parseLong(f.get());
                    if (l == 0) {
                        return null;
                    }
                } else if (!fChunk.isPresent() || !"chunked".equals(fChunk.get())) {
                    if (resp.version() == Version.HTTP_2) {
                        InputStream in = resp.body();
                        if (in.available() <= 0) {
                            return null;
                        }
                    } else {
                        return null;
                    }
                }
            }
            return new FilterInputStream(resp.body()) {
                boolean closed;
                @Override
                public int read() throws IOException {
                    if (closed) {
                        throw new IOException("stream is closed");
                    }
                    return super.read();
                }

                @Override
                public int read(byte[] b) throws IOException {
                    if (closed) {
                        throw new IOException("stream is closed");
                    }
                    return super.read(b);
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    if (closed) {
                        throw new IOException("stream is closed");
                    }
                    return super.read(b, off, len);
                }

                @Override
                public void close() throws IOException {
                    closed = true;
                    super.close();
                }
            };
        }

        @Override
        protected void closeInputStream() throws IOException {
            getInputStream().close();
        }

        @Override
        protected void handleResponseAsync() throws IOException {
            handleResponseOnWorkqueue(true, false);
        }
        @Override
        public void thresholdReached() throws IOException {
            //not really a way to set the chunk size so not really anything to do
            if (exception != null) {
                if (exception instanceof IOException) {
                    throw (IOException)exception;
                } else {
                    throw new IOException(exception);
                }
            }
        }

        @Override
        protected String getResponseMessage() throws IOException {
            try {
                // HttpClient does not provide access to the actual status message
                // We'll map some of the status codes to match the
                // returns from the HTTPUrlConnection
                HttpResponse<InputStream> in = getResponse();
                switch (in.statusCode()) {
                case 404:
                    return "Not Found";
                case 405:
                    return "Method Not Allowed";
                case 503:
                    return "Service Unavailable";
                case 200:
                    return "OK";
                default:
                    return in.toString();
                }
            } catch (IOException e) {
                //ignore
            }
            return null;
        }
        
        @Override
        protected HttpsURLConnectionInfo getHttpsURLConnectionInfo() throws IOException {
            Address addrss = (Address)outMessage.get(KEY_HTTP_CONNECTION_ADDRESS);
            URI uri = addrss.getURI();
            
            if ("http".equals(uri.getScheme())) {
                return null;
            }
            String method = (String)outMessage.get(Message.HTTP_REQUEST_METHOD);
            HttpClient cl = outMessage.get(HttpClient.class);
            
            while (!connectionComplete || !cl.sslContext().getClientSessionContext().getIds().hasMoreElements()) {
                Thread.yield();
            }
            byte[] key = cl.sslContext().getClientSessionContext().getIds().nextElement();
            SSLSession session = cl.sslContext().getClientSessionContext().getSession(key);
            Certificate[] localCerts = session.getLocalCertificates();
            String cipherSuite = session.getCipherSuite();
            Principal principal = session.getLocalPrincipal();
            Certificate[] serverCerts = session.getPeerCertificates();
            Principal peer = session.getPeerPrincipal();
            
            HttpsURLConnectionInfo info = new HttpsURLConnectionInfo(uri, method, cipherSuite,
                                                                     localCerts, principal,
                                                                     serverCerts, peer);
            
            return info;
        }

        

        @Override
        protected boolean usingProxy() {
            HttpClient cl = outMessage.get(HttpClient.class);
            return cl.proxy().isPresent();
        }


        @Override
        protected InputStream getPartialResponse() throws IOException {
            HttpResponse<InputStream> rsp = getResponse();
            int responseCode = rsp.statusCode();
            if (responseCode == HttpURLConnection.HTTP_ACCEPTED
                || responseCode == HttpURLConnection.HTTP_OK) {
                try {
                    PushbackInputStream pbin =
                        new PushbackInputStream(rsp.body());
                    int c = pbin.read();
                    if (c != -1) {
                        pbin.unread((byte)c);
                        return pbin;
                    }
                } catch (IOException ioe) {
                    // ignore
                }
            }            
            // Don't need to do anything
            return null;
        }

        @Override
        protected void setupNewConnection(String newURL) throws IOException {
            connectionComplete = false;
            
            HTTPClientPolicy cp = getClient(outMessage);
            Address address;
            try {
                if (defaultAddress.getString().equals(newURL)) {
                    address = defaultAddress;
                } else {
                    address = new Address(newURL);
                }
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
            setupConnection(outMessage, address, cp);
            this.url = address.getURI();
        }

        @Override
        protected void retransmitStream() throws IOException {
            cachedStream.writeCacheTo(pout);
            pout.close();
        }

        @Override
        protected void updateCookiesBeforeRetransmit() throws IOException {
            Headers h = new Headers();
            HttpResponse<InputStream> rsp = getResponse();
            h.readFromConnection(rsp.headers().map());
            cookies.readFromHeaders(h);
        }

    }

}
