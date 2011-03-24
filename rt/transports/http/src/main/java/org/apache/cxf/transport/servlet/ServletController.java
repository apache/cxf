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
import java.io.OutputStream;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transports.http.QueryHandler;
import org.apache.cxf.transports.http.QueryHandlerRegistry;

public class ServletController {
    protected static final String DEFAULT_LISTINGS_CLASSIFIER = "/services";
    private static final Logger LOG = LogUtils.getL7dLogger(ServletController.class);
    
    protected boolean isHideServiceList;
    protected boolean disableAddressUpdates;
    protected String forcedBaseAddress;
    protected String serviceListStyleSheet;
    protected String title;
    protected String serviceListRelativePath = DEFAULT_LISTINGS_CLASSIFIER;
    protected ServletConfig servletConfig;
    protected DestinationRegistry destinationRegistry;
    protected HttpServlet serviceListGenerator;

    public ServletController(DestinationRegistry destinationRegistry,
                                ServletConfig config, 
                                HttpServlet serviceListGenerator) {
        this.servletConfig = config;
        this.destinationRegistry = destinationRegistry;
        this.serviceListGenerator = serviceListGenerator;
        init();
    }

    public void setServiceListRelativePath(String relativePath) {
        serviceListRelativePath = relativePath;
    }

    public void setServiceListStyleSheet(String serviceListStyleSheet) {
        this.serviceListStyleSheet = serviceListStyleSheet;
    }
    public void setTitle(String t) {
        title = t;
    }
    
    protected synchronized void updateDests(HttpServletRequest request) {
        updateDests(request, false);
    }
    
    protected void updateDests(HttpServletRequest request, boolean force) {
        
        String base = forcedBaseAddress == null ? BaseUrlHelper.getBaseURL(request) : forcedBaseAddress;
                
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "/";
        }
        
