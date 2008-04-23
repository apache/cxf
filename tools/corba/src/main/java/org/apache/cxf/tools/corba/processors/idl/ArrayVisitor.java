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

import org.apache.cxf.binding.corba.wsdl.Anonarray;
import org.apache.cxf.binding.corba.wsdl.Array;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.tools.corba.common.ReferenceConstants;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaType;

public class ArrayVisitor extends VisitorBase {

    private static final String ELEMENT_NAME = "item";
    private AST identifierNode;
    
    public ArrayVisitor(Scope scope,
                        Definition defn,
                        XmlSchema schemaRef,
                        WSDLASTVisitor wsdlVisitor,
                        AST identifierNodeRef,
                        Scope fqName) {
        super(scope, defn, schemaRef, wsdlVisitor);
        setFullyQualifiedName(fqName);
        identifierNode = identifierNodeRef;
    }

    public static boolean accept(AST node) {
        boolean result = false;
        AST sizeNode = node.getFirstChild();
        if (sizeNode != null) {
            // check that node has a fixed_array_size child node
            result = true;
            while (sizeNode != null
                && result) {
                // check that all fixed_array_size nodes encode
                // positive integers
                String s = sizeNode.toString();
                for (int j = 0; j < s.length(); j++) {
                    if (!Character.isDigit(s.charAt(j))) {
                        result = false;
                    }
                }
                sizeNode = sizeNode.getNextSibling();
            }
        }
        return result;
    }

    public void visit(AST node) {
        // <array_declarator> ::= <identifier> <fixed_array_size>+
        // <fixed_array_size> ::= "[" <positive_int_const> "]"


        AST firstSizeNode = node.getFirstChild();
        AST nextSizeNode = firstSizeNode.getNextSibling();
        Types result = null;
        
        // process all anonarrays, skip first array as it might not be anonymous
        if (nextSizeNode != null) {
            result = doAnonarray(nextSizeNode, getSchemaType(), getCorbaType());
        } else {
            result = new Types();
            result.setSchemaType(getSchemaType());
            result.setCorbaType(getCorbaType());
            result.setFullyQualifiedName(getFullyQualifiedName());
        }
        
        // process first array
        Long size = new Long(firstSizeNode.toString());
        XmlSchemaType stype = null;
        CorbaTypeImpl ctype = null;
        if (identifierNode != null) {
            Scope scopedName = getScope();            
            if (result.getSchemaType() != null) {
                stype = generateSchemaArray(scopedName, size, 
                                            result.getSchemaType(), 
                                            result.getFullyQualifiedName());
            } else {
                stype = generateSchemaArray(scopedName, size, 
                                            null, result.getFullyQualifiedName());
            }
            if (result.getCorbaType() != null) {
                ctype = generateCorbaArray(scopedName, size, result.getCorbaType(), stype,
                                           getFullyQualifiedName());
            } else {
                ctype = generateCorbaArray(scopedName, size, null, stype, 
                                           getFullyQualifiedName());
            }
        } else {
            // anonymous array
            Scope scopedName = TypesUtils.generateAnonymousScopedName(getScope(), schema);
            if (result.getSchemaType() != null) {
                stype = generateSchemaArray(scopedName,
                                            size,
                                            result.getSchemaType(),
                                            getFullyQualifiedName());
            } else {
                stype = generateSchemaArray(scopedName,
                                            size, null, getFullyQualifiedName());
            }
            if (result.getCorbaType() != null) {
                ctype = generateCorbaAnonarray(scopedName.toString(),
                                               size,
                                               result.getCorbaType(),
                                               stype,
                                               getFullyQualifiedName());
            } else {
                ctype = generateCorbaAnonarray(scopedName.toString(),
                                               size, null, stype, getFullyQualifiedName());
            }
        }
        
        // add schemaType
        schema.getItems().add(stype);
        schema.addType(stype);

        // add corbaType
        typeMap.getStructOrExceptionOrUnion().add(ctype);
        
        setSchemaType(stype);
        setCorbaType(ctype);
    }

