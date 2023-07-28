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

package org.apache.cxf.transport.http_undertow;



import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.ObjectName;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.spring.ConfigurerImpl;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UndertowHTTPServerEngineTest {
    private static final int PORT1
        = Integer.valueOf(TestUtil.getPortNumber(UndertowHTTPServerEngineTest.class, 1));
    private static final int PORT2
        = Integer.valueOf(TestUtil.getPortNumber(UndertowHTTPServerEngineTest.class, 2));
    private static final int PORT3
        = Integer.valueOf(TestUtil.getPortNumber(UndertowHTTPServerEngineTest.class, 3));


    private Bus bus;
    private UndertowHTTPServerEngineFactory factory;

    @Before
    public void setUp() throws Exception {
        bus = mock(Bus.class);

        Configurer configurer = new ConfigurerImpl();
        when(bus.getExtension(Configurer.class)).thenReturn(configurer);

        InstrumentationManager iManager = mock(InstrumentationManager.class);
        when(iManager.getMBeanServer()).thenReturn(ManagementFactory.getPlatformMBeanServer());

        when(bus.getExtension(InstrumentationManager.class)).thenReturn(iManager);

        factory = new UndertowHTTPServerEngineFactory();
        factory.setBus(bus);

    }



    @Test
    public void testEngineRetrieval() throws Exception {
        UndertowHTTPServerEngine engine =
            factory.createUndertowHTTPServerEngine(PORT1, "http");

        assertTrue(
            "Engine references for the same port should point to the same instance",
            engine == factory.retrieveUndertowHTTPServerEngine(PORT1));

        UndertowHTTPServerEngineFactory.destroyForPort(PORT1);
    }

    @Test
    public void testHttpAndHttps() throws Exception {
        UndertowHTTPServerEngine engine =
            factory.createUndertowHTTPServerEngine(PORT1, "http");

        assertTrue("Protocol must be http",
                "http".equals(engine.getProtocol()));

        engine = new UndertowHTTPServerEngine();
        engine.setPort(PORT2);
        engine.setMaxIdleTime(30000);
        engine.setTlsServerParameters(new TLSServerParameters());
        engine.finalizeConfig();

        List<UndertowHTTPServerEngine> list = new ArrayList<>();
        list.add(engine);
        factory.setEnginesList(list);
        engine = factory.createUndertowHTTPServerEngine(PORT2, "https");
        UndertowHTTPTestHandler handler1 = new UndertowHTTPTestHandler("string1", true);

        engine.addServant(new URL("https://localhost:" + PORT2 + "/test"), handler1);
        assertTrue("Protocol must be https",
                "https".equals(engine.getProtocol()));

        assertEquals("Get the wrong maxIdleTime.", 30000, engine.getMaxIdleTime());

        factory.setTLSServerParametersForPort(PORT1, new TLSServerParameters());
        engine = factory.createUndertowHTTPServerEngine(PORT1, "https");
        assertTrue("Protocol must be https",
                   "https".equals(engine.getProtocol()));

        factory.setTLSServerParametersForPort(PORT3, new TLSServerParameters());
        engine = factory.createUndertowHTTPServerEngine(PORT3, "https");
        assertTrue("Protocol must be https",
                   "https".equals(engine.getProtocol()));

        UndertowHTTPServerEngineFactory.destroyForPort(PORT1);
        UndertowHTTPServerEngineFactory.destroyForPort(PORT2);
        UndertowHTTPServerEngineFactory.destroyForPort(PORT3);
    }

    @Test
    public void testaddServants() throws Exception {
        String urlStr = "http://localhost:" + PORT1 + "/hello/test";
        String urlStr2 = "http://localhost:" + PORT1 + "/hello233/test";
        UndertowHTTPServerEngine engine =
            factory.createUndertowHTTPServerEngine(PORT1, "http");
        engine.setMaxIdleTime(30000);
        engine.addServant(new URL(urlStr), new UndertowHTTPTestHandler("string1", true));
        assertEquals("Get the wrong maxIdleTime.", 30000, engine.getMaxIdleTime());

        String response = getResponse(urlStr);
        assertEquals("The undertow http handler did not take effect", response, "string1");

        try {
            engine.addServant(new URL(urlStr), new UndertowHTTPTestHandler("string2", true));
            fail("We don't support to publish the two service at the same context path");
        } catch (Exception ex) {
            assertTrue("Get a wrong exception message", ex.getMessage().indexOf("hello/test") > 0);
        }

        try {
            engine.addServant(new URL(urlStr + "/test"), new UndertowHTTPTestHandler("string2", true));
            fail("We don't support to publish the two service at the same context path");
        } catch (Exception ex) {
            assertTrue("Get a wrong exception message", ex.getMessage().indexOf("hello/test/test") > 0);
        }

        try {
            engine.addServant(new URL("http://localhost:" + PORT1 + "/hello"),
                              new UndertowHTTPTestHandler("string2", true));
            fail("We don't support to publish the two service at the same context path");
        } catch (Exception ex) {
            assertTrue("Get a wrong exception message", ex.getMessage().indexOf("hello") > 0);
        }

        // check if the system property change could work
        System.setProperty("org.apache.cxf.transports.http_undertow.DontCheckUrl", "true");
        engine.addServant(new URL(urlStr + "/test"), new UndertowHTTPTestHandler("string2", true));
        // clean up the System property setting
        System.clearProperty("org.apache.cxf.transports.http_undertow.DontCheckUrl");

        engine.addServant(new URL(urlStr2), new UndertowHTTPTestHandler("string2", true));

        Set<ObjectName>  s = CastUtils.cast(ManagementFactory.getPlatformMBeanServer().
            queryNames(new ObjectName("org.xnio:type=Xnio,provider=\"nio\""), null));
        assertEquals("Could not find Undertow Server: " + s, 1, s.size());

        engine.removeServant(new URL(urlStr));
        engine.shutdown();
        response = getResponse(urlStr2);
        assertEquals("The undertow http handler did not take effect", response, "string2");
        // set the get request
        UndertowHTTPServerEngineFactory.destroyForPort(PORT1);

    }

    /**
     * Test that multiple UndertowHTTPServerEngine instances can be used simultaneously
     * without having name collisions.
     */
    @Test
    public void testJmxSupport() throws Exception {
        String urlStr = "http://localhost:" + PORT1 + "/hello/test";
        String urlStr2 = "http://localhost:" + PORT2 + "/hello/test";
        UndertowHTTPServerEngine engine =
            factory.createUndertowHTTPServerEngine(PORT1, "http");
        UndertowHTTPServerEngine engine2 =
            factory.createUndertowHTTPServerEngine(PORT2, "http");
        UndertowHTTPTestHandler handler1 = new UndertowHTTPTestHandler("string1", true);
        UndertowHTTPTestHandler handler2 = new UndertowHTTPTestHandler("string2", true);

        engine.addServant(new URL(urlStr), handler1);

        Set<ObjectName>  s = CastUtils.cast(ManagementFactory.getPlatformMBeanServer().
            queryNames(new ObjectName("org.xnio:type=Xnio,provider=\"nio\""), null));
        assertEquals("Could not find 1 Undertow Server: " + s, 1, s.size());

        engine2.addServant(new URL(urlStr2), handler2);

        s = CastUtils.cast(ManagementFactory.getPlatformMBeanServer().
            queryNames(new ObjectName("org.xnio:type=Xnio,provider=\"nio\",worker=\"*\""), null));
        assertEquals("Could not find 2 Undertow Server: " + s, 2, s.size());

        engine.removeServant(new URL(urlStr));
        engine2.removeServant(new URL(urlStr2));


        engine.shutdown();

        s = CastUtils.cast(ManagementFactory.getPlatformMBeanServer().
            queryNames(new ObjectName("org.xnio:type=Xnio,provider=\"nio\",worker=\"*\""), null));
        assertEquals("Could not find 2 Undertow Server: " + s, 1, s.size());

        engine2.shutdown();

        s = CastUtils.cast(ManagementFactory.getPlatformMBeanServer().
            queryNames(new ObjectName("org.xnio:type=Xnio,provider=\"nio\",worker=\"*\""), null));
        assertEquals("Could not find 0 Undertow Server: " + s, 0, s.size());

        UndertowHTTPServerEngineFactory.destroyForPort(PORT1);
        UndertowHTTPServerEngineFactory.destroyForPort(PORT2);
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
