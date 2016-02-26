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


import javax.servlet.ServletContext;

import org.apache.cxf.Bus;

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
    
    private static final String SSL_CIPHER_SUITE_ATTRIBUTE = "javax.servlet.request.cipher_suite";
    private static final String SSL_PEER_CERT_CHAIN_ATTRIBUTE = "javax.servlet.request.X509Certificate";

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
            t.printStackTrace();
            if (undertowExchange.isResponseChannelAvailable()) {
                undertowExchange.setResponseCode(500);
                final String errorPage = "<html><head><title>Error</title>"
                    + "</head><body>Internal Error 500" + t.getMessage()
                    + "</body></html>";
                undertowExchange.getResponseHeaders().put(Headers.CONTENT_LENGTH,
                                                          "" + errorPage.length());
                undertowExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
                Sender sender = undertowExchange.getResponseSender();
                sender.send(errorPage);
            }
        }
    }
    
    
}
