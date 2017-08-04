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
package org.apache.cxf.jaxrs.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

// set of wrappers allowing to override mediatype for the wrapped provider.
// a real life sample is jackson which abuses */*.
public abstract class ConfigurableProviderWrapper extends AbstractConfigurableProvider {
    public static class Reader<T> extends ConfigurableProviderWrapper implements MessageBodyReader<T> {
        private final MessageBodyReader<T> delegate;

        public Reader(final MessageBodyReader<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean isReadable(final Class<?> aClass, final Type type, final Annotation[] annotations,
                                  final MediaType mediaType) {
            return delegate.isReadable(aClass, type, annotations, mediaType);
        }

        @Override
        public T readFrom(final Class<T> aClass, final Type type, final Annotation[] annotations,
                          final MediaType mediaType, final MultivaluedMap<String, String> multivaluedMap,
                          final InputStream inputStream) throws IOException, WebApplicationException {
            return delegate.readFrom(aClass, type, annotations, mediaType, multivaluedMap, inputStream);
        }
    }

    public static class Writer<T> extends ConfigurableProviderWrapper implements MessageBodyWriter<T> {
        private final MessageBodyWriter<T> delegate;

        public Writer(final MessageBodyWriter<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean isWriteable(final Class<?> aClass, final Type type, final Annotation[] annotations,
                                   final MediaType mediaType) {
            return delegate.isWriteable(aClass, type, annotations, mediaType);
        }

        @Override
        public long getSize(final T t, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType) {
            return delegate.getSize(t, type, genericType, annotations, mediaType);
        }

        @Override
        public void writeTo(final T t, final Class<?> aClass, final Type type, final Annotation[] annotations,
                            final MediaType mediaType, final MultivaluedMap<String, Object> multivaluedMap,
                            final OutputStream outputStream) throws IOException, WebApplicationException {
            delegate.writeTo(t, aClass, type, annotations, mediaType, multivaluedMap, outputStream);
        }
    }

    public static class ReaderWriter<T> extends ConfigurableProviderWrapper implements
            MessageBodyWriter<T>, MessageBodyReader<T> {
        private final MessageBodyReader<T> reader;
        private final MessageBodyWriter<T> writer;

        public ReaderWriter(final MessageBodyReader<T> reader, final MessageBodyWriter<T> writer) {
            this.reader = reader;
            this.writer = writer;
        }

        @Override
        public boolean isWriteable(final Class<?> aClass, final Type type, final Annotation[] annotations,
                                   final MediaType mediaType) {
            return writer.isWriteable(aClass, type, annotations, mediaType);
        }

        @Override
        public long getSize(final T t, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType) {
            return writer.getSize(t, type, genericType, annotations, mediaType);
        }

        @Override
        public void writeTo(final T t, final Class<?> aClass, final Type type, final Annotation[] annotations,
                            final MediaType mediaType, final MultivaluedMap<String, Object> multivaluedMap,
                            final OutputStream outputStream) throws IOException, WebApplicationException {
            writer.writeTo(t, aClass, type, annotations, mediaType, multivaluedMap, outputStream);
        }

        @Override
        public boolean isReadable(final Class<?> aClass, final Type type, final Annotation[] annotations,
                                  final MediaType mediaType) {
            return reader.isReadable(aClass, type, annotations, mediaType);
        }

        @Override
        public T readFrom(final Class<T> aClass, final Type type, final Annotation[] annotations,
                          final MediaType mediaType, final MultivaluedMap<String, String> multivaluedMap,
                          final InputStream inputStream) throws IOException, WebApplicationException {
            return reader.readFrom(aClass, type, annotations, mediaType, multivaluedMap, inputStream);
        }
    }
}
