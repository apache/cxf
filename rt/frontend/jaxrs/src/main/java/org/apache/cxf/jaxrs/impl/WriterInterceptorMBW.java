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
package org.apache.cxf.jaxrs.impl;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public class WriterInterceptorMBW implements WriterInterceptor {

    private static final Logger LOG = LogUtils.getL7dLogger(WriterInterceptorMBW.class);
    
    private MessageBodyWriter<Object> writer;
    private Message m;
    public WriterInterceptorMBW(MessageBodyWriter<Object> writer, Message m) {
        this.writer = writer;
        this.m = m;
    }

    public MessageBodyWriter<Object> getMBW() {
        return writer;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void aroundWriteTo(WriterInterceptorContext c) throws IOException, WebApplicationException {
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Response EntityProvider is: " + writer.getClass().getName());
        }
        
        MultivaluedMap<String, Object> headers = c.getHeaders();
        Object mtObject = headers.getFirst(HttpHeaders.CONTENT_TYPE);
        MediaType entityMt = mtObject == null ? c.getMediaType() : JAXRSUtils.toMediaType(mtObject.toString());
        m.put(Message.CONTENT_TYPE, mtObject.toString());
        
        Class<?> entityCls = c.getType();
        Type entityType = c.getGenericType();
        Annotation[] entityAnns = c.getAnnotations();
        
        if (m.get(ProviderFactory.PROVIDER_SELECTION_PROPERTY_CHANGED) == Boolean.TRUE
            && !writer.isWriteable(entityCls, entityType, entityAnns, entityMt)) {
            
            writer = (MessageBodyWriter<Object>)ProviderFactory.getInstance(m)
                .createMessageBodyWriter(entityCls, entityType, entityAnns, entityMt, m);
            if (writer == null) {
                throw new RuntimeException("No writer available");
            }
        }
        
        writer.writeTo(c.getEntity(), 
                       c.getType(), 
                       c.getGenericType(), 
                       c.getAnnotations(), 
                       entityMt, 
                       headers, 
                       c.getOutputStream());
    }
    
    
}
