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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.staxutils.StaxUtils;

@Provider
@Produces({"application/xml", "application/*+xml", "text/xml" })
@Consumes({"application/xml", "application/*+xml", "text/xml", "text/html" })
public class SourceProvider implements 
    MessageBodyReader<Object>, MessageBodyWriter<Source> {

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return Source.class.isAssignableFrom(type);
    }
    
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return Source.class.isAssignableFrom(type) 
               || XMLSource.class.isAssignableFrom(type)
               || Document.class.isAssignableFrom(type);
    }
    
    public Object readFrom(Class<Object> source, Type genericType, Annotation[] annotations, MediaType m,  
        MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException {
        if (DOMSource.class.isAssignableFrom(source) || Document.class.isAssignableFrom(source)) {
            
            boolean docRequired = Document.class.isAssignableFrom(source);
            XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
            try {
                Document doc = StaxUtils.read(reader);
                return docRequired ? doc : new DOMSource(doc);
            } catch (Exception e) {
                IOException ioex = new IOException("Problem creating a Source object");
                ioex.setStackTrace(e.getStackTrace());
                throw ioex;
            } finally {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    //ignore
                }
            }
        } else if (StreamSource.class.isAssignableFrom(source)
                   || Source.class.isAssignableFrom(source)) {
            return new StreamSource(is);
        } else if (XMLSource.class.isAssignableFrom(source)) {
            return new XMLSource(is);
        }
        
        throw new IOException("Unrecognized source");
    }

    public void writeTo(Source source, Class<?> clazz, Type genericType, Annotation[] annotations,  
        MediaType m, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException {
        
        String encoding = "utf-8"; //FIXME
        
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(source);
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(os, encoding);
        try {
            StaxUtils.copy(reader, writer);
        } catch (XMLStreamException e) {
            throw new WebApplicationException(e);
        } finally {
            try {
                reader.close();
            } catch (XMLStreamException e) {
                //ignore
            }
            try {
                writer.flush();
            } catch (XMLStreamException e) {
                //ignore
            }
        }
    }
    
    public long getSize(Source source, Class<?> type, Type genericType, Annotation[] annotations, 
                        MediaType mt) {
        return -1;
    }
}
