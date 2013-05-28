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
import org.apache.cxf.transport.http.netty.server.interceptor.ChannelInterceptor;
import org.apache.cxf.transport.http.netty.server.interceptor.HttpSessionInterceptor;
import org.apache.cxf.transport.http.netty.server.session.DefaultHttpSessionStore;
import org.apache.cxf.transport.http.netty.server.session.HttpSessionStore;
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
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

public class NettyHttpServletPipelineFactory implements ChannelPipelineFactory {

    private final ChannelGroup allChannels = new DefaultChannelGroup();

    private final HttpSessionWatchdog watchdog;

    private final ChannelHandler idleStateHandler;

    private final Timer timer;

    // TODO we may need to configure the thread pool from outside
    private final ExecutionHandler executionHandler =
            new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(200, 2048576, 204857600));

    private final Map<String, NettyHttpContextHandler> handlerMap;

    public NettyHttpServletPipelineFactory(Map<String, NettyHttpContextHandler> handlerMap) {

        this.timer = new HashedWheelTimer();
        this.idleStateHandler = new IdleStateHandler(this.timer, 60, 30, 0);
        this.watchdog = new HttpSessionWatchdog();
        this.handlerMap = handlerMap;
        new Thread(watchdog).start();
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

    public void shutdown() {
        this.watchdog.stopWatching();
        this.timer.stop();
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
        handler.addInterceptor(new HttpSessionInterceptor(getHttpSessionStore()));
        return handler;
    }

    protected ChannelPipeline getDefaulHttpChannelPipeline() {

        // Create a default pipeline implementation.
        ChannelPipeline pipeline = Channels.pipeline();

        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
        pipeline.addLast("encoder", new HttpResponseEncoder());

        // Remove the following line if you don't want automatic content
        // compression.
        pipeline.addLast("deflater", new HttpContentCompressor());
        pipeline.addLast("idle", this.idleStateHandler);

        return pipeline;
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
