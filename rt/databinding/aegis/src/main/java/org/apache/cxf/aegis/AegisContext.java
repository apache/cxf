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
package org.apache.cxf.aegis;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;

import org.xml.sax.SAXException;

import org.apache.cxf.aegis.type.AbstractTypeCreator;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.DefaultTypeCreator;
import org.apache.cxf.aegis.type.DefaultTypeMapping;
import org.apache.cxf.aegis.type.TypeCreationOptions;
import org.apache.cxf.aegis.type.TypeCreator;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.aegis.type.XMLTypeCreator;
import org.apache.cxf.aegis.type.basic.BeanType;
import org.apache.cxf.aegis.type.java5.Java5TypeCreator;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.xmlschema.XmlSchemaUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;

/**
 * The Aegis Databinding context object. This object coordinates the data binding process: reading and writing
 * XML. By default, this object sets up a default set of type mappings. This consists of two
 * DefaultTypeMapping objects. The first is empty and has the Default, Java5, and XML TypeCreator classes
 * configured. The second contains the standard mappings of the stock types. If a type can't be mapped in
 * either, then the creators create a mapping and store it in the first one. The application can control some
 * parameters of the type creators by creating a TypeCreationOptions object and setting properties. The
 * application can add custom mappings to the type mapping, or even use its own classes for the TypeMapping or
 * TypeCreator objects. Aegis, unlike JAXB, has no concept of a 'root element'. So, an application that uses
 * Aegis without a web service has to either depend on xsi:type (at least for root elements) or have its own
 * mapping from elements to classes, and pass the resulting Class objects to the readers. At this level, the
 * application must specify the initial set of classes to make make use of untyped collections or .aegis.xml
 * files. If the application leaves this list empty, and reads XML messages, then no .aegis.xml files are used
 * unless the application has specified a Class&lt;T&gt; for the root of a particular item read. Specifically,
 * if the application just leaves it to Aegis to map an element tagged with an xsi:type to a class, Aegis
 * can't know that some arbitrary class in some arbitrary package is mapped to a particular schema type by
 * QName in a mapping XML file. At the level of the CXF data binding, the 'root elements' are defined by the
 * WSDL message parts. Additional classes that participate are termed 'override' classes.
 */
public class AegisContext {

    /**
     * Namespace used for the miscellaneous Aegis type schema.
     */
    public static final String UTILITY_TYPES_SCHEMA_NS = "http://cxf.apache.org/aegisTypes";
    private Document aegisTypesSchemaDocument;
    private Document xmimeSchemaDocument;

    private boolean writeXsiTypes;
    private boolean readXsiTypes = true;

    private Set<String> rootClassNames;
    private Set<java.lang.reflect.Type> rootClasses;
    private Set<QName> rootTypeQNames;
    // this type mapping is the front of the chain of delegating type mappings.
    private TypeMapping typeMapping;
    private Set<AegisType> rootTypes;
    private Map<Class<?>, String> beanImplementationMap;
    private TypeCreationOptions configuration;
    private boolean mtomEnabled;
    private boolean mtomUseXmime;
    private boolean enableJDOMMappings;
    // this URI goes into the type map.
    private String mappingNamespaceURI;

    /**
     * Construct a context.
     */
    public AegisContext() {
        beanImplementationMap = new HashMap<Class<?>, String>();
        rootClasses = new HashSet<java.lang.reflect.Type>();
        rootTypeQNames = new HashSet<QName>();
    }

    public TypeCreator createTypeCreator() {
        AbstractTypeCreator xmlCreator = createRootTypeCreator();

        Java5TypeCreator j5Creator = new Java5TypeCreator();
        j5Creator.setNextCreator(createDefaultTypeCreator());
        j5Creator.setConfiguration(getTypeCreationOptions());
        xmlCreator.setNextCreator(j5Creator);

        return xmlCreator;
    }

    protected AbstractTypeCreator createRootTypeCreator() {
        AbstractTypeCreator creator = new XMLTypeCreator();
        creator.setConfiguration(getTypeCreationOptions());
        return creator;
    }

    protected AbstractTypeCreator createDefaultTypeCreator() {
        AbstractTypeCreator creator = new DefaultTypeCreator();
        creator.setConfiguration(getTypeCreationOptions());
        return creator;
    }

