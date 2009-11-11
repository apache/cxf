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

package org.apache.cxf.tools.corba.processors.wsdl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Operation;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import org.apache.cxf.binding.corba.wsdl.AddressType;
import org.apache.cxf.binding.corba.wsdl.ArgType;
import org.apache.cxf.binding.corba.wsdl.BindingType;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.binding.corba.wsdl.MemberType;
import org.apache.cxf.binding.corba.wsdl.OperationType;
import org.apache.cxf.binding.corba.wsdl.ParamType;
import org.apache.cxf.binding.corba.wsdl.RaisesType;
import org.apache.cxf.binding.corba.wsdl.TypeMappingType;
import org.apache.cxf.binding.corba.wsdl.W3CConstants;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.wsdl.JAXBExtensionHelper;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaAppInfo;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaExternal;
import org.apache.ws.commons.schema.XmlSchemaObjectTable;
import org.apache.ws.commons.schema.XmlSchemaType;

public class WSDLToCorbaBinding {
    
    protected static final Logger LOG = LogUtils.getL7dLogger(WSDLToCorbaBinding.class);
           
    static XmlSchema xmlSchemaType;                   
    static String wsdlFileName;
    static String outputFile;
    static String namespace;
    static boolean verboseOn;    
    protected Definition def;
    protected String idlNamespace;    

    WSDLToCorbaHelper helper = new WSDLToCorbaHelper();
    TypeMappingType typeMappingType;
    ExtensionRegistry extReg;

    List<String> interfaceNames = new ArrayList<String>();
    Map<Object, Object> bindingNameMap = new HashMap<Object, Object>();
    String bindingName;
    String address;
    String addressFile;
    WSDLParameter wsdlParameter;
    List<String> bindingNames;
    SchemaCollection xmlSchemaList;
    WSDLToTypeProcessor typeProcessor = new WSDLToTypeProcessor();
    private boolean allbindings;
        
    public WSDLToCorbaBinding() {            
    }


    public WSDLToCorbaHelper getHelper() {
        return helper;
    }
    

    public Definition generateCORBABinding() throws Exception {
        try {
            typeProcessor.parseWSDL(getWsdlFileName());           
            def = typeProcessor.getWSDLDefinition();
            generateCORBABinding(def);            
        } catch (Exception ex) {
            throw ex;
        }
        return def;
    }

    public Binding[] generateCORBABinding(Definition definition) throws Exception {    
        def = definition;
        helper.setWsdlDefinition(def);
        typeProcessor.setWSDLDefinition(def);
        wsdlParameter = new WSDLParameter();
        if (idlNamespace == null) {
            setIdlNamespace(def);
        }
        generateNSPrefix(def, getIdlNamespace(), "ns");

        typeProcessor.process();
        xmlSchemaList = typeProcessor.getXmlSchemaTypes();
        helper.setXMLSchemaList(xmlSchemaList);
        
        List<PortType> intfs = null;
        if (interfaceNames.size() > 0) {
            intfs = new ArrayList<PortType>(interfaceNames.size());

            for (String interfaceName : interfaceNames) {
                PortType portType = null;

                Map portTypes = def.getAllPortTypes();
                if (portTypes != null) {
                    Iterator it2 = portTypes.keySet().iterator();
                    while (it2.hasNext()) {
                        QName existPortQName = (QName)it2.next();
                        if (!existPortQName.getLocalPart().equals(interfaceName)) {
                            portType = null;
                        } else {
                            portType = (PortType)portTypes.get(existPortQName);
                            break;
                        }
                    }
                }

                if (portType == null) {                    
                    String msgStr = "PortType " + interfaceName 
                        + " doesn't exist in WSDL.";                     
                    throw new Exception(msgStr);
                }
                intfs.add(portType);
            }
        } else {
            // gets default portType or all portTypes.
            intfs = getPortTypeList();
        }

        Binding[] bindings = new Binding[intfs.size()];
        for (int i = 0; i < intfs.size(); i++) {
            bindings[i] = generateCORBABinding(def, (PortType) intfs.get(i));
            generateCORBAServiceForBinding(def, (PortType) intfs.get(i), bindings[i]);
        }
        return bindings;
    }

