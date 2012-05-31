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

package org.apache.cxf.systest.http_jetty;

import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceException;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.auth.DigestAuthSupplier;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests thread pool config.
 */

public class JettyDigestAuthTest extends AbstractClientServerTestBase {
    private static final String PORT = allocatePort(JettyDigestAuthTest.class);
    private static final String ADDRESS = "http://localhost:" + PORT + "/SoapContext/SoapPort";
    private static final QName SERVICE_NAME = 
        new QName("http://apache.org/hello_world_soap_http", "SOAPServiceAddressing");

    private Greeter greeter;

    
    public static class JettyDigestServer extends AbstractBusTestServerBase  {
        Endpoint ep;
        
        protected void run()  {
            String configurationFile = "jettyDigestServer.xml";
            URL configure =
                JettyBasicAuthServer.class.getResource(configurationFile);
            Bus bus = new SpringBusFactory().createBus(configure, true);
            bus.getInInterceptors().add(new LoggingInInterceptor());
            bus.getOutInterceptors().add(new LoggingOutInterceptor());
            SpringBusFactory.setDefaultBus(bus);
            setBus(bus);

            GreeterImpl implementor = new GreeterImpl();
            ep = Endpoint.publish(ADDRESS, implementor);
        }
        
        public void tearDown() throws Exception {
            if (ep != null) {
                ep.stop();
                ep = null;
            }
        }
    }
    
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(JettyDigestServer.class, true));
    }

    @Before
    public void setUp() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        greeter = new SOAPService(wsdl, SERVICE_NAME).getPort(Greeter.class);
        BindingProvider bp = (BindingProvider)greeter;
        ClientProxy.getClient(greeter).getInInterceptors().add(new LoggingInInterceptor());
        ClientProxy.getClient(greeter).getOutInterceptors().add(new LoggingOutInterceptor()); 
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                   ADDRESS);
        bp.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, "ffang");
        bp.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "pswd");
        HTTPConduit cond = (HTTPConduit)ClientProxy.getClient(greeter).getConduit();
        cond.setAuthSupplier(new DigestAuthSupplier());
        
        HTTPClientPolicy client = new HTTPClientPolicy();
        ClientProxy.getClient(greeter).getOutInterceptors()
            .add(new AbstractPhaseInterceptor<Message>(Phase.PRE_STREAM_ENDING) {
                
                public void handleMessage(Message message) throws Fault {
                    Map<String, ?> headers = CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
                    if (headers.containsKey("Proxy-Authorization")) {
                        throw new RuntimeException("Should not have Proxy-Authorization");
                    }
                }
            });
        client.setAllowChunking(false);
        cond.setClient(client);
    }

    @Test
    public void testBasicAuth() throws Exception { 
        assertEquals("Hello Alice", greeter.greetMe("Alice"));
        assertEquals("Hello Bob", greeter.greetMe("Bob"));

        try {
            BindingProvider bp = (BindingProvider)greeter;
            bp.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, "blah");
            bp.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "foo");
            greeter.greetMe("Alice");
            fail("Password was wrong, should have failed");
        } catch (WebServiceException wse) {
            //ignore - expected
        }
    }
}
