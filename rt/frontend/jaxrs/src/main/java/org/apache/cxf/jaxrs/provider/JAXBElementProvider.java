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


import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.WeakHashMap;

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

@Provider
public final class JAXBElementProvider 
    implements MessageBodyReader<Object>, MessageBodyWriter<Object>  {

    static Map<Class, JAXBContext> jaxbContexts = new WeakHashMap<Class, JAXBContext>();

    public boolean isWriteable(Class<?> type) {
        return type.getAnnotation(XmlRootElement.class) != null;
    }
    
    public boolean isReadable(Class<?> type) {
        return type.getAnnotation(XmlRootElement.class) != null;
    }

    public long getSize(Object o) {
        return -1;
    }
    
    public Object readFrom(Class<Object> type, MediaType m, MultivaluedMap<String, String> headers,
                           InputStream is) {
        try {
            JAXBContext context = getJAXBContext(type);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return unmarshaller.unmarshal(is);
        } catch (JAXBException e) {
            e.printStackTrace();         
        }

        return null;
    }

    public void writeTo(Object obj, MediaType m, MultivaluedMap<String, Object> headers, OutputStream os) {
        try {
            JAXBContext context = getJAXBContext(obj.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.marshal(obj, os);
            
            //TODO: support Calendar type
            //}
        } catch (JAXBException e) {
            //TODO: better exception handling
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
