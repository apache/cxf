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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngine;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.transport.https.SSLUtils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;


public class NettyHttpClientPipelineFactory extends ChannelInitializer<Channel> {
    
    private static final Logger LOG =
        LogUtils.getL7dLogger(NettyHttpClientPipelineFactory.class);
    private final TLSClientParameters tlsClientParameters;
    
    public NettyHttpClientPipelineFactory(TLSClientParameters clientParameters) {
        this.tlsClientParameters = clientParameters;
    }
    
    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        
        SslHandler sslHandler = configureClientSSLOnDemand();
        if (sslHandler != null) {
            LOG.log(Level.FINE, 
                    "Server SSL handler configured and added as an interceptor against the ChannelPipeline: {}",
                    sslHandler);
            pipeline.addLast("ssl", sslHandler);
        }

        
        pipeline.addLast("decoder", new HttpResponseDecoder());
        // TODO need to configure the aggregator size
        pipeline.addLast("aggregator", new HttpObjectAggregator(1048576));
        pipeline.addLast("encoder", new HttpRequestEncoder());
        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
        pipeline.addLast("client", new NettyHttpClientHandler());
    }
    
    private SslHandler configureClientSSLOnDemand() throws Exception {
        if (tlsClientParameters != null) {
            SSLEngine sslEngine = SSLUtils.createClientSSLEngine(tlsClientParameters);
            return new SslHandler(sslEngine);
        } else {
            return null;
        }
    }
    
    
}
