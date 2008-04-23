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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;
import javax.xml.namespace.QName;

import antlr.collections.AST;

import org.apache.cxf.binding.corba.wsdl.AddressType;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.binding.corba.wsdl.TypeMappingType;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.corba.common.ReferenceConstants;
import org.apache.cxf.tools.corba.common.ToolCorbaConstants;
import org.apache.cxf.tools.util.FileWriterUtil;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.constants.Constants;

public class IDLToWSDLProcessor extends IDLProcessor {

    private String idl;
    private String schemaFilename;
    private String importSchemaFilename;
    private String logical;
    private String physical;
    private String outputDir;
    private Writer outputWriter;
    private Writer schemaOutputWriter;   
    private Writer logicalOutputWriter;
    private Writer physicalOutputWriter;
    private Map<String, File> importDefnWriters;
    private Map<String, File> importSchemaWriters;
    private boolean ignoreImports;
    
    public void process() throws ToolException {
        super.process();
        process(getIDLTree());
    }

    public void process(AST idlTree) throws ToolException {
        idl = getBaseFilename(env.get(ToolCorbaConstants.CFG_IDLFILE).toString());
        checkFileOptions();
        try {
            parseIDL(idlTree);
        } catch (Exception e) {
            throw new ToolException(e);
        }
    }

    public void setOutputWriter(Writer writer) {
        outputWriter = writer;
    }
    
    public void setSchemaOutputWriter(Writer writer) {
        schemaOutputWriter = writer;
    }
    
    public void setLogicalOutputWriter(Writer writer) {
        logicalOutputWriter = writer;
    }
    
    public void setPhysicalOutputWriter(Writer writer) {
        physicalOutputWriter = writer;
    }

    /**
     * Used only for test cases to set writers for imports when using
     * the -mns option
     */
    protected void setImportDefinitionWriters(Map<String, File> writers) {
        importDefnWriters = writers;
    }

    protected void setImportSchemaWriters(Map<String, File> writers) {
        importSchemaWriters = writers;
    }

    protected void setIgnoreImports(boolean flag) {
        ignoreImports = flag;
    }
    
    private void checkFileOptions() {
                
        if (env.optionSet(ToolCorbaConstants.CFG_LOGICAL)) {
            // set the logical filename 
            logical = env.get(ToolCorbaConstants.CFG_LOGICAL).toString();        
        }
        if (env.optionSet(ToolCorbaConstants.CFG_PHYSICAL)) {
            // set the physical file name
            physical = env.get(ToolCorbaConstants.CFG_PHYSICAL).toString();            
        }
        if (env.optionSet(ToolCorbaConstants.CFG_SCHEMA)) {
            // deal with writing schema types to the schema specified file
            schemaFilename = env.get(ToolCorbaConstants.CFG_SCHEMA).toString();
        }
        if (env.optionSet(ToolCorbaConstants.CFG_IMPORTSCHEMA)) {
            // deal with importing schema types 
            importSchemaFilename = env.get(ToolCorbaConstants.CFG_IMPORTSCHEMA).toString();
        }                
    }    

