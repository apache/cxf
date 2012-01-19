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

package org.apache.cxf.systest.ws.policy;

import java.util.logging.Logger;

import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceException;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.greeter_control.BasicGreeterService;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.PingMeFault;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Tests the use of the WS-Policy Framework to determine the behaviour of the HTTP client
 * by policies including the HTTPClientPolicy assertion attached to different subjects
 * of the contract (endpoint, operation, binding, messager).
 * The server in this test is not policy aware.
 * Neither client nor server do have addressing interceptors installed: there are no addressing
 * assertions that would trigger the installation of the interceptors on the client side. The use
 * of the DecoupledEndpoint attribute in the HTTPClientPolicy assertions is merely for illustrating
 * the use of multiple compatible or incompatible assertions.
 */
public class HTTPServerPolicyTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);

    private static final Logger LOG = LogUtils.getLogger(HTTPServerPolicyTest.class);

    public static class Server extends AbstractBusTestServerBase {
   
        protected void run()  {            
            SpringBusFactory bf = new SpringBusFactory();
            Bus bus = bf.createBus("org/apache/cxf/systest/ws/policy/http-server.xml");
            
            GreeterImpl implementor = new GreeterImpl();
            implementor.setThrowAlways(true);
            Endpoint.publish("http://localhost:" + PORT + "/SoapContext/GreeterPort", implementor);

            LOG.info("Published greeter endpoint."); 
            
            LoggingInInterceptor in = new LoggingInInterceptor();
            LoggingOutInterceptor out = new LoggingOutInterceptor();
            
            bus.getInInterceptors().add(in);
            bus.getOutInterceptors().add(out);
            bus.getOutFaultInterceptors().add(out);
        }
        

        public static void main(String[] args) {
            try { 
                Server s = new Server(); 
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally { 
                System.out.println("done!");
            }
        }
    }    

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }
         
    @Test
    public void testUsingHTTPServerPolicies() throws Exception {        
        
        // use a plain client
        
        SpringBusFactory bf = new SpringBusFactory();
        Bus bus = bf.createBus();
        
        BasicGreeterService gs = new BasicGreeterService();
        final Greeter greeter = gs.getGreeterPort();
        
        updateAddressPort(greeter, PORT);
        LoggingInInterceptor in = new LoggingInInterceptor();
        LoggingOutInterceptor out = new LoggingOutInterceptor();
        
        bus.getInInterceptors().add(in);
        bus.getOutInterceptors().add(out);
        
        LOG.fine("Created greeter client.");

        // sayHi - this operation has message policies that are incompatible with
        // the endpoint policies

        try {
            greeter.sayHi();
            fail("Did not receive expected Exception.");
        } catch (WebServiceException wse) {
            SoapFault sf = (SoapFault)wse.getCause();
            assertEquals("Server", sf.getFaultCode().getLocalPart());
            
            String text = sf.getMessage();
            assertTrue(text.contains("{http://cxf.apache.org/transports/http/configuration}server"));
            
            // assertEquals("INCOMPATIBLE_HTTPSERVERPOLICY_ASSERTIONS", ex.getCode());
        }
        
        // greetMe - no operation or message specific policies

        assertEquals("CXF", greeter.greetMe("cxf")); 
     
        // pingMe - policy attached to binding operation fault should have no effect

        try {
            greeter.pingMe();
            fail("Expected PingMeFault not thrown.");
        } catch (PingMeFault ex) {
            assertEquals(2, (int)ex.getFaultInfo().getMajor());
            assertEquals(1, (int)ex.getFaultInfo().getMinor());
        }

    }
}
