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

package org.apache.cxf.jaxws;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.MessageContext.Scope;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.invoker.Factory;
import org.apache.cxf.service.invoker.FactoryInvoker;
import org.apache.cxf.service.invoker.SingletonFactory;

public abstract class AbstractJAXWSMethodInvoker extends FactoryInvoker {

    public AbstractJAXWSMethodInvoker(final Object bean) {
        super(new SingletonFactory(bean));
    }
    
    public AbstractJAXWSMethodInvoker(Factory factory) {
        super(factory);
    }
    
    protected SOAPFaultException findSoapFaultException(Throwable ex) {
        if (ex instanceof SOAPFaultException) {
            return (SOAPFaultException)ex;
        }
        if (ex.getCause() != null) {
            return findSoapFaultException(ex.getCause());
        }
        return null;
    }
    
    @Override
    protected Fault createFault(Throwable ex, Method m, List<Object> params, boolean checked) {
        //map the JAX-WS faults
        SOAPFaultException sfe = findSoapFaultException(ex);
        if (sfe != null) {
            SoapFault fault = new SoapFault(sfe.getFault().getFaultString(),
                                            ex,
                                            sfe.getFault().getFaultCodeAsQName());
            fault.setRole(sfe.getFault().getFaultActor());
            fault.setDetail(sfe.getFault().getDetail());
            
            return fault;
        }
        return super.createFault(ex, m, params, checked);
    }
    
    protected Map<String, Object> removeHandlerProperties(WrappedMessageContext ctx) {
        Map<String, Scope> scopes = CastUtils.cast((Map<?, ?>)ctx.get(WrappedMessageContext.SCOPES));
        Map<String, Object> handlerScopedStuff = new HashMap<String, Object>();
        if (scopes != null) {
            for (Map.Entry<String, Scope> scope : scopes.entrySet()) {
                if (scope.getValue() == Scope.HANDLER) {
                    handlerScopedStuff.put(scope.getKey(), ctx.get(scope.getKey()));
                }
            }
            for (String key : handlerScopedStuff.keySet()) {
                ctx.remove(key);
            }
        }
        return handlerScopedStuff;
    }
    
    protected void addHandlerProperties(WrappedMessageContext ctx,
                                        Map<String, Object> handlerScopedStuff) {
        for (Map.Entry<String, Object> key : handlerScopedStuff.entrySet()) {
            ctx.put(key.getKey(), key.getValue(), Scope.HANDLER);
        }
    }
   
    private Message createResponseMessage(Exchange exchange) {
        if (exchange == null) {
            return null;
        }
        Message m = exchange.getOutMessage();
        if (m == null && !exchange.isOneWay()) {
            Endpoint ep = exchange.get(Endpoint.class);
            m = ep.getBinding().createMessage();
            exchange.setOutMessage(m);
        }
        return m;
    }

    protected void updateWebServiceContext(Exchange exchange, MessageContext ctx) {
        // Guard against wrong type associated with header list.
        // Need to copy header only if the message is going out.
        if (ctx.containsKey(Header.HEADER_LIST) 
                && ctx.get(Header.HEADER_LIST) instanceof List<?>) {
            List list = (List) ctx.get(Header.HEADER_LIST);
            if (list != null && !list.isEmpty()) {
                SoapMessage sm = (SoapMessage) createResponseMessage(exchange);
                if (sm != null) {
                    Iterator iter = list.iterator();
                    while (iter.hasNext()) {
                        sm.getHeaders().add((Header) iter.next());
                    }
                }
            }
        }
        if (exchange.getOutMessage() != null) {
            Message out = exchange.getOutMessage();
            if (out.containsKey(Message.PROTOCOL_HEADERS)) {
                Map<String, List<String>> heads = CastUtils
                    .cast((Map<?, ?>)exchange.getOutMessage().get(Message.PROTOCOL_HEADERS));
                if (heads.containsKey("Content-Type")) {
                    List<String> ct = heads.get("Content-Type");
                    exchange.getOutMessage().put(Message.CONTENT_TYPE, ct.get(0));
                    heads.remove("Content-Type");
                }
            }
            Map<String, DataHandler> dataHandlers  
                = CastUtils.cast((Map<?, ?>)out.get(MessageContext.OUTBOUND_MESSAGE_ATTACHMENTS));
            if (dataHandlers != null && !dataHandlers.isEmpty()) {
                Collection<Attachment> attachments = out.getAttachments();
                if (attachments == null) {
                    attachments = new ArrayList<Attachment>();
                    out.setAttachments(attachments);
                }
                for (Map.Entry<String, DataHandler> entry : dataHandlers.entrySet()) {
                    Attachment att = new AttachmentImpl(entry.getKey(), entry.getValue());
                    attachments.add(att);
                }
            }
            out.remove(MessageContext.OUTBOUND_MESSAGE_ATTACHMENTS);
        }
    }

}
