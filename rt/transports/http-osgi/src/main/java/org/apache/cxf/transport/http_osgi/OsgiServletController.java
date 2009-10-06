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
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
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
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.HTTPSession;
import org.apache.cxf.transport.https.SSLUtils;
import org.apache.cxf.transport.servlet.AbstractServletController;
import org.apache.cxf.transports.http.QueryHandler;
import org.apache.cxf.transports.http.QueryHandlerRegistry;
import org.apache.cxf.wsdl.http.AddressType;

public class OsgiServletController extends AbstractServletController {
    private static final Logger LOG = LogUtils.getL7dLogger(OsgiServlet.class);
    
    private String lastBase = "";
    private OsgiServlet servlet;
    public OsgiServletController(OsgiServlet servlet) {
        super(servlet.getServletConfig());
        this.servlet = servlet;
    }

    private synchronized void updateDests(HttpServletRequest request) {
        if (disableAddressUpdates) {
            return;
        }
        String base = forcedBaseAddress == null ? getBaseURL(request) : forcedBaseAddress;

        //if (base.equals(lastBase)) {
        //    return;
        //}
        Set<String> paths = servlet.getTransport().getDestinationsPaths();
        for (String path : paths) {
            OsgiDestination d2 = servlet.getTransport().getDestinationForPath(path);
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
            OsgiDestination d = 
                (OsgiDestination)servlet.getTransport().getDestinationForPath(ei.getAddress());

            if (d == null) {
                if (!isHideServiceList && (request.getRequestURI().endsWith(serviceListRelativePath)
                    || request.getRequestURI().endsWith(serviceListRelativePath + "/"))
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
                Bus bus = d.getBus();
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
                                out.flush();
                                return;
                            } catch (Exception e) {
                                LOG.warning(qh.getClass().getName()
                                    + " Exception caught writing response: "
                                    + e.getMessage());
                                throw new ServletException(e);
                            }
                        }
                    }
                } 
                invokeDestination(request, res, d);
                
            }
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }

    private OsgiDestination checkRestfulRequest(HttpServletRequest request) throws IOException {

        String address = request.getPathInfo() == null ? "" : request.getPathInfo();

        for (String path : servlet.getTransport().getDestinationsPaths()) {
            if (address.startsWith(path)) {
                return servlet.getTransport().getDestinationForPath(path);
            }
        }
        return null;
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
        
        Collection<OsgiDestination> destinations = servlet.getTransport().getDestinations();
            
        if (destinations.size() > 0) {
            writeSOAPEndpoints(response, destinations);
            writeRESTfulEndpoints(response, destinations);
        } else {
            response.getWriter().write("<span class=\"heading\">No services have been found.</span>");
        }
        
        response.getWriter().write("</body></html>");
    }

    private void writeSOAPEndpoints(HttpServletResponse response, Collection<OsgiDestination> destinations)
        throws IOException {
        response.getWriter().write("<span class=\"heading\">Available SOAP services:</span><br/>");
        response.getWriter().write("<table " + (serviceListStyleSheet == null
                ? "cellpadding=\"1\" cellspacing=\"1\" border=\"1\" width=\"100%\"" : "") + ">");
        for (OsgiDestination sd : destinations) {
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
    
    
    private void writeRESTfulEndpoints(HttpServletResponse response, Collection<OsgiDestination> destinations)
        throws IOException {
        
        List<OsgiDestination> restfulDests = new ArrayList<OsgiDestination>();
        for (OsgiDestination sd : destinations) {
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
        for (OsgiDestination sd : destinations) {
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

    public void invokeDestination(final HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  OsgiDestination d) throws ServletException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Service http request on thread: " + Thread.currentThread());
        }

        try {
            MessageImpl inMessage = servlet.createInMessage();
            inMessage.setContent(InputStream.class, request.getInputStream());
            inMessage.put(AbstractHTTPDestination.HTTP_REQUEST, request);
            inMessage.put(AbstractHTTPDestination.HTTP_RESPONSE, response);
            inMessage.put(AbstractHTTPDestination.HTTP_CONTEXT, servlet.getServletContext());
            inMessage.put(AbstractHTTPDestination.HTTP_CONFIG, servlet.getServletConfig());
            inMessage.put(Message.HTTP_REQUEST_METHOD, request.getMethod());
            inMessage.put(Message.REQUEST_URI, request.getRequestURI());
            inMessage.put(Message.PATH_INFO, request.getPathInfo());
            inMessage.put(Message.QUERY_STRING, request.getQueryString());
            inMessage.put(Message.CONTENT_TYPE, request.getContentType());
            inMessage.put(Message.ACCEPT_CONTENT_TYPE, request.getHeader("Accept"));
            inMessage.put(Message.BASE_PATH, d.getAddress().getAddress().getValue());
            inMessage.put(SecurityContext.class, new SecurityContext() {
                public Principal getUserPrincipal() {
                    return request.getUserPrincipal();
                }
                public boolean isUserInRole(String role) {
                    return request.isUserInRole(role);
                }
            });

            // work around a bug with Jetty which results in the character
            // encoding not being trimmed correctly.
            String enc = request.getCharacterEncoding();
            if (enc != null && enc.endsWith("\"")) {
                enc = enc.substring(0, enc.length() - 1);
            }

            String normalizedEncoding = HttpHeaderHelper.mapCharset(enc);
            if (normalizedEncoding == null) {
                String m = new org.apache.cxf.common.i18n.Message("INVALID_ENCODING_MSG",
                                                                  LOG, enc).toString();
                LOG.log(Level.WARNING, m);
                throw new IOException(m);
            }

            inMessage.put(Message.ENCODING, normalizedEncoding);
            SSLUtils.propogateSecureSession(request, inMessage);

            ExchangeImpl exchange = servlet.createExchange();
            exchange.setInMessage(inMessage);
            exchange.setSession(new HTTPSession(request));

            d.doMessage(inMessage);
        } catch (IOException e) {
            throw new ServletException(e);
        }

    }

    
}
