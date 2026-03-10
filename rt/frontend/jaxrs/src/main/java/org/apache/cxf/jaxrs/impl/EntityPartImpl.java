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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;

public class EntityPartImpl implements EntityPart {
    private final String name;
    private final String fileName;
    private final Object content;
    private final MultivaluedMap<String, String> headers;
    private final MediaType mediaType;
    private final GenericType<?> genericType;
    private final Class<?> type;
    private final Providers providers;
    private final AtomicBoolean consumed = new AtomicBoolean();

    //CHECKSTYLE:OFF
    public EntityPartImpl(Providers providers, String name, String fileName, Object content, Class<?> type,
            GenericType<?> genericType, MultivaluedMap<String, String> headers, MediaType mediaType) {
        this.providers = providers;
        this.name = name;
        this.fileName = fileName;
        this.content = content;
        this.headers = headers;
        this.mediaType = mediaType;
        this.genericType = genericType;
        if (type == null) {
            if (content != null) {
                this.type = content.getClass();
            } else {
                this.type = InputStream.class;
            }
        } else {
            this.type = type;
        }
    }
    //CHECKSTYLE:ON

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<String> getFileName() {
        return Optional.ofNullable(fileName);
    }

    @Override
    public InputStream getContent() {
        try {
            if (content instanceof InputStream) {
                return (InputStream) content;
            }  else if (content instanceof byte[]) { 
                return new ByteArrayInputStream((byte[]) content);
            } else if (fileName != null && content instanceof File) { 
                return Files.newInputStream(((File) content).toPath());
            } else {
                return contentAsStream();
            } 
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> InputStream contentAsStream() throws IOException {
        final MediaType mt = Objects.requireNonNullElse(mediaType, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        final Type generic = (genericType != null) ? genericType.getType() : null;

        final MessageBodyWriter<T> writer = (MessageBodyWriter<T>) providers
            .getMessageBodyWriter(type, generic, null, mt);

        if (writer == null) {
            throw new IllegalArgumentException("No suitable MessageBodyWriter available to handle "
                + type.getName() + ", media type " + mediaType);
        }

        final PipedInputStream pipedInputStream = new PipedInputStream();
        try (PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream)) {
            writer.writeTo((T)content, (Class<T>)type, generic, null, mt, cast(headers), pipedOutputStream);
            return pipedInputStream;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getContent(Class<T> asType) throws IllegalArgumentException, IllegalStateException,
            IOException, WebApplicationException {

        if (consumed.compareAndExchange(false, true)) {
            throw new IllegalStateException("Content has been consumed already");
        }

        if (asType == null) {
            throw new NullPointerException("The type is required");
        }

        if (asType.isInstance(content)) {
            return (T) content;
        }

        final MessageBodyReader<T> reader = (MessageBodyReader<T>) providers
            .getMessageBodyReader(asType, null, null, mediaType);

        if (reader != null) {
            // The implementation is required to close the content stream when this method
            // is invoked, so it may only be invoked once. 
            try (InputStream is = getContent()) {
                return reader.readFrom(asType, null, null, mediaType, headers, is);
            }
        } else {
            throw new IllegalArgumentException("No suitable MessageBodyReader available to handle "
                + asType.getName() + ", media type " + mediaType);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getContent(GenericType<T> asType) throws IllegalArgumentException, IllegalStateException,
            IOException, WebApplicationException {

        if (consumed.compareAndExchange(false, true)) {
            throw new IllegalStateException("Content has been consumed already");
        }

        if (asType == null) {
            throw new NullPointerException("The generic type is required");
        }

        if (asType.getRawType().isInstance(content)) {
            return (T) content;
        }

        final MessageBodyReader<T> reader = (MessageBodyReader<T>) providers
            .getMessageBodyReader(asType.getRawType(), asType.getType(), null, mediaType);

        // The implementation is required to close the content stream when this method
        // is invoked, so it may only be invoked once. 
        if (reader != null) {
            // The implementation is required to close the content stream when this method
            // is invoked, so it may only be invoked once. 
            try (InputStream is = getContent()) {
                return reader.readFrom((Class<T>) asType.getRawType(), asType.getType(),
                    null, mediaType, headers, is);
            }
        } else {
            throw new IllegalArgumentException("No suitable MessageBodyReader available to handle "
                + asType.getRawType().getName() + ", media type " + mediaType);
        }
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }
    
    @SuppressWarnings("unchecked")
    private static <T, U> MultivaluedMap<T, U> cast(MultivaluedMap<?, ?> p) {
        return (MultivaluedMap<T, U>)p;
    }
}
