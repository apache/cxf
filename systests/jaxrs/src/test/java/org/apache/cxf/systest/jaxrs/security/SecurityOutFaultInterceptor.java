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
package org.apache.cxf.systest.jaxrs.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

public class SecurityOutFaultInterceptor extends AbstractPhaseInterceptor<Message> {

    public SecurityOutFaultInterceptor() {
        super(Phase.PRE_STREAM);

    }

    public void handleMessage(Message message) throws Fault {
        Fault fault = (Fault)message.getContent(Exception.class);
        Throwable ex = fault.getCause();
        if (!(ex instanceof SecurityException)) {
            throw new RuntimeException("Security Exception is expected");
        }

        HttpServletResponse response = (HttpServletResponse)message.getExchange().getInMessage()
            .get(AbstractHTTPDestination.HTTP_RESPONSE);
        int status = ex instanceof AccessDeniedException ? 403 : 401;
        response.setStatus(status);
        try {
            response.getOutputStream().write(ex.getMessage().getBytes());
            response.getOutputStream().flush();
        } catch (IOException iex) {
            // ignore
        }

        message.getInterceptorChain().abort();
    }

}
