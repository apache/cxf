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

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

@Provider
public class CachingMessageBodyWriter<T> extends AbstractCachingMessageProvider<T>
    implements MessageBodyWriter<T> {
    
    private MessageBodyWriter<T> delegatingWriter;
    
    public long getSize(T t, Class<?> type, Type gType, Annotation[] anns, MediaType mediaType) {
        return -1;
    }

    public boolean isWriteable(Class<?> type, Type gType, Annotation[] anns, MediaType mt) {
        if (delegatingWriter != null) {
            return delegatingWriter.isWriteable(type, gType, anns, mt);
        } else {
            return isProviderKeyNotSet();
        }
    }

    
    public void writeTo(T obj, Class<?> type, Type gType, Annotation[] anns, MediaType mt,
                        MultivaluedMap<String, Object> theheaders, OutputStream os) 
        throws IOException, WebApplicationException {
        this.setObject(obj);
        getWriter(type, gType, anns, mt).writeTo(getObject(), type, gType, anns, mt, theheaders, os);
    }
    
    
    protected MessageBodyWriter<T> getWriter(Class<?> type, Type gType, Annotation[] anns, MediaType mt) {
        if (delegatingWriter != null) {
            return delegatingWriter;
        }
        MessageBodyWriter<T> w = null;
        
        mc.put(ACTIVE_JAXRS_PROVIDER_KEY, this);
        try {
            @SuppressWarnings("unchecked")
            Class<T> actualType = (Class<T>)type;
            w = mc.getProviders().getMessageBodyWriter(actualType, gType, anns, mt);
        } finally {
            mc.put(ACTIVE_JAXRS_PROVIDER_KEY, null); 
        }
        
        if (w == null) {
            org.apache.cxf.common.i18n.Message message = 
                new org.apache.cxf.common.i18n.Message("NO_MSG_WRITER", BUNDLE, type);
            LOG.severe(message.toString());
            throw new InternalServerErrorException();
        }
        return w;
    }
}
