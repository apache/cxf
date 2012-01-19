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

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.wsdl.Alias;
import org.apache.cxf.binding.corba.wsdl.Anonarray;
import org.apache.cxf.binding.corba.wsdl.Anonfixed;
import org.apache.cxf.binding.corba.wsdl.Anonsequence;
import org.apache.cxf.binding.corba.wsdl.Anonstring;
import org.apache.cxf.binding.corba.wsdl.ArgType;
import org.apache.cxf.binding.corba.wsdl.Array;
import org.apache.cxf.binding.corba.wsdl.BindingType;
import org.apache.cxf.binding.corba.wsdl.CaseType;
import org.apache.cxf.binding.corba.wsdl.Const;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.binding.corba.wsdl.Enum;
import org.apache.cxf.binding.corba.wsdl.Enumerator;
import org.apache.cxf.binding.corba.wsdl.Fixed;
import org.apache.cxf.binding.corba.wsdl.MemberType;
import org.apache.cxf.binding.corba.wsdl.ParamType;
import org.apache.cxf.binding.corba.wsdl.RaisesType;
import org.apache.cxf.binding.corba.wsdl.Sequence;
import org.apache.cxf.binding.corba.wsdl.Struct;
import org.apache.cxf.binding.corba.wsdl.TypeMappingType;
import org.apache.cxf.binding.corba.wsdl.Union;
import org.apache.cxf.binding.corba.wsdl.Unionbranch;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.tools.corba.common.idltypes.CorbaUtils;
import org.apache.cxf.tools.corba.common.idltypes.IdlAnonArray;
import org.apache.cxf.tools.corba.common.idltypes.IdlAnonFixed;
import org.apache.cxf.tools.corba.common.idltypes.IdlAnonSequence;
import org.apache.cxf.tools.corba.common.idltypes.IdlArray;
import org.apache.cxf.tools.corba.common.idltypes.IdlAttribute;
import org.apache.cxf.tools.corba.common.idltypes.IdlConst;
import org.apache.cxf.tools.corba.common.idltypes.IdlDefn;
import org.apache.cxf.tools.corba.common.idltypes.IdlEnum;
import org.apache.cxf.tools.corba.common.idltypes.IdlEnumerator;
import org.apache.cxf.tools.corba.common.idltypes.IdlException;
import org.apache.cxf.tools.corba.common.idltypes.IdlField;
import org.apache.cxf.tools.corba.common.idltypes.IdlFixed;
import org.apache.cxf.tools.corba.common.idltypes.IdlInterface;
import org.apache.cxf.tools.corba.common.idltypes.IdlModule;
import org.apache.cxf.tools.corba.common.idltypes.IdlOperation;
import org.apache.cxf.tools.corba.common.idltypes.IdlParam;
import org.apache.cxf.tools.corba.common.idltypes.IdlRoot;
import org.apache.cxf.tools.corba.common.idltypes.IdlScopeBase;
import org.apache.cxf.tools.corba.common.idltypes.IdlSequence;
import org.apache.cxf.tools.corba.common.idltypes.IdlString;
import org.apache.cxf.tools.corba.common.idltypes.IdlStruct;
import org.apache.cxf.tools.corba.common.idltypes.IdlType;
import org.apache.cxf.tools.corba.common.idltypes.IdlTypedef;
import org.apache.cxf.tools.corba.common.idltypes.IdlUnion;
import org.apache.cxf.tools.corba.common.idltypes.IdlUnionBranch;
import org.apache.cxf.tools.corba.utils.FileOutputStreamFactory;
import org.apache.cxf.tools.corba.utils.OutputStreamFactory;
import org.apache.cxf.wsdl.JAXBExtensionHelper;

public class WSDLToIDLAction {
        
    protected static final Logger LOG = LogUtils.getL7dLogger(WSDLToIDLAction.class);
    private static String bindingName;
    private static String wsdlFileName;
    private static String namespace;
    private String outputFile;
    private boolean verboseOn;
    private PrintWriter printWriter;
    private OutputStreamFactory factory = new FileOutputStreamFactory();
    private Definition def;
    private IdlRoot root = IdlRoot.create();
    private IdlInterface intf;
    private ExtensionRegistry extReg;
    private WSDLToTypeProcessor typeProcessor = new WSDLToTypeProcessor(); 
    private boolean generateAllBindings;    

