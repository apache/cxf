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
package org.apache.cxf.transport.http_jetty.spring;

import javax.xml.namespace.QName;

import org.xml.sax.SAXParseException;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.configuration.spring.ConfigurerImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.test.TestApplicationContext;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http_jetty.JettyHTTPDestination;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngine;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.xml.XmlBeanDefinitionStoreException;


public class ApplicationContextTest extends Assert {
    
    private static final String S1 = 
        ApplicationContextTest.class.getResource("/META-INF/cxf/cxf.xml").toString();
    private static final String S2 = 
        ApplicationContextTest.class.getResource("/META-INF/cxf/cxf-extension-http.xml").toString();
    private static final String S3 = 
        ApplicationContextTest.class.getResource("/META-INF/cxf/cxf-extension-http-jetty.xml").toString();
    
    @BeforeClass
    public static void classUp() {
        BusFactory.setDefaultBus(null);
    }
    
    @AfterClass
    public static void classDown() {
        BusFactory.setDefaultBus(null);
    }
    
 
    @Test
    public void testInvalid() throws Exception {
        String s4 = getClass()
            .getResource("/org/apache/cxf/transport/http_jetty/spring/invalid-beans.xml").toString();
    
        try {
            new TestApplicationContext(new String[] {S1, S2, S3, s4});
            fail("Expected XmlBeanDefinitionStoreException not thrown.");
        } catch (XmlBeanDefinitionStoreException ex) {
            assertTrue(ex.getCause() instanceof SAXParseException);
        }
    }
    
    // This test fails because I think it is trying to read the http-conf.xsd
    // twice, failing on a duplicate name for 
    //http://cxf.apache.org/transports/http/configuration,authorization
    
    @Test
    public void testContext() throws Exception {
        String s4 = getClass()
            .getResource("/org/apache/cxf/transport/http_jetty/spring/beans.xml").toString();
        
        TestApplicationContext ctx = new TestApplicationContext(
            new String[] {S1, S2, S3, s4});
        
        //ctx.refresh();
        
        ConfigurerImpl cfg = new ConfigurerImpl(ctx);
        
        EndpointInfo info = getEndpointInfo("bla", "Foo", "http://localhost:9000");
        
        Bus bus = (Bus) ctx.getBean(Bus.DEFAULT_BUS_ID);
        bus.setExtension(cfg, Configurer.class);
        
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        DestinationFactory factory = dfm.getDestinationFactory("http://schemas.xmlsoap.org/soap/http");
        Destination d = factory.getDestination(info);
        assertTrue(d instanceof JettyHTTPDestination);
        JettyHTTPDestination jd = (JettyHTTPDestination) d;        
        assertEquals("foobar", jd.getServer().getContentEncoding());   
        
        JettyHTTPServerEngine engine = (JettyHTTPServerEngine)jd.getEngine();
        assertEquals(111, engine.getThreadingParameters().getMinThreads());
        assertEquals(120, engine.getThreadingParameters().getMaxThreads());
        
        ConduitInitiatorManager cim = bus.getExtension(ConduitInitiatorManager.class);
        ConduitInitiator ci = cim.getConduitInitiator("http://schemas.xmlsoap.org/soap/http");
        HTTPConduit conduit = (HTTPConduit) ci.getConduit(info);
        assertEquals(97, conduit.getClient().getConnectionTimeout());
        
        info.setName(new QName("urn:test:ns", "Bar"));
        conduit = (HTTPConduit) ci.getConduit(info);
        assertEquals(79, conduit.getClient().getConnectionTimeout());

        JettyHTTPDestination jd2 = 
            (JettyHTTPDestination)factory.getDestination(
                getEndpointInfo("foo", "bar", "http://localhost:9001"));
        
        engine = (JettyHTTPServerEngine)jd2.getEngine();
        assertEquals(99, engine.getThreadingParameters().getMinThreads());
        assertEquals(777, engine.getThreadingParameters().getMaxThreads());
        assertTrue("The engine should support session manager", engine.isSessionSupport());
        assertNotNull("The handlers should not be null", engine.getHandlers());
        assertEquals(1, engine.getHandlers().size());
        assertTrue("The connector should be instance of org.mortbay.jetty.bio.SocketConnector",
                   engine.getConnector() instanceof org.mortbay.jetty.bio.SocketConnector);
        
        JettyHTTPDestination jd3 = 
            (JettyHTTPDestination)factory.getDestination(
                getEndpointInfo("sna", "foo", "https://localhost:9002"));
        
        engine = (JettyHTTPServerEngine)jd3.getEngine();
        assertEquals(111, engine.getThreadingParameters().getMinThreads());
        assertEquals(120, engine.getThreadingParameters().getMaxThreads());
        assertEquals(engine.getTlsServerParameters().getClientAuthentication().isWant(), true);
        assertEquals(engine.getTlsServerParameters().getClientAuthentication().isRequired(), true);
        
        JettyHTTPDestination jd4 = 
            (JettyHTTPDestination)factory.getDestination(
                getEndpointInfo("sna", "foo2", "https://localhost:9003"));
        
        engine = (JettyHTTPServerEngine)jd4.getEngine();
        assertEquals(engine.getTlsServerParameters().getClientAuthentication().isWant(), false);
        assertEquals(engine.getTlsServerParameters().getClientAuthentication().isRequired(), false);

        JettyHTTPDestination jd5 = 
            (JettyHTTPDestination)factory.getDestination(
                getEndpointInfo("sna", "foo", "http://localhost:9100"));
        
        engine = (JettyHTTPServerEngine)jd5.getEngine();
        String r = "expected fallback thread parameters configured for port 0";
        assertNotNull(r, engine.getThreadingParameters());
        assertEquals(r, 21, engine.getThreadingParameters().getMinThreads());
        assertEquals(r, 389, engine.getThreadingParameters().getMaxThreads());
    }
    
    private EndpointInfo getEndpointInfo(String serviceNS, 
                                         String endpointLocal, 
                                         String address) {
        ServiceInfo serviceInfo2 = new ServiceInfo();
        serviceInfo2.setName(new QName(serviceNS, "Service"));        
        EndpointInfo info2 = new EndpointInfo(serviceInfo2, "");
        info2.setName(new QName("urn:test:ns", endpointLocal));
        info2.setAddress(address);
        return info2;
    }
}
