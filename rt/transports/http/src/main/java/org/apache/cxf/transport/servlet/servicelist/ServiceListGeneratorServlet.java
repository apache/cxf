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
package org.apache.cxf.transport.servlet.servicelist;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.servlet.ServletController;

public class ServiceListGeneratorServlet extends HttpServlet {
    private static final long serialVersionUID = -113918058557537996L;
    private DestinationRegistry destinationRegistry;
    private Bus bus;
    private String serviceListStyleSheet;
    private String title = "CXF - Service list";
    private boolean showForeignContexts = true;

    public ServiceListGeneratorServlet(DestinationRegistry destinationRegistry, Bus bus) {
        this.destinationRegistry = destinationRegistry;
        this.bus = bus != null ? bus : BusFactory.getDefaultBus(false);
    }

    public void setServiceListStyleSheet(String serviceListStyleSheet) {
        this.serviceListStyleSheet = serviceListStyleSheet;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public void service(HttpServletRequest request,
                        HttpServletResponse response) throws ServletException, IOException {
        final Object isAuthServiceList = request.getAttribute(ServletController.AUTH_SERVICE_LIST);
        if (isAuthServiceList != null && Boolean.valueOf(isAuthServiceList.toString())) {
            String authServiceListRealm = (String)request.getAttribute(ServletController.AUTH_SERVICE_LIST_REALM);
            ServiceListJAASAuthenticator authenticator = new ServiceListJAASAuthenticator();
            authenticator.setRealm(authServiceListRealm);
            if (!authenticator.authenticate(request, response)) {
                return;
            }
            request.removeAttribute(ServletController.AUTH_SERVICE_LIST);
            request.removeAttribute(ServletController.AUTH_SERVICE_LIST_REALM);
        }
        if (request.getParameter("stylesheet") != null) {
            renderStyleSheet(request, response);
            return;
        }

        if ("HEAD".equals(request.getMethod())) {
            return;
        }
        synchronized (this) {
            if (bus == null) {
                bus = BusFactory.getDefaultBus(false);
            }
        }
        final ServiceListWriter serviceListWriter;
        if ("false".equals(request.getParameter("formatted"))) {
            boolean renderWsdlList = "true".equals(request.getParameter("wsdlList"));
            serviceListWriter = new UnformattedServiceListWriter(renderWsdlList, bus);
        } else {
            String styleSheetPath;
            if (serviceListStyleSheet != null) {
                styleSheetPath = request.getContextPath() + "/" + serviceListStyleSheet;
            } else {
                styleSheetPath = "";
                String contextPath = request.getContextPath();
                if (contextPath != null) {
                    styleSheetPath += contextPath;
                }
                String servletPath = request.getServletPath();
                if (servletPath != null) {
                    styleSheetPath += servletPath;
                }
                String pathInfo = request.getPathInfo();
                if (pathInfo != null) {
                    styleSheetPath += pathInfo;
                }

                if (!styleSheetPath.endsWith("/")) {
                    styleSheetPath += "/";
                }
                styleSheetPath += "?stylesheet=1";
            }
            serviceListWriter =
                new FormattedServiceListWriter(styleSheetPath, title, showForeignContexts, bus);

        }
        response.setContentType(serviceListWriter.getContentType());
        final Object basePath = request.getAttribute(Message.BASE_PATH);
        final AbstractDestination[] destinations = destinationRegistry.getSortedDestinations();
        final List<String> privateEndpoints = bus != null
            ? CastUtils.cast((List<?>) bus.getProperty("org.apache.cxf.private.endpoints"))
            : Collections.emptyList();
        serviceListWriter.writeServiceList(response.getWriter(),
                                           basePath == null ? null : basePath.toString(),
                                           getSOAPEndpoints(destinations, privateEndpoints),
                                           getRestEndpoints(destinations, privateEndpoints));
    }


    private static boolean isPrivate(EndpointInfo ei, List<String> privateEndpoints) {
        if (privateEndpoints != null) {
            for (String s : privateEndpoints) {
                if (ei.getAddress().endsWith(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static AbstractDestination[] getSOAPEndpoints(AbstractDestination[] destinations,
                                                   List<String> privateEndpoints) {
        List<AbstractDestination> soapEndpoints = new ArrayList<>();
        for (AbstractDestination sd : destinations) {
            if (null != sd.getEndpointInfo().getName() && null != sd.getEndpointInfo().getInterface()
                && !isPrivate(sd.getEndpointInfo(), privateEndpoints)) {
                soapEndpoints.add(sd);
            }
        }
        return soapEndpoints.toArray(new AbstractDestination[0]);
    }

    private static AbstractDestination[] getRestEndpoints(AbstractDestination[] destinations,
                                                   List<String> privateEndpoints) {
        List<AbstractDestination> restfulDests = new ArrayList<>();
        for (AbstractDestination sd : destinations) {
            // use some more reasonable check - though this one seems to be the only option at the moment
            if (null == sd.getEndpointInfo().getInterface()
                && !isPrivate(sd.getEndpointInfo(), privateEndpoints)) {
                restfulDests.add(sd);
            }
        }
        return restfulDests.toArray(new AbstractDestination[0]);
    }


    private void renderStyleSheet(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        response.setContentType("text/css; charset=UTF-8");
        URL url = this.getClass().getResource("servicelist.css");
        if (url != null) {
            try (InputStream inputStream = url.openStream()) {
                IOUtils.copy(inputStream, response.getOutputStream());
            }
        }
    }

    public void init(ServletConfig servletConfig) {
        String configServiceListStyleSheet = servletConfig.getInitParameter("service-list-stylesheet");
        if (!StringUtils.isEmpty(configServiceListStyleSheet)) {
            this.serviceListStyleSheet = configServiceListStyleSheet;
        }
        String configTitle = servletConfig.getInitParameter("service-list-title");
        if (!StringUtils.isEmpty(configTitle)) {
            this.title = configTitle;
        }

        String showAllContexts = servletConfig.getInitParameter("service-list-all-contexts");
        if (!StringUtils.isEmpty(showAllContexts)) {
            this.showForeignContexts = Boolean.valueOf(showAllContexts);
        }
    }

    public ServletConfig getServletConfig() {
        return null;
    }

    public String getServletInfo() {
        return null;
    }

    public void destroy() {
    }

}
