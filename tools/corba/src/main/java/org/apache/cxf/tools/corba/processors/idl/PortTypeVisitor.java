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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Operation;
import javax.wsdl.PortType;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.xml.namespace.QName;

import antlr.collections.AST;

import org.apache.cxf.binding.corba.wsdl.BindingType;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.helpers.CastUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaType;

public class PortTypeVisitor extends VisitorBase {   

    ExtensionRegistry extReg;
    PortType portType;
    Definition rootDefinition;

    public PortTypeVisitor(Scope scope,
                           Definition defn,
                           XmlSchema schemaRef,
                           WSDLASTVisitor wsdlASTVisitor) {
        super(scope, defn, schemaRef, wsdlASTVisitor);
        extReg = definition.getExtensionRegistry();
        rootDefinition = wsdlASTVisitor.getDefinition();
    }

    public static boolean accept(AST node) {
        if (node.getType() == IDLTokenTypes.LITERAL_interface) {
            return true;
        }
        return false;
    }
    
    public void visit(AST node) {
        // <interface> ::= <interface_dcl>
        //               | <forward_dcl>
        // <interface_dcl> ::= <interface_header> "{" <interface_body> "}"
        // <forward_dcl> ::= ["abstract" | "local"] "interface" <identifier>
        // <interface_header> ::= ["abstract" | "local"] "interface" <identifier>
        //                        [<interface_inheritance_spec>]
        // <interface_body> ::= <export>*
        // <export> ::= <type_dcl> ";"
        //            | <const_dcl> ";"
        //            | <except_dcl> ";"
        //            | <attr_dcl> ";"
        //            | <op_dcl> ";"
        // <interface_inheritance_spec> ::= ":" <interface_name> { "," <interface_name> }*
        // <interface_name> ::= <scoped_name>
        
        
        AST identifierNode = node.getFirstChild();        
        
        // Check if its a forward declaration
        if (identifierNode.getFirstChild() == null && identifierNode.getNextSibling() == null) {
            visitForwardDeclaredInterface(identifierNode);        
        } else {
            visitInterface(identifierNode);                       
        }       
    }

