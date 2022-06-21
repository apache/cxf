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
package org.apache.cxf.jaxrs.provider.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.provider.AbstractConfigurableProvider;
import org.apache.cxf.jaxrs.provider.PrimitiveTextProvider;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

@Produces("application/json")
@Consumes("application/json")
public class SimpleTypeJsonProvider<T> extends AbstractConfigurableProvider
    implements MessageBodyWriter<T>, MessageBodyReader<T> {

    @Context
    private Providers providers;
    private boolean supportSimpleTypesOnly;
    private PrimitiveTextProvider<T> primitiveHelper = new PrimitiveTextProvider<>();
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return !supportSimpleTypesOnly || primitiveHelper.isReadable(type, genericType, annotations, mediaType);
    }

    @Override
    public long getSize(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException, WebApplicationException {
        if (!supportSimpleTypesOnly && !InjectionUtils.isPrimitive(type)) {
            @SuppressWarnings("unchecked")
            MessageBodyWriter<T> next =
                (MessageBodyWriter<T>)providers.getMessageBodyWriter(type, genericType, annotations, mediaType);
            JAXRSUtils.getCurrentMessage().put(ProviderFactory.ACTIVE_JAXRS_PROVIDER_KEY, this);
            try {
                next.writeTo(t, type, genericType, annotations, mediaType, headers, os);
            } finally {
                JAXRSUtils.getCurrentMessage().put(ProviderFactory.ACTIVE_JAXRS_PROVIDER_KEY, null);
            }
        } else {
            os.write(StringUtils.toBytesASCII("{\"" + type.getSimpleName().toLowerCase() + "\":"));
            writeQuote(os, type);
            primitiveHelper.writeTo(t, type, genericType, annotations, mediaType, headers, os);
            writeQuote(os, type);
            os.write(StringUtils.toBytesASCII("}"));
        }
    }

    private void writeQuote(OutputStream os, Class<?> type) throws IOException {
        if (type == String.class) {
            os.write(StringUtils.toBytesASCII("\""));
        }
    }
    public void setSupportSimpleTypesOnly(boolean supportSimpleTypesOnly) {
        this.supportSimpleTypesOnly = supportSimpleTypesOnly;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isWriteable(type, genericType, annotations, mediaType);
    }

    @Override
    public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                      MultivaluedMap<String, String> headers, InputStream is)
        throws IOException, WebApplicationException {
        if (!supportSimpleTypesOnly && !InjectionUtils.isPrimitive(type)) {
            MessageBodyReader<T> next =
                providers.getMessageBodyReader(type, genericType, annotations, mediaType);
            JAXRSUtils.getCurrentMessage().put(ProviderFactory.ACTIVE_JAXRS_PROVIDER_KEY, this);
            try {
                return next.readFrom(type, genericType, annotations, mediaType, headers, is);
            } finally {
                JAXRSUtils.getCurrentMessage().put(ProviderFactory.ACTIVE_JAXRS_PROVIDER_KEY, null);
            }
        }
        String data = IOUtils.toString(is).trim();
        int index = data.indexOf(':');
        data = data.substring(index + 1, data.length() - 1).trim();
        if (data.startsWith("\"")) {
            data = data.substring(1, data.length() - 1);
        }
        return primitiveHelper.readFrom(type, genericType, annotations, mediaType, headers,
                                        new ByteArrayInputStream(StringUtils.toBytesUTF8(data)));
    }

}
