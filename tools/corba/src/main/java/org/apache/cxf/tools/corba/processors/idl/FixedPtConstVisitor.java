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

import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;

public class FixedPtConstVisitor implements Visitor {
        
    private XmlSchemaType schemaType;
    private CorbaTypeImpl corbaType;
    private Scope scope;
    private XmlSchemaCollection schemas;
    
    
    public FixedPtConstVisitor(Scope scopeRef,
                               Definition defn,
                               XmlSchema schemaRef,
                               XmlSchemaCollection xmlSchemas) {
        scope = scopeRef;
        schemas = xmlSchemas;
    }

    
    public static boolean accept(AST node) {
        if (node.getType() == IDLTokenTypes.LITERAL_fixed) {
            return true;
        }
        return false;
    }
    
    public void visit(AST fixedNode) {
        //      <fixed_pt_const_type> ::= "fixed"
                
        XmlSchemaType stype = null; 
        CorbaTypeImpl ctype = null;
        
        QName corbaTypeQName =  CorbaConstants.NE_CORBA_FIXED; 
        
        if (corbaTypeQName != null) {
            QName schemaTypeQName = Constants.XSD_DECIMAL;
            if (schemaTypeQName != null) {        
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
        
}