    /**
     * Initialize the context. The encodingStyleURI allows .aegis.xml files to have multiple mappings for,
     * say, SOAP 1.1 versus SOAP 1.2. Passing null uses a default URI.
     * 
     * @param mappingNamespaceURI URI to select mappings based on the encoding.
     */
    public void initialize() {
        // allow spring config of an alternative mapping.
        if (configuration == null) {
            configuration = new TypeCreationOptions();
        }
        if (typeMapping == null) {
            boolean defaultNillable = configuration.isDefaultNillable();
            TypeMapping baseTM = DefaultTypeMapping.createDefaultTypeMapping(defaultNillable, 
                                                                             mtomUseXmime, 
                                                                             enableJDOMMappings);
            if (mappingNamespaceURI == null) {
                mappingNamespaceURI = DefaultTypeMapping.DEFAULT_MAPPING_URI;
            }
            DefaultTypeMapping defaultTypeMapping = new DefaultTypeMapping(mappingNamespaceURI, baseTM);
            defaultTypeMapping.setTypeCreator(createTypeCreator());
            typeMapping = defaultTypeMapping;
        }

        processRootTypes();
    }

    public AegisReader<org.w3c.dom.Element> createDomElementReader() {
        return new AegisElementDataReader(this);
    }

    public AegisReader<XMLStreamReader> createXMLStreamReader() {
        return new AegisXMLStreamDataReader(this);
    }

    public AegisWriter<org.w3c.dom.Element> createDomElementWriter() {
        return new AegisElementDataWriter(this);
    }

    public AegisWriter<XMLStreamWriter> createXMLStreamWriter() {
        return new AegisXMLStreamDataWriter(this);
    }

    /**
     * If a class was provided as part of the 'root' list, retrieve it's AegisType by Class.
     * 
     * @param clazz
     * @return
     */
    public AegisType getRootType(Class clazz) {
        if (rootClasses.contains(clazz)) {
            return typeMapping.getType(clazz);
        } else {
            return null;
        }
    }

    /**
     * If a class was provided as part of the root list, retrieve it's AegisType by schema type QName.
     * 
     * @param schemaTypeName
     * @return
     */
    public AegisType getRootType(QName schemaTypeName) {
        if (rootTypeQNames.contains(schemaTypeName)) {
            return typeMapping.getType(schemaTypeName);
        } else {
            return null;
        }
    }
    
    private Set<Class<?>> rootMappableClasses() {
        Set<Class<?>> mappableClasses = new HashSet<Class<?>>();
        for (java.lang.reflect.Type jtype : rootClasses) {
            addTypeToMappableClasses(mappableClasses, jtype);
        }
        return mappableClasses;
    }

    private void addTypeToMappableClasses(Set<Class<?>> mappableClasses, java.lang.reflect.Type jtype) {
        if (jtype instanceof Class) {
            Class<?> jclass = (Class<?>) jtype;
            if (jclass.isArray()) {
                mappableClasses.add(jclass.getComponentType());
            }
            mappableClasses.add(jclass);
        } else if (jtype instanceof ParameterizedType) {
            for (java.lang.reflect.Type t2 : ((ParameterizedType)jtype).getActualTypeArguments()) {
                addTypeToMappableClasses(mappableClasses, t2);
            }
        } else if (jtype instanceof GenericArrayType) {
            GenericArrayType gt = (GenericArrayType)jtype;
            Class ct = (Class) gt.getGenericComponentType();
            // this looks nutty, but there's no other way. Make an array and take it's class.
            ct = Array.newInstance(ct, 0).getClass();
            rootClasses.add(ct);
        }
    }

