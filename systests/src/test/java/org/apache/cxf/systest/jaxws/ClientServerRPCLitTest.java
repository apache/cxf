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

package org.apache.cxf.systest.jaxws;

import java.lang.reflect.UndeclaredThrowableException;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;

import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.hello_world_rpclit.GreeterRPCLit;
import org.apache.hello_world_rpclit.SOAPServiceRPCLit;
import org.apache.hello_world_rpclit.types.MyComplexStruct;
import org.apache.hello_world_soap_http.RPCLitGreeterImpl;
import org.junit.BeforeClass;
import org.junit.Test;


public class ClientServerRPCLitTest extends AbstractClientServerTestBase {

    private final QName portName = new QName("http://apache.org/hello_world_rpclit", "SoapPortRPCLit");

    public static class Server extends AbstractBusTestServerBase {        

        protected void run()  {
            Object implementor = new RPCLitGreeterImpl();
            String address = "http://localhost:9002/SOAPServiceRPCLit/SoapPort";
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
        
        SOAPServiceRPCLit service = new SOAPServiceRPCLit();
        assertNotNull(service);
        
        String response1 = new String("Hello Milestone-");
        String response2 = new String("Bonjour");
        try {
            GreeterRPCLit greeter = service.getPort(portName, GreeterRPCLit.class);
            for (int idx = 0; idx < 1; idx++) {
                String greeting = greeter.greetMe("Milestone-" + idx);
                assertNotNull("no response received from service", greeting);
                String exResponse = response1 + idx;
                assertEquals(exResponse, greeting);

                String reply = greeter.sayHi();
                assertNotNull("no response received from service", reply);
                assertEquals(response2, reply);
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    
    @Test
    public void testComplexType() throws Exception {
        SOAPServiceRPCLit service = new SOAPServiceRPCLit();
        assertNotNull(service);

        GreeterRPCLit greeter = service.getPort(portName, GreeterRPCLit.class);

        MyComplexStruct in = new MyComplexStruct(); 
        in.setElem1("elem1");
        in.setElem2("elem2");
        in.setElem3(45);

        try {            
            MyComplexStruct out = greeter.sendReceiveData(in); 
            assertNotNull("no response received from service", out);
            assertEquals(in.getElem1(), out.getElem1());
            assertEquals(in.getElem2(), out.getElem2());
            assertEquals(in.getElem3(), out.getElem3());
        } catch (UndeclaredThrowableException ex) {
            throw (Exception) ex.getCause();
        }
        
    }
}
