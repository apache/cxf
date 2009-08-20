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
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.staxutils.StaxUtils;

@Provider
@Produces({"application/xml", "application/*+xml", "text/xml" })
@Consumes({"application/xml", "application/*+xml", "text/xml" })
public class DataBindingProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

    private DataBinding binding;
    
    public DataBindingProvider() {
    }
    
    public DataBindingProvider(DataBinding db) {
        binding = db;
    }

    public void setDataBinding(DataBinding db) {
        binding = db;
    }
    
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return true;
    }

    public Object readFrom(Class<Object> clazz, Type genericType, Annotation[] annotations, MediaType type, 
                       MultivaluedMap<String, String> headers, InputStream is)
        throws IOException {
        try {
            XMLStreamReader reader = createReader(clazz, is);
            DataReader<XMLStreamReader> dataReader = binding.createReader(XMLStreamReader.class);
            return dataReader.read(null, reader, clazz);
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }

    protected XMLStreamReader createReader(Class<?> clazz, InputStream is) throws Exception {
        return StaxUtils.createXMLStreamReader(is);
    }
    
    public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        if (byte[].class.isAssignableFrom(t.getClass())) {
            return ((byte[])t).length;
        }
        return -1;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return true;
    }

    public void writeTo(Object o, Class<?> clazz, Type genericType, Annotation[] annotations, 
                        MediaType type, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException {
        try {
            XMLStreamWriter writer = createWriter(clazz, os);
            writeToWriter(writer, o);
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }
    
    protected void writeToWriter(XMLStreamWriter writer, Object o) throws Exception {
        DataWriter<XMLStreamWriter> dataWriter = binding.createWriter(XMLStreamWriter.class);
        dataWriter.write(o, writer);
        writer.flush();
    }
    
    protected XMLStreamWriter createWriter(Class<?> clazz, OutputStream os) throws Exception {
        return StaxUtils.createXMLStreamWriter(os);
    }
}

