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

package org.apache.cxf.systest.http2.netty;

import java.io.IOException;
import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.MediaType;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpClientUpgradeHandler;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DelegatingDecompressorFrameListener;
import io.netty.handler.codec.http2.Http2ClientUpgradeCodec;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapterBuilder;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.internal.PlatformDependent;

/**
 * TODO: Use CXF client once https://issues.apache.org/jira/browse/CXF-8606 is dones
 */
public class Http2TestClient implements AutoCloseable {
    private final SslContext ssl;
    
    public Http2TestClient(boolean secure) throws Exception {
        if (secure) {
            ssl = SslContext.newClientContext(
                SslProvider.JDK,
                null, 
                InsecureTrustManagerFactory.INSTANCE,
                Http2SecurityUtil.CIPHERS,
                SupportedCipherSuiteFilter.INSTANCE,
                new ApplicationProtocolConfig(
                        Protocol.ALPN,
                        SelectorFailureBehavior.FATAL_ALERT,
                        SelectedListenerFailureBehavior.FATAL_ALERT,
                        ApplicationProtocolNames.HTTP_2,
                        ApplicationProtocolNames.HTTP_1_1),
                0, 0);
        } else {
            ssl = null;
        }
    }
    
    public static class ClientResponse {
        private String body;
        private String protocol;
        private int responseCode;
        
        public ClientResponse(int responseCode, String protocol) {
            this.responseCode = responseCode;
            this.protocol = protocol;
        }

        public void setBody(String body) {
            this.body = body;
        }
        
        public String getBody() {
            return body;
        }
        
        public void setResponseCode(int rc) {
            this.responseCode = rc;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public String getProtocol() {
            return protocol;
        }
        
        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }
    }
    
    public class RequestBuilder {
        private final String address;
        private String path = "";
        private String accept = MediaType.WILDCARD;
        private HttpVersion version = HttpVersion.HTTP_1_1;

        public RequestBuilder(final String address) {
            this.address = address;
        }
        
        public RequestBuilder path(final String p) {
            this.path = p;
            return this;
        }
        
        
        public RequestBuilder accept(final String a) {
            this.accept = a;
            return this;
        }
        
        public RequestBuilder http2() {
            version = null;
            return this;
        }
        
        public ClientResponse get() throws Exception {
            return request(address, path, version, HttpMethod.GET, accept);
        }
        
        public ClientResponse trace() throws Exception {
            return request(address, path, version, HttpMethod.TRACE, accept);
        }
    }
    
    public RequestBuilder request(final String address) throws IOException {
        return new RequestBuilder(address);
    }