    public void parseIDL(AST idlTree) throws Exception {
        if (env.isVerbose()) {
            System.out.println(idlTree.toStringTree());
        }               

        // target namespace
        String tns = (String) env.get(ToolCorbaConstants.CFG_TNS);
        if (tns == null) {
            tns = CorbaConstants.WSDL_NS_URI + idl;
        }
        // XmlSchema namespace
        String schemans = (String) env.get(ToolCorbaConstants.CFG_SCHEMA_NAMESPACE);
        
        // corba typemap namespace
        String corbatypemaptns = (String) env.get(ToolCorbaConstants.CFG_CORBATYPEMAP_NAMESPACE);
        
        outputDir = ".";

        try {
            WSDLASTVisitor visitor = new WSDLASTVisitor(tns, schemans, corbatypemaptns);
            visitor.getManager().setIgnoreImports(ignoreImports);
            if (env.optionSet(ToolConstants.CFG_OUTPUTDIR)) {
                outputDir =  (String) env.get(ToolConstants.CFG_OUTPUTDIR);
            }
            visitor.setOutputDir(outputDir); 
            Definition def = visitor.getDefinition();
            if (env.optionSet(ToolCorbaConstants.CFG_SEQUENCE_OCTET_TYPE)) {
                visitor.setSequenceOctetType((String) env.get(ToolCorbaConstants.CFG_SEQUENCE_OCTET_TYPE));
            }
            if (env.optionSet(ToolCorbaConstants.CFG_SCHEMA_NAMESPACE)) {
                //visitor.getDefinition()
                def.addNamespace(ToolCorbaConstants.CFG_SCHEMA_NAMESPACE_PREFIX,
                                  (String) env.get(ToolCorbaConstants.CFG_SCHEMA_NAMESPACE));
            }
            if (env.optionSet(ToolCorbaConstants.CFG_BOUNDEDSTRINGS)) {
                visitor.setBoundedStringOverride(true);
            }
            
            if (env.optionSet(ToolCorbaConstants.CFG_MODULETONS)) {
                String mapping = (String) env.get(ToolCorbaConstants.CFG_MODULETONS);
                //parse the mapping & set a map of module to namespace mapping in the visitor
                visitor.setModuleToNSMapping(getModuleToNSMapping(mapping));
            }
            
            if (env.optionSet(ToolCorbaConstants.CFG_QUALIFIED)) {
                visitor.setQualified(true);
            }
            
            if (env.optionSet(ToolCorbaConstants.CFG_POLYMORPHIC_FACTORIES)) {
                visitor.setSupportPolymorphicFactories(true);
            }

            if (env.optionSet(ToolCorbaConstants.CFG_SCHEMA)) {
                visitor.setSchemaGenerated(true);
                // generate default namespace for schema if -T is used alone.
                if (env.get(ToolCorbaConstants.CFG_SCHEMA_NAMESPACE) == null) {
                    visitor.updateSchemaNamespace(def.getTargetNamespace() + "-types");
                    def.addNamespace(ToolCorbaConstants.CFG_SCHEMA_NAMESPACE_PREFIX, def.getTargetNamespace()
                                                                                     + "-types");
                }
            }

            if (env.optionSet(ToolCorbaConstants.CFG_EXCLUDEMODULES)) {
                String modules = (String) env.get(ToolCorbaConstants.CFG_EXCLUDEMODULES);
                //parse the mapping & set a map of module to namespace mapping in the visitor
                visitor.setExcludedModules(getExcludedModules(modules));
            }
            visitor.visit(idlTree);  
            
            cleanUpTypeMap(visitor.getTypeMap());
            
            Binding[] bindings = visitor.getCorbaBindings();
            generateCORBAService(def, bindings, visitor.getModuleToNSMapper().isDefaultMapping());
            writeDefinitions(visitor);           
        } catch (Exception ex) {           
            throw new ToolException(ex.getMessage(), ex);
        }
    }
    
