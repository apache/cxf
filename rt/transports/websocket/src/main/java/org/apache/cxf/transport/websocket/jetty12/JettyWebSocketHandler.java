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
package org.apache.cxf.transport.websocket.jetty12;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.transport.http_jetty.JettyHTTPDestination;
import org.apache.cxf.transport.http_jetty.JettyHTTPHandler;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServerContainer;
import org.eclipse.jetty.ee10.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.core.WebSocketComponents;
import org.eclipse.jetty.websocket.core.server.WebSocketServerComponents;

/**
 * The extended version of JettyHTTPHandler that can support websocket.
 */
class JettyWebSocketHandler extends JettyHTTPHandler {
    final Jetty12WebSocketDestination webSocketDestination;
    JettyWebSocketServerContainer webSocketContainer;

    JettyWebSocketHandler(JettyHTTPDestination jhd, boolean cmExact,
                          Jetty12WebSocketDestination wsd) {
        super(jhd, cmExact);
        this.webSocketDestination = wsd;
        
    }
    
    
    public void initHandler(Server server) {
        ServletContextHandler servletContextHandler = createContextHandler();
        servletContextHandler.setServer(server);
        setServletContext(servletContextHandler.getServletContext());
        webSocketContainer = webSocketDestination.getWebSocketContainer(getServletContext());
    }

   
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        if (webSocketContainer.upgrade(webSocketDestination.getCreator(), request, response)) {
            return;
        }
        super.service(request, response);
    }

    @Override
    public ServletContextHandler createContextHandler() {
        final ServletContextHandler handler = new ServletContextHandler();
        JettyWebSocketServletContainerInitializer.configure(handler, null);
        handler.setAttribute(WebSocketServerComponents.WEBSOCKET_COMPONENTS_ATTRIBUTE, new WebSocketComponents());
        return handler;
    }
}
