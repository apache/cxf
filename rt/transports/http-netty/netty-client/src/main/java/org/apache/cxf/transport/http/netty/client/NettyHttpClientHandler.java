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

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.timeout.ReadTimeoutException;

public class NettyHttpClientHandler extends ChannelDuplexHandler {
    private final BlockingQueue<NettyHttpClientRequest> sendedQueue =
        new LinkedBlockingDeque<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpObject) {
            if (msg instanceof HttpResponse) {
                // just make sure we can combine the request and response together
                HttpResponse response = (HttpResponse)msg;
                NettyHttpClientRequest request = sendedQueue.poll();
                request.setResponse(response);
                // calling the callback here
                request.getCxfResponseCallback().responseReceived(response);
            }
            
            if (msg instanceof LastHttpContent) {
                ctx.close();
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // need to deal with the request
        if (msg instanceof NettyHttpClientRequest) {
            NettyHttpClientRequest request = (NettyHttpClientRequest)msg;
            sendedQueue.put(request);
            ctx.writeAndFlush(request.getRequest(), promise);
        } else {
            super.write(ctx, msg, promise);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof ReadTimeoutException) {
            final NettyHttpClientRequest request = sendedQueue.poll();
            request.getCxfResponseCallback().error(new IOException(cause));
        } else {
            cause.printStackTrace();
            ctx.close();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