        Set<String> paths = destinationRegistry.getDestinationsPaths();
        for (String path : paths) {
            if (!force && pathInfo != null && !pathInfo.startsWith(path)) {
                continue;
            }
            AbstractHTTPDestination d2 = destinationRegistry.getDestinationForPath(path);
            String ad = d2.getEndpointInfo().getAddress();
            if (ad == null 
                && d2.getAddress() != null
                && d2.getAddress().getAddress() != null) {
                ad = d2.getAddress().getAddress().getValue();
                if (ad == null) {
                    ad = "/";
                }
            }
            if (ad != null 
                && (ad.equals(path))) {
                if (disableAddressUpdates) {
                    request.setAttribute("org.apache.cxf.transport.endpoint.address", 
                                         base + path);
                } else {
                    BaseUrlHelper.setAddress(d2, base + path);
                }
            }
        }
    }
    
    private void init() {
        if (servletConfig == null) {
            return;
        }
        
        String hideServiceList = servletConfig.getInitParameter("hide-service-list-page");
        if (!StringUtils.isEmpty(hideServiceList)) {
            this.isHideServiceList = Boolean.valueOf(hideServiceList);
        }
        String isDisableAddressUpdates = servletConfig.getInitParameter("disable-address-updates");
        if (!StringUtils.isEmpty(isDisableAddressUpdates)) {
            this.disableAddressUpdates = Boolean.valueOf(isDisableAddressUpdates);
        }
        String isForcedBaseAddress = servletConfig.getInitParameter("base-address");
        if (!StringUtils.isEmpty(isForcedBaseAddress)) {
            this.forcedBaseAddress = isForcedBaseAddress;
        }
        try {
            serviceListGenerator.init(servletConfig);
        } catch (ServletException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        String serviceListPath = servletConfig.getInitParameter("service-list-path");
        if (!StringUtils.isEmpty(serviceListPath)) {
            this.serviceListRelativePath = serviceListPath;
        }
    }
    
    public void invoke(HttpServletRequest request, HttpServletResponse res) throws ServletException {
        try {
            String pathInfo = request.getPathInfo() == null ? "" : request.getPathInfo();
            AbstractHTTPDestination d = destinationRegistry.getDestinationForPath(pathInfo, true);

            if (d == null) {
                if (!isHideServiceList && (request.getRequestURI().endsWith(serviceListRelativePath)
                    || request.getRequestURI().endsWith(serviceListRelativePath + "/")
                    || StringUtils.isEmpty(pathInfo)
                    || "/".equals(pathInfo))) {
                    updateDests(request, true);
                    serviceListGenerator.service(request, res);
                } else {
                    d = destinationRegistry.checkRestfulRequest(pathInfo);
                    if (d == null || d.getMessageObserver() == null) {
                        LOG.warning("Can't find the the request for "
                                    + request.getRequestURL() + "'s Observer ");
                        generateNotFound(request, res);
                    }  else { // the request should be a restful service request
                        updateDests(request);
                        invokeDestination(request, res, d);
                    }
                }
            } else {
                EndpointInfo ei = d.getEndpointInfo();
                Bus bus = d.getBus();
                ClassLoader orig = Thread.currentThread().getContextClassLoader();
                try {
                    ResourceManager manager = bus.getExtension(ResourceManager.class);
                    if (manager != null) {
                        ClassLoader loader = manager.resolveResource("", ClassLoader.class);
                        if (loader != null) {
                            //need to set the context classloader to the loader of the bundle
                            Thread.currentThread().setContextClassLoader(loader);
                        }
                    }
                    QueryHandlerRegistry queryHandlerRegistry = bus.getExtension(QueryHandlerRegistry.class);

                    if (!StringUtils.isEmpty(request.getQueryString()) && queryHandlerRegistry != null) {
                        
                        // update the EndPoint Address with request url
                        if ("GET".equals(request.getMethod())) {
                            updateDests(request);
                        }
                        
                        String ctxUri = request.getPathInfo();
                        String baseUri = request.getRequestURL().toString()
                            + "?" + request.getQueryString();

                        QueryHandler selectedHandler = 
                            findQueryHandler(queryHandlerRegistry, ei, ctxUri, baseUri);
                        
                        if (selectedHandler != null) {
                            respondUsingQueryHandler(selectedHandler, res, ei, ctxUri, baseUri);
                            return;
                        }
                    } else {
                        updateDests(request);
                    }
                    invokeDestination(request, res, d);
                } finally {
                    Thread.currentThread().setContextClassLoader(orig);
                }
                
            }
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }

    public void invokeDestination(final HttpServletRequest request, HttpServletResponse response,
                                  AbstractHTTPDestination d) throws ServletException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Service http request on thread: " + Thread.currentThread());
        }

        try {
            d.invoke(servletConfig, servletConfig.getServletContext(), request, response);
        } catch (IOException e) {
            throw new ServletException(e);
        } finally {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Finished servicing http request on thread: " + Thread.currentThread());
            }
        }
    }
    
    protected QueryHandler findQueryHandler(QueryHandlerRegistry queryHandlerRegistry, 
                                            EndpointInfo ei, 
                                            String ctxUri,
                                            String baseUri) {
        if (queryHandlerRegistry == null) {
            return null;
        }
        Iterable<QueryHandler> handlers = queryHandlerRegistry.getHandlers();
        for (QueryHandler qh : handlers) {
            if (qh.isRecognizedQuery(baseUri, ctxUri, ei)) {
                return qh;
            }
        }
        return null;
    }

    protected void respondUsingQueryHandler(QueryHandler selectedHandler, HttpServletResponse res,
                                          EndpointInfo ei, String ctxUri, String baseUri) throws IOException,
        ServletException {
        res.setContentType(selectedHandler.getResponseContentType(baseUri, ctxUri));
        OutputStream out = res.getOutputStream();
        try {
            selectedHandler.writeResponse(baseUri, ctxUri, ei, out);
            out.flush();
        } catch (Exception e) {
            LOG.warning(selectedHandler.getClass().getName()
                + " Exception caught writing response: "
                + e.getMessage());
            throw new ServletException(e);
        }
    }

    protected void generateNotFound(HttpServletRequest request, HttpServletResponse res) throws IOException {
        res.setStatus(404);
        res.setContentType("text/html");
        res.getWriter().write("<html><body>No service was found.</body></html>");
    }
    
}
