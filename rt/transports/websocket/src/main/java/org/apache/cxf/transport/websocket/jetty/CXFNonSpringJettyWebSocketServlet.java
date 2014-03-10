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
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.cxf.transport.websocket.WebSocketTransportFactory;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketFactory;

/**
 * 
 */
public class CXFNonSpringJettyWebSocketServlet extends CXFNonSpringServlet 
    implements CXFJettyWebSocketServletService, WebSocketFactory.Acceptor {
    private static final long serialVersionUID = 6921073894009215482L;
    private JettyWebSocketManager websocketManager;
    
    public CXFNonSpringJettyWebSocketServlet() {
    }
    
    public CXFNonSpringJettyWebSocketServlet(DestinationRegistry destinationRegistry, boolean loadBus) {
        super(destinationRegistry, loadBus);
    }

    public CXFNonSpringJettyWebSocketServlet(DestinationRegistry destinationRegistry) {
        super(destinationRegistry);
    }

    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        return new JettyWebSocket(websocketManager, request, protocol);
    }


    public boolean checkOrigin(HttpServletRequest request, String origin) {
        return true;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        if (websocketManager.acceptWebSocket(req, res)) {
            return;
        }
        super.service(req, res);
    }
    
    @Override
    public void serviceInternal(ServletRequest req, ServletResponse resp) throws ServletException, IOException {
        super.service(req, resp);
    }
    
    
    @Override
    public void init(ServletConfig sc) throws ServletException {
        super.init(sc);
        
        websocketManager = new JettyWebSocketManager();
        websocketManager.init(this, sc);
    }

    @Override
    protected DestinationRegistry getDestinationRegistryFromBus(Bus bus) {
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        try {
            DestinationFactory df = dfm
                .getDestinationFactory("http://cxf.apache.org/transports/websocket/configuration");
            if (df instanceof WebSocketTransportFactory) {
                WebSocketTransportFactory transportFactory = (WebSocketTransportFactory)df;
                return transportFactory.getRegistry();
            }
        } catch (BusException e) {
            // why are we throwing a busexception if the DF isn't found?
        }
        return null;
    }

    @Override
    public void destroy() {
        try {
            websocketManager.destroy();
        } catch (Exception e) {
            // ignore
        } finally {
            super.destroy();
        }
    }
}
