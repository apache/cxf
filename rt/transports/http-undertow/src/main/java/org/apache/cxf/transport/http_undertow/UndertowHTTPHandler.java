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

package org.apache.cxf.transport.http_undertow;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.commons_text.StringEscapeUtils;

import io.undertow.io.Sender;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.SSLSessionInfo;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import io.undertow.servlet.spec.HttpServletResponseImpl;
import io.undertow.servlet.spec.ServletContextImpl;
import io.undertow.util.Headers;



public class UndertowHTTPHandler implements HttpHandler {

    private static final Logger LOG = LogUtils.getLogger(UndertowHTTPHandler.class);
    private static final String ERROR_PAGE_START = "<html><head><title>Error</title>"
        + "</head><body>Internal Error 500";
    private static final String ERROR_PAGE_END = "</body></html>";

    private static final String SSL_CIPHER_SUITE_ATTRIBUTE = "jakarta.servlet.request.cipher_suite";
    private static final String SSL_PEER_CERT_CHAIN_ATTRIBUTE = "jakarta.servlet.request.X509Certificate";
    private static final String METHOD_TRACE = "TRACE";

    protected UndertowHTTPDestination undertowHTTPDestination;
    protected ServletContext servletContext;
    private boolean contextMatchExact;
    private String urlName;
    private Bus bus;

    public UndertowHTTPHandler(UndertowHTTPDestination uhd, boolean cmt) {
        undertowHTTPDestination = uhd;
        this.contextMatchExact = cmt;
    }
    public UndertowHTTPHandler(Bus bus) {
        this.bus = bus;
    }

    public boolean isContextMatchExact() {
        return this.contextMatchExact;
    }

    public void setServletContext(ServletContext sc) {
        servletContext = sc;
        if (undertowHTTPDestination != null) {
            undertowHTTPDestination.setServletContext(sc);
        }
    }

    public ServletContext getServletContext() {
        return this.servletContext;
    }

    public void setName(String name) {
        urlName = name;
    }

    public String getName() {
        return urlName;
    }


    public Bus getBus() {
        return undertowHTTPDestination != null ? undertowHTTPDestination.getBus() : bus;
    }

    @Override
    public void handleRequest(HttpServerExchange undertowExchange) throws Exception {
        try {
            // perform blocking operation on exchange
            if (undertowExchange.isInIoThread()) {
                undertowExchange.dispatch(this);
                return;
            }


            HttpServletResponseImpl response = new HttpServletResponseImpl(undertowExchange,
                                                                           (ServletContextImpl)servletContext);
            HttpServletRequestImpl request = new HttpServletRequestImpl(undertowExchange,
                                                                        (ServletContextImpl)servletContext);
            if (request.getMethod().equals(METHOD_TRACE)) {
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }
            ServletRequestContext servletRequestContext = new ServletRequestContext(((ServletContextImpl)servletContext)
                .getDeployment(), request, response, null);


            undertowExchange.putAttachment(ServletRequestContext.ATTACHMENT_KEY, servletRequestContext);
            request.setAttribute("HTTP_HANDLER", this);
            request.setAttribute("UNDERTOW_DESTINATION", undertowHTTPDestination);
            SSLSessionInfo ssl = undertowExchange.getConnection().getSslSessionInfo();
            if (ssl != null) {
                request.setAttribute(SSL_CIPHER_SUITE_ATTRIBUTE, ssl.getCipherSuite());
                try {
                    request.setAttribute(SSL_PEER_CERT_CHAIN_ATTRIBUTE, ssl.getPeerCertificates());
                } catch (Exception e) {
                    // for some case won't have the peer certification
                    // do nothing
                }
            }
            undertowHTTPDestination.doService(servletContext, request, response);

        } catch (Throwable t) {
            sendInternalError(undertowExchange, t);
        }
    }

    protected void sendInternalError(HttpServerExchange undertowExchange, Throwable t) {
        LOG.log(Level.WARNING, "Error processing request", t);
        if (undertowExchange.isResponseChannelAvailable()) {
            String errorPage = ERROR_PAGE_START;
            if (isIncludeExceptionMessage() && t.getMessage() != null) {
                errorPage += " " + StringEscapeUtils.escapeHtml4(t.getMessage());
            }
            errorPage += ERROR_PAGE_END;
            byte[] body = errorPage.getBytes(StandardCharsets.UTF_8);
            undertowExchange.setStatusCode(500);
            undertowExchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, Integer.toString(body.length));
            undertowExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html; charset=UTF-8");
            Sender sender = undertowExchange.getResponseSender();
            sender.send(errorPage, StandardCharsets.UTF_8);
        }
    }

    /**
     * The exception message is only included (HTML-escaped) in the error page if the
     * {@link Message#EXCEPTION_MESSAGE_CAUSE_ENABLED} bus property is set to "true", as it may contain
     * request data or internal details. The exception is always logged.
     */
    private boolean isIncludeExceptionMessage() {
        Bus b = getBus();
        return b != null && PropertyUtils.isTrue(b.getProperty(Message.EXCEPTION_MESSAGE_CAUSE_ENABLED));
    }


}
