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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class NettyHttpServletPipelineFactory extends ChannelInitializer<Channel> {
    private static final Logger LOG =
        LogUtils.getL7dLogger(NettyHttpServletPipelineFactory.class);
    
    //TODO how to manage the allChannels
    private final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);;

    private final HttpSessionWatchdog watchdog;

    private final ChannelHandler idleStateHandler;
    
    private final TLSServerParameters tlsServerParameters;
    
    private final boolean supportSession;
    
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
        allChannels.close();
        watchdog.stopWatching();
        
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

    protected ChannelPipeline getDefaulHttpChannelPipeline(Channel channel) throws Exception {

        // Create a default pipeline implementation.
        ChannelPipeline pipeline = channel.pipeline();
        
        SslHandler sslHandler = configureServerSSLOnDemand();
        if (sslHandler != null) {
            LOG.log(Level.FINE, 
                    "Server SSL handler configured and added as an interceptor against the ChannelPipeline: {}"
                    , sslHandler);
            pipeline.addLast("ssl", sslHandler);
        }

        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("aggregator", new HttpObjectAggregator(maxChunkContentSize));
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

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = getDefaulHttpChannelPipeline(ch);
        //TODO need to configure the thread size of EventExecutorGroup
        EventExecutorGroup e1 = new DefaultEventExecutorGroup(16);
        pipeline.addLast(e1, "handler", this.getServletHandler());
    }

}
