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
package org.apache.cxf.jaxws.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.MessageContext.Scope;

import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.attachment.LazyAttachmentCollection;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;

// Do some context mapping work from rt-core to jaxws standard.
// NOTE if there are changes in cxf Message property names, this
// class should be updated.

public final class ContextPropertiesMapping {    
    
    private static Map<String, String> cxf2jaxwsMap = new HashMap<String, String>();
    private static Map<String, String> jaxws2cxfMap = new HashMap<String, String>();
    
    static {
        cxf2jaxwsMap.put(Message.ENDPOINT_ADDRESS, 
                          BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
        cxf2jaxwsMap.put(Message.MAINTAIN_SESSION,
                         BindingProvider.SESSION_MAINTAIN_PROPERTY);
        
        cxf2jaxwsMap.put(Message.HTTP_REQUEST_METHOD,
                          MessageContext.HTTP_REQUEST_METHOD);
        cxf2jaxwsMap.put(Message.RESPONSE_CODE, 
                          MessageContext.HTTP_RESPONSE_CODE);        
        cxf2jaxwsMap.put(Message.PATH_INFO, 
                          MessageContext.PATH_INFO);
        cxf2jaxwsMap.put(Message.QUERY_STRING, 
                          MessageContext.QUERY_STRING);
        cxf2jaxwsMap.put("HTTP.REQUEST", 
                         MessageContext.SERVLET_REQUEST);
        cxf2jaxwsMap.put("HTTP.RESPONSE", 
                         MessageContext.SERVLET_RESPONSE);
        cxf2jaxwsMap.put("HTTP.CONTEXT", 
                         MessageContext.SERVLET_CONTEXT);
       
        jaxws2cxfMap.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, 
                         Message.ENDPOINT_ADDRESS);
        jaxws2cxfMap.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, 
                         Message.MAINTAIN_SESSION);
                
        jaxws2cxfMap.put(MessageContext.HTTP_REQUEST_METHOD,
                         Message.HTTP_REQUEST_METHOD);
        jaxws2cxfMap.put(MessageContext.HTTP_RESPONSE_CODE,
                         Message.RESPONSE_CODE);        
        jaxws2cxfMap.put(MessageContext.PATH_INFO,
                         Message.PATH_INFO);
        jaxws2cxfMap.put(MessageContext.QUERY_STRING,
                         Message.QUERY_STRING);
        
        jaxws2cxfMap.put(MessageContext.SERVLET_REQUEST, 
                         "HTTP.REQUEST"); 
        jaxws2cxfMap.put(MessageContext.SERVLET_RESPONSE, 
                         "HTTP.RESPONSE");
        jaxws2cxfMap.put(MessageContext.SERVLET_CONTEXT, 
                        "HTTP.CONTEXT");
        
