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

package org.apache.cxf.transport.http.netty.server;

import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.security.ClientAuthentication;
import org.apache.cxf.transport.http.netty.server.interceptor.ChannelInterceptor;
import org.apache.cxf.transport.http.netty.server.interceptor.HttpSessionInterceptor;
import org.apache.cxf.transport.http.netty.server.session.DefaultHttpSessionStore;
import org.apache.cxf.transport.http.netty.server.session.HttpSessionStore;
import org.apache.cxf.transport.https.SSLContextInitParameters;
import org.apache.cxf.transport.https.SSLUtils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.ImmediateEventExecutor;

public class NettyHttpServletPipelineFactory extends ChannelInitializer<Channel> {
    private static final Logger LOG =
        LogUtils.getL7dLogger(NettyHttpServletPipelineFactory.class);

    //Holds the child channel
    private final ChannelGroup allChannels = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);

    private final HttpSessionWatchdog watchdog;

    private final TLSServerParameters tlsServerParameters;

    private final boolean supportSession;

    private final Map<String, NettyHttpContextHandler> handlerMap;

    private final int maxChunkContentSize;

    private final EventExecutorGroup applicationExecutor;

    private final NettyHttpServerEngine nettyHttpServerEngine;
    
    private final boolean enableHttp2; /* enable HTTP2 support */

    /**
     * @deprecated use {@link #NettyHttpServletPipelineFactory(TLSServerParameters, boolean, int, Map,
     * NettyHttpServerEngine, EventExecutorGroup)}
     */
    @Deprecated
    public NettyHttpServletPipelineFactory(TLSServerParameters tlsServerParameters,
                                           boolean supportSession, int threadPoolSize, int maxChunkContentSize,
                                           Map<String, NettyHttpContextHandler> handlerMap,
                                           NettyHttpServerEngine engine) {
        this(tlsServerParameters, supportSession, maxChunkContentSize, handlerMap, engine,
                new DefaultEventExecutorGroup(threadPoolSize));
    }

    public NettyHttpServletPipelineFactory(TLSServerParameters tlsServerParameters,
            boolean supportSession, int maxChunkContentSize,
            Map<String, NettyHttpContextHandler> handlerMap,
            NettyHttpServerEngine engine, EventExecutorGroup applicationExecutor) {
        this(tlsServerParameters, supportSession, maxChunkContentSize, handlerMap, engine,
            applicationExecutor, false);
    }

    public NettyHttpServletPipelineFactory(TLSServerParameters tlsServerParameters,
                                           boolean supportSession, int maxChunkContentSize,
                                           Map<String, NettyHttpContextHandler> handlerMap,
                                           NettyHttpServerEngine engine, EventExecutorGroup applicationExecutor,
                                           boolean enableHttp2) {
        this.supportSession = supportSession;
        this.watchdog = new HttpSessionWatchdog();
        this.handlerMap = handlerMap;
        this.tlsServerParameters = tlsServerParameters;
        this.maxChunkContentSize = maxChunkContentSize;
        this.nettyHttpServerEngine = engine;
        this.applicationExecutor = applicationExecutor;
        this.enableHttp2 = enableHttp2;
    }

    public Map<String, NettyHttpContextHandler> getHttpContextHandlerMap() {
        return handlerMap;
    }

    public ChannelGroup getAllChannels() {
        return allChannels;
    }

    public NettyHttpContextHandler getNettyHttpHandler(String url) {
        for (Map.Entry<String, NettyHttpContextHandler> entry : handlerMap.entrySet()) {
            // Here just check the context path first
            if (url.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void start() {
        if (supportSession) {
            new Thread(watchdog).start();
        }
    }

    public void shutdown() {
        allChannels.close().awaitUninterruptibly();
        watchdog.stopWatching();
        applicationExecutor.shutdownGracefully();
    }

    protected HttpSessionStore getHttpSessionStore() {
        return new DefaultHttpSessionStore();
    }

    protected NettyHttpServletHandler getServletHandler() {

        NettyHttpServletHandler handler = new NettyHttpServletHandler(this);
        handler.addInterceptor(new ChannelInterceptor());
        if (supportSession) {
            handler.addInterceptor(new HttpSessionInterceptor(getHttpSessionStore()));
        }
        return handler;
    }

    protected ChannelPipeline getDefaultHttpChannelPipeline(Channel channel) throws Exception {

        // Create a default pipeline implementation.
        ChannelPipeline pipeline = channel.pipeline();

        SslHandler sslHandler = configureServerHttpSSLOnDemand();
        if (sslHandler != null) {
            LOG.log(Level.FINE,
                    "Server SSL handler configured and added as an interceptor against the ChannelPipeline: {}",
                    sslHandler);
            pipeline.addLast("ssl", sslHandler);
        }

        configureDefaultHttpPipeline(pipeline);

        return pipeline;
    }
    
    protected void configureDefaultHttp2Pipeline(ChannelPipeline pipeline) {
        pipeline
            .addLast(Http2FrameCodecBuilder.forServer().build())
            .addLast(new Http2MultiplexHandler(createHttp2ChannelInitializer()));
    }

    protected void configureDefaultHttpPipeline(ChannelPipeline pipeline) {
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("aggregator", new HttpObjectAggregator(maxChunkContentSize));
        
        // Remove the following line if you don't want automatic content
        // compression.
        pipeline.addLast("deflater", new HttpContentCompressor());
        
        // Set up the idle handler
        pipeline.addLast("idle", new IdleStateHandler(nettyHttpServerEngine.getReadIdleTime(),
                nettyHttpServerEngine.getWriteIdleTime(), 0));
    }

    private SslHandler configureServerHttpSSLOnDemand() throws Exception {
        if (tlsServerParameters != null) {
            SSLEngine sslEngine = SSLUtils.createServerSSLEngine(tlsServerParameters);
            return new SslHandler(sslEngine);
        }
        return null;
    }
    
    private SslContext configureServerHttp2SSLOnDemand() throws Exception {
        if (tlsServerParameters != null) {
            final SSLContextInitParameters initParams = SSLUtils.getSSLContextInitParameters(tlsServerParameters);
            // Use only JDK provider for now, leaving OpenSsl as an option
            final SslProvider provider = SslProvider.JDK;
    
            final KeyManager[] keyManagers = initParams.getKeyManagers();
            if (keyManagers == null || keyManagers.length == 0) {
                throw new IllegalStateException("No KeyManagers are configured, unable "
                        + "to create Netty's SslContext instance");
            }
            
            final String[] cipherSuites = org.apache.cxf.configuration.jsse.SSLUtils
                .getCiphersuitesToInclude(
                        tlsServerParameters.getCipherSuites(), 
                        tlsServerParameters.getCipherSuitesFilter(),
                        SSLContext.getDefault().getDefaultSSLParameters().getCipherSuites(),
                        Http2SecurityUtil.CIPHERS.toArray(new String[] {}),
                        LOG);
            
            final SslContextBuilder builder = SslContextBuilder
                .forServer(keyManagers[0]) /* only first is used, as with SSLContext::init*/
                .sslProvider(provider)
                .ciphers(Arrays.asList(cipherSuites), SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(
                    new ApplicationProtocolConfig(
                        Protocol.ALPN,
                        // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                        SelectorFailureBehavior.NO_ADVERTISE,
                        // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                        SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.HTTP_2,
                        ApplicationProtocolNames.HTTP_1_1
                    ));
            
            final TrustManager[] trustManagers = initParams.getTrustManagers();
            if (trustManagers != null && trustManagers.length > 0) {
                builder.trustManager(trustManagers[0]);
            }
            
            final ClientAuthentication clientAuth = tlsServerParameters.getClientAuthentication();
            if (clientAuth != null) {
                if (clientAuth.isSetRequired() && clientAuth.isRequired()) {
                    builder.clientAuth(ClientAuth.REQUIRE);
                } else if (clientAuth.isSetWant() && clientAuth.isWant()) {
                    builder.clientAuth(ClientAuth.OPTIONAL);
                }
            }
            
            return builder.build();
        }
        
        return null;
    }

    private final class HttpSessionWatchdog implements Runnable {

        private boolean shouldStopWatching;

        @Override
        public void run() {

            while (!shouldStopWatching) {

                try {
                    HttpSessionStore store = getHttpSessionStore();
                    if (store != null) {
                        store.destroyInactiveSessions();
                    }
                    Thread.sleep(5000);

                } catch (InterruptedException e) {
                    return;
                }

            }

        }

        public void stopWatching() {
            this.shouldStopWatching = true;
        }

    }
    
    /**
     * Application negotiation handler to select either HTTP 1.1 or HTTP 2 protocol, based
     * on client/server ALPN negotiations.
     */
    private class Http2OrHttpHandler extends ApplicationProtocolNegotiationHandler {
        protected Http2OrHttpHandler() {
            super(ApplicationProtocolNames.HTTP_1_1);
        }

        @Override
        protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
            if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
                configureDefaultHttp2Pipeline(ctx.pipeline());
            } else if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
                configureDefaultHttpPipeline(ctx.pipeline());
                ctx.pipeline().addLast(applicationExecutor, "handler", getServletHandler());
            } else {
                throw new IllegalStateException("Unknown application protocol: " + protocol);
            }
        }
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        if (!enableHttp2) {
            final ChannelPipeline pipeline = getDefaultHttpChannelPipeline(ch);
            pipeline.addLast(applicationExecutor, "handler", this.getServletHandler());
        } else {
            getDefaultHttp2ChannelPipeline(ch);
        }
    }
    
    protected ChannelPipeline getDefaultHttp2ChannelPipeline(Channel channel) throws Exception {
        // Create a default pipeline implementation with HTTP/2 support
        ChannelPipeline pipeline = channel.pipeline();

        SslContext sslCtx = configureServerHttp2SSLOnDemand();
        if (sslCtx != null) {
            final SslHandler sslHandler = sslCtx.newHandler(channel.alloc());

            LOG.log(Level.FINE,
                    "Server SSL handler configured and added as an interceptor against the ChannelPipeline: {}",
                    sslHandler);
            
            pipeline.addLast(sslHandler, new Http2OrHttpHandler());
            return pipeline;
        }
        
        final UpgradeCodecFactory upgradeCodecFactory = new UpgradeCodecFactory() {
            @Override
            public UpgradeCodec newUpgradeCodec(CharSequence protocol) {
                if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
                    return new Http2ServerUpgradeCodec(
                        Http2FrameCodecBuilder.forServer().build(),
                        new Http2MultiplexHandler(createHttp2ChannelInitializer()));
                } else {
                    return null;
                }
            }
        };        
        
        final HttpServerCodec sourceCodec = new HttpServerCodec();
        final HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(sourceCodec, upgradeCodecFactory,
                                                                                     32 * 1024 * 1024);
        final CleartextHttp2ServerUpgradeHandler cleartextUpgradeHandler = new CleartextHttp2ServerUpgradeHandler(
            sourceCodec, upgradeHandler, createHttp2ChannelInitializerPriorKnowledge());

        pipeline.addLast(cleartextUpgradeHandler);
        pipeline.addLast(new SimpleChannelInboundHandler<HttpMessage>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
                // If this handler is hit then no upgrade has been attempted and the client is just talking HTTP
                final ChannelPipeline pipeline = ctx.pipeline();
                
                pipeline.addAfter(applicationExecutor, ctx.name(), "handler", getServletHandler());
                pipeline.replace(this, "aggregator", new HttpObjectAggregator(maxChunkContentSize));

                // Remove the following line if you don't want automatic content compression.
                pipeline.addLast("deflater", new HttpContentCompressor());

                // Set up the idle handler
                pipeline.addLast("idle", new IdleStateHandler(nettyHttpServerEngine.getReadIdleTime(),
                        nettyHttpServerEngine.getWriteIdleTime(), 0));

                ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
            }
        });
        
        return pipeline;
    }

    private ChannelInitializer<Channel> createHttp2ChannelInitializer() {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel childChannel) throws Exception {
                childChannel.pipeline()
                    .addLast(new Http2StreamFrameToHttpObjectCodec(true))
                    .addLast("aggregator", new HttpObjectAggregator(maxChunkContentSize))
                    .addLast(applicationExecutor, getServletHandler());
            }
        };
    }
    
    private ChannelInitializer<Channel> createHttp2ChannelInitializerPriorKnowledge() {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel childChannel) throws Exception {
                configureDefaultHttp2Pipeline(childChannel.pipeline());
            }
        };
    }
}
