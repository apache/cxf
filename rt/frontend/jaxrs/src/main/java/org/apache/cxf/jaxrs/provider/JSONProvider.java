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
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.codehaus.jettison.mapped.MappedXMLInputFactory;
import org.codehaus.jettison.mapped.MappedXMLOutputFactory;

@Produces("application/json")
@Consumes("application/json")
@Provider
public final class JSONProvider extends AbstractJAXBProvider  {
    
    private Map<String, String> namespaceMap = new HashMap<String, String>();
    
    public void setSchemas(List<String> locations) {
        super.setSchemas(locations);
    }
    
    public void setNamespaceMap(Map<String, String> namespaceMap) {
        this.namespaceMap = namespaceMap;
    }

    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType m, 
        MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException {
        
        try {
            Class<?> theType = getActualType(type, genericType);
            JAXBContext context = getJAXBContext(theType, genericType);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            
            MappedXMLInputFactory factory = new MappedXMLInputFactory(namespaceMap);
            XMLStreamReader xsw = factory.createXMLStreamReader(is);
            Object response = null;
            if (JAXBElement.class.isAssignableFrom(type)) {
                response = unmarshaller.unmarshal(xsw, theType);
            } else {
                response = unmarshaller.unmarshal(xsw);
            }
            return response;
            
        } catch (JAXBException e) {
            throw new WebApplicationException(e);         
        } catch (XMLStreamException e) {
            throw new WebApplicationException(e);
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

            XMLOutputFactory factory = new MappedXMLOutputFactory(namespaceMap);
            XMLStreamWriter xsw = factory.createXMLStreamWriter(os);            
            ms.marshal(actualObject, xsw);
            xsw.close();
            
        } catch (JAXBException e) {
            throw new WebApplicationException(e);
        } catch (XMLStreamException e) {
            throw new WebApplicationException(e);
        }
    }

    
    
    
}
