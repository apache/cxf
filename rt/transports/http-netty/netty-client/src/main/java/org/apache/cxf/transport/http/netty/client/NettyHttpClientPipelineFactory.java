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

import java.security.KeyStore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.transport.https.SSLContextInitParameters;
import org.apache.cxf.transport.https.SSLUtils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpClientUpgradeHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DelegatingDecompressorFrameListener;
import io.netty.handler.codec.http2.Http2ClientUpgradeCodec;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapterBuilder;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.SimpleKeyManagerFactory;
import io.netty.handler.ssl.util.SimpleTrustManagerFactory;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.AttributeKey;

public class NettyHttpClientPipelineFactory extends ChannelInitializer<Channel> {
    private static final String WHEN_READY_KEY = "WhenReady-Key";
    private static final AttributeKey<ChannelFuture> WHEN_READY = AttributeKey.valueOf(WHEN_READY_KEY);

    private static final Logger LOG =
        LogUtils.getL7dLogger(NettyHttpClientPipelineFactory.class);

    private final TLSClientParameters tlsClientParameters;
    private final int readTimeout;
    private final int maxContentLength;
    private final boolean enableHttp2;

    public NettyHttpClientPipelineFactory(TLSClientParameters clientParameters) {
        this(clientParameters, 0);
    }

    public NettyHttpClientPipelineFactory(TLSClientParameters clientParameters, int readTimeout) {
        this(clientParameters, readTimeout, NettyHttpConduit.DEFAULT_MAX_RESPONSE_CONTENT_LENGTH);
    }

    public NettyHttpClientPipelineFactory(TLSClientParameters clientParameters, int readTimeout,
                                          int maxResponseContentLength) {
        this(clientParameters, readTimeout, maxResponseContentLength, false);
    }
    
    public NettyHttpClientPipelineFactory(TLSClientParameters clientParameters, int readTimeout,
            int maxResponseContentLength, boolean enableHttp2) {
        this.tlsClientParameters = clientParameters;
        this.readTimeout = readTimeout;
        this.maxContentLength = maxResponseContentLength;
        this.enableHttp2 = enableHttp2;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        SslHandler sslHandler = configureClientSSLOnDemand(ch);
        if (sslHandler != null) {
            LOG.log(Level.FINE,
                    "Server SSL handler configured and added as an interceptor against the ChannelPipeline: {}",
                    sslHandler);
            pipeline.addLast("ssl", sslHandler);
        }

        final NettyHttpClientHandler responseHandler = new NettyHttpClientHandler();
        final ChannelPromise readyFuture = ch.newPromise();
        ch.attr(WHEN_READY).set(readyFuture);

        if (enableHttp2) {
            final Http2Connection connection = new DefaultHttp2Connection(false);
            final Http2SettingsHandler settingsHandler = new Http2SettingsHandler(readyFuture);
            
            Http2ConnectionHandler connectionHandler = new HttpToHttp2ConnectionHandlerBuilder()
                .connection(connection)
                .frameListener(new DelegatingDecompressorFrameListener(connection,
                    new InboundHttp2ToHttpAdapterBuilder(connection)
                        .maxContentLength(maxContentLength)
                        .propagateSettings(true)
                        .build()))
                .build();

            if (sslHandler != null) {
                // Wait for the handshake to finish and the protocol to be negotiated before 
                // configuring the HTTP/2 components of the pipeline.
                pipeline.addLast(new ApplicationProtocolNegotiationHandler(ApplicationProtocolNames.HTTP_1_1) {
                    @Override
                    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
                        final ChannelPipeline p = ctx.pipeline();
                        if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
                            p.addLast(connectionHandler);
                            p.addLast(settingsHandler);
                            p.addLast("client", responseHandler);
                        } else if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
                            p.addLast("decoder", new HttpResponseDecoder());
                            p.addLast("aggregator", new HttpObjectAggregator(maxContentLength));
                            p.addLast("encoder", new HttpRequestEncoder());
                            p.addLast("chunkedWriter", new ChunkedWriteHandler());
                            readyFuture.setSuccess(null);
                        } else {
                            ctx.close();
                            final IllegalStateException ex = new IllegalStateException("Unknown protocol: " + protocol);
                            readyFuture.setFailure(ex);
                            throw ex;
                        }
                    }
                });
                
