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

import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.PortType;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.xml.namespace.QName;

import antlr.collections.AST;

import org.apache.cxf.binding.corba.wsdl.ArgType;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.binding.corba.wsdl.OperationType;
import org.apache.cxf.binding.corba.wsdl.RaisesType;
import org.apache.cxf.tools.corba.common.ReferenceConstants;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaType;

public class OperationVisitor extends VisitorBase {

    private static final String REQUEST_SUFFIX = "Request";
    private static final String RESPONSE_SUFFIX = "Response";
    
    private static final String IN_PARAMETER = "inparameter";
    private static final String OUT_PARAMETER = "outparameter";
    //private static final String INOUT_PARAMETER = "inoutparameter";
    private static final String RETURN_PARAMETER = "return";
    
    private ExtensionRegistry extReg;
    private PortType portType;
    private Binding binding;
    
    private Message inputMsg;
    private Message outputMsg;
    
    
    private OperationType corbaOperation;
    
    public OperationVisitor(Scope scope,
                            Definition defn,
                            XmlSchema schemaRef,
                            WSDLASTVisitor wsdlVisitor,
                            PortType wsdlPortType,
                            Binding wsdlBinding) {
        super(scope, defn, schemaRef, wsdlVisitor);
        extReg = definition.getExtensionRegistry();
        portType = wsdlPortType;
        binding = wsdlBinding;
    }
    
    public static boolean accept(Scope scope,
                                 Definition def,
                                 XmlSchema schema,
                                 AST node,
                                 WSDLASTVisitor wsdlVisitor) {
        boolean result = false;
        AST node2 = node.getFirstChild();
        if (node2.getType() == IDLTokenTypes.LITERAL_oneway) {
            result = true;
        } else {
            int type = node2.getType();
            result =
                type == IDLTokenTypes.LITERAL_void
                || PrimitiveTypesVisitor.accept(node2)
                || StringVisitor.accept(node2)
                || ScopedNameVisitor.accept(scope, def, schema, node2, wsdlVisitor)
                //REVISIT, change this to be def & then schema
                || ObjectReferenceVisitor.accept(scope, schema, def, node2, wsdlVisitor);
        }
        return result;
    }
    
    public void visit(AST node) {
        // <op_dcl> ::= [<op_attribute>] <op_type_spec>
        //              <identifier> <parameter_dcls>
        //              [<raises_expr>] [<context_expr>]
        // <op_attribute> ::= "oneway"
        // <op_type_spec> ::= <param_type_spec>
        //                  | "void"
        // <parameter_dcls> ::= "(" <param_dcl> {"," <param_dcl>}* ")"
        //                    | "(" ")"
        // <raises_expr> ::= "raises" "(" <scoped_name> {"," <scoped_name>}* ")"
        // <context_expr> ::= "context" "(" <string_literal> {"," <string_literal>}* ")"     

        QName operationQName = new QName(schema.getTargetNamespace(), node.toString());
        boolean isDuplicate = false;
        if (schema.getElements().contains(operationQName)) {
            isDuplicate = true;
        }
  
        Operation operation = generateOperation(operationQName.getLocalPart(), isDuplicate);

        BindingOperation bindingOperation = null;
        if (isDuplicate) {
            bindingOperation = generateBindingOperation(binding, operation, operationQName.getLocalPart());
        } else {
            bindingOperation = generateBindingOperation(binding, operation, operation.getName());
        }
        
        XmlSchemaSequence inputWrappingSequence = new XmlSchemaSequence();
        XmlSchemaElement inputElement = generateWrapper(new QName(schema.getTargetNamespace(),
                                                                  operation.getName()),
                                                        inputWrappingSequence);
        inputMsg = generateInputMessage(operation, bindingOperation);
        generateInputPart(inputMsg, inputElement);

        // <op_attribute>
        node = node.getFirstChild();
        XmlSchemaSequence outputWrappingSequence = null;
        XmlSchemaElement outputElement = null;
        if (node != null && (node.getType() == IDLTokenTypes.LITERAL_oneway)) {
            // oneway operations map to operations with only input message
            // no outputMsg nor outputPart need be created
            node = node.getNextSibling();
        } else {
            // normal operations map to request-response operations
            // with input and output messages
            outputWrappingSequence = new XmlSchemaSequence();
            outputElement = generateWrapper(new QName(schema.getTargetNamespace(),
                                                      operation.getName() + RESPONSE_SUFFIX),
                                            outputWrappingSequence);
            outputMsg = generateOutputMessage(operation, bindingOperation);
            generateOutputPart(outputMsg, outputElement);           
        }
        
        // <op_type_spec>
        visitOpTypeSpec(node, outputWrappingSequence);
        
        // <parameter_dcls>
        node = TypesUtils.getCorbaTypeNameNode(node);
        while (ParamDclVisitor.accept(node)) {

            ParamDclVisitor visitor = new ParamDclVisitor(getScope(),
                                                          definition,
                                                          schema,
                                                          wsdlVisitor,
                                                          inputWrappingSequence,
                                                          outputWrappingSequence,
                                                          corbaOperation);
            visitor.visit(node);

            node = node.getNextSibling();
        }

        // <raises_expr>
        if (node != null 
            && node.getType() == IDLTokenTypes.LITERAL_raises) {
            node = node.getFirstChild();
            
            while (node != null) {            
                // 
                ScopedNameVisitor visitor = new ScopedNameVisitor(getScope(),
                                                                  definition,
                                                                  schema,
                                                                  wsdlVisitor);                
                visitor.setExceptionMode(true);
                visitor.visit(node);                
                CorbaTypeImpl corbaType = visitor.getCorbaType();
                XmlSchemaType schemaType = visitor.getSchemaType();
                //REVISIT, schema type ends with Type for exceptions, so strip it to get the element name.
                int pos = schemaType.getQName().getLocalPart().indexOf("Type");
                QName elementQName = new QName(schemaType.getQName().getNamespaceURI(),
                                               schemaType.getQName().getLocalPart().substring(0, pos));
                createFaultMessage(corbaType, operation, bindingOperation, elementQName);                
                node = node.getNextSibling();
                visitor.setExceptionMode(false);
            }
        }
        
    }

