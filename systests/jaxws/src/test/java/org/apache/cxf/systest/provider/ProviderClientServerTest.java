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

package org.apache.cxf.systest.provider;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProviderClientServerTest extends AbstractBusClientServerTestBase {
    public static final String ADDRESS
        = "http://localhost:" + TestUtil.getPortNumber(Server.class)
            + "/SoapContext/SoapProviderPort";

    public static class Server extends AbstractBusTestServerBase {

        protected void run() {
            Object implementor = new HWSoapMessageDocProvider();
            Endpoint ep = Endpoint.create(implementor);
            Map<String, Object> map = new HashMap<>();
            map.put(Message.SCHEMA_VALIDATION_ENABLED, Boolean.TRUE);
            ep.setProperties(map);
            ((EndpointImpl)ep).getInInterceptors().add(new LoggingInInterceptor());
            ((EndpointImpl)ep).getOutInterceptors().add(new LoggingOutInterceptor());
            ep.publish(ADDRESS);

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
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testSOAPMessageModeDocLit() throws Exception {

        QName serviceName =
            new QName("http://apache.org/hello_world_soap_http", "SOAPProviderService");
        QName portName =
            new QName("http://apache.org/hello_world_soap_http", "SoapProviderPort");

        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull(service);

        String response1 = new String("TestSOAPOutputPMessage");
        String response2 = new String("Bonjour");
        try {
            Greeter greeter = service.getPort(portName, Greeter.class);
            setAddress(greeter, ADDRESS);
            try {
                greeter.greetMe("Return sayHi");
                fail("Should have thrown an exception");
            } catch (Exception ex) {
                //expected
                assertTrue(ex.getMessage().contains("sayHiResponse"));
            }
            for (int idx = 0; idx < 2; idx++) {
                String greeting = greeter.greetMe("Milestone-" + idx);
                assertNotNull("no response received from service", greeting);
                assertEquals(response1, greeting);

                String reply = greeter.sayHi();
                assertNotNull("no response received from service", reply);
                assertEquals(response2, reply);
            }

            try {
                greeter.greetMe("throwFault");
                fail("Expected a fault");
            } catch (SOAPFaultException ex) {
                assertTrue(ex.getMessage().contains("Test Fault String"));
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }

    }


    @Test
    public void testSOAPMessageModeDocLitWithSchemaValidation() throws Exception {

        QName serviceName =
            new QName("http://apache.org/hello_world_soap_http", "SOAPProviderService");
        QName portName =
            new QName("http://apache.org/hello_world_soap_http", "SoapProviderPort");

        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        assertNotNull(wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull(service);


        try {
            Greeter greeter = service.getPort(portName, Greeter.class);
            setAddress(greeter, ADDRESS);
            try {
                greeter.greetMe("this is a greetMe message which length is more "
                    + "than 30 so that I wanna a schema validation error");
                fail("Should have thrown an exception");
            } catch (Exception ex) {
                //expected
                assertTrue(ex.getMessage().contains("the length of the value is 96, but the required maximum is 30"));
            }

            try {
                greeter.greetMe("exceed maxLength");
                fail("Should have thrown an exception");
            } catch (Exception ex) {
                //expected
                assertTrue(ex.getMessage().contains("cvc-maxLength-valid"));
            }

        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }

    }

}