    /**
     * Examine a list of override classes, and register all of them.
     * 
     * @param tm type manager for this binding
     * @param classes list of class names
     */
    private void processRootTypes() {
        rootTypes = new HashSet<AegisType>();
        // app may have already supplied classes.
        if (rootClasses == null) {
            rootClasses = new HashSet<java.lang.reflect.Type>();
        }
        rootTypeQNames = new HashSet<QName>();
        if (this.rootClassNames != null) {
            for (String typeName : rootClassNames) {
                Class c = null;
                try {
                    c = ClassLoaderUtils.loadClass(typeName, TypeUtil.class);
                } catch (ClassNotFoundException e) {
                    throw new DatabindingException("Could not find override type class: " + typeName, e);
                }

                rootClasses.add(c);
            }
        }

        // This is a list of AegisType rather than Class so that it can set up for generic collections.
        // When we see a generic, we process both the generic outer class and each parameter class.
        // This is not the same thing as allowing mappings of arbitrary x<q> types.
        
        Set<Class<?>> rootMappableClassSet = rootMappableClasses();
        /*
         * First loop: process non-Class roots, creating full types for them
         * and registering them.
         */
        for (java.lang.reflect.Type reflectType : rootClasses) {
            if (!(reflectType instanceof Class)) {
                // if it's not a Class, it can't be mapped from Class to type in the mapping.
                // so we create 
                AegisType aegisType = typeMapping.getTypeCreator().createType(reflectType);
                typeMapping.register(aegisType);
                // note: we don't handle arbitrary generics, so no BeanType 
                // check here.
                rootTypeQNames.add(aegisType.getSchemaType());
            } 
        }
        /*
         * Second loop: process Class roots, including those derived from 
         * generic types, creating when not in the default mappings.
         */
        for (Class<?> c : rootMappableClassSet) {
            AegisType t = typeMapping.getType(c);
            if (t == null) {
                t = typeMapping.getTypeCreator().createType(c);
                typeMapping.register(t);
            }
            rootTypeQNames.add(t.getSchemaType());
            if (t instanceof BeanType) {
                BeanType bt = (BeanType)t;
                bt.getTypeInfo().setExtension(true);
                rootTypes.add(bt);
            }
        }
    }

    public static boolean schemaImportsUtilityTypes(XmlSchema schema) {
        return XmlSchemaUtils.schemaImportsNamespace(schema, UTILITY_TYPES_SCHEMA_NS);
    }
    
