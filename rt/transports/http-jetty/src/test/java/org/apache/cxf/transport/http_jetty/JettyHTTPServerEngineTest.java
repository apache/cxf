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
import java.util.Set;

import javax.management.ObjectName;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.spring.ConfigurerImpl;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.management.InstrumentationManager;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.SslSocketConnector;

public class JettyHTTPServerEngineTest extends Assert {

    private Bus bus;
    private IMocksControl control;
    private JettyHTTPServerEngineFactory factory;

    @Before
    public void setUp() throws Exception {
        control = EasyMock.createNiceControl();
        bus = control.createMock(Bus.class);
        factory = new JettyHTTPServerEngineFactory();
        factory.setBus(bus);

        Configurer configurer = new ConfigurerImpl();

        bus.getExtension(Configurer.class);
        EasyMock.expectLastCall().andReturn(configurer).anyTimes();
        
        InstrumentationManager iManager = control.createMock(InstrumentationManager.class);
        iManager.getMBeanServer();
        EasyMock.expectLastCall().andReturn(ManagementFactory.getPlatformMBeanServer()).anyTimes();
        
        bus.getExtension(InstrumentationManager.class);
        EasyMock.expectLastCall().andReturn(iManager).anyTimes();
        
        control.replay();
    }

    @Test
    public void testEngineRetrieval() throws Exception {
        JettyHTTPServerEngine engine =
            factory.createJettyHTTPServerEngine(9234, "http");

        assertTrue(
            "Engine references for the same port should point to the same instance",
            engine == factory.retrieveJettyHTTPServerEngine(9234));

        factory.destroyForPort(1234);
    }

    @Test
    public void testHttpAndHttps() throws Exception {
        JettyHTTPServerEngine engine =
            factory.createJettyHTTPServerEngine(9234, "http");

        assertTrue("Protocol must be http",
                "http".equals(engine.getProtocol()));

        engine = new JettyHTTPServerEngine();
        engine.setPort(9235);
        engine.setTlsServerParameters(new TLSServerParameters());
        engine.finalizeConfig();

        List<JettyHTTPServerEngine> list = new ArrayList<JettyHTTPServerEngine>();
        list.add(engine);
        factory.setEnginesList(list);

        engine = factory.createJettyHTTPServerEngine(9235, "https");

        assertTrue("Protocol must be https",
                "https".equals(engine.getProtocol()));

        factory.setTLSServerParametersForPort(9234, new TLSServerParameters());
        engine = factory.createJettyHTTPServerEngine(9234, "https");
        assertTrue("Protocol must be https",
                   "https".equals(engine.getProtocol()));

        factory.setTLSServerParametersForPort(9236, new TLSServerParameters());
        engine = factory.createJettyHTTPServerEngine(9236, "https");
        assertTrue("Protocol must be https",
                   "https".equals(engine.getProtocol()));

        factory.destroyForPort(9234);
        factory.destroyForPort(9235);
        factory.destroyForPort(9236);
    }


    @Test
    public void testSetConnector() throws Exception {
        JettyHTTPServerEngine engine = new JettyHTTPServerEngine();
        Connector conn = new SslSocketConnector();
        engine.setConnector(conn);
        engine.setPort(9000);
        try {
            engine.finalizeConfig();
            fail("We should get the connector not set with TSLServerParameter exception.");
        } catch (Exception ex) {
            // expect the excepion
        }

        engine = new JettyHTTPServerEngine();
        conn = new SelectChannelConnector();
        conn.setPort(9002);
        engine.setConnector(conn);
        engine.setPort(9000);
        try {
            engine.finalizeConfig();
            fail("We should get the connector not set right port exception.");
        } catch (Exception ex) {
            // expect the exception
        }

        engine = new JettyHTTPServerEngine();
        conn = new SslSocketConnector();
        conn.setPort(9003);
        engine.setConnector(conn);
        engine.setPort(9003);
        engine.setTlsServerParameters(new TLSServerParameters());
        engine.finalizeConfig();
    }

    @Test
    public void testaddServants() throws Exception {
        String urlStr = "http://localhost:9234/hello/test";
        String urlStr2 = "http://localhost:9234/hello233/test";
        JettyHTTPServerEngine engine =
            factory.createJettyHTTPServerEngine(9234, "http");
        JettyHTTPTestHandler handler1 = new JettyHTTPTestHandler("string1", true);
        JettyHTTPTestHandler handler2 = new JettyHTTPTestHandler("string2", true);
        engine.addServant(new URL(urlStr), handler1);
        String response = null;
        response = getResponse(urlStr);
        assertEquals("The jetty http handler did not take effect", response, "string1");

        engine.addServant(new URL(urlStr), handler2);
        response = getResponse(urlStr);
        assertEquals("The jetty http handler did not take effect", response, "string1string2");
        engine.addServant(new URL(urlStr2), handler2);
        
        Set<ObjectName>  s = ManagementFactory.getPlatformMBeanServer().
            queryNames(new ObjectName("org.mortbay.jetty:type=server,*"), null);
        assertEquals("Could not find 1 Jetty Server: " + s, 1, s.size());
        
        engine.removeServant(new URL(urlStr));
        engine.shutdown();
        response = getResponse(urlStr2);
        assertEquals("The jetty http handler did not take effect", response, "string2");
        // set the get request
        factory.destroyForPort(9234);

    }
    
