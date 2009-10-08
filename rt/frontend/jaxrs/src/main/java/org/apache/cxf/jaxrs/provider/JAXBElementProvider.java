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

import javax.ws.rs.ConsumeMime;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.schemas.SchemaHandler;
import org.apache.cxf.staxutils.StaxUtils;

@ProduceMime({"application/xml", "application/*+xml", "text/xml" })
@ConsumeMime({"application/xml", "application/*+xml", "text/xml" })
@Provider
public class JAXBElementProvider extends AbstractJAXBProvider  {
    
    private Map<String, Object> mProperties = new HashMap<String, Object>();
    private boolean enableStreaming;
    
    @Context
    public void setMessageContext(MessageContext mc) {
        super.setContext(mc);
    }
    
    public void setEnableStreaming(boolean enableStream) {
        enableStreaming = enableStream; 
    }
    
    public boolean getEnableStreaming() {
        return enableStreaming;
    }
    
    public void setEnableBuffering(boolean enableBuf) {
        super.setEnableBuffering(enableBuf);
    }
    
    public void setConsumeMediaTypes(List<String> types) {
        super.setConsumeMediaTypes(types);
    }
    
    public void setProduceMediaTypes(List<String> types) {
        super.setProduceMediaTypes(types);
    }
    
    public void setSchemas(List<String> locations) {
        super.setSchemaLocations(locations);
    }
    
    public void setSchemaHandler(SchemaHandler handler) {
        super.setSchema(handler.getSchema());
    }
    
    public void setMarshallerProperties(Map<String, Object> marshallProperties) {
        mProperties = marshallProperties;
    }
    
    public void setSchemaLocation(String schemaLocation) {
        mProperties.put(Marshaller.JAXB_SCHEMA_LOCATION, schemaLocation);
    }
    
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] anns, MediaType mt, 
        MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException {
        try {
            Class<?> theType = getActualType(type, genericType, anns);
            Unmarshaller unmarshaller = createUnmarshaller(theType, genericType);
            
            Object response = null;
            if (JAXBElement.class.isAssignableFrom(type)) {
                response = unmarshaller.unmarshal(new StreamSource(is), theType);
            } else {
                response = unmarshaller.unmarshal(is);
            }
            response = checkAdapter(response, anns, false);
            return response;
            
        } catch (JAXBException e) {
            handleJAXBException(e);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException(e);        
        }
        // unreachable
        return null;
    }

    
    public void writeTo(Object obj, Class<?> cls, Type genericType, Annotation[] anns,  
        MediaType m, MultivaluedMap<String, Object> headers, OutputStream os) 
        throws IOException {
        try {
            Object actualObject = checkAdapter(obj, anns, true);
            Class<?> actualClass = actualObject.getClass();
            if (cls == genericType) {
                genericType = actualClass;
            }
            Marshaller ms = createMarshaller(actualObject, actualClass, genericType, m);
            for (Map.Entry<String, Object> entry : mProperties.entrySet()) {
                ms.setProperty(entry.getKey(), entry.getValue());
            }
            if (enableStreaming) {
                XMLStreamWriter writer = 
                    (XMLStreamWriter)getContext().get(XMLStreamWriter.class.getName());
                if (writer == null) {
                    writer = StaxUtils.createXMLStreamWriter(os);
                }
                ms.marshal(actualObject, writer);
            } else {
                ms.marshal(actualObject, os);
            }
            
        } catch (JAXBException e) {
            handleJAXBException(e);
        }  catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException(e);        
        }
    }

    
    
}
