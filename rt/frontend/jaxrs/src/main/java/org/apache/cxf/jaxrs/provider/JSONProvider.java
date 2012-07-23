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
import java.io.StringReader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.Nullable;
import org.apache.cxf.jaxrs.provider.AbstractJAXBProvider;
import org.apache.cxf.jaxrs.provider.JSONUtils;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXBUtils;
import org.apache.cxf.jaxrs.utils.schemas.SchemaHandler;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.codehaus.jettison.JSONSequenceTooLargeException;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.SimpleConverter;
import org.codehaus.jettison.mapped.TypeConverter;
import org.codehaus.jettison.util.StringIndenter;

@Produces("application/json")
@Consumes("application/json")
@Provider
public class JSONProvider extends AbstractJAXBProvider  {
    
    private static final String MAPPED_CONVENTION = "mapped";
    private static final String BADGER_FISH_CONVENTION = "badgerfish";
    
    static {
        new SimpleConverter();
    }
    
    private ConcurrentHashMap<String, String> namespaceMap = 
        new ConcurrentHashMap<String, String>();
    private boolean serializeAsArray;
    private List<String> arrayKeys;
    private boolean unwrapped;
    private String wrapperName;
    private Map<String, String> wrapperMap;
    private boolean dropRootElement;
    private boolean dropCollectionWrapperElement;
    private boolean ignoreMixedContent; 
    private boolean writeXsiType = true;
    private boolean readXsiType = true;
    private boolean ignoreNamespaces;
    private String convention = MAPPED_CONVENTION;
    private TypeConverter typeConverter;
    private boolean attributesToElements;
    
    @Override
    public void setAttributesToElements(boolean value) {
        this.attributesToElements = value;
    }
    
    public void setConvention(String value) {
        if (!MAPPED_CONVENTION.equals(value) && !BADGER_FISH_CONVENTION.equals(value)) {
            throw new IllegalArgumentException("Unsupported convention \"" + value);
        }
        convention = value;
    }
    
    public void setConvertTypesToStrings(boolean convert) {
        if (convert) {
            this.setTypeConverter(new SimpleConverter());
        }
    }
    
    public void setTypeConverter(TypeConverter converter) {
        this.typeConverter = converter;
    }
    
