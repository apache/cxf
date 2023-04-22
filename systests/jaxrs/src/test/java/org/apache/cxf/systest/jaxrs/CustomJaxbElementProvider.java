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
package org.apache.cxf.systest.jaxrs;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.annotations.SchemaValidation;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;

public class CustomJaxbElementProvider extends JAXBElementProvider<Book> {
    @Context
    private UriInfo ui;
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {
        if (ui.getRequestUri().toString().endsWith("/test/5/bookstorestorage/thosebooks/123")) {
            for (Annotation ann : anns) {
                if (ann.annotationType() == SchemaValidation.class) {
                    return super.isWriteable(type, genericType, anns, mt);
                }
            }
            throw new RuntimeException();
        }
        return super.isWriteable(type, genericType, anns, mt);

    }
    @Override
    public void writeTo(Book obj, Class<?> cls, Type genericType, Annotation[] anns,
                        MediaType m, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException {
        headers.putSingle("Content-Type", MediaType.valueOf(m.toString() + ";a=b"));
        super.writeTo(obj, cls, genericType, anns, m, headers, os);
    }
}
