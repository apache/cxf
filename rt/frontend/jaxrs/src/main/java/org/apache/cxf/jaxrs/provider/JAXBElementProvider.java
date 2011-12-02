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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxb.NamespaceMapper;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.xml.XMLInstruction;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXBUtils;
import org.apache.cxf.jaxrs.utils.schemas.SchemaHandler;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.transform.TransformUtils;

@Produces({"application/xml", "application/*+xml", "text/xml" })
@Consumes({"application/xml", "application/*+xml", "text/xml" })
@Provider
public class JAXBElementProvider extends AbstractJAXBProvider  {
    private static final String XML_PI_START = "<?xml version=\"1.0\" encoding=\"";
    
    private static final List<String> MARSHALLER_PROPERTIES =
        Arrays.asList(new String[] {Marshaller.JAXB_ENCODING,
                                    Marshaller.JAXB_FORMATTED_OUTPUT,
                                    Marshaller.JAXB_FRAGMENT,
                                    Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION,
                                    Marshaller.JAXB_SCHEMA_LOCATION});
    
    private Map<String, Object> mProperties = Collections.emptyMap();
    private Map<String, String> nsPrefixes = Collections.emptyMap();
    
    public JAXBElementProvider() {
        
    }
    
    public void setNamespacePrefixes(Map<String, String> prefixes) {
        nsPrefixes = prefixes;
    }
    
    @Override
    protected boolean canBeReadAsJaxbElement(Class<?> type) {
        return super.canBeReadAsJaxbElement(type) 
            && type != XMLSource.class && !Source.class.isAssignableFrom(type);
    }
    
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
            
            checkContentLength();
            
            boolean isCollection = InjectionUtils.isSupportedCollectionOrArray(type);
            Class<?> theGenericType = isCollection ? InjectionUtils.getActualType(genericType) : type;
            Class<?> theType = getActualType(theGenericType, genericType, anns);