    /**
     * Test that multiple JettyHTTPServerEngine instances can be used simultaneously
     * without having name collisions.
     */
    @Test
    public void testJmxSupport() throws Exception {
        String urlStr = "http://localhost:9234/hello/test";
        String urlStr2 = "http://localhost:9235/hello/test";
        JettyHTTPServerEngine engine =
            factory.createJettyHTTPServerEngine(9234, "http");
        JettyHTTPServerEngine engine2 =
            factory.createJettyHTTPServerEngine(9235, "http");
        JettyHTTPTestHandler handler1 = new JettyHTTPTestHandler("string1", true);
        JettyHTTPTestHandler handler2 = new JettyHTTPTestHandler("string2", true);
        
        engine.addServant(new URL(urlStr), handler1);
        
        Set<ObjectName>  s = ManagementFactory.getPlatformMBeanServer().
            queryNames(new ObjectName("org.mortbay.jetty:type=server,*"), null);
        assertEquals("Could not find 1 Jetty Server: " + s, 1, s.size());
        
        engine2.addServant(new URL(urlStr2), handler2);
        
        s = ManagementFactory.getPlatformMBeanServer().
            queryNames(new ObjectName("org.mortbay.jetty:type=server,*"), null);
        assertEquals("Could not find 2 Jetty Server: " + s, 2, s.size());
        
        engine.removeServant(new URL(urlStr));
        engine2.removeServant(new URL(urlStr2));
        
        
        engine.shutdown();
        
        s = ManagementFactory.getPlatformMBeanServer().
            queryNames(new ObjectName("org.mortbay.jetty:type=server,*"), null);
        assertEquals("Could not find 2 Jetty Server: " + s, 1, s.size());
        
        engine2.shutdown();
        
        s = ManagementFactory.getPlatformMBeanServer().
            queryNames(new ObjectName("org.mortbay.jetty:type=server,*"), null);
        assertEquals("Could not find 0 Jetty Server: " + s, 0, s.size());
        
        factory.destroyForPort(9234);
        factory.destroyForPort(9235);
    }

    @Test
    public void testSetHandlers() throws Exception {
        URL url = new URL("http://localhost:9235/hello/test");
        JettyHTTPTestHandler handler1 = new JettyHTTPTestHandler("string1", true);
        JettyHTTPTestHandler handler2 = new JettyHTTPTestHandler("string2", true);

        JettyHTTPServerEngine engine = new JettyHTTPServerEngine();
        engine.setPort(9235);
        engine.setJettyHTTPServerEngineFactory(factory);

        List<Handler> handlers = new ArrayList<Handler>();
        handlers.add(handler1);
        engine.setHandlers(handlers);
        engine.finalizeConfig();

        engine.addServant(url, handler2);
        String response = null;
        try {
            response = getResponse(url.toString());
            assertEquals("the jetty http handler1 did not take effect", response, "string1string2");
        } catch (Exception ex) {
            fail("Can't get the reponse from the server " + ex);
        }
        engine.stop();
    }

    @Test
    public void testGetContextHandler() throws Exception {
        String urlStr = "http://localhost:9234/hello/test";
        JettyHTTPServerEngine engine =
            factory.createJettyHTTPServerEngine(9234, "http");
        ContextHandler contextHandler = engine.getContextHandler(new URL(urlStr));
        // can't find the context handler here
        assertNull(contextHandler);
        JettyHTTPTestHandler handler1 = new JettyHTTPTestHandler("string1", true);
        JettyHTTPTestHandler handler2 = new JettyHTTPTestHandler("string2", true);
        engine.addServant(new URL(urlStr), handler1);

        // Note: There appears to be an internal issue in Jetty that does not
        // unregister the MBean for handler1 during this setHandler operation.
        // This scenario may create a warning message in the logs 
        //     (javax.management.InstanceAlreadyExistsException: org.apache.cxf.
        //         transport.http_jetty:type=jettyhttptesthandler,id=0)
        // when running subsequent tests.
        contextHandler = engine.getContextHandler(new URL(urlStr));
        contextHandler.setHandler(handler2);
        contextHandler.start();

        String response = null;
        try {
            response = getResponse(urlStr);
        } catch (Exception ex) {
            fail("Can't get the reponse from the server " + ex);
        }
        assertEquals("the jetty http handler did not take effect", response, "string2");
        factory.destroyForPort(9234);
    }

    @Test
    public void testJettyHTTPHandler() throws Exception {
        String urlStr1 = "http://localhost:9236/hello/test";
        String urlStr2 = "http://localhost:9236/hello/test2";
        JettyHTTPServerEngine engine =
            factory.createJettyHTTPServerEngine(9236, "http");
        ContextHandler contextHandler = engine.getContextHandler(new URL(urlStr1));
        // can't find the context handler here
        assertNull(contextHandler);
        JettyHTTPHandler handler1 = new JettyHTTPTestHandler("test", false);
        JettyHTTPHandler handler2 = new JettyHTTPTestHandler("test2", false);
        engine.addServant(new URL(urlStr1), handler1);
        engine.addServant(new URL(urlStr2), handler2);


        String response = null;
        try {
            response = getResponse(urlStr1 + "/test");
        } catch (Exception ex) {
            fail("Can't get the reponse from the server " + ex);
        }
        assertEquals("the jetty http handler did not take effect", response, "test");

        try {
            response = getResponse(urlStr2 + "/test");
        } catch (Exception ex) {
            fail("Can't get the reponse from the server " + ex);
        }
        assertEquals("the jetty http handler did not take effect", response, "test2");

        factory.destroyForPort(9236);
    }

    private String getResponse(String target) throws Exception {
        URL url = new URL(target);

        URLConnection connection = url.openConnection();

        assertTrue(connection instanceof HttpURLConnection);
        connection.connect();
        InputStream in = connection.getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        IOUtils.copy(in, buffer);
        return buffer.toString();
    }
}
