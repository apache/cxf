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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.transports.http.QueryHandler;
import org.apache.cxf.transports.http.QueryHandlerRegistry;
import org.xmlsoap.schemas.wsdl.http.AddressType;

public class ServletController {
    
    private static final Logger LOG = LogUtils.getL7dLogger(ServletController.class);

    private ServletTransportFactory transport;
    private AbstractCXFServlet cxfServlet;
    private String lastBase = "";
    private boolean isHideServiceList;
    private boolean disableAddressUpdates;
    private String forcedBaseAddress;
    private String serviceListStyleSheet;
 
    public ServletController(ServletTransportFactory df, AbstractCXFServlet servlet) {
        this.transport = df;
        this.cxfServlet = servlet;       
    }
    
    public void setHideServiceList(boolean generate) {
        isHideServiceList = generate;
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
    
    private synchronized void updateDests(HttpServletRequest request) {
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
            if (ad.equals(path)
                || ad.equals(lastBase + path)) {
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
            ServletDestination d = (ServletDestination)transport.getDestinationForPath(ei.getAddress());
            
            if (d == null) {
                if (request.getRequestURI().endsWith("/services")
                    || request.getRequestURI().endsWith("/services/")
                    || StringUtils.isEmpty(request.getPathInfo())
                    || "/".equals(request.getPathInfo())) {
                    updateDests(request);
                    generateServiceList(request, res);
                } else {
                    d = checkRestfulRequest(request);
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
                ei = d.getEndpointInfo();
                Bus bus = cxfServlet.getBus();
                if (null != request.getQueryString() 
                    && request.getQueryString().length() > 0
                    && bus.getExtension(QueryHandlerRegistry.class) != null) {                    
                    
                    String ctxUri = request.getPathInfo();
                    String baseUri = request.getRequestURL().toString() 
                        + "?" + request.getQueryString();
                    // update the EndPoint Address with request url
                    if ("GET".equals(request.getMethod())) {
                        updateDests(request);
                    }

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
                }
                
                invokeDestination(request, res, d);
            }
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }
    
    private ServletDestination checkRestfulRequest(HttpServletRequest request) throws IOException {        
        
        String address = request.getPathInfo() == null ? "" : request.getPathInfo();
        
        for (String path : transport.getDestinationsPaths()) {           
            if (address.startsWith(path)) {                
                return transport.getDestinationForPath(path);
            }
        }
        return null; 
    }
    
    private void generateServiceList(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        
        if (request.getParameter("stylesheet") != null) {
            URL url = this.getClass().getResource("servicelist.css");
            if (url != null) {
                IOUtils.copy(url.openStream(), response.getOutputStream());
            }
            return;
        }
        
        response.setContentType("text/html");        
        response.setCharacterEncoding("UTF-8");
        
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
        if (!isHideServiceList) {
            List<ServletDestination> destinations 
                = new LinkedList<ServletDestination>(transport.getDestinations());
            Collections.sort(destinations, new Comparator<ServletDestination>() {
                public int compare(ServletDestination o1, ServletDestination o2) {
                    return o1.getEndpointInfo().getInterface().getName()
                         .getLocalPart().compareTo(o2.getEndpointInfo()
                                                       .getInterface().getName().getLocalPart());
                }
            });
                
            if (destinations.size() > 0) {  
                response.getWriter().write("<span class=\"heading\">Available services:</span><br/>");
                response.getWriter().write("<table " + (serviceListStyleSheet == null
                        ? "cellpadding=\"1\" cellspacing=\"1\" border=\"1\" width=\"100%\"" : "") + ">");
                for (ServletDestination sd : destinations) {
                    if (null != sd.getEndpointInfo().getName()) {
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
                        response.getWriter().write("<br/><span class=\"field\">Wsdl:</span> "
                                + "<a href=\"" + address + "?wsdl\">"
                                + sd.getEndpointInfo().getService().getName() + "</a>");
                        response.getWriter().write("<br/><span class=\"field\">Target namespace:</span> "
                                + "<span class=\"value\">" 
                                + sd.getEndpointInfo().getService().getTargetNamespace() + "</span>");
                        response.getWriter().write("</td></tr>");
                    }    
                }
                response.getWriter().write("</table>");
            } else {
                response.getWriter().write("<span class=\"heading\">No service was found.</span>");
            }
        }    
        response.getWriter().write("</body></html>");
    }

    private String getBaseURL(HttpServletRequest request) {
        String reqPrefix = request.getRequestURL().toString();        
        String pathInfo = request.getPathInfo() == null ? "" : request.getPathInfo();
        //fix for CXF-898
        if (!"/".equals(pathInfo) || reqPrefix.endsWith("/")) {            
            reqPrefix = reqPrefix.substring(0, reqPrefix.length() - pathInfo.length());
        }
        return reqPrefix;
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
            d.invoke(cxfServlet.getServletContext(), request, response);
        } catch (IOException e) {
            throw new ServletException(e);
        } finally {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Finished servicing http request on thread: " + Thread.currentThread());
            }
        }

    }
}
