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
package org.apache.cxf.jaxrs.nio;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

@Provider
public class NioMessageBodyWriter implements MessageBodyWriter<NioWriteEntity> {
    boolean is31;
    
    public NioMessageBodyWriter() {
        try {
            ClassLoaderUtils.loadClass("jakarta.servlet.WriteListener", HttpServletRequest.class);
            is31 = true;
        } catch (Throwable t) {
            is31 = false;
        }

    }

    @Override
    public boolean isWriteable(Class<?> cls, Type type, Annotation[] anns, MediaType mt) {
        return is31 && NioWriteEntity.class.isAssignableFrom(cls) && getContinuationProvider() != null;
    }

    @Override
    public void writeTo(NioWriteEntity entity, Class<?> cls, Type t, Annotation[] anns,
            MediaType mt, MultivaluedMap<String, Object> headers, OutputStream os)
                throws IOException, WebApplicationException {
        Continuation cont = getContinuationProvider().getContinuation();
        NioWriteListenerImpl listener = new NioWriteListenerImpl(cont, entity, os);
        Message m = JAXRSUtils.getCurrentMessage();
        m.put(WriteListener.class, listener);
        // return the current thread to the pool
        cont.suspend(0);
    }

    @Override
    public long getSize(NioWriteEntity t, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return -1;
    }
    private ContinuationProvider getContinuationProvider() {
        return (ContinuationProvider)JAXRSUtils.getCurrentMessage().getExchange()
            .getInMessage().get(ContinuationProvider.class.getName());
    }
}
