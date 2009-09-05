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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.AegisReader;
import org.apache.cxf.aegis.AegisWriter;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.staxutils.StaxUtils;

@Provider
@Produces({"application/xml", "application/*+xml", "text/xml" })
@Consumes({"application/xml", "application/*+xml", "text/xml" })
public class AegisElementProvider<T> extends AbstractAegisProvider<T>  {
    
    public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType m, 
        MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException {

        if (genericType == null) {
            genericType = type;
        }
        
        if (type == null) {
            type = messyCastToRawType(genericType);
        }

        AegisContext context = getAegisContext(type, genericType);
        AegisType typeToRead = context.getTypeMapping().getType(genericType);
        
        AegisReader<XMLStreamReader> aegisReader = context.createXMLStreamReader();
        try {
            XMLStreamReader xmlStreamReader = createStreamReader(typeToRead, is);
            return type.cast(aegisReader.read(xmlStreamReader, typeToRead));
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    /* No better solution found yet, since the alternative is to require callers to come up with the Class for
     * (e.g.) List<foo>, which is pretty messy all by itself. */
    @SuppressWarnings("unchecked")
    private Class<T> messyCastToRawType(Type genericType) {
        if (genericType instanceof Class) {
            return (Class<T>)genericType;
        } else if (genericType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) genericType;
            return (Class<T>)pType.getRawType();
        } else {
            return null;
        }
    }

    protected XMLStreamReader createStreamReader(AegisType topType, InputStream is) 
        throws Exception {
        return StaxUtils.createXMLStreamReader(is);
    }
    
    public void writeTo(T obj, Class<?> type, Type genericType, Annotation[] anns,  
        MediaType m, MultivaluedMap<String, Object> headers, OutputStream os) 
        throws IOException {
        if (type == null) {
            type = obj.getClass();
        }
        if (genericType == null) {
            genericType = type;
        }
        AegisContext context = getAegisContext(type, genericType);
        AegisType aegisType = context.getTypeMapping().getType(genericType);
        AegisWriter<XMLStreamWriter> aegisWriter = context.createXMLStreamWriter();
        try {
            XMLStreamWriter xmlStreamWriter = createStreamWriter(aegisType.getSchemaType(), os);
            // use type qname as element qname?
            xmlStreamWriter.writeStartDocument();
            aegisWriter.write(obj, aegisType.getSchemaType(), false, xmlStreamWriter, aegisType);
            xmlStreamWriter.writeEndDocument();
            xmlStreamWriter.close();
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }
    
    protected XMLStreamWriter createStreamWriter(QName typeQName, 
                                                 OutputStream os) throws Exception {
        return StaxUtils.createXMLStreamWriter(os);
    }
}
