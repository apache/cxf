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

package org.apache.cxf.systest.js;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;

import javax.xml.namespace.QName;

import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;
import org.apache.hello_world_soap_http.SOAPServiceTest1;
import org.junit.BeforeClass;
import org.junit.Test;



public class JSClientServerTest extends AbstractBusClientServerTestBase {
    
    public static final String JS_PORT = Server.JS_PORT;
    public static final String JSX_PORT = Server.JSX_PORT;
    
    private static final String NS = "http://apache.org/hello_world_soap_http";
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }

    @Test
    public void testJSMessageMode() throws Exception {
        QName serviceName = new QName(NS, "SOAPService");
        QName portName = new QName(NS, "SoapPort");

        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull(service);

        String response1 = new String("TestGreetMeResponse");
        String response2 = new String("TestSayHiResponse");
        try {
            Greeter greeter = service.getPort(portName, Greeter.class);
            updateAddressPort(greeter, JS_PORT);
            String greeting = greeter.greetMe("TestGreetMeRequest");
            assertNotNull("no response received from service", greeting);
            assertEquals(response1, greeting);

            String reply = greeter.sayHi();
            assertNotNull("no response received from service", reply);
            assertEquals(response2, reply);
        } catch (UndeclaredThrowableException ex) {
            ex.printStackTrace();
            throw (Exception)ex.getCause();
        }
    }

    @Test
    public void testJSPayloadMode() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        QName serviceName = new QName(NS, "SOAPService_Test1");
        QName portName = new QName(NS, "SoapPort_Test1");

        SOAPServiceTest1 service = new SOAPServiceTest1(wsdl, serviceName);
        assertNotNull(service);

        String response1 = new String("TestGreetMeResponse");
        String response2 = new String("TestSayHiResponse");
        try {
            Greeter greeter = service.getPort(portName, Greeter.class);
            updateAddressPort(greeter, JSX_PORT);
            String greeting = greeter.greetMe("TestGreetMeRequest");
            assertNotNull("no response received from service", greeting);
            assertEquals(response1, greeting);

            String reply = greeter.sayHi();
            assertNotNull("no response received from service", reply);
            assertEquals(response2, reply);
        } catch (UndeclaredThrowableException ex) {
            ex.printStackTrace();
            throw (Exception)ex.getCause();
        }
    }

}
