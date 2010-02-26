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
package org.apache.cxf.jaxrs.ext;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.mail.internet.InternetHeaders;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;

import org.apache.cxf.attachment.AttachmentDeserializer;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.interceptor.AttachmentInputInterceptor;
import org.apache.cxf.jaxrs.interceptor.AttachmentOutputInterceptor;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

public class MessageContextImpl implements MessageContext {

    private Message m;
    public MessageContextImpl(Message m) {
        this.m = m;
    }
    
    public Object get(Object key) {
        String keyValue = key.toString();
        if (MultipartBody.INBOUND_MESSAGE_ATTACHMENTS.equals(keyValue)
            || (MultipartBody.INBOUND_MESSAGE_ATTACHMENTS + ".embedded").equals(keyValue)) {
            return createAttachments(key.toString());
        }
        Object value = m.get(key);
        if (value == null && isRequestor()) {
            Message inMessage = m.getExchange().getInMessage();
            if (inMessage != null) {
                value = inMessage.get(key);
            }
        } 
        return value;
    }
    
    private boolean isRequestor() {
        return Boolean.TRUE.equals(m.containsKey(Message.REQUESTOR_ROLE));
    }
    
    public <T> T getContent(Class<T> format) {
        if (isRequestor() && m.getExchange().getInMessage() != null) {
            Message inMessage = m.getExchange().getInMessage();
            return inMessage.getContent(format);
        } 
        return m.getContent(format);
    }
    
    public Object getContextualProperty(Object key) {
        return m.getContextualProperty(key.toString());
    }

    public <T> T getContext(Class<T> contextClass) {
        return getContext(contextClass, contextClass);
    }
    
    protected <T> T getContext(Type genericType, Class<T> clazz) {
        return JAXRSUtils.createContextValue(m, genericType, clazz);
    }
    
    public <T, E> T getResolver(Class<T> resolverClazz, Class<E> resolveClazz) {
        if (ContextResolver.class == resolverClazz) {
            return resolverClazz.cast(getContext(resolveClazz, ContextResolver.class));
        }
        return null;
    }
    
    public Request getRequest() {
        return getContext(Request.class);
    }
    
    public HttpHeaders getHttpHeaders() {
        return getContext(HttpHeaders.class);
    }

    public Providers getProviders() {
        return getContext(Providers.class);
    }

    public SecurityContext getSecurityContext() {
        return getContext(SecurityContext.class);
    }

    public UriInfo getUriInfo() {
        return getContext(UriInfo.class);
    }
    
    public HttpServletRequest getHttpServletRequest() {
        return getContext(HttpServletRequest.class);
    }

    public HttpServletResponse getHttpServletResponse() {
        return getContext(HttpServletResponse.class);
    }
    
    public ServletConfig getServletConfig() {
        return getContext(ServletConfig.class);
    }

    public ServletContext getServletContext() {
        return getContext(ServletContext.class);
    }

    public void put(Object key, Object value) {
        if (MultipartBody.OUTBOUND_MESSAGE_ATTACHMENTS.equals(key.toString())) {
            convertToAttachments(value);
        }
        m.put(key.toString(), value);
    }

    private void convertToAttachments(Object value) {
        List<?> handlers = (List)value;
        List<org.apache.cxf.message.Attachment> atts = 
            new ArrayList<org.apache.cxf.message.Attachment>();
        
        for (int i = 1; i < handlers.size(); i++) {
            Attachment handler = (Attachment)handlers.get(i);
            AttachmentImpl att = new AttachmentImpl(handler.getContentId(), handler.getDataHandler());
            for (String key : handler.getHeaders().keySet()) {
                att.setHeader(key, att.getHeader(key));
            }
            att.setXOP(false);
            atts.add(att);
        }
        Message outMessage = getOutMessage();
        outMessage.setAttachments(atts);
        outMessage.put(AttachmentOutInterceptor.WRITE_ATTACHMENTS, "true");
        Attachment root = (Attachment)handlers.get(0);
        AttachmentOutputInterceptor attInterceptor =          
            new AttachmentOutputInterceptor(outMessage.get(Message.CONTENT_TYPE).toString(),
                                            root.getHeaders());
        
        outMessage.put(Message.CONTENT_TYPE, root.getContentType().toString());
        attInterceptor.handleMessage(outMessage);
    }
    
    private Message getOutMessage() {
        
        Message message = m.getExchange().getOutMessage();
        if (message == null) {
            Endpoint ep = m.getExchange().get(Endpoint.class);
            message = new org.apache.cxf.message.MessageImpl();
            message.setExchange(m.getExchange());
            message = ep.getBinding().createMessage(message);
            m.getExchange().setOutMessage(message);
        }
        
        return message;
    }
    
    private MultipartBody createAttachments(String propertyName) {
        Message inMessage = m.getExchange().getInMessage();
        boolean embeddedAttachment = inMessage.get("org.apache.cxf.multipart.embedded") != null;
        
        Object o = inMessage.get(propertyName);
        if (o != null) {
            return (MultipartBody)o;
        }
        
        if (embeddedAttachment) {
            inMessage = new MessageImpl();
            inMessage.setExchange(new ExchangeImpl());
            inMessage.put(AttachmentDeserializer.ATTACHMENT_DIRECTORY, 
                m.getExchange().getInMessage().get(AttachmentDeserializer.ATTACHMENT_DIRECTORY));
            inMessage.put(AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, 
                m.getExchange().getInMessage().get(AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD));
            inMessage.setContent(InputStream.class, 
                m.getExchange().getInMessage().get("org.apache.cxf.multipart.embedded.input"));
            inMessage.put(Message.CONTENT_TYPE, 
                m.getExchange().getInMessage().get("org.apache.cxf.multipart.embedded.ctype").toString());
        }
        
        
        new AttachmentInputInterceptor().handleMessage(inMessage);
    
        List<Attachment> newAttachments = new LinkedList<Attachment>();
        try {
            Attachment first = new Attachment(AttachmentUtil.createAttachment(
                                     inMessage.getContent(InputStream.class), 
                                     (InternetHeaders)inMessage.get(InternetHeaders.class.getName())));
            newAttachments.add(first);
        } catch (IOException ex) {
            throw new WebApplicationException(500);
        }
        
    
        Collection<org.apache.cxf.message.Attachment> childAttachments = inMessage.getAttachments();
        if (childAttachments == null) {
            childAttachments = Collections.emptyList();
        }
        childAttachments.size();
        for (org.apache.cxf.message.Attachment a : childAttachments) {
            newAttachments.add(new Attachment(a));
        }
        MediaType mt = embeddedAttachment 
            ? (MediaType)inMessage.get("org.apache.cxf.multipart.embedded.ctype")
            : getHttpHeaders().getMediaType();
        MultipartBody body = new MultipartBody(newAttachments, mt, false);
        inMessage.put(propertyName, body);
        return body;
    }
       
}
