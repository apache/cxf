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
package org.apache.cxf.aegis.databinding;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Node;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeClassInfo;
import org.apache.cxf.aegis.type.TypeCreationOptions;
import org.apache.cxf.aegis.type.TypeCreator;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.type.basic.ArrayType;
import org.apache.cxf.aegis.type.java5.AnnotationReader;
import org.apache.cxf.aegis.type.mtom.AbstractXOPType;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.common.xmlschema.XmlSchemaUtils;
import org.apache.cxf.databinding.AbstractDataBinding;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.SimpleMethodDispatcher;
import org.apache.cxf.service.invoker.MethodDispatcher;
import org.apache.cxf.service.model.AbstractMessageContainer;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.validation.StaxSchemaValidationInInterceptor;
import org.apache.cxf.wsdl.WSDLConstants;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAnnotated;
import org.apache.ws.commons.schema.XmlSchemaException;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;

/**
 * CXF databinding object for Aegis. By default, this creates an AegisContext object. To customize the
 * behavior of the binding, an application should create its own AegisContext object and pass it to
 * {@link #setAegisContext(AegisContext)} <i>before</i> any call to {@link #initialize(Service)}. That does
 * not require special arrangements; the service factories do not call {{@link #initialize(Service)} until
 * after the application passes the data binding into the factory. This class adds root classes to the context
 * based on the SEI and implementation.
 *
 * @see org.apache.cxf.aegis.AegisContext
 */
public class AegisDatabinding extends AbstractDataBinding {

    protected static final int IN_PARAM = 0;
    protected static final int OUT_PARAM = 1;
    protected static final int FAULT_PARAM = 2;

    private static final Logger LOG = LogUtils.getL7dLogger(AegisDatabinding.class);

    private AegisContext aegisContext;
    private Map<MessagePartInfo, AegisType> part2Type;
    private Service service;
    private boolean isInitialized;
    private Set<String> overrideTypes;
    private TypeCreationOptions configuration;
    private boolean mtomUseXmime;

    public AegisDatabinding() {
        super();
        part2Type = new HashMap<>();
    }
    public AegisDatabinding(AegisContext ctx) {
        this();
        aegisContext = ctx;
    }