    // Sets the output directory and the generated filenames.
    // Output directory is specified 
    //     - File names have no path specified
    //     - File names do have specified so they take precedence.
    // Output directory is not specified
    //     - File names have no path specified so use current directory.
    //     - File names have full path specified.
    private void writeDefinitions(WSDLASTVisitor visitor) 
        throws Exception {                              
        if (env.optionSet(ToolCorbaConstants.CFG_LOGICAL)
            || env.optionSet(ToolCorbaConstants.CFG_PHYSICAL)
            || env.optionSet(ToolCorbaConstants.CFG_SCHEMA)
            || env.optionSet(ToolCorbaConstants.CFG_IMPORTSCHEMA)) {
                        
            if (logical == null || physical == null) {
                if (outputWriter == null) {
                    outputWriter = getOutputWriter(idl + ".wsdl", outputDir);
                }
                String separator = System.getProperty("file.separator");
                File file = null;
                if (env.get(ToolConstants.CFG_OUTPUTDIR) != null) {
                    file = new File(outputDir + separator + idl + ".wsdl");                        
                } else {
                    file = new File(idl + ".wsdl");                        
                }   
                visitor.setIdlFile(file.getAbsolutePath());                                    
            }            
            
            if (logical != null) {                
                logical = getFilePath(logical).getAbsolutePath();
                if  (logicalOutputWriter == null) {
                    logicalOutputWriter = createOutputWriter(logical);                    
                }                                  
            }
                            
            if (physical != null) {               
                physical = getFilePath(physical).getAbsolutePath();
                if (physicalOutputWriter == null) {            
                    physicalOutputWriter = createOutputWriter(physical); 
                }                                
            }            
            
            if (schemaFilename != null) {                
                schemaFilename = getFilePath(schemaFilename).getAbsolutePath();
                if (schemaOutputWriter == null) {            
                    schemaOutputWriter = createOutputWriter(schemaFilename); 
                }    
            }
                        
            if (importSchemaFilename != null) {
                importSchemaFilename = getImportFile(importSchemaFilename);               
                visitor.setImportSchema(importSchemaFilename);
            }                     
                        
            visitor.writeDefinitions(outputWriter, schemaOutputWriter,
                                     logicalOutputWriter, physicalOutputWriter, 
                                     schemaFilename, logical, physical);
        } else {
            if (outputWriter == null) {
                String outputFile = idl + ".wsdl";
                if (env.optionSet(ToolCorbaConstants.CFG_WSDLOUTPUTFILE)) {
                    outputFile = (String) env.get(ToolCorbaConstants.CFG_WSDLOUTPUTFILE);
                    if (!outputFile.endsWith(".wsdl")) {
                        outputFile = outputFile + ".wsdl";
                    }
                }
                outputWriter = getOutputWriter(outputFile, outputDir); 
            }
            Definition defn = visitor.getDefinition();
            if (!visitor.getModuleToNSMapper().isDefaultMapping()) {
                addTypeMapSchemaImports(defn, visitor);
                visitor.getManager().attachDeferredSchemasToWSDL();
            }
            
            visitor.writeDefinition(defn, outputWriter);
            writeImportedDefinitionsAndSchemas(visitor);
        }
    }

    private void writeImportedDefinitionsAndSchemas(WSDLASTVisitor visitor)
        throws Exception {
        Map<File, Definition> defns = visitor.getManager().getImportedWSDLDefinitions();
        Map<File, XmlSchema> schemas = visitor.getManager().getImportedXmlSchemas();

        if (importDefnWriters != null) {
            assert importDefnWriters.size() == defns.size();
        }
        if (importSchemaWriters != null) {
            assert importSchemaWriters.size() == schemas.size();
        }
        
        for (java.util.Iterator<File> it = defns.keySet().iterator(); it.hasNext();) {
            File file = it.next();
            Definition defn = defns.get(file);
            Writer writer = null;
            if (importDefnWriters != null) {
                writer = getOutputWriter(importDefnWriters.get(defn.getTargetNamespace()));
            }
            if (writer == null) {
                writer = getOutputWriter(file);
            }
            visitor.writeDefinition(defn, writer);
            writer.close();
        }
        for (java.util.Iterator<File> it = schemas.keySet().iterator(); it.hasNext();) {
            File file = it.next();
            XmlSchema schema = schemas.get(file);
            Writer writer = null;
            if (importSchemaWriters != null) {
                writer = getOutputWriter(importSchemaWriters.get(schema.getTargetNamespace()));
            }
            if (writer == null) {
                writer = getOutputWriter(file);
            }
            visitor.writeSchema(schema, writer);
            writer.close();
        }
    }
    
    // Get the imported schema file.
    private String getImportFile(String importFilename) {
        // check that file exists        
        File file = new File(importFilename);                        
        
        if (!file.exists()) {            
            if (!file.isAbsolute()) {
                String separator = System.getProperty("file.separator");
                String userdir = System.getProperty("user.dir");                
                file = new File(userdir + separator + importFilename);
            }
            if (!file.exists()) {
                String msg = importFilename + " File not found";
                FileNotFoundException ex = new FileNotFoundException(msg);
                System.err.println("IDLToWsdl Error : " + ex.getMessage());
                System.err.println();            
                ex.printStackTrace();            
                System.exit(1);
            } else {
                URI url = file.toURI();
                return url.toString();
                
            }            
        } else {
            URI url = file.toURI();
            return url.toString();
        }
        return null;
    }
    
