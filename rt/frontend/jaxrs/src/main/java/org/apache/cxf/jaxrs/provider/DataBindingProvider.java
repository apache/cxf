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
import java.nio.charset.StandardCharsets;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.staxutils.StaxUtils;

@Provider
@Produces({"application/xml", "application/*+xml", "text/xml" })
@Consumes({"application/xml", "application/*+xml", "text/xml" })
public class DataBindingProvider<T> implements MessageBodyReader<T>, MessageBodyWriter<T> {

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

    public T readFrom(Class<T> clazz, Type genericType, Annotation[] annotations, MediaType type,
                      MultivaluedMap<String, String> headers, InputStream is)
        throws IOException {
        XMLStreamReader reader = null;
        try {
            reader = createReader(clazz, genericType, is);
            DataReader<XMLStreamReader> dataReader = binding.createReader(XMLStreamReader.class);
            Object o = dataReader.read(null, reader, clazz);
            return o == null ? null : clazz.cast(o);
        } catch (Exception ex) {
            throw ExceptionUtils.toBadRequestException(ex, null);
        } finally {
            try {
                StaxUtils.close(reader);
            } catch (XMLStreamException e) {
                // Ignore
            }
        }
    }

    protected XMLStreamReader createReader(Class<?> clazz, Type genericType, InputStream is)
        throws Exception {
        return StaxUtils.createXMLStreamReader(is);
    }

    public long getSize(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        if (byte[].class.isAssignableFrom(t.getClass())) {
            return ((byte[])t).length;
        }
        return -1;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return true;
    }

    public void writeTo(T o, Class<?> clazz, Type genericType, Annotation[] annotations,
                        MediaType m, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException {
        XMLStreamWriter writer = null;
        try {
            String enc = HttpUtils.getSetEncoding(m, headers, StandardCharsets.UTF_8.name());
            writer = createWriter(clazz, genericType, enc, os);
            writeToWriter(writer, o);
        } catch (Exception ex) {
            throw ExceptionUtils.toInternalServerErrorException(ex, null);
        } finally {
            StaxUtils.close(writer);
        }
    }

    protected void writeToWriter(XMLStreamWriter writer, Object o) throws Exception {
        DataWriter<XMLStreamWriter> dataWriter = binding.createWriter(XMLStreamWriter.class);
        dataWriter.write(o, writer);
        writer.flush();
    }

    protected XMLStreamWriter createWriter(Class<?> clazz, Type genericType, String enc, OutputStream os)
        throws Exception {
        return StaxUtils.createXMLStreamWriter(os);
    }
}