    /**
     * The Databinding API has initialize(Service). However, this object should be usable even if that API is
     * never called.
     */
    private void ensureInitialized() {
        if (!isInitialized) {
            if (aegisContext == null) {
                aegisContext = new AegisContext();
                if (overrideTypes != null) {
                    aegisContext.setRootClassNames(overrideTypes);
                }
                if (configuration != null) {
                    aegisContext.setTypeCreationOptions(configuration);
                }
                if (mtomEnabled) {
                    aegisContext.setMtomEnabled(true);
                }
                if (mtomUseXmime) {
                    aegisContext.setMtomUseXmime(true);
                }
                aegisContext.initialize();
            }
            isInitialized = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> DataReader<T> createReader(Class<T> cls) {
        ensureInitialized();
        if (cls.equals(XMLStreamReader.class)) {
            return (DataReader<T>)new XMLStreamDataReader(this, getBus());
        } else if (cls.equals(Node.class)) {
            return (DataReader<T>)new ElementDataReader(this);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> DataWriter<T> createWriter(Class<T> cls) {
        ensureInitialized();
        if (cls.equals(XMLStreamWriter.class)) {
            return (DataWriter<T>)new XMLStreamDataWriter(this, getBus());
        } else if (cls.equals(Node.class)) {
            return (DataWriter<T>)new ElementDataWriter(this);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Class<?>[] getSupportedReaderFormats() {
        return new Class[] {
            XMLStreamReader.class, Node.class
        };
    }

    /**
     * {@inheritDoc}
     */
    public Class<?>[] getSupportedWriterFormats() {
        return new Class[] {
            XMLStreamWriter.class, Node.class
        };
    }

    /**
     * {@inheritDoc} Set up the data binding for a service.
     */
    public void initialize(Service s) {

        // We want to support some compatibility configuration properties.
        if (aegisContext == null) {
            aegisContext = new AegisContext();

            Object val = s.get("mtom-enabled");
            if ("true".equals(val) || Boolean.TRUE.equals(val) || mtomEnabled) {
                setMtomEnabled(true);
                aegisContext.setMtomEnabled(true);
            }

            if (mtomUseXmime) {
                aegisContext.setMtomUseXmime(true);
            }

            Map<Class<?>, String> implMap = new HashMap<>();
            // now for a really annoying case, the .implementation objects.
            for (String key : s.keySet()) {
                if (key.endsWith(".implementation")) {
                    String className = key.substring(0, key.length() - ".implementation".length());
                    Class<?> clazz = null;
                    try {
                        clazz = ClassLoaderUtils.loadClass(className, getClass());
                    } catch (ClassNotFoundException e) {
                        Message message = new Message("MAPPED_CLASS_NOT_FOUND", LOG, className, key);
                        LOG.warning(message.toString());
                        continue;
                    }
                    String implClassName = (String)s.get(key);
                    implMap.put(clazz, implClassName);
                }
            }

            if (overrideTypes != null) {
                aegisContext.setRootClassNames(overrideTypes);
            }

            if (configuration != null) {
                aegisContext.setTypeCreationOptions(configuration);
            }

            if (!implMap.isEmpty()) {
                aegisContext.setBeanImplementationMap(implMap);
            }
        }

        aegisContext.setMappingNamespaceURI(s.getServiceInfos().get(0)
                                                .getInterface().getName().getNamespaceURI());
        aegisContext.initialize();
        this.service = s;
        s.getInInterceptors()
            .add(new StaxSchemaValidationInInterceptor());

        Set<AegisType> deps = new HashSet<>();

        for (ServiceInfo info : s.getServiceInfos()) {
            for (OperationInfo opInfo : info.getInterface().getOperations()) {
                if (opInfo.isUnwrappedCapable()) {
                    initializeOperation(s, aegisContext.getTypeMapping(), opInfo.getUnwrappedOperation(),
                                        deps);
                } else {
                    initializeOperation(s, aegisContext.getTypeMapping(), opInfo, deps);
                }
            }
        }

        Collection<AegisType> additional = aegisContext.getRootTypes();

        if (additional != null) {
            for (AegisType t : additional) {
                if (!deps.contains(t)) {
                    deps.add(t);
                }
                addDependencies(deps, t);
            }
        }

        createSchemas(s, deps);
        for (ServiceInfo info : s.getServiceInfos()) {
            for (OperationInfo opInfo : info.getInterface().getOperations()) {
                if (opInfo.isUnwrappedCapable()) {
                    initializeOperationTypes(info, opInfo.getUnwrappedOperation());
                } else {
                    initializeOperationTypes(info, opInfo);
                }
            }
        }
    }

    private void initializeOperation(Service s, TypeMapping serviceTM, OperationInfo opInfo,
                                     Set<AegisType> deps) {
        try {
            initializeMessage(s, serviceTM, opInfo.getInput(), IN_PARAM, deps);

            if (opInfo.hasOutput()) {
                initializeMessage(s, serviceTM, opInfo.getOutput(), OUT_PARAM, deps);
            }

            for (FaultInfo info : opInfo.getFaults()) {
                initializeMessage(s, serviceTM, info, FAULT_PARAM, deps);
            }

        } catch (DatabindingException e) {
            e.prepend("Error initializing parameters for operation " + opInfo.getName());
            throw e;
        }
    }

    private void initializeOperationTypes(ServiceInfo s, OperationInfo opInfo) {
        try {
            initializeMessageTypes(s, opInfo.getInput(), IN_PARAM);

            if (opInfo.hasOutput()) {
                initializeMessageTypes(s, opInfo.getOutput(), OUT_PARAM);
            }

            for (FaultInfo info : opInfo.getFaults()) {
                initializeMessageTypes(s, info, FAULT_PARAM);
            }

        } catch (DatabindingException e) {
            e.prepend("Error initializing parameters for operation " + opInfo.getName());
            throw e;
        }
    }

    protected void initializeMessage(Service s, TypeMapping serviceTM, AbstractMessageContainer container,
                                     int partType, Set<AegisType> deps) {
        if (container == null) {
            return;
        }
        for (MessagePartInfo part : container.getMessageParts()) {
            AegisType type = getParameterType(s, serviceTM, part, partType);

            if (part.getXmlSchema() == null) {
                // schema hasn't been filled in yet
                if (type.isAbstract()) {
                    part.setTypeQName(type.getSchemaType());
                } else {
                    part.setElementQName(type.getSchemaType());
                }
            }

            Annotation[] anns = part.getProperty("parameter.annotations", Annotation[].class);

            long miValue = -1;
            if (type.hasMinOccurs()) {
                miValue = type.getMinOccurs();
            }
            Integer i = AnnotationReader.getMinOccurs(anns);
            if (i != null) {
                miValue = i;
            }
            if (miValue > 0) {
                part.setProperty("minOccurs", Long.toString(miValue));
            }


            // The concept of type.isNillable is questionable: how are types nillable?
            // However, this if at least allow .aegis.xml files to get control.
            if (part.getProperty("nillable") == null) {
                boolean isNil = type.isNillable();
                Boolean b = AnnotationReader.isNillable(anns);
                if (b != null || (miValue != 0 && isNil)) {
                    part.setProperty("nillable", b == null ? isNil : b);
                }
                /*
                if (miValue == -1 && (b == null ? isNil : b)) {
                    part.setProperty("minOccurs", "1");
                }
                */
            }
            if (type.hasMaxOccurs()) {
                String moValue;
                long mo = type.getMaxOccurs();
                if (mo != Long.MAX_VALUE) {
                    moValue = Long.toString(mo);
                    part.setProperty("maxOccurs", moValue);
                }
            }


            part2Type.put(part, type);

            // QName elName = getSuggestedName(service, op, param)
            deps.add(type);
            type.getTypeMapping().register(type);
            addDependencies(deps, type);
        }
    }

    protected void initializeMessageTypes(ServiceInfo s, AbstractMessageContainer container, int partType) {
        if (container == null) {
            return;
        }
        SchemaCollection col = s.getXmlSchemaCollection();
        for (MessagePartInfo part : container.getMessageParts()) {
            if (part.getXmlSchema() == null) {
                if (part.isElement()) {
                    XmlSchemaAnnotated tp = col.getElementByQName(part.getElementQName());
                    part.setXmlSchema(tp);
                } else {
                    XmlSchemaAnnotated tp = col.getTypeByQName(part.getTypeQName());
                    part.setXmlSchema(tp);
                }
            }
        }
    }

    private void addDependencies(Set<AegisType> deps, AegisType type) {
        Set<AegisType> typeDeps = type.getDependencies();
        if (typeDeps != null) {
            for (AegisType t : typeDeps) {
                if (!deps.contains(t)) {
                    deps.add(t);
                    addDependencies(deps, t);
                }
            }
        }
    }

    private void createSchemas(Service s, Set<AegisType> deps) {

        Map<String, Set<AegisType>> tns2Type = new HashMap<>();
        for (AegisType t : deps) {
            String ns = t.getSchemaType().getNamespaceURI();
            Set<AegisType> types = tns2Type.get(ns);
            if (types == null) {
                types = new HashSet<>();
                tns2Type.put(ns, types);
            }
            types.add(t);
        }

        for (ServiceInfo si : s.getServiceInfos()) {
            SchemaCollection col = si.getXmlSchemaCollection();
            if (col.getXmlSchemas().length > 1) {
                // someone has already filled in the types
                continue;
            }
        }

        Map<String, String> namespaceMap = getDeclaredNamespaceMappings();

        for (ServiceInfo si : s.getServiceInfos()) {
            // these two must be recalculated per-service-info!
            boolean needXmimeSchema = false;
            boolean needTypesSchema = false;

            for (Map.Entry<String, Set<AegisType>> entry : tns2Type.entrySet()) {

                String schemaNamespaceUri = entry.getKey();

                if (Constants.URI_2001_SCHEMA_XSD.equals(schemaNamespaceUri)) {
                    continue;
                }

                if (AegisContext.UTILITY_TYPES_SCHEMA_NS.equals(schemaNamespaceUri)) {
                    continue; // we handle this separately.
                }

                if (AbstractXOPType.XML_MIME_NS.equals(schemaNamespaceUri)) {
                    continue; // similiarly.
                }

                SchemaInfo schemaInfo = si.addNewSchema(entry.getKey());
                XmlSchema schema = schemaInfo.getSchema();
                NamespaceMap xmlsNamespaceMap = new NamespaceMap();

                // user-requested prefix mappings.
                if (namespaceMap != null) {
                    for (Map.Entry<String, String> e : namespaceMap.entrySet()) {
                        xmlsNamespaceMap.add(e.getValue(), e.getKey());
                    }
                }

                // tns: is conventional, and besides we have unit tests that are hardcoded to it.
                if (!xmlsNamespaceMap.containsKey(WSDLConstants.CONVENTIONAL_TNS_PREFIX)
                // if some wants something other than TNS, they get it.
                    && !xmlsNamespaceMap.containsValue(entry.getKey())) {
                    xmlsNamespaceMap.add(WSDLConstants.CONVENTIONAL_TNS_PREFIX, entry.getKey());
                }

                // ditto for xsd: instead of just namespace= for the schema schema.
                if (!xmlsNamespaceMap.containsKey("xsd")
                    && !xmlsNamespaceMap.containsValue(Constants.URI_2001_SCHEMA_XSD)) {
                    xmlsNamespaceMap.add("xsd", Constants.URI_2001_SCHEMA_XSD);
                }

                schema.setNamespaceContext(xmlsNamespaceMap);
                schema.setTargetNamespace(entry.getKey());
                schema.setElementFormDefault(XmlSchemaForm.QUALIFIED);
                schema.setAttributeFormDefault(XmlSchemaForm.QUALIFIED);

                for (AegisType t : entry.getValue()) {
                    try {
                        t.writeSchema(schema);
                    } catch (XmlSchemaException ex) {
                        QName name = t.getSchemaType();
                        String expected = " Schema for namespace '" + name.getNamespaceURI()
                                          + "' already contains type '" + name.getLocalPart() + "'";
                        String message = ex.getMessage();
                        if (expected.equals(message)) {
                            continue;
                        }
                        throw ex;
                    }
                }

                if (schemaImportsXmime(schema)) {
                    needXmimeSchema = true;
                }

                if (AegisContext.schemaImportsUtilityTypes(schema)) {
                    needTypesSchema = true;
                }
            }

            if (needXmimeSchema) {
                XmlSchema schema = aegisContext.addXmimeSchemaDocument(si.getXmlSchemaCollection()
                    .getXmlSchemaCollection());
                SchemaInfo schemaInfo = new SchemaInfo(schema.getTargetNamespace());
                schemaInfo.setSchema(schema);
                si.addSchema(schemaInfo);
            }

            if (needTypesSchema) {
                XmlSchema schema = aegisContext.addTypesSchemaDocument(si.getXmlSchemaCollection()
                    .getXmlSchemaCollection());
                SchemaInfo schemaInfo = new SchemaInfo(schema.getTargetNamespace());
                schemaInfo.setSchema(schema);
                si.addSchema(schemaInfo);
            }
            // it's quite likely that the code in Aegis missed at least one ...
            si.getXmlSchemaCollection().addCrossImports();
        }
    }

    private boolean schemaImportsXmime(XmlSchema schema) {
        return XmlSchemaUtils.schemaImportsNamespace(schema, AbstractXOPType.XML_MIME_NS);
    }

    public QName getSuggestedName(Service s, TypeMapping tm, OperationInfo op, int param) {
        Method m = getMethod(s, op);
        if (m == null) {
            return null;
        }

        QName name = tm.getTypeCreator().getElementName(m, param);

        // No mapped name was specified, so if its a complex type use that name
        // instead
        if (name == null) {
            AegisType type = tm.getTypeCreator().createType(m, param);

            if (type.isComplex() && !type.isAbstract()) {
                name = type.getSchemaType();
            }
        }

        return name;
    }

    private AegisType getParameterType(Service s, TypeMapping tm, MessagePartInfo param, int paramtype) {
        AegisType type = tm.getType(param.getTypeQName());
        if (type != null && type.getTypeClass() != param.getTypeClass()) {
            type = null;
        }

        int offset = 0;
        if (paramtype == OUT_PARAM) {
            offset = 1;
        }

        TypeCreator typeCreator = tm.getTypeCreator();
        if (type == null) {
            // Current author doesn't know how type can be non-null here.
            boolean usingComponentType = false;
            OperationInfo op = param.getMessageInfo().getOperation();

            Method m = getMethod(s, op);
            TypeClassInfo info;
            if (paramtype != FAULT_PARAM && m != null) {
                info = typeCreator.createClassInfo(m, param.getIndex() - offset);
            } else {
                info = typeCreator.createBasicClassInfo(param.getTypeClass());
            }
            Boolean nillable = info.getNillable();
            /*
             * Note that, for types from the mapping, the minOccurs, maxOccurs, and nillable from the 'info'
             * will be ignored by createTypeForClass below. So we need to override.
             */
            type = typeCreator.createTypeForClass(info);

            // if not writing outer, we don't need anything special.
            if (param.getMessageInfo().getOperation().isUnwrapped() && param.getTypeClass().isArray()
                && type.isWriteOuter()) {
                /*
                 * The service factory expects arrays going into the wrapper to be mapped to the array
                 * component type and will then add min=0/max=unbounded. That doesn't work for Aegis where we
                 * already created a wrapper ArrayType so we'll let it know we want the default.
                 */
                param.setProperty("minOccurs", "1");
                param.setProperty("maxOccurs", "1");
                if (nillable == null) {
                    nillable = Boolean.TRUE;
                }
                param.setProperty("nillable", nillable);
            } else {
                if (nillable != null) {
                    param.setProperty("nillable", nillable);
                }
                /*
                 * TypeClassInfo uses -1 to mean 'not specified'
                 */
                if (info.getMinOccurs() != -1) {
                    param.setProperty("minOccurs", Long.toString(info.getMinOccurs()));
                }
                if (info.getMaxOccurs() != -1) {
                    param.setProperty("maxOccurs", Long.toString(info.getMaxOccurs()));
                }

                if ((type instanceof ArrayType) && !type.isWriteOuter()) {
                    param.setProperty("org.apache.cxf.aegis.outerType", type);
                    ArrayType aType = (ArrayType) type;
                    type = aType.getComponentType();
                    usingComponentType = true;
                }
            }

            if (info.getMappedName() != null) {
                param.setConcreteName(info.getMappedName());
                param.setName(info.getMappedName());
            }

            if (!usingComponentType) {
                // We have to register the type if we want minOccurs and such to
                // work. (for custom types). Is this really still true with all the
                // param setting above?
                if (info.nonDefaultAttributes()) {
                    tm.register(type);
                }
                type.setTypeMapping(tm);
            }
            part2Type.put(param, type);
        }

        return type;
    }

    private Method getMethod(Service s, OperationInfo op) {
        Method m = op.getProperty(Method.class.getName(), Method.class);
        if (m != null) {
            return m;
        }
        MethodDispatcher md = (MethodDispatcher)s.get(MethodDispatcher.class.getName());
        // The ibm jdk requires the simple frontend dependency to be
        // present for the SimpleMethodDispatcher cast below even if
        // md is null (sun jdk does not).  So, for the jaxrs frontend,
        // we can exclude the simple frontend from the aegis databinding
        // dependency as long as this null check is here.
        if (md == null) {
            return null;
        }
        SimpleMethodDispatcher smd = (SimpleMethodDispatcher)md;
        return smd.getPrimaryMethod(op);
    }

    public AegisType getType(MessagePartInfo part) {
        return part2Type.get(part);
    }

    public MessagePartInfo getPartFromClass(Class<?> cls) {
        for (Map.Entry<MessagePartInfo, AegisType> entry : part2Type.entrySet()) {
            if (entry.getValue().getTypeClass() == cls) {
                return entry.getKey();
            }
        }
        return null;
    }

    public AegisType getTypeFromClass(Class<?> cls) {
        for (AegisType t : part2Type.values()) {
            if (t.getTypeClass() == cls) {
                return t;
            }
        }
        return null;
    }

    public Service getService() {
        return service;
    }

    public AegisContext getAegisContext() {
        ensureInitialized();
        return aegisContext;
    }

    public void setAegisContext(AegisContext aegisContext) {
        this.aegisContext = aegisContext;
    }

    public void setOverrideTypes(Set<String> types) {
        overrideTypes = types;
    }

    public void setConfiguration(TypeCreationOptions configuration) {
        this.configuration = configuration;
    }

    public boolean isMtomUseXmime() {
        return mtomUseXmime;
    }

    public void setMtomUseXmime(boolean mtomUseXmime) {
        this.mtomUseXmime = mtomUseXmime;
    }
}
