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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.apache.cxf.configuration.jsse.TLSServerParameters;

import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.ImmediateEventExecutor;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NettyHttpServletPipelineFactoryTest {
    @Test
    public void testHttp2UsesIncludedProtocols() throws Exception {
        TLSServerParameters parameters = serverParameters();
        parameters.setIncludeProtocols(Collections.singletonList("TLSv1.2"));

        SslContext context = createHttp2Context(parameters);

        assertArrayEquals(new String[] {"TLSv1.2"}, context.newEngine(
            UnpooledByteBufAllocator.DEFAULT).getEnabledProtocols());
    }

    @Test
    public void testHttp2UsesExcludedProtocols() throws Exception {
        TLSServerParameters parameters = serverParameters();
        parameters.setExcludeProtocols(Collections.singletonList("TLSv1.2"));

        SslContext context = createHttp2Context(parameters);

        assertFalse(Arrays.asList(context.newEngine(
            UnpooledByteBufAllocator.DEFAULT).getEnabledProtocols()).contains("TLSv1.2"));
    }

    @Test
    public void testHttp2PreservesUnconstrainedDefaults() throws Exception {
        TLSServerParameters parameters = serverParameters();

        SslContext context = createHttp2Context(parameters);

        assertArrayEquals(SSLContext.getDefault().getDefaultSSLParameters().getProtocols(),
                  context.newEngine(UnpooledByteBufAllocator.DEFAULT).getEnabledProtocols());
    }

    @Test
    public void testHttp2RejectsUnsatisfiableProtocols() throws Exception {
        TLSServerParameters parameters = serverParameters();
        parameters.setIncludeProtocols(Collections.singletonList("NoSuchProtocol"));

        try {
            createHttp2Context(parameters);
            fail("Expected protocol configuration to be rejected");
        } catch (InvocationTargetException ex) {
            assertTrue(ex.getCause() instanceof java.security.GeneralSecurityException);
        }
    }

    private static TLSServerParameters serverParameters() throws Exception {
        TLSServerParameters parameters = new TLSServerParameters();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(NettyHttpServletPipelineFactoryTest.class.getResourceAsStream("/keys/clientstore.jks"),
                      "cspass".toCharArray());
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "cspass".toCharArray());
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
        parameters.setKeyManagers(keyManagers);
        return parameters;
    }

    private static SslContext createHttp2Context(TLSServerParameters parameters) throws Exception {
        NettyHttpServletPipelineFactory factory = new NettyHttpServletPipelineFactory(
            parameters, false, 1024, new HashMap<>(), new NettyHttpServerEngine(),
            ImmediateEventExecutor.INSTANCE, true);
        Method method = NettyHttpServletPipelineFactory.class
            .getDeclaredMethod("configureServerHttp2SSLOnDemand");
        method.setAccessible(true);
        return (SslContext) method.invoke(factory);
    }
}
