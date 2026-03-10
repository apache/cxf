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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.Address;
import org.apache.cxf.transport.http.Headers;
import org.apache.cxf.transport.http.HttpClientHTTPConduit;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.version.Version;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.ssl.SslHandler;


public class NettyHttpConduit extends HttpClientHTTPConduit implements BusLifeCycleListener {
    /**
     * Enable HTTP/2 support
     */
    public static final String ENABLE_HTTP2 = "org.apache.cxf.transports.http2.enabled";
    public static final String USE_ASYNC = "use.async.http.conduit";
    public static final String MAX_RESPONSE_CONTENT_LENGTH =
        "org.apache.cxf.transport.http.netty.maxResponseContentLength";
    static final Integer DEFAULT_MAX_RESPONSE_CONTENT_LENGTH = 1048576;
    private static final Set<String> KNOWN_HTTP_VERBS_WITH_NO_CONTENT =
            new HashSet<>(Arrays.asList(new String[]{"GET", "HEAD", "OPTIONS", "TRACE"}));
    
    final NettyHttpConduitFactory factory;
    private Bootstrap bootstrap;


    public NettyHttpConduit(Bus b, EndpointInfo ei, EndpointReferenceType t, NettyHttpConduitFactory conduitFactory)
        throws IOException {
        super(b, ei, t);
        factory = conduitFactory;
        bootstrap = new Bootstrap();
        EventLoopGroup eventLoopGroup = bus.getExtension(EventLoopGroup.class);
        bootstrap.group(eventLoopGroup);
        bootstrap.channel(NioSocketChannel.class);
    }

    public NettyHttpConduitFactory getNettyHttpConduitFactory() {
        return factory;
    }

