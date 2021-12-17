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

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.xml.bind.JAXBElement;

@SuppressWarnings("rawtypes")
public class JAXBElementTypedProvider extends JAXBElementProvider<JAXBElement>
    implements MessageBodyWriter<JAXBElement>, MessageBodyReader<JAXBElement> {

    public JAXBElement readFrom(Class<JAXBElement> type, Type genericType, Annotation[] anns, MediaType mt,
        MultivaluedMap<String, String> headers, InputStream is)
        throws IOException {
        return super.readFrom(type, genericType, anns, mt, headers, is);
    }
    public void writeTo(JAXBElement<?> obj, Class<?> cls, Type genericType, Annotation[] anns,
        MediaType m, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException {
        super.writeTo(obj, cls, genericType, anns, m, headers, os);
    }
}