    public void setIgnoreNamespaces(boolean ignoreNamespaces) {
        this.ignoreNamespaces = ignoreNamespaces;
    }
    
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
        this.namespaceMap.putAll(namespaceMap);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {
        return super.isReadable(type, genericType, anns, mt) || Document.class.isAssignableFrom(type);    
    }
    
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] anns, MediaType mt, 
        MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException {
        
        if (isPayloadEmpty()) {
            if (AnnotationUtils.getAnnotation(anns, Nullable.class) != null) {
                return null;
            } else {
                reportEmptyContentLength();
            }
        }
        
        try {
            InputStream realStream = getInputStream(type, genericType, is);
            if (Document.class.isAssignableFrom(type)) {
                W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
                copyReaderToWriter(createReader(type, realStream, false), writer);
                return writer.getDocument();
            }
            boolean isCollection = InjectionUtils.isSupportedCollectionOrArray(type);
            Class<?> theGenericType = isCollection ? InjectionUtils.getActualType(genericType) : type;
            Class<?> theType = getActualType(theGenericType, genericType, anns);
            
            Unmarshaller unmarshaller = createUnmarshaller(theType, genericType, isCollection);
            XMLStreamReader xsr = createReader(type, realStream, isCollection);
            
            Object response = null;
            if (JAXBElement.class.isAssignableFrom(type) 
                || unmarshalAsJaxbElement
                || jaxbElementClassMap != null && jaxbElementClassMap.containsKey(theType.getName())) {
                response = unmarshaller.unmarshal(xsr, theType);
            } else {
                response = unmarshaller.unmarshal(xsr);
            }
            if (response instanceof JAXBElement && !JAXBElement.class.isAssignableFrom(type)) {
                response = ((JAXBElement)response).getValue();    
            }
            if (isCollection) {
                response = ((CollectionWrapper)response).getCollectionOrArray(theType, type, 
                               org.apache.cxf.jaxrs.utils.JAXBUtils.getAdapter(theGenericType, anns)); 
            } else {
                response = checkAdapter(response, type, anns, false);
            }
            return response;
            
        } catch (JAXBException e) {
            handleJAXBException(e, true);
        } catch (XMLStreamException e) {
            if (e.getCause() instanceof JSONSequenceTooLargeException) {
                throw new WebApplicationException(413);
            } else {
                handleXMLStreamException(e, true);
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.status(400).build());
        }
        // unreachable
        return null;
    }

    protected XMLStreamReader createReader(Class<?> type, InputStream is, boolean isCollection) 
        throws Exception {
        XMLStreamReader reader = createReader(type, is);
        return isCollection ? new JAXBCollectionWrapperReader(reader) : reader;
    }
    
    protected XMLStreamReader createReader(Class<?> type, InputStream is) 
        throws Exception {
        XMLStreamReader reader = null;
        if (BADGER_FISH_CONVENTION.equals(convention)) {
            reader = JSONUtils.createBadgerFishReader(is);
        } else {
            reader = JSONUtils.createStreamReader(is, readXsiType, namespaceMap, getDepthProperties());
        }
        reader = createTransformReaderIfNeeded(reader, is);
        
        return reader;
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
            QName qname = getQName(cls, type, null);
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
    
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {
        
        return super.isWriteable(type, genericType, anns, mt)
            || Document.class.isAssignableFrom(type);
    }
    
    public void writeTo(Object obj, Class<?> cls, Type genericType, Annotation[] anns,  
        MediaType m, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException {
        if (os == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Jettison needs initialized OutputStream");
            if (getContext() != null && getContext().getContent(XMLStreamWriter.class) == null) {
                sb.append("; if you need to customize Jettison output with the custom XMLStreamWriter"
                          + " then extend JSONProvider or when possible configure it directly.");
            }
            throw new IOException(sb.toString());
        }
        try {
            
            String enc = HttpUtils.getSetEncoding(m, headers, "UTF-8");
            if (Document.class.isAssignableFrom(cls)) {
                XMLStreamWriter writer = createWriter(obj, cls, genericType, enc, os, false);
                copyReaderToWriter(StaxUtils.createXMLStreamReader((Document)obj), writer);
                return;
            }
            if (InjectionUtils.isSupportedCollectionOrArray(cls)) {
                marshalCollection(cls, obj, genericType, enc, os, m, anns);
            } else {
                Object actualObject = checkAdapter(obj, cls, anns, true);
                Class<?> actualClass = obj != actualObject || cls.isInterface()
                    ? actualObject.getClass() : cls;
                if (cls == genericType) {
                    genericType = actualClass;
                }
                
                marshal(actualObject, actualClass, genericType, enc, os);
            }
            
        } catch (JAXBException e) {
            handleJAXBException(e, false);
        } catch (XMLStreamException e) {
            handleXMLStreamException(e, false); 
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    protected void copyReaderToWriter(XMLStreamReader reader, XMLStreamWriter writer) 
        throws Exception {
        writer.writeStartDocument();
        StaxUtils.copy(reader, writer);
        writer.writeEndDocument();
    }
    
    protected void marshalCollection(Class<?> originalCls, Object collection, 
                                     Type genericType, String encoding, 
                                     OutputStream os, MediaType m, Annotation[] anns) 
        throws Exception {
        
        Class<?> actualClass = InjectionUtils.getActualType(genericType);
        actualClass = getActualType(actualClass, genericType, anns);
        
        Collection c = originalCls.isArray() ? Arrays.asList((Object[]) collection) 
                                             : (Collection) collection;

        Iterator it = c.iterator();
        
        Object firstObj = it.hasNext() ? it.next() : null;

        String startTag = null;
        String endTag = null;
        if (!dropCollectionWrapperElement) {
            QName qname = null;
            if (firstObj instanceof JAXBElement) {
                JAXBElement el = (JAXBElement)firstObj;
                qname = el.getName();
                actualClass = el.getDeclaredType();
            } else {
                qname = getCollectionWrapperQName(actualClass, genericType, firstObj, false);
            }
            String prefix = "";
            if (!ignoreNamespaces) {
                if (namespaceMap.containsKey(qname.getNamespaceURI())) {
                    prefix = namespaceMap.get(qname.getNamespaceURI());
                    if (prefix.length() > 0) {
                        prefix += ".";
                    }
                } else if (qname.getNamespaceURI().length() > 0) {
                    prefix = "ns1.";
                }
            }
            startTag = "{\"" + prefix + qname.getLocalPart() + "\":[";
            endTag = "]}";
        } else if (serializeAsArray) {
            startTag = "[";
            endTag = "]";
        } else {
            startTag = "{";
            endTag = "}";
        }
        
        os.write(startTag.getBytes());
        if (firstObj != null) {
            XmlJavaTypeAdapter adapter = 
                org.apache.cxf.jaxrs.utils.JAXBUtils.getAdapter(firstObj.getClass(), anns);
            marshalCollectionMember(JAXBUtils.useAdapter(firstObj, adapter, true),
                                    actualClass, genericType, encoding, os);
            while (it.hasNext()) {
                os.write(",".getBytes());
                marshalCollectionMember(JAXBUtils.useAdapter(it.next(), adapter, true), 
                                        actualClass, genericType, encoding, os);
            }
        }
        os.write(endTag.getBytes());
    }
    
    protected void marshalCollectionMember(Object obj, Class<?> cls, Type genericType,
                                           String enc, OutputStream os) throws Exception {
        if (obj instanceof JAXBElement) {
            obj = ((JAXBElement)obj).getValue();    
        } else {
            obj = convertToJaxbElementIfNeeded(obj, cls, genericType);
        }
        
        if (obj instanceof JAXBElement && cls != JAXBElement.class) {
            cls = JAXBElement.class;
        }
        Marshaller ms = createMarshaller(obj, cls, genericType, enc);
        marshal(ms, obj, cls, genericType, enc, os, true);
        
    }
    
    protected void marshal(Marshaller ms, Object actualObject, Class<?> actualClass, 
                  Type genericType, String enc, OutputStream os, boolean isCollection) throws Exception {
        OutputStream actualOs = os; 
        
        MessageContext mc = getContext();
        if (mc != null && MessageUtils.isTrue(mc.get(Marshaller.JAXB_FORMATTED_OUTPUT))) {
            actualOs = new CachedOutputStream();    
        }
        XMLStreamWriter writer = createWriter(actualObject, actualClass, genericType, enc, 
                                              actualOs, isCollection);
        ms.marshal(actualObject, writer);
        writer.close();
        if (os != actualOs) {
            StringIndenter formatter = new StringIndenter(
                IOUtils.newStringFromBytes(((CachedOutputStream)actualOs).getBytes()));
            Writer outWriter = new OutputStreamWriter(os, enc);
            IOUtils.copy(new StringReader(formatter.result()), outWriter, 2048);
            outWriter.close();
        }
    }
    
    protected XMLStreamWriter createWriter(Object actualObject, Class<?> actualClass, 
        Type genericType, String enc, OutputStream os, boolean isCollection) throws Exception {
        
        QName qname = getQName(actualClass, genericType, actualObject);
        if (ignoreNamespaces && (isCollection  || dropRootElement)) {        
            qname = new QName(qname.getLocalPart());
        }
        if (BADGER_FISH_CONVENTION.equals(convention)) {
            return JSONUtils.createBadgerFishWriter(os);
        }
        
        Configuration config = 
            JSONUtils.createConfiguration(namespaceMap, 
                                          writeXsiType && !ignoreNamespaces,
                                          attributesToElements,
                                          typeConverter);
        
        XMLStreamWriter writer = JSONUtils.createStreamWriter(os, qname, 
             writeXsiType && !ignoreNamespaces, config, serializeAsArray, arrayKeys,
             isCollection || dropRootElement);
        writer = JSONUtils.createIgnoreMixedContentWriterIfNeeded(writer, ignoreMixedContent);
        writer = JSONUtils.createIgnoreNsWriterIfNeeded(writer, ignoreNamespaces);
        return createTransformWriterIfNeeded(writer, os);
    }
    
    protected void marshal(Object actualObject, Class<?> actualClass, 
                           Type genericType, String enc, OutputStream os) throws Exception {
        
        actualObject = convertToJaxbElementIfNeeded(actualObject, actualClass, genericType);
        if (actualObject instanceof JAXBElement && actualClass != JAXBElement.class) {
            actualClass = JAXBElement.class;
        }
        
        Marshaller ms = createMarshaller(actualObject, actualClass, genericType, enc);
        if (!namespaceMap.isEmpty()) {
            setNamespaceMapper(ms, namespaceMap);
        }
        marshal(ms, actualObject, actualClass, genericType, enc, os, false);
    }
    
    private QName getQName(Class<?> cls, Type type, Object object) 
        throws Exception {
        QName qname = getJaxbQName(cls, type, object, false);
        if (qname != null) {
            String prefix = getPrefix(qname.getNamespaceURI());
            return new QName(qname.getNamespaceURI(), qname.getLocalPart(), prefix);
        }
        return null;
    }
    
    private String getPrefix(String namespace) {
        String prefix = namespaceMap.get(namespace);
        return prefix == null ? "" : prefix;
    }
    
    public void setWriteXsiType(boolean writeXsiType) {
        this.writeXsiType = writeXsiType;
    }
    
    public void setReadXsiType(boolean readXsiType) {
        this.readXsiType = readXsiType;
    }

}
