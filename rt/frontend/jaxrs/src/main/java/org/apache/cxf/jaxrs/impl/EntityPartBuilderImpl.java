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
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.EntityPart.Builder;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

public class EntityPartBuilderImpl implements EntityPart.Builder {
    private final String name;
    private MediaType mediaType;
    private MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    private String fileName;
    private Object content;
    private GenericType<?> genericType;
    private Class<?> type;

    public EntityPartBuilderImpl(final String name) {
        this.name = name;
    }

    @Override
    public Builder mediaType(MediaType mt) throws IllegalArgumentException {
        this.mediaType = mt;
        return this;
    }

    @Override
    public Builder mediaType(String mts) throws IllegalArgumentException {
        this.mediaType = MediaType.valueOf(mts);
        return this;
    }

    @Override
    public Builder header(String headerName, String... headerValues) throws IllegalArgumentException {
        headers.addAll(headerName, headerValues);
        return this;
    }

    @Override
    public Builder headers(MultivaluedMap<String, String> newHeaders) throws IllegalArgumentException {
        headers.clear();
        headers.putAll(newHeaders);
        return this;
    }

    @Override
    public Builder fileName(String fn) throws IllegalArgumentException {
        this.fileName = fn;
        return this;
    }

    @Override
    public Builder content(InputStream in) throws IllegalArgumentException {
        this.content = in;
        return this;
    }

    @Override
    public <T> Builder content(T c, Class<? extends T> t) throws IllegalArgumentException {
        this.content = c;
        this.type = t;
        this.genericType = null;
        return this;
    }

    @Override
    public <T> Builder content(T c, GenericType<T> t) throws IllegalArgumentException {
        this.content = c;
        this.genericType = t;
        this.type = null;
        return this;
    }

    @Override
    public EntityPart build() throws IllegalStateException, IOException, WebApplicationException {
        return new EntityPartImpl(getProviderFactory(), name, fileName, content, type, genericType, headers, mediaType);
    }

    private static Providers getProviderFactory() {
        final Message message = JAXRSUtils.getCurrentMessage();
        if (message == null) {
            return new ReaderWriterProviders(ServerProviderFactory.createInstance(BusFactory.getThreadDefaultBus()));
        } else {
            return new ReaderWriterProviders(ProviderFactory.getInstance(message));
        }
    }
    

    private static final class ReaderWriterProviders implements Providers {
        private final ProviderFactory factory;

        private ReaderWriterProviders(final ProviderFactory factory) {
            this.factory = factory;
        }

        @Override
        public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type, Type genericType,
                Annotation[] annotations, MediaType mediaType) {
            return factory.createMessageBodyWriter(type, genericType, annotations, mediaType, getCurrentMessage());
        }
        
        @Override
        public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type, Type genericType,
                Annotation[] annotations, MediaType mediaType) {
            return factory.createMessageBodyReader(type, genericType, annotations, mediaType, getCurrentMessage());
        }
        
        @Override
        public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType) {
            throw new UnsupportedOperationException();
        }

        private static Message getCurrentMessage() {
            Message message = JAXRSUtils.getCurrentMessage();
            if (message == null) { 
                message = new MessageImpl();
            }
            return message;
        }
    };
}
