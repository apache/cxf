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
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
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
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.HTTPSession;
import org.apache.cxf.transport.https.SSLUtils;
import org.apache.cxf.transports.http.QueryHandler;
import org.apache.cxf.transports.http.QueryHandlerRegistry;
import org.apache.cxf.wsdl.http.AddressType;

public class OsgiServlet extends HttpServlet {

    private static final Logger LOG = LogUtils.getL7dLogger(OsgiServlet.class);

    private OsgiDestinationRegistryIntf transport;
    private String lastBase = "";
    private boolean isHideServiceList;
    private boolean disableAddressUpdates;
    private String forcedBaseAddress;

    public OsgiServlet(OsgiDestinationRegistryIntf transport) {
        this.transport = transport;
    }

    @Override
    public void destroy() {
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        invoke(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        invoke(request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        invoke(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
        invoke(request, response);
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

    private synchronized void updateDests(HttpServletRequest request) {
        if (disableAddressUpdates) {
            return;
        }
        String base = forcedBaseAddress == null ? getBaseURL(request) : forcedBaseAddress;

        //if (base.equals(lastBase)) {
        //    return;
        //}
        Set<String> paths = transport.getDestinationsPaths();
        for (String path : paths) {
            OsgiDestination d2 = transport.getDestinationForPath(path);
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
            OsgiDestination d = (OsgiDestination) transport.getDestinationForPath(ei.getAddress());

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
                                //throw new ServletException(e);
                                LOG.warning(qh.getClass().getName()
                                    + " Exception caught writing response: "
                                    + e.getMessage());
                            }
                        }
                    }
                } else {
                    invokeDestination(request, res, d);
                }
            }
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }

    private OsgiDestination checkRestfulRequest(HttpServletRequest request) throws IOException {

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
        Collection<OsgiDestination> destinations = transport.getDestinations();
        response.setContentType("text/html");
        response.getWriter().write("<html><body>");
        if (!isHideServiceList) {
            if (destinations.size() > 0) {
                for (OsgiDestination sd : destinations) {
                    if (null != sd.getEndpointInfo().getName()) {
                        String address = sd.getEndpointInfo().getAddress();
                        response.getWriter().write("<p> <a href=\"" + address + "?wsdl\">");
                        response.getWriter().write(sd.getEndpointInfo().getName() + "</a> </p>");
                    }
                }
            } else {
                response.getWriter().write("No service was found.");
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

    public void invokeDestination(final HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  OsgiDestination d) throws ServletException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Service http request on thread: " + Thread.currentThread());
        }

        try {
            MessageImpl inMessage = createInMessage();
            inMessage.setContent(InputStream.class, request.getInputStream());
            inMessage.put(AbstractHTTPDestination.HTTP_REQUEST, request);
            inMessage.put(AbstractHTTPDestination.HTTP_RESPONSE, response);
            inMessage.put(AbstractHTTPDestination.HTTP_CONTEXT, getServletContext());
            inMessage.put(AbstractHTTPDestination.HTTP_CONFIG, getServletConfig());
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

            ExchangeImpl exchange = createExchange();
            exchange.setInMessage(inMessage);
            exchange.setSession(new HTTPSession(request));

            d.doMessage(inMessage);
        } catch (IOException e) {
            throw new ServletException(e);
        }

    }

    protected MessageImpl createInMessage() {
        return new MessageImpl();
    }

    protected ExchangeImpl createExchange() {
        return new ExchangeImpl();
    }
}
