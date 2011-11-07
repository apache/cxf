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

import javax.activation.DataHandler;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.ext.multipart.InputStreamDataSource;

public class DataHandlerProvider implements MessageBodyReader<DataHandler>, 
    MessageBodyWriter<DataHandler> {
    
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return isSupported(type);
    }

    public DataHandler readFrom(Class<DataHandler> clazz, Type genericType, Annotation[] annotations, 
                               MediaType type, MultivaluedMap<String, String> headers, InputStream is)
        throws IOException {
        return new DataHandler(new InputStreamDataSource(is, type.toString()));
    }
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return isSupported(type);
    }


    public long getSize(DataHandler t, Class<?> type, Type genericType, Annotation[] annotations, 
                        MediaType mt) {
        return -1;
    }
    
    private boolean isSupported(Class<?> type) {
        return DataHandler.class.isAssignableFrom(type);
    }
    
    public void writeTo(DataHandler src, Class<?> clazz, Type genericType, Annotation[] annotations, 
                        MediaType type, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException {
        IOUtils.copy(src.getInputStream(), os);
    }
    
    

}
