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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.transports.http.QueryHandler;
import org.apache.cxf.transports.http.QueryHandlerRegistry;
import org.apache.cxf.wsdl.http.AddressType;

public class ServletController extends AbstractServletController {
    
    private static final Logger LOG = LogUtils.getL7dLogger(ServletController.class);
    
    private ServletTransportFactory transport;
    private ServletContext servletContext;
    private ServletConfig servletConfig;
    private Bus bus;
    private volatile String lastBase = "";
    
    public ServletController(ServletTransportFactory df,
                             ServletConfig config,
                             ServletContext context, 
                             Bus b) {
        super(config);
        this.transport = df;
        this.servletConfig = config;
        this.servletContext = context;
        this.bus = b;
        init();
    }
    
    ServletController() {
    }
        
    String getLastBaseURL() {
        return lastBase;
    }
    
    protected synchronized void updateDests(HttpServletRequest request) {
        if (disableAddressUpdates) {
            return;
        }
        String base = forcedBaseAddress == null ? getBaseURL(request) : forcedBaseAddress;
                
        if (base.equals(lastBase)) {
            return;
        }
        Set<String> paths = transport.getDestinationsPaths();
        for (String path : paths) {
            ServletDestination d2 = transport.getDestinationForPath(path);
            String ad = d2.getEndpointInfo().getAddress();
            if (ad == null 
                && d2.getAddress() != null
                && d2.getAddress().getAddress() != null) {
                ad = d2.getAddress().getAddress().getValue();
            }
            if (ad != null 
                && (ad.equals(path)
                || ad.equals(lastBase + path))) {
                d2.getEndpointInfo().setAddress(base + path);
                if (d2.getEndpointInfo().getExtensor(AddressType.class) != null) {
                    d2.getEndpointInfo().getExtensor(AddressType.class).setLocation(base + path);
                }
            }
        }
        lastBase = base;
    }

