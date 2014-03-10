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
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http_jetty.JettyHTTPDestination;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.eclipse.jetty.websocket.WebSocketFactory;
import org.eclipse.jetty.websocket.WebSocketFactory.Acceptor;

/**
 * 
 */
public class JettyWebSocketManager {
    private WebSocketFactory webSocketFactory;
    
    // either servlet is set or destination + servletContext are set
    private CXFNonSpringServlet servlet;
    private AbstractHTTPDestination destination;
    private ServletContext servletContext;

    public void init(JettyWebSocketHandler handler, JettyHTTPDestination dest) {
        this.destination = dest;
        //TODO customize websocket factory configuration options when using the destination.
        
        webSocketFactory = new WebSocketFactory((Acceptor)handler, 8192);
    }

    public void init(CXFNonSpringServlet srvlt, ServletConfig sc) throws ServletException {
        this.servlet = srvlt;
        try {
            
            String bs = srvlt.getInitParameter("bufferSize");
            
            webSocketFactory = new WebSocketFactory((Acceptor)srvlt, bs == null ? 8192 : Integer.parseInt(bs));
            webSocketFactory.start();
        
            String max = srvlt.getInitParameter("maxIdleTime");
            if (max != null) {
                webSocketFactory.setMaxIdleTime(Integer.parseInt(max));
            }
            max = srvlt.getInitParameter("maxTextMessageSize");
            if (max != null) {
                webSocketFactory.setMaxTextMessageSize(Integer.parseInt(max));
            }
            max = srvlt.getInitParameter("maxBinaryMessageSize");
            if (max != null) {
                webSocketFactory.setMaxBinaryMessageSize(Integer.parseInt(max));
            }
        } catch (Exception e) {
            throw e instanceof ServletException ? (ServletException)e : new ServletException(e);
        }
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

    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (destination != null) {
            destination.invoke(null, servletContext, request, response);
        } else if (servlet != null) {
            ((CXFJettyWebSocketServletService)servlet).serviceInternal(request, response);
        }
    }
}