    private List<PortType> getPortTypeList() throws Exception {
        Map portTypes = def.getAllPortTypes();
        List<PortType> intfs = null;

        if (portTypes == null) {
            org.apache.cxf.common.i18n.Message msg = new org.apache.cxf.common.i18n.Message(
                "No PortTypes defined in wsdl", LOG);
            throw new Exception(msg.toString());
        } else {
            PortType portType = null;
            intfs = new ArrayList<PortType>();
            if (portTypes.size() == 1) {
                portType = (PortType) portTypes.values().iterator().next();
                interfaceNames.add(portType.getQName().getLocalPart());                    
                intfs.add(portType);
            } else if (portTypes.size() > 1) {
                if (def.getAllBindings().size() > 0) {
                    throwMultipleMultipleTypeException(def.getAllBindings());
                }
                Iterator iter = portTypes.values().iterator();
                while (iter.hasNext()) {                    
                    PortType port = (PortType)iter.next();
                    interfaceNames.add(port.getQName().getLocalPart());                        
                    intfs.add(port);
                }                    
            }
        }
        return intfs;
    }
    
    private void throwMultipleMultipleTypeException(Map binds) throws Exception {
        StringBuilder sb = new StringBuilder();
        org.apache.cxf.common.i18n.Message msgDef = 
            new org.apache.cxf.common.i18n.Message("Multiple Bindings already defined in the wsdl", LOG);
        sb.append(msgDef.toString());
        Iterator it2 = binds.keySet().iterator();
        int cnt = 0;
        while (it2.hasNext()) {
            cnt++;
            sb.append("  " + cnt + " --> " + ((QName) it2.next()).getLocalPart());
        }
        throw new Exception(sb.toString());
    }

    private Binding generateCORBABinding(Definition definition, PortType portType) throws Exception {
        QName bqname = null;

        if (extReg == null) {
            extReg = def.getExtensionRegistry();
            addExtensions(extReg);
        }

        bindingNames = new ArrayList<String>();
        String interfaceName = portType.getQName().getLocalPart();
        String bname = getMappedBindingName(interfaceName);
    
        String prefix = definition.getPrefix(definition.getTargetNamespace());
        if (prefix == null) {
            prefix = "";
        }
        if (bname == null && !allbindings) {
            bname = bindingName;
        }
        if (bname == null) {
            bname = mangleInterfaceName(interfaceName) + "CORBABinding";
            setBindingName(bname);
            bqname = new QName(definition.getTargetNamespace(), bname, prefix);
            int count = 0;
            while (WSDLToCorbaHelper.queryBinding(definition, bqname)) {
                bname = bname + count;
                bqname = new QName(definition.getTargetNamespace(), bname, prefix);
            }
        } else {
            bqname = new QName(definition.getTargetNamespace(), bname, prefix);
            // Check if the Binding with name already exists
            if (WSDLToCorbaHelper.queryBinding(definition, bqname)) {
                String msgStr = "Binding " + bqname.getLocalPart() 
                    + " already exists in WSDL.";
                org.apache.cxf.common.i18n.Message msg = 
                    new org.apache.cxf.common.i18n.Message(msgStr, LOG);
                throw new Exception(msg.toString());                    
            }
        }

        // jwsdl model should have all other bindings in it.
        String pfx = definition.getPrefix(CorbaConstants.NU_WSDL_CORBA);
        if (pfx == null) {
            pfx = new String("corba");
            def.addNamespace(pfx, CorbaConstants.NU_WSDL_CORBA);
        }

        Binding binding = null;
        binding = def.createBinding();
        binding.setPortType(portType);
        binding.setQName(bqname);

        bindingNames.add(bname);
        mapBindingToInterface(portType.getQName().getLocalPart(), bname);
        BindingType bindingType = null;
        
        addCorbaTypeMap(def);
        
        try {
            bindingType = (BindingType)extReg
                .createExtension(Binding.class, CorbaConstants.NE_CORBA_BINDING);
            bindingType.setRepositoryID(WSDLToCorbaHelper.REPO_STRING
                                        + binding.getPortType().getQName().getLocalPart().replace('.', '/')
                                        + WSDLToCorbaHelper.IDL_VERSION);

            binding.addExtensibilityElement(bindingType);
        } catch (WSDLException ex) {
            ex.printStackTrace();
        }

        try {
            addBindingOperations(def, portType, binding);        
            binding.setUndefined(false);
            definition.addBinding(binding);            
        } catch (Exception ex) {
            binding.setUndefined(true);
        }

        cleanUpTypeMap(typeMappingType);
        
        return binding;
    }
    