    public WSDLToIDLAction() {
    }
    
    public void generateIDL(Definition definition) throws Exception {
        if (definition == null) {
            typeProcessor.parseWSDL(wsdlFileName);
            def = typeProcessor.getWSDLDefinition();
        } else {
            def = definition;
        }
        extReg = def.getExtensionRegistry();

        if (printWriter == null) {
            printWriter = createPrintWriter(outputFile);
        }
        
        if (!isGenerateAllBindings()) {
            Binding binding = findBinding(def);
            if (binding == null) {
                String msgStr = "Binding " + bindingName + " doesn't exists in WSDL.";
                org.apache.cxf.common.i18n.Message msg = new org.apache.cxf.common.i18n.Message(msgStr, LOG);
                throw new Exception(msg.toString());
            }
            generateIDL(def, binding);
        } else {
            // generate idl for all bindings in the file.
            // each idl file will have the name of the binding.
            Map bindingList = def.getAllBindings();
            if (bindingList.size() == 0) {
                String msgStr = "No bindings exists within this WSDL.";
                org.apache.cxf.common.i18n.Message msg = new org.apache.cxf.common.i18n.Message(msgStr, LOG);
                throw new Exception(msg.toString());
            } else {
                List<QName> portTypes = new ArrayList<QName>();
                Iterator iter = bindingList.values().iterator();
                while (iter.hasNext()) {
                    Binding binding = (Binding)iter.next();
                    List ext = binding.getExtensibilityElements();
                    if (!(ext.get(0) instanceof BindingType)) {
                        continue;
                    }
                    if (portTypes.contains(binding.getPortType().getQName())) {
                        continue;
                    } else {
                        portTypes.add(binding.getPortType().getQName());
                    }
                    generateIDL(def, binding);
                    root = IdlRoot.create();
                }
            }
        }
        printWriter.close();
        
    }

    
    public void addExtensions(ExtensionRegistry extRegistry) throws JAXBException {
        extReg = extRegistry;
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

    private void generateIDL(Definition definition, Binding binding) {
        List ext = binding.getExtensibilityElements();
        if (!(ext.get(0) instanceof BindingType)) {
            // throw an error not a corba binding
            throw new RuntimeException(binding.getQName() + " is not a corba binding, "
                                       + "please pass a corba binding/porttype to use");
        }    

        String nm[] = unscopeName(binding.getPortType().getQName().getLocalPart());
        int pos = nm[nm.length - 1].lastIndexOf("Binding");

        if (pos != -1) {
            nm[nm.length - 1] = nm[nm.length - 1].substring(0, pos);
        }

        IdlScopeBase parent = root;

        if (nm.length > 1) {
            for (int i = 0; i < nm.length - 1; ++i) {
                IdlModule mod = IdlModule.create(parent, nm[i]);
                parent.addToScope(mod);
                parent = mod;
            }
        }
        intf = IdlInterface.create(parent, nm[nm.length - 1]);
        parent.holdForScope(intf);
        try {
            getAllIdlTypes();
            collectIdlDefns(binding);
            root.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        parent.promoteHeldToScope();
        root.write(printWriter);
    }
    
    private void collectIdlDefns(Binding binding) throws Exception {
        boolean isOneway = false;
        Iterator iterator = binding.getBindingOperations().iterator();
        while (iterator.hasNext()) {
            BindingOperation bindingOperation = (BindingOperation)iterator.next();
            if (bindingOperation.getBindingOutput() == null) {
                isOneway = true;
            }

            addOperation(bindingOperation, isOneway);
        }
    }

    private void addOperation(BindingOperation bindingOperation, 
                              boolean isOneway) throws Exception {

        String name = null;
        Iterator i = bindingOperation.getExtensibilityElements().iterator();
        while (i.hasNext()) {
            org.apache.cxf.binding.corba.wsdl.OperationType opType = 
                (org.apache.cxf.binding.corba.wsdl.OperationType)i
                .next();
            name = opType.getName();

            if (name.startsWith("_get_") || name.startsWith("_set_")) {
                createIdlAttribute(opType, name);
            } else {
                createIdlOperation(opType, name, isOneway);
            }
            root.flush();
        }
    }

    public void createIdlAttribute(org.apache.cxf.binding.corba.wsdl.OperationType 
                                   opType, String name) throws Exception {          
        String attrNm = name.substring(5, name.length());
        IdlAttribute attr;
        IdlDefn idlDef = intf.lookup(attrNm);

        if (idlDef == null) {
            if (name.startsWith("_get_")) {
                ArgType t = opType.getReturn();
                attr = IdlAttribute.create(intf, attrNm, 
                                           findType(t.getIdltype()), true);                               
            } else {
                Iterator it = opType.getParam().iterator();
                ParamType arg = (ParamType)it.next();                
                attr = IdlAttribute.create(intf, attrNm, findType(arg.getIdltype()), false);                
            }
            intf.addAttribute(attr);
        } else {
            attr = (IdlAttribute)idlDef;

            if (attr.readonly() && name.startsWith("_set_")) {
                attr.setReadonly(false);
            }
        }
    }

    public void createIdlOperation(org.apache.cxf.binding.corba.wsdl.OperationType opType, String name,
                                   boolean isOneway) throws Exception {

        IdlOperation idlOp = IdlOperation.create(intf, opType.getName(), isOneway);
        intf.holdForScope(idlOp);

        ArgType crt = opType.getReturn();

        if (crt != null) {
            IdlType rt = findType(crt.getIdltype());            
            idlOp.addReturnType(rt);
        }

        Iterator it = opType.getParam().iterator();

        while (it.hasNext()) {
            ParamType arg = (ParamType)it.next();
            IdlType type = findType(arg.getIdltype());            
            String mode = arg.getMode().value();
            IdlParam param = IdlParam.create(idlOp, arg.getName(), type, mode);
            idlOp.addParameter(param);
        }
        
        Iterator iter = opType.getRaises().iterator();

        while (iter.hasNext()) {
            RaisesType rs = (RaisesType)iter.next();
            IdlType type = findType(rs.getException());            

            if (type instanceof IdlException) {
                idlOp.addException((IdlException)type);
            } else {
                String msgStr = type.fullName() + " is not a type.";
                org.apache.cxf.common.i18n.Message msg = 
                    new org.apache.cxf.common.i18n.Message(msgStr, LOG);
                throw new Exception(msg.toString());                
            }
        }

        root.flush();
        intf.promoteHeldToScope();
    }

    private void getAllIdlTypes() throws Exception {

        try {
            Iterator types = def.getExtensibilityElements().iterator();
            TypeMappingType typeMappingType = null;            
            if (types != null) {
                while (types.hasNext()) {                    
                    typeMappingType = (TypeMappingType)types.next();
                }
            }
            if (typeMappingType != null) {
                Iterator i = typeMappingType.getStructOrExceptionOrUnion().iterator();
                while (i.hasNext()) {
                    CorbaTypeImpl corbaTypeImpl = (CorbaTypeImpl)i.next();                                
                    findCorbaIdlType(corbaTypeImpl);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private CorbaTypeImpl getCorbaType(QName qname) throws Exception {    
        CorbaTypeImpl corbaTypeImpl = null;

        try {

            Iterator types = def.getExtensibilityElements().iterator();
            TypeMappingType typeMappingType = null;
            if (types != null) {
                while (types.hasNext()) {
                    typeMappingType = (TypeMappingType)types.next();
                }
            }
            Iterator i = typeMappingType.getStructOrExceptionOrUnion().iterator();
            while (i.hasNext()) {
                CorbaTypeImpl corbaType = (CorbaTypeImpl)i.next();
                if (corbaType.getName().equals(qname.getLocalPart())) {                    
                    return corbaType;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
                
        return corbaTypeImpl;         
    }

    private IdlType findType(QName qname) throws Exception {        
        String local = qname.getLocalPart();        
        return findIdlType(local, qname, null);                
    }

    private IdlType findCorbaIdlType(CorbaTypeImpl corbaTypeImpl) throws Exception {        
        String local = corbaTypeImpl.getName();        
        return findIdlType(local, corbaTypeImpl.getType(), corbaTypeImpl);                
    }
    
    private IdlType findIdlType(String local, QName ntype, 
                                  CorbaTypeImpl corbatypeImpl) throws Exception {
        IdlType idlType = null;
        
        if (ntype.getNamespaceURI().equals(CorbaConstants.NU_WSDL_CORBA)) {
            try {
                idlType = createPrimitiveType(ntype, local);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            if (CorbaUtils.isTimeBaseDef(local)) {
                root.addInclude("<omg/TimeBase.idl>");
            }

            String name[] = unscopeName(local);
            IdlDefn defn = root.lookup(name);            
            
            if (defn != null) {
                if (defn instanceof IdlType) {
                    return (IdlType)defn;
                } else {
                    String msgStr = local + " is an incorrect idltype.";
                    org.apache.cxf.common.i18n.Message msg = 
                        new org.apache.cxf.common.i18n.Message(msgStr, LOG);
                    throw new Exception(msg.toString());
                }
            } else {
                try {
                    idlType = createType(ntype, name, corbatypeImpl);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        return idlType;
    }
    
    protected IdlType createPrimitiveType(QName idlType, String name) throws Exception {
        IdlDefn result = root.lookup(name);

        if (result != null          
            &&  (!(result instanceof IdlType))) {
            String msgStr = idlType.getLocalPart() + " is an incorrect idltype.";
            org.apache.cxf.common.i18n.Message msg = 
                new org.apache.cxf.common.i18n.Message(msgStr, LOG);
            throw new Exception(msg.toString());   
        }

        /**
         * If we find a corba:dateTime then add the TimeBase.idl to the include
         * list for the root.
         */
        if (idlType.equals(CorbaConstants.NT_CORBA_DATETIME)) {
            root.addInclude("<omg/TimeBase.idl>");
        }
        return (IdlType)result;
    }
       
    protected IdlType createType(QName idlType, String name[], CorbaTypeImpl corbaType) throws Exception {
        if (idlType.getLocalPart().equals("CORBA.Object")) {
            return IdlInterface.create(null, "Object");
        }        

        CorbaTypeImpl  corbaTypeImpl = corbaType;
        if (corbaTypeImpl == null) {
            corbaTypeImpl = getCorbaType(idlType);
        }
        
        if (corbaTypeImpl == null) {
            String msgStr = "Type " + idlType.getLocalPart() + " not found.";
            org.apache.cxf.common.i18n.Message msg = 
                new org.apache.cxf.common.i18n.Message(msgStr, LOG);
            throw new Exception(msg.toString());                
        }
        
        IdlScopeBase scope = root;
        String dotScopedName = "";

        for (int i = 0; i < name.length - 1; ++i) {
            dotScopedName += name[i];

            // If we have the name CORBA, we need to make sure we are not handling the CORBA.Object
            // name which is used for object references.  If so, we don't want to generate a module
            // since it is not a type we need to define in our IDL.  This only happens when the 
            // name is "CORBA", we have a name array of length 2 and we are at the beginning of the
            // name array.
            if ("CORBA".equals(dotScopedName)
                && name.length == 2 && i == 0
                && name[1].equals("Object")) {
                break;
            }
            
            IdlDefn idlDef = scope.lookup(name[i]);

            if (idlDef == null) {
                // Before creating module, check to see if a Corba type
                // represent this name aleady exists.
                // For example if type is a.b.c and we are about to create
                // module b,look to see if a.b
                // is an interface that needs to be processed
                QName qname = new QName(corbaTypeImpl.getType().getNamespaceURI(), dotScopedName);

                // Check to see if CORBAType exists. If so, create type for it
                // otherwise
                // create module for this scope               
                CorbaTypeImpl possibleCorbaType = getCorbaType(qname);

                if (possibleCorbaType != null) {
                    idlDef = findType(qname);                    
                }

                if (idlDef == null) {
                    idlDef = IdlModule.create(scope, name[i]);
                    scope.addToScope(idlDef);
                }
            }

            dotScopedName += ".";
            scope = (IdlScopeBase)idlDef;
        }

        IdlType result = null;
        String local = name[name.length - 1];

        if (corbaTypeImpl instanceof Enum) {
            result = createEnum(corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl instanceof Sequence) {
            result = createSequence(corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl instanceof Anonsequence) {
            result = createAnonSequence(corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl 
            instanceof org.apache.cxf.binding.corba.wsdl.Exception) {
            result = createIdlException(corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl instanceof Struct) {
            result = createStruct(corbaTypeImpl, scope, local);            
        } else if (corbaTypeImpl instanceof Union) {
            result = createUnion(corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl instanceof Alias) {
            result = createTypedef(corbaTypeImpl, scope, local);        
        } else if (corbaTypeImpl instanceof Array) {
            result = createArray(corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl instanceof Anonarray) {
            result = createAnonArray(corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl instanceof Fixed) {
            result = createFixed(corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl instanceof Anonfixed) {
            result = createAnonFixed(corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl instanceof Const) {
            result = createConst(corbaTypeImpl, scope, local);        
        } else {
            result = checkAnon(corbaTypeImpl, scope, local);                                    
        } 
        
        if (result == null
            && corbaTypeImpl instanceof Object) {
            result = createInterface(corbaTypeImpl, scope, local);            
        }
        
        return result; 
    }

    private IdlType checkAnon(CorbaTypeImpl corbaTypeImpl, IdlScopeBase scope, 
                              String local) throws Exception {
        IdlType result = null;
        
        if (corbaTypeImpl instanceof Anonstring) {
            Anonstring as = (Anonstring)corbaTypeImpl;   
            Long lbound = as.getBound();
            int bound = lbound.intValue();  
            result = IdlString.create(bound);        
        }
        return result;
    }

    private IdlType createInterface(CorbaTypeImpl ctype, IdlScopeBase scope, String local) 
        throws Exception {
    
        IdlType result = null;
        
        org.apache.cxf.binding.corba.wsdl.Object obj = 
            (org.apache.cxf.binding.corba.wsdl.Object)ctype;
        QName bqname = obj.getBinding();        

        Binding binding = def.getBinding(bqname);
        if (binding != null) {
            IdlDefn defn = scope.lookup(local);

            if (defn instanceof IdlInterface) {
                return (IdlInterface)defn;
            } else if (defn == null) {
                try {
                    IdlInterface storedIntf = intf;
                    intf = IdlInterface.create(scope, local);
                    scope.holdForScope(intf);
                    collectIdlDefns(binding);
                    scope.promoteHeldToScope();
                    result = intf;
                    intf = storedIntf;
                } catch (Exception ex) {
                    String msgStr = "Interface type " + intf.fullName() + " not found.";
                    org.apache.cxf.common.i18n.Message msg = 
                        new org.apache.cxf.common.i18n.Message(msgStr, LOG);
                    throw new Exception(msg.toString());
                }                
            }
        }
        return result;
    }
    
    private IdlType createIdlException(CorbaTypeImpl ctype, IdlScopeBase scope, 
                                       String local) throws Exception {
        IdlType result = null;
        org.apache.cxf.binding.corba.wsdl.Exception e = 
            (org.apache.cxf.binding.corba.wsdl.Exception)ctype;

        Object obj = scope.lookup(local);

        if (obj != null && (obj instanceof IdlException)) {
            result = (IdlType)obj;
        } else {
            IdlException exc = IdlException.create(scope, local);
            scope.holdForScope(exc);

            Iterator it = e.getMember().iterator();

            while (it.hasNext()) {
                MemberType m = (MemberType)it.next();
                QName qname = m.getIdltype();
                IdlType type = findType(qname);                
                exc.addToScope(IdlField.create(exc, m.getName(), type));
            }

            result = exc;
            scope.promoteHeldToScope();
        }

        return result;        
    }
    
    private IdlType createUnion(CorbaTypeImpl ctype, IdlScopeBase scope, 
                                String local) throws Exception {
        Union u = (Union)ctype;
        boolean undefinedCircular = false;
        IdlType disc = findType(u.getDiscriminator());
        IdlUnion union = IdlUnion.create(scope, local, disc);
        scope.holdForScope(union);

        Iterator it = u.getUnionbranch().iterator();
        while (it.hasNext()) {
            Unionbranch ub = (Unionbranch)it.next();
            QName qname = ub.getIdltype();
            IdlType bt = findType(qname);
            boolean isDefault = false;
            if (ub.isSetDefault()) {
                isDefault = ub.isDefault();
            }
            IdlUnionBranch b = IdlUnionBranch.create(union, ub.getName(), bt, isDefault);

            Iterator it2 = ub.getCase().iterator();
            while (it2.hasNext()) {                
                b.addCase(((CaseType)it2.next()).getLabel());
            }

            // Ensure that this union will not  be written until all of its circular members are
            // defined, unless the undefined circular members are of sequence type.

            if (!undefinedCircular && !(bt instanceof IdlSequence)) {
                String mlocal = qname.getLocalPart();
                String mname[] = unscopeName(mlocal);
                undefinedCircular = null != root.lookup(mname, true);
            }

            union.addBranch(b);
        }

        if (undefinedCircular) {
            scope.parkHeld();
        } else {
            scope.promoteHeldToScope();
            if (union.isCircular()) {
                // resolving this union closed a recursion
                scope.flush();
            }
        }
        return union;
    }
    
    private IdlType createStruct(CorbaTypeImpl ctype, IdlScopeBase scope, 
                                 String local) throws Exception {
        Struct s = (Struct)ctype;
        boolean undefinedCircular = false;
        IdlStruct struct = IdlStruct.create(scope, local);
        scope.holdForScope(struct);

        Iterator it = s.getMember().iterator();
        while (it.hasNext()) {
            MemberType m = (MemberType)it.next();
            QName qname = m.getIdltype();
            IdlType type = findType(qname);            
            
            // Ensure that this struct will not be written until 
            // all of its circular members are defined, unless 
            // the undefined circular members are of sequence type.

            if (!undefinedCircular && !(type instanceof IdlSequence)) {
                String mlocal = qname.getLocalPart();
                String mname[] = unscopeName(mlocal);
                undefinedCircular = null != root.lookup(mname, true);
            }

            struct.addToScope(IdlField.create(struct, m.getName(), type));
        }

        if (undefinedCircular) {
            scope.parkHeld();
        } else {
            scope.promoteHeldToScope();
            if (struct.isCircular()) {
                // resolving this struct closed a recursion
                scope.flush();
            }
        }
        
        return struct;        
    }
    
    private IdlType createTypedef(CorbaTypeImpl ctype, IdlScopeBase scope, 
                                  String local) throws Exception {
        IdlType idlType = null;
        Alias a = (Alias)ctype;
        IdlType base = findType(a.getBasetype());                               
        idlType = IdlTypedef.create(scope, local, base);
        scope.addToScope(idlType);
        return idlType;
    }
    
    private IdlType createConst(CorbaTypeImpl ctype, IdlScopeBase scope, 
                                String local) throws Exception {
        IdlType idlType = null;
        Const c = (Const)ctype;
        IdlType base = findType(c.getIdltype());
        String value = c.getValue(); 
        idlType = IdlConst.create(scope, local, base, value);
        scope.addToScope(idlType);
        return idlType;
    }
    
    private IdlType createSequence(CorbaTypeImpl ctype, IdlScopeBase scope, 
                                   String local) throws Exception {
        IdlType idlType = null;
        Sequence s = (Sequence)ctype;
        IdlType base = findType(s.getElemtype());        
        int bound = (int)s.getBound();
        idlType = IdlSequence.create(scope, local, base, bound);
        scope.addToScope(idlType);
        return idlType;
    }
    
    private IdlType createAnonSequence(CorbaTypeImpl ctype, IdlScopeBase scope, 
                                       String local)  throws Exception {
        IdlType idlType = null;
        Anonsequence s = (Anonsequence)ctype;
        IdlType base = findType(s.getElemtype());        
        int bound = (int)s.getBound();
        idlType = IdlAnonSequence.create(scope, base, bound);
        scope.addToScope(idlType);
        return idlType;
    }
    
    private IdlType createArray(CorbaTypeImpl ctype, IdlScopeBase scope, String local) 
        throws Exception {
        IdlType idlType = null;
        Array s = (Array)ctype;
        IdlType base = findType(s.getElemtype());        
        int bound = (int)s.getBound();
        idlType = IdlArray.create(scope, local, base, bound);
        scope.addToScope(idlType);
        return idlType;
    }
    
    private IdlType createAnonArray(CorbaTypeImpl ctype, IdlScopeBase scope, String local) 
        throws Exception {
        IdlType idlType = null;
        Anonarray s = (Anonarray)ctype;
        IdlType base = findType(s.getElemtype());        
        int bound = (int)s.getBound();
        idlType = IdlAnonArray.create(scope, base, bound);
        scope.addToScope(idlType);
        return idlType;
    }
    
    private IdlType createFixed(CorbaTypeImpl ctype, IdlScopeBase scope, String local) {
        IdlType idlType = null;
        Fixed f = (Fixed)ctype;     
        Long digits = f.getDigits();
        Long scale = f.getScale();        
        idlType = IdlFixed.create(scope, local, digits.intValue(),   
                                  scale.intValue());
        scope.addToScope(idlType);
        return idlType;
    }
    
    private IdlType createAnonFixed(CorbaTypeImpl ctype, IdlScopeBase scope, String local) {
        IdlType idlType = null;
        Anonfixed f = (Anonfixed)ctype;     
        Long digits = f.getDigits();
        Long scale = f.getScale();        
        idlType = IdlAnonFixed.create(scope, digits.intValue(), scale.intValue());
        scope.addToScope(idlType);
        return idlType;
    }

    private IdlType createEnum(CorbaTypeImpl ctype, IdlScopeBase scope, String local) {
        Enum e = (Enum)ctype;
        IdlEnum enum1 = IdlEnum.create(scope, local);
        Iterator it = e.getEnumerator().iterator();

        while (it.hasNext()) {
            // Enumerators are created in the same scope
            // as the enum, according to IDL grammar rules.
            String n = ((Enumerator)it.next()).getValue();
            IdlEnumerator enumerator = IdlEnumerator.create(scope, n);
            scope.addToScope(enumerator);
            enum1.addEnumerator(enumerator);
        }
        scope.addToScope(enum1);
        return enum1;
    }

    private PrintWriter createPrintWriter(String filename) throws Exception {
        OutputStream out = factory.createOutputStream(filename);
        return new PrintWriter(out);
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

        factory = new FileOutputStreamFactory(dir);
    }

    private Binding findBinding(Definition definition) {
        Binding binding = null;
        Map bindings = definition.getBindings();
        Iterator i = bindings.values().iterator();
        
        if (bindingName != null) {                    
            while (i.hasNext()) {
                binding = (Binding)i.next();
                if (binding.getQName().getLocalPart().equals(bindingName)) {
                    return binding;
                }
            }
        } else {
            if (bindings.size() >= 1) {
                binding = (Binding)i.next();
            }
            
        }
        return binding;
    }

    private String[] unscopeName(String nm) {
        StringTokenizer strtok = new StringTokenizer(nm, ".");
        String result[] = new String[strtok.countTokens()];

        for (int i = 0; strtok.hasMoreTokens(); ++i) {
            result[i] = new String(strtok.nextToken());
        }

        return result;
    }
        
    public void setOutputFile(String file) {
        outputFile = file;
    }
    
    public void setPrintWriter(PrintWriter pw) {
        printWriter = pw;
    }
 
    public void setWsdlFile(String file) {
        wsdlFileName = new String(file);
    }

    public void setVerboseOn(boolean verbose) {
        verboseOn = verbose;
    }
    public boolean isVerboseOn() {
        return verboseOn;
    }

    public void setBindingName(String bindName) {
        bindingName = new String(bindName);
    }
    
    public String getBindingName() {
        return bindingName;
    }

    public void setNamespace(String namespaceName) {
        namespace = new String(namespaceName);
    }
    
    public String getNamespace() {
        return namespace;
    }
    
    public void setGenerateAllBindings(boolean all) {
        generateAllBindings = all;
    }
    
    public boolean isGenerateAllBindings() {
        return generateAllBindings;
    }

}
