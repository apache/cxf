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

import org.apache.cxf.tools.corba.common.ToolCorbaConstants;
import org.apache.ws.commons.schema.XmlSchema;

public class ModuleVisitor extends VisitorBase {       

    public ModuleVisitor(Scope scope,
                         Definition defn,
                         XmlSchema schemaRef,
                         WSDLASTVisitor wsdlASTVisitor) {
        super(scope, defn, schemaRef, wsdlASTVisitor);
    }
    
    public void visit(AST node) {
        // <module> ::= "module" <identifier> "{" <definition>+ "}"
        
        AST identifierNode = node.getFirstChild();
        AST definitionNode = identifierNode.getNextSibling();        

        while (definitionNode != null) {
            Scope moduleScope = new Scope(getScope(), identifierNode);
            if (!mapper.containsExcludedModule(moduleScope.toString(ToolCorbaConstants.MODULE_SEPARATOR))) {
                DefinitionVisitor definitionVisitor =
                    new DefinitionVisitor(moduleScope,
                                          definition,
                                          schema,
                                          wsdlVisitor);

                definitionVisitor.visit(definitionNode);
            } else {
                //REVISIT, need to import excluded references.
            }
            
            definitionNode = definitionNode.getNextSibling();
        }
        
    }
}