        jaxws2cxfMap.put(BindingProvider.SOAPACTION_URI_PROPERTY, SoapBindingConstants.SOAP_ACTION);
    }
    
    private ContextPropertiesMapping() {
        
    }
    
    private static void mapContext(Map<String, Object> context, Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            Object o = context.get(entry.getKey());
            if (o != null) {
                context.put(entry.getValue(), o);
            }
        }
    }
   
    public static void mapRequestfromJaxws2Cxf(Map<String, Object> context) {
        //deal with PROTOCOL_HEADERS mapping  
        Object requestHeaders = 
            context.get(MessageContext.HTTP_REQUEST_HEADERS);
        if (null != requestHeaders) {
            context.put(Message.PROTOCOL_HEADERS, requestHeaders);
        }       
        mapJaxws2Cxf(context);
    }
    
    public static void mapResponsefromCxf2Jaxws(Map<String, Object> context) {
        //deal with PROTOCOL_HEADERS mapping
        Object responseHeaders = 
            context.get(Message.PROTOCOL_HEADERS);
        if (null != responseHeaders) {
            context.put(MessageContext.HTTP_RESPONSE_HEADERS, responseHeaders);
        }  
        mapContext(context, cxf2jaxwsMap);
    }
    
    private static void mapJaxws2Cxf(Map<String, Object> context) {
        mapContext(context, jaxws2cxfMap);
        if (context.containsKey(BindingProvider.USERNAME_PROPERTY)) {
            AuthorizationPolicy authPolicy = new AuthorizationPolicy();
            authPolicy.setUserName((String)context.get(BindingProvider.USERNAME_PROPERTY));
            authPolicy.setPassword((String)context.get(BindingProvider.PASSWORD_PROPERTY));
            context.put(AuthorizationPolicy.class.getName(), authPolicy);
        }
    }
        
    private static void mapCxf2Jaxws(WrappedMessageContext context) {
        
        for (Map.Entry<String, String> entry : cxf2jaxwsMap.entrySet()) {
            Object o = context.get(entry.getKey());
            if (o != null) {
                context.put(entry.getValue(), o, Scope.APPLICATION);
            } else if (context.containsKey(entry.getValue())) {
                context.put(entry.getValue(), 
                            context.get(entry.getValue()), Scope.APPLICATION);                
            }
        }
        
        if (context.containsKey(AuthorizationPolicy.class.getName())) {
            AuthorizationPolicy authPolicy =
                (AuthorizationPolicy)context.get(AuthorizationPolicy.class.getName());
            context.put(BindingProvider.USERNAME_PROPERTY, authPolicy.getUserName(), Scope.APPLICATION);
            context.put(BindingProvider.PASSWORD_PROPERTY, authPolicy.getPassword(), Scope.APPLICATION);
        }
    }
    
    
    public static MessageContext createWebServiceContext(Exchange exchange) {
        WrappedMessageContext ctx = new WrappedMessageContext(exchange.getInMessage(), Scope.APPLICATION);
        mapCxf2Jaxws(exchange, ctx, false);
        return ctx;
    }

    public static void mapCxf2Jaxws(Exchange exchange, WrappedMessageContext ctx, boolean requestor) {

        ctx.put(Message.WSDL_DESCRIPTION, exchange.get(Message.WSDL_DESCRIPTION));
        ctx.put(Message.WSDL_INTERFACE, exchange.get(Message.WSDL_INTERFACE));
        ctx.put(Message.WSDL_OPERATION, exchange.get(Message.WSDL_OPERATION));
        ctx.put(Message.WSDL_PORT, exchange.get(Message.WSDL_PORT));
        ctx.put(Message.WSDL_SERVICE, exchange.get(Message.WSDL_SERVICE));

        mapCxf2Jaxws(ctx);        
        Message inMessage = exchange.getInMessage();
        Message outMessage = exchange.getOutMessage();
        
        if (inMessage == null
            && Boolean.TRUE.equals(ctx.get(Message.INBOUND_MESSAGE))) {
            //inbound partial responses and stuff are not set in the exchange
            inMessage = ctx.getWrappedMessage();
        }
        
        if (inMessage != null) {
            addMessageAttachments(ctx, 
                                  inMessage, 
                                  MessageContext.INBOUND_MESSAGE_ATTACHMENTS);
            
            Object inHeaders = 
                inMessage.get(Message.PROTOCOL_HEADERS);
            if (null != inHeaders) {
                if (requestor) {
                    ctx.put(MessageContext.HTTP_RESPONSE_HEADERS,
                            inHeaders,
                            Scope.APPLICATION);
                } else {
                    ctx.put(MessageContext.HTTP_REQUEST_HEADERS,
                            inHeaders,
                            Scope.APPLICATION);                    
                }
            
                outMessage = exchange.getOutMessage();
                if (outMessage == null) {
                    Endpoint ep = exchange.get(Endpoint.class);
                    outMessage = ep.getBinding().createMessage();
                    exchange.setOutMessage(outMessage);
                }
            }

        }

        if (outMessage != null) {
            addMessageAttachments(ctx, 
                              outMessage, 
                              MessageContext.OUTBOUND_MESSAGE_ATTACHMENTS);
            Object outHeaders =
                outMessage.get(Message.PROTOCOL_HEADERS);
            
            if (outHeaders != null && !requestor) {
                ctx.put(MessageContext.HTTP_REQUEST_HEADERS,
                        outHeaders,
                        Scope.APPLICATION);
            }
        }

    }
    
    private static void addMessageAttachments(WrappedMessageContext ctx,
                                              Message message,
                                              String propertyName) {
        Map<String, DataHandler> dataHandlers = null;

        Collection<Attachment> attachments = message.getAttachments();
        if (attachments != null) {
            if (attachments instanceof LazyAttachmentCollection) {
                dataHandlers = ((LazyAttachmentCollection)attachments).createDataHandlerMap();
            } else {
                //preserve the order of iteration
                dataHandlers = new LinkedHashMap<String, DataHandler>();
                for (Attachment attachment : attachments) {
                    dataHandlers.put(attachment.getId(), attachment.getDataHandler());
                }
            }
        }

        ctx.put(propertyName, 
                dataHandlers == null ? new LinkedHashMap<String, DataHandler>()
                                     : dataHandlers,
                Scope.APPLICATION);
    }
    
    public static void updateWebServiceContext(Exchange exchange, MessageContext ctx) {
        //get the context response code and setback to out message
        if (ctx.containsKey(MessageContext.HTTP_RESPONSE_CODE)) {
            exchange.getOutMessage().put(Message.RESPONSE_CODE, ctx.get(MessageContext.HTTP_RESPONSE_CODE));
        }
        
        // Guard against wrong type associated with header list.
        // Need to copy header only if the message is going out.
        if (ctx.containsKey(Header.HEADER_LIST) 
                && ctx.get(Header.HEADER_LIST) instanceof List<?> 
                && exchange.getOutMessage() instanceof SoapMessage) {
            SoapMessage sm = (SoapMessage) exchange.getOutMessage();
            Iterator iter = ((List) ctx.get(Header.HEADER_LIST)).iterator();
            while (iter.hasNext()) {
                sm.getHeaders().add((Header) iter.next());
            }
        }
        if (ctx.containsKey(MessageContext.HTTP_RESPONSE_HEADERS)) {
            Map<String, List<String>> other = CastUtils
                .cast((Map<?, ?>)ctx.get(MessageContext.HTTP_RESPONSE_HEADERS));
            Map<String, List<String>> heads = CastUtils
                .cast((Map<?, ?>)exchange.getOutMessage().get(Message.PROTOCOL_HEADERS));
            if (heads != null) {
                heads.putAll(other);
            } else if (!other.isEmpty()) {
                exchange.getOutMessage().put(Message.PROTOCOL_HEADERS, 
                                             ctx.get(MessageContext.HTTP_RESPONSE_HEADERS));
                heads = other;
            }
            if (heads.containsKey("Content-Type")) {
                List<String> ct = heads.get("Content-Type");
                exchange.getOutMessage().put(Message.CONTENT_TYPE, ct.get(0));
                heads.remove("Content-Type");
            }
        }
        Map<String, DataHandler> dataHandlers  
            = CastUtils.cast((Map<?, ?>)ctx.get(MessageContext.OUTBOUND_MESSAGE_ATTACHMENTS));
        if (dataHandlers != null && !dataHandlers.isEmpty()) {
            Collection<Attachment> attachments = exchange.getOutMessage().getAttachments();
            if (attachments == null) {
                attachments = new ArrayList<Attachment>();
                exchange.getOutMessage().setAttachments(attachments);
            }
            for (Map.Entry<String, DataHandler> entry : dataHandlers.entrySet()) {
                Attachment att = new AttachmentImpl(entry.getKey(), entry.getValue());
                attachments.add(att);
            }
        }
    }

}
