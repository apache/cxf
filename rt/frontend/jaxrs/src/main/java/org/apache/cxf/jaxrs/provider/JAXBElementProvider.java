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
import javax.xml.bind.ValidationEventHandler;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxb.NamespaceMapper;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.schemas.SchemaHandler;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
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
    
    private Map<String, Object> mProperties = Collections.emptyMap();
    private boolean enableStreaming;
    private ValidationEventHandler eventHandler;
    
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {
        
        if (InjectionUtils.isSupportedCollectionOrArray(type)) {
            type = InjectionUtils.getActualType(genericType);
            if (type == null) {
                return false;
            }
        }
        
        return super.isReadable(type, genericType, anns, mt);
    }
    
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
            
            checkContentLength();
            
            boolean isCollection = InjectionUtils.isSupportedCollectionOrArray(type);
            Class<?> theType = isCollection ? InjectionUtils.getActualType(genericType) : type;
            theType = getActualType(theType, genericType, anns);

            Unmarshaller unmarshaller = createUnmarshaller(theType, genericType, isCollection);
            if (eventHandler != null) {
                unmarshaller.setEventHandler(eventHandler);
            }
            addAttachmentUnmarshaller(unmarshaller);
            Object response = null;
            if (JAXBElement.class.isAssignableFrom(type) 
                || unmarshalAsJaxbElement 
                || jaxbElementClassMap != null && jaxbElementClassMap.containsKey(theType.getName())) {
                response = unmarshaller.unmarshal(new StreamSource(is), theType);
            } else {
                response = doUnmarshal(unmarshaller, type, is, mt);
            }
            if (response instanceof JAXBElement && !JAXBElement.class.isAssignableFrom(type)) {
                response = ((JAXBElement)response).getValue();    
            }
            if (isCollection) {
                response = ((CollectionWrapper)response).getCollectionOrArray(theType, type); 
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
                        new RuntimeException("Cant' create XMLStreamReader", e));
                }
            }
        }
        
        reader = createTransformReaderIfNeeded(reader, is);
        if (InjectionUtils.isSupportedCollectionOrArray(type)) {
            return new JAXBCollectionWrapperReader(createNewReaderIfNeeded(reader, is));
        } else {
            return reader;
        }
        
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
            Class<?> actualClass = obj != actualObject ? actualObject.getClass() : cls;
            String encoding = getEncoding(m, headers);
            if (InjectionUtils.isSupportedCollectionOrArray(actualClass)) {
                actualClass = InjectionUtils.getActualType(genericType);
                marshalCollection(cls, actualObject, actualClass, genericType, encoding, os, m);
            } else {
                marshal(actualObject, actualClass, genericType, encoding, os, m);
            }
        } catch (JAXBException e) {
            handleJAXBException(e);
        }  catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException(e);        
        }
    }

    protected void marshalCollection(Class<?> originalCls, Object actualObject, Class<?> actualClass,
                                     Type genericType, String encoding, OutputStream os, MediaType m) 
        throws Exception {
        
        Object[] arr = originalCls.isArray() ? (Object[])actualObject : ((Collection)actualObject).toArray();
        
        QName qname = null;
        if (arr.length > 0 && arr[0] instanceof JAXBElement) {
            JAXBElement el = (JAXBElement)arr[0];
            qname = el.getName();
            actualClass = el.getDeclaredType();
        } else {
            qname = getCollectionWrapperQName(actualClass, genericType, actualObject, true);
        }
        if (qname == null) {
            String message = new org.apache.cxf.common.i18n.Message("NO_COLLECTION_ROOT", 
                                                                    BUNDLE).toString();
            throw new WebApplicationException(Response.serverError()
                                              .entity(message).build());
        }
        
        String startTag = null;
        String endTag = null;
        if (qname.getNamespaceURI().length() > 0) {
            startTag = "<ns1:" + qname.getLocalPart() + " xmlns:ns1=\"" + qname.getNamespaceURI()
                       + "\">";
            endTag = "</ns1:" + qname.getLocalPart() + ">"; 
        } else {
            startTag = "<" + qname.getLocalPart() + ">";
            endTag = "</" + qname.getLocalPart() + ">";
        }
        os.write(startTag.getBytes());
        for (Object o : arr) {
            marshalCollectionMember(o instanceof JAXBElement ? ((JAXBElement)o).getValue() : o, 
                                    actualClass, genericType, encoding, os, m, qname.getNamespaceURI());    
        }
        os.write(endTag.getBytes());
    }
    
    protected void marshalCollectionMember(Object obj, Class<?> cls, Type genericType, 
                           String enc, OutputStream os, MediaType mt, String ns) throws Exception {
        obj = convertToJaxbElementIfNeeded(obj, cls, genericType);
        if (obj instanceof JAXBElement && cls != JAXBElement.class) {
            cls = JAXBElement.class;
        }
        Marshaller ms = createMarshaller(obj, cls, genericType, enc);
        ms.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        if (ns.length() > 0) {
            Map<String, String> map = Collections.singletonMap(ns, "ns1");
            NamespaceMapper nsMapper = new NamespaceMapper(map);
            try {
                ms.setProperty("com.sun.xml.bind.namespacePrefixMapper", nsMapper);
            } catch (PropertyException ex) {
                ms.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", nsMapper);
            }
        }
        marshal(obj, cls, genericType, enc, os, mt, ms);
    }
    
    protected void marshal(Object obj, Class<?> cls, Type genericType, 
                           String enc, OutputStream os, MediaType mt) throws Exception {
        obj = convertToJaxbElementIfNeeded(obj, cls, genericType);
        if (obj instanceof JAXBElement && cls != JAXBElement.class) {
            cls = JAXBElement.class;
        }
        
        Marshaller ms = createMarshaller(obj, cls, genericType, enc);
        addAttachmentMarshaller(ms);
        marshal(obj, cls, genericType, enc, os, mt, ms);
    }
    
    protected void addAttachmentMarshaller(Marshaller ms) {
        Collection<Attachment> attachments = getAttachments();
        if (attachments != null) {
            Object value = getContext().getContextualProperty(Message.MTOM_THRESHOLD);
            Integer threshold = value != null ? Integer.valueOf(value.toString()) : 0;
            ms.setAttachmentMarshaller(new JAXBAttachmentMarshaller(
                attachments, threshold));
        }
    }
    
    protected void addAttachmentUnmarshaller(Unmarshaller um) {
        Collection<Attachment> attachments = getAttachments();
        if (attachments != null) {
            um.setAttachmentUnmarshaller(new JAXBAttachmentUnmarshaller(
                attachments));
        }
    }
    
    private Collection<Attachment> getAttachments() {
        MessageContext mc = getContext();
        if (mc != null) {
            return CastUtils.cast((Collection<?>)mc.get(Message.ATTACHMENTS));
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
            if (writer == null && enableStreaming) {
                writer = StaxUtils.createXMLStreamWriter(os);
            }
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
    
    
    protected static class JAXBCollectionWrapperReader extends DepthXMLStreamReader {
        
        private boolean firstName;
        private boolean firstNs;
        
        public JAXBCollectionWrapperReader(XMLStreamReader reader) {
            super(reader);
        }
        
        @Override
        public String getNamespaceURI() {
            if (!firstNs) {
                firstNs = true;
                return "";
            }
            return super.getNamespaceURI();
        }
        
        @Override
        public String getLocalName() {
            if (!firstName) {
                firstName = true;
                return "collectionWrapper";
            }
            
            return super.getLocalName();
        }
        
    }
    
    
}
