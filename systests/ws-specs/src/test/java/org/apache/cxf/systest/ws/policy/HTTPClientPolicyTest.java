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

import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceException;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.greeter_control.BasicGreeterService;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.PingMeFault;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.policy.PolicyException;
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
public class HTTPClientPolicyTest extends AbstractBusClientServerTestBase {

    private static final Logger LOG = LogUtils.getLogger(HTTPClientPolicyTest.class);
    private static final String POLICY_ENGINE_ENABLED_CFG =
        "org/apache/cxf/systest/ws/policy/http.xml";
    private static final String POLICY_VIA_FEATURE_CFG =
        "org/apache/cxf/systest/ws/policy/http_client_policy_feature.xml";
    private static final QName GREETER_QNAME =
        new QName("http://cxf.apache.org/greeter_control", "BasicGreeterService");
    
    public static class Server extends AbstractBusTestServerBase {
   
        protected void run()  {            
            SpringBusFactory bf = new SpringBusFactory();
            Bus bus = bf.createBus();
            
            BusFactory.setDefaultBus(bus);
            LoggingInInterceptor in = new LoggingInInterceptor();
            LoggingOutInterceptor out = new LoggingOutInterceptor();           
            bus.getInInterceptors().add(in);
            bus.getOutInterceptors().add(out);                    
            bus.getOutFaultInterceptors().add(out);
            
            HttpGreeterImpl implementor = new HttpGreeterImpl();
            implementor.setThrowAlways(true);
            String address = "http://localhost:9020/SoapContext/GreeterPort";
            Endpoint.publish(address, implementor);
            LOG.info("Published greeter endpoint.");            
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
    public void testUsingHTTPClientPolicies() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        bus = bf.createBus(POLICY_ENGINE_ENABLED_CFG);
        BusFactory.setDefaultBus(bus);
        
        LoggingInInterceptor in = new LoggingInInterceptor();
        bus.getInInterceptors().add(in);
        bus.getInFaultInterceptors().add(in);
        LoggingOutInterceptor out = new LoggingOutInterceptor();
        bus.getOutInterceptors().add(out);
        bus.getOutFaultInterceptors().add(out);
      
        // use a client wsdl with policies attached to endpoint, operation and message subjects
        
        URL url = HTTPClientPolicyTest.class.getResource("http_client_greeter.wsdl");
        
        BasicGreeterService gs = new BasicGreeterService(url, GREETER_QNAME);
        final Greeter greeter = gs.getGreeterPort();
        LOG.fine("Created greeter client.");
        
        // sayHi - this operation has message policies that are incompatible with
        // the endpoint policies
       
        try {
            greeter.sayHi();
            fail("Did not receive expected PolicyException.");
        } catch (WebServiceException wex) {
            PolicyException ex = (PolicyException)wex.getCause();
            assertEquals("INCOMPATIBLE_HTTPCLIENTPOLICY_ASSERTIONS", ex.getCode());
        }

        // greetMeOneWay - no message or operation policies

        greeter.greetMeOneWay("CXF");

        // greetMe - operation policy specifies receive timeout and should cause every 
        // other invocation to fail

        assertEquals("CXF", greeter.greetMe("cxf")); 

        try {
            greeter.greetMe("cxf");
            fail("Didn't get the exception");
        } catch (Exception ex) {
            //ex.printStackTrace();
            assertTrue(ex.getCause().getClass().getName(), ex.getCause() instanceof SocketTimeoutException);
        }
     
        // pingMe - policy attached to binding operation fault should have no effect

        try {
            greeter.pingMe();
            fail("Expected PingMeFault not thrown.");
        } catch (PingMeFault ex) {
            assertEquals(2, (int)ex.getFaultInfo().getMajor());
            assertEquals(1, (int)ex.getFaultInfo().getMinor());
        } 

    }
    
    @Test
    public void testHTTPClientPolicyViaFeature() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        bus = bf.createBus(POLICY_VIA_FEATURE_CFG);
        BusFactory.setDefaultBus(bus);
        
        // use a WSDL sanitized of any policy assertions, instead
        // the HTTPClientPolicy is applied via a feature set on the
        // <jaxws:client> bean
        //
        URL url = HTTPClientPolicyTest.class.getResource("bare_greeter.wsdl");
        
        BasicGreeterService gs = new BasicGreeterService(url, GREETER_QNAME);
        final Greeter greeter = gs.getGreeterPort();
        LOG.fine("Created greeter client.");
        
        greeter.greetMeOneWay("CXF");
        
        HTTPConduit c = 
            (HTTPConduit)(ClientProxy.getClient(greeter).getConduit());
        assertNotNull("expected HTTPConduit", c);
        assertNotNull("expected DecoupledEndpoint", 
                      c.getClient().getDecoupledEndpoint());
        assertEquals("unexpected DecoupledEndpoint", 
                     "http://localhost:9990/decoupled_endpoint",
                     c.getClient().getDecoupledEndpoint());
    }
}
