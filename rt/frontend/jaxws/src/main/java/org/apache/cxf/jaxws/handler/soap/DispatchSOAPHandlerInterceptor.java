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

package org.apache.cxf.jaxws.handler.soap;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Binding;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.SoapInterceptor;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.handler.AbstractProtocolHandlerInterceptor;
import org.apache.cxf.jaxws.handler.HandlerChainInvoker;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.MessageObserver;

public class DispatchSOAPHandlerInterceptor extends
        AbstractProtocolHandlerInterceptor<SoapMessage> implements
        SoapInterceptor {

    public DispatchSOAPHandlerInterceptor(Binding binding) {
        super(binding, Phase.USER_PROTOCOL);
    }

    public Set<URI> getRoles() {
        return new HashSet<URI>();
    }

    public Set<QName> getUnderstoodHeaders() {
        Set<QName> understood = new HashSet<QName>();
        for (Handler h : getBinding().getHandlerChain()) {
            if (h instanceof SOAPHandler) {
                Set<QName> headers = CastUtils.cast(((SOAPHandler) h).getHeaders());
                if (headers != null) {
                    understood.addAll(headers);
                }
            }
        }
        return understood;
    }

    public void handleMessage(SoapMessage message) {
        if (getInvoker(message).getProtocolHandlers().isEmpty()) {
            return;
        }

        MessageContext context = createProtocolMessageContext(message);
        HandlerChainInvoker invoker = getInvoker(message);
        invoker.setProtocolMessageContext(context);
        
        if (!invoker.invokeProtocolHandlers(isRequestor(message), context)) {
            handleAbort(message, context);
        } 
        
        // If this is the outbound and end of MEP, call MEP completion
        if (isRequestor(message) && invoker.getLogicalHandlers().isEmpty() 
            && !isOutbound(message) && isMEPComlete(message)) {
            onCompletion(message);
        } else if (isOutbound(message) && isMEPComlete(message)) {
            onCompletion(message);
        }
    }
    
    private void handleAbort(SoapMessage message, MessageContext context) {
        if (isRequestor(message)) {
            // client side outbound
            if (getInvoker(message).isOutbound()) {
                message.getInterceptorChain().abort();
                Endpoint e = message.getExchange().get(Endpoint.class);
                Message responseMsg = e.getBinding().createMessage();

                MessageObserver observer = (MessageObserver)message.getExchange().get(MessageObserver.class);
                if (observer != null) {
                    // the request message becomes the response message
                    message.getExchange().setInMessage(responseMsg);
                    SOAPMessage soapMessage = ((SOAPMessageContext)context).getMessage();

                    if (soapMessage != null) {
                        responseMsg.setContent(SOAPMessage.class, soapMessage);
                    }
                    responseMsg.put(PhaseInterceptorChain.STARTING_AT_INTERCEPTOR_ID,
                                    SOAPHandlerInterceptor.class.getName());
                    observer.onMessage(responseMsg);
                }
                
                //We dont call onCompletion here, as onCompletion will be called by inbound 
                //LogicalHandlerInterceptor
            } else {
                // client side inbound - Normal handler message processing
                // stops, but the inbound interceptor chain still continues, dispatch the message
                //By onCompletion here, we can skip following Logical handlers 
                onCompletion(message);
            }
        }
    }
    
    @Override
    protected MessageContext createProtocolMessageContext(SoapMessage message) {
        return new SOAPMessageContextImpl(message);
    }

    public void handleFault(SoapMessage message) {
    }
}
