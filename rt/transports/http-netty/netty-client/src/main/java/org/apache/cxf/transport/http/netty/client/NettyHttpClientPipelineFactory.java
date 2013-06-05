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
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

public class NettyHttpClientPipelineFactory implements ChannelPipelineFactory {
    
    private static final Logger LOG =
        LogUtils.getL7dLogger(NettyHttpClientPipelineFactory.class);
    
    private final TLSClientParameters tlsClientParameters;
    
    public NettyHttpClientPipelineFactory(TLSClientParameters tlsClientParameters) {
        this.tlsClientParameters = tlsClientParameters;
    }
    
    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();
        
        SslHandler sslHandler = configureClientSSLOnDemand();
        if (sslHandler != null) {
            LOG.log(Level.FINE, 
                    "Server SSL handler configured and added as an interceptor against the ChannelPipeline: {}"
                    , sslHandler);
            pipeline.addLast("ssl", sslHandler);
        }

        pipeline.addLast("decoder", new HttpResponseDecoder());
        pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
        pipeline.addLast("encoder", new HttpRequestEncoder());
        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
        pipeline.addLast("client", new NettyHttpClientHandler());
        return pipeline;

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
