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

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.jaxrs.provider.StringTextProvider;

public class StringTextWriter extends StringTextProvider {

    @Context
    private UriInfo ui;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return false;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        String path = ui.getAbsolutePath().toString();
        if (path.endsWith("/webapp/resources/bookstore/nonexistent")) {
            return super.isWriteable(type, genericType, annotations, mt);
        }
        return false;
    }

    public void writeTo(String obj, Class<?> type, Type genType, Annotation[] anns,
                        MediaType mt, MultivaluedMap<String, Object> headers,
                        OutputStream os) throws IOException {
        if (type == String.class && type == genType) {
            obj = "Nonexistent method".equals(obj) ? "StringTextWriter - " + obj : obj;
            super.writeTo(obj, type, genType, anns, mt, headers, os);
            return;
        }
        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);

    }
}
