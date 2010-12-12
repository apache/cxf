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

import java.io.IOException;
import java.util.Set;
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
import org.apache.cxf.transport.servlet.AbstractServletController;
import org.apache.cxf.transports.http.QueryHandler;
import org.apache.cxf.transports.http.QueryHandlerRegistry;
import org.apache.cxf.wsdl.http.AddressType;

public class OsgiServletController extends AbstractServletController {
    private static final Logger LOG = LogUtils.getL7dLogger(OsgiServlet.class);

    public OsgiServletController(ServletConfig config, 
                                 DestinationRegistry destinationRegistry, 
                                 HttpServlet serviceListGenerator) {
        super(config, destinationRegistry, serviceListGenerator);
    }

    private synchronized void updateDests(HttpServletRequest request) {
        if (disableAddressUpdates) {
            return;
        }
        String base = forcedBaseAddress == null ? getBaseURL(request) : forcedBaseAddress;

        Set<String> paths = destinationRegistry.getDestinationsPaths();
        for (String path : paths) {
            AbstractHTTPDestination d2 = destinationRegistry.getDestinationForPath(path);
            String ad = d2.getEndpointInfo().getAddress();
            if (ad.equals(path)) {
                d2.getEndpointInfo().setAddress(base + path);
                if (d2.getEndpointInfo().getExtensor(AddressType.class) != null) {
                    d2.getEndpointInfo().getExtensor(AddressType.class).setLocation(base + path);
                }
            }
        }
    }

    public void invoke(HttpServletRequest request, HttpServletResponse res) throws ServletException {
        try {
            String address = request.getPathInfo() == null ? "" : request.getPathInfo();
            AbstractHTTPDestination d = destinationRegistry.getDestinationForPath(address);

            if (d == null) {
                if (!isHideServiceList && (request.getRequestURI().endsWith(serviceListRelativePath)
                    || request.getRequestURI().endsWith(serviceListRelativePath + "/"))
                    || StringUtils.isEmpty(request.getPathInfo())
                    || "/".equals(request.getPathInfo())) {
                    updateDests(request);
                    serviceListGenerator.service(request, res);
                } else {
                    d = destinationRegistry.checkRestfulRequest(address);
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
                    } else if ("/".equals(address) || address.length() == 0) {
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


}