    private Writer createOutputWriter(String name) throws Exception {        
        String outDir = outputDir;
        String filename = name;               
        int index = name.lastIndexOf(System.getProperty("file.separator"));
        outDir = name.substring(0, index);
        filename = name.substring(index + 1, name.length());                        
        return getOutputWriter(filename, outDir);        
    }
    
    // Gets the fully qualified path of a file.
    private File getFilePath(String ifile) {        
        String separator = System.getProperty("file.separator");
        StringTokenizer token = new StringTokenizer(ifile, separator);        

        if (token.countTokens() == 1) {
            if (env.get(ToolConstants.CFG_OUTPUTDIR) != null) {
                return new File(outputDir + separator + ifile);
            } else {
                return new File(ifile);
            }
        } else {
            return new File(ifile);
        }                           
    }           
    
    public Writer getOutputWriter(String filename, String outputDirectory) throws Exception {

        
        if (env.optionSet(ToolCorbaConstants.CFG_WSDL_ENCODING)) { 
            String encoding = env.get(ToolCorbaConstants.CFG_WSDL_ENCODING).toString();            
            return FileWriterUtil.getWriter(new File(outputDirectory, filename), encoding); 
        } else {
            FileWriterUtil fw = new FileWriterUtil(outputDirectory);        
            return fw.getWriter("", filename); 
        }       
    }

    public Writer getOutputWriter(File file) throws Exception {        
        if (env.optionSet(ToolCorbaConstants.CFG_WSDL_ENCODING)) { 
            String encoding = env.get(ToolCorbaConstants.CFG_WSDL_ENCODING).toString();            
            return FileWriterUtil.getWriter(file, encoding); 
        } else {
            return FileWriterUtil.getWriter(file);
        }       
    }    

    public String getBaseFilename(String ifile) {
        String fileName = ifile;
        StringTokenizer token = new StringTokenizer(ifile, "\\/");

        while (token.hasMoreTokens()) {
            fileName = token.nextToken();
        }
        if (fileName.endsWith(".idl")) {
            fileName = new String(fileName.substring(0, fileName.length() - 4));
        }
        return fileName;
    }

    private Map<String, String> getServiceNames(Binding[] bindings, boolean isDefaultMapping) {
        Map<String, String> serviceNames = new HashMap<String, String>();
        for (int i = 0; i < bindings.length; i++) {
            QName portTypeName = bindings[i].getPortType().getQName();
            String ns = portTypeName.getNamespaceURI();
            if (!isDefaultMapping && !serviceNames.containsKey(ns)) {
                String[] bindingTokens = bindings[i].getQName().getLocalPart().split("\\.");
                if (bindingTokens.length > 1) {
                    String name = "";
                    for (int j = 0; j < bindingTokens.length - 2; j++) {
                        name += bindingTokens[j] + ".";
                    }
                    name += bindingTokens[bindingTokens.length - 2] + "CORBAService";
                    serviceNames.put(ns, name);
                } else {
                    serviceNames.put(ns, idl + "CORBAService");
                }
            }
        }
        return serviceNames;
    }

