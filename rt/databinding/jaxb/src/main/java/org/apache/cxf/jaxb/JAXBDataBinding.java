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

package org.apache.cxf.jaxb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.xml.sax.SAXException;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.CacheMap;
import org.apache.cxf.common.util.CachedClass;
import org.apache.cxf.common.util.ModCountCopyOnWriteArrayList;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.databinding.AbstractDataBinding;
import org.apache.cxf.databinding.AbstractWrapperHelper;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.databinding.WrapperCapableDatabinding;
import org.apache.cxf.databinding.WrapperHelper;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.jaxb.attachment.JAXBAttachmentSchemaValidationHack;
import org.apache.cxf.jaxb.io.DataReaderImpl;
import org.apache.cxf.jaxb.io.DataWriterImpl;
import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.ws.addressing.ObjectFactory;

public class JAXBDataBinding extends AbstractDataBinding 
    implements WrapperCapableDatabinding, InterceptorProvider {
    
    public static final String SCHEMA_RESOURCE = "SCHEMRESOURCE";
    public static final String MTOM_THRESHOLD = "org.apache.cxf.jaxb.mtomThreshold";

    public static final String UNWRAP_JAXB_ELEMENT = "unwrap.jaxb.element";

    public static final String USE_JAXB_BRIDGE = "use.jaxb.bridge";

    private static final Logger LOG = LogUtils.getLogger(JAXBDataBinding.class);

    private static final Class<?> SUPPORTED_READER_FORMATS[] = new Class<?>[] {Node.class,
                                                                               XMLEventReader.class,
                                                                               XMLStreamReader.class};
    private static final Class<?> SUPPORTED_WRITER_FORMATS[] = new Class<?>[] {OutputStream.class,
                                                                               Node.class,
                                                                               XMLEventWriter.class,
                                                                               XMLStreamWriter.class};

    private static final class CachedContextAndSchemas {
        private JAXBContext context;
        private Collection<DOMSource> schemas;

        CachedContextAndSchemas(JAXBContext context) {
            this.context = context;
        }

        public JAXBContext getContext() {
            return context;
        }

        public Collection<DOMSource> getSchemas() {
            return schemas;
        }

        public void setSchemas(Collection<DOMSource> schemas) {
            this.schemas = schemas;
        }

    }
    
    private static final Map<Set<Class<?>>, CachedContextAndSchemas> JAXBCONTEXT_CACHE 
        = new CacheMap<Set<Class<?>>, CachedContextAndSchemas>();
    
    private static final Map<Package, CachedClass> OBJECT_FACTORY_CACHE
        = new CacheMap<Package, CachedClass>();
    
    private static final Map<String, DOMResult> BUILT_IN_SCHEMAS = new HashMap<String, DOMResult>();
    static {
        URIResolver resolver = new URIResolver();
        try {
            resolver.resolve("", "classpath:/schemas/wsdl/ws-addr-wsdl.xsd", JAXBDataBinding.class);
            if (resolver.isResolved()) {
                InputStream ins = resolver.getInputStream();
                Document doc = XMLUtils.parse(ins);
                ins.close();
                DOMResult dr = new DOMResult(doc, "classpath:/schemas/wsdl/ws-addr-wsdl.xsd");
                BUILT_IN_SCHEMAS.put("http://www.w3.org/2005/02/addressing/wsdl", dr);
                resolver.unresolve();
            }
        } catch (IOException e) {
            //IGNORE
        } catch (ParserConfigurationException e) {
            //IGNORE
        } catch (SAXException e) {
            //IGNORE
        }
        try {
            resolver.resolve("", "classpath:/schemas/wsdl/ws-addr.xsd", JAXBDataBinding.class);
            if (resolver.isResolved()) {
                InputStream ins = resolver.getInputStream();
                Document doc = XMLUtils.parse(ins);
                ins.close();
                DOMResult dr = new DOMResult(doc, "classpath:/schemas/wsdl/ws-addr.xsd");
                BUILT_IN_SCHEMAS.put("http://www.w3.org/2005/08/addressing", dr);
                resolver.unresolve();
            }
        } catch (IOException e) {
            //IGNORE
        } catch (ParserConfigurationException e) {
            //IGNORE
        } catch (SAXException e) {
            //IGNORE
        }
        try {
            resolver.resolve("", "classpath:/schemas/wsdl/wsrm.xsd", JAXBDataBinding.class);
            if (resolver.isResolved()) {
                InputStream ins = resolver.getInputStream();
                Document doc = XMLUtils.parse(ins);
                ins.close();
                DOMResult dr = new DOMResult(doc, "classpath:/schemas/wsdl/wsrm.xsd");
                BUILT_IN_SCHEMAS.put("http://schemas.xmlsoap.org/ws/2005/02/rm", dr);
                resolver.unresolve();
            }
        } catch (IOException e) {
            //IGNORE
        } catch (ParserConfigurationException e) {
            //IGNORE
        } catch (SAXException e) {
            //IGNORE
        }
        try {
            resolver.resolve("", "classpath:/schemas/wsdl/wsrm.xsd", JAXBDataBinding.class);
            if (resolver.isResolved()) {
                InputStream ins = resolver.getInputStream();
                Document doc = XMLUtils.parse(ins);
                ins.close();
                DOMResult dr = new DOMResult(doc, "classpath:/schemas/wsdl/wsrm.xsd");
                BUILT_IN_SCHEMAS.put("http://schemas.xmlsoap.org/ws/2005/02/rm", dr);
                resolver.unresolve();
            }
        } catch (IOException e) {
            //IGNORE
        } catch (ParserConfigurationException e) {
            //IGNORE
        } catch (SAXException e) {
            //IGNORE
        }
    }
    

    Class[] extraClass;

    JAXBContext context;
    Set<Class<?>> contextClasses;

    Class<?> cls;

    private Map<String, Object> contextProperties = Collections.emptyMap();
    private Map<String, Object> marshallerProperties = Collections.emptyMap();
    private Map<String, Object> unmarshallerProperties = Collections.emptyMap();
    private Unmarshaller.Listener unmarshallerListener;
    private Marshaller.Listener marshallerListener;
    private ValidationEventHandler validationEventHandler;
    
    private boolean unwrapJAXBElement = true;

    private boolean qualifiedSchemas;
    private Service service;
    
    private List<Interceptor> in = new ModCountCopyOnWriteArrayList<Interceptor>();
    private List<Interceptor> out = new ModCountCopyOnWriteArrayList<Interceptor>();
    private List<Interceptor> outFault  = new ModCountCopyOnWriteArrayList<Interceptor>();
    private List<Interceptor> inFault  = new ModCountCopyOnWriteArrayList<Interceptor>();

    public JAXBDataBinding() {
    }

    public JAXBDataBinding(boolean q) {
        this.qualifiedSchemas = q;
    }

    public JAXBDataBinding(Class<?>... classes) throws JAXBException {
        contextClasses = new LinkedHashSet<Class<?>>();
        contextClasses.addAll(Arrays.asList(classes));
        setContext(createJAXBContext(contextClasses)); //NOPMD - specifically allow this
    }

    public JAXBDataBinding(JAXBContext context) {
        this();
        setContext(context);
    }

    public JAXBContext getContext() {
        return context;
    }

    public final void setContext(JAXBContext ctx) {
        context = ctx;
    }

    @SuppressWarnings("unchecked")
    public <T> DataWriter<T> createWriter(Class<T> c) {

        Integer mtomThresholdInt = new Integer(getMtomThreshold());
        if (c == XMLStreamWriter.class) {
            DataWriterImpl<XMLStreamWriter> r 
                = new DataWriterImpl<XMLStreamWriter>(this);
            r.setMtomThreshold(mtomThresholdInt);
            return (DataWriter<T>)r;
        } else if (c == OutputStream.class) {
            DataWriterImpl<OutputStream> r = new DataWriterImpl<OutputStream>(this);
            r.setMtomThreshold(mtomThresholdInt);
            return (DataWriter<T>)r;
        } else if (c == XMLEventWriter.class) {
            DataWriterImpl<XMLEventWriter> r = new DataWriterImpl<XMLEventWriter>(this);
            r.setMtomThreshold(mtomThresholdInt);
            return (DataWriter<T>)r;
        } else if (c == Node.class) {
            DataWriterImpl<Node> r = new DataWriterImpl<Node>(this);
            r.setMtomThreshold(mtomThresholdInt);
            return (DataWriter<T>)r;
        }
        return null;
    }

    public Class<?>[] getSupportedWriterFormats() {
        return SUPPORTED_WRITER_FORMATS;
    }

    @SuppressWarnings("unchecked")
    public <T> DataReader<T> createReader(Class<T> c) {
        DataReader<T> dr = null;
        if (c == XMLStreamReader.class) {
            dr = (DataReader<T>)new DataReaderImpl<XMLStreamReader>(this, unwrapJAXBElement);
        } else if (c == XMLEventReader.class) {
            dr = (DataReader<T>)new DataReaderImpl<XMLEventReader>(this, unwrapJAXBElement);
        } else if (c == Node.class) {
            dr = (DataReader<T>)new DataReaderImpl<Node>(this, unwrapJAXBElement);
        }

        return dr;
    }

    public Class<?>[] getSupportedReaderFormats() {
        return SUPPORTED_READER_FORMATS;
    }

    @SuppressWarnings("unchecked")
    public void initialize(Service aservice) {
        this.service = aservice;
        
        getInInterceptors().add(JAXBAttachmentSchemaValidationHack.INSTANCE);
        getInFaultInterceptors().add(JAXBAttachmentSchemaValidationHack.INSTANCE);
        
        // context is already set, don't redo it
        if (context != null) {
            return;
        }


        contextClasses = new LinkedHashSet<Class<?>>();
        for (ServiceInfo serviceInfo : service.getServiceInfos()) {
            JAXBContextInitializer initializer = new JAXBContextInitializer(serviceInfo, contextClasses);
            initializer.walk();
            if (serviceInfo.getProperty("extra.class") != null) {
                Set<Class<?>> exClasses = serviceInfo.getProperty("extra.class", Set.class);
                contextClasses.addAll(exClasses);
            }

        }

        String tns = service.getName().getNamespaceURI();
        CachedContextAndSchemas cachedContextAndSchemas = null;
        JAXBContext ctx = null;
        try {
            if (service.getServiceInfos().size() > 0) {
                tns = service.getServiceInfos().get(0).getInterface().getName().getNamespaceURI();
            }
            cachedContextAndSchemas = createJAXBContextAndSchemas(contextClasses, tns);
        } catch (JAXBException e1) {
            // load jaxb needed class and try to create jaxb context for more
            // times
            boolean added = addJaxbObjectFactory(e1);
            while (cachedContextAndSchemas == null && added) {
                try {
                    ctx = JAXBContext.newInstance(contextClasses
                                                  .toArray(new Class[contextClasses.size()]), null);
                    cachedContextAndSchemas = new CachedContextAndSchemas(ctx);
                } catch (JAXBException e) {
                    e1 = e;
                    added = addJaxbObjectFactory(e1);
                }
            }

            if (ctx == null) {
                throw new ServiceConstructionException(e1);
            } else {
                synchronized (JAXBCONTEXT_CACHE) {
                    JAXBCONTEXT_CACHE.put(contextClasses, cachedContextAndSchemas);
                }                
            }
        }
        ctx = cachedContextAndSchemas.getContext();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "CREATED_JAXB_CONTEXT", new Object[] {ctx, contextClasses});
        }
        setContext(ctx);

        for (ServiceInfo serviceInfo : service.getServiceInfos()) {
            SchemaCollection col = serviceInfo.getXmlSchemaCollection();

            if (col.getXmlSchemas().length > 1) {
                // someone has already filled in the types
                continue;
            }

            boolean schemasFromCache = false;
            Collection<DOMSource> schemas = getSchemas();
            if (schemas == null) {
                schemas = cachedContextAndSchemas.getSchemas();
                if (schemas != null) {
                    schemasFromCache = true;
                }
            } else {
                schemasFromCache = true;
            }
            Set<DOMSource> bi = new LinkedHashSet<DOMSource>();
            if (schemas == null) {
                schemas = new LinkedHashSet<DOMSource>();
                try {
                    for (DOMResult r : generateJaxbSchemas()) {
                        DOMSource src = new DOMSource(r.getNode(), r.getSystemId());
                        if (BUILT_IN_SCHEMAS.containsValue(r)) {
                            bi.add(src);
                        } else {
                            schemas.add(src);                            
                        }
                    }
                    //put any builtins at the end.   Anything that DOES import them 
                    //will cause it to load automatically and we'll skip them later
                    schemas.addAll(bi);
                } catch (IOException e) {
                    throw new ServiceConstructionException(new Message("SCHEMA_GEN_EXC", LOG), e);
                }
            }
            for (DOMSource r : schemas) {
                if (bi.contains(r)) {
                    String ns = ((Document)r.getNode()).getDocumentElement().getAttribute("targetNamespace");
                    if (serviceInfo.getSchema(ns) != null) {
                        continue;
                    }
                }
                addSchemaDocument(serviceInfo, 
                                  col, 
                                 (Document)r.getNode(),
                                  r.getSystemId());
            }

            JAXBContext riContext;
            if (context.getClass().getName().contains("com.sun.xml.")) {
                riContext = context;
            } else {
                // fall back if we're using another jaxb implementation
                try {
                    riContext = JAXBUtils.createRIContext(contextClasses
                        .toArray(new Class[contextClasses.size()]), tns);
                } catch (JAXBException e) {
                    throw new ServiceConstructionException(e);
                }
            }

            JAXBSchemaInitializer schemaInit = new JAXBSchemaInitializer(serviceInfo, col, riContext,
                                                                         this.qualifiedSchemas);
            schemaInit.walk();
            if (cachedContextAndSchemas != null && !schemasFromCache) {
                cachedContextAndSchemas.setSchemas(schemas);
            }
        }
    }
    
    public void setExtraClass(Class[] userExtraClass) {
        extraClass = userExtraClass;
    }

    public Class[] getExtraClass() {
        return extraClass;
    }

    // default access for tests.
    List<DOMResult> generateJaxbSchemas() throws IOException {
        return JAXBUtils.generateJaxbSchemas(context, BUILT_IN_SCHEMAS);
    }

    public JAXBContext createJAXBContext(Set<Class<?>> classes) throws JAXBException {
        return createJAXBContext(classes, null);
    }

    public JAXBContext createJAXBContext(Set<Class<?>> classes, String defaultNs) throws JAXBException {
        return createJAXBContextAndSchemas(classes, defaultNs).getContext();
    }
    
    public CachedContextAndSchemas createJAXBContextAndSchemas(Set<Class<?>> classes,
                                                               String defaultNs) 
        throws JAXBException {
        
        // add user extra class into jaxb context
        if (extraClass != null && extraClass.length > 0) {
            for (Class clz : extraClass) {
                classes.add(clz);
            }
        }

        JAXBUtils.scanPackages(classes, OBJECT_FACTORY_CACHE);
        addWsAddressingTypes(classes);

        for (Class<?> clz : classes) {
            if (clz.getName().endsWith("ObjectFactory")
                && checkObjectFactoryNamespaces(clz)) {
                // kind of a hack, but ObjectFactories may be created with empty
                // namespaces
                defaultNs = null;
            }
        }

        Map<String, Object> map = new HashMap<String, Object>();
        if (defaultNs != null) {
            map.put("com.sun.xml.bind.defaultNamespaceRemap", defaultNs);
        }

        if (contextProperties != null) {
            // add any specified context properties into the properties map
            map.putAll(contextProperties);
        }

        CachedContextAndSchemas cachedContextAndSchemas = null;
        synchronized (JAXBCONTEXT_CACHE) {
            cachedContextAndSchemas = JAXBCONTEXT_CACHE.get(classes);
        }
        if (cachedContextAndSchemas == null) {
            JAXBContext ctx;
            try {
                ctx = JAXBContext.newInstance(classes.toArray(new Class[classes.size()]), map);
            } catch (JAXBException ex) {
                if (map.containsKey("com.sun.xml.bind.defaultNamespaceRemap")
                    && ex.getMessage().contains("com.sun.xml.bind.defaultNamespaceRemap")) {
                    map.put("com.sun.xml.internal.bind.defaultNamespaceRemap",
                            map.remove("com.sun.xml.bind.defaultNamespaceRemap"));
                    ctx = JAXBContext.newInstance(classes.toArray(new Class[classes.size()]), map);
                } else {
                    throw ex;
                }
            }
            cachedContextAndSchemas = new CachedContextAndSchemas(ctx);
            synchronized (JAXBCONTEXT_CACHE) {
                JAXBCONTEXT_CACHE.put(classes, cachedContextAndSchemas);
            }
        }

        return cachedContextAndSchemas;
    }
    
    private boolean checkObjectFactoryNamespaces(Class<?> clz) {
        for (Method meth : clz.getMethods()) {
            XmlElementDecl decl = meth.getAnnotation(XmlElementDecl.class);
            if (decl != null 
                && XmlElementDecl.GLOBAL.class.equals(decl.scope())
                && StringUtils.isEmpty(decl.namespace())) {
                return true;
            }
        }

        return false;
    }

    private void addWsAddressingTypes(Set<Class<?>> classes) {
        if (classes.contains(ObjectFactory.class)) {
            // ws-addressing is used, lets add the specific types
            try {
                classes.add(Class.forName("org.apache.cxf.ws.addressing.wsdl.ObjectFactory"));
                classes.add(Class.forName("org.apache.cxf.ws.addressing.wsdl.AttributedQNameType"));
                classes.add(Class.forName("org.apache.cxf.ws.addressing.wsdl.ServiceNameType"));
            } catch (ClassNotFoundException unused) {
                // REVISIT - ignorable if WS-ADDRESSING not available?
                // maybe add a way to allow interceptors to add stuff to the
                // context?
            }
        }
    }
    
    public Set<Class<?>> getContextClasses() {
        return Collections.unmodifiableSet(this.contextClasses);
    }
    
    // Now we can not add all the classes that Jaxb needed into JaxbContext,
    // especially when
    // an ObjectFactory is pointed to by an jaxb @XmlElementDecl annotation
    // added this workaround method to load the jaxb needed ObjectFactory class
    public boolean addJaxbObjectFactory(JAXBException e1) {
        boolean added = false;
        java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();
        java.io.PrintStream pout = new java.io.PrintStream(bout);
        e1.printStackTrace(pout);
        String str = new String(bout.toByteArray());
        Pattern pattern = Pattern.compile("(?<=There's\\sno\\sObjectFactory\\swith\\san\\s"
                                          + "@XmlElementDecl\\sfor\\sthe\\selement\\s\\{)\\S*(?=\\})");
        java.util.regex.Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            String pkgName = JAXBUtils.namespaceURIToPackage(matcher.group());
            try {
                Class clz = getClass().getClassLoader().loadClass(pkgName + "." + "ObjectFactory");

                if (!contextClasses.contains(clz)) {
                    contextClasses.add(clz);
                    added = true;
                }
            } catch (ClassNotFoundException e) {
                // do nothing
            }

        }
        return added;
    }

    /**
     * Return a map of properties. These properties are passed to
     * JAXBContext.newInstance when this object creates a context.
     * 
     * @return the map of JAXB context properties.
     */
    public Map<String, Object> getContextProperties() {
        return contextProperties;
    }

    /**
     * Set a map of JAXB context properties. These properties are passed to
     * JAXBContext.newInstance when this object creates a context. Note that if
     * you create a JAXB context elsewhere, you will not respect these
     * properties unless you handle it manually.
     * 
     * @param contextProperties map of properties.
     */
    public void setContextProperties(Map<String, Object> contextProperties) {
        this.contextProperties = contextProperties;
    }

    /**
     * Return a map of properties. These properties are set into the JAXB
     * Marshaller (via Marshaller.setProperty(...) when the marshaller is
     * created.
     * 
     * @return the map of JAXB marshaller properties.
     */
    public Map<String, Object> getMarshallerProperties() {
        return marshallerProperties;
    }

    /**
     * Set a map of JAXB marshaller properties. These properties are set into
     * the JAXB Marshaller (via Marshaller.setProperty(...) when the marshaller
     * is created.
     * 
     * @param marshallerProperties map of properties.
     */
    public void setMarshallerProperties(Map<String, Object> marshallerProperties) {
        this.marshallerProperties = marshallerProperties;
    }
    
    
    /**
     * Return a map of properties. These properties are set into the JAXB
     * Unmarshaller (via Unmarshaller.setProperty(...) when the unmarshaller is
     * created.
     * 
     * @return the map of JAXB unmarshaller properties.
     */
    public Map<String, Object> getUnmarshallerProperties() {
        return unmarshallerProperties;
    }

    /**
     * Set a map of JAXB unmarshaller properties. These properties are set into
     * the JAXB Unmarshaller (via Unmarshaller.setProperty(...) when the unmarshaller
     * is created.
     * 
     * @param unmarshallerProperties map of properties.
     */
    public void setUnmarshallerProperties(Map<String, Object> unmarshallerProperties) {
        this.unmarshallerProperties = unmarshallerProperties;
    }
    
    /**
     * Returns the Unmarshaller.Listener that will be registered on the Unmarshallers
     * @return
     */
    public Unmarshaller.Listener getUnmarshallerListener() {
        return unmarshallerListener;
    }

    /**
     * Sets the Unmarshaller.Listener that will be registered on the Unmarshallers
     * @param unmarshallerListener
     */
    public void setUnmarshallerListener(Unmarshaller.Listener unmarshallerListener) {
        this.unmarshallerListener = unmarshallerListener;
    }
    /**
     * Returns the Marshaller.Listener that will be registered on the Marshallers
     * @return
     */
    public Marshaller.Listener getMarshallerListener() {
        return marshallerListener;
    }

    /**
     * Sets the Marshaller.Listener that will be registered on the Marshallers
     * @param marshallerListener
     */
    public void setMarshallerListener(Marshaller.Listener marshallerListener) {
        this.marshallerListener = marshallerListener;
    }
    
    
    public ValidationEventHandler getValidationEventHandler() {
        return validationEventHandler;
    }

    public void setValidationEventHandler(ValidationEventHandler validationEventHandler) {
        this.validationEventHandler = validationEventHandler;
    }

    
    public boolean isUnwrapJAXBElement() {
        return unwrapJAXBElement;
    }

    public void setUnwrapJAXBElement(boolean unwrapJAXBElement) {
        this.unwrapJAXBElement = unwrapJAXBElement;
    }

    public static void clearCaches() {
        synchronized (JAXBCONTEXT_CACHE) {
            JAXBCONTEXT_CACHE.clear();
        }
        synchronized (OBJECT_FACTORY_CACHE) {
            OBJECT_FACTORY_CACHE.clear();
        }
    }

    public WrapperHelper createWrapperHelper(Class<?> wrapperType, QName wrapperName, List<String> partNames,
                                             List<String> elTypeNames, List<Class<?>> partClasses) {
        List<Method> getMethods = new ArrayList<Method>(partNames.size());
        List<Method> setMethods = new ArrayList<Method>(partNames.size());
        List<Method> jaxbMethods = new ArrayList<Method>(partNames.size());
        List<Field> fields = new ArrayList<Field>(partNames.size());
        
        Method allMethods[] = wrapperType.getMethods();
        String packageName = PackageUtils.getPackageName(wrapperType);
        
        //if wrappertype class is generated by ASM,getPackage() always return null
        if (wrapperType.getPackage() != null) {
            packageName = wrapperType.getPackage().getName();
        }
        
        String objectFactoryClassName = packageName + ".ObjectFactory";

        Object objectFactory = null;
        try {
            objectFactory = wrapperType.getClassLoader().loadClass(objectFactoryClassName).newInstance();
        } catch (Exception e) {
            //ignore, probably won't need it
        }
        Method allOFMethods[];
        if (objectFactory != null) {
            allOFMethods = objectFactory.getClass().getMethods(); 
        } else {
            allOFMethods = new Method[0];
        }
        
        for (int x = 0; x < partNames.size(); x++) {
            String partName = partNames.get(x);            
            if (partName == null) {
                getMethods.add(null);
                setMethods.add(null);
                fields.add(null);
                jaxbMethods.add(null);
                continue;
            }
            
            String elementType = elTypeNames.get(x);
            
            String getAccessor = JAXBUtils.nameToIdentifier(partName, JAXBUtils.IdentifierType.GETTER);
            String setAccessor = JAXBUtils.nameToIdentifier(partName, JAXBUtils.IdentifierType.SETTER);
            Method getMethod = null;
            Method setMethod = null;
            Class<?> valueClass = wrapperType;
            
            try {               
                getMethod = valueClass.getMethod(getAccessor, AbstractWrapperHelper.NO_CLASSES); 
            } catch (NoSuchMethodException ex) {
                //ignore for now
            }
            
            Field elField = getElField(partName, valueClass);
            if (getMethod == null
                && elementType != null
                && "boolean".equals(elementType.toLowerCase())
                && (elField == null
                    || (!Collection.class.isAssignableFrom(elField.getType())
                    && !elField.getType().isArray()))) {
        
                try {
                    String newAcc = getAccessor.replaceFirst("get", "is");
                    getMethod = wrapperType.getMethod(newAcc, AbstractWrapperHelper.NO_CLASSES); 
                } catch (NoSuchMethodException ex) {
                    //ignore for now
                }            
            }                        
            if (getMethod == null 
                && "return".equals(partName)) {
                //RI generated code uses this
                try {
                    getMethod = valueClass.getMethod("get_return", AbstractWrapperHelper.NO_CLASSES);
                } catch (NoSuchMethodException ex) {
                    try {
                        getMethod = valueClass.getMethod("is_return",
                                                          new Class[0]);
                    } catch (NoSuchMethodException ex2) {
                        //ignore for now
                    } 
                }                
            }
            if (getMethod == null && elField != null) {
                getAccessor = JAXBUtils.nameToIdentifier(elField.getName(), JAXBUtils.IdentifierType.GETTER);
                setAccessor = JAXBUtils.nameToIdentifier(elField.getName(), JAXBUtils.IdentifierType.SETTER);
                try {               
                    getMethod = valueClass.getMethod(getAccessor, AbstractWrapperHelper.NO_CLASSES); 
                } catch (NoSuchMethodException ex) {
                    //ignore for now
                }
            }
            String setAccessor2 = setAccessor;
            if ("return".equals(partName)) {
                //some versions of jaxb map "return" to "set_return" instead of "setReturn"
                setAccessor2 = "set_return";
            }

            for (Method method : allMethods) {
                if (method.getParameterTypes() != null && method.getParameterTypes().length == 1
                    && (setAccessor.equals(method.getName())
                        || setAccessor2.equals(method.getName()))) {
                    setMethod = method;
                    break;
                }
            }
            
            getMethods.add(getMethod);
            setMethods.add(setMethod);
            if (setMethod != null
                && JAXBElement.class.isAssignableFrom(setMethod.getParameterTypes()[0])) {
                
                String methodName = "create" + wrapperType.getSimpleName()
                    + setMethod.getName().substring(3);

                for (Method m : allOFMethods) {
                    if (m.getName().equals(methodName)) {
                        jaxbMethods.add(m);
                    }
                }
            } else {
                jaxbMethods.add(null);
            }
            
            if (elField != null) {
                // JAXB Type get XmlElement Annotation
                XmlElement el = elField.getAnnotation(XmlElement.class);
                if (el != null
                    && partName.equals(el.name())) {
                    elField.setAccessible(true);
                    fields.add(elField);
                } else {
                    fields.add(null);
                } 
            } else {
                fields.add(null);
            }
            
        }
        
        return createWrapperHelper(wrapperType,
                                 setMethods.toArray(new Method[setMethods.size()]),
                                 getMethods.toArray(new Method[getMethods.size()]),
                                 jaxbMethods.toArray(new Method[jaxbMethods.size()]),
                                 fields.toArray(new Field[fields.size()]),
                                 objectFactory);
    }
    
    private static Field getElField(String partName, Class<?> wrapperType) {
        String fieldName = JAXBUtils.nameToIdentifier(partName, JAXBUtils.IdentifierType.VARIABLE);
        for (Field field : wrapperType.getDeclaredFields()) {
            XmlElement el = field.getAnnotation(XmlElement.class);
            if (el != null
                && partName.equals(el.name())) {
                return field;
            }
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }        
        return null;
    }

    
    private static WrapperHelper createWrapperHelper(Class<?> wrapperType, Method setMethods[],
                                                     Method getMethods[], Method jaxbMethods[],
                                                     Field fields[], Object objectFactory) {

        WrapperHelper wh = compileWrapperHelper(wrapperType, setMethods, getMethods, jaxbMethods, fields,
                                                objectFactory);

        if (wh == null) {
            wh = new JAXBWrapperHelper(wrapperType, setMethods, getMethods, jaxbMethods, fields,
                                       objectFactory);
        }
        return wh;
    }

    private static WrapperHelper compileWrapperHelper(Class<?> wrapperType, Method setMethods[],
                                                      Method getMethods[], Method jaxbMethods[],
                                                      Field fields[], Object objectFactory) {
        try {
            Class.forName("org.objectweb.asm.ClassWriter");
            return WrapperHelperCompiler.compileWrapperHelper(wrapperType, setMethods, getMethods,
                                                              jaxbMethods, fields, objectFactory);
        } catch (ClassNotFoundException e) {
            // ASM not found, just use reflection based stuff
        }
        return null;
    }   
    
    public List<Interceptor> getOutFaultInterceptors() {
        return outFault;
    }

    public List<Interceptor> getInFaultInterceptors() {
        return inFault;
    }

    public List<Interceptor> getInInterceptors() {
        return in;
    }

    public List<Interceptor> getOutInterceptors() {
        return out;
    }


}
