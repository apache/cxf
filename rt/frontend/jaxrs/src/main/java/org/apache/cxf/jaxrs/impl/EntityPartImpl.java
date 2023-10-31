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
import java.util.Optional;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public class EntityPartImpl implements EntityPart {
    private final String name;
    private final String fileName;
    private final InputStream content;
    private final MultivaluedMap<String, String> headers;
    private final MediaType mediaType;
    private final ProviderFactory providers;

    EntityPartImpl(final ProviderFactory providers, String name, String fileName, InputStream content,
            MultivaluedMap<String, String> headers, MediaType mediaType) {
        this.providers = providers;
        this.name = name;
        this.fileName = fileName;
        this.content = content;
        this.headers = headers;
        this.mediaType = mediaType;
    }

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
        return content;
    }

    @Override
    public <T> T getContent(Class<T> type) 
            throws IllegalArgumentException, IllegalStateException, IOException, WebApplicationException {
        final Message message = JAXRSUtils.getCurrentMessage();

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final MessageBodyReader<T> reader = (MessageBodyReader) providers
            .createMessageBodyReader(type, null, null, mediaType, message);

        return reader.readFrom(type, null, null, mediaType, headers, content);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getContent(GenericType<T> type)
            throws IllegalArgumentException, IllegalStateException, IOException, WebApplicationException {
        final Message message = JAXRSUtils.getCurrentMessage();
        
        @SuppressWarnings("rawtypes")
        final MessageBodyReader<T> reader = (MessageBodyReader) providers
            .createMessageBodyReader(type.getRawType(), type.getType(), null, mediaType, message);

        return reader.readFrom((Class<T>) type.getRawType(), type.getType(), null, mediaType, headers, content);
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }
}
