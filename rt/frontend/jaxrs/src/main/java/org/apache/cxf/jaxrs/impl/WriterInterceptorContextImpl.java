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
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public class WriterInterceptorContextImpl extends AbstractInterceptorContextImpl
    implements WriterInterceptorContext {

    private List<WriterInterceptor> writers;
    private OutputStream os;
    private Object entity;
    //CHECKSTYLE:OFF
    public WriterInterceptorContextImpl(Object entity,
                                        Class<?> cls,
                                        Type type,
                                        Annotation[] anns,
                                        OutputStream os,
                                        Message message,
                                        List<WriterInterceptor> writers) {
    //CHECKSTYLE:ON
        super(cls, type, anns, message);
        this.entity = entity;
        this.os = os;
        this.writers = writers;
    }


    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return HttpUtils.getModifiableHeaders(m);
    }

    @Override
    public OutputStream getOutputStream() {
        return os;
    }

    @Override
    public void proceed() throws IOException {
        if (writers == null || writers.isEmpty()) {
            return;
        }
        WriterInterceptor next = writers.remove(0);
        try {
            next.aroundWriteTo(this);
        } catch (IOException ex) {
            throw ex;
        }
    }

    @Override
    public void setEntity(Object object) {
        entity = object;
        super.setType(entity != null ? entity.getClass() : null);
    }

    @Override
    public void setOutputStream(OutputStream stream) {
        this.os = stream;
        m.put(OutputStream.class, stream);

    }


    @Override
    public MediaType getMediaType() {
        Object value = getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        return value instanceof MediaType ? (MediaType)value : JAXRSUtils.toMediaType((String)value);
    }


    @Override
    public void setMediaType(MediaType mt) {
        if (!getMediaType().isCompatible(mt)) {
            providerSelectionPropertyChanged();
        }
        m.put(Message.CONTENT_TYPE, JAXRSUtils.mediaTypeToString(mt));
        getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, mt);
    }

}
