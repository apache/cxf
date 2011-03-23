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
package org.apache.cxf.transport.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.servicelist.ServiceListGeneratorServlet;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class CXFSpringServlet extends HttpServlet {

    private ServletTransportFactory servletTransportFactory;
    private Bus bus;
    private ServletController controller;
    
    public CXFSpringServlet() {
    }

    @Override
    public void init(ServletConfig sc) throws ServletException {
        super.init(sc);
        WebApplicationContext wac = WebApplicationContextUtils.
            getRequiredWebApplicationContext(sc.getServletContext());
        this.bus = wac.getBean("cxf", Bus.class);
        this.servletTransportFactory = wac.getBean(ServletTransportFactory.class);
        this.controller = createServletController(sc);
    }

    private ServletController createServletController(ServletConfig servletConfig) {
        HttpServlet serviceListGeneratorServlet = 
            new ServiceListGeneratorServlet(servletTransportFactory.getRegistry(), bus);
        ServletController newController =
            new ServletController(servletTransportFactory.getRegistry(),
                                  servletConfig,
                                  serviceListGeneratorServlet);        
        return newController;
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        try {
            BusFactory.setThreadDefaultBus(bus);
            controller.invoke(request, response);
        } finally {
            BusFactory.setThreadDefaultBus(null);
        }
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }

}
