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
import java.net.URL;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http_jetty.JettyHTTPDestination;
import org.apache.cxf.transport.http_jetty.JettyHTTPHandler;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;
import org.apache.cxf.transport.websocket.WebSocketDestinationService;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketFactory;

/**
 * 
 */
public class JettyWebSocketDestination extends JettyHTTPDestination implements 
    WebSocketDestinationService {
    private JettyWebSocketManager webSocketManager;
    
    public JettyWebSocketDestination(Bus bus, DestinationRegistry registry, EndpointInfo ei,
                                     JettyHTTPServerEngineFactory serverEngineFactory) throws IOException {
        super(bus, registry, ei, serverEngineFactory);
        webSocketManager = new JettyWebSocketManager();
        webSocketManager.init(this, new Acceptor());
    }
    
    @Override
    public void invokeInternal(ServletConfig config, ServletContext context, HttpServletRequest req,
                               HttpServletResponse resp) throws IOException {
        super.invoke(config, context, req, resp);
    }

    @Override
    protected String getAddress(EndpointInfo endpointInfo) {
        String address = endpointInfo.getAddress();
        if (address.startsWith("ws")) {
            address = "http" + address.substring(2);
        }
        return address;
    }


    @Override
    protected String getBasePath(String contextPath) throws IOException {
        if (StringUtils.isEmpty(endpointInfo.getAddress())) {
            return "";
        }
        return new URL(getAddress(endpointInfo)).getPath();
    }
    
    @Override
    protected JettyHTTPHandler createJettyHTTPHandler(JettyHTTPDestination jhd, boolean cmExact) {
        return new JettyWebSocketHandler(jhd, cmExact, webSocketManager);
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
