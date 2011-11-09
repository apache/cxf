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
import javax.activation.DataSource;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.ext.multipart.InputStreamDataSource;

public class DataSourceProvider implements MessageBodyReader, MessageBodyWriter {
    
    public boolean isReadable(Class type, Type genericType, Annotation[] annotations, MediaType mt) {
        return isSupported(type, mt);
    }

    public Object readFrom(Class cls, Type genericType, Annotation[] annotations, 
                               MediaType type, MultivaluedMap headers, InputStream is)
        throws IOException {
        DataSource ds = new InputStreamDataSource(is, type.toString());
        return DataSource.class.isAssignableFrom(cls) ? ds : new DataHandler(ds);
    }

    public long getSize(Object t, Class type, Type genericType, Annotation[] annotations, 
                        MediaType mt) {
        return -1;
    }

    public boolean isWriteable(Class type, Type genericType, Annotation[] annotations, MediaType mt) {
        return isSupported(type, mt);
    }

    private boolean isSupported(Class<?> type, MediaType mt) {
        return  !mt.getType().equals("multipart")
            && (DataSource.class.isAssignableFrom(type) || DataHandler.class.isAssignableFrom(type));
    }
    
    public void writeTo(Object src, Class cls, Type genericType, Annotation[] annotations, 
                        MediaType type, MultivaluedMap headers, OutputStream os)
        throws IOException {
        DataSource ds = DataSource.class.isAssignableFrom(cls) 
            ? (DataSource)src : ((DataHandler)src).getDataSource();
        setContentTypeIfNeeded(type, headers, ds.getContentType());
        IOUtils.copy(ds.getInputStream(), os);
    }
    
    @SuppressWarnings("unchecked")
    private void setContentTypeIfNeeded(MediaType type, MultivaluedMap headers, String ct) {
        if (!StringUtils.isEmpty(ct) && !type.equals(MediaType.valueOf(ct))) { 
            headers.putSingle("Content-Type", ct);
        }
    }
    

}