    public void invoke(HttpServletRequest request, HttpServletResponse res) throws ServletException {
        try {
            EndpointInfo ei = new EndpointInfo();
            
            String address = request.getPathInfo() == null ? "" : request.getPathInfo();
            ei.setAddress(address);
            
            ServletDestination d = getDestination(ei.getAddress());
            if (d == null) {
                if (!isHideServiceList && (request.getRequestURI().endsWith(serviceListRelativePath)
                    || request.getRequestURI().endsWith(serviceListRelativePath + "/")
                    || StringUtils.isEmpty(request.getPathInfo())
                    || "/".equals(request.getPathInfo()))) {
                    updateDests(request);
                    
                    if (request.getParameter("stylesheet") != null) {
                        renderStyleSheet(request, res);
                    } else if ("false".equals(request.getParameter("formatted"))) {
                        generateUnformattedServiceList(request, res);
                    } else {
                        generateServiceList(request, res);
                    }
                } else {
                    d = checkRestfulRequest(request);
                    if (d == null || d.getMessageObserver() == null) {                        
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
                } else if ("/".equals(address) || address.length() == 0) {
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
    
    protected ServletDestination getDestination(String address) {
        return (ServletDestination)transport.getDestinationForPath(address, true);
    }
    
    protected ServletDestination checkRestfulRequest(HttpServletRequest request) throws IOException {        
        
        String address = request.getPathInfo() == null ? "" : request.getPathInfo();
        
        int len = -1;
        ServletDestination ret = null;
        for (String path : transport.getDestinationsPaths()) {           
            if (address.startsWith(path)
                && path.length() > len) {
                ret = transport.getDestinationForPath(path);
                len = path.length();
            }
        }
        return ret; 
    }
    
    @SuppressWarnings("unchecked")
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
        if (title != null) {
            response.getWriter().write("<title>" + title + "</title>");
        } else {
            response.getWriter().write("<title>CXF - Service list</title>");
        }
        response.getWriter().write("</head><body>");
        
        List<ServletDestination> destinations = getServletDestinations();
            
        if (destinations.size() > 0) {
            List<String> privateEndpoints = 
                (List<String>)bus.getProperty("org.apache.cxf.private.endpoints");
            writeSOAPEndpoints(response, destinations, privateEndpoints);
            writeRESTfulEndpoints(response, destinations, privateEndpoints);
        } else {
            response.getWriter().write("<span class=\"heading\">No services have been found.</span>");
        }
        
        response.getWriter().write("</body></html>");
    }

    private void writeSOAPEndpoints(HttpServletResponse response, List<ServletDestination> destinations,
                                    List<String> privateEndpoints)
        throws IOException {
        response.getWriter().write("<span class=\"heading\">Available SOAP services:</span><br/>");
        response.getWriter().write("<table " + (serviceListStyleSheet == null
                ? "cellpadding=\"1\" cellspacing=\"1\" border=\"1\" width=\"100%\"" : "") + ">");
        for (ServletDestination sd : destinations) {
            
            if (null != sd.getEndpointInfo().getName() 
                && null != sd.getEndpointInfo().getInterface()
                && !isPrivate(sd.getEndpointInfo(), privateEndpoints)) {
                response.getWriter().write("<tr><td>");
                response.getWriter().write("<span class=\"porttypename\">"
                        + sd.getEndpointInfo().getInterface().getName().getLocalPart()
                        + "</span>");
                response.getWriter().write("<ul>");
                for (OperationInfo oi : sd.getEndpointInfo().getInterface().getOperations()) {
                    if (oi.getProperty("operation.is.synthetic") != Boolean.TRUE) {
                        response.getWriter().write("<li>" + oi.getName().getLocalPart() + "</li>");
                    }
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
    
    
    private void writeRESTfulEndpoints(HttpServletResponse response, List<ServletDestination> destinations,
                                       List<String> privateEndpoints)
        throws IOException {
        
        List<ServletDestination> restfulDests = new ArrayList<ServletDestination>();
        for (ServletDestination sd : destinations) {
            // use some more reasonable check - though this one seems to be the only option at the moment
            if (null == sd.getEndpointInfo().getInterface() 
                && !isPrivate(sd.getEndpointInfo(), privateEndpoints)) {
                restfulDests.add(sd);
            }
        }
        if (restfulDests.size() == 0) {
            return;
        }
        
        response.getWriter().write("<span class=\"heading\">Available RESTful services:</span><br/>");
        response.getWriter().write("<table " + (serviceListStyleSheet == null
                ? "cellpadding=\"1\" cellspacing=\"1\" border=\"1\" width=\"100%\"" : "") + ">");
        for (ServletDestination sd : restfulDests) {
            response.getWriter().write("<tr><td>");
            String address = sd.getEndpointInfo().getAddress();
            response.getWriter().write("<span class=\"field\">Endpoint address:</span> "
                    + "<span class=\"value\">" + address + "</span>");
            response.getWriter().write("<br/><span class=\"field\">WADL :</span> "
                    + "<a href=\"" + address + "?_wadl&_type=xml\">"
                    + address + "?_wadl&type=xml" + "</a>");
            response.getWriter().write("</td></tr>");
        }
        response.getWriter().write("</table>");
    }
    
    private boolean isPrivate(EndpointInfo ei, List<String> privateAddresses) {
        if (privateAddresses != null) {
            for (String s : privateAddresses) {
                if (ei.getAddress().endsWith(s)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private void renderStyleSheet(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        response.setContentType("text/css; charset=UTF-8");

        URL url = this.getClass().getResource("servicelist.css");
        if (url != null) {
            IOUtils.copy(url.openStream(), response.getOutputStream());
        }
    }

    private List<ServletDestination> getServletDestinations() {
        List<ServletDestination> destinations = new LinkedList<ServletDestination>(
                transport.getDestinations());
        Collections.sort(destinations, new Comparator<ServletDestination>() {
            public int compare(ServletDestination o1, ServletDestination o2) {
                if (o1.getEndpointInfo().getInterface() == null) {
                    return -1;
                }
                if (o2.getEndpointInfo().getInterface() == null) {
                    return 1;
                }
                return o1.getEndpointInfo().getInterface().getName()
                        .getLocalPart().compareTo(
                                o2.getEndpointInfo().getInterface().getName()
                                        .getLocalPart());
            }
        });

        return destinations;
    }

    protected void generateUnformattedServiceList(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        response.setContentType("text/plain; charset=UTF-8");

        List<ServletDestination> destinations = getServletDestinations();
        if (destinations.size() > 0) {
            writeUnformattedSOAPEndpoints(response, destinations, request.getParameter("wsdlList"));
            writeUnformattedRESTfulEndpoints(response, destinations);
        } else {
            response.getWriter().write("No services have been found.");
        }
    }
    
    private void writeUnformattedSOAPEndpoints(HttpServletResponse response,
                                               List<ServletDestination> destinations,
                                               Object renderParam) 
        throws IOException {
        boolean renderWsdlList = "true".equals(renderParam);
        
        for (ServletDestination sd : destinations) {
            
            if (null != sd.getEndpointInfo().getInterface()) {
            
                String address = sd.getEndpointInfo().getAddress();
                response.getWriter().write(address);
                
                if (renderWsdlList) {
                    response.getWriter().write("?wsdl");
                }
                response.getWriter().write('\n');
            }
        }
        response.getWriter().write('\n');
    }
    
    private void writeUnformattedRESTfulEndpoints(HttpServletResponse response,
                                                  List<ServletDestination> destinations) 
        throws IOException {
        for (ServletDestination sd : destinations) {
            if (null == sd.getEndpointInfo().getInterface()) {
                String address = sd.getEndpointInfo().getAddress();
                response.getWriter().write(address + "?_wadl&_type=xml");
                response.getWriter().write('\n');
            }
        }
    }
    
    protected void generateNotFound(HttpServletRequest request, HttpServletResponse res) throws IOException {
        res.setStatus(404);
        res.setContentType("text/html");
        res.getWriter().write("<html><body>No service was found.</body></html>");
    }

    public void invokeDestination(final HttpServletRequest request, HttpServletResponse response,
                                  ServletDestination d) throws ServletException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Service http request on thread: " + Thread.currentThread());
        }

        try {
            d.invoke(servletConfig, servletContext, request, response);
        } catch (IOException e) {
            throw new ServletException(e);
        } finally {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Finished servicing http request on thread: " + Thread.currentThread());
            }
        }

    }
    
    private void init() {
        transport.setServletController(this);
    }
    
}
