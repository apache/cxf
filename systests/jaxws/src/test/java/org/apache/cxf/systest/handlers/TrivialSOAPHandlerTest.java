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

package org.apache.cxf.systest.handlers;

import java.lang.reflect.UndeclaredThrowableException;

import javax.xml.ws.Endpoint;


import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the use of a SOAPHandler which does not use the underlying SOAP message but simply
 * returns true instead.
 */
public class TrivialSOAPHandlerTest extends AbstractClientServerTestBase {
    static String address =  "http://localhost:"
        + TestUtil.getPortNumber(Server.class) 
        + "/SoapContext/GreeterPort"; 
    
    public static class Server extends AbstractBusTestServerBase {
        
        protected void run()  {            
            Object implementor = new TrivialSOAPHandlerAnnotatedGreeterImpl();
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
        assertTrue("server did not launch correctly",
                   launchServer(Server.class));
    }
    
    @Test
    public void testInvocation() throws Exception {

        GreeterService service = new GreeterService();
        assertNotNull(service);

        try {
            Greeter greeter = service.getGreeterPort();
            setAddress(greeter, address);
            
            String greeting = greeter.greetMe("Bonjour");
            assertNotNull("no response received from service", greeting);
            assertEquals("BONJOUR", greeting);

        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

}
