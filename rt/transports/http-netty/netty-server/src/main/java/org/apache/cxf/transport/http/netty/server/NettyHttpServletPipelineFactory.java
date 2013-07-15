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

import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngine;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.transport.http.netty.server.interceptor.ChannelInterceptor;
import org.apache.cxf.transport.http.netty.server.interceptor.HttpSessionInterceptor;
import org.apache.cxf.transport.http.netty.server.session.DefaultHttpSessionStore;
import org.apache.cxf.transport.http.netty.server.session.HttpSessionStore;
import org.apache.cxf.transport.https.SSLUtils;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;

public class NettyHttpServletPipelineFactory implements ChannelPipelineFactory {
    private static final Logger LOG =
        LogUtils.getL7dLogger(NettyHttpServletPipelineFactory.class);
    
    private final ChannelGroup allChannels = new DefaultChannelGroup();

    private final HttpSessionWatchdog watchdog;

    private final ChannelHandler idleStateHandler;
    
    private final TLSServerParameters tlsServerParameters;
    
    private final boolean supportSession;
    
    private final ExecutionHandler executionHandler;

    private final Map<String, NettyHttpContextHandler> handlerMap;
    
    private final int maxChunkContentSize;

    public NettyHttpServletPipelineFactory(TLSServerParameters tlsServerParameters, 
                                           boolean supportSession, int threadPoolSize, int maxChunkContentSize,
                                           Map<String, NettyHttpContextHandler> handlerMap,
                                           IdleStateHandler idleStateHandler) {
        this.supportSession = supportSession;
        this.idleStateHandler = idleStateHandler;
        this.watchdog = new HttpSessionWatchdog();
        this.handlerMap = handlerMap;
        this.tlsServerParameters = tlsServerParameters;
        this.maxChunkContentSize = maxChunkContentSize;
        // TODO need to check the if we need pass other setting
        this.executionHandler = 
            new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(threadPoolSize, 2048576, 204857600));
    }


    public Map<String, NettyHttpContextHandler> getHttpContextHandlerMap() {
        return handlerMap;
    }

    public ChannelGroup getAllChannels() {
        return allChannels;
    }

    public NettyHttpContextHandler getNettyHttpHandler(String url) {
        Set<String> keySet = handlerMap.keySet();
        for (String key : keySet) {
            // Here just check the context path first
            if (url.startsWith(key)) {
                return handlerMap.get(key);
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
        this.watchdog.stopWatching();
        this.allChannels.close().awaitUninterruptibly();
    }

    @Override
    public final ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = this.getDefaulHttpChannelPipeline();
        // need to add another executor handler for invocation the service
        pipeline.addLast("executionHandler", executionHandler);
        pipeline.addLast("handler", this.getServletHandler());
        return pipeline;
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

    protected ChannelPipeline getDefaulHttpChannelPipeline() throws Exception {

        // Create a default pipeline implementation.
        ChannelPipeline pipeline = Channels.pipeline();
        
        SslHandler sslHandler = configureServerSSLOnDemand();
        if (sslHandler != null) {
            LOG.log(Level.FINE, 
                    "Server SSL handler configured and added as an interceptor against the ChannelPipeline: {}"
                    , sslHandler);
            pipeline.addLast("ssl", sslHandler);
        }

        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("aggregator", new HttpChunkAggregator(maxChunkContentSize));
        pipeline.addLast("encoder", new HttpResponseEncoder());

        // Remove the following line if you don't want automatic content
        // compression.
        pipeline.addLast("deflater", new HttpContentCompressor());
        pipeline.addLast("idle", this.idleStateHandler);

        return pipeline;
    }

    private SslHandler configureServerSSLOnDemand() throws Exception {
        if (tlsServerParameters != null) {
            SSLEngine sslEngine = SSLUtils.createServerSSLEngine(tlsServerParameters);
            return new SslHandler(sslEngine);
        } else {
            return null;
        }
    }

    private class HttpSessionWatchdog implements Runnable {

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

}
