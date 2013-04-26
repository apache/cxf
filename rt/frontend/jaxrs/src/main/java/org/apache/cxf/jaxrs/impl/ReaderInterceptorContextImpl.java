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
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import org.apache.cxf.message.Message;

public class ReaderInterceptorContextImpl extends AbstractInterceptorContextImpl 
    implements ReaderInterceptorContext {

    private List<ReaderInterceptor> readers;
    private InputStream is;
    public ReaderInterceptorContextImpl(Class<?> cls,
                                        Type type,
                                        Annotation[] anns,
                                        MediaType mt,
                                        InputStream is,
                                        Message message,
                                        List<ReaderInterceptor> readers) {
        super(cls, type, anns, mt, message);
        this.is = is;
        this.readers = readers;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return new MetadataMap<String, String>(
            (Map<String, List<String>>)m.get(Message.PROTOCOL_HEADERS), false, false, true);
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
        m.put(InputStream.class, stream);

    }

}
