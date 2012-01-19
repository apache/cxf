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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;

public class BinaryDataProvider extends AbstractConfigurableProvider 
    implements MessageBodyReader<Object>, MessageBodyWriter<Object> {
    
    private static final String HTTP_RANGE_PROPERTY = "http.range.support";
    
    private static final int BUFFER_SIZE = 4096;

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return byte[].class.isAssignableFrom(type)
               || InputStream.class.isAssignableFrom(type)
               || Reader.class.isAssignableFrom(type);
    }

    public Object readFrom(Class<Object> clazz, Type genericType, Annotation[] annotations, MediaType type, 
                           MultivaluedMap<String, String> headers, InputStream is)
        throws IOException {
        if (InputStream.class.isAssignableFrom(clazz)) {
            return is;
        }
        if (Reader.class.isAssignableFrom(clazz)) {
            return new InputStreamReader(is, getEncoding(type));
        }
        if (byte[].class.isAssignableFrom(clazz)) {
            return IOUtils.readBytesFromStream(is);
        }
        throw new IOException("Unrecognized class");
    }

    public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        // TODO: if it's a range request, then we should probably always return -1 and set 
        // Content-Length and Content-Range in handleRangeRequest
        if (byte[].class.isAssignableFrom(t.getClass())) {
            return ((byte[])t).length;
        }
        return -1;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return byte[].class.isAssignableFrom(type)
            || InputStream.class.isAssignableFrom(type)
            || File.class.isAssignableFrom(type)
            || Reader.class.isAssignableFrom(type)
            || StreamingOutput.class.isAssignableFrom(type);
    }

    public void writeTo(Object o, Class<?> clazz, Type genericType, Annotation[] annotations, 
                        MediaType type, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException {
        
        if (InputStream.class.isAssignableFrom(o.getClass())) {
            copyInputToOutput((InputStream)o, os, headers);
        } else if (File.class.isAssignableFrom(o.getClass())) {
            copyInputToOutput(new BufferedInputStream(
                    new FileInputStream((File)o)), os, headers);
        } else if (byte[].class.isAssignableFrom(o.getClass())) {
            copyInputToOutput(new ByteArrayInputStream((byte[])o), os, headers);
        } else if (Reader.class.isAssignableFrom(o.getClass())) {
            try {
                Writer writer = new OutputStreamWriter(os, getEncoding(type));
                IOUtils.copy((Reader)o, 
                              writer,
                              BUFFER_SIZE);
                writer.flush();
            } finally {
                ((Reader)o).close();
            }
            
        } else if (StreamingOutput.class.isAssignableFrom(o.getClass())) {
            ((StreamingOutput)o).write(os);
        } else {
            throw new IOException("Unrecognized class");
        }

    }
    
    private String getEncoding(MediaType mt) {
        String enc = mt.getParameters().get("charset");
        return enc == null ? "UTF-8" : enc;
    }
    
    protected void copyInputToOutput(InputStream is, OutputStream os,
            MultivaluedMap<String, Object> outHeaders) throws IOException {
        if (isRangeSupported()) {
            Message inMessage = PhaseInterceptorChain.getCurrentMessage().getExchange().getInMessage();
            handleRangeRequest(is, os, new HttpHeadersImpl(inMessage), outHeaders);
        } else {
            IOUtils.copyAndCloseInput(is, os);
        }
    }
    
    protected void handleRangeRequest(InputStream is, 
                                      OutputStream os,
                                      HttpHeaders inHeaders, 
                                      MultivaluedMap<String, Object> outHeaders) throws IOException {
        String range = inHeaders.getRequestHeaders().getFirst("Range"); 
        if (range == null) {
            IOUtils.copyAndCloseInput(is, os);    
        } else {
            // implement
        }
           
    }
    
    protected boolean isRangeSupported() {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        if (message != null) {
            return MessageUtils.isTrue(message.get(HTTP_RANGE_PROPERTY));
        } else {
            return false;
        }
    }

}
