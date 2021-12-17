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

package org.apache.cxf.jaxrs.impl.tl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import org.apache.cxf.jaxrs.impl.ProvidersImpl;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public class ThreadLocalProviders extends AbstractThreadLocalProxy<Providers>
       implements Providers {

    public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type,
                                                         Type genericType,
                                                         Annotation[] annotations,
                                                         MediaType mediaType) {
        Providers p = getCurrentProviders();
        return p != null ? p.getMessageBodyReader(type, genericType, annotations, mediaType) : null;
    }

    public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type,
                                                         Type genericType,
                                                         Annotation[] annotations,
                                                         MediaType mediaType) {
        Providers p = getCurrentProviders();
        return p != null ? p.getMessageBodyWriter(type, genericType, annotations, mediaType) : null;
    }

    public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType) {
        Providers p = getCurrentProviders();
        return p != null ? p.getContextResolver(contextType, mediaType) : null;
    }

    public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type) {
        Providers p = getCurrentProviders();
        return p != null ? p.getExceptionMapper(type) : null;
    }

    private Providers getCurrentProviders() {
        Providers p = get();
        return p != null ? p : getProvidersImpl();
    }
    private Providers getProvidersImpl() {
        Message m = JAXRSUtils.getCurrentMessage();
        return m != null ? new ProvidersImpl(JAXRSUtils.getContextMessage(m)) : null;
    }
}
