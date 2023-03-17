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

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.transport.HttpUriMapper;
import org.apache.cxf.transport.http.HttpServerEngineSupport;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

public class NettyHttpServerEngine implements ServerEngine, HttpServerEngineSupport {

    private static final Logger LOG =
            LogUtils.getL7dLogger(NettyHttpServerEngine.class);

    /**
     * This is the network port for which this engine is allocated.
     */
    private int port;

    /**
     * This is the network address for which this engine is allocated.
     */
    private String host;

    /**
     * This field holds the protocol for which this engine is
     * enabled, i.e. "http" or "https".
     */
    private String protocol = "http";

    private volatile Channel serverChannel;

    private NettyHttpServletPipelineFactory servletPipeline;

    private Map<String, NettyHttpContextHandler> handlerMap = new ConcurrentHashMap<>();

    /**
     * This field holds the TLS ServerParameters that are programatically
     * configured. The tlsServerParamers (due to JAXB) holds the struct
     * placed by SpringConfig.
     */
    private TLSServerParameters tlsServerParameters;

    private ThreadingParameters threadingParameters = new ThreadingParameters();

    private List<String> registedPaths = new CopyOnWriteArrayList<>();

    // TODO need to setup configuration about them
    private int readIdleTime = 60;

    private int writeIdleTime = 30;

    private int maxChunkContentSize = 1048576;

    private boolean sessionSupport;

    // TODO need to setup configuration about them
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private EventExecutorGroup applicationExecutor;
    
    private Bus bus;

    
    public NettyHttpServerEngine() {

    }

    @Deprecated
    public NettyHttpServerEngine(String host, int port) {
        this(host, port, null);
    }

    public NettyHttpServerEngine(String host, int port, Bus bus) {
        this.host = host;
        this.port = port;
        this.bus = bus;
    }

    @PostConstruct
    public void finalizeConfig() {
        // need to check if we need to any other thing other than Setting the TLSServerParameter
    }
    
    public void setBus(Bus bus) {
        this.bus = bus;
    }
    
    public Bus getBus() {
        return bus;
    }

    /**
     * This method is used to programmatically set the TLSServerParameters.
     * This method may only be called by the factory.
     */
    public void setTlsServerParameters(TLSServerParameters params) {
        tlsServerParameters = params;
        if (tlsServerParameters != null) {
            protocol = "https";
        } else {
            protocol = "http";
        }
    }

    /**
     * This method returns the programmatically set TLSServerParameters, not
     * the TLSServerParametersType, which is the JAXB generated type used
     * in SpringConfiguration.
     */
    public TLSServerParameters getTlsServerParameters() {
        return tlsServerParameters;
    }

    public void setThreadingParameters(ThreadingParameters params) {
        threadingParameters = params;
    }

    public ThreadingParameters getThreadingParameters() {
        return threadingParameters;
    }

    protected Channel startServer() {
        if (bossGroup == null) {
            bossGroup = new NioEventLoopGroup();
        }
        if (workerGroup == null) {
            workerGroup = new NioEventLoopGroup();
        }
        if (applicationExecutor == null) {
            applicationExecutor = new DefaultEventExecutorGroup(threadingParameters.getThreadPoolSize());
        }

        final ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_REUSEADDR, true);

