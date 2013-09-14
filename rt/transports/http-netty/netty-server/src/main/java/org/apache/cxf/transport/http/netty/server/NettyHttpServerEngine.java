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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.transport.HttpUriMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

public class NettyHttpServerEngine implements ServerEngine {

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
    
    private Map<String, NettyHttpContextHandler> handlerMap = new ConcurrentHashMap<String, NettyHttpContextHandler>();
    
    /**
     * This field holds the TLS ServerParameters that are programatically
     * configured. The tlsServerParamers (due to JAXB) holds the struct
     * placed by SpringConfig.
     */
    private TLSServerParameters tlsServerParameters;
    
    private ThreadingParameters threadingParameters = new ThreadingParameters(); 
    
    private int readIdleTime = 60;
    
    private int writeIdleTime = 30;
    
    private int maxChunkContentSize = 1048576; 
    
    private boolean sessionSupport;
    
    // TODO need to setup configuration about them
    private EventLoopGroup bossGroup = new NioEventLoopGroup();
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    
    public NettyHttpServerEngine() {
        
    }

    public NettyHttpServerEngine(
            String host,
            int port) {
        this.host = host;
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    @PostConstruct
    public void finalizeConfig() {
        // need to check if we need to any other thing other than Setting the TLSServerParameter
    }
    
    
    /**
     * This method is used to programmatically set the TLSServerParameters.
     * This method may only be called by the factory.
     * @throws IOException 
     */
    public void setTlsServerParameters(TLSServerParameters params) {
        tlsServerParameters = params;
    }
    
    /**
     * This method returns the programmatically set TLSServerParameters, not
     * the TLSServerParametersType, which is the JAXB generated type used 
     * in SpringConfiguration.
     * @return
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
          
        final ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_REUSEADDR, true);
                
        // Set up the idle handler
        IdleStateHandler idleStateHandler = 
            new IdleStateHandler(getReadIdleTime(), getWriteIdleTime(), 0);
        // Set up the event pipeline factory.
        servletPipeline = 
            new NettyHttpServletPipelineFactory(
                 tlsServerParameters, sessionSupport, 
                 threadingParameters.getThreadPoolSize(),
                 maxChunkContentSize,
                 handlerMap, idleStateHandler);
        // Start the servletPipeline's timer
        servletPipeline.start();
        bootstrap.childHandler(servletPipeline);
        InetSocketAddress address = null;
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


    @Override
    public void addServant(URL url, NettyHttpHandler handler) {
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
    }

    @Override
    public NettyHttpHandler getServant(URL url) {
        final String contextName = HttpUriMapper.getContextName(url.getPath());
        NettyHttpContextHandler contextHandler = handlerMap.get(contextName);
        if (contextHandler != null) {
            return contextHandler.getNettyHttpHandler(url.getPath());
        } else {
            return null;
        }
    }

    public void shutdown() {
        // just unbind the channel
        if (servletPipeline != null) {
            servletPipeline.shutdown();
        }
        
        if (serverChannel != null) {
            serverChannel.close();
        }
        
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
       
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
}