    public void generateCORBAService(Definition def,
                                     Binding[] bindings,
                                     boolean isDefaultMapping)
        throws Exception {
        Map<String, Service> serviceMap = new HashMap<String, Service>();
        Map<String, String> serviceNames = getServiceNames(bindings, isDefaultMapping);
        for (int i = 0; i < bindings.length; i++) {
            QName portTypeName = bindings[i].getPortType().getQName();
            Service service;
            if (isDefaultMapping) {
                service = def.createService();
                service.setQName(new QName(def.getTargetNamespace(),
                                           portTypeName.getLocalPart() + "CORBAService"));
                def.addService(service);
            } else {
                String ns = portTypeName.getNamespaceURI();
                String serviceName = serviceNames.get(ns);
                service = serviceMap.get(ns);
                if (service == null) {
                    service = def.createService();
                    serviceMap.put(ns, service);
                    String[] serviceTokens = serviceName.split("\\.");
                    String serviceToken = serviceTokens[serviceTokens.length - 1];
                    QName serviceQName = new QName(def.getTargetNamespace(), serviceToken);
                    Service existingService = def.getService(serviceQName);
                    if (existingService != null) {
                        String existingServiceNS =
                            ((Port) existingService.getPorts().values().iterator().next())
                            .getBinding().getPortType().getQName().getNamespaceURI();
                        existingService.setQName(new QName(def.getTargetNamespace(),
                                                           serviceNames.get(existingServiceNS)));
                        serviceMap.put(existingServiceNS, existingService);
                        service.setQName(new QName(def.getTargetNamespace(),
                                                   serviceName));
                    } else {
                        service.setQName(serviceQName);
                    }
                    def.addService(service);
                }
            }
            Port port = def.createPort();
            port.setName(portTypeName.getLocalPart() + "CORBAPort");
            AddressType address =
                (AddressType) def.getExtensionRegistry().createExtension(Port.class,
                                                                         CorbaConstants.NE_CORBA_ADDRESS);
            
            String addr = null;
            String addrFileName = (String) env.get(ToolCorbaConstants.CFG_ADDRESSFILE); 
            if (addrFileName != null) {
                try {
                    File addrFile = new File(addrFileName);
                    FileReader fileReader = new FileReader(addrFile);
                    BufferedReader bufferedReader = new BufferedReader(fileReader);
                    addr = bufferedReader.readLine();
                } catch (Exception ex) {
                    throw new ToolException(ex.getMessage(), ex);
                }
            } else {
                addr = (String) env.get(ToolCorbaConstants.CFG_ADDRESS);
            }
            if (addr == null) {
                addr = "IOR:";
            }
            address.setLocation(addr);
            port.addExtensibilityElement(address);
            service.addPort(port);
            port.setBinding(bindings[i]);
        }
    }

    public void cleanUpTypeMap(TypeMappingType typeMap) {
        List<CorbaTypeImpl> types = typeMap.getStructOrExceptionOrUnion();
        if (types != null) {
            for (int i = 0; i < types.size(); i++) {
                CorbaTypeImpl type = types.get(i);
                if (type.getQName() != null) {
                    type.setName(type.getQName().getLocalPart());
                    type.setQName(null);
                }
            }
        }
    }
    
    public void addTypeMapSchemaImports(Definition def, WSDLASTVisitor visitor) {
        List<CorbaTypeImpl> types = visitor.getTypeMap().getStructOrExceptionOrUnion();
        ModuleToNSMapper mapper = visitor.getModuleToNSMapper();
        WSDLSchemaManager manager = visitor.getManager();
        Collection namespaces = def.getNamespaces().values();
        Set<Map.Entry<String, String>> userModuleMappings = mapper.getUserMapping().entrySet();
        
        if (types != null) {
            for (int i = 0; i < types.size(); i++) {
                CorbaTypeImpl type = types.get(i);
                QName schemaType = type.getType();
                if (schemaType != null) {
                    String typeNamespace = schemaType.getNamespaceURI();
                    try {
                        // WS-Addressing namespace is a special case.  We need to import the schema from 
                        // a remote location.
                        if (!namespaces.contains(typeNamespace) 
                            && typeNamespace.equals(ReferenceConstants.WSADDRESSING_NAMESPACE)) {
                            
                            // build up the ws-addressing schema import
                            Schema wsdlSchema = 
                                (Schema)def.getExtensionRegistry().createExtension(Types.class,
                                                     new QName(Constants.URI_2001_SCHEMA_XSD, "schema"));
                            SchemaImport schemaimport =  wsdlSchema.createImport();
                            schemaimport.setNamespaceURI(ReferenceConstants.WSADDRESSING_NAMESPACE);
                            schemaimport.setSchemaLocationURI(ReferenceConstants.WSADDRESSING_LOCATION);
                            wsdlSchema.addImport(schemaimport);
                            
                            // add the import and the prefix to the definition
                            def.getTypes().addExtensibilityElement(wsdlSchema);
                            CastUtils.cast(def.getNamespaces(), String.class, String.class)
                                .put(ReferenceConstants.WSADDRESSING_PREFIX, typeNamespace);
                        } else if (!namespaces.contains(typeNamespace)) {
                            String prefix = getModulePrefixForNamespace(userModuleMappings, mapper, 
                                                                        typeNamespace);
                            //prefix = mapper.mapNSToPrefix(typeNamespace);
                            XmlSchema schema = manager.getXmlSchema(typeNamespace);
                            // TODO: REVISIT - Is this the only way we can create the file name for the
                            // imported schema?
                            String importFile = visitor.getOutputDir()
                                + System.getProperty("file.separator")
                                + prefix + ".xsd";
                            manager.addWSDLSchemaImport(def, typeNamespace, importFile);
                            manager.getImportedXmlSchemas().put(new File(importFile), schema);
                            CastUtils.cast(def.getNamespaces(), String.class, String.class)
                                .put(prefix, typeNamespace);
                        }
                    } catch (Exception ex) {
                        throw new ToolException("Unable to add schema import for namespace"
                                                + typeNamespace);
                    }
                }
            }
        }
    }
    
