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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.schemas.SchemaHandler;
import org.apache.cxf.staxutils.StaxUtils;

@Produces({"application/xml", "application/*+xml", "text/xml" })
@Consumes({"application/xml", "application/*+xml", "text/xml" })
@Provider
public class JAXBElementProvider extends AbstractJAXBProvider  {
    
    private static final List<String> MARSHALLER_PROPERTIES =
        Arrays.asList(new String[] {Marshaller.JAXB_ENCODING,
                                    Marshaller.JAXB_FORMATTED_OUTPUT,
                                    Marshaller.JAXB_FRAGMENT,
                                    Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION,
                                    Marshaller.JAXB_SCHEMA_LOCATION});
    
    private Map<String, Object> mProperties = new HashMap<String, Object>();
    private boolean enableStreaming;
    private ValidationEventHandler eventHandler;
    
    @Context
    public void setMessageContext(MessageContext mc) {
        super.setContext(mc);
    }
    
    public void setValidationHandler(ValidationEventHandler handler) {
        eventHandler = handler;
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
            if (eventHandler != null) {
                unmarshaller.setEventHandler(eventHandler);
            }
            Object response = null;
            if (JAXBElement.class.isAssignableFrom(type)) {
                response = unmarshaller.unmarshal(new StreamSource(is), theType);
            } else {
                response = doUnmarshal(unmarshaller, is, mt);
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

    protected Object doUnmarshal(Unmarshaller unmarshaller, InputStream is, MediaType mt) 
        throws JAXBException {
        XMLStreamReader reader = getStreamReader(is, mt);
        if (reader != null) {
            return unmarshalFromReader(unmarshaller, reader, mt);
        }
        return unmarshalFromInputStream(unmarshaller, is, mt);
    }
    
    protected XMLStreamReader getStreamReader(InputStream is, MediaType mt) {
        MessageContext mc = getContext();
        return mc != null ? mc.getContent(XMLStreamReader.class) : null;
    }
    
    protected Object unmarshalFromInputStream(Unmarshaller unmarshaller, InputStream is, MediaType mt) 
        throws JAXBException {
        return unmarshaller.unmarshal(is);
    }

    protected Object unmarshalFromReader(Unmarshaller unmarshaller, XMLStreamReader reader, MediaType mt) 
        throws JAXBException {
        return unmarshaller.unmarshal(reader);
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
            String encoding = getEncoding(m, headers);
            marshal(actualObject, actualClass, genericType, encoding, os, m);
        } catch (JAXBException e) {
            handleJAXBException(e);
        }  catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException(e);        
        }
    }

    protected void marshal(Object obj, Class<?> cls, Type genericType, 
                           String enc, OutputStream os, MediaType mt)
        throws Exception {
        
        Marshaller ms = createMarshaller(obj, cls, genericType, enc);
        for (Map.Entry<String, Object> entry : mProperties.entrySet()) {
            ms.setProperty(entry.getKey(), entry.getValue());
        }
        
        MessageContext mc = getContext();
        if (mc != null) {
            // check Marshaller properties which might've been set earlier on,
            // they'll overwrite statically configured ones
            for (String key : MARSHALLER_PROPERTIES) {
                Object value = mc.get(key);
                if (value != null) {
                    ms.setProperty(key, value);
                }
            }
            
        }
        XMLStreamWriter writer = getStreamWriter(obj, os, mt);
        if (writer != null) {
            if (mc != null) {
                mc.put(XMLStreamWriter.class.getName(), writer);
            }
            marshalToWriter(ms, obj, writer, mt);
        } else {
            marshalToOutputStream(ms, obj, os, mt);
        }
    }
    
    protected XMLStreamWriter getStreamWriter(Object obj, OutputStream os, MediaType mt) {
        XMLStreamWriter writer = null;
        MessageContext mc = getContext();
        if (mc != null) {
            writer = mc.getContent(XMLStreamWriter.class);
            if (writer == null && enableStreaming) {
                writer = StaxUtils.createXMLStreamWriter(os);
            }
        }
        return writer;
    }
    
    protected void marshalToOutputStream(Marshaller ms, Object obj, OutputStream os, MediaType mt) 
        throws Exception {
        ms.marshal(obj, os);
    }
    
    protected void marshalToWriter(Marshaller ms, Object obj, XMLStreamWriter writer, MediaType mt) 
        throws Exception {
        ms.marshal(obj, writer);
    }
}
