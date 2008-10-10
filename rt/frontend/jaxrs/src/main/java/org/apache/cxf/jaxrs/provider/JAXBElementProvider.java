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
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

@Provider
public final class JAXBElementProvider extends AbstractJAXBProvider  {
    
    private String jaxbSchemaLocation;
    
    public void setSchemas(List<String> locations) {
        super.setSchemas(locations);
    }
    
    public void setSchemaLocation(String schemaLocation) {
        jaxbSchemaLocation = schemaLocation;
    }
    
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType m, 
        MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException {
        try {
            Class<?> theType = getActualType(type, genericType);
            Unmarshaller unmarshaller = createUnmarshaller(theType, genericType);
            
            if (JAXBElement.class.isAssignableFrom(type)) {
                return unmarshaller.unmarshal(new StreamSource(is), theType);
            } else {
                return unmarshaller.unmarshal(is);
            }
            
        } catch (JAXBException e) {
            throw new WebApplicationException(e);        
        }
    }

    
    public void writeTo(Object obj, Class<?> cls, Type genericType, Annotation[] anns,  
        MediaType m, MultivaluedMap<String, Object> headers, OutputStream os) 
        throws IOException {
        try {
            Object actualObject = checkAdapter(obj, anns);
            Class<?> actualClass = actualObject.getClass();
            if (cls == genericType) {
                genericType = actualClass;
            }
            Marshaller ms = createMarshaller(actualObject, actualClass, genericType, m);
            if (jaxbSchemaLocation != null) {
                ms.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, jaxbSchemaLocation);
            }
            ms.marshal(actualObject, os);
            
        } catch (JAXBException e) {
            throw new WebApplicationException(e);
        }
    }

    
    
}
