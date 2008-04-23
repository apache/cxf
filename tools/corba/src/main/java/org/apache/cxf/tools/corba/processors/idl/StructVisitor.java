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

import java.util.Iterator;
import java.util.List;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import antlr.collections.AST;

import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.binding.corba.wsdl.MemberType;
import org.apache.cxf.binding.corba.wsdl.Struct;
import org.apache.cxf.tools.corba.common.ReferenceConstants;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaType;

public class StructVisitor extends VisitorBase {
    
    public StructVisitor(Scope scope,
                         Definition defn,
                         XmlSchema schemaRef,
                         WSDLASTVisitor wsdlVisitor) {
        super(scope, defn, schemaRef, wsdlVisitor);
    }

    public static boolean accept(AST node) {
        if (node.getType() == IDLTokenTypes.LITERAL_struct) {
            return true;
        }
        return false;
    }
    
    public void visit(AST node) {
        // <struct_type> ::= "struct" <identifier> "{" <member_list> "}"
        // <member_list> ::= <member>+
        // <member> ::= <type_spec> <declarators> ";"

        AST identifierNode = node.getFirstChild();
        // Check if its a forward declaration
        if (identifierNode.getFirstChild() == null && identifierNode.getNextSibling() == null) {
            visitForwardDeclaredStruct(identifierNode);
        } else {
            visitDeclaredStruct(identifierNode);
        }
    }
        
    public void visitDeclaredStruct(AST identifierNode) {
        Scope structScope = new Scope(getScope(), identifierNode);        

        // xmlschema:struct
        XmlSchemaComplexType complexType = new XmlSchemaComplexType(schema);
        complexType.setName(mapper.mapToQName(structScope));
        XmlSchemaSequence sequence = new XmlSchemaSequence();
        complexType.setParticle(sequence);
        
        // corba:struct
        Struct struct = new Struct();
        struct.setQName(new QName(typeMap.getTargetNamespace(), structScope.toString()));
        struct.setType(complexType.getQName());
        struct.setRepositoryID(structScope.toIDLRepositoryID());

        boolean recursiveAdd = addRecursiveScopedName(identifierNode);

        // struct members
        visitStructMembers(identifierNode, struct, sequence, structScope);        
        
        if (recursiveAdd) {
            removeRecursiveScopedName(identifierNode);
        }
        // add schemaType
        schema.getItems().add(complexType);
        schema.addType(complexType);

        // add corbaType
        typeMap.getStructOrExceptionOrUnion().add(struct);
        
        // REVISIT: are there assignment needed?
        setSchemaType(complexType);
        setCorbaType(struct);
        
        // Need to check if the struct was forward declared
        processForwardStructActions(structScope);

        // Once we've finished declaring the struct, we should make sure it has been removed from
        // the list of scopedNames so that we inidicate that is no longer simply forward declared.
        scopedNames.remove(structScope);
    }
    
