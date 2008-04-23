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

package org.apache.cxf.tools.corba.processors.wsdl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.wsdl.Definition;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.wsdl.ArgType;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.binding.corba.wsdl.ModeType;
import org.apache.cxf.binding.corba.wsdl.ParamType;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaAppInfo;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaExternal;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaObjectTable;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaType;

public final class WSDLParameter {

    static Definition definition;

    public void processParameters(WSDLToCorbaBinding wsdlToCorbaBinding,
            Operation operation, Definition def, SchemaCollection xmlSchemaList,
            List<ParamType> params, List<ArgType> returns,
            boolean simpleOrdering) throws Exception {

        definition = def;
        List<ParamType> inputs = new ArrayList<ParamType>();
        List<ParamType> outputs = new ArrayList<ParamType>();
        List<ArgType> returnOutputs = new ArrayList<ArgType>();
        boolean isWrapped = isWrappedOperation(operation, xmlSchemaList);

        if (isWrapped) {
            processWrappedInputParams(wsdlToCorbaBinding, operation,
                    xmlSchemaList, inputs);
        } else {
            processInputParams(wsdlToCorbaBinding, operation, xmlSchemaList,
                    inputs);
        }
        if (isWrapped) {
            processWrappedOutputParams(wsdlToCorbaBinding, operation,
                    xmlSchemaList, inputs, outputs);
        } else {
            processOutputParams(wsdlToCorbaBinding, operation, xmlSchemaList,
                    inputs, outputs);
        }
        processReturnParams(outputs, returnOutputs);
        orderParameters(inputs, outputs, true);
        returns.addAll(returnOutputs);
        params.addAll(inputs);

    }

    private void processWrappedInputParams(WSDLToCorbaBinding wsdlToCorbaBinding,
                                           Operation operation,
                                           SchemaCollection xmlSchemaList,
                                           List<ParamType> inputs)
        throws Exception {
        Input input = operation.getInput();

        if (input != null) {
            Message msg = input.getMessage();
            Part part = (Part) msg.getOrderedParts(null).iterator().next();

            XmlSchemaComplexType schemaType = null;

            XmlSchemaElement el = getElement(part, xmlSchemaList);
            if ((el != null) && (el.getSchemaType() != null)) {
                schemaType = (XmlSchemaComplexType) el.getSchemaType();
            }
          
            XmlSchemaSequence seq = (XmlSchemaSequence) schemaType.getParticle();
            if (seq != null) {
                XmlSchemaObjectCollection items = seq.getItems();
                for (int i = 0; i < items.getCount(); i++) {
                    el = (XmlSchemaElement) items.getItem(i);
                    //REVISIT, handle element ref's?
                    QName typeName = el.getSchemaTypeName();
                    if (typeName == null) {
                        typeName = el.getQName();
                    }
                    QName idltype = getIdlType(wsdlToCorbaBinding,
                                               el.getSchemaType(),
                                               typeName,
                                               el.isNillable());
                    ParamType paramtype = createParam(wsdlToCorbaBinding,
                                                      "in",
                                                      el.getQName().getLocalPart(),
                                                      idltype);
                    if (paramtype != null) {
                        inputs.add(paramtype);
                    }
                }
            }       
        }
    }

