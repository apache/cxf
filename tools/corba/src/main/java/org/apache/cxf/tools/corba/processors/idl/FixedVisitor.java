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

import org.apache.cxf.binding.corba.wsdl.Fixed;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaFractionDigitsFacet;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaTotalDigitsFacet;
import org.apache.ws.commons.schema.constants.Constants;

public class FixedVisitor extends VisitorBase {

    private AST identifierNode;
    
    public FixedVisitor(Scope scope,
                        Definition defn,
                        XmlSchema schemaRef,
                        WSDLASTVisitor wsdlVisitor,
                        AST identifierNodeRef) {
        super(scope, defn, schemaRef, wsdlVisitor);
        identifierNode = identifierNodeRef;
    }
    
    public static boolean accept(AST node) {
        if (node.getType() == IDLTokenTypes.LITERAL_fixed) {
            return true;
        }
        return false;
    }
    
    public void visit(AST fixedNode) {
        //      "typedef" <type_declarator>
        //      <type_declarator> ::= <type_spec> <declarators>
        //      <type_spec> ::= <simple_type_spec>
        //                    | <constr_type_spec>
        //      <simple_type_spec> ::= <base_type_spec>
        //                           | <template_type_spec> 
        //                           | <scoped_name>
        //      <base_type_spec> ::= ... omitted (integer, char, octect, etc)
        //      <template_type_spec> ::= <sequence_type>
        //                             | <string_type>
        //                             | <wstring_type>
        //                             | <fixed_pt_type>
        //      <constr_type_spec> ::= <struct_type>
        //                           | <union_type>
        //                           | <enum_type>
        //      <declarators> ::= <declarator> {"," <declarator>}*
        //      <declarator> ::= <simple_declarator>
        //                     | <complex_declarator>
        //      <simple_declarator> ::= <identifier>
        //      <complex_declarator> ::= <array_declarator>
        //      <array_declarator> ::= <identifier> <fixed_array_size>+
        //      <fixed_array_size> ::= "[" <positive_int_const> "]"
        
        
        AST digitsNode = fixedNode.getFirstChild();
        AST scaleNode = digitsNode.getNextSibling();
        Scope scopedName = new Scope(getScope(), identifierNode);
        
        // validate digits and scale
        Long digits = new Long(digitsNode.toString());
        Long scale = new Long(scaleNode.toString());
        if (digits < 1 || digits > 31) {
            //throw IllegalIDLException();
            System.out.println("Digits cannot be greater than 31");
            return;
        }
        if (scale.compareTo(digits) > 0) {
            //throw IllegalIDLException();
            System.out.println("Scale cannot be greater than digits");
            return;
        }
        
        // xmlschema:fixed
        XmlSchemaSimpleType fixedSimpleType = new XmlSchemaSimpleType(schema);
        XmlSchemaSimpleTypeRestriction fixedRestriction = new XmlSchemaSimpleTypeRestriction();
        fixedRestriction.setBaseTypeName(Constants.XSD_DECIMAL);
        XmlSchemaTotalDigitsFacet fixedTotalDigits = new XmlSchemaTotalDigitsFacet();
        fixedTotalDigits.setValue(digitsNode.toString());
        XmlSchemaFractionDigitsFacet fixedFractionDigits = new XmlSchemaFractionDigitsFacet();
        fixedFractionDigits.setValue(scaleNode.toString());
        fixedFractionDigits.setFixed(true);
        fixedRestriction.getFacets().add(fixedTotalDigits);
        fixedRestriction.getFacets().add(fixedFractionDigits);
        fixedSimpleType.setName(mapper.mapToQName(scopedName));
        fixedSimpleType.setContent(fixedRestriction);

        // add xmlschema:fixed
        setSchemaType(fixedSimpleType);

        
        // corba:fixed
        Fixed corbaFixed = new Fixed();
        corbaFixed.setQName(new QName(typeMap.getTargetNamespace(), scopedName.toString()));
        corbaFixed.setDigits(digits);
        corbaFixed.setScale(scale);
        corbaFixed.setRepositoryID(scopedName.toIDLRepositoryID());
        //corbaFixed.setType(Constants.XSD_DECIMAL);
        corbaFixed.setType(fixedSimpleType.getQName());
        
        // add corba:fixed
        setCorbaType(corbaFixed);
    }
}
