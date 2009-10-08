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

import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.cxf.BusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.resource.ResourceManager;

public class CXFNonSpringServlet extends AbstractCXFServlet {

    public static Logger getLogger() {
        return LogUtils.getL7dLogger(CXFNonSpringServlet.class);
    }

    @Override
    public void loadBus(ServletConfig servletConfig) throws ServletException {
        loadBusNoConfig(servletConfig);
        // You could add the endpoint publish codes here
    }

    private void loadBusNoConfig(ServletConfig servletConfig) throws ServletException {

        if (bus == null) {
            LOG.info("LOAD_BUS_WITHOUT_APPLICATION_CONTEXT");
            bus = BusFactory.newInstance().createBus();
        }
        ResourceManager resourceManager = bus.getExtension(ResourceManager.class);
        resourceManager.addResourceResolver(new ServletContextResourceResolver(
                                               servletConfig.getServletContext()));

        replaceDestinationFactory();
        // Set up the ServletController
        controller = createServletController(servletConfig);

    }

}
