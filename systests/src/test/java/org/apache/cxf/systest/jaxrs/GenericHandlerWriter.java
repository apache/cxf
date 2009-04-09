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
package org.apache.cxf.systest.jaxrs;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.jaxrs.utils.InjectionUtils;

public class GenericHandlerWriter implements MessageBodyWriter<GenericHandler<Book>> {

    public long getSize(GenericHandler<Book> t, Class<?> type, Type genericType, Annotation[] annotations, 
                        MediaType mediaType) {        
        return -1;
    }

    public boolean isWriteable(Class<?> type, Type genericType, 
                               Annotation[] annotations, MediaType mediaType) {
        return type == GenericHandler.class && InjectionUtils.getActualType(genericType) == Book.class;
    }

    public void writeTo(GenericHandler<Book> o, Class<?> c, Type t, Annotation[] anns, MediaType m,
                        MultivaluedMap<String, Object> headers, OutputStream os) 
        throws IOException, WebApplicationException {
        JAXBElementProvider jaxb = new JAXBElementProvider();
        jaxb.writeTo(o.getEntity(), c, InjectionUtils.getActualType(t), anns, m, headers, os);
    }


}