    private void generateCORBAServiceForBinding(Definition definition, PortType portType,
                                                Binding binding) throws Exception {
        if (extReg == null) {
            extReg = def.getExtensionRegistry();
            addExtensions(extReg);
        }

        String interfaceName = portType.getQName().getLocalPart();
        interfaceName = mangleInterfaceName(interfaceName);
        String serviceName = interfaceName + "CORBAService";
        String portName = interfaceName + "CORBAPort";

        String prefix = definition.getPrefix(definition.getTargetNamespace());
        if (prefix == null) {
            prefix = "";
        }

        String corbaPrefix = definition.getPrefix(CorbaConstants.NU_WSDL_CORBA);
        if (corbaPrefix == null) {
            corbaPrefix = new String("corba");
            def.addNamespace(corbaPrefix, CorbaConstants.NU_WSDL_CORBA);
        }
        
        // Build the service and port information and add it to the wsdl
        Service service = def.createService();

        Port servicePort = def.createPort();
        servicePort.setName(portName);
        servicePort.setBinding(binding);

        try {
            AddressType addressType =
                (AddressType) def.getExtensionRegistry().createExtension(Port.class,
                                                                         CorbaConstants.NE_CORBA_ADDRESS);
            
            String addr = null; 
            if (getAddressFile() != null) {
                try {
                    File addrFile = new File(getAddressFile());
                    FileReader fileReader = new FileReader(addrFile);
                    BufferedReader bufferedReader = new BufferedReader(fileReader);
                    addr = bufferedReader.readLine();
                } catch (Exception ex) {
                    throw new ToolException(ex.getMessage(), ex);
                }
            } else {
                addr = getAddress();
            }
            if (addr == null) {
                addr = "file:./" + interfaceName + ".ref";            
            }
            addressType.setLocation(addr);
            servicePort.addExtensibilityElement(addressType);
        } catch (WSDLException ex) {
            throw new Exception("Failed to create CORBA address for service", ex);
        }
        
        QName serviceQName = new QName(definition.getTargetNamespace(), serviceName, prefix);
        service.setQName(serviceQName);
        service.addPort(servicePort);
        definition.addService(service);
    }

    private void addBindingOperations(Definition definition, PortType portType, Binding binding) 
        throws Exception {
        
        List<Operation> ops = CastUtils.cast(portType.getOperations());
        for (Operation op : ops) {
            try {
                BindingOperation bindingOperation = definition.createBindingOperation();
                addCorbaOperationExtElement(bindingOperation, op);
                bindingOperation.setName(op.getName());
                if (op.getInput() != null) {
                    BindingInput bindingInput = definition.createBindingInput();
                    bindingInput.setName(op.getInput().getName());
                    bindingOperation.setBindingInput(bindingInput);
                }
                if (op.getOutput() != null) {
                    BindingOutput bindingOutput = definition.createBindingOutput();
                    bindingOutput.setName(op.getOutput().getName());
                    bindingOperation.setBindingOutput(bindingOutput);
                }
                // add Faults
                if (op.getFaults() != null && op.getFaults().size() > 0) {
                    Map faults = op.getFaults();
                    Iterator i = faults.values().iterator();               
                    while (i.hasNext()) {
                        Fault fault = (Fault)i.next();
                        BindingFault bindingFault = definition.createBindingFault();
                        bindingFault.setName(fault.getName());                    
                        bindingOperation.addBindingFault(bindingFault);
                    }               
                }
                bindingOperation.setOperation(op);
                binding.addBindingOperation(bindingOperation);
            } catch (Exception ex) {
                LOG.warning("Operation " + op.getName() + " not mapped to CORBA binding.");
            }
        }
    }

