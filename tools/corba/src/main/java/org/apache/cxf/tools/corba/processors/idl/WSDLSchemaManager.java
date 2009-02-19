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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Port;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;
import javax.wsdl.factory.WSDLFactory;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.wsdl.AddressType;
import org.apache.cxf.binding.corba.wsdl.BindingType;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.OperationType;
import org.apache.cxf.binding.corba.wsdl.TypeMappingType;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.wsdl.JAXBExtensionHelper;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;

public class WSDLSchemaManager {

    Map<String, Definition> defns;
    Map<String, XmlSchema> schemas;

    Map<File, Definition> importedDefns;
    Map<File, XmlSchema> importedSchemas;
    Map<String, XmlSchema> defnSchemas;

    boolean ignoreImports;

    class DeferredSchemaAttachment {
        Definition defn;
        XmlSchema schema;
        boolean isGenerated;
    }
    
    List<DeferredSchemaAttachment> deferredAttachments;
    
    public WSDLSchemaManager() {
        defns = new HashMap<String, Definition>();
        schemas = new HashMap<String, XmlSchema>();
        importedDefns = new HashMap<File, Definition>();
        importedSchemas = new HashMap<File, XmlSchema>();     
        defnSchemas = new HashMap<String, XmlSchema>();
        
        deferredAttachments = new ArrayList<DeferredSchemaAttachment>();
    }

    public Definition createWSDLDefinition(String tns) throws WSDLException, JAXBException {
        WSDLFactory wsdlFactory = WSDLFactory.newInstance();
        Definition wsdlDefinition = wsdlFactory.newDefinition();        
        wsdlDefinition.setTargetNamespace(tns);
        wsdlDefinition.addNamespace("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        wsdlDefinition.addNamespace(WSDLConstants.NP_SCHEMA_XSD, WSDLConstants.NS_SCHEMA_XSD);
        wsdlDefinition.addNamespace(WSDLConstants.SOAP11_PREFIX, WSDLConstants.NS_SOAP11);
        wsdlDefinition.addNamespace("tns", tns);
        wsdlDefinition.addNamespace(CorbaConstants.NP_WSDL_CORBA, CorbaConstants.NU_WSDL_CORBA);
        addCorbaExtensions(wsdlDefinition.getExtensionRegistry());
        defns.put(tns, wsdlDefinition);
        return wsdlDefinition;
    }

    public void setIgnoreImports(boolean flag) {
        ignoreImports = flag;
    }

    public Definition getWSDLDefinition(String ns) {
        return defns.get(ns);
    }

    public XmlSchema getXmlSchema(String ns) {
        return schemas.get(ns);
    }

    public XmlSchema createXmlSchema(String schemans, XmlSchemaCollection schemaCol) {
        XmlSchema xmlSchema = new XmlSchema(schemans, schemaCol);
        schemas.put(schemans, xmlSchema);
        return xmlSchema;
    }
    
    public XmlSchema createXmlSchemaForDefinition(Definition defn,
                                                  String schemans,
                                                  XmlSchemaCollection schemaCol) {
        XmlSchema xmlSchema = createXmlSchema(schemans, schemaCol);
        defnSchemas.put(schemans, xmlSchema);
        return xmlSchema;
    }

    public boolean isXmlSchemaInDefinition(String schemans) {
        return defnSchemas.containsKey(schemans);
    }

    public void addWSDLDefinitionNamespace(Definition defn, String prefix, String ns) {
        if (!defn.getNamespaces().values().contains(ns)) {
            defn.addNamespace(prefix, ns);
        }
    }
    
    public void addWSDLDefinitionImport(Definition rootDefn,
                                        Definition defn,
                                        String prefix,
                                        String fileName) {
        if (!fileName.endsWith(".wsdl")) {
            fileName = fileName + ".wsdl";
        }
        File file = new File(fileName);
        addWSDLDefinitionImport(rootDefn, defn, prefix, file);
    }

    public void addWSDLDefinitionImport(Definition rootDefn,
                                        Definition defn,
                                        String prefix,
                                        File file) {
        if (rootDefn.getImports().get(defn.getTargetNamespace()) == null 
            && !file.getName().equals(".wsdl")) {
            // Only import if not already done to prevent multiple imports of the same file 
            // in the WSDL.  Also watch out for empty fileNames, which by this point in the
            // code would show up as ".wsdl".
            Import importDefn = rootDefn.createImport();
            if (!ignoreImports) {
                importDefn.setLocationURI(file.toURI().toString());
            }
            importDefn.setNamespaceURI(defn.getTargetNamespace());
            rootDefn.addImport(importDefn);
        }
        if (!rootDefn.getNamespaces().values().contains(defn.getTargetNamespace())) {
            rootDefn.addNamespace(prefix, defn.getTargetNamespace());
        }
        if (!importedDefns.containsKey(file)) {
            importedDefns.put(file, defn);
        }

    }

    public void addXmlSchemaImport(XmlSchema rootSchema, XmlSchema schema, String fileName) {
        // We need the file name whether already included or not.
        if (!fileName.endsWith(".xsd")) {
            fileName = fileName + ".xsd";
        }
        File file = new File(fileName);
        addXmlSchemaImport(rootSchema, schema, file);
    }

    public void addXmlSchemaImport(XmlSchema rootSchema, XmlSchema schema, File file) {
        // Make sure we haven't already imported the schema.
        String importNamespace = schema.getTargetNamespace();
        boolean included = false;
        for (Iterator i = rootSchema.getIncludes().getIterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof XmlSchemaImport) {
                XmlSchemaImport imp = (XmlSchemaImport)o;
                if (imp.getNamespace().equals(importNamespace)) {
                    included = true;
                    break;
                }
            }
        }

        if (!included) {
            XmlSchemaImport importSchema = new XmlSchemaImport();
            if (!ignoreImports) {
                importSchema.setSchemaLocation(file.toURI().toString());
            }
            importSchema.setNamespace(schema.getTargetNamespace());
            rootSchema.getItems().add(importSchema);
            rootSchema.getIncludes().add(importSchema);
        }
        if (!importedSchemas.containsKey(file)) {
            importedSchemas.put(file, schema);
        }
    }

