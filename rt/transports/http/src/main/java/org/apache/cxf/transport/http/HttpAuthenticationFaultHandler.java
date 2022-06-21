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
package org.apache.cxf.transport.http;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.AuthenticationException;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * Translates an AuthenticationException into a 401 response
 */
public class HttpAuthenticationFaultHandler extends AbstractPhaseInterceptor<Message> {
    String authenticationType;
    String realm;

    public HttpAuthenticationFaultHandler() {
        super(Phase.UNMARSHAL);
        this.authenticationType = "Basic";
        this.realm = "CXF service";
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        // Nothing
    }

    @Override
    public void handleFault(Message message) {
        Exception ex = message.getContent(Exception.class);
        if (ex instanceof AuthenticationException) {
            HttpServletResponse resp = (HttpServletResponse)message.getExchange()
                .getInMessage().get(AbstractHTTPDestination.HTTP_RESPONSE);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setHeader("WWW-Authenticate", authenticationType + " realm=\"" + realm + "\"");
            resp.setContentType("text/plain");
            try {
                resp.getOutputStream().write(ex.getMessage().getBytes());
                resp.getOutputStream().flush();
                message.getInterceptorChain().setFaultObserver(null); //avoid return soap fault
                message.getInterceptorChain().abort();
            } catch (IOException e) {
                // TODO
            }
        }
    }

    public void setAuthenticationType(String authenticationType) {
        this.authenticationType = authenticationType;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

}
