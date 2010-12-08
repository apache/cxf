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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.servlet.AbstractServletController;
import org.apache.cxf.transports.http.QueryHandler;
import org.apache.cxf.transports.http.QueryHandlerRegistry;
import org.apache.cxf.wsdl.http.AddressType;

public class OsgiServletController extends AbstractServletController {
    private static final Logger LOG = LogUtils.getL7dLogger(OsgiServlet.class);
      
    private DestinationRegistry destinationRegistry;
    public OsgiServletController(DestinationRegistry destinationRegistry, ServletConfig config) {
        super(config);
        this.destinationRegistry = destinationRegistry;
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
                    generateServiceList(request, res);
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
                Bus bus = ((OsgiDestination)d).getBus();
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
                    Iterable<QueryHandler> queryHandlers = bus.getExtension(QueryHandlerRegistry.class)
                        .getHandlers();
                    if (!StringUtils.isEmpty(request.getQueryString()) && queryHandlers != null) {
                        
                        // update the EndPoint Address with request url
                        if ("GET".equals(request.getMethod())) {
                            updateDests(request);
                        }
                        
                        String ctxUri = request.getPathInfo();
                        String baseUri = request.getRequestURL().toString()
                            + "?" + request.getQueryString();

                        QueryHandler selectedHandler = findQueryHandler(queryHandlers, ei, ctxUri, baseUri);
                        
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

    private QueryHandler findQueryHandler(Iterable<QueryHandler> handlers, EndpointInfo ei, String ctxUri,
                                          String baseUri) {
        for (QueryHandler qh : handlers) {
            if (qh.isRecognizedQuery(baseUri, ctxUri, ei)) {
                return qh;
            }
        }
        return null;
    }

    private void respondUsingQueryHandler(QueryHandler selectedHandler, HttpServletResponse res,
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

    protected void generateServiceList(HttpServletRequest request, HttpServletResponse response)
        throws IOException {        
        response.setContentType("text/html; charset=UTF-8");        
        
        response.getWriter().write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" " 
                + "\"http://www.w3.org/TR/html4/loose.dtd\">");
        response.getWriter().write("<HTML><HEAD>");
        if (serviceListStyleSheet != null) {
            response.getWriter().write(
                    "<LINK type=\"text/css\" rel=\"stylesheet\" href=\"" 
                    + request.getContextPath() + "/" + serviceListStyleSheet + "\">");
        } else {
            response.getWriter().write(
                                       "<LINK type=\"text/css\" rel=\"stylesheet\" href=\"" 
                                       + request.getRequestURI() + "/?stylesheet=1\">");            
        }
        response.getWriter().write("<meta http-equiv=content-type content=\"text/html; charset=UTF-8\">");
        response.getWriter().write("<title>CXF - Service list</title>");
        response.getWriter().write("</head><body>");
        
        Collection<AbstractHTTPDestination> destinations = destinationRegistry.getDestinations();
            
        if (destinations.size() > 0) {
            writeSOAPEndpoints(response, destinations);
            writeRESTfulEndpoints(response, destinations);
        } else {
            response.getWriter().write("<span class=\"heading\">No services have been found.</span>");
        }
        
        response.getWriter().write("</body></html>");
    }

    private void writeSOAPEndpoints(HttpServletResponse response, 
                                    Collection<AbstractHTTPDestination> destinations)
        throws IOException {
        response.getWriter().write("<span class=\"heading\">Available SOAP services:</span><br/>");
        response.getWriter().write("<table " + (serviceListStyleSheet == null
                ? "cellpadding=\"1\" cellspacing=\"1\" border=\"1\" width=\"100%\"" : "") + ">");
        for (AbstractHTTPDestination sd : destinations) {
            if (null != sd.getEndpointInfo().getName() 
                && null != sd.getEndpointInfo().getInterface()) {
                response.getWriter().write("<tr><td>");
                response.getWriter().write("<span class=\"porttypename\">"
                        + sd.getEndpointInfo().getInterface().getName().getLocalPart()
                        + "</span>");
                response.getWriter().write("<ul>");
                for (OperationInfo oi : sd.getEndpointInfo().getInterface().getOperations()) {
                    response.getWriter().write("<li>" + oi.getName().getLocalPart() + "</li>");
                }
                response.getWriter().write("</ul>");
                response.getWriter().write("</td><td>");
                String address = sd.getEndpointInfo().getAddress();
                response.getWriter().write("<span class=\"field\">Endpoint address:</span> "
                        + "<span class=\"value\">" + address + "</span>");
                response.getWriter().write("<br/><span class=\"field\">WSDL :</span> "
                        + "<a href=\"" + address + "?wsdl\">"
                        + sd.getEndpointInfo().getService().getName() + "</a>");
                response.getWriter().write("<br/><span class=\"field\">Target namespace:</span> "
                        + "<span class=\"value\">" 
                        + sd.getEndpointInfo().getService().getTargetNamespace() + "</span>");
                response.getWriter().write("</td></tr>");
            }    
        }
        response.getWriter().write("</table><br/><br/>");
    }
    
    
    private void writeRESTfulEndpoints(HttpServletResponse response, 
                                       Collection<AbstractHTTPDestination> destinations)
        throws IOException {
        
        List<AbstractHTTPDestination> restfulDests = new ArrayList<AbstractHTTPDestination>();
        for (AbstractHTTPDestination sd : destinations) {
            // use some more reasonable check - though this one seems to be the only option at the moment
            if (null == sd.getEndpointInfo().getInterface()) {
                restfulDests.add(sd);
            }
        }
        if (restfulDests.size() == 0) {
            return;
        }
        
        response.getWriter().write("<span class=\"heading\">Available RESTful services:</span><br/>");
        response.getWriter().write("<table " + (serviceListStyleSheet == null
                ? "cellpadding=\"1\" cellspacing=\"1\" border=\"1\" width=\"100%\"" : "") + ">");
        for (AbstractHTTPDestination sd : destinations) {
            if (null == sd.getEndpointInfo().getInterface()) {
                response.getWriter().write("<tr><td>");
                String address = sd.getEndpointInfo().getAddress();
                response.getWriter().write("<span class=\"field\">Endpoint address:</span> "
                        + "<span class=\"value\">" + address + "</span>");
                response.getWriter().write("<br/><span class=\"field\">WADL :</span> "
                        + "<a href=\"" + address + "?_wadl&_type=xml\">"
                        + address + "?_wadl&type=xml" + "</a>");
                response.getWriter().write("</td></tr>");
            }    
        }
        response.getWriter().write("</table>");
    }
    
    protected void generateNotFound(HttpServletRequest request, HttpServletResponse res) throws IOException {
        res.setStatus(404);
        res.setContentType("text/html");
        res.getWriter().write("<html><body>No service was found.</body></html>");
    }

}