    public void addWSDLSchemaImport(Definition def, String tns, String schemaFileName) throws Exception {
        if (!schemaFileName.endsWith(".xsd")) {
            schemaFileName = schemaFileName + ".xsd";
        }

        File file = new File(schemaFileName);
        addWSDLSchemaImport(def, tns, file);
    }

    public void addWSDLSchemaImport(Definition def, String tns, File file) throws Exception {
        //REVISIT, check if the wsdl schema already exists.
        Types types = def.getTypes();
        if (types == null) {
            types = def.createTypes();
            def.setTypes(types);            
        }
        Schema wsdlSchema = (Schema) 
            def.getExtensionRegistry().createExtension(Types.class,
                                                       new QName(Constants.URI_2001_SCHEMA_XSD,
                                                                 "schema"));

        addWSDLSchemaImport(wsdlSchema, tns, file);
        types.addExtensibilityElement(wsdlSchema);
    }

    private void addWSDLSchemaImport(Schema wsdlSchema, String tns, File file) {
        if (!wsdlSchema.getImports().containsKey(tns)) {
            SchemaImport schemaimport =  wsdlSchema.createImport();
            schemaimport.setNamespaceURI(tns);
            if (file != null
                && !ignoreImports) {
                schemaimport.setSchemaLocationURI(file.toURI().toString());
            }
            wsdlSchema.addImport(schemaimport);
        }
    }

    private void addCorbaExtensions(ExtensionRegistry extReg) throws JAXBException {
        try {                      
            JAXBExtensionHelper.addExtensions(extReg, Binding.class, BindingType.class);
            JAXBExtensionHelper.addExtensions(extReg, BindingOperation.class, OperationType.class);
            JAXBExtensionHelper.addExtensions(extReg, Definition.class, TypeMappingType.class);
            JAXBExtensionHelper.addExtensions(extReg, Port.class, AddressType.class);

            extReg.mapExtensionTypes(Binding.class, CorbaConstants.NE_CORBA_BINDING, BindingType.class);
            extReg.mapExtensionTypes(BindingOperation.class, CorbaConstants.NE_CORBA_OPERATION,
                                     org.apache.cxf.binding.corba.wsdl.OperationType.class);
            extReg.mapExtensionTypes(Definition.class, CorbaConstants.NE_CORBA_TYPEMAPPING,
                                     TypeMappingType.class);
            extReg.mapExtensionTypes(Port.class, CorbaConstants.NE_CORBA_ADDRESS,
                                     org.apache.cxf.binding.corba.wsdl.AddressType.class);
        } catch (javax.xml.bind.JAXBException ex) {
            throw new JAXBException(ex.getMessage());
        }
    }
    
