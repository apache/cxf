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

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import antlr.collections.AST;

import org.apache.cxf.binding.corba.wsdl.Alias;
import org.apache.cxf.binding.corba.wsdl.Anonstring;
import org.apache.cxf.binding.corba.wsdl.Anonwstring;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaMaxLengthFacet;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.constants.Constants;

public class StringVisitor extends VisitorBase {

    private AST stringNode;
    private AST boundNode;
    private AST identifierNode;
    private Scope stringScopedName;

    
    public StringVisitor(Scope scope,
                         Definition definition,
                         XmlSchema schemaRef,
                         WSDLASTVisitor wsdlVisitor,
                         AST identifierNodeRef) {
        super(scope, definition, schemaRef, wsdlVisitor);
        stringNode = null;
        boundNode = null;
        identifierNode = identifierNodeRef;
        stringScopedName = null;
    }

    public static boolean accept(AST node) {
        if ((node.getType() == IDLTokenTypes.LITERAL_string) 
            || (node.getType() == IDLTokenTypes.LITERAL_wstring)) {
            return true;
        }
        return false;
    }

    public static boolean isBounded(AST node) {
        if (node.getFirstChild() == null) {
            return false;
        }
        return true;
    }
    
    public void visit(AST node) {
        // <string_type> ::= "string" "<" <positive_int_const> ">"
        //                 | "string"
        // <wstring_type> ::= "wstring" "<" <positive_int_const> ">"
        //                  | "wstring"

        
        stringNode = node;
        boundNode = stringNode.getFirstChild();

        if (identifierNode == null) {    
            stringScopedName = TypesUtils.generateAnonymousScopedName(getScope(), schema);
        } else {
            if (identifierNode.getFirstChild() == null) {
                stringScopedName = new Scope(getScope(), identifierNode);
            } else {
                // array of anonymous bounded string
                Scope anonScope = new Scope(getScope(), identifierNode);
                stringScopedName = TypesUtils.generateAnonymousScopedName(anonScope, schema);
                identifierNode = null;
            } 
        }
        
        if (boundNode != null
            && !wsdlVisitor.getBoundedStringOverride()) {
            if (identifierNode != null) {
                // bounded string/wstring
                visitBoundedString();

            } else {
                // anonymous bounded string/wstring
                visitAnonBoundedString();
            }
        } else {
            // unbounded string/wstring
            visitUnboundedString();
        }
    }
    
    private void visitAnonBoundedString() {
        // xmlschema:bounded anon string
        XmlSchemaSimpleType simpleType = new XmlSchemaSimpleType(schema);
        simpleType.setName(stringScopedName.toString());
        XmlSchemaSimpleTypeRestriction restriction = new XmlSchemaSimpleTypeRestriction();
        restriction.setBaseTypeName(Constants.XSD_STRING);
        XmlSchemaMaxLengthFacet maxLengthFacet = new XmlSchemaMaxLengthFacet();
        maxLengthFacet.setValue(boundNode.toString());
        restriction.getFacets().add(maxLengthFacet);
        simpleType.setContent(restriction);

        // add schemaType
        schema.getItems().add(simpleType);
        schema.addType(simpleType);
        setSchemaType(simpleType);

        CorbaTypeImpl anon = null;
        if (stringNode.getType() == IDLTokenTypes.LITERAL_string) {
            // corba:anonstring
            Anonstring anonstring = new Anonstring();
            anonstring.setQName(new QName(typeMap.getTargetNamespace(), stringScopedName.toString()));
            anonstring.setBound(new Long(boundNode.toString()));
            anonstring.setType(simpleType.getQName());

            anon = anonstring;
            
        } else if (stringNode.getType() == IDLTokenTypes.LITERAL_wstring) {
            // corba:anonwstring
            Anonwstring anonwstring = new Anonwstring();
            anonwstring.setQName(new QName(typeMap.getTargetNamespace(), stringScopedName.toString()));
            anonwstring.setBound(new Long(boundNode.toString()));
            anonwstring.setType(simpleType.getQName());
            
            anon = anonwstring;
            
        } else { 
            // should never get here
            throw new RuntimeException("StringVisitor attempted to visit an invalid node");
        }
        
        
        // add corba:anonstring
        typeMap.getStructOrExceptionOrUnion().add(anon);
        setCorbaType(anon);
    }
    
    private void visitBoundedString() {
        // xmlschema:bounded string
        XmlSchemaSimpleType simpleType = new XmlSchemaSimpleType(schema);
        simpleType.setName(stringScopedName.toString());
        XmlSchemaSimpleTypeRestriction restriction = new XmlSchemaSimpleTypeRestriction();
        restriction.setBaseTypeName(Constants.XSD_STRING);
        XmlSchemaMaxLengthFacet maxLengthFacet = new XmlSchemaMaxLengthFacet();
        maxLengthFacet.setValue(boundNode.toString());
        restriction.getFacets().add(maxLengthFacet);
        simpleType.setContent(restriction);

        setSchemaType(simpleType);
                       
        Scope anonstringScopedName = new Scope(getScope(), "_Anon1_" + stringScopedName.tail());
        String anonstringName = anonstringScopedName.toString();
        CorbaTypeImpl anon = null;
        if (stringNode.getType() == IDLTokenTypes.LITERAL_string) {
            // corba:anonstring
            Anonstring anonstring = new Anonstring();
            anonstring.setQName(new QName(typeMap.getTargetNamespace(), anonstringName));
            anonstring.setBound(new Long(boundNode.toString()));
            anonstring.setType(simpleType.getQName());

            anon = anonstring;
            
        } else if (stringNode.getType() == IDLTokenTypes.LITERAL_wstring) {
            // corba:anonwstring
            Anonwstring anonwstring = new Anonwstring();
            anonwstring.setQName(new QName(typeMap.getTargetNamespace(), anonstringName));
            anonwstring.setBound(new Long(boundNode.toString()));
            anonwstring.setType(simpleType.getQName());
            
            anon = anonwstring;
            
        } else { 
            // should never get here
            throw new RuntimeException("StringVisitor attempted to visit an invalid node");
        }
        
        // add corba:anonstring
        typeMap.getStructOrExceptionOrUnion().add(anon);

        // corba:alias
        Alias alias = new Alias();
        alias.setQName(new QName(typeMap.getTargetNamespace(), stringScopedName.toString()));
        alias.setBasetype(anon.getQName());
        alias.setType(simpleType.getQName());
        alias.setRepositoryID(stringScopedName.toIDLRepositoryID());

        // add corba:alias
        setCorbaType(alias);
    }
    
    private void visitUnboundedString() {
        // schema type
        setSchemaType(schemas.getTypeByQName(Constants.XSD_STRING));

        
        // corba type
        CorbaTypeImpl corbaString = new CorbaTypeImpl();
        if (stringNode.getType() == IDLTokenTypes.LITERAL_string) {
            corbaString.setQName(CorbaConstants.NT_CORBA_STRING);
            corbaString.setName(CorbaConstants.NT_CORBA_STRING.getLocalPart());
        } else if (stringNode.getType() == IDLTokenTypes.LITERAL_wstring) {
            corbaString.setQName(CorbaConstants.NT_CORBA_WSTRING);
            corbaString.setName(CorbaConstants.NT_CORBA_WSTRING.getLocalPart());
        } else { 
            // should never get here
            throw new RuntimeException("StringVisitor attempted to visit an invalid node");
        }
        corbaString.setType(Constants.XSD_STRING);

        setCorbaType(corbaString);
    }
    
}
