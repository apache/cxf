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
import java.util.Collection;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.schemas.SchemaHandler;
import org.apache.cxf.staxutils.DelegatingXMLStreamWriter;
import org.codehaus.jettison.AbstractXMLStreamWriter;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLInputFactory;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;

@Produces("application/json")
@Consumes("application/json")
@Provider
public class JSONProvider extends AbstractJAXBProvider  {
    
    private Map<String, String> namespaceMap = new HashMap<String, String>();
    private boolean serializeAsArray;
    private List<String> arrayKeys;
    private boolean unwrapped;
    private String wrapperName;
    private Map<String, String> wrapperMap;
    private boolean dropRootElement;
    private boolean dropCollectionWrapperElement;
    private boolean ignoreMixedContent; 
    @Context
    public void setMessageContext(MessageContext mc) {
        super.setContext(mc);
    }
    
    public void setDropRootElement(boolean drop) {
        this.dropRootElement = drop;
    }
    
    public void setDropCollectionWrapperElement(boolean drop) {
        this.dropCollectionWrapperElement = drop;
    }
    
    public void setIgnoreMixedContent(boolean ignore) {
        this.ignoreMixedContent = ignore;
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
            XMLStreamReader xsw = createReader(type, realStream);
            
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

    protected XMLStreamReader createReader(Class<?> type, InputStream is) 
        throws Exception {
        MappedXMLInputFactory factory = new MappedXMLInputFactory(namespaceMap);
        return factory.createXMLStreamReader(is);
    }
    
    protected InputStream getInputStream(Class<Object> cls, Type type, InputStream is) throws Exception {
        if (unwrapped) {
            String rootName = getRootName(cls, type);
            InputStream isBefore = new ByteArrayInputStream(rootName.getBytes());
            String after = "}";
            InputStream isAfter = new ByteArrayInputStream(after.getBytes());
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
            Class<?> actualClass = obj != actualObject ? actualObject.getClass() : cls;
            if (cls == genericType) {
                genericType = actualClass;
            }
            String encoding = getEncoding(m, headers);
            if (encoding == null) {
                encoding = "UTF-8";
            }
            if (InjectionUtils.isSupportedCollectionOrArray(actualClass)) {
                actualClass = InjectionUtils.getActualType(genericType);
                marshalCollection(cls, actualObject, actualClass, genericType, encoding, os, m);
            } else {
                marshal(actualObject, actualClass, genericType, encoding, os);
            }
            
        } catch (JAXBException e) {
            handleJAXBException(e);
        } catch (XMLStreamException e) {
            throw new WebApplicationException(e);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    protected void marshalCollection(Class<?> originalCls, Object actualObject, Class<?> actualClass,
                                     Type genericType, String encoding, OutputStream os, MediaType m) 
        throws Exception {
        
        String startTag = null;
        String endTag = null;
        if (!dropCollectionWrapperElement) {
            QName qname = getCollectionWrapperQName(actualClass, genericType, actualObject, false);
            if (qname == null) {
                String message = new org.apache.cxf.common.i18n.Message("NO_COLLECTION_ROOT", 
                                                                        BUNDLE).toString();
                throw new WebApplicationException(Response.serverError()
                                                  .entity(message).build());
            }
            if (qname.getNamespaceURI().length() > 0) {
                startTag = "{\"ns1." + qname.getLocalPart() + "\":[";
            } else {
                startTag = "{\"" + qname.getLocalPart() + "\":[";
            }
            endTag = "]}";
        } else {
            startTag = "{";
            endTag = "}";
        }
        os.write(startTag.getBytes());
        Object[] arr = originalCls.isArray() ? (Object[])actualObject : ((Collection)actualObject).toArray();
        for (int i = 0; i < arr.length; i++) {
            Marshaller ms = createMarshaller(actualObject, actualClass, genericType, encoding);
            marshal(ms, arr[i], actualClass, genericType, encoding, os, true);
            if (i + 1 < arr.length) {
                os.write(",".getBytes());
            }
        }
        os.write(endTag.getBytes());
    }
    
    protected void marshal(Marshaller ms, Object actualObject, Class<?> actualClass, 
                  Type genericType, String enc, OutputStream os, boolean isCollection) throws Exception {
        
        XMLStreamWriter writer = createWriter(actualObject, actualClass, genericType, enc, 
                                              os, isCollection);
        if (ignoreMixedContent) {
            writer = new IgnoreMixedContentWriter(writer);
        }
        ms.marshal(actualObject, writer);
        writer.close();
    }
    
    protected XMLStreamWriter createWriter(Object actualObject, Class<?> actualClass, 
        Type genericType, String enc, OutputStream os, boolean isCollection) throws Exception {
        QName qname = getQName(actualClass, genericType, actualObject, true);
        Configuration c = new Configuration(namespaceMap);
        MappedNamespaceConvention convention = new MappedNamespaceConvention(c);
        AbstractXMLStreamWriter xsw = new MappedXMLStreamWriter(
                                            convention, 
                                            new OutputStreamWriter(os, enc));
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

        return isCollection || dropRootElement ? new JSONCollectionWriter(xsw, qname) : xsw; 
    }
    
    protected void marshal(Object actualObject, Class<?> actualClass, 
                           Type genericType, String enc, OutputStream os) throws Exception {
        
        actualObject = convertToJaxbElementIfNeeded(actualObject, actualClass, genericType);
        if (actualObject instanceof JAXBElement && actualClass != JAXBElement.class) {
            actualClass = JAXBElement.class;
        }
        
        Marshaller ms = createMarshaller(actualObject, actualClass, genericType, enc);
        marshal(ms, actualObject, actualClass, genericType, enc, os, false);
    }
    
    private String getKey(MappedNamespaceConvention convention, QName qname) throws Exception {
        return convention.createKey(qname.getPrefix(), 
                                    qname.getNamespaceURI(),
                                    qname.getLocalPart());
            
        
    }
    
    private QName getQName(Class<?> cls, Type type, Object object, boolean allocatePrefix) 
        throws Exception {
        QName qname = getJaxbQName(cls, type, object, false);
        if (qname != null) {
            String prefix = getPrefix(qname.getNamespaceURI(), allocatePrefix);
            return new QName(qname.getNamespaceURI(), qname.getLocalPart(), prefix);
        }
        return null;
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
    
    protected static class JSONCollectionWriter extends DelegatingXMLStreamWriter {
        private QName ignoredQName;
        public JSONCollectionWriter(XMLStreamWriter writer, QName qname) {
            super(writer);
            ignoredQName = qname;
        }
        
        @Override
        public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
            if (ignoredQName.getLocalPart().equals(local) 
                && ignoredQName.getNamespaceURI().equals(uri)) {
                return;
            }
            super.writeStartElement(prefix, local, uri);
        }
    }
    
    protected static class IgnoreMixedContentWriter extends DelegatingXMLStreamWriter {
        boolean lastWriteChars;
        
        public IgnoreMixedContentWriter(XMLStreamWriter writer) {
            super(writer);
        }

        public void writeCharacters(String text) throws XMLStreamException {
            if (!lastWriteChars || text.trim().length() > 0) {
                if (lastWriteChars) {
                    text = text.trim();
                }
                super.writeCharacters(text);
                lastWriteChars = true;
            }
        }

        public void writeStartElement(String prefix, String localName, String namespaceURI) 
            throws XMLStreamException {
            super.writeStartElement(prefix, localName, namespaceURI);
            lastWriteChars = false;
        }
    }
}
