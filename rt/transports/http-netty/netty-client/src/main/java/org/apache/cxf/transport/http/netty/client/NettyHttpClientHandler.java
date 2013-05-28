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


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.HttpResponse;

public class NettyHttpClientHandler extends SimpleChannelHandler {

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
        throws Exception {
        HttpResponse response = (HttpResponse)e.getMessage();
        Channel ch = ctx.getChannel();
        BlockingQueue<NettyHttpClientRequest> sendedQueue = (BlockingQueue<NettyHttpClientRequest>)ch.getAttachment();
        NettyHttpClientRequest request = sendedQueue.poll();
        request.setResponse(response);
        // calling the callback here
        request.getCxfResponseCallback().responseReceived(response);
    }


    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
        throws Exception {
        Object p = e.getMessage();
        // need to deal with the request
        if (p instanceof NettyHttpClientRequest) {
            NettyHttpClientRequest request = (NettyHttpClientRequest)e.getMessage();
            Channel ch = ctx.getChannel();
            BlockingQueue<NettyHttpClientRequest> sendedQueue = 
                (BlockingQueue<NettyHttpClientRequest>)ch.getAttachment();
            if (sendedQueue == null) {
                sendedQueue = new LinkedBlockingDeque<NettyHttpClientRequest>();
                ch.setAttachment(sendedQueue);
            }
            sendedQueue.put(request);
            ctx.getChannel().write(request.getRequest());
        } else {
            super.writeRequested(ctx, e);
        }
    }

}
