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

import javax.xml.namespace.QName;

import antlr.ASTVisitor;
import antlr.collections.AST;

import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.binding.corba.wsdl.TypeMappingType;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaType;

public abstract class TypesVisitorBase implements ASTVisitor {

    protected XmlSchema schema;
    protected XmlSchemaCollection schemas;
    protected TypeMappingType typeMap;

    XmlSchemaType schemaType;
    CorbaTypeImpl corbaType;
    
    public TypesVisitorBase(XmlSchemaCollection xmlSchemas,
                            XmlSchema xmlSchema,
                            TypeMappingType typeMapRef) {
        schemas = xmlSchemas;
        schema = xmlSchema;
        typeMap = typeMapRef;
    }

    public abstract void visit(AST node);

    public XmlSchema getSchema() {
        return schema;
    }

    public TypeMappingType getCorbaTypeMap() {
        return typeMap;
    }

    public XmlSchemaType getSchemaType() {
        return schemaType;
    }

    public CorbaTypeImpl getCorbaType() {
        return corbaType;
    }

    public QName getSchemaTypeName() {
        return schemaType.getQName();
    }
    
    public QName getCorbaTypeName() {
        return corbaType.getQName();
    }

    public void setSchemaType(XmlSchemaType type) {
        schemaType = type;
    }

    public void setCorbaType(CorbaTypeImpl type) {
        corbaType = type;
    }
}
