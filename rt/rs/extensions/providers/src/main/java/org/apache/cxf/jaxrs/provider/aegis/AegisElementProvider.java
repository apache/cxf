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

package org.apache.cxf.jaxrs.provider.aegis;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.AegisReader;
import org.apache.cxf.aegis.AegisWriter;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
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
        XMLStreamReader xmlStreamReader = null;
        try {
            xmlStreamReader = createStreamReader(typeToRead, is);
            return type.cast(aegisReader.read(xmlStreamReader, typeToRead));
        } catch (Exception e) {
            throw ExceptionUtils.toBadRequestException(e, null);
        } finally {
            try {
                StaxUtils.close(xmlStreamReader);
            } catch (XMLStreamException e) {
                throw ExceptionUtils.toBadRequestException(e, null);
            }
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
            String enc = HttpUtils.getSetEncoding(m, headers, StandardCharsets.UTF_8.name());
            XMLStreamWriter xmlStreamWriter = createStreamWriter(aegisType.getSchemaType(), enc, os);
            // use type qname as element qname?
            xmlStreamWriter.writeStartDocument();
            aegisWriter.write(obj, aegisType.getSchemaType(), false, xmlStreamWriter, aegisType);
            xmlStreamWriter.writeEndDocument();
            xmlStreamWriter.close();
        } catch (Exception e) {
            throw ExceptionUtils.toInternalServerErrorException(e, null);
        }
    }

    protected XMLStreamWriter createStreamWriter(QName typeQName,
                                                 String enc,
                                                 OutputStream os) throws Exception {
        return StaxUtils.createXMLStreamWriter(os, enc);
    }
}
