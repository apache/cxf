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

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.UrlUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transports.http.QueryHandler;
import org.apache.cxf.transports.http.QueryHandlerRegistry;
import org.apache.cxf.wsdl.WSDLLibrary;
import org.apache.cxf.wsdl.http.AddressType;

public abstract class AbstractServletController {
    protected static final String DEFAULT_LISTINGS_CLASSIFIER = "/services";
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractServletController.class);
    
    protected boolean isHideServiceList;
    protected boolean disableAddressUpdates;
    protected String forcedBaseAddress;
    protected String serviceListStyleSheet;
    protected String title;
    protected String serviceListRelativePath = DEFAULT_LISTINGS_CLASSIFIER;
    protected ServletConfig servletConfig;
    protected DestinationRegistry destinationRegistry;
    protected HttpServlet serviceListGenerator;

    protected AbstractServletController(ServletConfig config, 
                                        DestinationRegistry destinationRegistry,
                                        HttpServlet serviceListGenerator) {
        this.servletConfig = config;
        this.destinationRegistry = destinationRegistry;
        this.serviceListGenerator = serviceListGenerator;
        init();
    }
    
    public void setHideServiceList(boolean generate) {
        isHideServiceList = generate;
    }
    
    public void setServiceListRelativePath(String relativePath) {
        serviceListRelativePath = relativePath;
    }
    
    public void setDisableAddressUpdates(boolean noupdates) {
        disableAddressUpdates = noupdates;
    }
    public void setForcedBaseAddress(String s) {
        forcedBaseAddress = s;
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
    
    protected synchronized void updateDests(HttpServletRequest request, boolean force) {
        
        String base = forcedBaseAddress == null ? getBaseURL(request) : forcedBaseAddress;
                
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
                    synchronized (d2) {
                        d2.getEndpointInfo().setAddress(base + path);
                        if (WSDLLibrary.isAvailable() 
                            && d2.getEndpointInfo().getExtensor(AddressType.class) != null) {
                            d2.getEndpointInfo().getExtensor(AddressType.class).setLocation(base + path);
                        }
                    }
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
    
    protected String getBaseURL(HttpServletRequest request) {
        String reqPrefix = request.getRequestURL().toString();        
        String pathInfo = request.getPathInfo() == null ? "" : request.getPathInfo();
        //fix for CXF-898
        if (!"/".equals(pathInfo) || reqPrefix.endsWith("/")) {
            // needs to be done given that pathInfo is decoded
            // TODO : it's unlikely servlet path will contain encoded values so we're most 
            // likely safe however we need to ensure if it happens then this code works properly too
            reqPrefix = UrlUtils.pathDecode(reqPrefix);
            // pathInfo drops matrix parameters attached to a last path segment
            int offset = 0;
            int index = getMatrixParameterIndex(reqPrefix, pathInfo);
            if (index >= pathInfo.length()) {
                offset = reqPrefix.length() - index;
            }
            reqPrefix = reqPrefix.substring(0, reqPrefix.length() - pathInfo.length() - offset);
        }
        return reqPrefix;
    }
    
    private int getMatrixParameterIndex(String reqPrefix, String pathInfo) {
        int index = reqPrefix.lastIndexOf(';');
        int lastIndex = -1;
        while (index >= pathInfo.length()) {
            lastIndex = index;
            reqPrefix = reqPrefix.substring(0, index);
            if (reqPrefix.endsWith(pathInfo)) {
                break;
            }
            index = reqPrefix.lastIndexOf(';');
        }
        return lastIndex;
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
