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

package org.apache.cxf.systests.cdi.base;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.staxutils.StaxUtils;

@Produces({"application/atom+xml", "application/atom+xml;type=feed", "application/json" })
@Consumes({"application/atom+xml", "application/atom+xml;type=feed" })
@Provider
public class AtomFeedProvider implements MessageBodyWriter<AtomFeed> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return AtomFeed.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(AtomFeed t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) 
                throws IOException, WebApplicationException {
        final XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(entityStream);
        try {
            writer.writeStartDocument();
            writer.writeStartElement("feed");
            if (t.getId() != null) {
                writer.writeAttribute("id", t.getId());
            }
            if (t.getLanguage() != null) {
                writer.writeAttribute("language", t.getLanguage());
            }
            writer.writeStartElement("entries");
            for (final AtomFeedEntry entry: t.getEntries()) {
                writer.writeStartElement("entry");
                if (entry.getLink() != null) {
                    writer.writeAttribute("link", entry.getLink());
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndDocument();
        } catch (XMLStreamException ex) {
            throw new IOException(ex);
        } finally {
            try {
                writer.close();
            } catch (XMLStreamException ex) {
                throw new IOException(ex);
            }
        }
    }
}