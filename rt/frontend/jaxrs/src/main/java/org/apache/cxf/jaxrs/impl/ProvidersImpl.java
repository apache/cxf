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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;

import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.message.Message;

public class ProvidersImpl implements Providers {

    private Message m;
    public ProvidersImpl(Message m) {
        this.m = m;
    }
    
    public <T> MessageBodyReader<T> getMessageBodyReader(
         Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return ProviderFactory.getInstance(m).createMessageBodyReader(
            type, genericType, annotations, mediaType, m);
    }

    public <T> MessageBodyWriter<T> getMessageBodyWriter(
        Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return ProviderFactory.getInstance(m).createMessageBodyWriter(
                   type, genericType, annotations, mediaType, m);
    }

    public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType) {
        return ProviderFactory.getInstance(m).createContextResolver(contextType, m, mediaType);
    }

    public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type) {
        return ProviderFactory.getInstance(m).createExceptionMapper(type, m);
    }

}
