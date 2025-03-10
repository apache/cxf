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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;
import org.apache.html.dom.HTMLAnchorElementImpl;
import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NoSpringServletClientTest extends AbstractServletTest {
    private static final String PORT = NoSpringServletServer.PORT;
    private static final String SERVICE_URL = "http://localhost:" + PORT + "/soap/";
    private static Bus serverBus;
    private static NoSpringServletServer server;
    private final QName portName = new QName("http://apache.org/hello_world_soap_http", "SoapPort");
    
    @BeforeClass
    public static void startServers() throws Exception {
        server = new NoSpringServletServer();
        server.run();
        serverBus = server.getBus();
        createStaticBus();
    }

    @AfterClass
    public static void shutdownServer() throws Exception {
        if (server != null) {
            server.tearDown();
        }
    }

    @Test
    public void testBasicConnection() throws Exception {
        SOAPService service = new SOAPService(new URL(SERVICE_URL + "Greeter?wsdl"));
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
        String address = SERVICE_URL + "Hello";
        cpfb.setServiceClass(Hello.class);
        cpfb.setAddress(address);
        Hello hello = (Hello) cpfb.create();
        String reply = hello.sayHi(" Willem");
        assertEquals("Get the wrongreply ", reply, "get Willem");
    }

    @Test
    public void testStartAndStopServer() throws Exception {
        stopServer();
        // we should not invoke the server this time
        try {
            testHelloService();
            fail("Expect Exception here.");
        } catch (Exception ex) {
            // do nothing here
            assertTrue(ex.getCause() instanceof java.io.IOException);
            assertTrue(ex.getCause().getMessage().contains("404"));
        }
        startServer();
        testHelloService();
    }

    private void stopServer() {
        ServerRegistry reg = serverBus.getExtension(ServerRegistry.class);
        List<Server> servers = reg.getServers();
        for (Server serv : servers) {
            serv.stop();
        }
    }

    private void startServer() {
        ServerRegistry reg = serverBus.getExtension(ServerRegistry.class);
        List<Server> servers = reg.getServers();
        for (Server serv : servers) {
            serv.start();
        }
    }

    @Test
    public void testGetServiceList() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet method = new HttpGet(SERVICE_URL + "/services");

            try (CloseableHttpResponse res = client.execute(method)) {
                HTMLDocumentImpl doc = parse(res.getEntity().getContent());
                Collection<HTMLAnchorElementImpl> links = getLinks(doc);

                Set<String> s = new HashSet<>();
                for (HTMLAnchorElementImpl l : links) {
                    s.add(l.getHref());
                }
                assertEquals("There should be 3 links for the service", 3, links.size());
                assertTrue(s.contains(SERVICE_URL + "Greeter?wsdl"));
                assertTrue(s.contains(SERVICE_URL + "Hello?wsdl"));
                assertTrue(s.contains(SERVICE_URL + "?wsdl"));
                assertEquals("text/html", getContentType(res));
            }
        }
    }
    
    @Override
    protected int getPort() {
        return Integer.parseInt(NoSpringServletServer.PORT);
    }
}
