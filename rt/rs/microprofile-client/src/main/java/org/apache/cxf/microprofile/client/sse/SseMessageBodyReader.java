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
package org.apache.cxf.microprofile.client.sse;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ExecutorService;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.sse.InboundSseEvent;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.impl.ProvidersImpl;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProviders;
import org.apache.cxf.microprofile.client.Utils;
import org.reactivestreams.Publisher;

@Produces(MediaType.SERVER_SENT_EVENTS)
public class SseMessageBodyReader implements MessageBodyReader<Publisher<?>> {

    @Context
    Providers providers;

    @Context
    private MessageContext mc;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Publisher.class.isAssignableFrom(type) && MediaType.SERVER_SENT_EVENTS_TYPE.isCompatible(mediaType);
    }

    @Override
    public Publisher<?> readFrom(Class<Publisher<?>> type, Type genericType,
            Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream) throws IOException, WebApplicationException {
        ProvidersImpl providersImpl = (ProvidersImpl) (providers instanceof ThreadLocalProviders
            ? ((ThreadLocalProviders)providers).get() : providers);
        ExecutorService executor = Utils.getExecutorService(mc);
        SsePublisher publisher = new SsePublisher(entityStream, executor, providersImpl);
        if (genericType instanceof ParameterizedType) {
            Type typeArgument = ((ParameterizedType)genericType).getActualTypeArguments()[0];
            if (typeArgument.equals(InboundSseEvent.class)) {
                return publisher;
            }

            return new SseTypeSafeProcessor<Object>(new GenericType<Object>(typeArgument), publisher);
        }
        return null;
    }
}