    public ClientResponse request(final String address, final String path, 
            final HttpVersion version, final HttpMethod method, final String accept) 
                throws Exception {

        final URI uri = URI.create(address);
                
        final Http2ClientInitializer initializer = new Http2ClientInitializer(Integer.MAX_VALUE);
        final NioEventLoopGroup worker = new NioEventLoopGroup();
        
        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(worker);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.remoteAddress(uri.getHost(), uri.getPort());
        bootstrap.handler(initializer);

        final Channel channel = bootstrap.connect().syncUninterruptibly().channel();
        final HttpResponseHandler responseHandler = initializer.getResponseHandler();
        final Http2SettingsHandler http2SettingsHandler = initializer.getSettingsHandler();

        try {
            final FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, path);
            request.headers().add(HttpHeaderNames.HOST, uri.getHost());
            request.headers().add(HttpHeaderNames.ACCEPT, accept);
            request.headers().add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), uri.getScheme());

            http2SettingsHandler.awaitSettings(5, TimeUnit.SECONDS);
            responseHandler.put(3, channel.write(request), channel.newPromise());
            
            channel.flush();
            responseHandler.awaitResponses(15, TimeUnit.SECONDS);
        } finally {
            channel.close().awaitUninterruptibly();
            worker.shutdownGracefully();
        }
        
        
        final List<ClientResponse> responses = responseHandler.responses();
        if (responses.size() != 1) {
            throw new IllegalStateException("Expected exactly one response, but got 0 or more");
        }
        
        return responses.get(0);
    }
    
    @Override
    public void close() throws Exception {
    }
    
    private class Http2SettingsHandler extends SimpleChannelInboundHandler<Http2Settings> {
        private ChannelPromise promise;

        Http2SettingsHandler(ChannelPromise promise) {
            this.promise = promise;
        }

        /**
         * Wait for this handler to be added after the upgrade to HTTP/2, and for initial preface
         * handshake to complete.
         */
        void awaitSettings(long timeout, TimeUnit unit) throws Exception {
            if (!promise.awaitUninterruptibly(timeout, unit)) {
                throw new IllegalStateException("Timed out waiting for settings");
            }
            if (!promise.isSuccess()) {
                throw new RuntimeException(promise.cause());
            }
        }
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Http2Settings msg) throws Exception {
            promise.setSuccess();
            ctx.pipeline().remove(this);
        }
    }
    
    private class HttpResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
        private final Map<Integer, Entry<ChannelFuture, ChannelPromise>> streamidPromiseMap;
        private final List<ClientResponse> responses = new CopyOnWriteArrayList<>(); 
        
        HttpResponseHandler() {
            streamidPromiseMap = PlatformDependent.newConcurrentHashMap();
        }

        Entry<ChannelFuture, ChannelPromise> put(int streamId, ChannelFuture writeFuture, ChannelPromise promise) {
            return streamidPromiseMap.put(streamId, new SimpleEntry<>(writeFuture, promise));
        }

        void awaitResponses(long timeout, TimeUnit unit) {
            final Iterator<Entry<Integer, Entry<ChannelFuture, ChannelPromise>>> itr = streamidPromiseMap
                .entrySet()
                .iterator();
            
            while (itr.hasNext()) {
                final Entry<Integer, Entry<ChannelFuture, ChannelPromise>> entry = itr.next();

                final ChannelFuture writeFuture = entry.getValue().getKey();
                if (!writeFuture.awaitUninterruptibly(timeout, unit)) {
                    throw new IllegalStateException("Timed out waiting to write for stream id " + entry.getKey());
                }
                
                if (!writeFuture.isSuccess()) {
                    throw new RuntimeException(writeFuture.cause());
                }
                
                final ChannelPromise promise = entry.getValue().getValue();
                if (!promise.awaitUninterruptibly(timeout, unit)) {
                    throw new IllegalStateException("Timed out waiting for response on stream id " + entry.getKey());
                }
                
                if (!promise.isSuccess()) {
                    throw new RuntimeException(promise.cause());
                }

                itr.remove();
            }
        }
        
        List<ClientResponse> responses() {
            return responses;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
            Integer streamId = msg.headers().getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
            if (streamId == null) {
                System.err.println("HttpResponseHandler unexpected message received: " + msg);
                return;
            }

            final Entry<ChannelFuture, ChannelPromise> entry = streamidPromiseMap.get(streamId);
            if (entry == null) {
                System.err.println("Message received for unknown stream id " + streamId);
            } else {
                final ByteBuf content = msg.content();
                final ClientResponse response = new ClientResponse(msg.status().code(), "HTTP/2.0");
                
                if (content.isReadable()) {
                    int contentLength = content.readableBytes();
                    byte[] arr = new byte[contentLength];
                    content.readBytes(arr);
                    response.setBody(new String(arr));
                }

                responses.add(response);
                entry.getValue().setSuccess();
            }
        }
    }
    
    private class Http2ClientInitializer extends ChannelInitializer<SocketChannel> {
        private final int maxContentLength;
        private HttpResponseHandler responseHandler;
        private Http2SettingsHandler settingsHandler;
        private Http2ConnectionHandler connectionHandler;

        Http2ClientInitializer(int maxContentLength) {
            this.maxContentLength = maxContentLength;
        }

        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            final Http2Connection connection = new DefaultHttp2Connection(false);

            responseHandler = new HttpResponseHandler();
            settingsHandler = new Http2SettingsHandler(ch.newPromise());
            
            connectionHandler = new HttpToHttp2ConnectionHandlerBuilder()
                .connection(connection)
                .frameListener(new DelegatingDecompressorFrameListener(connection,
                    new InboundHttp2ToHttpAdapterBuilder(connection)
                        .maxContentLength(maxContentLength)
                        .propagateSettings(true)
                        .build()))
                .build();

            if (ssl != null) {
                ch.pipeline().addLast(ssl.newHandler(ch.alloc()));
                ch.pipeline().addLast(connectionHandler);
                ch.pipeline().addLast(settingsHandler);
                ch.pipeline().addLast(responseHandler);
            } else {
                final HttpClientCodec sourceCodec = new HttpClientCodec();
                final Http2ClientUpgradeCodec upgradeCodec = new Http2ClientUpgradeCodec(connectionHandler);
                final HttpClientUpgradeHandler upgradeHandler = new HttpClientUpgradeHandler(sourceCodec, 
                    upgradeCodec, 65536);

                ch.pipeline().addLast(sourceCodec);
                ch.pipeline().addLast(upgradeHandler);
                ch.pipeline().addLast(new UpgradeRequestHandler(settingsHandler, responseHandler));
            }
        }
        
        HttpResponseHandler getResponseHandler() {
            return responseHandler;
        }
        
        Http2SettingsHandler getSettingsHandler() {
            return settingsHandler;
        }
    }
    
    /**
     * A handler that triggers the cleartext upgrade to HTTP/2 by sending an 
     * initial HTTP request.
     */
    private class UpgradeRequestHandler extends ChannelInboundHandlerAdapter {
        private final Http2SettingsHandler settingsHandler;
        private final HttpResponseHandler responseHandler;

        UpgradeRequestHandler(final Http2SettingsHandler settingsHandler, final HttpResponseHandler responseHandler) {
            this.settingsHandler = settingsHandler;
            this.responseHandler = responseHandler;
        }
        
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            final FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
            request.headers().add(HttpHeaderNames.HOST, "localhost");
            request.headers().add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), "http");

            ctx.writeAndFlush(request);
            ctx.fireChannelActive();
            
            ctx.pipeline().remove(this);
            ctx.pipeline().addLast(settingsHandler);
            ctx.pipeline().addLast(responseHandler);
        }
    }
}
