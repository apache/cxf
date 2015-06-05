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
package org.apache.cxf.tracing.htrace;

import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.htrace.TraceScope;

public class HTraceStopInterceptor extends AbstractHTraceInterceptor {
    public HTraceStopInterceptor(final String phase) {
        super(phase, null);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        Map<String, List<Object>> responseHeaders = CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
        
        boolean isRequestor = MessageUtils.isRequestor(message);
        Message requestMessage = isRequestor ? message.getExchange().getOutMessage() 
            : message.getExchange().getInMessage();
        Map<String, List<String>> requestHeaders =  
            CastUtils.cast((Map<?, ?>)requestMessage.get(Message.PROTOCOL_HEADERS));
        
        TraceScope span = (TraceScope)message.getExchange().get(TRACE_SPAN);
        
        super.stopTraceSpan(requestHeaders, responseHeaders, span);
    }
    
}
