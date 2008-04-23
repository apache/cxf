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

import org.apache.cxf.binding.corba.wsdl.CaseType;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.binding.corba.wsdl.Union;
import org.apache.cxf.binding.corba.wsdl.Unionbranch;
import org.apache.cxf.tools.corba.common.ReferenceConstants;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaType;

public class UnionVisitor extends VisitorBase {

    public UnionVisitor(Scope scope,
                        Definition defn,
                        XmlSchema schemaRef,
                        WSDLASTVisitor wsdlVisitor) {
        super(scope, defn, schemaRef, wsdlVisitor);
    }
    
    public static boolean accept(AST node) {
        if (node.getType() == IDLTokenTypes.LITERAL_union) {
            return true;
        }
        return false;
    }
    
    public void visit(AST unionNode) {
        // <union_type> ::= "union" <identifier> "switch" "(" <switch_type_spec> ")"
        //                  "{" <switch_body> "}"
        // <switch_type_spec> ::= <integer_type>
        //                      | <char_type>
        //                      | <boolean_type>
        //                      | <enum_type>
        //                      | <scoped_type>
        // <switch_body> ::= <case>+
        // <case> ::= <case_label>+ <element_spec> ";"
        // <case_label> ::= "case" <const_expr> ":"
        //                | "default" ":"
        // <element_spec> ::= <type_spec> <declarator>
        
        
        AST identifierNode = unionNode.getFirstChild();
        // Check if its a forward declaration
        if (identifierNode.getFirstChild() == null && identifierNode.getNextSibling() == null) {
            visitForwardDeclaredUnion(identifierNode);
        } else {
            visitDeclaredUnion(identifierNode);
        }
    }
    
    public void visitDeclaredUnion(AST identifierNode) {        
        
        Scope unionScope = new Scope(getScope(), identifierNode);
        AST discriminatorNode = identifierNode.getNextSibling();
        AST caseNode = discriminatorNode.getNextSibling();
        // xmlschema:union
        XmlSchemaComplexType unionSchemaComplexType = new XmlSchemaComplexType(schema);
        unionSchemaComplexType.setName(mapper.mapToQName(unionScope));
        
        // REVISIT
        // TEMPORARILY 
        // using TypesVisitor to visit <const_type>
        // it should be visited by a SwitchTypeSpecVisitor
        TypesVisitor visitor = new TypesVisitor(getScope(), definition, schema, wsdlVisitor, null);
        visitor.visit(discriminatorNode);
        CorbaTypeImpl ctype = visitor.getCorbaType();
        Scope fullyQualifiedName = visitor.getFullyQualifiedName();
        
        XmlSchemaChoice choice = new XmlSchemaChoice();
        choice.setMinOccurs(1);
        choice.setMaxOccurs(1);
        unionSchemaComplexType.setParticle(choice);
        
        
        // corba:union
        Union corbaUnion = new Union();
        corbaUnion.setQName(new QName(typeMap.getTargetNamespace(), unionScope.toString()));
        corbaUnion.setRepositoryID(unionScope.toIDLRepositoryID());
        corbaUnion.setType(unionSchemaComplexType.getQName());
        if (ctype != null) {
            corbaUnion.setDiscriminator(ctype.getQName());
        } else {
            // Discriminator type is forward declared.
            UnionDeferredAction unionDiscriminatorAction = 
                new UnionDeferredAction(corbaUnion);
            wsdlVisitor.getDeferredActions().add(fullyQualifiedName, unionDiscriminatorAction);
        }
       
        boolean recursiveAdd = addRecursiveScopedName(identifierNode);

        processCaseNodes(caseNode, unionScope, choice, corbaUnion);

        if (recursiveAdd) {
            removeRecursiveScopedName(identifierNode);
        }

        // add schemaType
        schema.getItems().add(unionSchemaComplexType);
        schema.addType(unionSchemaComplexType);

        // add corbaType
        typeMap.getStructOrExceptionOrUnion().add(corbaUnion);

        // REVISIT: are these assignments needed?
        setSchemaType(unionSchemaComplexType);
        setCorbaType(corbaUnion);
        
        // Need to check if the union was forward declared
        processForwardUnionActions(unionScope);

        // Once we've finished declaring the union, we should make sure it has been removed from
        // the list of scopedNames so that we indicate that is no longer simply forward declared.
        scopedNames.remove(unionScope);
    }
    
