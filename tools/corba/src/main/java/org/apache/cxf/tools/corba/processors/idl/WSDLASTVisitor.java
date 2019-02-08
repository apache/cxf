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

package org.apache.cxf.tools.corba.processors.idl;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Message;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import antlr.ASTVisitor;
import antlr.collections.AST;

import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.TypeMappingType;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.corba.common.ToolCorbaConstants;
import org.apache.cxf.tools.corba.common.WSDLUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;

public final class WSDLASTVisitor implements ASTVisitor {

    Definition definition;
    XmlSchema schema;
    XmlSchemaCollection schemas;

    TypeMappingType typeMap;
    ScopeNameCollection scopedNames;
    ScopeNameCollection recursionList;
    DeferredActionCollection deferredActions;
    String targetNamespace;
    private boolean declaredWSAImport;
    private boolean supportPolymorphicFactories;

    private XmlSchemaType sequenceOctetType;
    private boolean boundedStringOverride;
    private String idlFile;
    private String outputDir;
    private String importSchemaFilename;
    private boolean schemaGenerated;
    private ModuleToNSMapper moduleToNSMapper;
    private WSDLSchemaManager manager;
    private Map<Scope, List<Scope>> inheritScopeMap;
    private String pragmaPrefix;

    public WSDLASTVisitor(String tns, String schemans, String corbatypemaptns, String pragmaPrefix)
        throws WSDLException, JAXBException {

        manager = new WSDLSchemaManager();

        definition = manager.createWSDLDefinition(tns);

        inheritScopeMap = new TreeMap<>();

        targetNamespace = tns;
        schemas = new XmlSchemaCollection();
        scopedNames = new ScopeNameCollection();
        deferredActions = new DeferredActionCollection();

        if (schemans == null) {
            schemans = tns;
        }
        schema = manager.createXmlSchemaForDefinition(definition, schemans, schemas);
        declaredWSAImport = false;

        typeMap = manager.createCorbaTypeMap(definition, corbatypemaptns);

        // idl:sequence<octet> maps to xsd:base64Binary by default
        sequenceOctetType = schemas.getTypeByQName(Constants.XSD_BASE64);

        // treat bounded corba:string/corba:wstring as unbounded if set to true
        setBoundedStringOverride(false);

        moduleToNSMapper = new ModuleToNSMapper();
        this.setPragmaPrefix(pragmaPrefix);
    }

    public WSDLASTVisitor(String tns, String schemans, String corbatypemaptns)
        throws WSDLException, JAXBException {
        this(tns, schemans, corbatypemaptns, null);
    }

