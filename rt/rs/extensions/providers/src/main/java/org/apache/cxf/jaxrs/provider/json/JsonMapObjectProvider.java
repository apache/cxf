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
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObject;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;

@Produces({"application/json", "application/*+json" })
@Consumes({"application/json", "application/*+json" })
@Provider
public class JsonMapObjectProvider implements MessageBodyReader<JsonMapObject>, MessageBodyWriter<JsonMapObject> {
    private JsonMapObjectReaderWriter handler = new JsonMapObjectReaderWriter();
    @Override
    public long getSize(JsonMapObject o, Class<?> cls, Type t, Annotation[] anns, MediaType mt) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> cls, Type t, Annotation[] anns, MediaType mt) {
        return JsonMapObject.class.isAssignableFrom(cls);
    }

    @Override
    public void writeTo(JsonMapObject o, Class<?> cls, Type t, Annotation[] anns, MediaType mt,
                        MultivaluedMap<String, Object> headers, OutputStream os) throws IOException,
        WebApplicationException {
        handler.toJson(o, os);
    }

    @Override
    public boolean isReadable(Class<?> cls, Type t, Annotation[] anns, MediaType mt) {
        return JsonMapObject.class.isAssignableFrom(cls);
    }

    @Override
    public JsonMapObject readFrom(Class<JsonMapObject> cls, Type t, Annotation[] anns, MediaType mt,
                                  MultivaluedMap<String, String> headers, InputStream is) throws IOException,
        WebApplicationException {
        String s = IOUtils.readStringFromStream(is);
        try {
            JsonMapObject obj = cls == JsonMapObject.class ? new JsonMapObject() : cls.newInstance();
            handler.fromJson(obj, s);
            return obj;
        } catch (Exception ex) {
            throw new IOException(ex);
        }

    }

}
