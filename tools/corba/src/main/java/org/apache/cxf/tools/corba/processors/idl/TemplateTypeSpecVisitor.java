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

public class TemplateTypeSpecVisitor extends VisitorBase {
    
    private AST identifierNode;
    
    public TemplateTypeSpecVisitor(Scope scope,
                                   Definition defn,
                                   XmlSchema schemaRef,
                                   WSDLASTVisitor wsdlVisitor,
                                   AST identifierNodeRef) {
        super(scope, defn, schemaRef, wsdlVisitor);
        identifierNode = identifierNodeRef;
    }

    public static boolean accept(AST node) {
        boolean result = 
            SequenceVisitor.accept(node)
            || StringVisitor.accept(node)
            || FixedVisitor.accept(node);
        return result;
    }
    
    public void visit(AST node) {
        // <template_type_spec> ::= <sequence_type>
        //                        | <string_type>
        //                        | <wide_string_type>
        //                        | <fixed_pt_type>


        Visitor visitor = null;
        
        if (SequenceVisitor.accept(node)) {
            // <sequence_type>
            visitor = new SequenceVisitor(getScope(), definition, schema, wsdlVisitor, identifierNode);
        } else if (StringVisitor.accept(node)) {
            // <string_type>
            // <wstring_type>
            visitor = new StringVisitor(getScope(), definition, schema, wsdlVisitor, identifierNode);
        } else if (FixedVisitor.accept(node)) {
            // <fixed_pt_type>
            visitor = new FixedVisitor(getScope(), definition, schema, wsdlVisitor, identifierNode);
        }

        visitor.visit(node);

        setSchemaType(visitor.getSchemaType());
        setCorbaType(visitor.getCorbaType());
        setFullyQualifiedName(visitor.getFullyQualifiedName());
    }

}
