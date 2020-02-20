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

package org.apache.cxf.management.interceptor;

import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

public class ResponseTimeMessageInInterceptor extends AbstractMessageResponseTimeInterceptor {

    public ResponseTimeMessageInInterceptor() {
        super(Phase.RECEIVE);
        addBefore(AttachmentInInterceptor.class.getName());
    }

    public void handleMessage(Message message) throws Fault {
        Exchange ex = message.getExchange();
        //if serviceCounter is disabled , all responseTimeInterceptors will be skipped
        boolean forceDisabled = Boolean.FALSE.equals(ex.get("org.apache.cxf.management.counter.enabled"));
        if (!forceDisabled && isServiceCounterEnabled(ex)) {
            if (isClient(message)) {
                if (!ex.isOneWay()) {
                    endHandlingMessage(ex);
                }
            } else {
                beginHandlingMessage(ex);
            }
        }
    }

    @Override
    public void handleFault(Message message) {
        Exchange ex = message.getExchange();
        if (Boolean.TRUE.equals(ex.get("org.apache.cxf.management.counter.enabled"))) {
            FaultMode mode = message.get(FaultMode.class);
            if (mode == null) {
                mode = FaultMode.UNCHECKED_APPLICATION_FAULT;
            }
            ex.put(FaultMode.class, mode);
        }
    }
}
