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

import org.apache.cxf.configuration.jsse.TLSClientParameters;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.SslHandler;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NettyHttpClientPipelineFactoryTest {

    @Test
    public void testHttp11SslEngineBoundToPeer() {
        assertSslEngineBoundToPeer(false);
    }

    @Test
    public void testHttp2SslEngineBoundToPeer() {
        assertSslEngineBoundToPeer(true);
    }

    private void assertSslEngineBoundToPeer(boolean enableHttp2) {
        NettyHttpClientPipelineFactory factory = new NettyHttpClientPipelineFactory(
            new TLSClientParameters(), 0, NettyHttpConduit.DEFAULT_MAX_RESPONSE_CONTENT_LENGTH,
            enableHttp2, "api.example.com", 8443);
        EmbeddedChannel channel = new EmbeddedChannel(factory);
        try {
            SslHandler sslHandler = channel.pipeline().get(SslHandler.class);
            assertNotNull(sslHandler);
            assertEquals("api.example.com", sslHandler.engine().getPeerHost());
            assertEquals(8443, sslHandler.engine().getPeerPort());
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}