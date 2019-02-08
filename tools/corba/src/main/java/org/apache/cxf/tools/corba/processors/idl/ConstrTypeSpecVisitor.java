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

import org.apache.ws.commons.schema.XmlSchema;

public class ConstrTypeSpecVisitor extends VisitorBase {

    protected AST identifierNode;

    public ConstrTypeSpecVisitor(Scope scope,
                                 Definition defn,
                                 XmlSchema schemaRef,
                                 WSDLASTVisitor wsdlASTVisitor,
                                 AST identifierNodeRef) {
        super(scope, defn, schemaRef, wsdlASTVisitor);
        identifierNode = identifierNodeRef;
    }

    public static boolean accept(AST node) {
        return StructVisitor.accept(node)
            || UnionVisitor.accept(node)
            || EnumVisitor.accept(node);
    }

    public void visit(AST node) {
        // <constr_type_spec> ::= <struct_type>
        //                      | <union_type>
        //                      | <enum_type>

        Visitor visitor = null;

        if (StructVisitor.accept(node)) {
            visitor = new StructVisitor(getScope(), definition, schema, wsdlVisitor);
        }

        if (UnionVisitor.accept(node)) {
            visitor = new UnionVisitor(getScope(), definition, schema, wsdlVisitor);
        }

        if (EnumVisitor.accept(node)) {
            visitor = new EnumVisitor(getScope(), definition, schema, wsdlVisitor);
        }

        if (visitor != null) {
            visitor.visit(node);

            setSchemaType(visitor.getSchemaType());
            setCorbaType(visitor.getCorbaType());
        }
    }

}
