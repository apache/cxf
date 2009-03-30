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

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

public class ResponseTimeMessageOutInterceptor extends AbstractMessageResponseTimeInterceptor {
    
    public ResponseTimeMessageOutInterceptor() {
        super(Phase.SEND);
    }
    
    public void handleMessage(Message message) throws Fault {
        Exchange ex = message.getExchange();
        if (isClient(message)) {
            if (ex.isOneWay()) {
                setOneWayMessage(ex);
            } else {
                beginHandlingMessage(ex);
            }
        } else { // the message is handled by server
            endHandlingMessage(ex);
        }
    }
    
    @Override
    public void handleFault(Message message) {
        Exchange ex = message.getExchange();
        if (ex.isOneWay()) {
            // do nothing, done by the ResponseTimeInvokerInterceptor
        } else {
            FaultMode faultMode = message.get(FaultMode.class);
            if (faultMode == null) {
                // client side exceptions don't have FaultMode set un the message properties (as of 2.1.4)
                faultMode = FaultMode.RUNTIME_FAULT;
            }
            ex.put(FaultMode.class, faultMode);
            endHandlingMessage(ex);
        }
    }
}
