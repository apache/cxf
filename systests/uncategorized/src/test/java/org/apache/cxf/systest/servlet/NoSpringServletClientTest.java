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
package org.apache.cxf.systest.servlet;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import javax.xml.namespace.QName;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebResponse;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.systest.jaxws.Hello;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;
import org.junit.BeforeClass;
import org.junit.Test;



public class NoSpringServletClientTest extends AbstractBusClientServerTestBase {
    private final QName portName = new QName("http://apache.org/hello_world_soap_http", "SoapPort");
    private final String serviceURL = "http://localhost:9000/soap/";
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(NoSpringServletServer.class));
    }

    @Test
    public void testBasicConnection() throws Exception {
        SOAPService service = new SOAPService(new URL(serviceURL + "Greeter?wsdl"));
        Greeter greeter = service.getPort(portName, Greeter.class);
        try {
            String reply = greeter.greetMe("test");
            assertNotNull("no response received from service", reply);
            assertEquals("Hello test", reply);
            reply = greeter.sayHi();
            assertNotNull("no response received from service", reply);
            assertEquals("Bonjour", reply);
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    @Test
    public void testHelloService() throws Exception {
        JaxWsProxyFactoryBean cpfb = new JaxWsProxyFactoryBean();
        String address = serviceURL + "Hello";
        cpfb.setServiceClass(Hello.class);
        cpfb.setAddress(address);
        Hello hello = (Hello) cpfb.create();
        String reply = hello.sayHi(" Willem");
        assertEquals("Get the wrongreply ", reply, "get Willem");
    }

    @Test
    public void testGetServiceList() throws Exception {
        WebConversation client = new WebConversation();
        WebResponse res = client.getResponse(serviceURL);
        WebLink[] links = res.getLinks();
        assertEquals("There should get two links for the service", 2, links.length);
        assertEquals(serviceURL + "Greeter?wsdl", links[0].getURLString());
        assertEquals("text/html", res.getContentType());
    }
}
