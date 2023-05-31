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

package org.apache.cxf.systest.jaxrs.websocket;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.jaxrs.Book;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.DefaultHandler;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * JAXRSClientServerWebSocketSpringWebAppTest without atmosphere
 */
public class JAXRSClientServerWebSocketSpringWebAppNoAtmosphereTest extends AbstractJAXRSClientServerWebSocketTest {
    private static final String PORT = BookServerWebSocket.PORT2_WAR;
    private static org.eclipse.jetty.server.Server server;

    @BeforeClass
    public static void startServers() throws Exception {
        System.setProperty("org.apache.cxf.transport.websocket.atmosphere.disabled", "true");
        AbstractResourceInfo.clearAllMaps();

        startServers(PORT);
    }

    protected static void startServers(String port) throws Exception {
        server = new org.eclipse.jetty.server.Server(Integer.parseInt(port));

        WebAppContext webappcontext = new WebAppContext();
        String contextPath = JAXRSClientServerWebSocketSpringWebAppTest.class
                .getResource("/jaxrs_websocket").toString();
        webappcontext.setContextPath("/webapp");

        webappcontext.setWar(contextPath);
        Handler.Collection handlers = new Handler.Sequence();
        handlers.setHandlers(new Handler[] {webappcontext, new DefaultHandler()});
        server.setHandler(handlers);
        server.start();
    }

    @AfterClass
    public static void stopServers() throws Exception {
        server.stop();
        server.destroy();
        System.clearProperty("org.apache.cxf.transport.websocket.atmosphere.disabled");
    }

    @Test
    public void testGetBookHTTP() throws Exception {
        String address = "http://localhost:" + getPort() + getContext() + "/http/web/bookstore/books/1";
        WebClient wc = WebClient.create(address);
        wc.accept("application/xml");
        Book book = wc.get(Book.class);
        assertEquals(1L, book.getId());
    }

    protected String getPort() {
        return PORT;
    }

    @Override
    protected String getContext() {
        return "/webapp";
    }
}
