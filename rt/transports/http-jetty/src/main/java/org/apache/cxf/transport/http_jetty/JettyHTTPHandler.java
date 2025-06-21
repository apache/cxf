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

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.Bus;
import org.apache.cxf.transport.http.HttpUrlUtil;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletContextRequest;



public class JettyHTTPHandler extends HttpServlet {
    private static final String METHOD_TRACE = "TRACE";

    protected JettyHTTPDestination jettyHTTPDestination;
    protected ServletContext servletContext;
    private String urlName;
    private boolean contextMatchExact;
    private Bus bus;

    public JettyHTTPHandler(JettyHTTPDestination jhd, boolean cmExact) {
        contextMatchExact = cmExact;
        jettyHTTPDestination = jhd;
    }

    public JettyHTTPHandler(Bus bus) {
        this.bus = bus;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }
    public void setServletContext(ServletContext sc) {
        servletContext = sc;
        if (jettyHTTPDestination != null) {
            jettyHTTPDestination.setServletContext(sc);
        }
    }
    public void setName(String name) {
        urlName = name;
    }

    public String getName() {
        return urlName;
    }

    
    public ServletContextHandler createContextHandler() {
        return new ServletContextHandler();
    }

    public Bus getBus() {
        return jettyHTTPDestination != null ? jettyHTTPDestination.getBus() : bus;
    }
    
    
    
    
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        String target = ServletContextRequest.getServletContextRequest(request).getDecodedPathInContext();
        if (request.getMethod().equals(METHOD_TRACE)) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        } else {
            if (contextMatchExact) {
                if (target.equals(urlName)) {
                    jettyHTTPDestination.doService(servletContext, request, response);
                }
            } else {
                if (target.equals(urlName) || HttpUrlUtil.checkContextPath(urlName, target)) {
                    jettyHTTPDestination.doService(servletContext, request, response);
                }
            }
        }

    }

}
