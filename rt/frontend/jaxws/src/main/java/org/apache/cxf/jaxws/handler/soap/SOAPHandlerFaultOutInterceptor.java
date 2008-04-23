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
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Binding;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.soap.SOAPFaultException;

import org.w3c.dom.Node;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.jaxws.handler.AbstractProtocolHandlerInterceptor;
import org.apache.cxf.jaxws.handler.HandlerChainInvoker;
import org.apache.cxf.phase.Phase;


public class SOAPHandlerFaultOutInterceptor extends
        AbstractProtocolHandlerInterceptor<SoapMessage> implements
        SoapInterceptor {
    private static final SAAJOutInterceptor SAAJ_OUT = new SAAJOutInterceptor();
    private static final String ENDING_ID = SOAPHandlerFaultOutInterceptor.class.getName() + ".ENDING";
    
    AbstractSoapInterceptor ending = new AbstractSoapInterceptor(ENDING_ID, Phase.USER_PROTOCOL) {
        public void handleMessage(SoapMessage message) throws Fault {
            handleMessageInternal(message);
        }
    };
    
    public SOAPHandlerFaultOutInterceptor(Binding binding) {
        super(binding, Phase.PRE_PROTOCOL);
        addAfter(MustUnderstandInterceptor.class.getName());
        addAfter(StaxOutInterceptor.class.getName());
        addAfter(SAAJOutInterceptor.class.getName());
    }

    public Set<URI> getRoles() {
        Set<URI> roles = new HashSet<URI>();
        // TODO
        return roles;
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

        if (getInvoker(message).isOutbound()) {
            //The SOAPMessage might be set from the outchain, in this case, 
            //we need to clean it up and create a new SOAPMessage dedicated to fault.
            message.setContent(SOAPMessage.class, null);

            SAAJ_OUT.handleMessage(message);

            message.getInterceptorChain().add(ending);
        } 
    }
    
    private void handleMessageInternal(SoapMessage message) {
        MessageContext context = createProtocolMessageContext(message);
        HandlerChainInvoker invoker = getInvoker(message);
        invoker.setProtocolMessageContext(context);        

        try {
            if (!invoker.invokeProtocolHandlersHandleFault(isRequestor(message), context)) {
                // handleAbort(message, context);
            }
        } catch (RuntimeException exception) {
            /*
             * handleFault throws exception, in this case we need to replace
             * SOAPFault with the exception thrown from HandleFault so that the
             * exception can be dispatched.
             */
            try {
                SOAPMessage originalMsg = message.getContent(SOAPMessage.class);
                SOAPBody body = originalMsg.getSOAPBody();
                body.removeContents();

                SOAPFault soapFault = body.addFault();

                if (exception instanceof SOAPFaultException) {
                    SOAPFaultException sf = (SOAPFaultException)exception;
                    soapFault.setFaultString(sf.getFault().getFaultString());
                    soapFault.setFaultCode(sf.getFault().getFaultCodeAsQName());
                    soapFault.setFaultActor(sf.getFault().getFaultActor());
                    if (sf.getFault().hasDetail()) {
                        Node nd = originalMsg.getSOAPPart().importNode(
                                                                       sf.getFault().getDetail()
                                                                           .getFirstChild(), true);
                        soapFault.addDetail().appendChild(nd);
                    }
                } else if (exception instanceof Fault) {
                    SoapFault sf = SoapFault.createFault((Fault)exception, ((SoapMessage)message)
                        .getVersion());
                    soapFault.setFaultString(sf.getReason());
                    soapFault.setFaultCode(sf.getFaultCode());
                    Node nd = originalMsg.getSOAPPart().importNode(sf.getOrCreateDetail(), true);
                    soapFault.addDetail().appendChild(nd);
                } else {
                    soapFault.setFaultString(exception.getMessage());
                    soapFault.setFaultCode(new QName("http://cxf.apache.org/faultcode", "HandleFault"));
                }
            } catch (SOAPException e) {
                // do nothing
                e.printStackTrace();
            }
        }

        onCompletion(message);
    }
    
    @Override
    protected MessageContext createProtocolMessageContext(SoapMessage message) {
        return new SOAPMessageContextImpl(message);
    }

    public void handleFault(SoapMessage message) {
    }
}