    public void deferAttachSchemaToWSDL(Definition definition, XmlSchema schema, 
                                        boolean isSchemaGenerated) throws Exception {
        DeferredSchemaAttachment attachment = new DeferredSchemaAttachment();
        attachment.defn = definition;
        attachment.schema = schema;
        attachment.isGenerated = isSchemaGenerated;
        deferredAttachments.add(attachment);
    }
    
    public void attachDeferredSchemasToWSDL() throws Exception {
        for (Iterator<DeferredSchemaAttachment> iter = deferredAttachments.iterator(); iter.hasNext();) {
            DeferredSchemaAttachment attachment = iter.next();
            attachSchemaToWSDL(attachment.defn, attachment.schema, attachment.isGenerated);
        }
    }

    public void attachSchemaToWSDL(Definition definition, XmlSchema schema, boolean isSchemaGenerated)
        throws Exception {
        Types types = definition.getTypes();
        if (types == null) {
            types = definition.createTypes();
            definition.setTypes(types);
        }
        Schema wsdlSchema = (Schema) 
            definition.getExtensionRegistry().createExtension(Types.class,
                                                              new QName(Constants.URI_2001_SCHEMA_XSD,
                                                                        "schema"));

        // See if a NamespaceMap has already been added to the schema (this can be the case with object 
        // references.  If so, simply add the XSD URI to the map.  Otherwise, create a new one.
        NamespaceMap nsMap = null;
        try {
            nsMap = (NamespaceMap)schema.getNamespaceContext();
        } catch (ClassCastException ex) {
            // Consume.  This will mean that the context has not been set.
        }
        if (nsMap == null) {
            nsMap = new NamespaceMap();
            nsMap.add("xs", Constants.URI_2001_SCHEMA_XSD);
            schema.setNamespaceContext(nsMap);
        } else {
            nsMap.add("xs", Constants.URI_2001_SCHEMA_XSD);
        }
        if (isSchemaGenerated) {
            nsMap.add("tns", schema.getTargetNamespace());
        }
        org.w3c.dom.Element el = schema.getAllSchemas()[0].getDocumentElement();
        wsdlSchema.setElement(el);
        
        XmlSchemaObjectCollection imports = schema.getIncludes();
        for (java.util.Iterator<XmlSchemaObject> it = CastUtils.cast(imports.getIterator()); it.hasNext();) {
            XmlSchemaImport xmlSchemaImport = (XmlSchemaImport) it.next();
            SchemaImport schemaimport =  wsdlSchema.createImport();
            schemaimport.setNamespaceURI(xmlSchemaImport.getNamespace());
            if (xmlSchemaImport.getSchemaLocation() != null
                && !ignoreImports) {
                schemaimport.setSchemaLocationURI(xmlSchemaImport.getSchemaLocation());
            }
            wsdlSchema.addImport(schemaimport);  
        }
        types.addExtensibilityElement(wsdlSchema);
    }

    public TypeMappingType createCorbaTypeMap(Definition definition, String corbatypemaptns)
        throws WSDLException { 
        TypeMappingType typeMap = (TypeMappingType)
            definition.getExtensionRegistry().createExtension(Definition.class,
                                                              CorbaConstants.NE_CORBA_TYPEMAPPING);
        if (corbatypemaptns == null) {
            typeMap.setTargetNamespace(definition.getTargetNamespace()
                + "/"
                + CorbaConstants.NS_CORBA_TYPEMAP);
        } else {
            typeMap.setTargetNamespace(corbatypemaptns);
        }
        definition.addExtensibilityElement(typeMap);
        return typeMap;
    }

    public Map<String, Definition> getWSDLDefinitions() {
        return defns;
    }

    public Map<String, XmlSchema> getXmlSchemas() {
        return schemas;
    }

    public Map<File, Definition> getImportedWSDLDefinitions() {
        return importedDefns;
    }

    public Map<File, XmlSchema> getImportedXmlSchemas() {
        return importedSchemas;
    }

    public File getImportedWSDLDefinitionFile(String ns) {
        for (Iterator<File> it = importedDefns.keySet().iterator(); it.hasNext();) {
            File file = it.next();
            Definition defn = importedDefns.get(file);
            if (defn.getTargetNamespace().equals(ns)) {
                return file;
            }
        }
        return null;
    }

    public File getImportedXmlSchemaFile(String ns) {
        for (Iterator<File> it = importedSchemas.keySet().iterator(); it.hasNext();) {
            File file = it.next();
            XmlSchema schema = importedSchemas.get(file);
            if (schema.getTargetNamespace().equals(ns)) {
                return file;
            }
        }
        return null;
    }
}
