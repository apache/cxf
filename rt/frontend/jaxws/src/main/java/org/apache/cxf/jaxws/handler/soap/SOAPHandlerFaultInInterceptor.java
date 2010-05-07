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
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Binding;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;

import org.apache.cxf.binding.soap.HeaderUtil;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.SoapInterceptor;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.handler.AbstractProtocolHandlerInterceptor;
import org.apache.cxf.jaxws.handler.HandlerChainInvoker;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;

public class SOAPHandlerFaultInInterceptor extends
        AbstractProtocolHandlerInterceptor<SoapMessage> implements
        SoapInterceptor {

    public SOAPHandlerFaultInInterceptor(Binding binding) {
        super(binding, Phase.PRE_PROTOCOL);
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
        if (binding.getHandlerChain().isEmpty()) {
            return;
        }
        if (getInvoker(message).getProtocolHandlers().isEmpty()) {
            return;
        }

        checkUnderstoodHeaders(message);
        MessageContext context = createProtocolMessageContext(message);
        HandlerChainInvoker invoker = getInvoker(message);
        invoker.setProtocolMessageContext(context);

        if (!invoker.invokeProtocolHandlersHandleFault(isRequestor(message), context)) {
            handleAbort(message, context);
        }

        SOAPMessage msg = message.getContent(SOAPMessage.class);
        if (msg != null) {
            XMLStreamReader xmlReader = createXMLStreamReaderFromSOAPMessage(msg);
            message.setContent(XMLStreamReader.class, xmlReader);
        }

    }

    private void checkUnderstoodHeaders(SoapMessage soapMessage) {
        Set<QName> paramHeaders = HeaderUtil.getHeaderQNameInOperationParam(soapMessage);
        if (soapMessage.getHeaders().isEmpty() && paramHeaders.isEmpty()) {
            //the TCK expects the getHeaders method to always be
            //called.   If there aren't any headers in the message,
            //THe MustUnderstandInterceptor quickly returns without 
            //trying to calculate the understood headers.   Thus, 
            //we need to call it here.
            getUnderstoodHeaders();
        }
    }

    private void handleAbort(SoapMessage message, MessageContext context) {
        if (isRequestor(message)) {

            if (getInvoker(message).isOutbound()) {
                // client side outbound
                // wont get here
            } else {
                // client side inbound - Normal handler message processing
                // stops, but the inbound interceptor chain still continues, dispatch the message
                //By onCompletion here, we can skip rest Logical handlers
                onCompletion(message);
            }
        } else {
            if (!getInvoker(message).isOutbound()) {
                // server side inbound
                // wont get here
            } else {
                // server side outbound
                // wont get here
            }
        }
    }

    @Override
    protected MessageContext createProtocolMessageContext(SoapMessage message) {
        return new SOAPMessageContextImpl(message);
    }

    private XMLStreamReader createXMLStreamReaderFromSOAPMessage(SOAPMessage soapMessage) {
        // responseMsg.setContent(SOAPMessage.class, soapMessage);
        XMLStreamReader xmlReader = null;
        try {
            DOMSource bodySource = new DOMSource(soapMessage.getSOAPPart().getEnvelope().getBody());
            xmlReader = StaxUtils.createXMLStreamReader(bodySource);
            xmlReader.nextTag();
            xmlReader.nextTag(); // move past body tag
        } catch (SOAPException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
        return xmlReader;
    }

    public void handleFault(SoapMessage message) {
    }
}
