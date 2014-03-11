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

import java.net.URISyntaxException;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;

import org.junit.BeforeClass;

/**
 * JAXRSClientServerWebSocket test with jaxrs:server using the jetty webapp server.
 */
public class JAXRSClientServerWebSocketSpringWebAppTest extends JAXRSClientServerWebSocketTest {
    private static final String PORT = BookServerWebSocket.PORT_WAR;
    @BeforeClass
    public static void startServers() throws Exception {
        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server();

        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(Integer.parseInt(BookServerWebSocket.PORT_WAR));
        server.setConnectors(new Connector[] {connector});

        WebAppContext webappcontext = new WebAppContext();
        String contextPath = null;
        try {
            contextPath = JAXRSClientServerWebSocketSpringWebAppTest.class
                .getResource("/jaxrs_websocket").toURI().getPath();
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        webappcontext.setContextPath("/");

        webappcontext.setWar(contextPath);
        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[] {webappcontext, new DefaultHandler()});
        server.setHandler(handlers);
        server.start();

    }

    protected String getPort() {
        return PORT;
    }
}
