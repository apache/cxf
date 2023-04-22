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
import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public class ReaderInterceptorContextImpl extends AbstractInterceptorContextImpl
    implements ReaderInterceptorContext {

    private List<ReaderInterceptor> readers;
    private InputStream is;
    public ReaderInterceptorContextImpl(Class<?> cls,
                                        Type type,
                                        Annotation[] anns,
                                        InputStream is,
                                        Message message,
                                        List<ReaderInterceptor> readers) {
        super(cls, type, anns, message);
        this.is = is;
        this.readers = readers;
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return HttpUtils.getModifiableStringHeaders(m);
    }

    @Override
    public InputStream getInputStream() {
        return is;
    }

    @Override
    public Object proceed() throws IOException {
        if (readers == null || readers.isEmpty()) {
            return null;
        }
        ReaderInterceptor next = readers.remove(0);
        return next.aroundReadFrom(this);
    }

    @Override
    public void setInputStream(InputStream stream) {
        is = stream;
        m.setContent(InputStream.class, stream);

    }

    @Override
    public MediaType getMediaType() {
        return JAXRSUtils.toMediaType(getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
    }

    @Override
    public void setMediaType(MediaType mt) {
        if (!getMediaType().isCompatible(mt)) {
            providerSelectionPropertyChanged();
        }
        getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, JAXRSUtils.mediaTypeToString(mt));

    }

}
