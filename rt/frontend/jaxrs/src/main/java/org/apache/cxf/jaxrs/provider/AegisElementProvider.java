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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.AegisReader;
import org.apache.cxf.aegis.AegisWriter;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.staxutils.StaxUtils;

@Provider
public final class AegisElementProvider extends AbstractAegisProvider  {
    
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType m, 
        MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException {
        AegisContext context = getAegisContext(type, genericType);
        AegisReader<XMLStreamReader> aegisReader = context.createXMLStreamReader();
        XMLStreamReader xmlStreamReader = StaxUtils.createXMLStreamReader(is);
        try {
            return aegisReader.read(xmlStreamReader);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    
    public void writeTo(Object obj, Class<?> type, Type genericType, Annotation[] anns,  
        MediaType m, MultivaluedMap<String, Object> headers, OutputStream os) 
        throws IOException {
        if (type == null) {
            type = obj.getClass();
        }
        AegisContext context = getAegisContext(type, genericType);
        org.apache.cxf.aegis.type.Type aegisType = TypeUtil.getWriteTypeStandalone(context, obj, null);
        AegisWriter<XMLStreamWriter> aegisWriter = context.createXMLStreamWriter();
        XMLStreamWriter xmlStreamWriter = StaxUtils.createXMLStreamWriter(os);
        try {
            // use type qname as element qname?
            aegisWriter.write(obj, aegisType.getSchemaType(), false, xmlStreamWriter, aegisType);
            xmlStreamWriter.close();
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }
}
