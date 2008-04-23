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

import org.apache.cxf.binding.corba.wsdl.Enum;
import org.apache.cxf.binding.corba.wsdl.Enumerator;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.constants.Constants;

public class EnumVisitor extends VisitorBase {

    public EnumVisitor(Scope scope,
                       Definition defn,
                       XmlSchema schemaRef,
                       WSDLASTVisitor wsdlVisitor) {
        super(scope, defn, schemaRef, wsdlVisitor);
    }
    
    public static boolean accept(AST node) {
        if (node.getType() == IDLTokenTypes.LITERAL_enum) {
            return true;
        }
        return false;
    }
    
    public void visit(AST enumNode) {
        // <enum_type> ::= "enum" <identifier> "{" <enumerator> {"," <enumerator>}* "}"
        // <enumerator> ::= <identifier>
        
        
        AST enumNameNode = enumNode.getFirstChild();
        Scope enumNameScope = new Scope(getScope(), enumNameNode);

        // xmlschema:enum
        XmlSchemaSimpleType enumSchemaSimpleType = new XmlSchemaSimpleType(schema);
        enumSchemaSimpleType.setName(mapper.mapToQName(enumNameScope));
        
        XmlSchemaSimpleTypeRestriction enumSchemaSimpleTypeRestriction = new XmlSchemaSimpleTypeRestriction();
        enumSchemaSimpleTypeRestriction.setBaseTypeName(Constants.XSD_STRING);
        
        //XmlSchemaSimpleTypeContent xmlSchemaSimpleTypeContent = enumSchemaSimpleTypeRestriction;
        enumSchemaSimpleType.setContent(enumSchemaSimpleTypeRestriction);

        
        // corba:enum
        Enum corbaEnum = new Enum();
        corbaEnum.setQName(new QName(typeMap.getTargetNamespace(), enumNameScope.toString()));
        corbaEnum.setRepositoryID(enumNameScope.toIDLRepositoryID());
        corbaEnum.setType(enumSchemaSimpleType.getQName());
        
        
        AST node = enumNameNode.getNextSibling();
        while (node != null) {
            // xmlschema:enumeration
            XmlSchemaEnumerationFacet enumeration = new XmlSchemaEnumerationFacet();
            enumeration.setValue(node.toString());
            enumSchemaSimpleTypeRestriction.getFacets().add(enumeration);

            // corba:enumerator
            Enumerator enumerator = new Enumerator();
            enumerator.setValue(node.toString());
            corbaEnum.getEnumerator().add(enumerator);
            
            node = node.getNextSibling();
        }
        
        // add schemaType
        schema.getItems().add(enumSchemaSimpleType);
        schema.addType(enumSchemaSimpleType);

        // add corbaType
        typeMap.getStructOrExceptionOrUnion().add(corbaEnum);

        // REVISIT: are there assignments needed?
        setSchemaType(enumSchemaSimpleType);
        setCorbaType(corbaEnum);
    }
}