    private Document getSchemaDocument(String resourcePath) { 
        try {
            return XMLUtils.parse(getClass().getResourceAsStream(resourcePath));
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    // could we make these documents static? What would we synchronize on?
    private Document getAegisTypesSchemaDocument() { 
        if (aegisTypesSchemaDocument == null) {
            aegisTypesSchemaDocument = getSchemaDocument("/META-INF/cxf/aegisTypes.xsd");
        } 
        return aegisTypesSchemaDocument;
    }
    
    private Document getXmimeSchemaDocument() {
        if (xmimeSchemaDocument == null) {
            xmimeSchemaDocument = getSchemaDocument("/schemas/wsdl/xmime.xsd");
        }
        return xmimeSchemaDocument;
    }

    public XmlSchema addTypesSchemaDocument(XmlSchemaCollection collection) {
        return collection.read(getAegisTypesSchemaDocument(), null);
    }
    
    public XmlSchema addXmimeSchemaDocument(XmlSchemaCollection collection) {
        return collection.read(getXmimeSchemaDocument(), null);
    }

    public static void addUtilityTypesToSchema(XmlSchema root) {
        XmlSchemaUtils.addImportIfNeeded(root, UTILITY_TYPES_SCHEMA_NS);
    }

    /**
     * Retrieve the set of root class names. Note that if the application specifies the root classes by Class
     * instead of by name, this will return null.
     * 
     * @return
     */
    public Set<String> getRootClassNames() {
        return rootClassNames;
    }

    /**
     * Set the root class names. This function is a convenience for Spring configuration. It sets the same
     * underlying collection as {@link #setRootClasses(Set)}.
     * 
     * @param classNames
     */
    public void setRootClassNames(Set<String> classNames) {
        rootClassNames = classNames;
    }

    /**
     * Return the type mapping configuration associated with this context.
     * 
     * @return Returns the configuration.
     * @deprecated 2.1
     */
    public TypeCreationOptions getConfiguration() {
        return configuration;
    }

    /**
     * Return the type mapping configuration associated with this context.
     * 
     * @return Returns the configuration.
     */
    public TypeCreationOptions getTypeCreationOptions() {
        return configuration;
    }

    /**
     * Set the configuration object. The configuration specifies default type mapping behaviors.
     * 
     * @param configuration The configuration to set.
     * @deprecated 2.1
     */
    public void setConfiguration(TypeCreationOptions newConfiguration) {
        this.configuration = newConfiguration;
    }

    /**
     * Set the configuration object. The configuration specifies default type mapping behaviors.
     * 
     * @param configuration The configuration to set.
     */
    public void setTypeCreationOptions(TypeCreationOptions newConfiguration) {
        this.configuration = newConfiguration;
    }

    public boolean isWriteXsiTypes() {
        return writeXsiTypes;
    }

    public boolean isReadXsiTypes() {
        return readXsiTypes;
    }

    /**
     * Controls whether Aegis writes xsi:type attributes on all elements. False by default.
     * 
     * @param flag
     */
    public void setWriteXsiTypes(boolean flag) {
        this.writeXsiTypes = flag;
    }

    /**
     * Controls the use of xsi:type attributes when reading objects. By default, xsi:type reading is enabled.
     * When disabled, Aegis will only map for objects that the application manually maps in the type mapping.
     * 
     * @param flag
     */
    public void setReadXsiTypes(boolean flag) {
        this.readXsiTypes = flag;
    }

    /**
     * Return the type mapping object used by this context.
     * 
     * @return
     */
    public TypeMapping getTypeMapping() {
        return typeMapping;
    }

    /**
     * Set the type mapping object used by this context.
     * 
     * @param typeMapping
     */
    public void setTypeMapping(TypeMapping typeMapping) {
        this.typeMapping = typeMapping;
    }

    /**
     * Retrieve the Aegis type objects for the root classes.
     * 
     * @return the set of type objects.
     */
    public Set<AegisType> getRootTypes() {
        return rootTypes;
    }

    /**
     * This property provides support for interfaces. If there is a mapping from an interface's Class<T> to a
     * string containing a class name, Aegis will create proxy objects of that class name.
     * 
     * @see org.apache.cxf.aegis.type.basic.BeanType
     * @return
     */
    public Map<Class<?>, String> getBeanImplementationMap() {
        return beanImplementationMap;
    }

    public void setBeanImplementationMap(Map<Class<?>, String> beanImplementationMap) {
        this.beanImplementationMap = beanImplementationMap;
    }

    public Set<java.lang.reflect.Type> getRootClasses() {
        return rootClasses;
    }

    /**
     * The list of initial classes.
     * 
     * @param rootClasses
     */
    public void setRootClasses(Set<java.lang.reflect.Type> rootClasses) {
        this.rootClasses = rootClasses;
    }

    /**
     * Is MTOM enabled in this context?
     * 
     * @return
     */
    public boolean isMtomEnabled() {
        return mtomEnabled;
    }

    public void setMtomEnabled(boolean mtomEnabled) {
        this.mtomEnabled = mtomEnabled;
    }

    /**
     * Should this service use schema for MTOM types xmime:base64Binary instead of xsd:base64Binary?
     * 
     * @return
     */
    public boolean isMtomUseXmime() {
        return mtomUseXmime;
    }

    public void setMtomUseXmime(boolean mtomUseXmime) {
        this.mtomUseXmime = mtomUseXmime;
    }

    /**
     * What URI identifies the type mapping for this context? When the XMLTypeCreator reads .aegis.xml file,
     * it will only read mappings for this URI (or no URI). When the abstract type creator is otherwise at a
     * loss for a namespace URI, it will use this URI.
     * 
     * @return
     */
    public String getMappingNamespaceURI() {
        return mappingNamespaceURI;
    }

    public void setMappingNamespaceURI(String mappingNamespaceURI) {
        this.mappingNamespaceURI = mappingNamespaceURI;
        if (typeMapping != null) {
            typeMapping.setMappingIdentifierURI(mappingNamespaceURI);
        }
    }

    public boolean isEnableJDOMMappings() {
        return enableJDOMMappings;
    }
    
    /**
     * Whether to enable JDOM as a mapping for xsd:anyType if JDOM is in the classpath. 
     * @param enableJDOMMappings
     */
    public void setEnableJDOMMappings(boolean enableJDOMMappings) {
        this.enableJDOMMappings = enableJDOMMappings;
    }
}
