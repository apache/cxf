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


import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import io.undertow.servlet.spec.HttpServletResponseImpl;
import io.undertow.servlet.spec.ServletContextImpl;
import io.undertow.util.Headers;

public class UndertowHTTPTestHandler extends UndertowHTTPHandler {

    private String responseStr;

    public UndertowHTTPTestHandler(String s, boolean cmExact) {
        super(null, cmExact);
        responseStr = s;
    }


    @Override
    public void handleRequest(HttpServerExchange undertowExchange) throws Exception {
        try {

            HttpServletResponseImpl response = new HttpServletResponseImpl(undertowExchange,
                                                                           (ServletContextImpl)servletContext);
            HttpServletRequestImpl request = new HttpServletRequestImpl(undertowExchange,
                                                                        (ServletContextImpl)servletContext);

            ServletRequestContext servletRequestContext = new ServletRequestContext(((ServletContextImpl)servletContext)
                .getDeployment(), request, response, null);


            undertowExchange.putAttachment(ServletRequestContext.ATTACHMENT_KEY, servletRequestContext);
            request.setAttribute("HTTP_HANDLER", this);
            request.setAttribute("UNDERTOW_DESTINATION", undertowHTTPDestination);

            // just return the response for testing
            response.getOutputStream().write(responseStr.getBytes());
            response.flushBuffer();
        } catch (Throwable t) {
            t.printStackTrace();
            if (undertowExchange.isResponseChannelAvailable()) {
                undertowExchange.setStatusCode(500);
                final String errorPage = "<html><head><title>Error</title>"
                    + "</head><body>Internal Error 500" + t.getMessage()
                    + "</body></html>";
                undertowExchange.getResponseHeaders().put(Headers.CONTENT_LENGTH,
                                                          Integer.toString(errorPage.length()));
                undertowExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
                Sender sender = undertowExchange.getResponseSender();
                sender.send(errorPage);
            }
        }
    }



}
