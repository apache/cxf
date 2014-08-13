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
import java.util.concurrent.Executor;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.websocket.WebSocketDestinationService;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.eclipse.jetty.websocket.WebSocketFactory;
import org.eclipse.jetty.websocket.WebSocketFactory.Acceptor;

/**
 * This class is used to provide the common functionality used by
 * the two jetty websocket destination classes: JettyWebSocketDestination and JettyWebSocketServletDestination.
 */
class JettyWebSocketManager {
    private WebSocketFactory webSocketFactory;
    private AbstractHTTPDestination destination;
    private ServletContext servletContext;
    private Executor executor;

    public void init(AbstractHTTPDestination dest, Acceptor acceptor) {
        this.destination = dest;

        //TODO customize websocket factory configuration options when using the destination.
        webSocketFactory = new WebSocketFactory(acceptor, 8192);

        // the executor for decoupling the service invocation from websocket's onMessage call which is
        // synchronously blocked
        executor = dest.getBus().getExtension(WorkQueueManager.class).getAutomaticWorkQueue();
    }

    void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void destroy() {
        try {
            webSocketFactory.stop();
        } catch (Exception e) {
            // ignore
        }
    }

    public boolean acceptWebSocket(ServletRequest req, ServletResponse res) throws IOException {
        try {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;
            if (webSocketFactory.acceptWebSocket(request, response) || response.isCommitted()) {
                return true;
            }
        } catch (ClassCastException e) {
            // ignore
        }
        return false;
    }

    void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (destination != null) {
            ((WebSocketDestinationService)destination).invokeInternal(null, servletContext, request, response);
        }
    }
    
    Executor getExecutor() {
        return executor;
    }
}
