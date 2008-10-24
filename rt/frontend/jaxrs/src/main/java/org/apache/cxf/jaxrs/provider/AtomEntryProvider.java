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
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.writer.Writer;

@Produces({"application/atom+xml", "application/atom+xml;type=entry", "application/json" })
@Consumes({"application/atom+xml", "application/atom+xml;type=entry" })
@Provider
public class AtomEntryProvider 
    implements MessageBodyReader<Entry>, MessageBodyWriter<Entry> {

    private static final Abdera ATOM_ENGINE = new Abdera();
    private static final String JSON_TYPE = "application/json";
    
    public long getSize(Entry feed,
                        Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return -1;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return Entry.class.isAssignableFrom(type);
    }

    public void writeTo(Entry entry, Class<?> clazz, Type type, Annotation[] a, 
                        MediaType mt, MultivaluedMap<String, Object> headers, OutputStream os) 
        throws IOException {
        if (JSON_TYPE.equals(mt.toString())) {
            Writer w = ATOM_ENGINE.getWriterFactory().getWriter("json");
            entry.writeTo(w, os);   
        } else {
            entry.writeTo(os);
        }
        
    }

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return Entry.class.isAssignableFrom(type);
    }

    public Entry readFrom(Class<Entry> clazz, Type t, Annotation[] a, MediaType mt, 
                          MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException {
        Document<Entry> doc = ATOM_ENGINE.getParser().parse(is);
        return doc.getRoot();
    }
}
