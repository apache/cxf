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


import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NettyHttpServerEngineTest {
    private static final int PORT1
        = Integer.valueOf(TestUtil.getPortNumber(NettyHttpServerEngineTest.class, 1));
    private static final int PORT2
        = Integer.valueOf(TestUtil.getPortNumber(NettyHttpServerEngineTest.class, 2));
    private static final int PORT3
        = Integer.valueOf(TestUtil.getPortNumber(NettyHttpServerEngineTest.class, 3));


    private Bus bus;
    private NettyHttpServerEngineFactory factory;

    @Before
    public void setUp() throws Exception {
        bus = mock(Bus.class);

        Configurer configurer = mock(Configurer.class);
        when(bus.getExtension(Configurer.class)).thenReturn(configurer);

        factory = new NettyHttpServerEngineFactory();
        factory.setBus(bus);

    }

    @Test
    public void testEngineRetrieval() throws Exception {
        NettyHttpServerEngine engine =
            factory.createNettyHttpServerEngine(PORT1, "http");

        assertTrue(
            "Engine references for the same port should point to the same instance",
            engine == factory.retrieveNettyHttpServerEngine(PORT1));

        NettyHttpServerEngineFactory.destroyForPort(PORT1);
    }

    @Test
    public void testaddServants() throws Exception {
        String urlStr = "http://localhost:" + PORT1 + "/hello/test";
        String urlStr2 = "http://localhost:" + PORT1 + "/hello233/test";
        NettyHttpServerEngine engine =
            factory.createNettyHttpServerEngine(PORT1, "http");

        NettyHttpTestHandler handler1 = new NettyHttpTestHandler("string1", true);
        NettyHttpTestHandler handler2 = new NettyHttpTestHandler("string2", true);
        engine.addServant(new URL(urlStr), handler1);
        //assertEquals("Get the wrong maxIdleTime.", 30000, engine.getConnector().getMaxIdleTime());

        String response = getResponse(urlStr);
        assertEquals("The netty http handler did not take effect", response, "string1");

        try {
            engine.addServant(new URL(urlStr), handler2);
            fail("We don't support to publish the two service at the same context path");
        } catch (Exception ex) {
            assertTrue("Get a wrong exception message", ex.getMessage().indexOf("hello/test") > 0);
        }

        engine.addServant(new URL(urlStr2), handler2);

        engine.removeServant(new URL(urlStr));
        response = getResponse(urlStr2);
        assertEquals("The netty http handler did not take effect", response, "string2");
        engine.shutdown();
        // set the get request
        NettyHttpServerEngineFactory.destroyForPort(PORT1);

    }

    @Test
    public void testNettyHttpHandler() throws Exception {
        String urlStr1 = "http://localhost:" + PORT3 + "/hello/test";
        String urlStr2 = "http://localhost:" + PORT3 + "/hello/test2";
        NettyHttpServerEngine engine =
            factory.createNettyHttpServerEngine(PORT3, "http");

        NettyHttpTestHandler handler1 = new NettyHttpTestHandler("test", false);
        NettyHttpTestHandler handler2 = new NettyHttpTestHandler("test2", false);
        engine.addServant(new URL(urlStr1), handler1);
        engine.addServant(new URL(urlStr2), handler2);


        String response = null;
        try {
            response = getResponse(urlStr1 + "/test");
        } catch (Exception ex) {
            fail("Can't get the reponse from the server " + ex);
        }
        assertEquals("the netty http handler did not take effect", response, "test");

        try {
            response = getResponse(urlStr2 + "/test");
        } catch (Exception ex) {
            fail("Can't get the reponse from the server " + ex);
        }
        assertEquals("the netty http handler did not take effect", response, "test2");

        NettyHttpServerEngineFactory.destroyForPort(PORT3);
    }

    @Test
    public void testHttps() throws Exception {
        Map<String, TLSServerParameters> tlsParamsMap = new HashMap<>();
        tlsParamsMap.put(Integer.toString(PORT2), new TLSServerParameters());
        factory.setTlsServerParameters(tlsParamsMap);

        factory.createNettyHttpServerEngine(PORT2, "https");

        NettyHttpServerEngineFactory.destroyForPort(PORT2);
    }

    private static String getResponse(String target) throws Exception {
        URL url = new URL(target);

        URLConnection connection = url.openConnection();

        assertTrue(connection instanceof HttpURLConnection);
        connection.connect();
        try (InputStream in = connection.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            IOUtils.copy(in, buffer);
            return buffer.toString();
        }
    }
}