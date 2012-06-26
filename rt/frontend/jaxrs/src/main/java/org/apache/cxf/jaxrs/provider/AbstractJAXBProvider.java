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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.validation.Schema;

import org.xml.sax.helpers.DefaultHandler;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.jaxb.JAXBUtils;
import org.apache.cxf.jaxb.NamespaceMapper;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.jaxrs.utils.schemas.SchemaHandler;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.staxutils.DepthRestrictingStreamReader;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.DocumentDepthProperties;
import org.apache.cxf.staxutils.transform.TransformUtils;

public abstract class AbstractJAXBProvider extends AbstractConfigurableProvider
    implements MessageBodyReader<Object>, MessageBodyWriter<Object> {
    
    protected static final ResourceBundle BUNDLE = BundleUtils.getBundle(AbstractJAXBProvider.class);

    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractJAXBProvider.class);
    protected static final String NS_MAPPER_PROPERTY = "com.sun.xml.bind.namespacePrefixMapper";
    protected static final String NS_MAPPER_PROPERTY_INT = "com.sun.xml.internal.bind.namespacePrefixMapper";
    private static final String JAXB_DEFAULT_NAMESPACE = "##default";
    private static final String JAXB_DEFAULT_NAME = "##default";
    
    
    protected Set<Class<?>> collectionContextClasses = new HashSet<Class<?>>();
    
    protected Map<String, String> jaxbElementClassMap = Collections.emptyMap();
    protected boolean unmarshalAsJaxbElement;
    protected boolean marshalAsJaxbElement;
    
    protected Map<String, String> outElementsMap;
    protected Map<String, String> outAppendMap;
    protected List<String> outDropElements;
    protected List<String> inDropElements;
    protected Map<String, String> inElementsMap;
    protected Map<String, String> inAppendMap;
    private boolean attributesToElements;
    
    private Map<String, JAXBContext> packageContexts = new HashMap<String, JAXBContext>();
    private Map<Class<?>, JAXBContext> classContexts = new HashMap<Class<?>, JAXBContext>();
    
    private MessageContext mc;
    private Schema schema;
    private String catalogLocation;
    private String collectionWrapperName;
    private Map<String, String> collectionWrapperMap;
    private List<String> jaxbElementClassNames = Collections.emptyList();
    private Map<String, Object> cProperties;
    private Map<String, Object> uProperties;
    
    private boolean skipJaxbChecks;
    private boolean singleJaxbContext;
    private Class[] extraClass;
    
    private boolean validateOutput;
    private boolean validateBeforeWrite;
    private ValidationEventHandler eventHandler;
    private Unmarshaller.Listener unmarshallerListener;
    private Marshaller.Listener marshallerListener;
    private DocumentDepthProperties depthProperties;
    
    protected static void setNamespaceMapper(Marshaller ms, Map<String, String> map) throws Exception {
        NamespaceMapper nsMapper = new NamespaceMapper(map);
        setMarshallerProp(ms, nsMapper, NS_MAPPER_PROPERTY, NS_MAPPER_PROPERTY_INT);
    }
    
    protected static void setMarshallerProp(Marshaller ms, Object value, 
                                          String name1, String name2) throws Exception {
        try {
            ms.setProperty(name1, value);
        } catch (PropertyException ex) {
            ms.setProperty(name2, value);
        }
        
    }
    
    public void setValidationHandler(ValidationEventHandler handler) {
        eventHandler = handler;
    }
    
    public void setSingleJaxbContext(boolean useSingleContext) {
        singleJaxbContext = useSingleContext;
    }
    
    public void setExtraClass(Class[] userExtraClass) {
        extraClass = userExtraClass;
    }
    
    @Override
    public void init(List<ClassResourceInfo> cris) {
        if (singleJaxbContext) {
            Set<Class<?>> allTypes = 
                new HashSet<Class<?>>(ResourceUtils.getAllRequestResponseTypes(cris, true)
                    .getAllTypes().keySet());
            JAXBContext context = 
                ResourceUtils.createJaxbContext(allTypes, extraClass, cProperties);
            if (context != null) {
                for (Class<?> cls : allTypes) {
                    classContexts.put(cls, context);
                }
            }
        }
    }
    
    public void setContextProperties(Map<String, Object> contextProperties) {
        cProperties = contextProperties;
    }
    
    public void setUnmarshallerProperties(Map<String, Object> unmarshalProperties) {
        uProperties = unmarshalProperties;
    }
    
    public void setUnmarshallAsJaxbElement(boolean value) {
        unmarshalAsJaxbElement = value;
    }
    
    public void setMarshallAsJaxbElement(boolean value) {
        marshalAsJaxbElement = value;
    }
    
    public void setJaxbElementClassNames(List<String> names) {
        jaxbElementClassNames = names;
    }
    
    public void setJaxbElementClassMap(Map<String, String> map) {
        jaxbElementClassMap = map;
    }
    
    protected boolean isPayloadEmpty() {
        return mc != null ? isPayloadEmpty(mc.getHttpHeaders()) : false;     
    }
    
    protected void reportEmptyContentLength() {
        String message = new org.apache.cxf.common.i18n.Message("EMPTY_BODY", BUNDLE).toString();
        LOG.warning(message);
        throw new WebApplicationException(400);
    }

    protected <T> T getStaxHandlerFromCurrentMessage(Class<T> staxCls) {
        Message m = PhaseInterceptorChain.getCurrentMessage();
        if (m != null) {
            return staxCls.cast(m.getContent(staxCls));
        }
        return null;
    }
    
    protected static boolean isXmlRoot(Class<?> cls) {
        return cls.getAnnotation(XmlRootElement.class) != null;
    }
    
    @SuppressWarnings("unchecked")
    protected Object convertToJaxbElementIfNeeded(Object obj, Class<?> cls, Type genericType) 
        throws Exception {
        
        boolean asJaxbElement = jaxbElementClassNames.contains(cls.getName());
        if (!asJaxbElement && isXmlRoot(cls)) {
            return obj;
        }
        
        QName name = null;
        String expandedName = jaxbElementClassMap.get(cls.getName());
        if (expandedName != null) {
            name = JAXRSUtils.convertStringToQName(expandedName);
        } else if (marshalAsJaxbElement || asJaxbElement) {
            name = getJaxbQName(cls, genericType, obj, false);
        }
        return name != null ? new JAXBElement(name, cls, null, obj) : obj;
    }
    
    public void setCollectionWrapperName(String wName) {
        collectionWrapperName = wName;
    }
    
    public void setCollectionWrapperMap(Map<String, String> map) {
        collectionWrapperMap = map;
    }
    
    protected void setContext(MessageContext context) {
        mc = context;
    }
    
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {
        
        if (InjectionUtils.isSupportedCollectionOrArray(type)) {
            type = InjectionUtils.getActualType(genericType);
            if (type == null) {
                return false;
            }
        }
        
        return marshalAsJaxbElement || isSupported(type, genericType, anns);
    }
    
    protected JAXBContext getCollectionContext(Class<?> type) throws JAXBException {
        synchronized (collectionContextClasses) {
            if (!collectionContextClasses.contains(type)) {
                collectionContextClasses.add(CollectionWrapper.class);
                collectionContextClasses.add(type);
            }
            return JAXBContext.newInstance(collectionContextClasses.toArray(new Class[]{}), 
                                                        cProperties);
        }
    }
    
    protected QName getCollectionWrapperQName(Class<?> cls, Type type, Object object, boolean pluralName)
        throws Exception {
        String name = getCollectionWrapperName(cls);
        if (name == null) {
            return getJaxbQName(cls, type, object, pluralName);
        }
            
        return JAXRSUtils.convertStringToQName(name);
    }
    
    private String getCollectionWrapperName(Class<?> cls) {
        if (collectionWrapperName != null) { 
            return collectionWrapperName;
        }
        if (collectionWrapperMap != null) {
            return collectionWrapperMap.get(cls.getName());
        }
        
        return null;
    }
    
    protected QName getJaxbQName(Class<?> cls, Type type, Object object, boolean pluralName) 
        throws Exception {
        
        if (cls == JAXBElement.class) {
            return object != null ? ((JAXBElement)object).getName() : null;
        }
        
        XmlRootElement root = cls.getAnnotation(XmlRootElement.class);
        if (root != null) {
            return getQNameFromNamespaceAndName(root.namespace(), root.name(), cls, pluralName);
        } else if (cls.getAnnotation(XmlType.class) != null) {
            XmlType xmlType = cls.getAnnotation(XmlType.class);
            return getQNameFromNamespaceAndName(xmlType.namespace(), xmlType.name(), cls, pluralName);
        } else {
            return new QName(getPackageNamespace(cls), cls.getSimpleName());
        }
    }
    
    private QName getQNameFromNamespaceAndName(String ns, String localName, Class<?> cls, boolean plural) {
        String name = getLocalName(localName, cls.getSimpleName() , plural);
        String namespace = getNamespace(ns);
        if ("".equals(namespace)) {
            namespace = getPackageNamespace(cls);
        }
        return new QName(namespace, name);
    }
    
    private String getLocalName(String name, String clsName, boolean pluralName) {
        if (JAXB_DEFAULT_NAME.equals(name)) {
            name = clsName;
            if (name.length() > 1) {
                name = name.substring(0, 1).toLowerCase() + name.substring(1); 
            } else {
                name = name.toLowerCase();
            }
        }
        if (pluralName) {
            name += 's';
        }
        return name;
    }
    
    private String getPackageNamespace(Class<?> cls) {
        String packageNs = JAXBUtils.getPackageNamespace(cls);
        return packageNs != null ? getNamespace(packageNs) : "";
    }
    
    private String getNamespace(String namespace) {
        if (JAXB_DEFAULT_NAMESPACE.equals(namespace)) {
            return "";
        }
        return namespace;
    }
    
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {
        if (InjectionUtils.isSupportedCollectionOrArray(type)) {
            type = InjectionUtils.getActualType(genericType);
            if (type == null) {
                return false;
            }
        }    
        return canBeReadAsJaxbElement(type) || isSupported(type, genericType, anns);
    }

    protected boolean canBeReadAsJaxbElement(Class<?> type) {
        return unmarshalAsJaxbElement && type != Response.class;
    }
    
    public void setSchemaLocations(List<String> locations) {
        schema = SchemaHandler.createSchema(locations, catalogLocation, getBus());    
    }
    
    public void setCatalogLocation(String name) {
        this.catalogLocation = name;
    }
    
    public void setSchema(Schema s) {
        schema = s;    
    }
    
    public long getSize(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return -1;
    }

    protected MessageContext getContext() {
        return mc;
    }
    
    @SuppressWarnings("unchecked")
    protected JAXBContext getJAXBContext(Class<?> type, Type genericType) throws JAXBException {
        if (mc != null) {
            ContextResolver<JAXBContext> resolver = 
                mc.getResolver(ContextResolver.class, JAXBContext.class);
            if (resolver != null) {
                JAXBContext customContext = resolver.getContext(type);
                if (customContext != null) {
                    return customContext;
                }
            }
        }
        
        synchronized (classContexts) {
            JAXBContext context = classContexts.get(type);
            if (context != null) {
                return context;
            }
        }
        
        JAXBContext context = getPackageContext(type);
                
        return context != null ? context : getClassContext(type);
    }
    
    public JAXBContext getClassContext(Class<?> type) throws JAXBException {
        synchronized (classContexts) {
            JAXBContext context = classContexts.get(type);
            if (context == null) {
                Class[] classes = null;
                if (extraClass != null) {
                    classes = new Class[extraClass.length + 1];
                    classes[0] = type;
                    System.arraycopy(extraClass, 0, classes, 1, extraClass.length);
                } else {
                    classes = new Class[] {type};    
                }
                
                context = JAXBContext.newInstance(classes, cProperties);
                classContexts.put(type, context);
            }
            return context;
        }
    }
    
    public JAXBContext getPackageContext(Class<?> type) {
        if (type == null || type == JAXBElement.class) {
            return null;
        }
        synchronized (packageContexts) {
            String packageName = PackageUtils.getPackageName(type);
            JAXBContext context = packageContexts.get(packageName);
            if (context == null) {
                try {
                    if (type.getClassLoader() != null && objectFactoryOrIndexAvailable(type)) { 
                        context = JAXBContext.newInstance(packageName, type.getClassLoader(), cProperties);
                        packageContexts.put(packageName, context);
                    }
                } catch (JAXBException ex) {
                    LOG.fine("Error creating a JAXBContext using ObjectFactory : " 
                                + ex.getMessage());
                    return null;
                }
            }
            return context;
        }
    }
    
    protected boolean isSupported(Class<?> type, Type genericType, Annotation[] anns) {
        if (jaxbElementClassMap != null && jaxbElementClassMap.containsKey(type.getName())
            || isSkipJaxbChecks()) {
            return true;
        }
        return isXmlRoot(type)
            || JAXBElement.class.isAssignableFrom(type)
            || objectFactoryOrIndexAvailable(type)
            || (type != genericType && objectFactoryForType(genericType))
            || org.apache.cxf.jaxrs.utils.JAXBUtils.getAdapter(type, anns) != null;
    
    }
    
    protected boolean objectFactoryOrIndexAvailable(Class<?> type) {
        return type.getResource("ObjectFactory.class") != null
               || type.getResource("jaxb.index") != null; 
    }
    
    private boolean objectFactoryForType(Type genericType) {
        return objectFactoryOrIndexAvailable(InjectionUtils.getActualType(genericType));
    }
    
    protected Unmarshaller createUnmarshaller(Class<?> cls, Type genericType) 
        throws JAXBException {
        return createUnmarshaller(cls, genericType, false);        
    }
    
    protected Unmarshaller createUnmarshaller(Class<?> cls, Type genericType, boolean isCollection) 
        throws JAXBException {
        JAXBContext context = isCollection ? getCollectionContext(cls) 
                                           : getJAXBContext(cls, genericType);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        if (schema != null) {
            unmarshaller.setSchema(schema);
        }
        if (eventHandler != null) {
            unmarshaller.setEventHandler(eventHandler);
        }
        if (unmarshallerListener != null) {
            unmarshaller.setListener(unmarshallerListener);
        }
        if (uProperties != null) {
            for (Map.Entry<String, Object> entry : uProperties.entrySet()) {
                unmarshaller.setProperty(entry.getKey(), entry.getValue());
            }
        }
        return unmarshaller;        
    }
    
    protected Marshaller createMarshaller(Object obj, Class<?> cls, Type genericType, String enc)
        throws JAXBException {
        
        Class<?> objClazz = JAXBElement.class.isAssignableFrom(cls) 
                            ? ((JAXBElement)obj).getDeclaredType() : cls;
                            
        JAXBContext context = getJAXBContext(objClazz, genericType);
        Marshaller marshaller = context.createMarshaller();
        if (enc != null) {
            marshaller.setProperty(Marshaller.JAXB_ENCODING, enc);
        }
        if (marshallerListener != null) {
            marshaller.setListener(marshallerListener);
        }
        validateObjectIfNeeded(marshaller, obj);
        return marshaller;
    }
    
    protected void validateObjectIfNeeded(Marshaller marshaller, Object obj) 
        throws JAXBException {
        if (validateOutput && schema != null) {
            marshaller.setEventHandler(eventHandler);
            marshaller.setSchema(schema);
            if (validateBeforeWrite) {
                marshaller.marshal(obj, new DefaultHandler());
                marshaller.setSchema(null);
            }
        }
    }
        
    protected Class<?> getActualType(Class<?> type, Type genericType, Annotation[] anns) {
        Class<?> theType = null;
        if (JAXBElement.class.isAssignableFrom(type)) {
            theType = InjectionUtils.getActualType(genericType);
        } else {
            theType = type;
        }
        XmlJavaTypeAdapter adapter = org.apache.cxf.jaxrs.utils.JAXBUtils.getAdapter(theType, anns);
        theType = org.apache.cxf.jaxrs.utils.JAXBUtils.getTypeFromAdapter(adapter, theType, false);
        
        return theType;
    }
    
    protected static Object checkAdapter(Object obj, Class<?> cls, Annotation[] anns, boolean marshal) {
        XmlJavaTypeAdapter adapter = org.apache.cxf.jaxrs.utils.JAXBUtils.getAdapter(cls, anns);
        return org.apache.cxf.jaxrs.utils.JAXBUtils.useAdapter(obj, adapter, marshal); 
    }
    
    protected Schema getSchema() {
        return schema;
    }

    
    public void clearContexts() {
        classContexts.clear();
        packageContexts.clear();
    }
    
    protected static String getStackTrace(Exception ex) { 
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
    //TODO: move these methods into the dedicated utility class
    protected static StringBuilder handleExceptionStart(Exception e) {
        LOG.warning(getStackTrace(e));
        StringBuilder sb = new StringBuilder();
        if (e.getMessage() != null) {
            sb.append(e.getMessage()).append(". ");
        }
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            sb.append(e.getCause().getMessage()).append(". ");
        }
        return sb;
    }
    
    protected static void handleExceptionEnd(Throwable t, String message, boolean read) {
        Response.Status status = read 
            ? Response.Status.BAD_REQUEST : Response.Status.INTERNAL_SERVER_ERROR; 
        Response r = Response.status(status)
            .type(MediaType.TEXT_PLAIN).entity(message).build();
        throw new WebApplicationException(t, r);
    }
    
    protected static void handleJAXBException(JAXBException e, boolean read) {
        StringBuilder sb = handleExceptionStart(e);
        if (e.getLinkedException() != null && e.getLinkedException().getMessage() != null) {
            sb.append(e.getLinkedException().getMessage()).append(". ");
        }
        Throwable t = e.getLinkedException() != null 
            ? e.getLinkedException() : e.getCause() != null ? e.getCause() : e;
        String message = new org.apache.cxf.common.i18n.Message("JAXB_EXCEPTION", 
                             BUNDLE, sb.toString()).toString();
        handleExceptionEnd(t, message, read);
    }
    
    protected static void handleXMLStreamException(XMLStreamException e, boolean read) {
        StringBuilder sb = handleExceptionStart(e);
        handleExceptionEnd(e, sb.toString(), read);
    }
    
    public void setOutTransformElements(Map<String, String> outElements) {
        this.outElementsMap = outElements;
    }
    
    public void setInAppendElements(Map<String, String> inElements) {
        this.inAppendMap = inElements;
    }
    
    public void setInTransformElements(Map<String, String> inElements) {
        this.inElementsMap = inElements;
    }
    
    public void setOutAppendElements(Map<String, String> map) {
        this.outAppendMap = map;
    }

    public void setOutDropElements(List<String> dropElementsSet) {
        this.outDropElements = dropElementsSet;
    }

    public void setInDropElements(List<String> dropElementsSet) {
        this.inDropElements = dropElementsSet;
    }
    
    public void setAttributesToElements(boolean value) {
        this.attributesToElements = value;
    }

    public void setSkipJaxbChecks(boolean skipJaxbChecks) {
        this.skipJaxbChecks = skipJaxbChecks;
    }

    public boolean isSkipJaxbChecks() {
        return skipJaxbChecks;
    }

    protected XMLStreamWriter createTransformWriterIfNeeded(XMLStreamWriter writer,
                                                            OutputStream os) {
        return TransformUtils.createTransformWriterIfNeeded(writer, os, 
                                                      outElementsMap,
                                                      outDropElements,
                                                      outAppendMap,
                                                      attributesToElements,
                                                      null);
    }
    
    protected XMLStreamReader createTransformReaderIfNeeded(XMLStreamReader reader, InputStream is) {
        return TransformUtils.createTransformReaderIfNeeded(reader, is,
                                                            inDropElements,
                                                            inElementsMap,
                                                            inAppendMap,
                                                            true);
    }
    
    protected XMLStreamReader createDepthReaderIfNeeded(XMLStreamReader reader, InputStream is) {
        DocumentDepthProperties props = getDepthProperties();
        if (props != null && props.isEffective()) {
            reader = TransformUtils.createNewReaderIfNeeded(reader, is);
            return new DepthRestrictingStreamReader(reader, props);
        }
        return reader;
    }
    
    protected DocumentDepthProperties getDepthProperties() {
        if (depthProperties != null) {
            return depthProperties;
        }
        if (getContext() != null) {
            String totalElementCountStr = (String)getContext().getContextualProperty(
                DocumentDepthProperties.TOTAL_ELEMENT_COUNT);
            String innerElementCountStr = (String)getContext().getContextualProperty(
                DocumentDepthProperties.INNER_ELEMENT_COUNT);
            String elementLevelStr = (String)getContext().getContextualProperty(
                DocumentDepthProperties.INNER_ELEMENT_LEVEL);
            if (totalElementCountStr != null || innerElementCountStr != null || elementLevelStr != null) {
                try {
                    int totalElementCount = totalElementCountStr != null 
                        ? Integer.valueOf(totalElementCountStr) : -1;
                    int elementLevel = elementLevelStr != null ? Integer.valueOf(elementLevelStr) : -1;
                    int innerElementCount = innerElementCountStr != null 
                        ? Integer.valueOf(innerElementCountStr) : -1;
                    return new DocumentDepthProperties(totalElementCount, elementLevel, innerElementCount);
                } catch (Exception ex) {
                    throw new WebApplicationException(ex);
                }
            }
        }
        return null;
    }
    
    public void setValidateBeforeWrite(boolean validateBeforeWrite) {
        this.validateBeforeWrite = validateBeforeWrite;
    }

    public void setValidateOutput(boolean validateOutput) {
        this.validateOutput = validateOutput;
    }

    public void setDepthProperties(DocumentDepthProperties depthProperties) {
        this.depthProperties = depthProperties;
    }

    public void setUnmarshallerListener(Unmarshaller.Listener unmarshallerListener) {
        this.unmarshallerListener = unmarshallerListener;
    }

    public void setMarshallerListener(Marshaller.Listener marshallerListener) {
        this.marshallerListener = marshallerListener;
    }

    @XmlRootElement
    protected static class CollectionWrapper {
        
        @XmlAnyElement(lax = true)
        private List<?> l;
        
        public void setList(List<?> list) {
            l = list;
        }
        
        public List<?> getList() {
            if (l == null) {
                l = new ArrayList<Object>();
            }
            return l;
        }
        
        @SuppressWarnings("unchecked")
        public <T> Object getCollectionOrArray(Class<T> type, Class<?> origType,
                                               XmlJavaTypeAdapter adapter) {
            List<?> theList = getList();
            boolean adapterChecked = false;
            if (theList.size() > 0) {
                Object first = theList.get(0);
                if (first instanceof JAXBElement && !JAXBElement.class.isAssignableFrom(type)) {
                    adapterChecked = true;
                    List<Object> newList = new ArrayList<Object>(theList.size());
                    for (Object o : theList) {
                        newList.add(org.apache.cxf.jaxrs.utils.JAXBUtils.useAdapter(
                                        ((JAXBElement)o).getValue(), adapter, false));
                    }
                    theList = newList;
                }
            }
            if (origType.isArray()) {
                T[] values = (T[])Array.newInstance(type, theList.size());
                for (int i = 0; i < theList.size(); i++) {
                    values[i] = (T)org.apache.cxf.jaxrs.utils.JAXBUtils.useAdapter(
                                       theList.get(i), adapter, false);
                }
                return values;
            } else {
                if (!adapterChecked && adapter != null) {
                    List<Object> newList = new ArrayList<Object>(theList.size());
                    for (Object o : theList) {
                        newList.add(org.apache.cxf.jaxrs.utils.JAXBUtils.useAdapter(o, adapter, false));
                    }
                    theList = newList;
                }
                if (origType == Set.class) {
                    return new HashSet(theList);
                } else {
                    return theList;
                }
            }
        }
        
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