    private Operation generateOperation(String name, boolean isDuplicate) {
        Operation op = definition.createOperation();
        if (isDuplicate) {
            // Replace '.' in the scoping name with '_'.  This results in the final
            // operation name being more readable (otherwise generated code removes
            // the '.' and merges all of the scoping names together)
            String prefix = getScope().toString().replace('.', '_');
            name = prefix + "_" + name;
        }
        op.setName(name);
        op.setUndefined(false);
        portType.addOperation(op);
        return op;
    }
    
    private BindingOperation generateBindingOperation(Binding wsdlBinding, Operation op,
                                                      String corbaOpName) {
        BindingOperation bindingOperation = definition.createBindingOperation();
        //OperationType operationType = null;
        try {
            corbaOperation = (OperationType)extReg.createExtension(BindingOperation.class,
                                                                   CorbaConstants.NE_CORBA_OPERATION);
        } catch (WSDLException ex) {
            throw new RuntimeException(ex);
        }
        corbaOperation.setName(corbaOpName);
        bindingOperation.addExtensibilityElement(corbaOperation);
        bindingOperation.setOperation(op);
        bindingOperation.setName(op.getName());
        binding.addBindingOperation(bindingOperation);
        return bindingOperation;
    }


    public Message generateInputMessage(Operation operation, BindingOperation bindingOperation) {
        Message msg = definition.createMessage();
        QName msgName;
        if (!mapper.isDefaultMapping()) {
            //mangle the message name
            //REVISIT, do we put in the entire scope for mangling
            msgName = new QName(definition.getTargetNamespace(),
                                getScope().tail() + "." + operation.getName());
        } else {
            msgName = new QName(definition.getTargetNamespace(), operation.getName()); 
        }
        msg.setQName(msgName);
        msg.setUndefined(false);
        
        String inputName = operation.getName() + REQUEST_SUFFIX;
        Input input = definition.createInput();
        input.setName(inputName);
        input.setMessage(msg);
        
        BindingInput bindingInput = definition.createBindingInput();
        bindingInput.setName(inputName);    
        
        bindingOperation.setBindingInput(bindingInput);
        operation.setInput(input);
        
        definition.addMessage(msg);
        
        return msg;
    }

    public Message generateOutputMessage(Operation operation, BindingOperation bindingOperation) {
        Message msg = definition.createMessage(); 
        QName msgName;
        if (!mapper.isDefaultMapping()) {
            //mangle the message name
            //REVISIT, do we put in the entire scope for mangling
            msgName = new QName(definition.getTargetNamespace(),
                                getScope().tail() + "." + operation.getName() + RESPONSE_SUFFIX);
        } else {
            msgName = new QName(definition.getTargetNamespace(),
                                operation.getName() + RESPONSE_SUFFIX); 
        }
        msg.setQName(msgName);
        msg.setUndefined(false);
        
        String outputName = operation.getName() + RESPONSE_SUFFIX;
        Output output = definition.createOutput();
        output.setName(outputName);
        output.setMessage(msg);
        
        BindingOutput bindingOutput = definition.createBindingOutput();
        bindingOutput.setName(outputName);    
        
        bindingOperation.setBindingOutput(bindingOutput);
        operation.setOutput(output);
        
        definition.addMessage(msg);
        
        return msg;
    }
    