    public void addExtensions(ExtensionRegistry extRegistry) throws JAXBException {
        try {                      
            JAXBExtensionHelper.addExtensions(extReg, Binding.class, BindingType.class);
            JAXBExtensionHelper.addExtensions(extReg, BindingOperation.class,
                                              org.apache.cxf.binding.corba.wsdl.OperationType.class);
            JAXBExtensionHelper.addExtensions(extReg, Definition.class, TypeMappingType.class);
            JAXBExtensionHelper.addExtensions(extReg, Port.class,
                                              org.apache.cxf.binding.corba.wsdl.AddressType.class);

            extReg.mapExtensionTypes(Binding.class, CorbaConstants.NE_CORBA_BINDING, BindingType.class);
            extReg.mapExtensionTypes(BindingOperation.class, CorbaConstants.NE_CORBA_OPERATION,
                                     org.apache.cxf.binding.corba.wsdl.OperationType.class);
            extReg.mapExtensionTypes(Definition.class, CorbaConstants.NE_CORBA_TYPEMAPPING,
                                     TypeMappingType.class);
            extReg.mapExtensionTypes(Port.class, CorbaConstants.NE_CORBA_ADDRESS,
                                     org.apache.cxf.binding.corba.wsdl.AddressType.class);
        } catch (javax.xml.bind.JAXBException ex) {
            LOG.log(Level.SEVERE, "Failing to serialize/deserialize extensions", ex);
            throw new JAXBException(ex.getMessage());
        }
    }    
    
    private void addCorbaOperationExtElement(BindingOperation bo, Operation op) 
        throws Exception {

        OperationType operationType = null;
        try {
            operationType = (OperationType)extReg.createExtension(BindingOperation.class,
                                                                  CorbaConstants.NE_CORBA_OPERATION);
        } catch (WSDLException wse) {            
            LOG.log(Level.SEVERE, "Failed to create a Binding Operation extension", wse);
            throw new Exception(LOG.toString(), wse);
        }

        operationType.setName(op.getName());
        List<ParamType> params = new ArrayList<ParamType>();
        List<ArgType> returns = new ArrayList<ArgType>();

        wsdlParameter.processParameters(this, op, def, xmlSchemaList, params, returns, true);

        for (ParamType paramtype : params) {
            operationType.getParam().add(paramtype);
        }
        for (ArgType retType : returns) {
            operationType.setReturn(retType);
        }

        Map faults = op.getFaults();
        Iterator i = faults.values().iterator();               
        while (i.hasNext()) {
            Fault fault = (Fault)i.next();
            RaisesType raisestype = new RaisesType();            
            CorbaTypeImpl extype = convertFaultToCorbaType(xmlSchemaType, fault);           
            if (extype != null) {
                raisestype.setException(helper.createQNameCorbaNamespace(extype.getName()));                
                operationType.getRaises().add(raisestype);
            }
        }
               
        bo.addExtensibilityElement(operationType);
    }

    
    private void addCorbaTypeMap(Definition definition) throws Exception {

        Iterator t = definition.getExtensibilityElements().iterator();
        Iterator j = definition.getExtensibilityElements().iterator();
        while (t.hasNext()) {
            if (j.next() instanceof TypeMappingType) {
                typeMappingType = (TypeMappingType)t.next();
                break;
            }
        }

        if (typeMappingType == null) {
            typeMappingType = (TypeMappingType)extReg.createExtension(Definition.class,
                                                                      CorbaConstants.NE_CORBA_TYPEMAPPING);
            typeMappingType.setTargetNamespace(getIdlNamespace());
            definition.addExtensibilityElement(typeMappingType);
        }
        helper.setTypeMap(typeMappingType);
        addCorbaTypes(definition);
    }
    
    private void addCorbaTypes(Definition definition) throws Exception {   
        for (XmlSchema xmlSchemaTypes : xmlSchemaList.getXmlSchemas()) {
                
            Iterator schemas = xmlSchemaTypes.getIncludes().getIterator();
            while (schemas.hasNext()) {
                Object obj = schemas.next();
                if (obj instanceof XmlSchemaExternal) {
                    XmlSchemaExternal extSchema = (XmlSchemaExternal) obj;                 
                    addCorbaTypes(extSchema.getSchema());
                    // REVISIT: This was preventing certain types from being added to the corba
                    // typemap even when they are referenced from other parts of the wsdl.
                    //
                    // Should this add the corba types if it IS an instance of the XmlSchemaImport 
                    // (and not an XmlSchemaInclude or XmlSchemaRedefine)?
                    //if (!(extSchema instanceof XmlSchemaImport)) {
                    //    addCorbaTypes(extSchema.getSchema());
                    //}
                }
            }
            if (!W3CConstants.NU_SCHEMA_XSD.equals(xmlSchemaTypes.getTargetNamespace())) {
                addCorbaTypes(xmlSchemaTypes);
            }
        }
    }              
    
