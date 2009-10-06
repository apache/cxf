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
package org.apache.cxf.transport.http_osgi;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.servlet.AbstractHTTPServlet;

public class OsgiServlet extends AbstractHTTPServlet {
    
    private OsgiDestinationRegistryIntf transport;
    private OsgiServletController controller;
    
    public OsgiServlet(OsgiDestinationRegistryIntf transport) {
        this.transport = transport;
    }

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        controller = new OsgiServletController(this);
    }
    
    @Override
    public void destroy() {
    }

    public OsgiDestinationRegistryIntf getTransport() {
        return transport;
    }

    public void invoke(HttpServletRequest request, HttpServletResponse res) throws ServletException {
        controller.invoke(request, res);
    }
    
    protected MessageImpl createInMessage() {
        return new MessageImpl();
    }

    protected ExchangeImpl createExchange() {
        return new ExchangeImpl();
    }
}
