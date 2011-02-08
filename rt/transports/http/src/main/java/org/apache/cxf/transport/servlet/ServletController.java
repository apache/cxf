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
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transports.http.QueryHandler;
import org.apache.cxf.transports.http.QueryHandlerRegistry;
import org.apache.cxf.wsdl.WSDLLibrary;
import org.apache.cxf.wsdl.http.AddressType;

public class ServletController extends AbstractServletController {
    
    private static final Logger LOG = LogUtils.getL7dLogger(ServletController.class);  
    private volatile String lastBase = "";
    
    public ServletController(DestinationRegistry destinationRegistry,
                             ServletConfig config, 
                             HttpServlet serviceListGeneratorServlet) {
        super(config, destinationRegistry, serviceListGeneratorServlet);
    }
        
    String getLastBaseURL() {
        return lastBase;
    }
    
    protected synchronized void updateDests(HttpServletRequest request) {
        
        String base = forcedBaseAddress == null ? getBaseURL(request) : forcedBaseAddress;
                
        if (base.equals(lastBase)) {
            return;
        }
        Set<String> paths = destinationRegistry.getDestinationsPaths();
        for (String path : paths) {
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
                && (ad.equals(path)
                || ad.equals(lastBase + path))) {
                if (disableAddressUpdates) {
                    request.setAttribute("org.apache.cxf.transport.endpoint.address", base + path);
                } else {
                    d2.getEndpointInfo().setAddress(base + path);
                    if (WSDLLibrary.isAvailable() 
                        && d2.getEndpointInfo().getExtensor(AddressType.class) != null) {
                        d2.getEndpointInfo().getExtensor(AddressType.class).setLocation(base + path);
                    }
                }
            }
        }
        if (disableAddressUpdates) {
            return;
        }
        lastBase = base;
    }

    public void invoke(HttpServletRequest request, HttpServletResponse res) 
        throws ServletException {
        try {
            EndpointInfo ei = new EndpointInfo();
            
            String address = request.getPathInfo() == null ? "" : request.getPathInfo();
            ei.setAddress(address);
            
            AbstractHTTPDestination d = destinationRegistry.getDestinationForPath(ei.getAddress(), true);
            if (d == null) {
                if (!isHideServiceList && (request.getRequestURI().endsWith(serviceListRelativePath)
                    || request.getRequestURI().endsWith(serviceListRelativePath + "/")
                    || StringUtils.isEmpty(request.getPathInfo())
                    || "/".equals(request.getPathInfo()))) {
                    updateDests(request);
                    serviceListGenerator.service(request, res);
                } else {
                    d = destinationRegistry.checkRestfulRequest(address);
                    if (d == null) {                        
                        LOG.warning("Can't find the request for " 
                                    + request.getRequestURL() + "'s Observer ");
                        generateNotFound(request, res);
                    }  else { // the request should be a restful service request
                        updateDests(request);
                        invokeDestination(request, res, d);
                    }
                }
            } else {
                ei = d.getEndpointInfo();
                Bus bus = d.getBus();
                if ("GET".equals(request.getMethod())
                    && null != request.getQueryString() 
                    && request.getQueryString().length() > 0
                    && bus.getExtension(QueryHandlerRegistry.class) != null) {                    
                    
                    String ctxUri = request.getPathInfo();
                    String baseUri = request.getRequestURL().toString() 
                        + "?" + request.getQueryString();
                    // update the EndPoint Address with request url
                    updateDests(request);
                    
                    for (QueryHandler qh : bus.getExtension(QueryHandlerRegistry.class).getHandlers()) {
                        if (qh.isRecognizedQuery(baseUri, ctxUri, ei)) {
                            
                            res.setContentType(qh.getResponseContentType(baseUri, ctxUri));
                            OutputStream out = res.getOutputStream();
                            try {
                                qh.writeResponse(baseUri, ctxUri, ei, out);
                            } catch (Exception e) {
                                LogUtils.log(LOG, Level.WARNING,
                                             qh.getClass().getName() 
                                             + " Exception caught writing response.",
                                             e);
                                throw new ServletException(e);                                
                            }
                            out.flush();
                            return;
                        }   
                    }
                } else {
                    updateDests(request);
                }
                
                invokeDestination(request, res, d);
            }
        } catch (Fault ex) {
            if (ex.getCause() instanceof RuntimeException) {
                throw (RuntimeException)ex.getCause(); 
            } else {
                throw new ServletException(ex.getCause());
            }
        } catch (IOException e) {
            throw new ServletException(e);
        } 
    }

}