    private void addCorbaTypes(XmlSchema xmlSchemaTypes) throws Exception {
        XmlSchemaObjectTable objs = xmlSchemaTypes.getSchemaTypes(); 
        Iterator i = objs.getValues();
        CorbaTypeImpl corbaTypeImpl = null;
        while (i.hasNext()) {                
            XmlSchemaType type = (XmlSchemaType)i.next();
            boolean anonymous = WSDLTypes.isAnonymous(type.getName());
            corbaTypeImpl = helper.convertSchemaToCorbaType(type, type.getQName(), null, 
                                                            null, anonymous);
            if (corbaTypeImpl != null
                && !helper.isDuplicate(corbaTypeImpl)) {
                typeMappingType.getStructOrExceptionOrUnion().add(corbaTypeImpl);
            }                   
        }
        addCorbaElements(corbaTypeImpl, xmlSchemaTypes);
    }

    
    private void addCorbaElements(CorbaTypeImpl corbaTypeImpl, 
                                  XmlSchema xmlSchemaTypes) throws Exception {
        XmlSchemaObjectTable elements = xmlSchemaTypes.getElements();                            
        Iterator i2 = elements.getValues();                
        while (i2.hasNext()) {                
            XmlSchemaElement el = (XmlSchemaElement)i2.next();
            QName elName = el.getQName();
            XmlSchemaType schemaType = el.getSchemaType();
            if (elName == null) {
                elName = el.getRefName();
                schemaType = helper.getSchemaType(elName);
            }
            boolean anonymous = false;
            if (schemaType == null) {
                anonymous = true;
            } else {
                anonymous = WSDLTypes.isAnonymous(schemaType.getName());
            }
            
            if (schemaType != null) {
                XmlSchemaAnnotation annotation = null;
                if (el.getAnnotation() != null) {
                    annotation = el.getAnnotation();
                }
                
                // Check to see if this element references the binding we are creating.  For now,
                // this situation won't be handled. REVISIT.
                if (annotation != null) {
                    Iterator i = annotation.getItems().getIterator();
                    XmlSchemaAppInfo appInfo = null;
                    while (i.hasNext()) {
                        Object item = i.next();
                        if (item instanceof XmlSchemaAppInfo) {
                            appInfo = (XmlSchemaAppInfo)item;
                            break;
                        }
                    }
                    
                    if (appInfo != null) {
                        NodeList nlist = appInfo.getMarkup();
                        Node node = nlist.item(0);
                        String info = node.getNodeValue();
                        info = info.trim();
                        String annotationBindingName = "";
                        if ("corba:binding=".equals(info.substring(0, 14))) {
                            annotationBindingName = info.substring(14);
                        }
                        if (bindingName.equals(annotationBindingName)) {
                            annotation = null;
                        }
                    }
                }
                corbaTypeImpl = 
                    helper.convertSchemaToCorbaType(schemaType,
                                                    elName, schemaType, 
                                                    annotation, anonymous);
                if (el.isNillable()) {                            
                    QName uname =  
                        helper.createQNameCorbaNamespace(corbaTypeImpl.getQName().getLocalPart() + "_nil");
                    boolean isQualified = corbaTypeImpl.isSetQualified() && corbaTypeImpl.isQualified();
                    corbaTypeImpl = helper.createNillableUnion(uname, 
                                                               helper.checkPrefix(elName),
                                                               helper.checkPrefix(corbaTypeImpl.getQName()),
                                                               isQualified);
                } 

                if (corbaTypeImpl != null
                    && !helper.isDuplicate(corbaTypeImpl)) {
                    typeMappingType.getStructOrExceptionOrUnion().add(corbaTypeImpl);
                }
            }                   
        }
    }    

