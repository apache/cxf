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
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.schemas.SchemaHandler;
import org.codehaus.jettison.AbstractXMLStreamWriter;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLInputFactory;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;

@Produces("application/json")
@Consumes("application/json")
@Provider
public class JSONProvider extends AbstractJAXBProvider  {
    
    private static final String JAXB_DEFAULT_NAMESPACE = "##default";
    private static final String JAXB_DEFAULT_NAME = "##default";
    
    private Map<String, String> namespaceMap = new HashMap<String, String>();
    private boolean serializeAsArray;
    private List<String> arrayKeys;
    
    @Context
    public void setMessageContext(MessageContext mc) {
        super.setContext(mc);
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
    
    public void setSerializeAsArray(boolean asArray) {
        this.serializeAsArray = asArray;
    }
    
    public void setArrayKeys(List<String> keys) {
        this.arrayKeys = keys;
    }
    
    public void setNamespaceMap(Map<String, String> namespaceMap) {
        this.namespaceMap = namespaceMap;
    }

    public Object readFrom(Class<Object> type, Type genericType, Annotation[] anns, MediaType mt, 
        MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException {
        
        try {
            Class<?> theType = getActualType(type, genericType, anns);
            
            Unmarshaller unmarshaller = createUnmarshaller(theType, genericType);
            
            MappedXMLInputFactory factory = new MappedXMLInputFactory(namespaceMap);
            XMLStreamReader xsw = factory.createXMLStreamReader(is);
            Object response = null;
            if (JAXBElement.class.isAssignableFrom(type)) {
                response = unmarshaller.unmarshal(xsw, theType);
            } else {
                response = unmarshaller.unmarshal(xsw);
            }
            response = checkAdapter(response, anns, false);
            return response;
            
        } catch (JAXBException e) {
            handleJAXBException(e);
        } catch (XMLStreamException e) {
            throw new WebApplicationException(e);
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
            String encoding = getEncoding(m, headers);
            if (encoding == null) {
                encoding = "UTF-8";
            }
            Marshaller ms = createMarshaller(actualObject, actualClass, genericType, encoding);

            Configuration c = new Configuration(namespaceMap);
            MappedNamespaceConvention convention = new MappedNamespaceConvention(c);
            AbstractXMLStreamWriter xsw = new MappedXMLStreamWriter(
                                               convention, 
                                               new OutputStreamWriter(os, encoding));
            if (serializeAsArray) {
                if (arrayKeys != null) {
                    for (String key : arrayKeys) {
                        xsw.seriliazeAsArray(key);
                    }
                } else {
                    String key = getKey(convention, cls);
                    xsw.seriliazeAsArray(key);
                }
            }
                        
            ms.marshal(actualObject, xsw);
            xsw.close();
            
        } catch (JAXBException e) {
            handleJAXBException(e);
        } catch (XMLStreamException e) {
            throw new WebApplicationException(e);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    private String getKey(MappedNamespaceConvention convention, Class<?> cls) {
        String key = null;
        
        XmlRootElement root = cls.getAnnotation(XmlRootElement.class);
        if (root != null) {
            
            String namespace = root.namespace();
            if (JAXB_DEFAULT_NAMESPACE.equals(namespace)) {
                namespace = "";
            }
            
            String prefix = namespaceMap.get(namespace);
            if (prefix == null) {
                prefix = "";
            }
            
            String name = root.name();
            if (JAXB_DEFAULT_NAME.equals(name)) {
                name = cls.getSimpleName();
            }
            key = convention.createKey(prefix, namespace, name);
            
        } else {
            key = convention.createKey("", "", cls.getSimpleName());
        }
        return key;
    }
    
    
}
