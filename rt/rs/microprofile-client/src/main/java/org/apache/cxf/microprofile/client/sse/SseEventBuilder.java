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
package org.apache.cxf.microprofile.client.sse;

import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.sse.InboundSseEvent;

class SseEventBuilder {
    static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[] {};
    String theName;
    String theId;
    String theComment;
    String theData;
    Providers providers;

    SseEventBuilder(Providers providers) {
        this.providers = providers;
    }

    SseEventBuilder name(String name) {
        this.theName = name;
        return this;
    }

    SseEventBuilder id(String id) {
        this.theId = id;
        return this;
    }

    SseEventBuilder comment(String comment) {
        this.theComment = comment;
        return this;
    }

    SseEventBuilder data(String data) {
        this.theData = data;
        return this;
    }

    InboundSseEvent build() {
        return new InboundSseEventImpl(providers, theName, theId, theComment, theData);
    }

    static class InboundSseEventImpl implements InboundSseEvent {

        private final Providers providers;
        private final String name;
        private final String id;
        private final String comment;
        private final String data;

        InboundSseEventImpl(Providers providers, String name, String id, String comment, String data) {
            this.providers = providers;
            this.name = name;
            this.id = id;
            this.comment = comment;
            this.data = data;
        }

        @Override
        public boolean isReconnectDelaySet() {
            return false;
        }

        @Override
        public long getReconnectDelay() {
            return -1;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getComment() {
            return comment;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T readData(GenericType<T> type, MediaType mediaType) {
            return (T) readData(type.getRawType(), type.getType(), mediaType);
        }

        @Override
        public <T> T readData(Class<T> messageType, MediaType mediaType) {
            return readData(messageType, messageType, mediaType);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T readData(GenericType<T> type) {
            return (T) readData(type.getRawType(), type.getType(), guessMediaType(data));
        }

        @Override
        public <T> T readData(Class<T> type) {
            return readData(type, type, guessMediaType(data));
        }

        private <T> T readData(Class<T> type, Type genericType, MediaType mediaType) {
            if (data == null) {
                return null;
            }
            try {
                MessageBodyReader<T> mbr = providers.getMessageBodyReader(type, genericType, EMPTY_ANNOTATIONS,
                        mediaType);
                if (mbr == null) {
                    throw new ProcessingException("No MessageBodyReader found to handle class type, " + type
                            + " using MediaType, " + mediaType);
                }
                return mbr.readFrom(type, genericType, EMPTY_ANNOTATIONS, mediaType, new MultivaluedHashMap<>(),
                        new ByteArrayInputStream(data.getBytes()));
            } catch (Exception ex) {
                throw new ProcessingException(ex);
            }
        }

        private MediaType guessMediaType(String dataString) {
            if (dataString != null) {
                if (dataString.startsWith("<")) {
                    return MediaType.APPLICATION_XML_TYPE;
                }
                if (dataString.startsWith("{")) {
                    return MediaType.APPLICATION_JSON_TYPE;
                }
            }
            return MediaType.WILDCARD_TYPE;
        }

        @Override
        public String readData() {
            return data;
        }

        @Override
        public boolean isEmpty() {
            return data == null || data.isEmpty();
        }
    }
}