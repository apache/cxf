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

package org.apache.cxf.jaxws.handler.logical;

import javax.xml.ws.Binding;

import org.apache.cxf.jaxws.handler.AbstractJAXWSHandlerInterceptor;
import org.apache.cxf.jaxws.handler.HandlerChainInvoker;
import org.apache.cxf.jaxws.handler.soap.SOAPHandlerFaultInInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;


public class LogicalHandlerFaultInInterceptor<T extends Message> 
    extends AbstractJAXWSHandlerInterceptor<T> {

    public LogicalHandlerFaultInInterceptor(Binding binding) {
        super(binding, Phase.PRE_PROTOCOL);
        addAfter(SOAPHandlerFaultInInterceptor.class.getName());       
    }

    public void handleMessage(T message) {
        if (binding.getHandlerChain().isEmpty()) {
            return;
        }
        HandlerChainInvoker invoker = getInvoker(message);
        if (invoker.getLogicalHandlers().isEmpty()) {
            return;
        }
        
        LogicalMessageContextImpl lctx = new LogicalMessageContextImpl(message);
        invoker.setLogicalMessageContext(lctx);
        boolean requestor = isRequestor(message);
        if (!invoker.invokeLogicalHandlersHandleFault(requestor, lctx)) {
            if (!requestor) {
                //server side, wont get here
            } else {
                //Client side inbound, thus no response expected, do nothing, the close will  
                //be handled by MEPComplete later
            }
        }
 
        //If this is the inbound and end of MEP, call MEP completion
        if (!isOutbound(message) && isMEPComlete(message)) {
            onCompletion(message);
        }
    }
    
    public void handleFault(T message) {
        // TODO
    }
}