    // Using Netty API directly
    protected void setupConnection(Message message, Address address, HTTPClientPolicy csPolicy) throws IOException {
        propagateProtocolSettings(message, csPolicy);

        URI uri = address.getURI();
        boolean addressChanged = false;

        // need to do some clean up work on the URI address
        String uriString = uri.toString();
        if (uriString.startsWith("netty://")) {
            try {
                uriString = uriString.substring(8);
                uri = new URI(uriString);
                addressChanged = true;
            } catch (URISyntaxException ex) {
                throw new MalformedURLException("unsupport uri: "  + uriString);
            }
        }
        String s = uri.getScheme();
        if (!"http".equals(s) && !"https".equals(s)) {
            throw new MalformedURLException("unknown protocol: " + s);
        }

        Object o = message.getContextualProperty(USE_ASYNC);
        if (o == null) {
            o = factory.getUseAsyncPolicy();
        }
        switch (NettyHttpConduitFactory.UseAsyncPolicy.getPolicy(o)) {
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

        // check tlsClientParameters from message header
        TLSClientParameters clientParameters = message.get(TLSClientParameters.class);
        if (clientParameters == null) {
            clientParameters = tlsClientParameters;
        }
        if ("https".equals(uri.getScheme())
            && clientParameters != null
            && clientParameters.getSSLSocketFactory() != null) {
            //if they configured in an SSLSocketFactory, we cannot do anything
            //with it as the NIO based transport cannot use socket created from
            //the SSLSocketFactory.
            o = false;
        }
        if (!PropertyUtils.isTrue(o)) {
            message.put(USE_ASYNC, Boolean.FALSE);
            super.setupConnection(message, addressChanged ? new Address(uriString, uri) : address, csPolicy);
            return;
        }
        message.put(USE_ASYNC, Boolean.TRUE);

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
        final boolean enableHttp2 = "2.0".equals(csPolicy.getVersion());
        final NettyHttpClientRequest request = new NettyHttpClientRequest(uri, httpRequestMethod, enableHttp2);
        final int ctimeout = determineConnectionTimeout(message, csPolicy);
        final int rtimeout = determineReceiveTimeout(message, csPolicy);
        final int maxResponseContentLength = determineMaxResponseContentLength(message);
        request.setConnectionTimeout(ctimeout);
        request.setReceiveTimeout(rtimeout);
        request.setMaxResponseContentLength(maxResponseContentLength);

        message.put(NettyHttpClientRequest.class, request);

    }

    private void propagateProtocolSettings(Message message, HTTPClientPolicy csPolicy) {
        if (message != null) {
            final Object o = message.getContextualProperty(ENABLE_HTTP2);
            if (o != null) {
                csPolicy.setVersion("2.0");
            }
        }
    }

    protected OutputStream createOutputStream(Message message,
                                              boolean needToCacheRequest,
                                              boolean isChunking,
                                              int chunkThreshold) throws IOException {

        if (Boolean.TRUE.equals(message.get(USE_ASYNC))) {

            NettyHttpClientRequest entity = message.get(NettyHttpClientRequest.class);
            NettyWrappedOutputStream out = new NettyWrappedOutputStream(message,
                    needToCacheRequest,
                    isChunking,
                    chunkThreshold,
                    getConduitName(),
                    entity.getUri());
            entity.createRequest(out.getOutBuffer());
            // TODO need to check how to set the Chunked feature
            //request.getRequest().setChunked(true);
            Object contentType = message.get(Message.CONTENT_TYPE);
            if (contentType != null) {
                entity.getRequest().headers().set(Message.CONTENT_TYPE, contentType);
            }
            return out;
        }
        return super.createOutputStream(message, needToCacheRequest, isChunking, chunkThreshold);
    }

    public class NettyWrappedOutputStream extends WrappedOutputStream {
        final HTTPClientPolicy csPolicy;
        final boolean enableHttp2;
        NettyHttpClientRequest entity;
        volatile HttpResponse httpResponse;
        volatile Throwable exception;
        volatile Channel channel;
        volatile SSLSession session;
        boolean isAsync;
        ByteBuf outBuffer;
        OutputStream outputStream;

        final Lock syncLock = new ReentrantLock();
        final Condition connected = syncLock.newCondition();
        final Condition responded = syncLock.newCondition();

        protected NettyWrappedOutputStream(Message message, boolean possibleRetransmit,
                                           boolean isChunking, int chunkThreshold, String conduitName, URI url) {
            super(message, possibleRetransmit, isChunking, chunkThreshold, conduitName, url);
            csPolicy = getClient(message);
            entity = message.get(NettyHttpClientRequest.class);
            int bufSize = csPolicy.getChunkLength() > 0 ? csPolicy.getChunkLength() : 16320;
            outBuffer = Unpooled.buffer(bufSize);
            outputStream = new ByteBufOutputStream(outBuffer);
            enableHttp2 = "2.0".equals(csPolicy.getVersion());
        }

        protected ByteBuf getOutBuffer() {
            return outBuffer;
        }


        protected HttpResponse getHttpResponse() throws IOException {
            syncLock.lock();
            try {
                while (httpResponse == null) {
                    if (exception == null) { //already have an exception, skip waiting
                        try {
                            responded.await(entity.getReceiveTimeout(), TimeUnit.MILLISECONDS);
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
            } finally {
                syncLock.unlock();
            }
        }

        protected HttpContent getHttpResponseContent() throws IOException {
            return (HttpContent) getHttpResponse();
        }

        protected Channel getChannel() throws IOException {
            syncLock.lock();
            try {
                while (channel == null) {
                    if (exception == null) { //already have an exception, skip waiting
                        try {
                            // connection timeout
                            connected.await(entity.getConnectionTimeout(), TimeUnit.MILLISECONDS);
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
            } finally {
                syncLock.unlock();
            }
        }


        @Override
        protected void setupWrappedStream() throws IOException {
            connect(true);
            wrappedStream = new OutputStream() {
                public void write(byte[] b, int off, int len) throws IOException {
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
                                setException(future.cause());
                            }
                        }
                    };
                    
                    synchronized (entity) {
                        Channel syncChannel = getChannel();
                        ChannelFuture channelFuture = syncChannel.writeAndFlush(entity);
                        channelFuture.addListener(listener);
                        outputStream.close();
                    }
                }
            };

            // If we need to cache for retransmission, store data in a
            // CacheAndWriteOutputStream. Otherwise write directly to the output stream.
            if (cachingForRetransmission) {
                cachedStream = new CacheAndWriteOutputStream(wrappedStream);
                wrappedStream = cachedStream;
            }
        }
        
        @Override
        protected void handleNoOutput() throws IOException {
            connect(false);
        }

        protected TLSClientParameters findTLSClientParameters() {
            TLSClientParameters clientParameters = outMessage.get(TLSClientParameters.class);
            if (clientParameters == null) {
                clientParameters = getTlsClientParameters();
            }
            if (clientParameters == null) {
                clientParameters = new TLSClientParameters();
            }
            return clientParameters;
        }

        protected void connect(boolean output) {
            final NettyHttpClientPipelineFactory handler; 
            if ("https".equals(url.getScheme())) {
                TLSClientParameters clientParameters = findTLSClientParameters();
                handler = new NettyHttpClientPipelineFactory(clientParameters, entity.getReceiveTimeout(),
                    entity.getMaxResponseContentLength(), enableHttp2);
            } else {
                handler = new NettyHttpClientPipelineFactory(null, entity.getReceiveTimeout(),
                    entity.getMaxResponseContentLength(), enableHttp2);
            }

            // Set handler
            bootstrap.handler(handler);

            ChannelFuture connFuture =
                bootstrap.connect(new InetSocketAddress(url.getHost(), url.getPort() != -1 ? url.getPort()
                                                            : "http".equals(url.getScheme()) ? 80 : 443));

            // Setup the call back on the NettyHttpClientRequest
            ChannelFutureListener listener = new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        setChannel(future.channel());
                        
                        SslHandler sslHandler = channel.pipeline().get(SslHandler.class);
                        
                        if (sslHandler != null) {
                            session = sslHandler.engine().getSession();
                        }
                    } else {
                        setException(future.cause());
                    }
                    synchronized (entity) {
                        //ensure entity is write in main thread
                    }
                }
            };

            connFuture.addListener(listener);

            // setup the CxfResponseCallBack
            CxfResponseCallBack callBack = new CxfResponseCallBack() {
                @Override
                public void responseReceived(HttpResponse response) {
                    setHttpResponse(response);
                }
                
                @Override
                public void error(Throwable ex) {
                    setException(ex);
                }
            };
            entity.setCxfResponseCallback(callBack);

            if (!output) {
                entity.getRequest().headers().remove("Transfer-Encoding");
                entity.getRequest().headers().remove("Content-Type");
                
                ChannelFutureListener writeFailureListener = new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            setException(future.cause());
                        }
                    }
                };
                
                connFuture.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess()) {
                                handler.whenReady(future.channel())
                                    .addListener(new ChannelFutureListener() {
                                        @Override
                                        public void operationComplete(ChannelFuture future) throws Exception {
                                            if (future.isSuccess()) {
                                                ChannelFuture channelFuture = future.channel().writeAndFlush(entity);
                                                channelFuture.addListener(writeFailureListener);
                                            }
                                        }
                                    }
                                );
                            }
                        }
                    });
            }
        }

        @Override
        protected HttpsURLConnectionInfo getHttpsURLConnectionInfo() throws IOException {
            if ("http".equals(outMessage.get("http.scheme"))) {
                return null;
            }
            connect(true);

            HostnameVerifier verifier = org.apache.cxf.transport.https.SSLUtils
                .getHostnameVerifier(findTLSClientParameters());

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
            setContentTypeHeader(h);
            boolean addHeaders = MessageUtils.getContextualBoolean(outMessage, Headers.ADD_HEADERS_PROPERTY, false);

            for (Map.Entry<String, List<String>> header : h.headerMap().entrySet()) {
                if (HttpHeaderHelper.CONTENT_TYPE.equalsIgnoreCase(header.getKey())) {
                    continue;
                }
                if (addHeaders || HttpHeaderHelper.COOKIE.equalsIgnoreCase(header.getKey())) {
                    for (String s : header.getValue()) {
                        entity.getRequest().headers().add(HttpHeaderHelper.COOKIE, s);
                    }
                } else if (!"Content-Length".equalsIgnoreCase(header.getKey())) {
                    StringBuilder b = new StringBuilder();
                    for (int i = 0; i < header.getValue().size(); i++) {
                        b.append(header.getValue().get(i));
                        if (i + 1 < header.getValue().size()) {
                            b.append(',');
                        }
                    }
                    entity.getRequest().headers().set(header.getKey(), b.toString());
                }
                if (!entity.getRequest().headers().contains("User-Agent")) {
                    entity.getRequest().headers().set("User-Agent", Version.getCompleteVersionString());
                }
            }
        }
        
        private void setContentTypeHeader(Headers headers) {
            if (outMessage.get(Message.CONTENT_TYPE) == null) {
                // if no content type is set then check for a request body
                Object requestMethod = outMessage.get(Message.HTTP_REQUEST_METHOD);
                boolean emptyRequest = KNOWN_HTTP_VERBS_WITH_NO_CONTENT.contains(requestMethod) 
                        || PropertyUtils.isTrue(outMessage.get(Headers.EMPTY_REQUEST_PROPERTY));
                // If it is not an empty request then add a content type
                if (!emptyRequest) {
                    entity.getRequest().headers().set(Message.CONTENT_TYPE, headers.determineContentType());
                }
            } else {
                entity.getRequest().headers().set(Message.CONTENT_TYPE, headers.determineContentType());
            }
        }

        @Override
        protected void setFixedLengthStreamingMode(int i) {
            // Here we can set the Content-Length
            entity.getRequest().headers().set("Content-Length", i);
            // TODO we need to deal with the Chunked information ourself
            //entity.getRequest().setChunked(false);
        }

        @Override
        protected int getResponseCode() throws IOException {
            return getHttpResponse().status().code();
        }

        @Override
        protected String getResponseMessage() throws IOException {
            return getHttpResponse().status().reasonPhrase();
        }

        @Override
        protected void updateResponseHeaders(Message inMessage) throws IOException {
            Headers h = new Headers(inMessage);
            inMessage.put(Message.CONTENT_TYPE, readHeaders(h));
            cookies.readFromHeaders(h);
        }

        private String readHeaders(Headers h) throws IOException {
            Set<String> headerNames = getHttpResponse().headers().names();
            String ct = null;
            for (String name : headerNames) {
                List<String> s = getHttpResponse().headers().getAll(name);
                h.headerMap().put(name, s);
                if (Message.CONTENT_TYPE.equalsIgnoreCase(name)) {
                    ct = getHttpResponse().headers().get(name);
                }
            }
            return ct;
        }

        @Override
        protected void handleResponseAsync() throws IOException {
            // The response hasn't been handled yet, should be handled asynchronously
            if (httpResponse == null) {
                isAsync = true;
            }
        }

        @Override
        protected void closeInputStream() throws IOException {
            //We just clear the buffer
            getHttpResponseContent().content().clear();
        }

        @Override
        protected boolean usingProxy() {
            // TODO we need to support it
            return false;
        }

        @Override
        protected InputStream getInputStream() throws IOException {
            return new ByteBufInputStream(getHttpResponseContent().content(), true);
        }

        @Override
        protected InputStream getPartialResponse() throws IOException {
            InputStream in = null;
            int responseCode = getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_ACCEPTED
                    || responseCode == HttpURLConnection.HTTP_OK) {

                String head = httpResponse.headers().get(HttpHeaderHelper.CONTENT_LENGTH);
                int cli = 0;
                if (head != null) {
                    cli = Integer.parseInt(head);
                }
                head = httpResponse.headers().get(HttpHeaderHelper.TRANSFER_ENCODING);
                boolean isChunked = head != null &&  HttpHeaderHelper.CHUNKED.equalsIgnoreCase(head);
                head = httpResponse.headers().get(HttpHeaderHelper.CONNECTION);
                boolean isEofTerminated = head != null &&  HttpHeaderHelper.CLOSE.equalsIgnoreCase(head);
                if (cli > 0) {
                    in = getInputStream();
                } else if (isChunked || isEofTerminated) {
                    // ensure chunked or EOF-terminated response is non-empty
                    try {
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
                Address address;
                if (defaultAddress.getString().equals(newURL)) {
                    address = defaultAddress;
                    this.url = defaultAddress.getURI();
                } else {
                    this.url = new URI(newURL);
                    address = new Address(newURL, this.url);
                }
                setupConnection(outMessage, address, csPolicy);
                entity = outMessage.get(NettyHttpClientRequest.class);
                //reset the buffers
                outBuffer.clear();
                outputStream = new ByteBufOutputStream(outBuffer);

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
            //TODO need to support the chunked version
            //entity.getRequest().setChunked(true);
        }

        protected void setHttpResponse(HttpResponse r) {
            syncLock.lock();
            try {
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
                responded.signalAll();
            } finally {
                syncLock.unlock();
            }
        }

        protected void setException(Throwable ex) {
            syncLock.lock();
            try {
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
                responded.signalAll();
            } finally {
                syncLock.unlock();
            }
        }

        protected void setChannel(Channel ch) {
            syncLock.lock();
            try {
                channel = ch;
                connected.signalAll();
            } finally {
                syncLock.unlock();
            }
        }
    }

    @Override
    public void initComplete() {

    }

    @Override
    public void postShutdown() {
        // shutdown the conduit
        this.close();

    }

    @Override
    public void preShutdown() {
    }

    protected static int determineMaxResponseContentLength(Message message) {
        Integer maxResponseContentLength = null;
        if (message.get(MAX_RESPONSE_CONTENT_LENGTH) != null) {
            Object obj = message.get(MAX_RESPONSE_CONTENT_LENGTH);
            try {
                maxResponseContentLength = Integer.parseInt(obj.toString());
            } catch (NumberFormatException e) {
                LOG.log(Level.WARNING, "INVALID_TIMEOUT_FORMAT", new Object[] {
                    MAX_RESPONSE_CONTENT_LENGTH, obj.toString()
                });
            }
        }
        if (maxResponseContentLength == null) {
            maxResponseContentLength = DEFAULT_MAX_RESPONSE_CONTENT_LENGTH;
        }
        return maxResponseContentLength;
    }


}