    // Visits a fully declared interface
    private void visitInterface(AST identifierNode) {
        try {
            String interfaceName = identifierNode.toString();        
            Scope interfaceScope = new Scope(getScope(), interfaceName);
            portType = definition.createPortType();

            String portTypeName = interfaceScope.toString();

            XmlSchema newSchema = schema;
            if (!mapper.isDefaultMapping()) {
                portTypeName = interfaceScope.tail();
                //add a schema based on the interface
                String tns = mapper.map(interfaceScope);
                newSchema = manager.createXmlSchemaForDefinition(definition, tns, schemas);
                definition.addNamespace(interfaceScope.toString("_"), tns);
            }
            String tns = definition.getTargetNamespace();
            portType.setQName(new QName(tns, portTypeName));
            definition.addPortType(portType);
            portType.setUndefined(false);

            Binding binding = createBinding(interfaceScope.toString());
        
            AST specNode = identifierNode.getNextSibling();        
            if  (specNode.getType() == IDLTokenTypes.LCURLY) {
                specNode = specNode.getNextSibling();
            }
        
            AST exportNode = null;        
            if (specNode.getType() == IDLTokenTypes.RCURLY) {
                exportNode = specNode.getNextSibling();        
            } else if (specNode.getType() == IDLTokenTypes.COLON) {
                exportNode = visitInterfaceInheritanceSpec(specNode, binding, interfaceScope);
                exportNode = exportNode.getNextSibling();
            } else {
                exportNode = specNode;
            }
           
            while (exportNode != null  
                   && exportNode.getType() != IDLTokenTypes.RCURLY) {
            
                if (TypeDclVisitor.accept(exportNode)) {
                    TypeDclVisitor visitor = new TypeDclVisitor(interfaceScope,
                                                                definition,
                                                                newSchema,
                                                                wsdlVisitor);
                    visitor.visit(exportNode);
                } else if (ConstVisitor.accept(exportNode)) {
                    ConstVisitor visitor = new ConstVisitor(interfaceScope,
                                                            definition,
                                                            newSchema,
                                                            wsdlVisitor);
                    visitor.visit(exportNode);
                } else if (ExceptionVisitor.accept(exportNode)) {
                    ExceptionVisitor visitor = new ExceptionVisitor(interfaceScope,
                                                                    definition,
                                                                    newSchema,
                                                                    wsdlVisitor);
                    visitor.visit(exportNode);
                } else if (AttributeVisitor.accept(exportNode)) {
                    AttributeVisitor attributeVisitor = new AttributeVisitor(interfaceScope,
                                                                             definition,
                                                                             newSchema,
                                                                             wsdlVisitor,
                                                                             portType,
                                                                             binding);
                    attributeVisitor.visit(exportNode);
                } else if (OperationVisitor.accept(interfaceScope,
                                                   definition,
                                                   newSchema, 
                                                   exportNode,
                                                   wsdlVisitor)) {
                    OperationVisitor visitor = new OperationVisitor(interfaceScope,
                                                                    definition,
                                                                    newSchema,
                                                                    wsdlVisitor,
                                                                    portType,
                                                                    binding);
                    visitor.visit(exportNode);
                } else {
                    throw new RuntimeException("[InterfaceVisitor] Invalid IDL: unknown element "
                                               + exportNode.toString());
                }
            
                exportNode = exportNode.getNextSibling();
            }

            // Once we've finished declaring the interface, we should make sure it has been removed 
            // from the list of scopedNames so that we indicate that is no longer simply forward
            // declared.
            Scope scopedName = new Scope(getScope(), identifierNode);
            scopedNames.remove(scopedName);
        
            if (wsdlVisitor.getDeferredActions() != null) {
                handleDeferredActions(wsdlVisitor.getDeferredActions(), scopedName, identifierNode);
            }

            if (!mapper.isDefaultMapping()) {
                manager.deferAttachSchemaToWSDL(definition, newSchema, false);
                //manager.attachSchemaToWSDL(definition, newSchema, false);
            }

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }           
    }
    
    private void handleDeferredActions(DeferredActionCollection deferredActions,
                                       Scope scopedName,
                                       AST identifierNode) {
        List list = deferredActions.getActions(scopedName);
        if ((list != null) && !list.isEmpty()) {
            XmlSchemaType stype = null;
            CorbaTypeImpl ctype = null;
            if (ObjectReferenceVisitor.accept(getScope(), schema, 
                                              definition, identifierNode, wsdlVisitor)) {
                ObjectReferenceVisitor visitor = new ObjectReferenceVisitor(getScope(),
                                                                            definition,
                                                                            schema,
                                                                            wsdlVisitor);
                visitor.visit(identifierNode);
                stype = visitor.getSchemaType();
                ctype = visitor.getCorbaType();
            }
            Iterator iterator = list.iterator();
            while (iterator.hasNext()) {
                DeferredAction action = (DeferredAction) iterator.next();
                if (action instanceof SchemaDeferredAction
                    && (stype != null) && (ctype != null)) {
                    SchemaDeferredAction schemaAction = (SchemaDeferredAction) action;
                    schemaAction.execute(stype, ctype);
                }
            }
            deferredActions.removeScope(scopedName);
        }
    }

    
    public Binding createBinding(String scopedPortTypeName) {
        String bname = scopedPortTypeName + "CORBABinding";
        QName bqname = new QName(rootDefinition.getTargetNamespace(),
                                 bname);
        int count = 0;
        while (queryBinding(bqname)) {
            bname = bname + count;
            bqname = new QName(rootDefinition.getTargetNamespace(), bname);
        }
        Binding binding = rootDefinition.createBinding();
        binding.setPortType(portType);
        binding.setQName(bqname);

        try {
            BindingType bindingType = (BindingType)
                extReg.createExtension(Binding.class, CorbaConstants.NE_CORBA_BINDING);
            bindingType.setRepositoryID(CorbaConstants.REPO_STRING
                                        + scopedPortTypeName.replace('.', '/')
                                        + CorbaConstants.IDL_VERSION);
            binding.addExtensibilityElement(bindingType);
        } catch (WSDLException ex) {
            throw new RuntimeException(ex);
        }
        binding.setUndefined(false);
        rootDefinition.addBinding(binding);
        return binding;
    }

