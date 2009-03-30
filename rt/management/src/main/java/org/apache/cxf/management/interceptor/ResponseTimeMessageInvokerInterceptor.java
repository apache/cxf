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
import org.apache.cxf.interceptor.ServiceInvokerInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

/* When the message get from the server side
 * The exchange.isOneWay() is workable when the message
 * handler by the binging interceptor  
 * */
public class ResponseTimeMessageInvokerInterceptor extends AbstractMessageResponseTimeInterceptor {
    
    public ResponseTimeMessageInvokerInterceptor() {
        super(Phase.INVOKE);
        // this interceptor should be add before the serviceInvokerInterceptor
        addBefore(ServiceInvokerInterceptor.class.getName());
    }
    
    public void handleMessage(Message message) throws Fault {
        Exchange ex = message.getExchange();
        if (ex.isOneWay()) {
            setOneWayMessage(ex);
        }
    }
    
    @Override
    public void handleFault(Message message) {
        Exchange ex = message.getExchange();
        ex.put(FaultMode.class, message.get(FaultMode.class));
        if (ex.isOneWay()) {
            endHandlingMessage(ex);
        }
    }
}
