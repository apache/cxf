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
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import org.apache.cxf.jaxrs.ext.StreamingResponse;
import org.apache.cxf.jaxrs.utils.InjectionUtils;

public class StreamingResponseProvider<T> extends AbstractConfigurableProvider
    implements MessageBodyWriter<StreamingResponse<T>> {

    @Context
    private Providers providers;

    @Override
    public boolean isWriteable(Class<?> cls, Type type, Annotation[] anns, MediaType mt) {
        return StreamingResponse.class.isAssignableFrom(cls);
    }

    @Override
    public void writeTo(StreamingResponse<T> p, Class<?> cls, Type t, Annotation[] anns,
                        MediaType mt, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException, WebApplicationException {
        Class<?> actualCls = InjectionUtils.getActualType(t);
        if (cls == actualCls) {
            actualCls = Object.class;
        }
        //TODO: review the possibility of caching the providers
        StreamingResponseWriter thewriter = new StreamingResponseWriter(actualCls, anns, mt, headers, os);
        p.writeTo(thewriter);
    }

    @Override
    public long getSize(StreamingResponse<T> arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    private class StreamingResponseWriter implements StreamingResponse.Writer<T> {
        private volatile MessageBodyWriter<T> writer;
        private Class<?> entityCls;
        private MediaType mt;
        private Annotation[] anns;
        private MultivaluedMap<String, Object> headers;
        private OutputStream os;

        StreamingResponseWriter(Class<?> entityCls,
                                Annotation[] anns,
                                MediaType mt,
                                MultivaluedMap<String, Object> headers,
                                OutputStream os) {
            this.entityCls = entityCls;
            this.anns = anns;
            this.mt = mt;
            this.headers = headers;
            this.os = os;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void write(T data) throws IOException {
            Class<?> actualCls = entityCls != Object.class ? entityCls : data.getClass();
            if (writer == null) {
                writer = (MessageBodyWriter<T>)providers.getMessageBodyWriter(actualCls, actualCls, anns, mt);
                if (writer == null) {
                    throw new InternalServerErrorException();
                }
            }
            writer.writeTo(data, actualCls, actualCls, anns, mt, headers, os);
        }

        @Override
        public OutputStream getEntityStream() {
            return os;
        }

    }
}
