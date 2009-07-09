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


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.SequenceInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Enumeration;
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
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.common.util.ReflectionInvokationHandler;
import org.apache.cxf.jaxb.JAXBBeanInfo;
import org.apache.cxf.jaxb.JAXBContextProxy;
import org.apache.cxf.jaxb.JAXBUtils;
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
    private boolean unwrapped;
    private String wrapperName;
    private Map<String, String> wrapperMap;
    
    @Context
    public void setMessageContext(MessageContext mc) {
        super.setContext(mc);
    }
    
    public void setSupportUnwrapped(boolean unwrap) {
        this.unwrapped = unwrap;
    }
    
    public void setWrapperName(String wName) {
        wrapperName = wName;
    }
    
    public void setWrapperMap(Map<String, String> map) {
        wrapperMap = map;
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
            
            InputStream realStream = getInputStream(type, genericType, is);
            MappedXMLInputFactory factory = new MappedXMLInputFactory(namespaceMap);
            XMLStreamReader xsw = factory.createXMLStreamReader(realStream);
            
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

    protected InputStream getInputStream(Class<Object> cls, Type type, InputStream is) throws Exception {
        if (unwrapped) {
            String rootName = getRootName(cls, type);
            InputStream isBefore = new ByteArrayInputStream(rootName.getBytes());
            InputStream isAfter = new ByteArrayInputStream("}".getBytes());
            final InputStream[] streams = new InputStream[]{isBefore, is, isAfter};
            
            Enumeration<InputStream> list = new Enumeration<InputStream>() {
                private int index; 
                public boolean hasMoreElements() {
                    return index < streams.length;
                }

                public InputStream nextElement() {
                    return streams[index++];
                }  
                
            };
            return new SequenceInputStream(list);
        } else {
            return is;
        }
                 
    }
    
    protected String getRootName(Class<Object> cls, Type type) throws Exception {
        String name = null;
        if (wrapperName != null) {
            name = wrapperName;
        } else if (wrapperMap != null) {
            name = wrapperMap.get(cls.getName());
        }
        if (name == null) {
            QName qname = getQName(cls, type, null, false);
            if (qname != null) {
                name = qname.getLocalPart();
                String prefix = qname.getPrefix();
                if (prefix.length() > 0) {
                    name = prefix + "." + name;
                }
            }
        }
        
        if (name == null) {
            throw new WebApplicationException(500);
        }
        
        return "{\"" + name + "\":";
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

            QName qname = getQName(actualClass, genericType, actualObject, true);
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
                    String key = getKey(convention, qname);
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

    private String getKey(MappedNamespaceConvention convention, QName qname) throws Exception {
        return convention.createKey(qname.getPrefix(), 
                                    qname.getNamespaceURI(),
                                    qname.getLocalPart());
            
        
    }
    
    private QName getQName(Class<?> cls, Type type, Object object, boolean allocatePrefix) 
        throws Exception {
        //try the easy way first
        XmlRootElement root = cls.getAnnotation(XmlRootElement.class);
        QName qname = null;
        if (root != null) {
            String namespace = getNamespace(root.namespace());
            String name = getLocalName(root.name(), cls.getSimpleName());
            String prefix = getPrefix(namespace, allocatePrefix);
            qname = new QName(namespace, name, prefix);
        } else {
            JAXBContext context = getJAXBContext(cls, type);
            JAXBContextProxy proxy = ReflectionInvokationHandler.createProxyWrapper(context,
                                                                                    JAXBContextProxy.class);
            JAXBBeanInfo info = JAXBUtils.getBeanInfo(proxy, cls);
            if (info != null) {
                try {
                    Object instance = object == null ? cls.newInstance() : object;
                    String name = getLocalName(info.getElementLocalName(instance), cls.getSimpleName());
                    String namespace = getNamespace(info.getElementNamespaceURI(instance));
                    String prefix = getPrefix(namespace, allocatePrefix);
                    qname = new QName(namespace, name, prefix);
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
        return qname;
    }
    
    private String getLocalName(String name, String clsName) {
        if (JAXB_DEFAULT_NAME.equals(name)) {
            name = clsName;
            if (name.length() > 1) {
                name = name.substring(0, 1).toLowerCase() + name.substring(1); 
            } else {
                name = name.toLowerCase();
            }
        }
        return name;
    }
    
    private String getNamespace(String namespace) {
        if (JAXB_DEFAULT_NAMESPACE.equals(namespace)) {
            return "";
        }
        return namespace;
    }
    
    private String getPrefix(String namespace, boolean allocatePrefix) {
        String prefix = namespaceMap.get(namespace);
        if (prefix == null) {
            if (allocatePrefix && namespace.length() > 0) {
                prefix = "ns" + (namespaceMap.size() + 1);
                namespaceMap.put(namespace, prefix);
            } else {
                prefix = "";
            }
        }
        return prefix;
    }
}
