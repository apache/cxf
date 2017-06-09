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
package org.apache.cxf.systests.cdi.base.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import static java.util.Locale.ENGLISH;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

// base class allowing to create easily children providers for tests
public abstract class CustomTestReaderWriter implements MessageBodyWriter<Object>, MessageBodyReader<Object> {
    @Override
    public boolean isReadable(final Class<?> aClass, final Type type, final Annotation[] annotations,
                              final MediaType mediaType) {
        return false;
    }

    @Override
    public Object readFrom(final Class<Object> aClass, final Type type, final Annotation[] annotations,
                           final MediaType mediaType, final MultivaluedMap<String, String> multivaluedMap,
                           final InputStream inputStream) throws IOException, WebApplicationException {
        return null;
    }

    @Override
    public boolean isWriteable(final Class<?> aClass, final Type type, final Annotation[] annotations,
                               final MediaType mediaType) {
        return mediaType.getType().contains(
                getClass().getSimpleName().replace("ReaderWriter", "").toLowerCase(ENGLISH));
    }

    @Override
    public void writeTo(final Object o, final Class<?> aClass, final Type type, final Annotation[] annotations,
                        final MediaType mediaType, final MultivaluedMap<String, Object> multivaluedMap,
                        final OutputStream outputStream) throws IOException, WebApplicationException {
        outputStream.write(getClass().getName().getBytes(StandardCharsets.UTF_8));
    }
}