    private CorbaTypeImpl convertFaultToCorbaType(XmlSchema xmlSchema, Fault fault) throws Exception {
        org.apache.cxf.binding.corba.wsdl.Exception corbaex = null;
        XmlSchemaType schemaType = null;
        Iterator parts = fault.getMessage().getParts().values().iterator();
        
        if (!parts.hasNext()) {
            String msgStr = "Fault " + fault.getMessage().getQName().getLocalPart() 
                + " UNSUPPORTED_FAULT.";
            org.apache.cxf.common.i18n.Message msg = 
                new org.apache.cxf.common.i18n.Message(msgStr, LOG);
            throw new Exception(msg.toString());            
        }

        Part part = (Part)parts.next();
        schemaType = helper.lookUpType(part);
        if (schemaType != null) {
            QName name = schemaType.getQName();
            if (name == null) {
                name = part.getElementName();
            }
            if (!helper.isSchemaTypeException(schemaType)) {
                corbaex = new org.apache.cxf.binding.corba.wsdl.Exception();
                String faultName = fault.getMessage().getQName().getLocalPart();
                int pos = faultName.indexOf("_exception.");             
                if (pos != -1) {
                    faultName = faultName.substring(pos + 11);
                    faultName = faultName + "Exception";
                }
                QName faultMsgName = helper.createQNameCorbaNamespace(faultName);
                corbaex.setName(faultName);
                corbaex.setQName(faultMsgName);
                CorbaTypeImpl corbaTypeImpl = 
                    helper.convertSchemaToCorbaType(schemaType, name, null, null, false);
                if (corbaTypeImpl != null) {
                    MemberType member = new MemberType();
                    member.setName(corbaTypeImpl.getQName().getLocalPart());
                    member.setIdltype(corbaTypeImpl.getQName());
                    if (corbaTypeImpl.isSetQualified() && corbaTypeImpl.isQualified()) {
                        member.setQualified(true);
                    }
                    corbaex.getMember().add(member);
                }
            } else {
                corbaex = createCorbaException(name, schemaType);
            }
        }
        if (schemaType == null) {
            String msgStr = "Fault " + fault.getMessage().getQName().getLocalPart() 
                 + " INCORRECT_FAULT_MSG.";
            org.apache.cxf.common.i18n.Message msg = 
                new org.apache.cxf.common.i18n.Message(msgStr, LOG);
            throw new Exception(msg.toString());                           
        }

        if (corbaex == null) {
            String msgStr = "Fault " + fault.getMessage().getQName().getLocalPart() 
                + " UNSUPPORTED_FAULT.";
            org.apache.cxf.common.i18n.Message msg = 
                new org.apache.cxf.common.i18n.Message(msgStr, LOG);
            throw new Exception(msg.toString());            
        } else {
            // Set the repository ID for Exception
            // add to CorbaTypeMapping
            String repoId = WSDLToCorbaHelper.REPO_STRING 
                + corbaex.getName().replace('.', '/') 
                + WSDLToCorbaHelper.IDL_VERSION;
            corbaex.setRepositoryID(repoId);
            CorbaTypeImpl corbaTypeImpl = (CorbaTypeImpl)corbaex;
            if (!helper.isDuplicate(corbaTypeImpl)) {
                CorbaTypeImpl dup = helper.isDuplicateException(corbaTypeImpl);               
                if (dup != null) {                    
                    typeMappingType.getStructOrExceptionOrUnion().remove(dup);
                    typeMappingType.getStructOrExceptionOrUnion().add(corbaTypeImpl);
                } else {
                    typeMappingType.getStructOrExceptionOrUnion().add(corbaTypeImpl);
                }
            }
        }
        return corbaex;
    }
    
    private org.apache.cxf.binding.corba.wsdl.Exception createCorbaException(QName schemaTypeName,
                                                                                  XmlSchemaType stype)
        throws Exception {
        org.apache.cxf.binding.corba.wsdl.Exception corbaex = null;
        XmlSchemaComplexType complex = null;

        if (stype instanceof XmlSchemaComplexType) {
            QName defaultName = schemaTypeName;
            complex = (XmlSchemaComplexType)stype;
            corbaex = new org.apache.cxf.binding.corba.wsdl.Exception();
            corbaex.setQName(schemaTypeName);
            corbaex.setType(helper.checkPrefix(schemaTypeName));
            corbaex.setName(schemaTypeName.getLocalPart());

            corbaex.setRepositoryID(WSDLToCorbaHelper.REPO_STRING
                                    + "/"
                                    + defaultName.getLocalPart()
                                    + WSDLToCorbaHelper.IDL_VERSION);
            String uri = defaultName.getNamespaceURI();
            List attributeMembers = helper.processAttributesAsMembers(complex.getAttributes().getIterator(),
                                                                      uri);
            Iterator iterator = attributeMembers.iterator();
            while (iterator.hasNext()) {
                MemberType memberType = (MemberType)iterator.next();
                corbaex.getMember().add(memberType);
            }            
            List members = helper.processContainerAsMembers(complex.getParticle(),
                                                            stype.getQName(),
                                                            defaultName);
            Iterator it = members.iterator();
            while (it.hasNext()) {
                MemberType memberType = (MemberType)it.next();
                corbaex.getMember().add(memberType);
            }            
        }        
        return corbaex;
    }
                

    
    public void setWsdlFile(String file) {
        wsdlFileName = new String(file);
    }

