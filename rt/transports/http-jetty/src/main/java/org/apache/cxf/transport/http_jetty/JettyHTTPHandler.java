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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.handler.AbstractHandler;

public class JettyHTTPHandler extends AbstractHandler {
    private String urlName;
    private boolean contextMatchExact;
    private JettyHTTPDestination jettyHTTPDestination;
    private ServletContext servletContext;

    public JettyHTTPHandler(JettyHTTPDestination jhd, boolean cmExact) {
        contextMatchExact = cmExact;
        jettyHTTPDestination = jhd;
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

    boolean checkContextPath(String target) {
        String pathString = urlName;
        if (!pathString.endsWith("/")) {
            pathString = pathString + "/";
        }
        return target.startsWith(pathString);
    }

    public void handle(String target, HttpServletRequest req,
                       HttpServletResponse resp, int dispatch) throws IOException {
        if (contextMatchExact) {
            if (target.equals(urlName)) {
                jettyHTTPDestination.doService(servletContext, req, resp);
            }
        } else {
            if (target.equals(urlName) || checkContextPath(target)) {
                jettyHTTPDestination.doService(servletContext, req, resp);
            }
        }
    }



}