    private void processCaseNodes(AST caseNode,
                                  Scope scope,
                                  XmlSchemaChoice choice,
                                  Union corbaUnion) {
        while (caseNode != null) {
            AST typeNode  = null;
            AST nameNode  = null;
            AST labelNode = null;
            
            // xmlschema:element
            XmlSchemaElement element = new XmlSchemaElement();

            // corba:unionbranch
            Unionbranch unionBranch = new Unionbranch();

            if (caseNode.getType() == IDLTokenTypes.LITERAL_default) {
                // default:
                unionBranch.setDefault(true);
                
                typeNode = caseNode.getFirstChild();
                nameNode = typeNode.getNextSibling();
            } else {
                // case:
                createCase(caseNode, unionBranch);
                
                labelNode = caseNode.getFirstChild();
                if (labelNode.getType() == IDLTokenTypes.LITERAL_case) {
                    labelNode = labelNode.getNextSibling();
                }
                
                typeNode = labelNode.getNextSibling();
                nameNode = typeNode.getNextSibling();
            }
            

            TypesVisitor visitor = new TypesVisitor(scope,
                                                    definition,
                                                    schema,
                                                    wsdlVisitor,
                                                    null);
            visitor.visit(typeNode);
            XmlSchemaType stype = visitor.getSchemaType();
            CorbaTypeImpl ctype = visitor.getCorbaType();
            Scope fullyQualifiedName = visitor.getFullyQualifiedName();
            
            
            // needed for anonymous arrays in unions
            if (ArrayVisitor.accept(nameNode)) {
                Scope anonScope = new Scope(scope, TypesUtils.getCorbaTypeNameNode(nameNode));
                ArrayVisitor arrayVisitor = new ArrayVisitor(anonScope,
                                                             definition,
                                                             schema,
                                                             wsdlVisitor,
                                                             null,
                                                             fullyQualifiedName);
                arrayVisitor.setSchemaType(stype);
                arrayVisitor.setCorbaType(ctype);
                arrayVisitor.visit(nameNode);
                stype = arrayVisitor.getSchemaType();
                ctype = arrayVisitor.getCorbaType();
                fullyQualifiedName = visitor.getFullyQualifiedName();
            }
            
            
            // xmlschema:element
            element.setName(nameNode.toString());
            if (stype != null) {
                element.setSchemaTypeName(stype.getQName());
                if (stype.getQName().equals(ReferenceConstants.WSADDRESSING_TYPE)) {
                    element.setNillable(true);
                }
            } else {
                UnionDeferredAction elementAction = 
                    new UnionDeferredAction(element);
                wsdlVisitor.getDeferredActions().add(fullyQualifiedName, elementAction); 
            }
            choice.getItems().add(element);
            
            
            // corba:unionbranch
            unionBranch.setName(nameNode.toString());
            if (ctype != null) {
                unionBranch.setIdltype(ctype.getQName());
            } else {
                // its type is forward declared.
                UnionDeferredAction unionBranchAction = 
                    new UnionDeferredAction(unionBranch);
                wsdlVisitor.getDeferredActions().add(fullyQualifiedName, unionBranchAction); 
            }
            corbaUnion.getUnionbranch().add(unionBranch);
            
            caseNode = caseNode.getNextSibling();
        }
    }
    
    private void createCase(AST caseNode, Unionbranch unionBranch) {
        AST node = caseNode.getFirstChild();
        if (node != null) {
            if (node.getType() == IDLTokenTypes.LITERAL_case) {
                // corba:case
                CaseType caseType = new CaseType();
                caseType.setLabel(node.getNextSibling().toString());
                unionBranch.getCase().add(caseType);
                
                // recursive call
                createCase(node, unionBranch);
            } else {
                // corba:case
                CaseType caseType = new CaseType();
                caseType.setLabel(node.toString());
                unionBranch.getCase().add(caseType);
            }
        }
    }
    
    private void visitForwardDeclaredUnion(AST identifierNode) {
        String unionName = identifierNode.toString();        
        Scope unionScope = new Scope(getScope(), unionName);
        
        ScopeNameCollection scopedNames = wsdlVisitor.getScopedNames();
        if (scopedNames.getScope(unionScope) == null) {
            scopedNames.add(unionScope);
        }        
    }
 
    // Process any actions that were defered for a forward declared union
    private void processForwardUnionActions(Scope unionScope) {
        if (wsdlVisitor.getDeferredActions() != null) {
            DeferredActionCollection deferredActions = wsdlVisitor.getDeferredActions();
            List list = deferredActions.getActions(unionScope);
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
