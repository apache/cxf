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

import antlr.collections.AST;

import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.binding.corba.wsdl.TypeMappingType;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaType;

public abstract class VisitorBase implements Visitor {

    protected static ScopeNameCollection scopedNames;
    protected WSDLASTVisitor wsdlVisitor;
    protected XmlSchemaCollection schemas;
    protected TypeMappingType typeMap;
    protected ModuleToNSMapper mapper;
    protected WSDLSchemaManager manager;
    protected DeferredActionCollection deferredActions;
    protected XmlSchema schema;
    protected Definition definition;
    
    private XmlSchemaType schemaType;
    private CorbaTypeImpl corbaType;
    private Scope fullyQualifiedName;

    private Scope scope;

    public VisitorBase(Scope scopeRef,
                       Definition defn,
                       XmlSchema schemaRef,
                       WSDLASTVisitor wsdlASTVisitor) {
        wsdlVisitor = wsdlASTVisitor;
        schemas = wsdlVisitor.getSchemas();
        scopedNames = wsdlVisitor.getScopedNames();
        deferredActions = wsdlVisitor.getDeferredActions();
        typeMap = wsdlVisitor.getTypeMap();
        
        manager = wsdlVisitor.getManager();
        mapper = wsdlVisitor.getModuleToNSMapper();

        scope = scopeRef;

        fullyQualifiedName = null;        
        schemaType = null;
        corbaType = null;
        definition = defn;
        schema = schemaRef;
    }

    public abstract void visit(AST node);
    
    protected void setSchemaType(XmlSchemaType type) {
        schemaType = type;
    }
    
    public XmlSchemaType getSchemaType() {
        return schemaType;
    }
    
    protected void setCorbaType(CorbaTypeImpl type) {
        corbaType = type;
    }
    
    public CorbaTypeImpl getCorbaType() {
        return corbaType;        
    }
    
    public Scope getScope() {
        return scope;
    }
    
    public static ScopeNameCollection getScopedNames() {
        return scopedNames;
    }
    
    public void setFullyQualifiedName(Scope declaredName) {
        fullyQualifiedName = declaredName;
    }
    
    public Scope getFullyQualifiedName() {
        return fullyQualifiedName;
    }
    
    public WSDLASTVisitor getWsdlVisitor() {
        return wsdlVisitor;
    }
}
