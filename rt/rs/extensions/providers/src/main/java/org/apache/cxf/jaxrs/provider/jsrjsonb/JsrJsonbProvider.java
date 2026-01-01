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

package org.apache.cxf.jaxrs.provider.jsrjsonb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.json.JsonException;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.Providers;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.jspecify.annotations.Nullable;

/**
 * 11.2.7 Java API for JSON Binding (JSR-370)
 * 
 * In a product that supports the Java API for JSON Binding (JSON-B) [19], implementations MUST support
 * entity providers for all Java types supported by JSON-B in combination with the following media
 * types: application/json, text/json as well as any other media types matching "*"/json or "*"/"*"+json".
 *
 * Note that if JSON-B and JSON-P are both supported in the same environment, entity providers for 
 * JSON-B take precedence over those for JSON-P for all types except JsonValue and its sub-types.
*/
@Produces({"application/json", "text/json", "application/*+json" })
@Consumes({"application/json", "text/json", "application/*+json" })
@Provider
public class JsrJsonbProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {
    @Nullable private final Jsonb jsonb;
    @Context private Providers providers;

    /**
     * Create and capture only singleton instance of the Jsonb, if needed.
     */
    private static final class DefaultJsonbSupplier {
        private static final Jsonb INSTANCE = JsonbBuilder.create();
    }
     
    public JsrJsonbProvider() {
        this(null); 
    }
    
    public JsrJsonbProvider(Jsonb jsonb) {
        this.jsonb = jsonb; 
    }
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isSupportedMediaType(mediaType) 
            && !OutputStream.class.isAssignableFrom(type)
            && !Writer.class.isAssignableFrom(type)
            && !isKnownUnsupportedInOutType(type)
            && !JAXRSUtils.isStreamingLikeOutType(type, genericType);
    }

    @Override
    public void writeTo(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) 
                throws IOException, WebApplicationException {
        jsonbFor(type).toJson(t, type, entityStream);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isSupportedMediaType(mediaType)
            && !isKnownUnsupportedInOutType(type)
            && !JAXRSUtils.isStreamingLikeOutType(type, genericType);
    }
    
    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) 
                throws IOException, WebApplicationException {
        try {
            if (genericType == null) {
                return jsonbFor(type).fromJson(entityStream, type);
            } else {
                return jsonbFor(type).fromJson(entityStream, genericType);
            }
        } catch (JsonException | JsonbException ex) {
            throw ExceptionUtils.toBadRequestException(ex, null);
        }
    }
    
    protected boolean isSupportedMediaType(MediaType mediaType) {
        if (mediaType != null) {
            final String subtype = mediaType.getSubtype();
            return "json".equalsIgnoreCase(subtype) || subtype.endsWith("+json");
        } else {
            // Return 'false' if no media type has been specified
            return false;
        }
    }

    private Jsonb jsonbFor(Class<?> type) {
        if (providers != null) {
            final ContextResolver<Jsonb> contextResolver = providers
                .getContextResolver(Jsonb.class, MediaType.APPLICATION_JSON_TYPE);
            if (contextResolver != null) {
                return contextResolver.getContext(type);
            }
        }

        if (jsonb != null) {
            return jsonb;
        } else {
            return DefaultJsonbSupplier.INSTANCE;
        }
    }

    private static boolean isKnownUnsupportedInOutType(Class<?> type) {
        return Response.class.isAssignableFrom(type)
            || CharSequence.class.isAssignableFrom(type)
            || File.class.isAssignableFrom(type)
            || byte[].class.isAssignableFrom(type);
    }
}
