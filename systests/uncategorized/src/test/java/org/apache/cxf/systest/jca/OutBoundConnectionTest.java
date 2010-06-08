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

package org.apache.cxf.systest.jca;

import java.net.URL;

import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;
import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceException;

import org.apache.cxf.connector.Connection;
import org.apache.cxf.jca.cxf.CXFConnectionRequestInfo;
import org.apache.cxf.jca.cxf.ManagedConnectionFactoryImpl;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.apache.hello_world_soap_http.SOAPService;
import org.junit.BeforeClass;
import org.junit.Test;

public class OutBoundConnectionTest extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;
    private final QName serviceName = new QName("http://apache.org/hello_world_soap_http",
                                                "SOAPService");
    
    private final QName portName = new QName("http://apache.org/hello_world_soap_http",
                                             "SoapPort");

    public static class Server extends AbstractBusTestServerBase {        
        public static final String PORT = allocatePort(Server.class);
        protected void run() {
            Object implementor = new GreeterImpl();
            String address = "http://localhost:" + PORT + "/SoapContext/SoapPort";
            Endpoint.publish(address, implementor);
            
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
    public void testBasicConnection() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);
        
        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull(service);
                
        CXFConnectionRequestInfo cri = new CXFConnectionRequestInfo(Greeter.class, 
                                           wsdl,
                                           service.getServiceName(),
                                           portName);
        cri.setAddress("http://localhost:" + PORT + "/SoapContext/SoapPort");
        ManagedConnectionFactory managedFactory = new ManagedConnectionFactoryImpl();
        Subject subject = new Subject();
        ManagedConnection mc = managedFactory.createManagedConnection(subject, cri);        
        Object o = mc.getConnection(subject, cri);
        
        // test for the Object hash()
        try {
            o.hashCode();
            o.toString();
        } catch (WebServiceException ex) {
            fail("The connection object should support Object method");
        }
        
        verifyResult(o);
    }
    
    
    @Test
    public void testGetConnectionFromSEI() throws Exception {
        CXFConnectionRequestInfo requestInfo = new CXFConnectionRequestInfo();
        requestInfo.setInterface(Greeter.class);
        requestInfo.setAddress("http://localhost:" + PORT + "/SoapContext/SoapPort");
        
        ManagedConnectionFactory factory = new ManagedConnectionFactoryImpl();
        ManagedConnection mc = factory.createManagedConnection(null, requestInfo);
        Object client = mc.getConnection(null, requestInfo);
        
        verifyResult(client);
    }
    
    
    private void verifyResult(Object o) throws Exception {
        
        assertTrue("returned connect does not implement Connection interface", o instanceof Connection);
        assertTrue("returned connect does not implement Connection interface", o instanceof Greeter);
   
        Greeter greeter = (Greeter) o;   
        String response = new String("Bonjour");      
        for (int idx = 0; idx < 5; idx++) {
            String reply = greeter.sayHi();
            assertNotNull("no response received from service", reply);
            assertEquals(response, reply);
        }
    }
}
