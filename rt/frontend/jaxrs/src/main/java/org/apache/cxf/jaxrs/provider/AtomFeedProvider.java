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

import javax.ws.rs.ConsumeMime;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Feed;
import org.apache.abdera.writer.Writer;

@ProduceMime({"application/atom+xml", "application/atom+xml;type=feed", "application/json" })
@ConsumeMime({"application/atom+xml", "application/atom+xml;type=feed" })
@Provider
public class AtomFeedProvider 
    implements MessageBodyWriter<Feed>, MessageBodyReader<Feed> {

    private static final String JSON_TYPE = "application/json";
    private static final Abdera ATOM_ENGINE = new Abdera();
        
    public long getSize(Feed feed) {
        return -1;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations) {
        return Feed.class.isAssignableFrom(type);
    }

    public void writeTo(Feed feed, Class<?> clazz, Type type, Annotation[] a, 
                        MediaType mt, MultivaluedMap<String, Object> headers, OutputStream os) 
        throws IOException {
        if (JSON_TYPE.equals(mt.toString())) {
            Writer w = ATOM_ENGINE.getWriterFactory().getWriter("json");
            feed.writeTo(w, os);   
        } else {
            feed.writeTo(os);
        }
        
    }

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations) {
        return Feed.class.isAssignableFrom(type);
    }

    public Feed readFrom(Class<Feed> clazz, Type t, Annotation[] a, MediaType mt, 
                         MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException {
        Document<Feed> doc = ATOM_ENGINE.getParser().parse(is);
        return doc.getRoot();
    }

}
