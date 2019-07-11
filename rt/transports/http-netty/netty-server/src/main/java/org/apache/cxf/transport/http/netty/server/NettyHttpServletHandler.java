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

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.transport.http.netty.server.interceptor.NettyInterceptor;
import org.apache.cxf.transport.http.netty.server.servlet.NettyHttpServletRequest;
import org.apache.cxf.transport.http.netty.server.servlet.NettyServletResponse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;

public class NettyHttpServletHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG =
            LogUtils.getL7dLogger(NettyHttpServletHandler.class);

    private final ChannelGroup allChannels;

    private final NettyHttpServletPipelineFactory pipelineFactory;

    private List<NettyInterceptor> interceptors;

    public NettyHttpServletHandler(NettyHttpServletPipelineFactory pipelineFactory) {
        this.allChannels = pipelineFactory.getAllChannels();
        this.pipelineFactory = pipelineFactory;
    }

    public NettyHttpServletHandler addInterceptor(
            NettyInterceptor interceptor) {

        if (this.interceptors == null) {
            this.interceptors = new ArrayList<>();
        }
        this.interceptors.add(interceptor);
        return this;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOG.log(Level.FINE, "Opening new channel: {}", ctx.channel());
        // Agent map
        allChannels.add(ctx.channel());
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE || e.state() == IdleState.WRITER_IDLE) {
                LOG.log(Level.FINE, "Closing idle channel: {}", e.state());
                ctx.close();
            }
        }
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        HttpRequest request = (HttpRequest) msg;
        if (HttpUtil.is100ContinueExpected(request)) {
            ctx.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
        }

        // find the nettyHttpContextHandler by lookup the request url
        NettyHttpContextHandler nettyHttpContextHandler = pipelineFactory.getNettyHttpHandler(request.uri());
        if (nettyHttpContextHandler != null) {
            handleHttpServletRequest(ctx, request, nettyHttpContextHandler);
        } else {
            throw new RuntimeException(
                    new Fault(new Message("NO_NETTY_SERVLET_HANDLER_FOUND", LOG, request.uri())));
        }
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    protected void handleHttpServletRequest(ChannelHandlerContext ctx,
                                            HttpRequest request, NettyHttpContextHandler nettyHttpContextHandler)
        throws Exception {

        interceptOnRequestReceived(ctx, request);

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

        NettyServletResponse nettyServletResponse = buildHttpServletResponse(response);
        NettyHttpServletRequest nettyServletRequest =
            buildHttpServletRequest(request, nettyHttpContextHandler.getContextPath(), ctx);

        nettyHttpContextHandler.handle(nettyServletRequest.getRequestURI(), nettyServletRequest, nettyServletResponse);
        interceptOnRequestSuccessed(ctx, response);

        nettyServletResponse.getWriter().flush();

        boolean keepAlive = HttpUtil.isKeepAlive(request);

        if (keepAlive) {
            // Add 'Content-Length' header only for a keep-alive connection.
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            // Add keep alive header as per:
            // -
            // http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        // write response...
        ChannelFuture future = ctx.write(response);

        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        LOG.log(Level.SEVERE, "UNEXPECTED_EXCEPCTION_IN_NETTY_SERVLET_HANDLER", cause);

        interceptOnRequestFailed(ctx, cause);

        Channel ch = ctx.channel();
        if (cause instanceof IllegalArgumentException) {

            ch.close();

        } else {

            if (cause instanceof TooLongFrameException) {
                sendError(ctx, HttpResponseStatus.BAD_REQUEST);
                return;
            }

            if (ch.isActive()) {
                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }

        }
        ctx.close();
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        ByteBuf content = Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                                                                status,
                                                                content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        ctx.write(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void interceptOnRequestReceived(ChannelHandlerContext ctx, HttpRequest request) {

        if (this.interceptors != null) {
            for (NettyInterceptor interceptor : this.interceptors) {
                interceptor.onRequestReceived(ctx, request);
            }
        }

    }

    private void interceptOnRequestSuccessed(ChannelHandlerContext ctx,
                                             HttpResponse response) {
        if (this.interceptors != null) {
            for (NettyInterceptor interceptor : this.interceptors) {
                interceptor.onRequestSuccessed(ctx, response);
            }
        }

    }

    private void interceptOnRequestFailed(ChannelHandlerContext ctx,
                                          Throwable e) {
        if (this.interceptors != null) {
            for (NettyInterceptor interceptor : this.interceptors) {
                interceptor.onRequestFailed(ctx, e);
            }
        }

    }

    protected NettyServletResponse buildHttpServletResponse(
            HttpResponse response) {
        // need to access the
        return new NettyServletResponse(response);
    }

    protected NettyHttpServletRequest buildHttpServletRequest(
            HttpRequest request, String contextPath, ChannelHandlerContext ctx) {
        return new NettyHttpServletRequest(request, contextPath, ctx);
    }

}
