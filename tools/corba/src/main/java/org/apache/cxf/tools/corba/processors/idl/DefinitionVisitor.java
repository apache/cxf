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

public class DefinitionVisitor extends VisitorBase {
    
    public DefinitionVisitor(Scope scope,
                             Definition defn,
                             XmlSchema schemaRef,
                             WSDLASTVisitor wsdlVisitor) {
        super(scope, defn, schemaRef, wsdlVisitor);
    }

    public void visit(AST node) {
        // <definition> ::= <type_dcl> ";"
        //                | <const_dcl> ";"
        //                | <except_dcl> ";"
        //                | <interface> ";"
        //                | <module> ";"
        //                | <value> ";"
        
        switch (node.getType()) {
        case IDLTokenTypes.LITERAL_custom:
        case IDLTokenTypes.LITERAL_valuetype: {
            System.out.println("Valuetypes not supported");
            System.exit(1);
            break;
        }
        case IDLTokenTypes.LITERAL_module: {
            ModuleVisitor moduleVisitor = new ModuleVisitor(getScope(),
                                                            definition,
                                                            schema,
                                                            wsdlVisitor);
            moduleVisitor.visit(node);
            break;
        }
        case IDLTokenTypes.LITERAL_interface: {
            Definition newDefinition = createDefinition(null);
            PortTypeVisitor portTypeVisitor = new PortTypeVisitor(getScope(),
                                                                  newDefinition,
                                                                  schema,
                                                                  wsdlVisitor);
            portTypeVisitor.visit(node);
            break;
        }
        case IDLTokenTypes.LITERAL_exception: {
            XmlSchema newSchema = createSchema();
            Definition newDefinition = createDefinition(newSchema);
            ExceptionVisitor exceptionVisitor = new ExceptionVisitor(getScope(),
                                                                     newDefinition,
                                                                     newSchema,
                                                                     wsdlVisitor);
            exceptionVisitor.visit(node);
            break;
        }
        case IDLTokenTypes.LITERAL_const: {
            XmlSchema newSchema = createSchema();
            ConstVisitor constVisitor = new ConstVisitor(getScope(),
                                                         definition,
                                                         newSchema,
                                                         wsdlVisitor);
            constVisitor.visit(node);
            break;
        }
        default: {
            XmlSchema newSchema = createSchema();
            TypeDclVisitor typeDclVisitor = new TypeDclVisitor(getScope(),
                                                               definition,
                                                               newSchema,
                                                               wsdlVisitor);
            typeDclVisitor.visit(node);
        }
        
        }
    }

    /**
     * Creates the XmlSchema corresponding to the module scope if none exists
     */
    private XmlSchema createSchema() {
        String tns = mapper.map(getScope());
        XmlSchema xmlSchema = schema;
        if (tns != null) {
            xmlSchema = manager.getXmlSchema(tns);
            if (xmlSchema == null) {
                xmlSchema = manager.createXmlSchema(tns, schemas);
            }
        }

        return xmlSchema;
    }

    /**
     * Creates the WSDL definition correspoinding to the module scope if none exists
     */
    private Definition createDefinition(XmlSchema schema) {
        String tns = mapper.map(getScope());
        Definition defn = definition;
        if (tns != null) {
            defn = manager.getWSDLDefinition(tns);
            if (defn == null) {
                try {
                    defn = manager.createWSDLDefinition(tns);
                    String key = getScope().toString("_");
                    String fileName = getWsdlVisitor().getOutputDir()
                        + System.getProperty("file.separator")
                        + key;
                    manager.addWSDLDefinitionImport(definition,
                                                    defn,
                                                    key,
                                                    fileName);
                    if (schema == null) {
                        schema = manager.getXmlSchema(tns);
                    }
                    if (schema != null) {
                        // add a schema based on the module with a corresponding import to the actual schema
                        manager.addWSDLSchemaImport(defn, tns, fileName);
                        // make sure that if we are adding the import to the wsdl, then we also
                        // add the schema to the list of imported schemas
                        manager.getImportedXmlSchemas().put(new java.io.File(fileName + ".xsd"),
                                                            schema);
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return defn;
    }
    
}
