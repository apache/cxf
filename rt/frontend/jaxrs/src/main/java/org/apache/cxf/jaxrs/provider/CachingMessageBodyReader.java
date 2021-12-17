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
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;

@Provider
public class CachingMessageBodyReader<T> extends AbstractCachingMessageProvider<T>
    implements MessageBodyReader<T> {

    private List<MessageBodyReader<T>> delegatingReaders;

    public boolean isReadable(Class<?> type, Type gType, Annotation[] anns, MediaType mt) {
        if (delegatingReaders != null) {
            return getDelegatingReader(type, gType, anns, mt) != null;
        }
        return isProviderKeyNotSet();
    }

    private MessageBodyReader<T> getDelegatingReader(Class<?> type, Type gType, Annotation[] anns, MediaType mt) {
        for (MessageBodyReader<T> reader: delegatingReaders) {
            if (reader.isReadable(type, gType, anns, mt)) {
                return reader;
            }
        }
        return null;
    }

    public T readFrom(Class<T> type, Type gType, Annotation[] anns, MediaType mt,
                        MultivaluedMap<String, String> theheaders, InputStream is)
        throws IOException, WebApplicationException {
        this.setObject(
            getReader(type, gType, anns, mt).readFrom(type, gType, anns, mt, theheaders, is));
        return getObject();
    }


    protected MessageBodyReader<T> getReader(Class<?> type, Type gType, Annotation[] anns, MediaType mt) {
        if (delegatingReaders != null) {
            return getDelegatingReader(type, gType, anns, mt);
        }
        final MessageBodyReader<T> r;

        mc.put(ACTIVE_JAXRS_PROVIDER_KEY, this);
        try {
            @SuppressWarnings("unchecked")
            Class<T> actualType = (Class<T>)type;
            r = mc.getProviders().getMessageBodyReader(actualType, gType, anns, mt);
        } finally {
            mc.put(ACTIVE_JAXRS_PROVIDER_KEY, null);
        }

        if (r == null) {
            org.apache.cxf.common.i18n.Message message =
                new org.apache.cxf.common.i18n.Message("NO_MSG_READER", BUNDLE, type);
            LOG.severe(message.toString());
            throw ExceptionUtils.toNotAcceptableException(null, null);
        }
        return r;
    }

    public void setDelegatingReader(MessageBodyReader<T> reader) {
        this.delegatingReaders = Collections.singletonList(reader);
    }

    public void setDelegatingReaders(List<MessageBodyReader<T>> readers) {
        this.delegatingReaders = readers;
    }

}
