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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
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
import org.apache.cxf.binding.corba.wsdl.CorbaType;
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
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.tools.common.ToolException;
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
        if (printWriter == null) {
            printWriter = createPrintWriter(outputFile);
        }
        try (PrintWriter pw = printWriter != null ? printWriter : createPrintWriter(outputFile)) {
            if (!isGenerateAllBindings()) {
                Binding binding = findBinding(def);
                if (binding == null) {
                    String msgStr = "Binding " + bindingName + " doesn't exists in WSDL.";
                    org.apache.cxf.common.i18n.Message msg = new org.apache.cxf.common.i18n.Message(msgStr, LOG);
                    throw new ToolException(msg.toString());
                }
                generateIDL(def, binding);
            } else {
                // generate idl for all bindings in the file.
                // each idl file will have the name of the binding.
                Collection<Binding> bindings = CastUtils.cast(def.getAllBindings().values());
                if (bindings.isEmpty()) {
                    String msgStr = "No bindings exists within this WSDL.";
                    org.apache.cxf.common.i18n.Message msg = new org.apache.cxf.common.i18n.Message(msgStr, LOG);
                    throw new ToolException(msg.toString());
                }
                List<QName> portTypes = new ArrayList<>();
                for (Binding binding : bindings) {
                    List<?> ext = binding.getExtensibilityElements();
                    if (!(ext.get(0) instanceof BindingType)) {
                        continue;
                    }
                    if (portTypes.contains(binding.getPortType().getQName())) {
                        continue;
                    }
                    portTypes.add(binding.getPortType().getQName());
                    generateIDL(def, binding);
                    root = IdlRoot.create();
                }
            }
        }
    }

    private void generateIDL(Definition definition, Binding binding) {
        List<?> ext = binding.getExtensibilityElements();
        if (!(ext.get(0) instanceof BindingType)) {
            // throw an error not a corba binding
            throw new RuntimeException(binding.getQName() + " is not a corba binding, "
                                       + "please pass a corba binding/porttype to use");
        }

        String[] nm = unscopeName(binding.getPortType().getQName().getLocalPart());
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
        Iterator<?> iterator = binding.getBindingOperations().iterator();
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

        Iterator<?> i = bindingOperation.getExtensibilityElements().iterator();
        while (i.hasNext()) {
            org.apache.cxf.binding.corba.wsdl.OperationType opType =
                (org.apache.cxf.binding.corba.wsdl.OperationType)i
                .next();
            String name = opType.getName();

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
                ParamType arg = opType.getParam().iterator().next();
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

        for (ParamType arg : opType.getParam()) {
            IdlType type = findType(arg.getIdltype());
            String mode = arg.getMode().value();
            IdlParam param = IdlParam.create(idlOp, arg.getName(), type, mode);
            idlOp.addParameter(param);
        }

        for (RaisesType rs : opType.getRaises()) {
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
            TypeMappingType typeMappingType = getTypeMappingType();
            if (typeMappingType != null) {
                for (CorbaType corbaTypeImpl
                    : typeMappingType.getStructOrExceptionOrUnion()) {
                    findCorbaIdlType(corbaTypeImpl);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private CorbaType getCorbaType(QName qname) throws Exception {
        CorbaType corbaTypeImpl = null;

        try {
            TypeMappingType typeMappingType = getTypeMappingType();
            if (typeMappingType != null) {
                for (CorbaType corbaType : typeMappingType.getStructOrExceptionOrUnion()) {
                    if (corbaType.getName().equals(qname.getLocalPart())) {
                        return corbaType;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return corbaTypeImpl;
    }
    private TypeMappingType getTypeMappingType() {
        Iterator<?> types = def.getExtensibilityElements().iterator();
        if (types != null) {
            while (types.hasNext()) {
                return (TypeMappingType)types.next();
            }
        }
        return null;
    }
    private IdlType findType(QName qname) throws Exception {
        String local = qname.getLocalPart();
        return findIdlType(local, qname, null);
    }

    private IdlType findCorbaIdlType(CorbaType corbaTypeImpl) throws Exception {
        String local = corbaTypeImpl.getName();
        return findIdlType(local, corbaTypeImpl.getType(), corbaTypeImpl);
    }

    private IdlType findIdlType(String local, QName ntype,
                                  CorbaType corbatypeImpl) throws Exception {
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

            String[] name = unscopeName(local);
            IdlDefn defn = root.lookup(name);

            if (defn != null) {
                if (defn instanceof IdlType) {
                    return (IdlType)defn;
                }
                String msgStr = local + " is an incorrect idltype.";
                org.apache.cxf.common.i18n.Message msg =
                    new org.apache.cxf.common.i18n.Message(msgStr, LOG);
                throw new Exception(msg.toString());
            }
            try {
                idlType = createType(ntype, name, corbatypeImpl);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return idlType;
    }

    protected IdlType createPrimitiveType(QName idlType, String name) throws Exception {
        IdlDefn result = root.lookup(name);

        if (result != null
            &&  !(result instanceof IdlType)) {
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

    protected IdlType createType(QName idlType, String[] name, CorbaType corbaType) throws Exception {
        if ("CORBA.Object".equals(idlType.getLocalPart())) {
            return IdlInterface.create(null, "Object");
        }

        CorbaType corbaTypeImpl = corbaType;
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
        StringBuilder dotScopedName = new StringBuilder("");

        for (int i = 0; i < name.length - 1; ++i) {
            dotScopedName.append(name[i]);

            // If we have the name CORBA, we need to make sure we are not handling the CORBA.Object
            // name which is used for object references.  If so, we don't want to generate a module
            // since it is not a type we need to define in our IDL.  This only happens when the
            // name is "CORBA", we have a name array of length 2 and we are at the beginning of the
            // name array.
            if ("CORBA".equals(dotScopedName.toString())
                && name.length == 2 && i == 0
                && "Object".equals(name[1])) {
                break;
            }

            IdlDefn idlDef = scope.lookup(name[i]);

            if (idlDef == null) {
                // Before creating module, check to see if a Corba type
                // represent this name aleady exists.
                // For example if type is a.b.c and we are about to create
                // module b, look to see if a.b
                // is an interface that needs to be processed
                QName qname = new QName(corbaTypeImpl.getType().getNamespaceURI(), dotScopedName.toString());

                // Check to see if CORBAType exists. If so, create type for it
                // otherwise
                // create module for this scope
                CorbaType possibleCorbaType = getCorbaType(qname);

                if (possibleCorbaType != null) {
                    idlDef = findType(qname);
                }

                if (idlDef == null) {
                    idlDef = IdlModule.create(scope, name[i]);
                    scope.addToScope(idlDef);
                }
            }

            dotScopedName.append('.');
            scope = (IdlScopeBase)idlDef;
        }

        IdlType result;
        String local = name[name.length - 1];

        if (corbaTypeImpl instanceof Enum) {
            result = createEnum((Enum)corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl instanceof Sequence) {
            result = createSequence((Sequence)corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl instanceof Anonsequence) {
            result = createAnonSequence((Anonsequence)corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl
            instanceof org.apache.cxf.binding.corba.wsdl.Exception) {
            result = createIdlException((org.apache.cxf.binding.corba.wsdl.Exception)corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl instanceof Struct) {
            result = createStruct((Struct)corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl instanceof Union) {
            result = createUnion((Union)corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl instanceof Alias) {
            result = createTypedef((Alias)corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl instanceof Array) {
            result = createArray((Array)corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl instanceof Anonarray) {
            result = createAnonArray((Anonarray)corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl instanceof Fixed) {
            result = createFixed((Fixed)corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl instanceof Anonfixed) {
            result = createAnonFixed((Anonfixed)corbaTypeImpl, scope, local);
        } else if (corbaTypeImpl instanceof Const) {
            result = createConst((Const)corbaTypeImpl, scope, local);
        } else {
            result = checkAnon(corbaTypeImpl, scope, local);
        }

        if (result == null && corbaTypeImpl instanceof org.apache.cxf.binding.corba.wsdl.Object) {
            result = createInterface((org.apache.cxf.binding.corba.wsdl.Object)corbaTypeImpl, scope, local);
        }

        return result;
    }

    private IdlType checkAnon(CorbaType corbaTypeImpl, IdlScopeBase scope,
                              String local) throws Exception {
        IdlType result = null;

        if (corbaTypeImpl instanceof Anonstring) {
            Anonstring as = (Anonstring)corbaTypeImpl;
            long lbound = as.getBound();
            result = IdlString.create((int)lbound);
        }
        return result;
    }

    private IdlType createInterface(org.apache.cxf.binding.corba.wsdl.Object obj, IdlScopeBase scope, String local)
        throws Exception {

        IdlType result = null;

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

    private IdlType createIdlException(org.apache.cxf.binding.corba.wsdl.Exception e, IdlScopeBase scope,
                                       String local) throws Exception {
        final IdlType result;

        Object obj = scope.lookup(local);

        if (obj instanceof IdlException) {
            result = (IdlType)obj;
        } else {
            IdlException exc = IdlException.create(scope, local);
            scope.holdForScope(exc);
            for (MemberType m : e.getMember()) {
                QName qname = m.getIdltype();
                IdlType type = findType(qname);
                exc.addToScope(IdlField.create(exc, m.getName(), type));
            }
            result = exc;
            scope.promoteHeldToScope();
        }

        return result;
    }

    private IdlType createUnion(Union u, IdlScopeBase scope,
                                String local) throws Exception {
        boolean undefinedCircular = false;
        IdlType disc = findType(u.getDiscriminator());
        IdlUnion union = IdlUnion.create(scope, local, disc);
        scope.holdForScope(union);

        for (Unionbranch ub : u .getUnionbranch()) {
            QName qname = ub.getIdltype();
            IdlType bt = findType(qname);
            boolean isDefault = false;
            if (ub.isSetDefault()) {
                isDefault = ub.isDefault();
            }
            IdlUnionBranch b = IdlUnionBranch.create(union, ub.getName(), bt, isDefault);

            for (CaseType cs : ub.getCase()) {
                b.addCase(cs.getLabel());
            }

            // Ensure that this union will not  be written until all of its circular members are
            // defined, unless the undefined circular members are of sequence type.

            if (!undefinedCircular && !(bt instanceof IdlSequence)) {
                String mlocal = qname.getLocalPart();
                String[] mname = unscopeName(mlocal);
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

    private IdlType createStruct(Struct s, IdlScopeBase scope,
                                 String local) throws Exception {
        boolean undefinedCircular = false;
        IdlStruct struct = IdlStruct.create(scope, local);
        scope.holdForScope(struct);

        for (MemberType m : s.getMember()) {
            QName qname = m.getIdltype();
            IdlType type = findType(qname);

            // Ensure that this struct will not be written until
            // all of its circular members are defined, unless
            // the undefined circular members are of sequence type.

            if (!undefinedCircular && !(type instanceof IdlSequence)) {
                String mlocal = qname.getLocalPart();
                String[] mname = unscopeName(mlocal);
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

    private IdlType createTypedef(Alias a, IdlScopeBase scope,
                                  String local) throws Exception {
        IdlType base = findType(a.getBasetype());
        IdlType idlType = IdlTypedef.create(scope, local, base);
        scope.addToScope(idlType);
        return idlType;
    }

    private IdlType createConst(Const c, IdlScopeBase scope,
                                String local) throws Exception {
        IdlType base = findType(c.getIdltype());
        String value = c.getValue();
        IdlType idlType = IdlConst.create(scope, local, base, value);
        scope.addToScope(idlType);
        return idlType;
    }

    private IdlType createSequence(Sequence s, IdlScopeBase scope,
                                   String local) throws Exception {
        IdlType base = findType(s.getElemtype());
        int bound = (int)s.getBound();
        IdlType idlType = IdlSequence.create(scope, local, base, bound);
        scope.addToScope(idlType);
        return idlType;
    }

    private IdlType createAnonSequence(Anonsequence s, IdlScopeBase scope,
                                       String local)  throws Exception {
        IdlType base = findType(s.getElemtype());
        int bound = (int)s.getBound();
        IdlType idlType = IdlAnonSequence.create(scope, base, bound);
        scope.addToScope(idlType);
        return idlType;
    }

    private IdlType createArray(Array s, IdlScopeBase scope, String local)
        throws Exception {
        IdlType base = findType(s.getElemtype());
        int bound = (int)s.getBound();
        IdlType idlType = IdlArray.create(scope, local, base, bound);
        scope.addToScope(idlType);
        return idlType;
    }

    private IdlType createAnonArray(Anonarray s, IdlScopeBase scope, String local)
        throws Exception {
        IdlType base = findType(s.getElemtype());
        int bound = (int)s.getBound();
        IdlType idlType = IdlAnonArray.create(scope, base, bound);
        scope.addToScope(idlType);
        return idlType;
    }

    private IdlType createFixed(Fixed f, IdlScopeBase scope, String local) {
        long digits = f.getDigits();
        long scale = f.getScale();
        IdlType idlType = IdlFixed.create(scope, local, (int)digits, (int)scale);
        scope.addToScope(idlType);
        return idlType;
    }

    private IdlType createAnonFixed(Anonfixed f, IdlScopeBase scope, String local) {
        long digits = f.getDigits();
        long scale = f.getScale();
        IdlType idlType = IdlAnonFixed.create(scope, (int)digits, (int)scale);
        scope.addToScope(idlType);
        return idlType;
    }

    private IdlType createEnum(Enum e, IdlScopeBase scope, String local) {
        IdlEnum enum1 = IdlEnum.create(scope, local);
        Iterator<Enumerator> it = e.getEnumerator().iterator();

        while (it.hasNext()) {
            // Enumerators are created in the same scope
            // as the enum, according to IDL grammar rules.
            String n = it.next().getValue();
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
        Collection<Binding> bindings = CastUtils.cast(definition.getBindings().values());
        if (bindingName != null) {
            for (Binding b : bindings) {
                binding = b;
                if (binding.getQName().getLocalPart().equals(bindingName)) {
                    return binding;
                }
            }
        } else {
            if (!bindings.isEmpty()) {
                binding = bindings.iterator().next();
            }
        }
        return binding;
    }

    private String[] unscopeName(String nm) {
        StringTokenizer strtok = new StringTokenizer(nm, ".");
        String[] result = new String[strtok.countTokens()];

        for (int i = 0; strtok.hasMoreTokens(); ++i) {
            result[i] = strtok.nextToken();
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
        wsdlFileName = file;
    }

    public void setVerboseOn(boolean verbose) {
        verboseOn = verbose;
    }
    public boolean isVerboseOn() {
        return verboseOn;
    }

    public void setBindingName(String bindName) {
        bindingName = bindName;
    }

    public String getBindingName() {
        return bindingName;
    }

    public void setNamespace(String namespaceName) {
        namespace = namespaceName;
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
