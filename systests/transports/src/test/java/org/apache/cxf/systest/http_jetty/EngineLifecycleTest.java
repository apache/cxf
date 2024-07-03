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
package org.apache.cxf.systest.http_jetty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.http_jetty.JettyHTTPDestination;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngine;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Handler;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


/**
 * This class tests starting up and shutting down the embedded server when there
 * is extra jetty configuration.
 */
public class EngineLifecycleTest {
    private static final String PORT1 = TestUtil.getPortNumber(EngineLifecycleTest.class, 1);
    private static final String PORT2 = TestUtil.getPortNumber(EngineLifecycleTest.class, 2);
    private GenericApplicationContext applicationContext;

    @Test
    public void testUpDownWithServlets() throws Exception {
        setUpBus();

        Bus bus = (Bus)applicationContext.getBean("cxf");
        ServerRegistry sr = bus.getExtension(ServerRegistry.class);
        ServerImpl si = (ServerImpl) sr.getServers().get(0);
        JettyHTTPDestination jhd = (JettyHTTPDestination) si.getDestination();
        JettyHTTPServerEngine e = (JettyHTTPServerEngine) jhd.getEngine();
        org.eclipse.jetty.server.Server jettyServer = e.getServer();

        for (Handler h : jettyServer.getDescendants(WebAppContext.class)) {
            WebAppContext wac = (WebAppContext) h;
            if ("/jsunit".equals(wac.getContextPath())) {
                wac.addServlet("org.eclipse.jetty.ee10.servlet.DefaultServlet", "/bloop");
                break;
            }
        }

        try {
            verifyStaticHtml();
            invokeService();
        } finally {
            shutdownService();
        }
    }

    @Test
    public void testServerUpDownUp() throws Exception {
        for (int i = 0; i < 2; ++i) { // twice
            setUpBus();
            try {
                verifyStaticHtml();
                invokeService();
                invokeService8801();
            } finally {
                shutdownService();
            }
        }
    }

    private void setUpBus() throws Exception {
        verifyNoServer(PORT2);
        verifyNoServer(PORT1);

        applicationContext = new GenericApplicationContext();

        System.setProperty("jetty.staticResourceURL", 
                           "src/test/resources/"  
                           + getClass().getPackage().getName().replace('.', '/'));

        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(applicationContext);
        reader.loadBeanDefinitions(
                new ClassPathResource("META-INF/cxf/cxf.xml"),
                new ClassPathResource("cxf.xml", getClass()),
                new ClassPathResource("jetty-engine.xml", getClass()),
                new ClassPathResource("server-lifecycle-beans.xml", getClass()));

        applicationContext.refresh();
    }

    private void invokeService() {
        DummyInterface client = (DummyInterface) applicationContext.getBean("dummy-client");
        assertEquals("We should get out put from this client", "hello world", client.echo("hello world"));
    }

    private void invokeService8801() {
        DummyInterface client = (DummyInterface) applicationContext.getBean("dummy-client-8801");
        assertEquals("We should get out put from this client", "hello world", client.echo("hello world"));
    }

    private static void verifyStaticHtml() throws Exception {
        String response = null;
        for (int i = 0; i < 50 && null == response; i++) {
            try (InputStream in = new URL("http://localhost:" + PORT2 + "/test.html").openConnection()
                    .getInputStream()) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                IOUtils.copy(in, os);
                response = new String(os.toByteArray());
            } catch (Exception ex) {
                Thread.sleep(100L);
            }
        }
        assertNotNull("Test doc can not be read", response);

        String html;
        try (InputStream htmlFile = EngineLifecycleTest.class.getResourceAsStream("test.html")) {
            byte[] buf = new byte[htmlFile.available()];
            htmlFile.read(buf);
            html = new String(buf);
        }
        assertEquals("Can't get the right test html", html, response);
    }

    private void shutdownService() throws Exception {
        applicationContext.close();
        applicationContext = null;
//        System.gc(); // make sure the port is cleaned up a bit

        verifyNoServer(PORT2);
        verifyNoServer(PORT1);
    }

    private static void verifyNoServer(String port) {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress().getHostName(), Integer.parseInt(port))) {
            fail("Server on port " + port + " accepted a connection.");
        } catch (UnknownHostException e) {
            fail("Unknown host for local address");
        } catch (IOException e) {
            // this is what we want.
        }
    }

}