        // Set up the event pipeline factory.
        // Netty has issues with "UPGRADE" requests with payloads (POST/PUT)
        // when not using SSL so we'll only start Http2 if the user specifically configures it
        servletPipeline =
            new NettyHttpServletPipelineFactory(
                 tlsServerParameters, sessionSupport,
                 maxChunkContentSize, handlerMap,
                 this, applicationExecutor, isHttp2Required(bus));
        // Start the servletPipeline's timer
        servletPipeline.start();
        bootstrap.childHandler(servletPipeline);
        InetSocketAddress address;
        if (host == null) {
            address = new InetSocketAddress(port);
        } else {
            address = new InetSocketAddress(host, port);
        }
        // Bind and start to accept incoming connections.
        try {
            return bootstrap.bind(address).sync().channel();
        } catch (InterruptedException ex) {
            // do nothing here
            return null;
        }
    }

    protected void checkRegistedContext(URL url) {
        String path = url.getPath();
        for (String registedPath : registedPaths) {
            if (path.equals(registedPath)) {
                // Throw the address is already used exception
                throw new Fault(new Message("ADD_HANDLER_CONTEXT_IS_USED_MSG", LOG, url, registedPath));
            }
        }
    }

    @Override
    public void addServant(URL url, NettyHttpHandler handler) {
        checkRegistedContext(url);

        if (serverChannel == null) {
            serverChannel = startServer();
        }
        // need to set the handler name for looking up
        handler.setName(url.getPath());
        String contextName = HttpUriMapper.getContextName(url.getPath());
        // need to check if the NettyContext is there
        NettyHttpContextHandler contextHandler = handlerMap.get(contextName);
        if (contextHandler == null) {
            contextHandler = new NettyHttpContextHandler(contextName);
            handlerMap.put(contextName, contextHandler);
        }
        contextHandler.addNettyHttpHandler(handler);
        registedPaths.add(url.getPath());
    }

    @Override
    public void removeServant(URL url) {
        final String contextName = HttpUriMapper.getContextName(url.getPath());
        NettyHttpContextHandler contextHandler = handlerMap.get(contextName);
        if (contextHandler != null) {
            contextHandler.removeNettyHttpHandler(url.getPath());
            if (contextHandler.isEmpty()) {
                // remove the contextHandler from handlerMap
                handlerMap.remove(contextName);
            }
        }
        registedPaths.remove(url.getPath());
    }

    @Override
    public NettyHttpHandler getServant(URL url) {
        final String contextName = HttpUriMapper.getContextName(url.getPath());
        NettyHttpContextHandler contextHandler = handlerMap.get(contextName);
        if (contextHandler != null) {
            return contextHandler.getNettyHttpHandler(url.getPath());
        }
        return null;
    }

    public void shutdown() {
        // clean up the handler maps
        handlerMap.clear();
        registedPaths.clear();

        // just unbind the channel
        if (servletPipeline != null) {
            servletPipeline.shutdown();
        } else if (applicationExecutor != null) {
            // shutdown executor if it set but not server started
            applicationExecutor.shutdownGracefully();
        }

        if (serverChannel != null) {
            serverChannel.close();
        }

        // shutdown executors
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

    public int getReadIdleTime() {
        return readIdleTime;
    }

    public void setReadIdleTime(int readIdleTime) {
        this.readIdleTime = readIdleTime;
    }

    public int getWriteIdleTime() {
        return writeIdleTime;
    }

    public void setWriteIdleTime(int writeIdleTime) {
        this.writeIdleTime = writeIdleTime;
    }

    public boolean isSessionSupport() {
        return sessionSupport;
    }

    public void setSessionSupport(boolean session) {
        this.sessionSupport = session;
    }

    public int getMaxChunkContentSize() {
        return maxChunkContentSize;
    }

    public void setMaxChunkContentSize(int maxChunkContentSize) {
        this.maxChunkContentSize = maxChunkContentSize;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setBossGroup(EventLoopGroup bossGroup) {
        if (this.bossGroup == null) {
            this.bossGroup = bossGroup;
        } else {
            throw new IllegalStateException("bossGroup is already defined");
        }
    }

    public EventLoopGroup getBossGroup() {
        return bossGroup;
    }

    public void setWorkerGroup(EventLoopGroup workerGroup) {
        if (this.workerGroup == null) {
            this.workerGroup = workerGroup;
        } else {
            throw new IllegalStateException("workerGroup is already defined");
        }
    }

    public EventLoopGroup getWorkerGroup() {
        return workerGroup;
    }

    public EventExecutorGroup getApplicationExecutor() {
        return applicationExecutor;
    }

    public void setApplicationExecutor(EventExecutorGroup applicationExecutor) {
        if (this.applicationExecutor == null) {
            this.applicationExecutor = applicationExecutor;
        } else {
            throw new IllegalStateException("applicationExecutor is already defined");
        }
    }
}
