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
package org.apache.cxf.transport.http.netty.server.spring;

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
import org.apache.cxf.transport.http.netty.server.NettyHttpDestination;
import org.apache.cxf.transport.http.netty.server.NettyHttpServerEngine;
import org.springframework.beans.factory.xml.XmlBeanDefinitionStoreException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class ApplicationContextTest {

    private static final String S1 =
        ApplicationContextTest.class.getResource("/META-INF/cxf/cxf.xml").toString();

    @Before
    public void setUp() {
        BusFactory.setDefaultBus(null);
    }

    @After
    public void clearBus() {
        BusFactory.setDefaultBus(null);
    }


    @Test
    public void testInvalid() throws Exception {
        String s4 = getClass()
            .getResource("/org/apache/cxf/transport/http/netty/server/spring/invalid-beans.xml").toString();

        try {
            new TestApplicationContext(new String[] {S1, s4}).close();
            fail("Expected XmlBeanDefinitionStoreException not thrown.");
        } catch (XmlBeanDefinitionStoreException ex) {
            assertTrue(ex.getCause() instanceof SAXParseException);
        }
    }

    @Test
    public void testContext() throws Exception {
        String s4 = getClass()
            .getResource("/org/apache/cxf/transport/http/netty/server/spring/beans.xml").toString();

        TestApplicationContext ctx = new TestApplicationContext(
            new String[] {S1, s4});

        //ctx.refresh();
        checkContext(ctx);
        ctx.close();
    }

    @Test
    public void testContextWithProperties() throws Exception {
        String s4 = getClass()
            .getResource("/org/apache/cxf/transport/http/netty/server/spring/beans-props.xml").toString();

        TestApplicationContext ctx = new TestApplicationContext(
            new String[] {S1, s4});
        checkContext(ctx);
        ctx.close();
    }
    private void checkContext(TestApplicationContext ctx) throws Exception {
        ConfigurerImpl cfg = new ConfigurerImpl(ctx);

        EndpointInfo info = getEndpointInfo("bla", "Foo", "http://localhost:9000");

        Bus bus = (Bus) ctx.getBean(Bus.DEFAULT_BUS_ID);
        bus.setExtension(cfg, Configurer.class);

        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        DestinationFactory factory = dfm.getDestinationFactory("http://cxf.apache.org/transports/http");
        Destination d = factory.getDestination(info, bus);
        assertTrue(d instanceof NettyHttpDestination);
        NettyHttpDestination jd = (NettyHttpDestination) d;
        assertEquals("foobar", jd.getServer().getContentEncoding());

        NettyHttpServerEngine engine = (NettyHttpServerEngine)jd.getEngine();
        assertEquals(120, engine.getThreadingParameters().getThreadPoolSize());


        ConduitInitiatorManager cim = bus.getExtension(ConduitInitiatorManager.class);
        ConduitInitiator ci = cim.getConduitInitiator("http://cxf.apache.org/transports/http");
        HTTPConduit conduit = (HTTPConduit) ci.getConduit(info, bus);
        assertEquals(97, conduit.getClient().getConnectionTimeout());

        info.setName(new QName("urn:test:ns", "Bar"));
        conduit = (HTTPConduit) ci.getConduit(info, bus);
        assertEquals(79, conduit.getClient().getConnectionTimeout());

        NettyHttpDestination jd2 =
            (NettyHttpDestination)factory.getDestination(
                getEndpointInfo("foo", "bar", "http://localhost:9001"), bus);

        engine = (NettyHttpServerEngine)jd2.getEngine();
        assertEquals(40000, engine.getReadIdleTime());
        assertEquals(10000, engine.getMaxChunkContentSize());
        assertTrue("The engine should support session manager", engine.isSessionSupport());

        NettyHttpDestination jd3 =
            (NettyHttpDestination)factory.getDestination(
                getEndpointInfo("sna", "foo", "https://localhost:9002"), bus);

        engine = (NettyHttpServerEngine)jd3.getEngine();
        assertTrue(engine.getTlsServerParameters().getClientAuthentication().isWant());
        assertTrue(engine.getTlsServerParameters().getClientAuthentication().isRequired());

        NettyHttpDestination jd4 =
            (NettyHttpDestination)factory.getDestination(
                getEndpointInfo("sna", "foo2", "https://localhost:9003"), bus);

        engine = (NettyHttpServerEngine)jd4.getEngine();
        assertFalse(engine.getTlsServerParameters().getClientAuthentication().isWant());
        assertFalse(engine.getTlsServerParameters().getClientAuthentication().isRequired());

        /*NettyHttpDestination jd5 =
            (NettyHttpDestination)factory.getDestination(
                getEndpointInfo("sna", "foo", "http://localhost:9100"));*/

        /*engine = (NettyHttpServerEngine)jd5.getEngine();
        String r = "expected fallback thread parameters configured for port 0";
        assertNotNull(r, engine.getThreadingParameters());
        assertEquals(r, 21, engine.getThreadingParameters().getMinThreads());
        assertEquals(r, 389, engine.getThreadingParameters().getMaxThreads());*/
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
