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

package org.apache.cxf.transport.http.netty.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.Headers;
import org.apache.cxf.transport.http.URLConnectionHTTPConduit;
import org.apache.cxf.transport.https.CertificateHostnameVerifier;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.version.Version;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.ssl.SslHandler;

public class NettyHttpConduit extends URLConnectionHTTPConduit {
    public static final String USE_ASYNC = "use.async.http.conduit";
    final NettyHttpConduitFactory factory;
    private final ClientBootstrap bootstrap;
    
    public NettyHttpConduit(Bus b, EndpointInfo ei, EndpointReferenceType t, NettyHttpConduitFactory conduitFactory)
        throws IOException {
        super(b, ei, t);
        factory = conduitFactory;
        bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory());
    }

    public NettyHttpConduitFactory getNettyHttpConduitFactory() {
        return factory;
    }
    
    public void close() {
        super.close();
        // clean up the resource that ClientChannelFactory used
        bootstrap.shutdown();
    }

    // Using Netty API directly
    protected void setupConnection(Message message, URI uri, HTTPClientPolicy csPolicy) throws IOException {

        String s = uri.getScheme();
        if (!"http".equals(s) && !"https".equals(s)) {
            throw new MalformedURLException("unknown protocol: " + s);
        }

        if (StringUtils.isEmpty(uri.getPath())) {
            //hc needs to have the path be "/"
            uri = uri.resolve("/");
        }

        message.put("http.scheme", uri.getScheme());
        String httpRequestMethod =
                (String)message.get(Message.HTTP_REQUEST_METHOD);
        if (httpRequestMethod == null) {
            httpRequestMethod = "POST";
            message.put(Message.HTTP_REQUEST_METHOD, httpRequestMethod);
        }
        // setup a new NettyHttpClientRequest
        final NettyHttpClientRequest request = new NettyHttpClientRequest(uri, httpRequestMethod);
        final int ctimeout = determineConnectionTimeout(message, csPolicy);
        final int rtimeout = determineReceiveTimeout(message, csPolicy);
        request.setConnectionTimeout(ctimeout);
        request.setReceiveTimeout(rtimeout);
        request.getRequest().setChunked(true);
        request.getRequest().setHeader(Message.CONTENT_TYPE, (String)message.get(Message.CONTENT_TYPE));
        // need to socket connection timeout

        message.put(NettyHttpClientRequest.class, request);
        bootstrap.setPipelineFactory(new NettyHttpClientPipelineFactory(getTlsClientParameters()));
    }

    protected OutputStream createOutputStream(Message message,
                                              boolean needToCacheRequest,
                                              boolean isChunking,
                                              int chunkThreshold) throws IOException {

        NettyHttpClientRequest entity = message.get(NettyHttpClientRequest.class);
        NettyWrappedOutputStream out = new NettyWrappedOutputStream(message,
                needToCacheRequest,
                isChunking,
                chunkThreshold,
                getConduitName(),
                entity.getUri());
        entity.getRequest().setContent(out.getOutBuffer());
        return out;


    }

    public class NettyWrappedOutputStream extends WrappedOutputStream {
        final HTTPClientPolicy csPolicy;
        NettyHttpClientRequest entity;
        volatile HttpResponse httpResponse;
        volatile Throwable exception;
        volatile Channel channel;
        volatile SSLSession session;
        boolean isAsync;
        ChannelBuffer outBuffer;
        OutputStream outputStream;

        protected NettyWrappedOutputStream(Message message, boolean possibleRetransmit,
                                           boolean isChunking, int chunkThreshold, String conduitName, URI url) {
            super(message, possibleRetransmit, isChunking, chunkThreshold, conduitName, url);
            csPolicy = getClient(message);
            entity  = message.get(NettyHttpClientRequest.class);
            int bufSize = csPolicy.getChunkLength() > 0 ? csPolicy.getChunkLength() : 16320;
            outBuffer = ChannelBuffers.dynamicBuffer(bufSize);
            outputStream = new ChannelBufferOutputStream(outBuffer);
        }

        protected ChannelBuffer getOutBuffer() {
            return outBuffer;
        }


        protected synchronized HttpResponse getHttpResponse() throws IOException {
            while (httpResponse == null) {
                if (exception == null) { //already have an exception, skip waiting
                    try {
                        wait(entity.getReceiveTimeout());
                    } catch (InterruptedException e) {
                        throw new IOException(e);
                    }
                }
                if (httpResponse == null) {

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


        protected synchronized Channel getChannel() throws IOException {
            while (channel == null) {
                if (exception == null) { //already have an exception, skip waiting
                    try {
                        // connection timeout
                        wait(entity.getConnectionTimeout());
                    } catch (InterruptedException e) {
                        throw new IOException(e);
                    }
                }
                if (channel == null) {

                    if (exception != null) {
                        if (exception instanceof IOException) {
                            throw (IOException)exception;
                        }
                        if (exception instanceof RuntimeException) {
                            throw (RuntimeException)exception;
                        }
                        throw new IOException(exception);
                    }

                    throw new SocketTimeoutException("Connection Timeout");
                }
            }
            return channel;

        }


        @Override
        protected void setupWrappedStream() throws IOException {
            connect(true);
            wrappedStream =  new OutputStream() {
                public void write(byte b[], int off, int len) throws IOException {
                    outputStream.write(b, off, len);
                }
                public void write(int b) throws IOException {
                    outputStream.write(b);
                }
                public void close() throws IOException {
                    // Setup the call back for sending the message
                    ChannelFutureListener listener = new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (!future.isSuccess()) {
                                setException(future.getCause());
                            }
                        }
                    };
                    ChannelFuture channelFuture = getChannel().write(entity);
                    channelFuture.addListener(listener);
                    outputStream.close();
                }
            };

            // If we need to cache for retransmission, store data in a
            // CacheAndWriteOutputStream. Otherwise write directly to the output stream.
            if (cachingForRetransmission) {
                cachedStream = new CacheAndWriteOutputStream(wrappedStream);
                wrappedStream = cachedStream;
            }
        }

        protected void connect(boolean output) {

            ChannelFuture connFuture = 
                bootstrap.connect(new InetSocketAddress(url.getHost(), url.getPort()));

            // Setup the call back on the NettyHttpClientRequest
            ChannelFutureListener listener = new ChannelFutureListener() {
                
                private final AtomicBoolean handshakeDone = new AtomicBoolean(false);

                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        setChannel(future.getChannel());
                        SslHandler sslHandler = channel.getPipeline().get(SslHandler.class);
                        if (sslHandler != null) {
                            session = sslHandler.getEngine().getSession();
                        }
                    } else {
                        setException((Exception) future.getCause());
                    }
                }
            };

            connFuture.addListener(listener);

            if (!output) {
                entity.getRequest().removeHeader("Transfer-Encoding");
                entity.getRequest().removeHeader("Content-Type");
                entity.getRequest().setContent(null);
            }

            // setup the CxfResponseCallBack
            CxfResponseCallBack callBack = new CxfResponseCallBack() {
                @Override
                public void responseReceived(HttpResponse response) {
                    setHttpResponse(response);
                }
            };
            entity.setCxfResponseCallback(callBack);

        }

        @Override
        protected HttpsURLConnectionInfo getHttpsURLConnectionInfo() throws IOException {
            if ("http".equals(outMessage.get("http.scheme"))) {
                return null;
            }
            connect(true);
           
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

        @Override
        protected void setProtocolHeaders() throws IOException {
            Headers h = new Headers(outMessage);
            entity.getRequest().setHeader(Message.CONTENT_TYPE, h.determineContentType());
            boolean addHeaders = MessageUtils.isTrue(outMessage.getContextualProperty(Headers.ADD_HEADERS_PROPERTY));

            for (Map.Entry<String, List<String>> header : h.headerMap().entrySet()) {
                if (HttpHeaderHelper.CONTENT_TYPE.equalsIgnoreCase(header.getKey())) {
                    continue;
                }
                if (addHeaders || HttpHeaderHelper.COOKIE.equalsIgnoreCase(header.getKey())) {
                    for (String s : header.getValue()) {
                        entity.getRequest().addHeader(HttpHeaderHelper.COOKIE, s);
                    }
                } else if (!"Content-Length".equalsIgnoreCase(header.getKey())) {
                    StringBuilder b = new StringBuilder();
                    for (int i = 0; i < header.getValue().size(); i++) {
                        b.append(header.getValue().get(i));
                        if (i + 1 < header.getValue().size()) {
                            b.append(',');
                        }
                    }
                    entity.getRequest().setHeader(header.getKey(), b.toString());
                }
                if (!entity.getRequest().containsHeader("User-Agent")) {
                    entity.getRequest().setHeader("User-Agent", Version.getCompleteVersionString());
                }
            }
        }

        @Override
        protected void setFixedLengthStreamingMode(int i) {
            // Here we can set the Content-Length
            entity.getRequest().setHeader("Content-Length", i);
            entity.getRequest().setChunked(false);
        }

        @Override
        protected int getResponseCode() throws IOException {
            return getHttpResponse().getStatus().getCode();
        }

        @Override
        protected String getResponseMessage() throws IOException {
            return getHttpResponse().getStatus().getReasonPhrase();
        }

        @Override
        protected void updateResponseHeaders(Message inMessage) throws IOException {
            Headers h = new Headers(inMessage);
            inMessage.put(Message.CONTENT_TYPE, readHeaders(h));
            cookies.readFromHeaders(h);
        }

        private String readHeaders(Headers h) throws IOException {
            Set<String> headerNames = getHttpResponse().getHeaderNames();
            String ct = null;
            for (String name : headerNames) {
                List<String> s = getHttpResponse().getHeaders(name);
                h.headerMap().put(name, s);
                if (Message.CONTENT_TYPE.equalsIgnoreCase(name)) {
                    ct = getHttpResponse().getHeader(name);
                }
            }
            return ct;
        }

        @Override
        protected void handleResponseAsync() throws IOException {
            isAsync = true;
        }

        @Override
        protected void closeInputStream() throws IOException {
            //We just clear the buffer
            getHttpResponse().getContent().clear();
        }

        @Override
        protected boolean usingProxy() {
            // TODO we need to support it
            return false;
        }

        @Override
        protected InputStream getInputStream() throws IOException {
            return new ChannelBufferInputStream(getHttpResponse().getContent());
        }

        @Override
        protected InputStream getPartialResponse() throws IOException {
            InputStream in = null;
            int responseCode = getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_ACCEPTED
                    || responseCode == HttpURLConnection.HTTP_OK) {

                String head = httpResponse.getHeader(HttpHeaderHelper.CONTENT_LENGTH);
                int cli = 0;
                if (head != null) {
                    cli = Integer.parseInt(head);
                }
                head = httpResponse.getHeader(HttpHeaderHelper.TRANSFER_ENCODING);
                boolean isChunked = head != null &&  HttpHeaderHelper.CHUNKED.equalsIgnoreCase(head);
                head = httpResponse.getHeader(HttpHeaderHelper.CONNECTION);
                boolean isEofTerminated = head != null &&  HttpHeaderHelper.CLOSE.equalsIgnoreCase(head);
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

        @Override
        protected void setupNewConnection(String newURL) throws IOException {
            httpResponse = null;
            isAsync = false;
            exception = null;
            if (channel != null) {
                channel.close();
                channel = null;
            }

            try {
                this.url = new URI(newURL);
                setupConnection(outMessage, this.url, csPolicy);
                entity = outMessage.get(NettyHttpClientRequest.class);
                //reset the buffers
                outBuffer.clear();
                outputStream = new ChannelBufferOutputStream(outBuffer);

            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }

        @Override
        protected void retransmitStream() throws IOException {
            cachingForRetransmission = false; //already cached
            setupWrappedStream();
            cachedStream.writeCacheTo(wrappedStream);
            wrappedStream.flush();
            wrappedStream.close();
        }

        @Override
        protected void updateCookiesBeforeRetransmit() throws IOException {
            Headers h = new Headers();
            readHeaders(h);
            cookies.readFromHeaders(h);
        }

        @Override
        public void thresholdReached() throws IOException {
            entity.getRequest().setChunked(true);
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

        protected synchronized void setException(Throwable ex) {
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

        protected synchronized void setChannel(Channel ch) {
            channel = ch;
            notifyAll();
        }
    }


}
