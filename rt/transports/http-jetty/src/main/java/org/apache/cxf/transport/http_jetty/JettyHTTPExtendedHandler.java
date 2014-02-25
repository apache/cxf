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
package org.apache.cxf.transport.http_jetty;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketFactory;

/**
 * The extended version of JettyHTTPHandler that can support websocket.
 */
class JettyHTTPExtendedHandler extends JettyHTTPHandler implements WebSocketFactory.Acceptor {
    private final WebSocketFactory webSocketFactory = new WebSocketFactory(this);

    public JettyHTTPExtendedHandler(JettyHTTPDestination jhd, boolean cmExact) {
        super(jhd, cmExact);
    }

    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        return new JettyWebSocket(jettyHTTPDestination, servletContext, request, protocol);
    }
    
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
                       HttpServletResponse response) throws IOException, ServletException {
        // only switch to websocket if websocket is enabled for this destination 
        if (jettyHTTPDestination != null && jettyHTTPDestination.isEnableWebSocket()
            && (webSocketFactory.acceptWebSocket(request, response) || response.isCommitted())) {
            baseRequest.setHandled(true);
        } else {
            super.handle(target, baseRequest, request, response);
        }
    }

    public boolean checkOrigin(HttpServletRequest request, String protocol) {
        return true;
    }
}