    private String getModulePrefixForNamespace(Set<Map.Entry<String, String>> map, 
                                               ModuleToNSMapper mapper, String namespace) {
        String prefix = null;
        
        for (Iterator<Map.Entry<String, String>> iter = map.iterator(); iter.hasNext();) {
            Map.Entry<String, String> entry = iter.next();
            if (entry.getValue().equals(namespace)) {
                prefix = entry.getKey().replace(ToolCorbaConstants.MODULE_SEPARATOR, "_");
                break;
            }
        }
        
        if (prefix == null) {
            prefix = mapper.mapNSToPrefix(namespace);   
        }
        
        return prefix;
    }

    private Map<String, String> getModuleToNSMapping(String mapping) {
        Map<String, String> map = new HashMap<String, String>();
        if ((mapping != null) && (mapping.length() > 0)) {
            if ((mapping.startsWith("[")) && (mapping.endsWith("]"))) {
                mapping = mapping.substring(1, mapping.length() - 1);
                StringTokenizer tokens = new StringTokenizer(mapping, ",;");
                while (tokens.hasMoreTokens()) {
                    String token = tokens.nextToken();
                    int pos = token.indexOf("=");
                    if (pos == -1) {
                        throw new RuntimeException("Mapping of idl modules to namespaces "
                                                   + "is not specified correctly."
                                                   + "Missing a equals(=) sign for specifying "
                                                   + "the custom mapping."
                                                   + "(" + token + ")");
                    }
                    map.put(token.substring(0, pos), token.substring(pos + 1));
                }
            } else if (mapping.startsWith(":")) {
                mapping = mapping.substring(1);
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(mapping));
                    String token = reader.readLine();
                    while (token != null) {
                        int pos = token.indexOf("=");
                        if (pos == -1) {
                            throw new RuntimeException("Mapping of idl modules to namespaces "
                                                       + "is not specified correctly in the file "
                                                       + mapping + "."
                                                       + "Missing a equals(=) sign for specifying "
                                                       + "the custom mapping."
                                                       + "(" + token + ")");
                        }
                        map.put(token.substring(0, pos), token.substring(pos + 1));
                        token = reader.readLine();
                    }
                } catch (Exception ex) {
                    throw new RuntimeException("Incorrect properties file for mns mapping - " + mapping
                                               + ". Cause: " + ex.getMessage());
                }
            } else {
                throw new RuntimeException("Option mns should have a start([) & close(]) bracket"
                                           + " or a properties file"
                                           + " to customize the mapping of modules to namespaces");
            }
        }
        return map;
    }

    private Map<String, List> getExcludedModules(String modules) {
        Map<String, List> exModules = new HashMap<String, List>();
        if ((modules != null) && (modules.length() > 0)) {
            if ((modules.startsWith("[")) && (modules.endsWith("]"))) {
                modules = modules.substring(1, modules.length() - 1);
                StringTokenizer tokens = new StringTokenizer(modules, ",;");
                while (tokens.hasMoreTokens()) {
                    String token = tokens.nextToken();
                    //Revisit, Do we also take in the imports of the wsdl/schema?
                    exModules.put(token, new ArrayList());
                }               
            } else if (modules.startsWith(":")) {
                //TO DO
            } else {
                throw new RuntimeException("Option ex should have a start([) & close(]) bracket"
                                           + " or a properties file"
                                           + " to specify the exclusion of modules");
            }
        }
        return exModules;
    }
    
}