    private void visitStructMembers(AST identifierNode, Struct struct,
                                    XmlSchemaSequence sequence,
                                    Scope structScope) {
        AST memberTypeNode = identifierNode.getNextSibling();
        while (memberTypeNode != null) {
            AST memberNode = TypesUtils.getCorbaTypeNameNode(memberTypeNode);
            
            XmlSchemaType schemaType = null;
            CorbaTypeImpl corbaType = null;
            Scope fqName = null;
            try {
                TypesVisitor visitor = new TypesVisitor(structScope,
                                                        definition,
                                                        schema,
                                                        wsdlVisitor,
                                                        null);
                visitor.visit(memberTypeNode);                
                schemaType = visitor.getSchemaType();
                corbaType = visitor.getCorbaType();
                fqName = visitor.getFullyQualifiedName();                
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            // Handle multiple struct member declarators
            // <declarators> :== <declarator> { "," <declarator> }*
            //
            // A multiple declarator must be an identifier (i.e. of type IDENT)
            // and cannot be a previous declared (or forward declared) type
            // (hence the ScopedNameVisitor.accept() call).
            while (memberNode != null
                   && memberNode.getType() == IDLTokenTypes.IDENT
                   && !ScopedNameVisitor.accept(structScope, definition, schema, memberNode, wsdlVisitor)) {

                XmlSchemaType memberSchemaType = schemaType;
                CorbaTypeImpl memberCorbaType = corbaType;
                // needed for anonymous arrays in structs 
                if (ArrayVisitor.accept(memberNode)) {
                    Scope anonScope = new Scope(structScope,
                                                TypesUtils.getCorbaTypeNameNode(memberTypeNode));
                    ArrayVisitor arrayVisitor = new ArrayVisitor(anonScope,
                                                                 definition,
                                                                 schema,
                                                                 wsdlVisitor,
                                                                 null,
                                                                 fqName);
                    arrayVisitor.setSchemaType(schemaType);
                    arrayVisitor.setCorbaType(corbaType);
                    arrayVisitor.visit(memberNode);
                    memberSchemaType = arrayVisitor.getSchemaType();
                    memberCorbaType = arrayVisitor.getCorbaType();
                    fqName = arrayVisitor.getFullyQualifiedName();
                }

                XmlSchemaElement member = 
                    createXmlSchemaElement(memberNode, memberSchemaType, fqName);
                sequence.getItems().add(member);           
                MemberType memberType = 
                    createMemberType(memberNode, memberCorbaType, fqName);            
                struct.getMember().add(memberType);
                
                memberNode = memberNode.getNextSibling();
            }
            
            memberTypeNode = memberNode;
        }
    }
    
    private XmlSchemaElement createXmlSchemaElement(AST memberNode, 
                                                    XmlSchemaType schemaType,
                                                    Scope fqName) {
        // xmlschema:member
        XmlSchemaElement member = new XmlSchemaElement();
        String memberName = memberNode.toString();
        member.setName(memberName);
        member.setSchemaType(schemaType);
        if (schemaType != null) {
            member.setSchemaTypeName(schemaType.getQName());
            if (schemaType.getQName().equals(ReferenceConstants.WSADDRESSING_TYPE)) {
                member.setNillable(true);
            }
        } else {
            wsdlVisitor.getDeferredActions().
                add(fqName, new StructDeferredAction(member));
        }
        return member;
    }
    
    private MemberType createMemberType(AST memberNode,
                                        CorbaTypeImpl corbaType,
                                        Scope fqName) {
        // corba:member
        String memberName = memberNode.toString();
        MemberType memberType = new MemberType();
        memberType.setName(memberName);
        if (corbaType != null) {                            
            memberType.setIdltype(corbaType.getQName());
        } else {
            wsdlVisitor.getDeferredActions().
                add(fqName, new StructDeferredAction(memberType)); 
        }
        return memberType;
    }
    
    private void visitForwardDeclaredStruct(AST identifierNode) {
        String structName = identifierNode.toString();        
        Scope structScope = new Scope(getScope(), structName);
        
        ScopeNameCollection scopedNames = wsdlVisitor.getScopedNames();
        if (scopedNames.getScope(structScope) == null) {
            scopedNames.add(structScope);
        }        
    }
 
    // Process any actions that were defered for a forward declared struct
    private void processForwardStructActions(Scope structScope) {
        if (wsdlVisitor.getDeferredActions() != null) {
            DeferredActionCollection deferredActions = wsdlVisitor.getDeferredActions();
            List list = deferredActions.getActions(structScope);
            if ((list != null) && !list.isEmpty()) {
                XmlSchemaType stype = getSchemaType();
                CorbaTypeImpl ctype = getCorbaType();
                Iterator iterator = list.iterator();                    
                while (iterator.hasNext()) {
                    SchemaDeferredAction action = (SchemaDeferredAction)iterator.next();
                    action.execute(stype, ctype);                       
                }
                iterator = list.iterator();                    
                while (iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();                       
                }                                          
            }            
        }   
    }

    private boolean addRecursiveScopedName(AST identifierNode) {
        String structName = identifierNode.toString();        
        Scope structScope = new Scope(getScope(), structName);
        
        ScopeNameCollection scopedNames = wsdlVisitor.getScopedNames();
        if (scopedNames.getScope(structScope) == null) {
            scopedNames.add(structScope);
            return true;
        }
        return false;
    }

    private void removeRecursiveScopedName(AST identifierNode) {
        String structName = identifierNode.toString();        
        Scope structScope = new Scope(getScope(), structName);
        
        ScopeNameCollection scopedNames = wsdlVisitor.getScopedNames();
        scopedNames.remove(structScope);
    }


}