    private void processInputParams(WSDLToCorbaBinding wsdlToCorbaBinding,
                                    Operation operation,
                                    SchemaCollection xmlSchemaList,
                                    List<ParamType> inputs)
        throws Exception {

        Input input = operation.getInput();

        if (input != null) {
            Message msg = input.getMessage();
            Iterator i = msg.getOrderedParts(null).iterator();
            while (i.hasNext()) {
                Part part = (Part) i.next();
                XmlSchemaType schemaType = null;
                boolean isObjectRef = isObjectReference(xmlSchemaList, part.getElementName());
                if (part.getElementName() != null && !isObjectRef) {
                    XmlSchemaElement el = getElement(part, xmlSchemaList);
                    if ((el != null) && (el.getSchemaType() != null)) {
                        schemaType = el.getSchemaType();
                    }
                    QName typeName = el.getSchemaTypeName();
                    if (typeName == null) {
                        typeName = el.getQName();
                    }
                    QName idltype = getIdlType(wsdlToCorbaBinding,
                                               schemaType,
                                               typeName,
                                               el.isNillable());
                    ParamType paramtype = createParam(wsdlToCorbaBinding,
                                                      "in",
                                                      part.getName(),
                                                      idltype);
                    if (paramtype != null) {
                        inputs.add(paramtype);
                    }
                } else if (part.getTypeName() != null) {
                    schemaType = getType(part, xmlSchemaList);
                    QName typeName = part.getTypeName();
                    if (isObjectRef) {
                        typeName = part.getElementName();
                    }
                    QName idltype = getIdlType(wsdlToCorbaBinding,
                                               schemaType,
                                               typeName,
                                               false);
                    ParamType paramtype = createParam(wsdlToCorbaBinding,
                                                      "in",
                                                      part.getName(),
                                                      idltype);
                    if (paramtype != null) {
                        inputs.add(paramtype);
                    }
                }
            }
        }
    }

    private void processWrappedOutputParams(
            WSDLToCorbaBinding wsdlToCorbaBinding, Operation operation,
            SchemaCollection xmlSchemaList, List<ParamType> inputs,
            List<ParamType> outputs) throws Exception {

        Output output = operation.getOutput();

        if (output != null) {
            Message msg = output.getMessage();
            Part part = (Part) msg.getOrderedParts(null).iterator().next();
            XmlSchemaComplexType schemaType = null;

            XmlSchemaElement el = getElement(part, xmlSchemaList);
            if (el != null && el.getSchemaType() != null) {
                schemaType = (XmlSchemaComplexType) el.getSchemaType();
            }
            XmlSchemaSequence seq = (XmlSchemaSequence) schemaType.getParticle();
            if (seq != null) {
                XmlSchemaObjectCollection items = seq.getItems();
                for (int i = 0; i < items.getCount(); i++) {
                    el = (XmlSchemaElement) items.getItem(i);
                    processWrappedOutputParam(wsdlToCorbaBinding, el, inputs, outputs);
                }
            }
        }
    }

