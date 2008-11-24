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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

@Produces({"application/xml", "text/xml" })
@Consumes({"application/xml", "text/xml" })
@Provider
public class JAXBElementProvider extends AbstractJAXBProvider  {
    
    private Map<String, Object> mProperties = new HashMap<String, Object>();
    
    public void setSchemas(List<String> locations) {
        super.setSchemas(locations);
    }
    
    public void setMarshallerProperties(Map<String, Object> marshallProperties) {
        mProperties = marshallProperties;
    }
    
    public void setSchemaLocation(String schemaLocation) {
        mProperties.put(Marshaller.JAXB_SCHEMA_LOCATION, schemaLocation);
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
            Throwable t = e.getLinkedException() != null 
                ? e.getLinkedException() : e.getCause() != null ? e.getCause() : e;
            String message = new org.apache.cxf.common.i18n.Message("JAXB_EXCEPTION", 
                                 BUNDLE, t.getMessage()).toString();
            Response r = Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.TEXT_PLAIN).entity(message).build();
            throw new WebApplicationException(t, r);
        } catch (Exception e) {
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
            for (Map.Entry<String, Object> entry : mProperties.entrySet()) {
                ms.setProperty(entry.getKey(), entry.getValue());
            }
            ms.marshal(actualObject, os);
            
        } catch (JAXBException e) {
            throw new WebApplicationException(e);
        }
    }

    
    
}
