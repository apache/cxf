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
package org.apache.cxf.systest.grizzly;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.MTOMFeature;
import jakarta.xml.ws.spi.http.HttpContext;
import org.apache.cxf.testutil.common.TestUtil;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class EndpointAPITest {

    private static int counter;
    private static int currentPort;
    private HttpServer server;

    @Before
    public void setUp() {
        //grizzly gets confused with the 2.0 "Connect: upgrade" header
        System.setProperty("org.apache.cxf.transport.http.forceVersion", "1.1");
        currentPort = Integer.valueOf(TestUtil.getPortNumber(EndpointAPITest.class, counter++));
        server = new HttpServer();
        NetworkListener networkListener = new NetworkListener("jaxwslistener", "0.0.0.0", currentPort);
        server.addListener(networkListener);
    }

    @After
    public void tearDown() {
        server.shutdownNow();
        server = null;
    }

    @Test
    public void testSingleEndpoint() throws Exception {

        String contextPath = "/ctxt";
        String path = "/echo";
        String address = "http://localhost:" + currentPort + contextPath + path;

        HttpContext context = new GrizzlyHttpContext(server, contextPath, path);
        Endpoint endpoint = Endpoint.create(new EndpointBean());
        endpoint.publish(context); // Use grizzly HTTP context for publishing

        server.start();

        invokeEndpoint(address);

        endpoint.stop();
    }

    @Test
    public void testMultiplePublishSameAddress() throws Exception {
        server.start();
        String contextPath = "/ctxt";
        String path = "/echo";
        //need to use the same HttpContext, otherwise Grizzly get confused
        HttpContext ctx = new GrizzlyHttpContext(server, contextPath, path);
        for (int i = 0; i < 3; i++) {
            String address = "http://localhost:" + currentPort + contextPath + path;

            Endpoint endpoint = Endpoint.create(new EndpointBean());
            endpoint.publish(ctx); // Use grizzly HTTP context for publishing

            invokeEndpoint(address);

            endpoint.stop();
        }
    }

    @Test
    public void testMultipleEndpointsSameContext() throws Exception {
        server.start();
        String contextPath = "/ctxt";
        String path = "/echo";
        int k = 3;
        Endpoint[] endpoints = new Endpoint[k];
        HttpContext[] contexts = new HttpContext[k];
        String[] addresses = new String[k];
        for (int i = 0; i < k; i++) {
            addresses[i] = "http://localhost:" + currentPort + contextPath + path + i;
            contexts[i] = new GrizzlyHttpContext(server, contextPath, path + i);
            endpoints[i] = Endpoint.create(new EndpointBean());
            endpoints[i].publish(contexts[i]);
        }
        for (int i = 0; i < k; i++) {
            invokeEndpoint(addresses[i]);
        }
        for (int i = 0; i < k; i++) {
            endpoints[i].stop();
        }
    }

    @Test
    public void testMultipleEndpointsDifferentContexts() throws Exception {
        server.start();
        String contextPath = "/ctxt";
        String path = "/echo";
        int k = 3;
        Endpoint[] endpoints = new Endpoint[k];
        HttpContext[] contexts = new HttpContext[k];
        String[] addresses = new String[k];
        for (int i = 0; i < k; i++) {
            addresses[i] = "http://localhost:" + currentPort + contextPath + i + path;
            contexts[i] = new GrizzlyHttpContext(server, contextPath + i, path);
            endpoints[i] = Endpoint.create(new EndpointBean());
            endpoints[i].publish(contexts[i]);
        }
        for (int i = 0; i < k; i++) {
            invokeEndpoint(addresses[i]);
        }
        for (int i = 0; i < k; i++) {
            endpoints[i].stop();
        }
    }

    private void invokeEndpoint(String publishURL) throws Exception {
        URL wsdlURL = new URL(publishURL + "?wsdl");
        QName qname = new QName("http://org.apache.cxf/jaxws/endpoint/", "EndpointService");
        Service service = Service.create(wsdlURL, qname);
        checkBasicInvocations(service);
        checkMTOMInvocation(service);
    }

    private static void checkBasicInvocations(Service service) {
        EndpointInterface port = service.getPort(EndpointInterface.class);
        String helloWorld = "Hello world!";
        assertEquals(0, port.getCount());
        Object retObj = port.echo(helloWorld);
        assertEquals(helloWorld, retObj);
        assertEquals(1, port.getCount());
        port.echo(helloWorld);
        assertEquals(2, port.getCount());
        try {
            port.getException();
            fail("Exception expected!");
        } catch (Exception e) {
            assertEquals("Ooops", e.getMessage());
        }
    }

    private static void checkMTOMInvocation(Service service) throws IOException {
        DataSource ds = new DataSource() {
            public String getContentType() {
                return "text/plain";
            }

            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream("some string".getBytes());
            }

            public String getName() {
                return "none";
            }

            public OutputStream getOutputStream() throws IOException {
                return null;
            }
        };
        EndpointInterface port = service.getPort(EndpointInterface.class,
                                                                    new MTOMFeature(true));
        DataHandler dh = new DataHandler(ds);
        DHResponse response = port.echoDataHandler(new DHRequest(dh));
        assertNotNull(response);
        Object content = response.getDataHandler().getContent();
        assertEquals("Server data", content);
        String contentType = response.getDataHandler().getContentType();
        assertEquals("text/plain", contentType);
    }

}