    private void processWrappedOutputParam(WSDLToCorbaBinding wsdlToCorbaBinding,
                                           XmlSchemaElement el,
                                           List<ParamType> inputs,
                                           List<ParamType> outputs)
        throws Exception {
        ParamType paramtype = null;
        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.get(i).getName().equals(el.getQName().getLocalPart())) {
                inputs.remove(i);
                QName typeName = el.getSchemaTypeName();
                if (typeName == null) {
                    typeName = el.getQName();
                }
                QName idltype = getIdlType(wsdlToCorbaBinding,
                                           el.getSchemaType(),
                                           typeName,
                                           el.isNillable());
                paramtype = createParam(wsdlToCorbaBinding,
                                        "inout",
                                        el.getQName().getLocalPart(),
                                        idltype);
                if (paramtype != null) {
                    inputs.add(paramtype);
                }
            }
        }
        if (paramtype == null) {
            QName typeName = el.getSchemaTypeName();
            if (typeName == null) {
                typeName = el.getQName();
            }
            QName idltype = getIdlType(wsdlToCorbaBinding,
                                       el.getSchemaType(),
                                       typeName,
                                       el.isNillable());
            paramtype = createParam(wsdlToCorbaBinding,
                                    "out",
                                    el.getQName().getLocalPart(),
                                    idltype);
            if (paramtype != null) {
                outputs.add(paramtype);
            }
        }
    }

    private void processOutputParams(WSDLToCorbaBinding wsdlToCorbaBinding,
            Operation operation, SchemaCollection xmlSchemaList,
            List<ParamType> inputs, List<ParamType> outputs) throws Exception {

        Output output = operation.getOutput();

        if (output != null) {
            Message msg = output.getMessage();
            Iterator i = msg.getOrderedParts(null).iterator();
            while (i.hasNext()) {
                Part part = (Part) i.next();
                XmlSchemaType schemaType = null;
                // check if in input list
                String mode = "out";
                ParamType paramtype = null;
                boolean isObjectRef = isObjectReference(xmlSchemaList, part.getElementName());
                for (int x = 0; x < inputs.size(); x++) {
                    paramtype = null;
                    ParamType d2 = (ParamType) inputs.get(x);
                    if (part.getElementName() != null && !isObjectRef) {
                        XmlSchemaElement el = getElement(part,
                                                         xmlSchemaList);
                        if (el != null && el.getSchemaType() != null) {
                            schemaType = el.getSchemaType();
                        }
                        QName typeName = el.getSchemaTypeName();
                        if (typeName == null) {
                            typeName = el.getQName();
                        }
                        QName idltype = getIdlType(wsdlToCorbaBinding,
                                                   schemaType,
                                                   typeName,
                                                   el.isNillable());
                        if ((d2.getName().equals(part.getName()))
                            && (d2.getIdltype().equals(idltype))) {
                            inputs.remove(x);
                            paramtype = createParam(wsdlToCorbaBinding,
                                                    "inout",
                                                    part.getName(),
                                                    idltype);
                            inputs.add(paramtype);
                        }
                    } else {
                        schemaType = getType(part, xmlSchemaList);
                        QName typeName = part.getTypeName();
                        if (isObjectRef) {
                            typeName = part.getElementName();
                        }
                        QName idltype = getIdlType(wsdlToCorbaBinding,
                                                   schemaType,
                                                   typeName,
                                                   false);
                        if ((d2.getName().equals(part.getName()))
                            && (d2.getIdltype().equals(idltype))) {
                            inputs.remove(x);
                            paramtype = createParam(wsdlToCorbaBinding,
                                                    "inout",
                                                    part.getName(),
                                                    idltype);
                            inputs.add(paramtype);
                        }
                    }
                }
                if (paramtype == null) {
                    if (part.getElementName() != null && !isObjectRef) {
                        XmlSchemaElement el = getElement(part, xmlSchemaList);
                        QName typeName = el.getSchemaTypeName();
                        if (typeName == null) {
                            typeName = el.getQName();
                        }
                        QName idltype = getIdlType(wsdlToCorbaBinding,
                                                   schemaType,
                                                   typeName,
                                                   el.isNillable());
                        paramtype = createParam(wsdlToCorbaBinding,
                                                mode,
                                                part.getName(),
                                                idltype);
                    } else {
                        QName typeName = part.getTypeName();
                        if (isObjectRef) {
                            typeName = part.getElementName();
                        }
                        QName idltype = getIdlType(wsdlToCorbaBinding,
                                                   schemaType,
                                                   typeName,
                                                   false);
                        paramtype = createParam(wsdlToCorbaBinding,
                                                mode,
                                                part.getName(),
                                                idltype);
                    }
                    if (paramtype != null) {
                        outputs.add(paramtype);
                    }
                }
            }
        }
    }

    private void processReturnParams(List<ParamType> outputs,
                                     List<ArgType> returns) {

        if (outputs.size() > 0) {
            ParamType d2 = (ParamType) outputs.get(0);

            if (d2.getMode().value().equals("out")) {
                ArgType argType = new ArgType();
                argType.setName(d2.getName());
                argType.setIdltype(d2.getIdltype());
                returns.add(argType);
                outputs.remove(0);
            }
        }
    }

    private void orderParameters(List<ParamType> inputs,
            List<ParamType> outputs, boolean simpleOrdering) {
        ListIterator<ParamType> inputit = inputs.listIterator();

        while (inputit.hasNext()) {
            ParamType d2 = (ParamType) inputit.next();
            if (d2.getMode().value().equals("inout")) {
                ListIterator it = outputs.listIterator();

                while (it.hasNext()) {
                    ParamType d3 = (ParamType) it.next();
                    if (!d3.getName().equals(d2.getName()) 
                        && (!simpleOrdering)
                        && (!d3.getMode().value().equals("inout"))) {
                        // the in/outs are in a different order in the
                        // output than the input
                        // we'll try and use the input oder for the INOUT's,
                        // but also try and
                        // maintain some sort of ordering for the OUT's
                        it.remove();
                        inputit.previous();
                        inputit.add(d3);
                        inputit.next();
                    }
                }
            }
        }
        for (ParamType d3 : outputs) {
            inputs.add(d3);
        }
    }

    private static XmlSchemaType getType(Part part,
                                         SchemaCollection xmlSchemaList)
        throws Exception {
        XmlSchemaType schemaType = null;

        for (XmlSchema xmlSchema : xmlSchemaList.getXmlSchemas()) {
            if (part.getTypeName() != null) {
                schemaType = findSchemaType(xmlSchema, part.getTypeName());                
                if (schemaType != null) {
                    return schemaType;
                }
            }
        }
            
        return schemaType;
    }

    // This willl search the current schemas and any included 
    // schemas for the schema type.
    private static XmlSchemaType findSchemaType(XmlSchema xmlSchema, QName typeName) {
                          
        XmlSchemaType schemaType = xmlSchema.getTypeByName(typeName);

        // Endpoint reference types will give a null schemaType
        // here, so we need to
        // go through the list of imports to find the definition for
        // an Endpoint
        // reference type.
                
        if (schemaType == null
            && xmlSchema.getIncludes().getCount() > 0) {
            XmlSchemaObjectCollection includes = xmlSchema.getIncludes();
            Iterator includeIter = includes.getIterator();
            while (includeIter.hasNext()) {
                Object obj = includeIter.next();
                if (obj instanceof XmlSchemaExternal) {
                    XmlSchemaExternal extSchema = (XmlSchemaExternal) obj;
                    if (extSchema instanceof XmlSchemaImport) {
                        XmlSchemaImport xmlImport = (XmlSchemaImport) obj;
                        if (xmlImport.getNamespace().equals(typeName.getNamespaceURI())) {
                            XmlSchema importSchema = xmlImport.getSchema();
                            schemaType = importSchema.getTypeByName(typeName);
                        }
                    } else {
                        schemaType = findSchemaType(extSchema.getSchema(), typeName);
                        if (schemaType != null) {
                            return schemaType;
                        }
                    }                    
                }                                
            }
            if (schemaType != null) {
                return schemaType;
            }
        }
        return schemaType;
    }

    private static XmlSchemaElement getElement(Part part,
                                               SchemaCollection xmlSchemaList)
        throws Exception {
        XmlSchemaElement schemaElement = null;

        for (XmlSchema xmlSchema : xmlSchemaList.getXmlSchemas()) {
            if (part.getElementName() != null) {
                schemaElement = findElement(xmlSchema, part.getElementName()); 
                if (schemaElement !=  null) {
                    return schemaElement;
                }
            }
        }
        return schemaElement;
    }
    
    // Will check if the schema includes other schemas.
    private static XmlSchemaElement findElement(XmlSchema xmlSchema, QName elName) {
        XmlSchemaElement schemaElement = null;
        
        schemaElement = xmlSchema.getElementByName(elName);
        if (schemaElement == null) {           
            String prefix = definition.getPrefix(elName.getNamespaceURI());
            QName name = new QName(elName.getNamespaceURI(), prefix
                                   + ":" + elName.getLocalPart(), prefix);
            schemaElement = xmlSchema.getElementByName(name);
        }
        if (schemaElement != null) {
            return schemaElement;
        } else {            
            if (xmlSchema.getIncludes() != null) {
                Iterator schemas = xmlSchema.getIncludes().getIterator();
                while (schemas.hasNext()) {
                    Object obj = schemas.next();
                    if (obj instanceof XmlSchemaExternal) {
                        XmlSchemaExternal extSchema = (XmlSchemaExternal) obj;
                        if (!(extSchema instanceof XmlSchemaImport)) {
                            schemaElement = findElement(extSchema.getSchema(), elName);
                            if (schemaElement != null) {
                                return schemaElement;
                            }
                        }
                    }
                }                
            }
        }      
        return schemaElement;
    }

    private static QName getIdlType(WSDLToCorbaBinding wsdlToCorbaBinding,
        XmlSchemaType schemaType, QName typeName, boolean nill)
        throws Exception {
        QName idltype = null;
        CorbaTypeImpl corbaTypeImpl = null;
        if (schemaType == null) {
            corbaTypeImpl = (CorbaTypeImpl) WSDLToCorbaHelper.CORBAPRIMITIVEMAP.get(typeName);
            if (nill) {
                QName qname = corbaTypeImpl.getQName();
                idltype = 
                    wsdlToCorbaBinding.getHelper().createQNameCorbaNamespace(qname.getLocalPart() + "_nil");
            } else {
                if (corbaTypeImpl == null) {
                    XmlSchemaObject schemaObj = getSchemaObject(
                        wsdlToCorbaBinding, typeName);
                    XmlSchemaAnnotation annotation = null;
                    if (schemaObj instanceof XmlSchemaType) {
                        schemaType = (XmlSchemaType) schemaObj;
                    } else if (schemaObj instanceof XmlSchemaElement) {
                        XmlSchemaElement el = (XmlSchemaElement) schemaObj;
                        schemaType = el.getSchemaType();
                        annotation = ((XmlSchemaElement) schemaObj).getAnnotation();
                    }
                    idltype = getSchemaTypeName(wsdlToCorbaBinding, schemaType,
                        annotation, typeName, nill);
                } else {
                    idltype = corbaTypeImpl.getQName();
                }
            }
        } else {
            // We need to get annotation information for the schema type we are
            // about to pass in.
            // This is used to produce the correct object reference type.
            XmlSchemaObject schemaObj = getSchemaObject(wsdlToCorbaBinding, typeName);
            XmlSchemaAnnotation annotation = null;
            if (schemaObj instanceof XmlSchemaElement) {
                annotation = ((XmlSchemaElement) schemaObj).getAnnotation();
            }
            idltype = getSchemaTypeName(wsdlToCorbaBinding, schemaType,
                annotation, typeName, nill);
        }
        return idltype;
    }

    private static XmlSchemaObject getSchemaObject(
        WSDLToCorbaBinding wsdlToCorbaBinding, QName typeName) {

        SchemaCollection schemaList = wsdlToCorbaBinding.getHelper().getXMLSchemaList();
        XmlSchemaObject schemaObj = null;
        for (XmlSchema s : schemaList.getXmlSchemas()) {
            XmlSchemaObjectTable schemaTable = s.getElements();
            schemaObj = schemaTable.getItem(typeName);
            if (schemaObj != null) {
                break;
            }
        }
        return schemaObj;
    }

    private static QName getSchemaTypeName(WSDLToCorbaBinding wsdlToCorbaBinding, 
        XmlSchemaType schemaType, XmlSchemaAnnotation annotation, QName typeName, 
        boolean nill) throws Exception {
        QName idltype = null;
        CorbaTypeImpl corbaTypeImpl = null;

        corbaTypeImpl = wsdlToCorbaBinding.getHelper().convertSchemaToCorbaType(schemaType, 
            typeName, null, annotation, false);
        if (corbaTypeImpl == null) {
            throw new Exception("Couldn't convert schema type to corba type : " + typeName);
        } else {
            if (nill) {
                QName qname = corbaTypeImpl.getQName();
                idltype = 
                    wsdlToCorbaBinding.getHelper().createQNameCorbaNamespace(qname.getLocalPart() + "_nil");
            } else {
                idltype = corbaTypeImpl.getQName();
            }
        }
        return idltype;
    }


    private static ParamType createParam(WSDLToCorbaBinding wsdlToCorbaBinding,
                                         String mode,
                                         String name,
                                         QName idltype)
        throws Exception {
        ParamType paramtype = new ParamType();
        ModeType modeType = ModeType.fromValue(mode);
        paramtype.setName(name);
        paramtype.setMode(modeType);
        paramtype.setIdltype(idltype);
        return paramtype;
    }

    private boolean isWrappedOperation(Operation op, SchemaCollection xmlSchemaList)
        throws Exception {
        Message inputMessage = op.getInput().getMessage();
        Message outputMessage = null;
        if (op.getOutput() != null) {
            outputMessage = op.getOutput().getMessage();
        }

        boolean passedRule = true;

        // RULE No.1:
        // The operation's input and output message (if present) each contain
        // only a single part
        // input message must exist
        if (inputMessage == null || inputMessage.getParts().size() != 1
            || (outputMessage != null && outputMessage.getParts().size() > 1)) {
            passedRule = false;
        }

        if (!passedRule) {
            return false;
        }

        XmlSchemaElement inputEl = null;
        XmlSchemaElement outputEl = null;

        // RULE No.2:
        // The input message part refers to a global element decalration whose
        // localname
        // is equal to the operation name
        Part inputPart = (Part) inputMessage.getParts().values().iterator().next();
        if (inputPart.getElementName() == null) {
            passedRule = false;
        } else {
            QName inputElementName = inputPart.getElementName();
            inputEl = getElement(inputPart, xmlSchemaList);
            if (inputEl == null || !op.getName().equals(inputElementName.getLocalPart())) {
                passedRule = false;
            }
        }
        
        if (!passedRule) {
            return false;
        }

        // RULE No.3:
        // The output message part refers to a global element declaration
        Part outputPart = null;
        if (outputMessage != null && outputMessage.getParts().size() == 1) {
            outputPart = (Part) outputMessage.getParts().values().iterator().next();
            if (outputPart != null) {
                if ((outputPart.getElementName() == null)
                    || getElement(outputPart, xmlSchemaList) == null) {
                    passedRule = false;
                } else {
                    outputEl = getElement(outputPart, xmlSchemaList);
                }
            }
        }
        
        if (!passedRule) {
            return false;
        }

        // RULE No.4 and No5:
        // wrapper element should be pure complex type

        // Now lets see if we have any attributes...
        // This should probably look at the restricted and substitute types too.

        XmlSchemaComplexType xsct = null;
        if (inputEl.getSchemaType() instanceof XmlSchemaComplexType) {
            xsct = (XmlSchemaComplexType)inputEl.getSchemaType();
            if (hasAttributes(xsct)
                || !isWrappableSequence(xsct)) {
                passedRule = false;
            }
        } else {
            passedRule = false;
        }
        
        if (!passedRule) {
            return false;
        }

        if (outputMessage != null) {
            if (outputEl != null && outputEl.getSchemaType() instanceof XmlSchemaComplexType) {
                xsct = (XmlSchemaComplexType)outputEl.getSchemaType();
                if (hasAttributes(xsct)
                    || !isWrappableSequence(xsct)) {
                    passedRule = false;
                }
            } else {
                passedRule = false;
            }
        }       

        return passedRule;
    }

    private boolean hasAttributes(XmlSchemaComplexType complexType) {
        // Now lets see if we have any attributes...
        // This should probably look at the restricted and substitute types too.
        if (complexType.getAnyAttribute() != null || complexType.getAttributes().getCount() > 0) {
            return true;
        }
        return false;
    }

    private boolean isWrappableSequence(XmlSchemaComplexType type) {
        if (type.getParticle() instanceof XmlSchemaSequence) {
            XmlSchemaSequence seq = (XmlSchemaSequence)type.getParticle();
            XmlSchemaObjectCollection items = seq.getItems();

            for (int x = 0; x < items.getCount(); x++) {
                XmlSchemaObject o = items.getItem(x);
                if (!(o instanceof XmlSchemaElement)) {
                    return false;
                }
            }

            return true;
        } else if (type.getParticle() == null) {
            return true;
        }
        return false;
    }

    private boolean isObjectReference(SchemaCollection schemaList, QName name) {
        for (XmlSchema schema : schemaList.getXmlSchemas()) {
            XmlSchemaElement element = schema.getElementByName(name);
            if (element != null) {
                XmlSchemaAnnotation annotation = element.getAnnotation();
                if (annotation != null) {
                    XmlSchemaObjectCollection annotationColl = annotation.getItems();
                    Iterator annotationIter = annotationColl.getIterator();
                    while (annotationIter.hasNext()) {
                        Object o = annotationIter.next();
                        if (o instanceof XmlSchemaAppInfo) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

}