    private Types doAnonarray(AST node, XmlSchemaType stype, CorbaTypeImpl ctype) {
        Types result = new Types();
        
        if (node != null) {
            
            AST next = node.getNextSibling();
            result = doAnonarray(next, stype, ctype);

            Scope scopedName = TypesUtils.generateAnonymousScopedName(getScope(), schema);
            Long size = new Long(node.toString());
            
            if (result.getSchemaType() == null) {
                result.setSchemaType(generateSchemaArray(scopedName,
                                                         size,
                                                         stype,
                                                         getFullyQualifiedName()));
            } else {
                result.setSchemaType(generateSchemaArray(scopedName,
                                                         size,
                                                         result.getSchemaType(),
                                                         getFullyQualifiedName()));
            }
            
            if (result.getCorbaType() == null) {
                result.setCorbaType(generateCorbaAnonarray(scopedName.toString(),
                                                           size,
                                                           ctype,
                                                           result.getSchemaType(),
                                                           getFullyQualifiedName()));
            } else {
                result.setCorbaType(generateCorbaAnonarray(scopedName.toString(),
                                                           size,
                                                           result.getCorbaType(),
                                                           result.getSchemaType(),
                                                           getFullyQualifiedName()));
            }
            

            // add schemaType
            schema.getItems().add(result.getSchemaType());
            schema.addType(result.getSchemaType());

            // add corbaType
            typeMap.getStructOrExceptionOrUnion().add(result.getCorbaType());
        }
        
        return result;
    }
    
    private XmlSchemaComplexType generateSchemaArray(Scope scopedName, Long size, 
                                                     XmlSchemaType type, Scope fQName) {
        XmlSchemaComplexType complexType = new XmlSchemaComplexType(schema);
        complexType.setName(mapper.mapToQName(scopedName));

        XmlSchemaSequence sequence = new XmlSchemaSequence();

        XmlSchemaElement element = new XmlSchemaElement();
        element.setMinOccurs(size);
        element.setMaxOccurs(size);
        element.setName(ELEMENT_NAME);
        if (type != null) {
            element.setSchemaTypeName(type.getQName());
            if (type.getQName().equals(ReferenceConstants.WSADDRESSING_TYPE)) {
                element.setNillable(true);
            }
        } else {
            ArrayDeferredAction arrayAction = 
                new ArrayDeferredAction(element);
            wsdlVisitor.getDeferredActions().add(fQName, arrayAction);
        }
        
        sequence.getItems().add(element);

        complexType.setParticle(sequence);

        return complexType;
    }

    private Array generateCorbaArray(Scope scopedName, Long size, 
                                     CorbaTypeImpl type, XmlSchemaType stype, Scope fQName) {
        Array array = new Array();
        array.setQName(new QName(typeMap.getTargetNamespace(), scopedName.toString()));
        array.setBound(size);
        array.setRepositoryID(scopedName.toIDLRepositoryID());
        array.setType(stype.getQName());
        //REVISIT, if we add qualification option, then change below.
        array.setElemname(new QName("", ELEMENT_NAME));
        if (type != null) {
            array.setElemtype(type.getQName());
        } else {
            ArrayDeferredAction arrayAction = 
                new ArrayDeferredAction(array);
            wsdlVisitor.getDeferredActions().add(fQName, arrayAction);
        }
        return array;
    }

    private Anonarray generateCorbaAnonarray(String name, Long size, 
                                             CorbaTypeImpl type, XmlSchemaType stype, Scope fQName) {
        Anonarray anonarray = new Anonarray();
        anonarray.setQName(new QName(typeMap.getTargetNamespace(), name));
        anonarray.setBound(size);
        anonarray.setType(stype.getQName());
        //REVISIT, if we add qualification option, then change below.
        anonarray.setElemname(new QName("", ELEMENT_NAME));
        if (type != null) {
            anonarray.setElemtype(type.getQName());
        } else {
            ArrayDeferredAction anonarrayAction = 
                new ArrayDeferredAction(anonarray);
            wsdlVisitor.getDeferredActions().add(fQName, anonarrayAction);
        }
        return anonarray;
    }
    
    class Types {
        private XmlSchemaType schemaType;
        private CorbaTypeImpl corbaType;
        private Scope fullyQualifiedName;
        
        public Types() {
            schemaType = null;
            corbaType = null;
        }
        
        public Types(XmlSchemaType stype, CorbaTypeImpl ctype,
                     Scope fqName) {
            schemaType = stype;
            corbaType = ctype;
            fullyQualifiedName = fqName;
        }
        
        public void setSchemaType(XmlSchemaType stype) {
            schemaType = stype;
        }
        
        public void setCorbaType(CorbaTypeImpl ctype) {
            corbaType = ctype;
        }
        
        public XmlSchemaType getSchemaType() {
            return schemaType;
        }
        
        public CorbaTypeImpl getCorbaType() {
            return corbaType;
        }
        
        public void setFullyQualifiedName(Scope fqName) {
            fullyQualifiedName = fqName;
        }
        
        public Scope getFullyQualifiedName() {
            return fullyQualifiedName;
        }
    }
}