    public void visit(AST node) {
        // <specification> ::= <definition>+

        while (node != null) {
            Scope rootScope = new Scope();
            DefinitionVisitor definitionVisitor = new DefinitionVisitor(rootScope,
                                                                        definition,
                                                                        schema,
                                                                        this);
            definitionVisitor.visit(node);
            node = node.getNextSibling();
        }

        try {
            manager.attachSchemaToWSDL(definition, schema, isSchemaGenerated());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void setSchemaGenerated(boolean value) {
        schemaGenerated = value;
    }

    public boolean isSchemaGenerated() {
        return schemaGenerated;
    }

    public void updateSchemaNamespace(String name) throws Exception  {
        schema.setTargetNamespace(name);
    }

    public void setQualified(boolean qualified) throws Exception  {
        if (qualified) {
            XmlSchemaForm form = XmlSchemaForm.QUALIFIED;
            schema.setAttributeFormDefault(form);
            schema.setElementFormDefault(form);
        }
    }

    public void setSupportPolymorphicFactories(boolean support) throws Exception  {
        supportPolymorphicFactories = support;
    }

    public boolean getSupportPolymorphicFactories() {
        return supportPolymorphicFactories;
    }

    public void setIdlFile(String idl) {
        idlFile = idl;
    }

    public String getIdlFile() {
        return idlFile;
    }

    public Map<Scope, List<Scope>> getInheritedScopeMap() {
        return inheritScopeMap;
    }

    public void setOutputDir(String outDir) {
        outputDir = outDir;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public Definition getDefinition() {
        return definition;
    }

    public WSDLSchemaManager getManager() {
        return manager;
    }

    public XmlSchema getSchema() {
        return schema;
    }

    public XmlSchemaCollection getSchemas() {
        return schemas;
    }

    public ScopeNameCollection getScopedNames() {
        return scopedNames;
    }

    public ScopeNameCollection getRecursionList() {
        return recursionList;
    }

    public DeferredActionCollection getDeferredActions() {
        return deferredActions;
    }

    public TypeMappingType getTypeMap() {
        return typeMap;
    }

    public XmlSchemaType getSequenceOctetType() {
        return sequenceOctetType;
    }

    public void setImportSchema(String filename) {
        importSchemaFilename = filename;
    }

    public String getImportSchemaFilename() {
        return importSchemaFilename;
    }

    public void setSequenceOctetType(String type) throws Exception {
        XmlSchemaType stype = null;
        if (type.equals(ToolCorbaConstants.CFG_SEQUENCE_OCTET_TYPE_BASE64BINARY)) {
            stype = schemas.getTypeByQName(Constants.XSD_BASE64);
        } else if (type.equals(ToolCorbaConstants.CFG_SEQUENCE_OCTET_TYPE_HEXBINARY)) {
            stype = schemas.getTypeByQName(Constants.XSD_HEXBIN);
        } else {
            throw new ToolException("WSDLASTVisitor: Invalid XmlSchemaType specified "
                                    + "for idl:sequence<octet> mapping.");
        }
        sequenceOctetType = stype;
    }

    public boolean getBoundedStringOverride() {
        return boundedStringOverride;
    }

    public void setBoundedStringOverride(boolean value) {
        boundedStringOverride = value;
    }

    public Binding[] getCorbaBindings() {
        List<Binding> result = new ArrayList<>();
        Map<QName, Binding> bindings = CastUtils.cast(definition.getBindings());
        for (Binding binding : bindings.values()) {
            List<ExtensibilityElement> extElements
                = CastUtils.cast(binding.getExtensibilityElements());
            for (int i = 0; i < extElements.size(); i++) {
                ExtensibilityElement el = extElements.get(i);
                if (el.getElementType().equals(CorbaConstants.NE_CORBA_BINDING)) {
                    result.add(binding);
                    break;
                }
            }
        }
        return result.toArray(new Binding[0]);
    }

    public boolean writeDefinition(Writer writer) throws Exception {
        writeDefinition(definition, writer);
        return true;
    }

    public boolean writeDefinition(Definition def, Writer writer) throws Exception {
        WSDLUtils.writeWSDL(def, writer);
        return true;
    }

    public boolean writeSchemaDefinition(Definition definit, Writer writer) throws Exception  {
        Definition def = manager.createWSDLDefinition(targetNamespace + "-types");
        def.createTypes();
        def.setTypes(definit.getTypes());
        WSDLUtils.writeSchema(def, writer);
        return true;
    }

    public boolean writeSchema(XmlSchema schemaRef, Writer writer) throws Exception  {
        //REVISIT, it should be easier to  write out the schema directly, but currently,
        //the XmlSchemaSerializer throws a NullPointerException, when setting up namespaces!!!
        //schemaRef.write(writer);
        Definition defn = manager.createWSDLDefinition(schemaRef.getTargetNamespace());
        manager.attachSchemaToWSDL(defn, schemaRef, true);
        writeSchemaDefinition(defn, writer);
        return true;
    }

    // REVISIT - When CXF corrects the wsdlValidator - will switch back on the
    // validation of the generated wsdls.
    public boolean writeDefinitions(Writer writer, Writer schemaWriter,
                                    Writer logicalWriter, Writer physicalWriter,
                                    String schemaFilename, String logicalFile,
                                    String physicalFile) throws Exception {

        Definition logicalDef = getLogicalDefinition(schemaFilename, schemaWriter);
        Definition physicalDef = null;
        // schema only
        if ((schemaFilename != null || importSchemaFilename != null)
            && (logicalFile == null && physicalFile == null)) {
            physicalDef = getPhysicalDefinition(logicalDef, true);
        } else {
            physicalDef = getPhysicalDefinition(logicalDef, false);
        }

        // write out logical file -L and physical in default
        if (logicalFile != null && physicalFile == null) {
            writeDefinition(logicalDef, logicalWriter);
            manager.addWSDLDefinitionImport(physicalDef,
                                            logicalDef,
                                            "logicaltns",
                                            logicalFile);
            writeDefinition(physicalDef, writer);
        } else if (logicalFile != null && physicalFile != null) {
            // write both logical -L and physical files -P
            writeDefinition(logicalDef, logicalWriter);
            manager.addWSDLDefinitionImport(physicalDef,
                                            logicalDef,
                                            "logicaltns",
                                            logicalFile);
            writeDefinition(physicalDef, physicalWriter);
        } else if (logicalFile == null && physicalFile != null) {
            // write pyhsical file -P and logical in default
            writeDefinition(logicalDef, writer);
            manager.addWSDLDefinitionImport(physicalDef,
                                            logicalDef,
                                            "logicaltns",
                                            getIdlFile());
            writeDefinition(physicalDef, physicalWriter);
        } else if ((logicalFile == null && physicalFile == null)
            && (schemaFilename != null || importSchemaFilename != null)) {
            // write out the schema file -T and default of logical
            // and physical together.
            writeDefinition(physicalDef, writer);
        } else if (logicalFile == null && physicalFile == null
            && schemaFilename == null) {
            writeDefinition(definition, writer);
        }

        return true;
    }

    // Gets the logical definition for a file - an import will be added for the
    // schema types if -T is used and a separate schema file generated.
    // if -n is used an import will be added for the schema types and no types generated.
    private Definition getLogicalDefinition(String schemaFilename, Writer schemaWriter)
        throws WSDLException, JAXBException, Exception {
        Definition def = manager.createWSDLDefinition(targetNamespace);

        // checks for -T option.
        if (schemaFilename != null) {
            writeSchemaDefinition(definition, schemaWriter);
            manager.addWSDLSchemaImport(def, schema.getTargetNamespace(), schemaFilename);
        } else {
            // checks for -n option
            if (importSchemaFilename == null) {
                Types types = definition.getTypes();
                def.setTypes(types);
            } else {
                manager.addWSDLSchemaImport(def, schema.getTargetNamespace(), importSchemaFilename);
            }
        }

        Collection<PortType> portTypes = CastUtils.cast(definition.getAllPortTypes().values());
        for (PortType port : portTypes) {
            def.addPortType(port);
        }

        Collection<Message> messages = CastUtils.cast(definition.getMessages().values());
        for (Message msg : messages) {
            def.addMessage(msg);
        }

        Collection<String> namespaces = CastUtils.cast(definition.getNamespaces().values());
        for (String namespace : namespaces) {
            String prefix = definition.getPrefix(namespace);
            if (!"corba".equals(prefix)) {
                def.addNamespace(prefix, namespace);
            } else {
                def.removeNamespace(prefix);
            }
        }

        Collection<Import> imports = CastUtils.cast(definition.getImports().values());
        for (Import importType : imports) {
            def.addImport(importType);
        }

        def.setDocumentationElement(definition.getDocumentationElement());
        def.setDocumentBaseURI(definition.getDocumentBaseURI());

        return def;
    }

    // Write the physical definitions to a file.
    private Definition getPhysicalDefinition(Definition logicalDef, boolean schemaOnly)
        throws WSDLException, JAXBException {

        Definition def = null;
        if (schemaOnly) {
            def = logicalDef;
        } else {
            def = manager.createWSDLDefinition(targetNamespace);
        }

        Collection<String> namespaces = CastUtils.cast(definition.getNamespaces().values());
        for (String namespace : namespaces) {
            String prefix = definition.getPrefix(namespace);
            def.addNamespace(prefix, namespace);
        }
        Collection<Binding> bindings = CastUtils.cast(definition.getAllBindings().values());
        for (Binding binding : bindings) {
            def.addBinding(binding);
        }
        Collection<Service> services = CastUtils.cast(definition.getAllServices().values());
        for (Service service : services) {
            def.addService(service);
        }
        Collection<ExtensibilityElement> extns = CastUtils.cast(definition.getExtensibilityElements());
        for (ExtensibilityElement ext : extns) {
            def.addExtensibilityElement(ext);
        }

        def.setExtensionRegistry(definition.getExtensionRegistry());

        return def;
    }

    public boolean getDeclaredWSAImport() {
        return declaredWSAImport;
    }

    public void setDeclaredWSAImport(boolean declaredImport) {
        declaredWSAImport = declaredImport;
    }

    public void setModuleToNSMapping(Map<String, String> map) {
        moduleToNSMapper.setDefaultMapping(false);
        moduleToNSMapper.setUserMapping(map);
    }

    public ModuleToNSMapper getModuleToNSMapper() {
        return moduleToNSMapper;
    }

    public void setExcludedModules(Map<String, List<String>> modules) {
        moduleToNSMapper.setExcludedModuleMap(modules);
    }

    public void setPragmaPrefix(String pragmaPrefix) {
        this.pragmaPrefix = pragmaPrefix;
    }

    public String getPragmaPrefix() {
        return pragmaPrefix;
    }

}