    private Part generateInputPart(Message inputMessage, XmlSchemaElement element) {
        // message - part 
        Part part = definition.createPart();
        part.setName(IN_PARAMETER);
        part.setElementName(element.getQName());
        inputMessage.addPart(part);
        return part;        
    }

    private Part generateOutputPart(Message outputMessage, XmlSchemaElement element) {
        // message - part
        Part part = definition.createPart();
        part.setName(OUT_PARAMETER);
        part.setElementName(element.getQName());
        outputMessage.addPart(part);
        return part;
    }
    
    /*-
     * Build the Wrapped Document Style wrapping elements
     * i.e. <xs:element name="...">
     *       <xs:complexType>
     *        <xs:sequence>
     *         ...
     *        </xs:sequence>
     *       </xs:complexType>
     *      </xs:element>
     */
    private XmlSchemaElement generateWrapper(QName el, XmlSchemaSequence wrappingSequence) {
        XmlSchemaComplexType schemaComplexType = new XmlSchemaComplexType(schema);
        schemaComplexType.setParticle(wrappingSequence);
        
        XmlSchemaElement wrappingSchemaElement = new XmlSchemaElement();
        wrappingSchemaElement.setQName(el);
        wrappingSchemaElement.setName(el.getLocalPart());
        wrappingSchemaElement.setSchemaType(schemaComplexType);

        schema.getElements().add(el, wrappingSchemaElement);
        schema.getItems().add(wrappingSchemaElement);
        
        return wrappingSchemaElement;
    }

    private XmlSchemaElement addElement(XmlSchemaSequence schemaSequence,
                                        XmlSchemaType schemaType,
                                        Scope fqName,
                                        String name) {
        XmlSchemaElement element = new XmlSchemaElement();
        element.setName(name);
        if (schemaType != null) {
            element.setSchemaTypeName(schemaType.getQName());
            if (schemaType.getQName().equals(ReferenceConstants.WSADDRESSING_TYPE)) {
                element.setNillable(true);
            }
        } else {
            wsdlVisitor.getDeferredActions().
                add(fqName, new OperationDeferredAction(element));  
        }
        
        schemaSequence.getItems().add(element);
        
        return element;
    }
    
    private void visitOpTypeSpec(AST node, XmlSchemaSequence outputWrappingSequence) {
        if (node.getType() == IDLTokenTypes.LITERAL_void) {
            // nothing to do here, move along
            return;
        } else {
            ParamTypeSpecVisitor visitor = new ParamTypeSpecVisitor(getScope(),
                                                                    definition,
                                                                    schema,
                                                                    wsdlVisitor);

            visitor.visit(node);
            
            XmlSchemaType schemaType = visitor.getSchemaType();
            CorbaTypeImpl corbaType = visitor.getCorbaType();
            Scope fqName = visitor.getFullyQualifiedName();
            
            addElement(outputWrappingSequence, schemaType, fqName, RETURN_PARAMETER);
            addCorbaReturn(corbaType, fqName, RETURN_PARAMETER);
        }
    }

    private void addCorbaReturn(CorbaTypeImpl corbaType, Scope fqName, String partName) {
        ArgType param = new ArgType();
        param.setName(partName);
        if (corbaType != null) {
            param.setIdltype(corbaType.getQName());
        } else {
            wsdlVisitor.getDeferredActions().
                add(fqName, new OperationDeferredAction(param));
        }
        corbaOperation.setReturn(param);
    }

    private void createFaultMessage(CorbaTypeImpl corbaType,
                                    Operation operation, 
                                    BindingOperation bindingOperation,
                                    QName elementQName) {
        String exceptionName = corbaType.getQName().getLocalPart();        

        Definition faultDef = manager.getWSDLDefinition(elementQName.getNamespaceURI());
        if (faultDef == null) {
            faultDef = definition;
        }
        Message faultMsg = faultDef.getMessage(new QName(faultDef.getTargetNamespace(), exceptionName));
        if (faultMsg == null) {
            throw new RuntimeException("Fault message for exception " + exceptionName + " not found");
        }

        // porttype - operation - fault
        Fault fault = definition.createFault();
        fault.setMessage(faultMsg);        
        fault.setName(faultMsg.getQName().getLocalPart());        
        operation.addFault(fault);

        // binding - operation - corba:operation - corba:raises
        RaisesType raisesType = new RaisesType();        
        raisesType.setException(new QName(typeMap.getTargetNamespace(),
                                          exceptionName));
        corbaOperation.getRaises().add(raisesType);

        // binding - operation - fault
        BindingFault bindingFault = definition.createBindingFault();        
        bindingFault.setName(faultMsg.getQName().getLocalPart());        
        bindingOperation.addBindingFault(bindingFault);    

        //add the fault element namespace to the definition
        String nsURI = elementQName.getNamespaceURI();
        manager.addWSDLDefinitionNamespace(definition, mapper.mapNSToPrefix(nsURI), nsURI);    
    }
}
