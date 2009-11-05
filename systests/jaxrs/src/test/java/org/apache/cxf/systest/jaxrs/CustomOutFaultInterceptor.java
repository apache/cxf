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
package org.apache.cxf.systest.jaxrs;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

public class CustomOutFaultInterceptor extends AbstractPhaseInterceptor<Message> {
    private boolean handleMessageCalled;
    public CustomOutFaultInterceptor() {
        this(Phase.PRE_STREAM);
    }

    public CustomOutFaultInterceptor(String s) {
        super(Phase.MARSHAL);
        
    } 

    public void handleMessage(Message message) throws Fault {
        if (message.getExchange().get("org.apache.cxf.systest.for-out-fault-interceptor") == null) {
            return;
        }
        handleMessageCalled = true;
        Exception ex = message.getContent(Exception.class);
        if (ex == null) {
            throw new RuntimeException("Exception is expected");
        }
        Fault fault = (Fault)ex;
        if (fault == null) {
            throw new RuntimeException("Fault is expected");
        }
        // deal with the actual exception : fault.getCause()
        HttpServletResponse response = (HttpServletResponse)message.getExchange()
            .getInMessage().get(AbstractHTTPDestination.HTTP_RESPONSE);
        response.setStatus(500);
        try {
            response.getOutputStream().write("<nobook/>".getBytes());
            response.getOutputStream().flush();
            message.getInterceptorChain().abort();           
        } catch (IOException ioex) {
            throw new RuntimeException("Error writing the response");
        }
        
    }

    protected boolean handleMessageCalled() {
        return handleMessageCalled;
    }

}
