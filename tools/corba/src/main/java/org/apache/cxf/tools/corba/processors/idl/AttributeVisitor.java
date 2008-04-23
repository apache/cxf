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
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
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
import org.apache.cxf.binding.corba.wsdl.ModeType;
import org.apache.cxf.binding.corba.wsdl.OperationType;
import org.apache.cxf.binding.corba.wsdl.ParamType;
import org.apache.cxf.tools.corba.common.ReferenceConstants;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaType;

public class AttributeVisitor extends VisitorBase {

    private static final String GETTER_PREFIX     = "_get_";
    private static final String SETTER_PREFIX     = "_set_";
    private static final String RESULT_POSTFIX    = "Result";
    private static final String RESPONSE_POSTFIX  = "Response";
    private static final String PART_NAME         = "parameters";
    private static final String PARAM_NAME        = "_arg";
    private static final String RETURN_PARAM_NAME = "return";
    
    private ExtensionRegistry   extReg;
    private PortType            portType;
    private Binding             binding;
    
    public AttributeVisitor(Scope scope,
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
    
    public static boolean accept(AST node) {
        if (node.getType() == IDLTokenTypes.LITERAL_readonly
            || node.getType() == IDLTokenTypes.LITERAL_attribute) {
            return true;
        }
        return false;
    }
    
    public void visit(AST attributeNode) {
        // <attr_dcl> ::= ["readonly"] "attribute" <param_type_spec> <simple_declarator>
        //                {"," <simple_declarator>}*
        
        
        AST node = attributeNode.getFirstChild();
        
        AST readonlyNode = null;
        AST typeNode = null;
        AST nameNode = null;
        
        if (node.getType() == IDLTokenTypes.LITERAL_readonly) {
            readonlyNode = node;
            typeNode = readonlyNode.getNextSibling();
        } else {
            typeNode = node;
        }
        nameNode = TypesUtils.getCorbaTypeNameNode(typeNode);
        while (nameNode != null) {
            // getter is generated for readonly and readwrite attributes
            generateGetter(typeNode, nameNode);

            // setter is generated only for readwrite attributes
            if (readonlyNode == null) {
                generateSetter(typeNode, nameNode);
            }
            nameNode = nameNode.getNextSibling();
        }
    }

    private void generateGetter(AST typeNode, AST nameNode) {
        // generate wrapped doc element in parameter
        XmlSchemaElement inParameters = 
            generateWrappedDocElement(null,
                                      GETTER_PREFIX + nameNode.toString(),
                                      PARAM_NAME);
        // generate wrapped doc element out parameter
        XmlSchemaElement outParameters = 
            generateWrappedDocElement(typeNode,
                                      GETTER_PREFIX + nameNode.toString() + RESULT_POSTFIX,
                                      RETURN_PARAM_NAME);                                      

        // generate input message
        Message inMsg = generateMessage(inParameters,
                                        GETTER_PREFIX + nameNode.toString());
        // generate output message
        Message outMsg = generateMessage(outParameters,
                                         GETTER_PREFIX + nameNode.toString() + RESPONSE_POSTFIX);
        
        // generate operation
        String name = GETTER_PREFIX + nameNode.toString();
        Operation op = generateOperation(name, inMsg, outMsg);
        
        
        // generate corba return param
        ArgType corbaReturn = generateCorbaReturnParam(typeNode);
        
        // generate corba operation
        OperationType corbaOp = generateCorbaOperation(op, null, corbaReturn);
        
        // generate binding
        generateCorbaBindingOperation(binding, op, corbaOp);
    }

    private void generateSetter(AST typeNode, AST nameNode) {
        // generate wrapped doc element in parameter
        XmlSchemaElement inParameters = 
            generateWrappedDocElement(typeNode,
                                      SETTER_PREFIX + nameNode.toString(),
                                      PARAM_NAME);
        // generate wrapped doc element out parameter
        XmlSchemaElement outParameters =
            generateWrappedDocElement(null,
                                      SETTER_PREFIX + nameNode.toString() + RESULT_POSTFIX,
                                      RETURN_PARAM_NAME);
        
        // generate input message
        Message inMsg = generateMessage(inParameters,
                                        SETTER_PREFIX + nameNode.toString());
        // generate output message
        Message outMsg = generateMessage(outParameters,
                                         SETTER_PREFIX + nameNode.toString() + RESPONSE_POSTFIX);
        
        // generate operation
        String name = SETTER_PREFIX + nameNode.toString();
        Operation op = generateOperation(name, inMsg, outMsg);
        
        
        // generate corba return param
        ParamType corbaParam = generateCorbaParam(typeNode);
        
        // generate corba operation
        OperationType corbaOp = generateCorbaOperation(op, corbaParam, null);
        
        // generate binding
        generateCorbaBindingOperation(binding, op, corbaOp);
    }
    
    /** Generate a wrapped doc style XmlSchemaElement containing one element.
     * 
     * I.e.: generateWrappedDocElement(null, "foo", "bar");
     * <xs:element name="foo">
     *   <xs:complexType>
     *     <xs:sequence>
     *     </xs:sequence>
     *   </xs:complexType>
     * </xs:element>
     * 
     * i.e.: generateWrappedDocElement(type, "foo", "bar");
     * <xs:element name="foo">
     *   <xs:complexType>
     *     <xs:sequence>
     *       <xs:element name="bar" type="xs:short">
     *       </xs:element>
     *     </xs:sequence>
     *   </xs:complexType>
     * </xs:element>

     * 
     * @param typeNode is the type of the element wrapped in the sequence, no element is created if null.
     * @param name is the name of the wrapping element.
     * @param paramName is the name of the  wrapping element.
     * @return the wrapping element.
     */
    private XmlSchemaElement generateWrappedDocElement(AST typeNode, String name, 
                                                       String paramName) {
        XmlSchemaElement element = new XmlSchemaElement();
        if (typeNode != null) {
            ParamTypeSpecVisitor visitor = new ParamTypeSpecVisitor(getScope(),
                                                                    definition,
                                                                    schema,
                                                                    wsdlVisitor);
            visitor.visit(typeNode);
            XmlSchemaType stype = visitor.getSchemaType();
            Scope fqName = visitor.getFullyQualifiedName();
            
            if (stype != null) {
                element.setSchemaTypeName(stype.getQName());
                if (stype.getQName().equals(ReferenceConstants.WSADDRESSING_TYPE)) {
                    element.setNillable(true);
                }
            } else {
                wsdlVisitor.getDeferredActions().
                    add(fqName, new AttributeDeferredAction(element)); 
            }
            
            element.setName(paramName);
        }
        
        XmlSchemaSequence sequence = new XmlSchemaSequence();
        if (typeNode != null) {
            sequence.getItems().add(element);
        }
        
        XmlSchemaComplexType complex = new XmlSchemaComplexType(schema);
        complex.setParticle(sequence);
        
        XmlSchemaElement result = new XmlSchemaElement();
        result.setName(name);
        result.setQName(new QName(definition.getTargetNamespace(), name));
        result.setSchemaType(complex);

        
        schema.getItems().add(result);
        
        return result;
    }
    
    private Message generateMessage(XmlSchemaElement element, String name) {
        Part part = definition.createPart();
        part.setName(PART_NAME);
        part.setElementName(element.getQName());
        
        Message result = definition.createMessage();
        result.setQName(new QName(definition.getTargetNamespace(), name));
        result.addPart(part);
        result.setUndefined(false);
        
        definition.addMessage(result);
        
        return result;
    }
    
    private Operation generateOperation(String name, Message inputMsg, Message outputMsg) {
        Input input = definition.createInput();
        input.setName(inputMsg.getQName().getLocalPart());
        input.setMessage(inputMsg);
        
        Output output = definition.createOutput();
        output.setName(outputMsg.getQName().getLocalPart());
        output.setMessage(outputMsg);
        
        Operation result = definition.createOperation();
        result.setName(name);
        result.setInput(input);
        result.setOutput(output);
        result.setUndefined(false);
        
        portType.addOperation(result);
        
        return result;
    }

    private ArgType generateCorbaReturnParam(AST type) {
        ArgType param = new ArgType();
        param.setName(RETURN_PARAM_NAME);

        ParamTypeSpecVisitor visitor = new ParamTypeSpecVisitor(getScope(),
                                                                definition,
                                                                schema,
                                                                wsdlVisitor);
        visitor.visit(type);
        CorbaTypeImpl corbaType = visitor.getCorbaType();
        
        if (corbaType != null) {
            param.setIdltype(corbaType.getQName());
        } else {
            wsdlVisitor.getDeferredActions().
                add(visitor.getFullyQualifiedName(), new AttributeDeferredAction(param));
        }
        
        return param;
    }
    
    private ParamType generateCorbaParam(AST type) {
        ParamType param = new ParamType();
        param.setName(PARAM_NAME);
        param.setMode(ModeType.IN);
        
        ParamTypeSpecVisitor visitor = new ParamTypeSpecVisitor(getScope(),
                                                                definition,
                                                                schema,
                                                                wsdlVisitor);
        visitor.visit(type);
        CorbaTypeImpl corbaType = visitor.getCorbaType();
        if (corbaType != null) {
            param.setIdltype(corbaType.getQName());
        } else {
            wsdlVisitor.getDeferredActions().
                add(visitor.getFullyQualifiedName(), new AttributeDeferredAction(param));
        }

        return param;
    }
    
    /** Generates a corba:operation in the corba:binding container within a wsdl:binding.
     * 
     * Only one (or none) corba parameter and only one (or none) corba return parameter are supported.
     * 
     * @param op is the wsdl operation to bind.
     * @param param is the corba parameter, none if null.
     * @param arg is the corba return parameter, none if null.
     * @return the generated corba:operation.
     */
    private OperationType generateCorbaOperation(Operation op, ParamType param, ArgType arg) {
        OperationType operation = new OperationType();
        try {
            operation = (OperationType)extReg.createExtension(BindingOperation.class,
                                                              CorbaConstants.NE_CORBA_OPERATION);
        } catch (WSDLException ex) {
            throw new RuntimeException(ex);
        }
        operation.setName(op.getName());
   
        if (param != null) {
            operation.getParam().add(param);
        }
        
        if (arg != null) {
            operation.setReturn(arg);
        }
        
        return operation;
    }
    
    private BindingOperation generateCorbaBindingOperation(Binding wsdlBinding,
                                                           Operation op,
                                                           OperationType corbaOp) {
        BindingInput bindingInput = definition.createBindingInput();
        bindingInput.setName(op.getInput().getName());
        
        BindingOutput bindingOutput = definition.createBindingOutput();
        bindingOutput.setName(op.getOutput().getName());
        
        BindingOperation bindingOperation = definition.createBindingOperation();
        bindingOperation.addExtensibilityElement(corbaOp);
        bindingOperation.setOperation(op);
        bindingOperation.setName(op.getName());

        bindingOperation.setBindingInput(bindingInput);
        bindingOperation.setBindingOutput(bindingOutput);
        
        binding.addBindingOperation(bindingOperation);

        return bindingOperation;
    }

}
