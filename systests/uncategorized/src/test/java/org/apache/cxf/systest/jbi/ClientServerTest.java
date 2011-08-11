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

package org.apache.cxf.systest.jbi;


import java.net.URL;
import java.util.logging.Logger;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;


import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jbi.se.CXFServiceEngine;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.jbi.JBITransportFactory;
import org.apache.hello_world.jbi.Greeter;
import org.apache.hello_world.jbi.GreeterImpl;
import org.apache.hello_world.jbi.HelloWorldService;
import org.apache.hello_world.jbi.PingMeFault;
import org.apache.servicemix.components.util.ComponentSupport;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ClientServerTest extends AbstractBusClientServerTestBase {
    
    static final Logger LOG = LogUtils.getLogger(ClientServerTest.class);
    private final QName serviceName = new QName(
                                      "http://apache.org/hello_world/jbi",
                                                "HelloWorldService");
    private JBIContainer container;
    
    @BeforeClass
    public static void startServers() throws Exception {
        //assertTrue("server did not launch correctly", launchServer(Server.class));
    }
    
    @Before
    public void setUp() throws Exception {
        container = new JBIContainer();
        container.setEmbedded(true);
        container.init();
        container.start();

    }
    
    @After
    public void tearDown() throws Exception {
        container.shutDown();
    }
    
    @Test
    public void testJBI() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world_jbi.wsdl");
        assertNotNull(wsdl);
        //Bus bus = BusFactory.getDefaultBus();
        createBus();
        assert bus != null;
        TestComponent component = new TestComponent(new QName("http://apache.org/hello_world/jbi", 
                                                              "HelloWorldService"), 
                                                    "endpoint");
        
        container.activateComponent(new ActivationSpec("component", component));
        
        DeliveryChannel channel = component.getChannel();
        JBITransportFactory jbiTransportFactory = 
            (JBITransportFactory)bus.getExtension(ConduitInitiatorManager.class).
                getConduitInitiator(CXFServiceEngine.JBI_TRANSPORT_ID);
        jbiTransportFactory.setBus(bus);
        
        jbiTransportFactory.setDeliveryChannel(channel);
        HelloWorldService ss = new HelloWorldService(wsdl, serviceName);
        
        Greeter port = ss.getSoapPort();
        Object implementor = new GreeterImpl();
        String address = "http://foo/bar/baz";
        EndpointImpl e = (EndpointImpl)Endpoint.publish(address, implementor);
        e.getServer().getEndpoint().getInInterceptors().add(new LoggingInInterceptor());
        e.getServer().getEndpoint().getOutInterceptors().add(new LoggingOutInterceptor());
        
        port.greetMeOneWay("test");
        String rep = port.greetMe("ffang");
        assertEquals(rep, "Hello ffang");
        rep = port.sayHi();
        assertEquals(rep, "Bonjour");
        try {
            port.pingMe();
            fail();
        } catch (PingMeFault ex) {
            assertEquals(ex.getFaultInfo().getMajor(), (short)2);
            assertEquals(ex.getFaultInfo().getMinor(), (short)1);
        }
    }

    public static class TestComponent extends ComponentSupport {
        public TestComponent(QName service, String endpoint) {
            super(service, endpoint);
        }
        public DeliveryChannel getChannel() throws MessagingException {
            return getContext().getDeliveryChannel();
        }
    }

}
