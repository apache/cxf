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

package org.apache.cxf.transport.websocket.jetty;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.servlet.ServletDestination;
import org.apache.cxf.transport.websocket.WebSocketDestinationService;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketFactory;

/**
 * 
 */
public class JettyWebSocketServletDestination extends ServletDestination implements 
    WebSocketDestinationService {
    private JettyWebSocketManager webSocketManager;

    public JettyWebSocketServletDestination(Bus bus, DestinationRegistry registry, EndpointInfo ei,
                                            String path) throws IOException {
        super(bus, registry, ei, path);
        webSocketManager = new JettyWebSocketManager();
        webSocketManager.init(this, new Acceptor());
    }

    @Override
    public void invoke(ServletConfig config, ServletContext context, HttpServletRequest req,
                       HttpServletResponse resp) throws IOException {
        if (webSocketManager.acceptWebSocket(req, resp)) {
            return;
        }

        super.invoke(config, context, req, resp);
    }

    @Override
    public void invokeInternal(ServletConfig config, ServletContext context, HttpServletRequest req,
                               HttpServletResponse resp) throws IOException {
        super.invoke(config, context, req, resp);
    }

    @Override
    public void shutdown() {
        try {
            webSocketManager.destroy();
        } catch (Exception e) {
            // ignore
        } finally {
            super.shutdown();
        }
    }

    private class Acceptor implements WebSocketFactory.Acceptor {

        @Override
        public boolean checkOrigin(HttpServletRequest request, String protocol) {
            return true;
        }

        @Override
        public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
            return new JettyWebSocket(webSocketManager, request, protocol);
        }
    }
}