            Unmarshaller unmarshaller = createUnmarshaller(theType, genericType, isCollection);
            addAttachmentUnmarshaller(unmarshaller);
            Object response = null;
            if (JAXBElement.class.isAssignableFrom(type) 
                || !isCollection && (unmarshalAsJaxbElement  
                || jaxbElementClassMap != null && jaxbElementClassMap.containsKey(theType.getName()))) {
                XMLStreamReader reader = getStreamReader(is, type, mt);
                response = unmarshaller.unmarshal(
                     TransformUtils.createNewReaderIfNeeded(reader, is), 
                     theType);
            } else {
                response = doUnmarshal(unmarshaller, type, is, mt);
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
            handleJAXBException(e);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            LOG.warning(getStackTrace(e));
            throw new WebApplicationException(e);        
        }
        // unreachable
        return null;
    }

    protected Object doUnmarshal(Unmarshaller unmarshaller, Class<?> type, InputStream is, MediaType mt) 
        throws JAXBException {
        XMLStreamReader reader = getStreamReader(is, type, mt);
        if (reader != null) {
            return unmarshalFromReader(unmarshaller, reader, mt);
        }
        return unmarshalFromInputStream(unmarshaller, is, mt);
    }
    
    protected XMLStreamReader getStreamReader(InputStream is, Class<?> type, MediaType mt) {
        MessageContext mc = getContext();
        XMLStreamReader reader = mc != null ? mc.getContent(XMLStreamReader.class) : null;
        if (reader == null && mc != null) {
            XMLInputFactory factory = (XMLInputFactory)mc.get(XMLInputFactory.class.getName());
            if (factory != null) {
                try {
                    reader = factory.createXMLStreamReader(is);
                } catch (XMLStreamException e) {
                    throw new WebApplicationException(
                        new RuntimeException("Can not create XMLStreamReader", e));
                }
            }
        }
        
        if (reader == null && is == null) {
            reader = getStaxHandlerFromCurrentMessage(XMLStreamReader.class);
        }
        
        reader = createTransformReaderIfNeeded(reader, is);
        if (InjectionUtils.isSupportedCollectionOrArray(type)) {
            return new JAXBCollectionWrapperReader(TransformUtils.createNewReaderIfNeeded(reader, is));
        } else {
            return reader;
        }
        
    }
    
    protected Object unmarshalFromInputStream(Unmarshaller unmarshaller, InputStream is, MediaType mt) 
        throws JAXBException {
        // Try to create the read before unmarshalling the stream
        return unmarshaller.unmarshal(StaxUtils.createXMLStreamReader(is));
    }

    protected Object unmarshalFromReader(Unmarshaller unmarshaller, XMLStreamReader reader, MediaType mt) 
        throws JAXBException {
        return unmarshaller.unmarshal(reader);
    }
    
    public void writeTo(Object obj, Class<?> cls, Type genericType, Annotation[] anns,  
        MediaType m, MultivaluedMap<String, Object> headers, OutputStream os) 
        throws IOException {
        try {
            String encoding = HttpUtils.getSetEncoding(m, headers, null);
            if (InjectionUtils.isSupportedCollectionOrArray(cls)) {
                marshalCollection(cls, obj, genericType, encoding, os, m, anns);
            } else {
                Object actualObject = checkAdapter(obj, cls, anns, true);
                Class<?> actualClass = obj != actualObject || cls.isInterface() 
                    ? actualObject.getClass() : cls;
                marshal(actualObject, actualClass, genericType, encoding, os, m, anns);
            }
        } catch (JAXBException e) {
            handleJAXBException(e);
        }  catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(e);        
        }
    }

    protected void marshalCollection(Class<?> originalCls, Object collection, 
                                     Type genericType, String enc, OutputStream os, 
                                     MediaType m, Annotation[] anns) 
        throws Exception {
        
        Class<?> actualClass = InjectionUtils.getActualType(genericType);
        actualClass = getActualType(actualClass, genericType, anns);
        
        Collection c = originalCls.isArray() ? Arrays.asList((Object[]) collection) 
                                             : (Collection) collection;

        Iterator it = c.iterator();
        
        Object firstObj = it.hasNext() ? it.next() : null;

        QName qname = null;
        if (firstObj instanceof JAXBElement) {
            JAXBElement el = (JAXBElement)firstObj;
            qname = el.getName();
            actualClass = el.getDeclaredType();
        } else {
            qname = getCollectionWrapperQName(actualClass, genericType, firstObj, true);
        }
        if (qname == null) {
            String message = new org.apache.cxf.common.i18n.Message("NO_COLLECTION_ROOT", 
                                                                    BUNDLE).toString();
            throw new WebApplicationException(Response.serverError()
                                              .entity(message).build());
        }
        
        StringBuilder pi = new StringBuilder();
        pi.append(XML_PI_START + (enc == null ? "UTF-8" : enc) + "\"?>");
        os.write(pi.toString().getBytes());
        String startTag = null;
        String endTag = null;
        
        if (qname.getNamespaceURI().length() > 0) {
            String prefix = nsPrefixes.get(qname.getNamespaceURI());
            if (prefix == null) {
                prefix = "ns1";
            }
            startTag = "<" + prefix + ":" + qname.getLocalPart() + " xmlns:" + prefix + "=\"" 
                + qname.getNamespaceURI() + "\">";
            endTag = "</" + prefix + ":" + qname.getLocalPart() + ">"; 
        } else {
            startTag = "<" + qname.getLocalPart() + ">";
            endTag = "</" + qname.getLocalPart() + ">";
        }
        os.write(startTag.getBytes());
        if (firstObj != null) {
            XmlJavaTypeAdapter adapter = 
                org.apache.cxf.jaxrs.utils.JAXBUtils.getAdapter(firstObj.getClass(), anns);
            marshalCollectionMember(JAXBUtils.useAdapter(firstObj, adapter, true), 
                                    actualClass, genericType, enc, os, m, 
                                    qname.getNamespaceURI());
            while (it.hasNext()) {
                marshalCollectionMember(JAXBUtils.useAdapter(it.next(), adapter, true), actualClass, 
                                        genericType, enc, os, m, 
                                        qname.getNamespaceURI());
            }
        }
        os.write(endTag.getBytes());
    }
    
    protected void marshalCollectionMember(Object obj, 
                                           Class<?> cls, 
                                           Type genericType, 
                                           String enc, 
                                           OutputStream os, 
                                           MediaType mt, 
                                           String ns) throws Exception {
        
        if (obj instanceof JAXBElement) {
            obj = ((JAXBElement)obj).getValue();    
        } else {
            obj = convertToJaxbElementIfNeeded(obj, cls, genericType);
        }
        
        if (obj instanceof JAXBElement && cls != JAXBElement.class) {
            cls = JAXBElement.class;
        }
        
        Marshaller ms = createMarshaller(obj, cls, genericType, enc);
        ms.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        if (ns.length() > 0) {
            Map<String, String> map = new HashMap<String, String>();
            // set the default just in case
            if (!nsPrefixes.containsKey(ns)) {
                map.put(ns, "ns1");
            }
            map.putAll(nsPrefixes);
            setNamespaceMapper(ms, map);
        }
        marshal(obj, cls, genericType, enc, os, mt, ms);
    }
    
    protected static void setNamespaceMapper(Marshaller ms, Map<String, String> map) throws Exception {
        NamespaceMapper nsMapper = new NamespaceMapper(map);
        try {
            ms.setProperty("com.sun.xml.bind.namespacePrefixMapper", nsMapper);
        } catch (PropertyException ex) {
            ms.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", nsMapper);
        }
    }
    
    protected void marshal(Object obj, Class<?> cls, Type genericType, 
                           String enc, OutputStream os, MediaType mt) throws Exception {
        marshal(obj, cls, genericType, enc, os, mt, new Annotation[]{});
    }
    
    protected void marshal(Object obj, Class<?> cls, Type genericType, 
                           String enc, OutputStream os, MediaType mt,
                           Annotation[] anns) throws Exception {
        obj = convertToJaxbElementIfNeeded(obj, cls, genericType);
        if (obj instanceof JAXBElement && cls != JAXBElement.class) {
            cls = JAXBElement.class;
        }
        
        Marshaller ms = createMarshaller(obj, cls, genericType, enc);
        if (!nsPrefixes.isEmpty()) {
            setNamespaceMapper(ms, nsPrefixes);
        }
        addAttachmentMarshaller(ms);
        addProcessingInstructions(ms, anns);
        marshal(obj, cls, genericType, enc, os, mt, ms);
    }
    
    private void addProcessingInstructions(Marshaller ms, Annotation[] anns) throws Exception {
        XMLInstruction pi = AnnotationUtils.getAnnotation(anns, XMLInstruction.class);
        if (pi != null) {
            String value = pi.value();
            // Should we even consider adding a base URI here ?
            // Relative references may be resolved OK, to be verified
            try {
                ms.setProperty("com.sun.xml.bind.xmlHeaders", value);
            } catch (PropertyException ex) {
                ms.setProperty("com.sun.xml.internal.bind.xmlHeaders", value);
            }
        }
    }
    
    protected void addAttachmentMarshaller(Marshaller ms) {
        Collection<Attachment> attachments = getAttachments(true);
        if (attachments != null) {
            Object value = getContext().getContextualProperty(Message.MTOM_THRESHOLD);
            Integer threshold = value != null ? Integer.valueOf(value.toString()) : 0;
            ms.setAttachmentMarshaller(new JAXBAttachmentMarshaller(
                attachments, threshold));
        }
    }
    
    protected void addAttachmentUnmarshaller(Unmarshaller um) {
        Collection<Attachment> attachments = getAttachments(false);
        if (attachments != null) {
            um.setAttachmentUnmarshaller(new JAXBAttachmentUnmarshaller(
                attachments));
        }
    }
    
    private Collection<Attachment> getAttachments(boolean write) {
        MessageContext mc = getContext();
        if (mc != null) {
            // TODO: there has to be a better fix
            String propertyName = write ? "WRITE-" + Message.ATTACHMENTS : Message.ATTACHMENTS;
            return CastUtils.cast((Collection<?>)mc.get(propertyName));
        } else {
            return null;
        }
    }
    
    protected void marshal(Object obj, Class<?> cls, Type genericType, 
                           String enc, OutputStream os, MediaType mt, Marshaller ms)
        throws Exception {
        
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
            if (os == null) {
                ms.setProperty(Marshaller.JAXB_FRAGMENT, true);
            } else if (mc != null) {
                if (mc.getContent(XMLStreamWriter.class) != null) {
                    ms.setProperty(Marshaller.JAXB_FRAGMENT, true);
                }
                mc.put(XMLStreamWriter.class.getName(), writer);    
            }
            marshalToWriter(ms, obj, writer, mt);
            writer.writeEndDocument();
        } else {
            marshalToOutputStream(ms, obj, os, mt);
        }
    }
    
    protected XMLStreamWriter getStreamWriter(Object obj, OutputStream os, MediaType mt) {
        XMLStreamWriter writer = null;
        MessageContext mc = getContext();
        if (mc != null) {
            writer = mc.getContent(XMLStreamWriter.class);
            if (writer == null) {
                XMLOutputFactory factory = (XMLOutputFactory)mc.get(XMLOutputFactory.class.getName());
                if (factory != null) {
                    try {
                        writer = factory.createXMLStreamWriter(os);
                    } catch (XMLStreamException e) {
                        throw new WebApplicationException(
                            new RuntimeException("Cant' create XMLStreamWriter", e));
                    }
                }
            }
            if (writer == null && getEnableStreaming()) {
                writer = StaxUtils.createXMLStreamWriter(os);
            }
        } 
        
        if (writer == null && os == null) {
            writer = getStaxHandlerFromCurrentMessage(XMLStreamWriter.class);
        }
        return createTransformWriterIfNeeded(writer, os);
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
