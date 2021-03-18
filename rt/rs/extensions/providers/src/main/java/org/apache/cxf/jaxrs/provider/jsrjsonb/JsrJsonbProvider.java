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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.json.JsonException;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.utils.ExceptionUtils;

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
    private final Jsonb jsonb;
     
    public JsrJsonbProvider() {
        this(JsonbBuilder.create()); 
    }
    
    public JsrJsonbProvider(Jsonb jsonb) {
        this.jsonb = jsonb; 
    }
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isSupportedMediaType(mediaType);
    }

    @Override
    public void writeTo(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) 
                throws IOException, WebApplicationException {
        jsonb.toJson(t, type, entityStream);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isSupportedMediaType(mediaType);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) 
                throws IOException, WebApplicationException {
        try {
            if (genericType == null) {
                return jsonb.fromJson(entityStream, type);
            } else {
                return jsonb.fromJson(entityStream, genericType);
            }
        } catch (JsonException ex) {
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

}
