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

import jakarta.ws.rs.ProcessingException;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class CustomFaultInInterceptor extends AbstractPhaseInterceptor<Message> {
    private boolean useProcEx;
    public CustomFaultInInterceptor(boolean useProcEx) {
        super(Phase.PRE_STREAM);
        this.useProcEx = useProcEx;
    }

    public void handleMessage(Message message) throws Fault {
        Exception ex = message.getContent(Exception.class);
        String errorMessage = ex.getCause().getClass().getSimpleName()
            + ": Microservice at "
            + message.get(Message.REQUEST_URI)
            + " is not available";
        message.getExchange().put("wrap.in.processing.exception", useProcEx);
        throw useProcEx ? new ProcessingException(new CustomRuntimeException(errorMessage))
            : new CustomRuntimeException(errorMessage);
    }
    public static class CustomRuntimeException extends RuntimeException {
        private static final long serialVersionUID = -4664563239685175537L;

        public CustomRuntimeException(String errorMessage) {
            super(errorMessage);
        }
    }

}
