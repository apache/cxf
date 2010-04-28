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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;

public class PrimitiveTextProvider 
    implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

    private static boolean isSupported(Class<?> type) { 
        return InjectionUtils.isPrimitive(type);
    }
    
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return isSupported(type);
    }

    public Object readFrom(Class<Object> type, Type genType, Annotation[] anns, MediaType mt, 
                           MultivaluedMap<String, String> headers, InputStream is) throws IOException {
        
        return InjectionUtils.handleParameter(
                    IOUtils.readStringFromStream(is), 
                    type,
                    ParameterType.REQUEST_BODY, null);
        
    }

    public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return -1;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return isSupported(type);
    }

    public void writeTo(Object obj, Class<?> type, Type genType, Annotation[] anns, 
                        MediaType mt, MultivaluedMap<String, Object> headers,
                        OutputStream os) throws IOException {
        String encoding = HttpUtils.getSetEncoding(mt, headers, "UTF-8");
        os.write(obj.toString().getBytes(encoding));
    }

}