    private boolean queryBinding(QName bqname) {
        Map bindings = definition.getBindings();
        Iterator i = bindings.values().iterator();
        while (i.hasNext()) {
            Binding binding = (Binding)i.next();
            if (binding.getQName().getLocalPart().equals(bqname.getLocalPart())) {
                return true;
            }
        }
        return false;
    }
    
    private AST visitInterfaceInheritanceSpec(AST interfaceInheritanceSpecNode, Binding binding,
                                              Scope childScope) {
        // <interface_inheritance_spec> ::= ":" <interface_name> { "," <interface_name> }*
                
        AST interfaceNameNode = interfaceInheritanceSpecNode.getFirstChild();
        BindingType corbaBinding = findCorbaBinding(binding);
        List<Scope> inheritedScopes = new ArrayList<Scope>();
        
        while (interfaceNameNode != null) {            
            //check for porttypes in current & parent scopes
            Scope interfaceScope = null;
            PortType intf = null;
            if (ScopedNameVisitor.isFullyScopedName(interfaceNameNode)) {
                interfaceScope = ScopedNameVisitor.getFullyScopedName(new Scope(), interfaceNameNode);
                intf = findPortType(interfaceScope);
            }
            Scope currentScope = getScope();
            while (intf == null
                   && currentScope != currentScope.getParent()) {
                if (ScopedNameVisitor.isFullyScopedName(interfaceNameNode)) {
                    interfaceScope = ScopedNameVisitor.getFullyScopedName(currentScope, interfaceNameNode);
                } else {
                    interfaceScope = new Scope(currentScope, interfaceNameNode.toString());
                }
                intf = findPortType(interfaceScope);
                currentScope = currentScope.getParent();
            }
            
            if (intf == null) {
                if (ScopedNameVisitor.isFullyScopedName(interfaceNameNode)) {
                    interfaceScope = ScopedNameVisitor.getFullyScopedName(new Scope(), interfaceNameNode);
                } else {
                    interfaceScope = new Scope(new Scope(), interfaceNameNode);
                }
                intf = findPortType(interfaceScope);
            }

            if (intf == null) {
                throw new RuntimeException("[InterfaceVisitor] Unknown Interface: "
                                           + interfaceNameNode.toString());
            }

            Scope defnScope = interfaceScope.getParent();
            Definition defn = manager.getWSDLDefinition(mapper.map(defnScope));
            inheritedScopes.add(interfaceScope);
            
            if (defn != null && !defn.getTargetNamespace().equals(definition.getTargetNamespace())) {
                String key = defnScope.toString("_");
                String fileName = getWsdlVisitor().getOutputDir()
                    + System.getProperty("file.separator")
                    + key;
                manager.addWSDLDefinitionImport(definition,
                                                defn,
                                                key,
                                                fileName);
            }                        
            
            Binding inheritedBinding = findBinding(intf);
            BindingType inheritedCorbaBinding = findCorbaBinding(inheritedBinding);
            corbaBinding.getBases().add(inheritedCorbaBinding.getRepositoryID());

            //add all the operations of the inherited port type.            
            for (Operation op : CastUtils.cast(intf.getOperations(), Operation.class)) {

                //check to see all the inherited namespaces are added.
                String inputNS = op.getInput().getMessage().getQName().getNamespaceURI();
                manager.addWSDLDefinitionNamespace(definition, mapper.mapNSToPrefix(inputNS), inputNS);

                // Make sure we import the wsdl for the input namespace
                if (definition.getImports().get(inputNS) == null && !mapper.isDefaultMapping()
                    && !definition.getTargetNamespace().equals(inputNS)) {
                    manager.addWSDLDefinitionImport(definition, 
                                                    manager.getWSDLDefinition(inputNS), 
                                                    mapper.mapNSToPrefix(inputNS), 
                                                    manager.getImportedWSDLDefinitionFile(inputNS));
                }

                String outputNS = op.getOutput().getMessage().getQName().getNamespaceURI();
                manager.addWSDLDefinitionNamespace(definition, mapper.mapNSToPrefix(outputNS), outputNS);

                // Make sure we import the wsdl for the output namespace
                if (definition.getImports().get(outputNS) == null && !mapper.isDefaultMapping()
                    && !definition.getTargetNamespace().equals(outputNS)) {
                    manager.addWSDLDefinitionImport(definition, 
                                                    manager.getWSDLDefinition(outputNS), 
                                                    mapper.mapNSToPrefix(outputNS), 
                                                    manager.getImportedWSDLDefinitionFile(outputNS));
                }
                
                for (Iterator<Fault> faults = CastUtils.cast(op.getFaults().values().iterator());
                    faults.hasNext();) {
                    
                    String faultNS = faults.next().getMessage().getQName().getNamespaceURI();
                    manager.addWSDLDefinitionNamespace(definition, mapper.mapNSToPrefix(faultNS), faultNS);
                    // Make sure we import the wsdl for the fault namespace
                    if (definition.getImports().get(faultNS) == null && !mapper.isDefaultMapping()
                        && !definition.getTargetNamespace().equals(faultNS)) {
                        manager.addWSDLDefinitionImport(definition,
                                                        manager.getWSDLDefinition(faultNS), 
                                                        mapper.mapNSToPrefix(faultNS), 
                                                        manager.getImportedWSDLDefinitionFile(faultNS));
                    }
                }
                
                portType.addOperation(op);
            }

            //add all the binding extensions of the inherited corba binding
            for (Iterator<BindingOperation> it = 
                    CastUtils.cast(inheritedBinding.getBindingOperations().iterator());
                 it.hasNext();) {
                binding.addBindingOperation(it.next());
            }
            interfaceNameNode = interfaceNameNode.getNextSibling();
        }
        
        if ((!inheritedScopes.isEmpty()) 
            && (wsdlVisitor.getInheritedScopeMap() != null)) {
            wsdlVisitor.getInheritedScopeMap().put(childScope, inheritedScopes);             
        }
        
        return interfaceInheritanceSpecNode.getNextSibling();
    }
    
