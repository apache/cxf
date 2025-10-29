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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.EntityPart.Builder;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
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
        final MediaType mt = Objects.requireNonNullElse(mediaType, MediaType.APPLICATION_OCTET_STREAM_TYPE);

        Message message = JAXRSUtils.getCurrentMessage();
        ProviderFactory factory = null;

        if (message == null) {
            message = new MessageImpl();
            factory = ServerProviderFactory.createInstance(BusFactory.getThreadDefaultBus());
        } else {
            factory = ProviderFactory.getInstance(message);
        }

        if (genericType != null) {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                writeTo(factory, genericType, mt, message, out);
                return new EntityPartImpl(factory, name, fileName, new ByteArrayInputStream(out.toByteArray()),
                    headers, mt);
            }
        } else if (type != null) {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                writeTo(factory, type, mt, message, out);
                return new EntityPartImpl(factory, name, fileName, new ByteArrayInputStream(out.toByteArray()),
                    headers, mt);
            }
        } else {
            throw new IllegalStateException("Either type or genericType is expected for the content");
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void writeTo(final ProviderFactory providers, final GenericType<T> t, final MediaType mt,
            final Message message, final ByteArrayOutputStream out) throws IOException {

        final MessageBodyWriter<T> writer = (MessageBodyWriter<T>) providers
            .createMessageBodyWriter(t.getRawType(), t.getType(), null, mt, message);

        writer.writeTo((T) content, t.getRawType(), t.getType(), null, mt, cast(headers), out);
    }

    @SuppressWarnings("unchecked")
    private <T> void writeTo(final ProviderFactory providers, final Class<T> t, final MediaType mt,
            final Message message, final ByteArrayOutputStream out) throws IOException {

        final MessageBodyWriter<T> writer = (MessageBodyWriter<T>) providers
                .createMessageBodyWriter(t, null, null, mt, message);

        writer.writeTo((T) content, t, null, null, mt, cast(headers), out);
    }
    
    @SuppressWarnings("unchecked")
    private static <T, U> MultivaluedMap<T, U> cast(MultivaluedMap<?, ?> p) {
        return (MultivaluedMap<T, U>)p;
    }
}
