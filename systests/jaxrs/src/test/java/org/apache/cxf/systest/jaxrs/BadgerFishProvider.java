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


import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.WeakHashMap;

import javax.ws.rs.ConsumeMime;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.codehaus.jettison.badgerfish.BadgerFishXMLInputFactory;
import org.codehaus.jettison.badgerfish.BadgerFishXMLOutputFactory;

@ProduceMime("application/json")
@ConsumeMime("application/json")
@Provider
public final class BadgerFishProvider 
    implements MessageBodyReader<Object>, MessageBodyWriter<Object>  {

    
    private static Map<Class, JAXBContext> jaxbContexts = new WeakHashMap<Class, JAXBContext>();
    @Context
    private HttpHeaders requestHeaders;  
    
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations) {
        return type.getAnnotation(XmlRootElement.class) != null;
    }
    
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations) {
        return type.getAnnotation(XmlRootElement.class) != null;
    }

    public long getSize(Object o) {
        return -1;
    }
    
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, 
                           MediaType m, MultivaluedMap<String, String> headers, InputStream is) {
        try {
            JAXBContext context = getJAXBContext(type);
            Unmarshaller unmarshaller = context.createUnmarshaller();
               
            BadgerFishXMLInputFactory factory = new BadgerFishXMLInputFactory();
            XMLStreamReader xsw = factory.createXMLStreamReader(is);            
            Object obj = unmarshaller.unmarshal(xsw);
            xsw.close();
            return obj;
        } catch (JAXBException e) {
            e.printStackTrace();         
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void writeTo(Object obj, Class<?> clazz, Type genericType, Annotation[] annotations,  
        MediaType m, MultivaluedMap<String, Object> headers, OutputStream os) {
        try {
            if (!"badger-fish-language".equals(requestHeaders.getLanguage())) {
                throw new RuntimeException();
            }
            
            JAXBContext context = getJAXBContext(obj.getClass());
            Marshaller marshaller = context.createMarshaller();
                        
            XMLOutputFactory factory = new BadgerFishXMLOutputFactory();
            XMLStreamWriter xsw = factory.createXMLStreamWriter(os);            
            marshaller.marshal(obj, xsw);
            xsw.close();
            
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }

    private JAXBContext getJAXBContext(Class type) throws JAXBException {
        synchronized (jaxbContexts) {
            JAXBContext context = jaxbContexts.get(type);
            if (context == null) {
                context = JAXBContext.newInstance(type);
                jaxbContexts.put(type, context);
            }
            return context;
        }
    }
}
