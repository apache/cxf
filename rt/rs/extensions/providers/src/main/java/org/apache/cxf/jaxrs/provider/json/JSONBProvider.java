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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.CompletionStage;
import java.util.logging.Logger;

import javax.json.bind.Jsonb;
import javax.json.bind.spi.JsonbProvider;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;

@Produces({ "*/*", "application/json", "application/*+json" })
@Consumes({ "*/*", "application/json", "application/*+json"})
@Provider
public class JSONBProvider implements MessageBodyWriter<Object>, MessageBodyReader<Object> {

    private static final Logger LOG = LogUtils.getL7dLogger(JSONBProvider.class);
    private final Jsonb jsonb;
    private final Iterable<ProviderInfo<ContextResolver<?>>> contextResolvers;

    public JSONBProvider(ProviderFactory factory) {
        this.contextResolvers = factory.getContextResolvers();

        try {
            JsonbProvider provider = AccessController.doPrivileged(new PrivilegedExceptionAction<JsonbProvider>() {

                @Override
                public JsonbProvider run() throws Exception {
                    // first try thread context classloader
                    Iterator<JsonbProvider> providers = ServiceLoader.load(JsonbProvider.class).iterator();
                    if (providers.hasNext()) {
                        return providers.next();
                    }
                    // next try this classloader
                    providers = ServiceLoader.load(JsonbProvider.class, JSONBProvider.class.getClassLoader())
                        .iterator();
                    if (providers.hasNext()) {
                        return providers.next();
                    }

                    LOG.warning(() -> {
                        return "Cannot find a suitable JSON-B Provider"; });
                    throw new RuntimeException("Cannot find a suitable JSON-B Provider");
                } });
            if (provider != null) {
                this.jsonb = provider.create().build();
            } else {
                this.jsonb = null;
            }
        } catch (PrivilegedActionException ex) {
            Throwable t = ex.getCause();
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new IllegalArgumentException(t);
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return !isJSONPClass(type) && isJsonType(mediaType);
    }

    @Override
    public Object readFrom(Class<Object> clazz, Type genericType, Annotation[] annotations,
                           MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                               throws IOException, WebApplicationException {
        Object obj = null;
        // For most generic return types, we want to use the genericType so as to ensure
        // that the generic value is not lost on conversion - specifically in collections.
        // But for CompletionStage<SomeType> we want to use clazz to pull the right value
        // - and then client code will handle the result, storing it in the CompletionStage.
        if ((genericType instanceof ParameterizedType)
            && CompletionStage.class.equals(((ParameterizedType)genericType).getRawType())) {
            obj = getJsonb().fromJson(entityStream, clazz);
        } else {
            obj = getJsonb().fromJson(entityStream, genericType);
        }

        return obj;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return !isJSONPClass(type) && isJsonType(mediaType);
    }

    private boolean isJSONPClass(Class<?> clazz) {
        final String[] jsonpClasses = new String[] {"javax.json.JsonArray", "javax.json.JsonObject",
                                                    "javax.json.JsonStructure" };
        for (String c : jsonpClasses) {
            if (clazz.getName().equals(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean isJsonType(MediaType mediaType) {
        return mediaType.getSubtype().toLowerCase().startsWith("json")
                        || mediaType.getSubtype().toLowerCase().contains("+json");
    }

    @Override
    public void writeTo(Object obj, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                            throws IOException, WebApplicationException {
        getJsonb().toJson(obj, entityStream);

    }

    private Jsonb getJsonb() {
        for (ProviderInfo<ContextResolver<?>> crPi : contextResolvers) {
            ContextResolver<?> cr = crPi.getProvider();
            Object o = cr.getContext(null);
            if (o instanceof Jsonb) {
                return (Jsonb) o;
            }
        }

        return this.jsonb;
    }
}
