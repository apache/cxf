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
package org.apache.cxf.jaxrs.provider.jsrjsonp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;

@Produces({"application/json", "application/*+json" })
@Consumes({"application/json", "application/*+json" })
@Provider
public class JsrJsonpProvider implements MessageBodyReader<JsonStructure>, MessageBodyWriter<JsonStructure> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return JsonStructure.class.isAssignableFrom(type)
                || JsonObject.class.isAssignableFrom(type)
                || JsonArray.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(JsonStructure t, Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType) {

        return -1;
    }

    @Override
    public void writeTo(JsonStructure t, Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
        throws IOException, WebApplicationException {

        if (entityStream == null) {
            throw new IOException("Initialized OutputStream should be provided");
        }

        try (JsonWriter writer = Json.createWriter(entityStream)) {
            writer.write(t);
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return JsonStructure.class.isAssignableFrom(type)
                || JsonObject.class.isAssignableFrom(type)
                || JsonArray.class.isAssignableFrom(type);
    }

    @Override
    public JsonStructure readFrom(Class<JsonStructure> type, Type genericType, Annotation[] annotations,
        MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
        throws IOException, WebApplicationException {

        if (entityStream == null) {
            throw new IOException("Initialized InputStream should be provided");
        }

        try (JsonReader reader = Json.createReader(entityStream)) {
            return reader.read();
        } catch (JsonException ex) {
            throw ExceptionUtils.toBadRequestException(ex, null);
        }
    }
}
