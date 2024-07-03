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

package org.apache.cxf.transport.http_jetty;



import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHandler;
import org.eclipse.jetty.ee10.servlet.ServletHandler.MappedServlet;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.http.pathmap.MatchedResource;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JettyHTTPServerEngineTest {
    private static final int PORT1
        = Integer.valueOf(TestUtil.getPortNumber(JettyHTTPServerEngineTest.class, 1));
    private static final int PORT2
        = Integer.valueOf(TestUtil.getPortNumber(JettyHTTPServerEngineTest.class, 2));
    private static final int PORT3
        = Integer.valueOf(TestUtil.getPortNumber(JettyHTTPServerEngineTest.class, 3));
    

    private Bus bus;
    private JettyHTTPServerEngineFactory factory;

    @Before
    public void setUp() throws Exception {
        bus = mock(Bus.class);

        Configurer configurer = new ConfigurerImpl();
        when(bus.getExtension(Configurer.class)).thenReturn(configurer);

        InstrumentationManager iManager = mock(InstrumentationManager.class);
        when(iManager.getMBeanServer()).thenReturn(ManagementFactory.getPlatformMBeanServer());

        when(bus.getExtension(InstrumentationManager.class)).thenReturn(iManager);

        factory = new JettyHTTPServerEngineFactory();
        factory.setBus(bus);

    }

    /**
     * Check that names of threads serving requests for instances of JettyHTTPServerEngine
     * can be set with user specified name.
     */
    @Test
    public void testSettingThreadNames() throws Exception {
        // User specific thread name prefix 1
        String threadNamePrefix1 = "TestPrefix";
        JettyHTTPServerEngine engine = factory.createJettyHTTPServerEngine(PORT1, "http");
        ThreadingParameters parameters = new ThreadingParameters();
        parameters.setThreadNamePrefix(threadNamePrefix1);
        engine.setThreadingParameters(parameters);
        engine.finalizeConfig();
        JettyHTTPTestHandler handler = new JettyHTTPTestHandler("string1", true);
        engine.addServant(new URL("https://localhost:" + PORT1 + "/test"), handler);
        assertTrue("No threads whose name is started with " + threadNamePrefix1,
                checkForExistenceOfThreads(threadNamePrefix1));

        // Default thread name prefix
        engine = factory.createJettyHTTPServerEngine(PORT3, "http");
        engine.finalizeConfig();
        handler = new JettyHTTPTestHandler("string3", true);
        engine.addServant(new URL("https://localhost:" + PORT3 + "/test"), handler);
        ThreadPool threadPool = engine.getServer().getThreadPool();
        QueuedThreadPool qtp = (QueuedThreadPool)threadPool;
        String prefixDefault = qtp.getName();
        assertTrue("No threads whose name is started with " + prefixDefault,
                checkForExistenceOfThreads(prefixDefault));

        // User specific thread name prefix 2
        String threadNamePrefix2 = "AnotherPrefix";
        engine = factory.createJettyHTTPServerEngine(PORT2, "http");
        parameters = new ThreadingParameters();
        parameters.setThreadNamePrefix(threadNamePrefix2);
        engine.setThreadingParameters(parameters);
        engine.finalizeConfig();
        handler = new JettyHTTPTestHandler("string2", true);
        engine.addServant(new URL("https://localhost:" + PORT2 + "/test"), handler);
        assertTrue("No threads whose name is started with " + threadNamePrefix2,
                checkForExistenceOfThreads(threadNamePrefix2));

        JettyHTTPServerEngineFactory.destroyForPort(PORT1);
        JettyHTTPServerEngineFactory.destroyForPort(PORT2);
        JettyHTTPServerEngineFactory.destroyForPort(PORT3);
    }

    private boolean checkForExistenceOfThreads(String prefixName) {
        Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
        Set<Thread> threadSet = threads.keySet();
        for (Thread thread : threadSet) {
            if (thread.getName().startsWith(prefixName)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testEngineRetrieval() throws Exception {
        JettyHTTPServerEngine engine =
            factory.createJettyHTTPServerEngine(PORT1, "http");

        assertTrue(
            "Engine references for the same port should point to the same instance",
            engine == factory.retrieveJettyHTTPServerEngine(PORT1));

        JettyHTTPServerEngineFactory.destroyForPort(PORT1);
    }

    @Test
    public void testHttpAndHttps() throws Exception {
        JettyHTTPServerEngine engine =
            factory.createJettyHTTPServerEngine(PORT1, "http");

        assertTrue("Protocol must be http",
                "http".equals(engine.getProtocol()));

        engine = new JettyHTTPServerEngine();
        engine.setPort(PORT2);
        engine.setMaxIdleTime(30000);
        engine.setTlsServerParameters(new TLSServerParameters());
        engine.finalizeConfig();

        List<JettyHTTPServerEngine> list = new ArrayList<>();
        list.add(engine);
        factory.setEnginesList(list);
        engine = factory.createJettyHTTPServerEngine(PORT2, "https");
        JettyHTTPTestHandler handler1 = new JettyHTTPTestHandler("string1", true);
        // need to create a servant to create the connector
        engine.addServant(new URL("https://localhost:" + PORT2 + "/test"), handler1);
        assertTrue("Protocol must be https",
                "https".equals(engine.getProtocol()));

        assertEquals("Get the wrong maxIdleTime.", 30000, getMaxIdle(engine.getConnector()));

        factory.setTLSServerParametersForPort(PORT1, new TLSServerParameters());
        engine = factory.createJettyHTTPServerEngine(PORT1, "https");
        assertTrue("Protocol must be https",
                   "https".equals(engine.getProtocol()));

        factory.setTLSServerParametersForPort(PORT3, new TLSServerParameters());
        engine = factory.createJettyHTTPServerEngine(PORT3, "https");
        assertTrue("Protocol must be https",
                   "https".equals(engine.getProtocol()));

        JettyHTTPServerEngineFactory.destroyForPort(PORT1);
        JettyHTTPServerEngineFactory.destroyForPort(PORT2);
        JettyHTTPServerEngineFactory.destroyForPort(PORT3);
    }


    private int getMaxIdle(Connector connector) throws Exception {
        try {
            return (int)connector.getClass().getMethod("getMaxIdleTime").invoke(connector);
        } catch (NoSuchMethodException nex) {
            //jetty 9
        }
        return ((Long)connector.getClass().getMethod("getIdleTimeout").invoke(connector)).intValue();
    }

    @Test
    public void testaddServants() throws Exception {
        String urlStr = "http://localhost:" + PORT1 + "/hello/test";
        String urlStr2 = "http://localhost:" + PORT1 + "/hello233/test";
        JettyHTTPServerEngine engine =
            factory.createJettyHTTPServerEngine(PORT1, "http");
        engine.setMaxIdleTime(30000);
        engine.addServant(new URL(urlStr), new JettyHTTPTestHandler("string1", true));
        assertEquals("Get the wrong maxIdleTime.", 30000, getMaxIdle(engine.getConnector()));

        String response = getResponse(urlStr);
        assertEquals("The jetty http handler did not take effect", response, "string1");

        try {
            engine.addServant(new URL(urlStr), new JettyHTTPTestHandler("string2", true));
            fail("We don't support to publish the two service at the same context path");
        } catch (Exception ex) {
            assertTrue("Get a wrong exception message", ex.getMessage().indexOf("hello/test") > 0);
        }

        try {
            engine.addServant(new URL(urlStr + "/test"), new JettyHTTPTestHandler("string2", true));
            fail("We don't support to publish the two service at the same context path");
        } catch (Exception ex) {
            assertTrue("Get a wrong exception message", ex.getMessage().indexOf("hello/test/test") > 0);
        }

        try {
            engine.addServant(new URL("http://localhost:" + PORT1 + "/hello"),
                              new JettyHTTPTestHandler("string2", true));
            fail("We don't support to publish the two service at the same context path");
        } catch (Exception ex) {
            assertTrue("Get a wrong exception message", ex.getMessage().indexOf("hello") > 0);
        }

        // check if the system property change could work
        System.setProperty("org.apache.cxf.transports.http_jetty.DontCheckUrl", "true");
        engine.addServant(new URL(urlStr + "/test"), new JettyHTTPTestHandler("string2", true));
        // clean up the System property setting
        System.clearProperty("org.apache.cxf.transports.http_jetty.DontCheckUrl");

        engine.addServant(new URL(urlStr2), new JettyHTTPTestHandler("string2", true));

        Set<ObjectName>  s = CastUtils.cast(ManagementFactory.getPlatformMBeanServer().
            queryNames(new ObjectName("org.eclipse.jetty.server:type=server,*"), null));
        assertEquals("Could not find 1 Jetty Server: " + s, 1, s.size());

        engine.removeServant(new URL(urlStr));
        engine.shutdown();
        response = getResponse(urlStr2);
        assertEquals("The jetty http handler did not take effect", response, "string2");
        // set the get request
        JettyHTTPServerEngineFactory.destroyForPort(PORT1);

    }

    /**
     * Test that multiple JettyHTTPServerEngine instances can be used simultaneously
     * without having name collisions.
     */
    @Test
    public void testJmxSupport() throws Exception {
        String urlStr = "http://localhost:" + PORT1 + "/hello/test";
        String urlStr2 = "http://localhost:" + PORT2 + "/hello/test";
        JettyHTTPServerEngine engine =
            factory.createJettyHTTPServerEngine(PORT1, "http");
        JettyHTTPServerEngine engine2 =
            factory.createJettyHTTPServerEngine(PORT2, "http");
        JettyHTTPTestHandler handler1 = new JettyHTTPTestHandler("string1", true);
        JettyHTTPTestHandler handler2 = new JettyHTTPTestHandler("string2", true);

        engine.addServant(new URL(urlStr), handler1);

        Set<ObjectName>  s = CastUtils.cast(ManagementFactory.getPlatformMBeanServer().
            queryNames(new ObjectName("org.eclipse.jetty.server:type=server,*"), null));
        assertEquals("Could not find 1 Jetty Server: " + s, 1, s.size());

        engine2.addServant(new URL(urlStr2), handler2);

        s = CastUtils.cast(ManagementFactory.getPlatformMBeanServer().
            queryNames(new ObjectName("org.eclipse.jetty.server:type=server,*"), null));
        assertEquals("Could not find 2 Jetty Server: " + s, 2, s.size());

        engine.removeServant(new URL(urlStr));
        engine2.removeServant(new URL(urlStr2));
        engine.shutdown();

        s = CastUtils.cast(ManagementFactory.getPlatformMBeanServer().
            queryNames(new ObjectName("org.eclipse.jetty.server:type=server,*"), null));
        assertEquals("Could not find 2 Jetty Server: " + s, 1, s.size());

        engine2.shutdown();

        s = CastUtils.cast(ManagementFactory.getPlatformMBeanServer().
            queryNames(new ObjectName("org.eclipse.jetty.server:type=server,*"), null));
        assertEquals("Could not find 0 Jetty Server: " + s, 0, s.size());

        JettyHTTPServerEngineFactory.destroyForPort(PORT1);
        JettyHTTPServerEngineFactory.destroyForPort(PORT2);
    }

   

    @Test
    public void testGetContextHandler() throws Exception {
        String urlStr = "http://localhost:" + PORT1 + "/hello/test";
        JettyHTTPServerEngine engine =
            factory.createJettyHTTPServerEngine(PORT1, "http");
        ServletContextHandler contextHandler = engine.getContextHandler(new URL(urlStr));
        // can't find the context handler here
        assertNull(contextHandler);
        JettyHTTPTestHandler handler1 = new JettyHTTPTestHandler("string1", true);
        JettyHTTPTestHandler handler2 = new JettyHTTPTestHandler("string2", true);
        engine.addServant(new URL(urlStr), handler1);
        String response = getResponse(urlStr);
        assertEquals("the jetty http handler1 did not take effect", response, "string1");
        // Note: There appears to be an internal issue in Jetty that does not
        // unregister the MBean for handler1 during this setHandler operation.
        // This scenario may create a warning message in the logs
        //     (javax.management.InstanceAlreadyExistsException: org.apache.cxf.
        //         transport.http_jetty:type=jettyhttptesthandler,id=0)
        // when running subsequent tests.
        contextHandler = engine.getContextHandler(new URL(urlStr));
        //contextHandler.stop();
        ServletHandler servletHandler = contextHandler.getServletHandler();
        MatchedResource<MappedServlet> mappedServlet = servletHandler.getMatchedServlet("/test");
        if (mappedServlet != null) {
            ServletHolder servletHolder = mappedServlet.getResource().getServletHolder();
            if (servletHolder != null) {
                // the servlet exist with the same path
                // just update the servlet
                servletHolder.doStop();
                servletHolder.setServlet(handler2);
                servletHolder.initialize();
            } 
        }
        

        response = getResponse(urlStr);
        assertEquals("the jetty http handler2 did not take effect", response, "string2");

        JettyHTTPServerEngineFactory.destroyForPort(PORT1);
    }

    @Test
    public void testJettyHTTPHandler() throws Exception {
        String urlStr1 = "http://localhost:" + PORT3 + "/hello/test1";
        String urlStr2 = "http://localhost:" + PORT3 + "/hello/test2";
        JettyHTTPServerEngine engine =
            factory.createJettyHTTPServerEngine(PORT3, "http");
        ContextHandler contextHandler = engine.getContextHandler(new URL(urlStr1));
        // can't find the context handler here
        assertNull(contextHandler);
        JettyHTTPHandler handler1 = new JettyHTTPTestHandler("test", false);
        JettyHTTPHandler handler2 = new JettyHTTPTestHandler("test2", false);
        engine.addServant(new URL(urlStr1), handler1);
        
        contextHandler = engine.getContextHandler(new URL(urlStr1));
        assertNotNull(contextHandler);

        engine.addServant(new URL(urlStr2), handler2);
        contextHandler = engine.getContextHandler(new URL(urlStr2));
        assertNotNull(contextHandler);

        String response = getResponse(urlStr1);
        assertEquals("the jetty http handler1 did not take effect", response, "test");

        response = getResponse(urlStr2);
        assertEquals("the jetty http handler2 did not take effect", response, "test2");

        JettyHTTPServerEngineFactory.destroyForPort(PORT3);
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
