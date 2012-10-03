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
import java.util.List;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import antlr.collections.AST;

import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.tools.corba.common.XmlSchemaPrimitiveMap;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaType;

public class PrimitiveTypesVisitor implements Visitor {

    private static XmlSchemaPrimitiveMap xmlSchemaPrimitiveMap = new XmlSchemaPrimitiveMap();
    
    private static final List<Integer> PRIMITIVE_TYPES = new ArrayList<Integer>();

    static {
        PRIMITIVE_TYPES.add(new Integer(IDLTokenTypes.LITERAL_float));
        PRIMITIVE_TYPES.add(new Integer(IDLTokenTypes.LITERAL_double));
        PRIMITIVE_TYPES.add(new Integer(IDLTokenTypes.LITERAL_long));
        PRIMITIVE_TYPES.add(new Integer(IDLTokenTypes.LITERAL_short));
        PRIMITIVE_TYPES.add(new Integer(IDLTokenTypes.LITERAL_unsigned));
        PRIMITIVE_TYPES.add(new Integer(IDLTokenTypes.LITERAL_char));
        PRIMITIVE_TYPES.add(new Integer(IDLTokenTypes.LITERAL_wchar));
        PRIMITIVE_TYPES.add(new Integer(IDLTokenTypes.LITERAL_boolean));
        PRIMITIVE_TYPES.add(new Integer(IDLTokenTypes.LITERAL_any));
        PRIMITIVE_TYPES.add(new Integer(IDLTokenTypes.LITERAL_octet));
        PRIMITIVE_TYPES.add(new Integer(IDLTokenTypes.LITERAL_any));
    }

    private XmlSchemaType schemaType;
    private CorbaTypeImpl corbaType;
    private Scope scope;
    private XmlSchemaCollection schemas;
    
    public PrimitiveTypesVisitor(Scope scopeRef,
                                 Definition defn,
                                 XmlSchema schemaRef,
                                 XmlSchemaCollection xmlSchemas) {
        scope = scopeRef;
        schemas = xmlSchemas;
    }

    public static boolean accept(AST node) {
        return PRIMITIVE_TYPES.contains(node.getType());
    }
    
    public void visit(AST node) {
        // <base_type_spec> ::= <floating_pt_type>
        //                    | <integer_type>
        //                    | <char_type>
        //                    | <wide_char_type>
        //                    | <boolean_type>
        //                    | <octet_type>
        //                    | <any_type>
        //                    | <object_type>      <= NOT SUPPORTED
        //                    | <value_base_type>  <= NOT SUPPORTED
        // <floating_pt_type> ::= "float"
        //                      | "double"
        //                      | "long" double"
        // <integer_type> ::= <signed_int>
        //                  | <unsigned_int>
        // <signed_int> ::= <signed_short_int>
        //                | <signed_long_int>
        //                | <signed_longlong_int>
        // <signed_short_int> ::= "short"
        // <signed_long_int> ::= "long"
        // <signed_longlong_int> ::= "long" "long"
        // <unsigned_int> ::= <unsigned_short_int>
        //                  | <unsigned_long_int>
        //                  | <unsigned_longlong_int>
        // <unsigned_short_int> ::= "unsigned" "short"
        // <unsigned_long_int> ::= "unsigned" "long"
        // <unsigned_longlong_int> ::= "unsigned" "long" "long"
        // <char_type> ::= "char"
        // <wide_char_type> ::= "wchar"
        // <boolean_type> ::= "boolean"
        // <octet_type> ::= "octet"
        // <any_type> ::= "any"
 
        
        XmlSchemaType stype = null; 
        CorbaTypeImpl ctype = null;
        QName corbaTypeQName = PrimitiveTypesVisitor.getPrimitiveType(node);
        if (corbaTypeQName != null) {
            QName schemaTypeQName = xmlSchemaPrimitiveMap.get(corbaTypeQName);
            if (schemaTypeQName != null) {
                //XmlSchemaCollection schemas = new XmlSchemaCollection();
                stype = schemas.getTypeByQName(schemaTypeQName);
                if (stype != null) {
                    ctype = new CorbaTypeImpl();
                    ctype.setQName(corbaTypeQName);
                    ctype.setType(stype.getQName());
                    ctype.setName(stype.getQName().getLocalPart());
                }
            }
        }

        
        schemaType = stype;
        corbaType = ctype;        
    }
    
    public XmlSchemaType getSchemaType() {
        return schemaType;
    }
    
    public CorbaTypeImpl getCorbaType() {
        return corbaType;
    }
    
    public Scope getScope() {
        return scope;
    }
    
    public Scope getFullyQualifiedName() {
        return scope;
    }
    
    public static QName getPrimitiveType(AST node) {
        QName result = null;
        switch (node.getType()) {
        case IDLTokenTypes.LITERAL_long:
            if ((node.getNextSibling() != null)
                && (node.getNextSibling().getType() == IDLTokenTypes.LITERAL_long)) {
                // long long
                result = CorbaConstants.NT_CORBA_LONGLONG;
            } else if ((node.getFirstChild() != null)
                && (node.getFirstChild().getType() == IDLTokenTypes.LITERAL_double)) {
                // "double" node is a child of "long" node - instead of being a sibling
                // long double
                result = CorbaConstants.NT_CORBA_LONGDOUBLE;
            } else {
                // long
                result = CorbaConstants.NT_CORBA_LONG;
            }
            break;
        case IDLTokenTypes.LITERAL_unsigned:
            AST node2 = node.getNextSibling();
            if (node2 != null && node2.getType() == IDLTokenTypes.LITERAL_short) {
                // unsigned short
                result = CorbaConstants.NT_CORBA_USHORT;
            } else if (node2 != null && node2.getType() == IDLTokenTypes.LITERAL_long) {
                AST node3 = node2.getNextSibling();
                if (node3 != null && node3.getType() == IDLTokenTypes.LITERAL_long) {
                    // unsigned long long
                    result = CorbaConstants.NT_CORBA_ULONGLONG;
                } else {
                    // unsigned long
                    result = CorbaConstants.NT_CORBA_ULONG;
                }
            } else {
                // TBD: we should never get here
            }
            break;
        case IDLTokenTypes.LITERAL_short:
            result = CorbaConstants.NT_CORBA_SHORT;
            break;
        case IDLTokenTypes.LITERAL_float:
            result = CorbaConstants.NT_CORBA_FLOAT;
            break;            
        case IDLTokenTypes.LITERAL_double:
            result = CorbaConstants.NT_CORBA_DOUBLE;
            break;            
        case IDLTokenTypes.LITERAL_char:
            result = CorbaConstants.NT_CORBA_CHAR;
            break;
        case IDLTokenTypes.LITERAL_wchar:
            result = CorbaConstants.NT_CORBA_WCHAR;
            break;
        case IDLTokenTypes.LITERAL_string:
            result = CorbaConstants.NT_CORBA_STRING;
            break;
        case IDLTokenTypes.LITERAL_wstring:
            result = CorbaConstants.NT_CORBA_WSTRING;
            break;
        case IDLTokenTypes.LITERAL_boolean:
            result = CorbaConstants.NT_CORBA_BOOLEAN;
            break;
        case IDLTokenTypes.LITERAL_octet:
            result = CorbaConstants.NT_CORBA_OCTET;
            break;
        case IDLTokenTypes.LITERAL_any:
            result = CorbaConstants.NT_CORBA_ANY;
            break;
        default:
            // TBD 
            break;
        }
        return result;
    }

}