    public String getWsdlFileName() {
        return wsdlFileName;
    }

    public void setIdlNamespace(Definition definition) {
            
        if (idlNamespace == null) {
            String tns = definition.getTargetNamespace();
            if (!tns.endsWith("/")) {
                tns += "/";
            }

            idlNamespace = tns + "corba/typemap/";
        }
        setNamespace(idlNamespace);
    }

    public String getIdlNamespace() {
        return idlNamespace;
    }

    public void generateNSPrefix(Definition definition, String namespaceURI, String str) {
        String pfx = def.getPrefix(namespaceURI);

        if (pfx != null) {
            return;
        }

        int cnt = 0;

        while (pfx == null) {
            cnt++;
            pfx = str + cnt;
            if (def.getNamespace(pfx) != null) {
                pfx = null;
            }
        }
        def.addNamespace(pfx, namespaceURI);
    }
    

    public void setBindingName(String bname) {
        bindingName = bname;
    }

    public void mapBindingToInterface(String intfName, String bName) {
        bindingNameMap.put(intfName, bName);
    }

    public String getMappedBindingName(String interfaceName) {
        return (String)bindingNameMap.get(interfaceName);
    }

    public List<String> getGeneratedBindingNames() {
        return bindingNames;
    }

    private String mangleInterfaceName(String name) {
        int idx = name.indexOf("PortType");

        if (idx != -1) {
            return name.substring(0, idx);
        } else {
            return name;
        }
    }

    public QName convertToQName(String name) {
        String namespaceName = null;
        String nametype = null;
        String pfx = "";

        int i = name.lastIndexOf("}");
        int i2 = name.indexOf("}");

        if (i >= 1) {
            if (i == i2) {
                namespaceName = name.substring(1, i);
                nametype = name.substring(i + 1, name.length());
            } else {
                namespaceName = name.substring(1, i2);
                pfx = name.substring(i2 + 2, i);
                nametype = name.substring(i + 1, name.length());
            }
        }
        return new QName(namespaceName, nametype, pfx);
    }

    public void setOutputDirectory(String dir) {
        // Force directory creation
        // before setting output directory
        if (dir != null) {
            File fileOutputDir = new File(dir);

            if (!fileOutputDir.exists()) {
                fileOutputDir.mkdir();
            }
        }    
    }

    public void setExtensionRegistry(ExtensionRegistry reg) {
        extReg = reg;
    }
    
    public ExtensionRegistry getExtensionRegistry() {
        return extReg;
    }

    public void setAddress(String addr) {
        address = addr;
    } 
    
    public String getAddress() {
        return address;
    }
    
    public void setAddressFile(String addrFile) {
        addressFile = addrFile;    
    }
    
    public String getAddressFile() {
        return addressFile;
    }
    
    public void setOutputFile(String file) {
        outputFile = new String(file);
    }

    public void setNamespace(String nameSpaceName) {
        idlNamespace = nameSpaceName;
        helper.setIdlNamespace(idlNamespace);
    }

    public void addInterfaceName(String interfaceName) {
        interfaceNames.add(interfaceName);
    }

    public List getInterfaceNames() {
        return interfaceNames;
    }
    
    public void setVerboseOn(boolean verbose) {
        verboseOn = verbose;
    }
    
    public void setAllBindings(boolean all) {
        allbindings = all;
    }
    
    public boolean isGenerateAllBindings() {
        return allbindings;
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

    public void main(String args[]) {
        if (args.length != 6) {
            System.err.println("usage: WSDLToCORBABinding "
                               + "-w <wsdl file> -i <interfaceName> -o <output wsdl file>");
            return;
        }
        try {
            WSDLToCorbaBinding wsdlToCorbaBinding = new WSDLToCorbaBinding();
            wsdlToCorbaBinding.setWsdlFile(args[1]);
            wsdlToCorbaBinding.addInterfaceName(args[3]);
            wsdlToCorbaBinding.setOutputDirectory(".");
            wsdlToCorbaBinding.generateCORBABinding();
        } catch (Exception ex) {
            System.err.println("Error : " + ex.getMessage());
            System.err.println();
            if (verboseOn) {
                ex.printStackTrace();
            }
            System.exit(1);
        }
    }

}