                if (readTimeout > 0) {
                    pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS));
                }
            } else {
                final HttpClientCodec sourceCodec = new HttpClientCodec();
                final Http2ClientUpgradeCodec upgradeCodec = new Http2ClientUpgradeCodec(connectionHandler);
                final HttpClientUpgradeHandler upgradeHandler = new HttpClientUpgradeHandler(sourceCodec, 
                    upgradeCodec, maxContentLength);

                pipeline.addLast(sourceCodec);
                pipeline.addLast(upgradeHandler);

                if (readTimeout > 0) {
                    pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS));
                }
                
                pipeline.addLast("client", responseHandler);
                readyFuture.setSuccess(null);
            }
        } else {
            pipeline.addLast("decoder", new HttpResponseDecoder());
            pipeline.addLast("aggregator", new HttpObjectAggregator(maxContentLength));
            pipeline.addLast("encoder", new HttpRequestEncoder());
            pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
            if (readTimeout > 0) {
                pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS));
            }
            pipeline.addLast("client", responseHandler);
            readyFuture.setSuccess(null);
        }
    }

    ChannelFuture whenReady(Channel channel) {
        return channel.attr(NettyHttpClientPipelineFactory.WHEN_READY).get();
    }

    private SslHandler configureClientSSLOnDemand(Channel channel) throws Exception {
        if (tlsClientParameters != null) {
            final SSLEngine sslEngine;

            if (enableHttp2) {
                final SSLContextInitParameters initParams = SSLUtils.getSSLContextInitParameters(tlsClientParameters);

                sslEngine = SslContextBuilder
                    .forClient()
                    .sslProvider(SslContext.defaultClientProvider())
                    .keyManager(new SimpleKeyManagerFactory() {
                        
                        @Override
                        protected void engineInit(KeyStore keyStore, char[] var2) throws Exception {
                        }
                        
                        @Override
                        protected void engineInit(ManagerFactoryParameters params) throws Exception {
                        }
                        
                        @Override
                        protected KeyManager[] engineGetKeyManagers() {
                            final KeyManager[] keyManagers = initParams.getKeyManagers();
                            if (keyManagers == null) {
                                return new KeyManager[0];
                            }
                            return keyManagers;
                        }
                    })
                    .trustManager(new SimpleTrustManagerFactory() {
                        @Override
                        protected void engineInit(KeyStore keyStore) throws Exception {
                        }

                        @Override
                        protected void engineInit(ManagerFactoryParameters params) throws Exception {
                        }

                        @Override
                        protected TrustManager[] engineGetTrustManagers() {
                            final TrustManager[] trustManagers = initParams.getTrustManagers();
                            if (trustManagers == null) {
                                return new TrustManager[0];
                            }
                            return trustManagers;
                        }
                        
                    })
                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                    .applicationProtocolConfig(
                        new ApplicationProtocolConfig(
                            Protocol.ALPN,
                            // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                            SelectorFailureBehavior.NO_ADVERTISE,
                            // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                            SelectedListenerFailureBehavior.ACCEPT,
                            ApplicationProtocolNames.HTTP_2,
                            ApplicationProtocolNames.HTTP_1_1
                        )
                    )
                    .build()
                    .newEngine(channel.alloc());
            } else {
                sslEngine = SSLUtils.createClientSSLEngine(tlsClientParameters);
            }

            return new SslHandler(sslEngine);
        }
        return null;
    }

    private static class Http2SettingsHandler extends SimpleChannelInboundHandler<Http2Settings> {
        private ChannelPromise promise;

        Http2SettingsHandler(ChannelPromise promise) {
            this.promise = promise;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Http2Settings msg) throws Exception {
            promise.setSuccess();
            ctx.pipeline().remove(this);
        }
    }
}
