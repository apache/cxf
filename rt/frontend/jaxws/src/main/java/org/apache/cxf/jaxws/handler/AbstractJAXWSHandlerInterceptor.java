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

package org.apache.cxf.jaxws.handler;

import javax.xml.namespace.QName;
import javax.xml.ws.Binding;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceModelUtil;

public abstract class AbstractJAXWSHandlerInterceptor<T extends Message> extends AbstractPhaseInterceptor<T> {
    protected Binding binding;
    
    protected AbstractJAXWSHandlerInterceptor(Binding b, String phase) {
        super(phase);
        binding = b;
    }
    
    protected boolean isOutbound(T message) {
        return isOutbound(message, message.getExchange());
    }
    
    private boolean isOutbound(T message, Exchange ex) {
        return message == ex.getOutMessage()
            || message == ex.getOutFaultMessage();
    }
    
    protected HandlerChainInvoker getInvoker(T message) {
        Exchange ex = message.getExchange();
        HandlerChainInvoker invoker = 
            ex.get(HandlerChainInvoker.class);
        if (null == invoker) {
            invoker = new HandlerChainInvoker(binding.getHandlerChain(),
                                              isOutbound(message));
            ex.put(HandlerChainInvoker.class, invoker);
        }
        
        boolean outbound = isOutbound(message, ex);
        if (outbound) {
            invoker.setOutbound();
        } else {
            invoker.setInbound();
        }
        invoker.setRequestor(isRequestor(message));
        
        if (ex.isOneWay()
            || ((isRequestor(message) && !outbound) 
                || (!isRequestor(message) && outbound))) {
            invoker.setResponseExpected(false);
        } else { 
            invoker.setResponseExpected(true);
        }
        
        return invoker;
    }
    
    protected Binding getBinding() {
        return binding;
    }
    
    public void onCompletion(T message) {
        getInvoker(message).mepComplete(message);
    }   
    
    public boolean isMEPComlete(T message) {
        HandlerChainInvoker invoker = getInvoker(message);
      
        if (invoker.isRequestor()) {
            //client inbound and client outbound with no response are end of MEP
            if (invoker.isInbound()) {
                return true;
            } else if (!invoker.isResponseExpected()) {
                return true;
            }            
        } else {
            //server outbound and server inbound with no response are end of MEP
            if (!invoker.isInbound()) {
                return true;
            } else if (!invoker.isResponseExpected()) {
                return true;
            }            
        } 
        
        return false;
    }
    
    protected void setupBindingOperationInfo(Exchange exch, Object data) {
        if (exch.get(BindingOperationInfo.class) == null) {
            //need to know the operation to determine if oneway
            QName opName = getOpQName(exch, data);
            if (opName == null) {
                return;
            }
            BindingOperationInfo bop = ServiceModelUtil
                .getOperationForWrapperElement(exch, opName, false);
            if (bop == null) {
                bop = ServiceModelUtil.getOperation(exch, opName);
            }
            if (bop != null) {
                exch.put(BindingOperationInfo.class, bop);
                exch.put(OperationInfo.class, bop.getOperationInfo());
                if (bop.getOutput() == null) {
                    exch.setOneWay(true);
                }
            }

        }
    }
    
    protected QName getOpQName(Exchange ex, Object data) {
        return null;
    }
}
