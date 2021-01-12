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

import jakarta.wsdl.Definition;
import jakarta.wsdl.Message;
import jakarta.wsdl.Part;
import javax.xml.namespace.QName;

import antlr.collections.AST;

import org.apache.cxf.binding.corba.wsdl.CorbaType;
import org.apache.cxf.binding.corba.wsdl.MemberType;
import org.apache.cxf.tools.corba.common.ReferenceConstants;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaType;

public class ExceptionVisitor extends VisitorBase {

    private static final String TYPE_SUFFIX = "Type";

    public ExceptionVisitor(Scope scope,
                            Definition defn,
                            XmlSchema schemaRef,
                            WSDLASTVisitor wsdlASTVisitor) {
        super(scope, defn, schemaRef, wsdlASTVisitor);
    }

    public static boolean accept(AST node) {
        return node.getType() == IDLTokenTypes.LITERAL_exception;
    }

    public void visit(AST node) {
        // <exception_dcl> ::= "exception" <identifier> "{" <member>* "}"
        // <member> ::= <type_spec> <declarators> ";"

        // <type_spec> visited by TypesVisitor

        // Following should be visited by a separate visitor
        // <declarators> ::= <declarator> { "," <declarator> }*
        // <declarator> ::= <simple_declarator>
        //                | <complex_declarator>
        // <simple_declarator> ::= <identifier>
        // <complex_declarator> ::= <array_declarator>
        // <array_declarator> ::= <identifier> <fixed_array_size>+
        // <fixed_array_size> ::= "[" <positive_int_const> "]"


        AST identifierNode = node.getFirstChild();
        Scope exceptionScope = new Scope(getScope(), identifierNode);

        // xmlschema:exception
        Scope scopedName = new Scope(getScope(), identifierNode);
        String exceptionName = mapper.mapToQName(scopedName);
        XmlSchemaElement element = new XmlSchemaElement(schema, true);
        element.setName(mapper.mapToQName(scopedName));

        String exceptionTypeName = exceptionName + TYPE_SUFFIX;

        XmlSchemaComplexType complexType = new XmlSchemaComplexType(schema, true);
        complexType.setName(exceptionTypeName);
        //complexType.setQName(new QName(schema.getTargetNamespace(), exceptionTypeName));
        XmlSchemaSequence sequence = new XmlSchemaSequence();
        complexType.setParticle(sequence);

        element.setSchemaTypeName(complexType.getQName());

        // corba:exception
        org.apache.cxf.binding.corba.wsdl.Exception exception
            = new org.apache.cxf.binding.corba.wsdl.Exception();
        exception.setQName(new QName(typeMap.getTargetNamespace(), exceptionName));
        exception.setType(complexType.getQName());
        exception.setRepositoryID(scopedName.toIDLRepositoryID());


        // exception members
        AST memberTypeNode = identifierNode.getNextSibling();
        while (memberTypeNode != null) {
            AST memberNode = TypesUtils.getCorbaTypeNameNode(memberTypeNode);

            TypesVisitor visitor = new TypesVisitor(exceptionScope,
                                                    definition,
                                                    schema,
                                                    wsdlVisitor,
                                                    null);
            visitor.visit(memberTypeNode);
            XmlSchemaType stype = visitor.getSchemaType();
            CorbaType ctype = visitor.getCorbaType();
            Scope fullyQualifiedName = visitor.getFullyQualifiedName();

            // needed for anonymous arrays in exceptions
            if (ArrayVisitor.accept(memberNode)) {
                Scope anonScope = new Scope(exceptionScope,
                                            TypesUtils.getCorbaTypeNameNode(memberTypeNode));
                ArrayVisitor arrayVisitor = new ArrayVisitor(anonScope,
                                                             definition,
                                                             schema,
                                                             wsdlVisitor,
                                                             null,
                                                             fullyQualifiedName);
                arrayVisitor.setSchemaType(stype);
                arrayVisitor.setCorbaType(ctype);
                arrayVisitor.visit(memberNode);
                stype = arrayVisitor.getSchemaType();
                ctype = arrayVisitor.getCorbaType();
            }


            XmlSchemaElement member = createElementType(memberNode, stype,
                                                        fullyQualifiedName);
            sequence.getItems().add(member);

            MemberType memberType = createMemberType(memberNode, ctype,
                                                     fullyQualifiedName);
            exception.getMember().add(memberType);


            memberTypeNode = memberNode.getNextSibling();
        }

        // add exception to corba typemap
        typeMap.getStructOrExceptionOrUnion().add(exception);

        setSchemaType(complexType);
        setCorbaType(exception);
        createFaultMessage(element.getQName());
    }

    private void createFaultMessage(QName qname) {
        String exceptionName = qname.getLocalPart();
        // messages
        Message faultMsg = definition.createMessage();

        faultMsg.setQName(new QName(definition.getTargetNamespace(), exceptionName));
        faultMsg.setUndefined(false);
        // message - part
        Part part = definition.createPart();
        part.setName("exception");
        part.setElementName(qname);
        faultMsg.addPart(part);

        //add the fault element namespace to the definition
        String nsURI = qname.getNamespaceURI();
        manager.addWSDLDefinitionNamespace(definition, mapper.mapNSToPrefix(nsURI), nsURI);

        definition.addMessage(faultMsg);
    }

    private XmlSchemaElement createElementType(AST memberNode, XmlSchemaType stype,
                                               Scope fqName) {
        // xmlschema:member
        XmlSchemaElement member = new XmlSchemaElement(schema, false);
        String memberName = memberNode.toString();
        member.setName(memberName);
        if (stype != null) {
            member.setSchemaType(stype);
            member.setSchemaTypeName(stype.getQName());
            if (stype.getQName().equals(ReferenceConstants.WSADDRESSING_TYPE)) {
                member.setNillable(true);
            }
        } else {
            wsdlVisitor.getDeferredActions().
                add(fqName, new ExceptionDeferredAction(member));
        }
        return member;
    }

    private MemberType createMemberType(AST memberNode, CorbaType ctype,
                                        Scope fqName) {
        // corba:member
        MemberType memberType = new MemberType();
        memberType.setName(memberNode.toString());
        if (ctype != null) {
            memberType.setIdltype(ctype.getQName());
        } else {
            wsdlVisitor.getDeferredActions().
                add(fqName, new ExceptionDeferredAction(memberType));
        }

        return memberType;
    }

}
