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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.transport.http.netty.server.interceptor.NettyInterceptor;
import org.apache.cxf.transport.http.netty.server.servlet.NettyHttpServletRequest;
import org.apache.cxf.transport.http.netty.server.servlet.NettyServletResponse;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.jboss.netty.util.CharsetUtil;


public class NettyHttpServletHandler extends IdleStateAwareChannelHandler {
    private static final Logger LOG =
            LogUtils.getL7dLogger(NettyHttpServletHandler.class);
    private final ChannelGroup allChannels;

    private final NettyHttpServletPipelineFactory pipelineFactory;

    private List<NettyInterceptor> interceptors;

    //private List<String> contexts = new ArrayList<String>();

    public NettyHttpServletHandler(NettyHttpServletPipelineFactory pipelineFactory) {
        this.allChannels = pipelineFactory.getAllChannels();
        this.pipelineFactory = pipelineFactory;
    }

    public NettyHttpServletHandler addInterceptor(
            NettyInterceptor interceptor) {

        if (this.interceptors == null) {
            this.interceptors = new ArrayList<NettyInterceptor>();
        }
        this.interceptors.add(interceptor);
        return this;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
        throws Exception {
        LOG.log(Level.FINE, "Opening new channel: {}", e.getChannel().getId());
        // Agent map
        allChannels.add(e.getChannel());
    }

    @Override
    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) {
        LOG.log(Level.FINE, "Closing idle channel: {}", e.getChannel().getId());
        e.getChannel().close();
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
        throws Exception {

        HttpRequest request = (HttpRequest) e.getMessage();
        if (HttpHeaders.is100ContinueExpected(request)) {
            e.getChannel().write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
        }

        // find the nettyHttpContextHandler by lookup the request url
        NettyHttpContextHandler nettyHttpContextHandler = pipelineFactory.getNettyHttpHandler(request.getUri());
        if (nettyHttpContextHandler != null) {
            handleHttpServletRequest(ctx, e, nettyHttpContextHandler);
        } else {
            throw new RuntimeException(
                    "No handler found for uri: " + request.getUri());
        }

    }

    protected void handleHttpServletRequest(ChannelHandlerContext ctx,
                                            MessageEvent e, NettyHttpContextHandler nettyHttpContextHandler)
        throws Exception {

        interceptOnRequestReceived(ctx, e);

        HttpRequest request = (HttpRequest) e.getMessage();
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

        NettyServletResponse nettyServletResponse = buildHttpServletResponse(response);
        NettyHttpServletRequest nettyServletRequest = 
            buildHttpServletRequest(request, nettyHttpContextHandler.getContextPath());

        nettyHttpContextHandler.handle(request.getUri(), nettyServletRequest, nettyServletResponse);
        interceptOnRequestSuccessed(ctx, e, response);

        nettyServletResponse.getWriter().flush();

        boolean keepAlive = HttpHeaders.isKeepAlive(request);

        if (keepAlive) {
            // Add 'Content-Length' header only for a keep-alive connection.
            response.setHeader(Names.CONTENT_LENGTH, response.getContent()
                    .readableBytes());
            // Add keep alive header as per:
            // -
            // http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.setHeader(Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        // write response...
        ChannelFuture future = e.getChannel().write(response);

        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        Throwable cause = e.getCause();
        LOG.log(Level.SEVERE, "Unexpected exception from downstream.", cause);

        interceptOnRequestFailed(ctx, e);

        Channel ch = e.getChannel();
        if (cause instanceof IllegalArgumentException) {

            ch.close();

        } else {

            if (cause instanceof TooLongFrameException) {
                sendError(ctx, HttpResponseStatus.BAD_REQUEST);
                return;
            }

            if (ch.isConnected()) {
                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }

        }

    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
        response.setHeader(Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.setContent(ChannelBuffers.copiedBuffer("Failure: "
                + status.toString() + "\r\n", CharsetUtil.UTF_8));
        ctx.getChannel().write(response).addListener(
                ChannelFutureListener.CLOSE);
    }

    private void interceptOnRequestReceived(ChannelHandlerContext ctx,
                                            MessageEvent e) {

        if (this.interceptors != null) {
            for (NettyInterceptor interceptor : this.interceptors) {
                interceptor.onRequestReceived(ctx, e);
            }
        }

    }

    private void interceptOnRequestSuccessed(ChannelHandlerContext ctx,
                                             MessageEvent e, HttpResponse response) {
        if (this.interceptors != null) {
            for (NettyInterceptor interceptor : this.interceptors) {
                interceptor.onRequestSuccessed(ctx, e, response);
            }
        }

    }

    private void interceptOnRequestFailed(ChannelHandlerContext ctx,
                                          ExceptionEvent e) {
        if (this.interceptors != null) {
            for (NettyInterceptor interceptor : this.interceptors) {
                interceptor.onRequestFailed(ctx, e);
            }
        }

    }

    protected NettyServletResponse buildHttpServletResponse(
            HttpResponse response) {
        return new NettyServletResponse(response);
    }

    protected NettyHttpServletRequest buildHttpServletRequest(
            HttpRequest request, String contextPath) {
        return new NettyHttpServletRequest(request, contextPath);
    }
    
}
