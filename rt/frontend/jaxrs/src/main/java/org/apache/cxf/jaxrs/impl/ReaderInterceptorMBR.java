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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.message.Message;

public class ReaderInterceptorMBR implements ReaderInterceptor {

    private MessageBodyReader<?> reader;
    private Message m;
    
    public ReaderInterceptorMBR(MessageBodyReader<?> reader,
                                Message m) {
        this.reader = reader;
        this.m = m;
    }
    
    public MessageBodyReader<?> getMBR() {
        return reader;
    }
    
    @SuppressWarnings({
        "unchecked", "rawtypes"
    })
    @Override
    public Object aroundReadFrom(ReaderInterceptorContext c) throws IOException, WebApplicationException {
        Class entityCls = (Class)c.getType();
        Type entityType = c.getGenericType();
        MediaType entityMt = c.getMediaType();
        Annotation[] entityAnns = c.getAnnotations();
        
        if (m.get(ProviderFactory.PROVIDER_SELECTION_PROPERTY_CHANGED) == Boolean.TRUE
            && !reader.isReadable(entityCls, entityType, entityAnns, entityMt)) {
            reader = ProviderFactory.getInstance(m)
                .createMessageBodyReader(entityCls, entityType, entityAnns, entityMt, m);
            if (reader == null) {
                throw new RuntimeException("No reader available");
            }
        }
        
        
        return reader.readFrom(entityCls, entityType, entityAnns, entityMt, 
                               new HttpHeadersImpl(m).getRequestHeaders(),
                               c.getInputStream());
    }

}