    private void visitForwardDeclaredInterface(AST identifierNode) {
        String interfaceName = identifierNode.toString();        
        Scope interfaceScope = new Scope(getScope(), interfaceName);
        
        ScopeNameCollection scopedNames = wsdlVisitor.getScopedNames();
        if (scopedNames.getScope(interfaceScope) == null) {
            scopedNames.add(interfaceScope);
        }
        
    }


    private PortType findPortType(Scope intfScope) {
        String tns = mapper.map(intfScope.getParent());
        String intfName = intfScope.toString();
        Definition defn = definition;
        if (tns != null) {           
            defn = manager.getWSDLDefinition(tns);
            intfName = intfScope.tail();
        }
        if (defn != null) {
            tns = defn.getTargetNamespace();
            QName name = new QName(tns, intfName);
            return defn.getPortType(name);
        }
        return null;
    }

    private Binding findBinding(PortType intf) {
        Object[] bindings = rootDefinition.getBindings().values().toArray();   
        for (int i = 0; i < bindings.length; i++) {
            Binding binding = (Binding) bindings[i];
            if (binding.getPortType().getQName().equals(intf.getQName())) {
                return binding;
            }
        }
        throw new RuntimeException("[InterfaceVisitor] Couldn't find binding for porttype "
                                   + intf.getQName());
    }

    private BindingType findCorbaBinding(Binding binding) {
        java.util.List list = binding.getExtensibilityElements();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof BindingType) {
                return (BindingType) list.get(i);
            }
        }
        throw new RuntimeException("[InterfaceVisitor] Couldn't find Corba binding in Binding "
                                   + binding.getQName());
    }
}
