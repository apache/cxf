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

package org.apache.cxf.javascript.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapBindingFactory;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.common.xmlschema.XmlSchemaUtils;
import org.apache.cxf.javascript.JavascriptUtils;
import org.apache.cxf.javascript.NameManager;
import org.apache.cxf.javascript.NamespacePrefixAccumulator;
import org.apache.cxf.javascript.ParticleInfo;
import org.apache.cxf.javascript.UnsupportedConstruct;
import org.apache.cxf.service.ServiceModelVisitor;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaObjectTable;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaType;

/**
 * Class to construct the JavaScript corresponding to a service.
 */
public class ServiceJavascriptBuilder extends ServiceModelVisitor {
    private static final Logger LOG = LogUtils.getL7dLogger(ServiceJavascriptBuilder.class);

    private SoapBindingInfo soapBindingInfo;
    private JavascriptUtils utils;
    private NameManager nameManager;
    private StringBuilder code;
    private String currentInterfaceClassName;
    private OperationInfo currentOperation;
    private Set<OperationInfo> operationsWithNameConflicts;
    private Set<MessageInfo> inputMessagesWithNameConflicts;
    private Set<MessageInfo> outputMessagesWithNameConflicts;
    private SchemaCollection xmlSchemaCollection;
    // When generating from a tool or ?js, we know the endpoint addr and can build it into the javascript.
    private String endpointAddress;
    
    private boolean isWrapped;
    // facts about the wrapper when there is one.
    private MessagePartInfo inputWrapperPartInfo;
    private String inputWrapperClassName;
    private XmlSchemaElement inputWrapperElement;
    private XmlSchemaComplexType inputWrapperComplexType;

    private MessagePartInfo outputWrapperPartInfo;
    private XmlSchemaElement outputWrapperElement;
    private XmlSchemaComplexType outputWrapperComplexType;

    // Javascript parameter names for the input parameters,
    // derived from the parts.
    private List<String> inputParameterNames = new ArrayList<String>();
    // when not wrapped, we use this to keep track of the bits.
    private List<ParticleInfo> unwrappedElementsAndNames;
    
    private NamespacePrefixAccumulator prefixAccumulator;
    private BindingInfo xmlBindingInfo;
    private Map<String, OperationInfo> localOperationsNameMap;
    private Map<String, MessageInfo> localInputMessagesNameMap;
    private Map<String, MessageInfo> localOutputMessagesNameMap;

    private String opFunctionPropertyName;
    private String opFunctionGlobalName;

    private boolean isInUnwrappedOperation;
    private boolean nonVoidOutput;
    private boolean isRPC;

    /**
     * Construct builder object.
     * @param serviceInfo CXF service model description of the service.
     * @param endpointAddress http:// URL for the service, or null if not known.
     * @param prefixAccumulator object that keeps track of prefixes through an entire WSDL.
     * @param nameManager object that generates names for JavaScript objects.
     */
    public ServiceJavascriptBuilder(ServiceInfo serviceInfo,
                                    String endpointAddress,
                                    NamespacePrefixAccumulator prefixAccumulator,
                                    NameManager nameManager) {
        super(serviceInfo);
        this.endpointAddress = endpointAddress;
        code = new StringBuilder();
        utils = new JavascriptUtils(code);
        this.nameManager = nameManager;
        xmlSchemaCollection = serviceInfo.getXmlSchemaCollection();
        this.prefixAccumulator = prefixAccumulator;
    }

    public String getCode() {
        return code.toString();
    }

    @Override
    public void begin(FaultInfo fault) {
    }

    @Override
    public void begin(InterfaceInfo intf) {
        code.append("\n// Javascript for " + intf.getName() + "\n\n");

        currentInterfaceClassName = nameManager.getJavascriptName(intf.getName());
        operationsWithNameConflicts = new HashSet<OperationInfo>();
        inputMessagesWithNameConflicts = new HashSet<MessageInfo>();
        outputMessagesWithNameConflicts = new HashSet<MessageInfo>();
        localOperationsNameMap = new HashMap<String, OperationInfo>();
        localInputMessagesNameMap = new HashMap<String, MessageInfo>();
        localOutputMessagesNameMap = new HashMap<String, MessageInfo>();

        code.append("function " + currentInterfaceClassName + " () {\n");
        utils.appendLine("this.jsutils = new CxfApacheOrgUtil();");
        utils.appendLine("this.jsutils.interfaceObject = this;");
        utils.appendLine("this.synchronous = false;");
        if (endpointAddress != null) {
            utils.appendLine("this.url = '" + endpointAddress + "';");
        } else {
            utils.appendLine("this.url = null;");
        }
        utils.appendLine("this.client = null;");
        utils.appendLine("this.response = null;");
        generateGlobalElementDictionary();
        code.append("}\n\n");
    }
    
    private void generateGlobalElementDictionary() {
        // to handle 'any', we need a dictionary of all the global elements of all the schemas.
        utils.appendLine("this.globalElementSerializers = [];");
        utils.appendLine("this.globalElementDeserializers = [];");
        for (XmlSchema schemaInfo : xmlSchemaCollection.getXmlSchemas()) {
            XmlSchemaObjectTable globalElements = schemaInfo.getElements();
            Iterator namesIterator = globalElements.getNames();
            while (namesIterator.hasNext()) {
                QName name = (QName)namesIterator.next();
                XmlSchemaElement element = (XmlSchemaElement) globalElements.getItem(name);
                // For now, at least, don't handle elements with simple types.
                // That comes later to improve deserialization.
                if (JavascriptUtils.notVeryComplexType(element.getSchemaType())) {
                    continue;
                }
                // If the element uses a named type, we use the functions for the type.
                XmlSchemaComplexType elementType = (XmlSchemaComplexType)element.getSchemaType();
                if (elementType != null && elementType.getQName() != null) {
                    name = elementType.getQName();
                }
                utils.appendLine("this.globalElementSerializers['" + name.toString() + "'] = "
                                 + nameManager.getJavascriptName(name)
                                 + "_serialize;");
                utils.appendLine("this.globalElementDeserializers['" + name.toString() + "'] = "
                                 + nameManager.getJavascriptName(name)
                                 + "_deserialize;");
            }
            
            globalElements = schemaInfo.getSchemaTypes();
            namesIterator = globalElements.getNames();
            while (namesIterator.hasNext()) {
                QName name = (QName)namesIterator.next();
                XmlSchemaType type = (XmlSchemaType) globalElements.getItem(name);
                // For now, at least, don't handle simple types.
                if (JavascriptUtils.notVeryComplexType(type)) {
                    continue;
                }
                // the names are misleading, but that's OK.
                utils.appendLine("this.globalElementSerializers['" + name.toString() + "'] = "
                                 + nameManager.getJavascriptName(name)
                                 + "_serialize;");
                utils.appendLine("this.globalElementDeserializers['" + name.toString() + "'] = "
                                 + nameManager.getJavascriptName(name)
                                 + "_deserialize;");
            }
        }
    }

    private String getFunctionGlobalName(QName itemName, String itemType) {
        return nameManager.getJavascriptName(itemName) + "_" + itemType; 
    }
    
    
    private<T> String getFunctionPropertyName(Set<T> conflictMap, T object, QName fullName) {
        boolean needsLongName = conflictMap.contains(object);
        String functionName;
        if (needsLongName) {
            functionName = nameManager.getJavascriptName(fullName);
        } else {
            functionName = JavascriptUtils.javaScriptNameToken(fullName.getLocalPart());
        }
        return functionName;
        
    }

    // we do this at the end so we can inventory name conflicts sooner.
    @Override
    public void end(OperationInfo op) {
        // we only process the wrapped operation, not the unwrapped alternative.
        if (op.isUnwrapped()) {
            isInUnwrappedOperation = false;
            return;
        }

        isWrapped = op.isUnwrappedCapable();
        
        StringBuilder parameterList = new StringBuilder();

        inputParameterNames = new ArrayList<String>();
        
        if (isWrapped) {
            collectWrapperElementInfo();
        } else {
            collectUnwrappedInputInfo();
        }

        buildParameterList(parameterList);

        MessageInfo outputMessage = op.getOutput();
        nonVoidOutput = outputMessage != null && outputMessage.getMessageParts().size() != 0; 

        if (!op.isOneWay()) {
            buildSuccessFunction(outputMessage);
            buildErrorFunction(); // fault part some day.
        }

        buildOperationFunction(parameterList);

        createInputSerializer();

        if (nonVoidOutput) {
            createResponseDeserializer(outputMessage);
        } 
    }

    /**
     * visit the input message parts and collect relevant data.
     */
    private void collectUnwrappedInputInfo() {
        unwrappedElementsAndNames = new ArrayList<ParticleInfo>();
        if (currentOperation.getInput() != null) {
            getElementsForParts(currentOperation.getInput(), unwrappedElementsAndNames);
        }
        
        for (ParticleInfo ean : unwrappedElementsAndNames) {
            inputParameterNames.add(ean.getJavascriptName());
        }
    }
    
    

    private void buildOperationFunction(StringBuilder parameterList) {
        String responseCallbackParams = "";
        if (!currentOperation.isOneWay()) {
            responseCallbackParams = "successCallback, errorCallback";
        }
        
        MessageInfo inputMessage = currentOperation.getInput();

        code.append("//\n");
        code.append("// Operation " + currentOperation.getName() + "\n");
        if (!isWrapped) {
            code.append("// - bare operation. Parameters:\n");
            for (ParticleInfo ei : unwrappedElementsAndNames) {
                code.append("// - " + getElementObjectName(ei) + "\n");
            }
        } else {
            code.append("// Wrapped operation.\n");
            QName contextQName = inputWrapperComplexType.getQName();
            if (contextQName == null) {
                contextQName = inputWrapperElement.getQName();
            }
            XmlSchemaSequence sequence = XmlSchemaUtils.getSequence(inputWrapperComplexType);
            XmlSchema wrapperSchema = 
                xmlSchemaCollection.getSchemaByTargetNamespace(contextQName.getNamespaceURI());

            for (int i = 0; i < sequence.getItems().getCount(); i++) {
                code.append("// parameter " + inputParameterNames.get(i) + "\n");
                XmlSchemaObject sequenceItem = sequence.getItems().getItem(i);
                ParticleInfo itemInfo = ParticleInfo.forLocalItem(sequenceItem,
                                                                  wrapperSchema,
                                                                  xmlSchemaCollection,
                                                                  prefixAccumulator,
                                                                  contextQName); 
                if (itemInfo.isArray()) {
                    code.append("// - array\n");
                }
                
                XmlSchemaType type = itemInfo.getType(); // null for an any.
                if (type instanceof XmlSchemaComplexType) {
                    QName baseName;
                    if (type.getQName() != null) {
                        baseName = type.getQName();
                    } else {
                        baseName = ((XmlSchemaElement)sequenceItem).getQName();
                    }
                    code.append("// - Object constructor is " 
                                    + nameManager.getJavascriptName(baseName) + "\n");
                } else if (type != null) {
                    code.append("// - simple type " + type.getQName());
                }
            }       
        }
        
        code.append("//\n");
        
        code.append("function " 
                    +  opFunctionGlobalName
                    + "("  + responseCallbackParams
                    + ((parameterList.length() > 0 && !currentOperation.isOneWay()) 
                        ? ", " : "") + parameterList + ") {\n");
        utils.appendLine("this.client = new CxfApacheOrgClient(this.jsutils);");
        utils.appendLine("var xml = null;");
        if (inputMessage != null) {
            utils.appendLine("var args = new Array(" + inputParameterNames.size() + ");");
            int px = 0;
            for (String param : inputParameterNames) {
                utils.appendLine("args[" + px + "] = " + param + ";");
                px++;
            }
            utils.appendLine("xml = this."
                             + getFunctionPropertyName(inputMessagesWithNameConflicts,
                                                       inputMessage, 
                                                       inputMessage.getName())
                             + "_serializeInput"
                             + "(this.jsutils, args);");
        }

        // we need to pass the caller's callback functions to our callback
        // functions.
        if (!currentOperation.isOneWay()) {
            utils.appendLine("this.client.user_onsuccess = successCallback;");
            utils.appendLine("this.client.user_onerror = errorCallback;");
            utils.appendLine("var closureThis = this;");
            // client will pass itself and the response XML.
            utils.appendLine("this.client.onsuccess = function(client, responseXml) { closureThis." 
                                 + opFunctionPropertyName
                                 + "_onsuccess(client, responseXml); };");
            // client will pass itself.
            utils.appendLine("this.client.onerror = function(client) { closureThis."
                             + opFunctionPropertyName
                             + "_onerror(client); };");
        }
        utils.appendLine("var requestHeaders = [];");

        if (soapBindingInfo != null) {
            String action = soapBindingInfo.getSoapAction(currentOperation);
            utils.appendLine("requestHeaders['SOAPAction'] = '" + action + "';");
        }

        // default 'method' by passing null. Is there some place this lives in the
        // service model?
        String syncAsyncFlag;
        if (currentOperation.isOneWay()) {
            utils.appendLine("this.jsutils.trace('oneway operation');");
            syncAsyncFlag = "false";
        } else {
            utils.appendLine("this.jsutils.trace('synchronous = ' + this.synchronous);");
            syncAsyncFlag = "this.synchronous";
        }
        utils.appendLine("this.client.request(this.url, xml, null, "
                         + syncAsyncFlag + ", requestHeaders);");

        code.append("}\n\n");
        code.append(currentInterfaceClassName + ".prototype." 
                    + opFunctionPropertyName 
                    + " = " 
                    + opFunctionGlobalName
                    + ";\n\n");
    }

    private void buildErrorFunction() {
        String errorFunctionPropertyName = opFunctionPropertyName + "_onerror";
        String errorFunctionGlobalName = opFunctionGlobalName + "_onerror";
        
        code.append("function " + errorFunctionGlobalName + "(client) {\n");
        utils.startIf("client.user_onerror");
        // Is this a good set of parameters for the error function?
        // Not if we want to process faults, it isn't. To be revisited.
        utils.appendLine("var httpStatus;");
        utils.appendLine("var httpStatusText;");
        utils.appendLine("try {");
        utils.appendLine(" httpStatus = client.req.status;");
        utils.appendLine(" httpStatusText = client.req.statusText;");
        utils.appendLine("} catch(e) {");
        utils.appendLine(" httpStatus = -1;");
        utils.appendLine(" httpStatusText = 'Error opening connection to server';");
        utils.appendLine("}");
        utils.appendLine("client.user_onerror(httpStatus, httpStatusText);");
        utils.endBlock();
        code.append("}\n\n");
        code.append(currentInterfaceClassName + ".prototype." 
                    + errorFunctionPropertyName 
                    + " = "
                    + errorFunctionGlobalName 
                    + ";\n\n");
    }

    // Note: the response XML that we get from the XMLHttpRequest is the document element,
    // not the root element.
    private void buildSuccessFunction(MessageInfo outputMessage) {
        // Here are the success and error callbacks. They have the job of
        // calling callbacks provided to the operation function with appropriate
        // parameters.
        String successFunctionGlobalName = opFunctionGlobalName + "_onsuccess"; 
        String successFunctionPropertyName = opFunctionPropertyName + "_onsuccess"; 
        String arglist = "(client)";
        if (nonVoidOutput) {
            arglist = "(client, responseXml)";
        }
        
        code.append("function " + successFunctionGlobalName + arglist + " {\n");
        utils.startIf("client.user_onsuccess");
        utils.appendLine("var responseObject = null;");
        if (nonVoidOutput) {
            utils.appendLine("var element = responseXml.documentElement;");
            utils.appendLine("this.jsutils.trace('responseXml: ' "
                             + "+ this.jsutils.traceElementName(element));");

            if (soapBindingInfo != null) { // soap
                // The following code is probably only right for basic
                // Doc/Literal/Wrapped services.
                // the top element should be the Envelope, then the Body, then
                // the actual response item.
                utils.appendLine("element = this.jsutils.getFirstElementChild(element);");
                utils.appendLine("this.jsutils.trace('first element child: ' "
                                 + "+ this.jsutils.traceElementName(element));");
                // loop to find the body.
                utils.startWhile("!this.jsutils.isNodeNamedNS(element, "
                                 + "'http://schemas.xmlsoap.org/soap/envelope/', 'Body')");
                utils.appendLine("element = this.jsutils.getNextElementSibling(element);");
                utils.startIf("element == null");
                utils.appendLine("throw 'No env:Body in message.'");
                utils.endBlock();
                utils.endBlock();
                // Go down one more from the body to the response item.
                utils.appendLine("element = this.jsutils.getFirstElementChild(element);");
                utils.appendLine("this.jsutils.trace('part element: ' "
                                 + "+ this.jsutils.traceElementName(element));");
            } 
            String deserializerFunctionName = outputDeserializerFunctionName(outputMessage);
            utils.appendLine("this.jsutils.trace('calling " + deserializerFunctionName + "');");
            utils.appendLine("responseObject = " + deserializerFunctionName + "(this.jsutils, element);");
        }
        utils.appendLine("client.user_onsuccess(responseObject);");
        utils.endBlock();
        code.append("}\n\n");
        code.append(currentInterfaceClassName + ".prototype." 
                    + successFunctionPropertyName 
                    + " = "
                    + successFunctionGlobalName + ";\n\n");
    }

    private void buildParameterList(StringBuilder parameterList) {
        for (String param : inputParameterNames) {
            parameterList.append(param);
            parameterList.append(", ");
        }
        // trim last comma.
        if (parameterList.length() > 2) {
            parameterList.setLength(parameterList.length() - 2);
        }
    }

    private String outputDeserializerFunctionName(MessageInfo message) {
        return getFunctionGlobalName(message.getName(), "deserializeResponse");
    }

    // This ignores 'wrapped', because it assumes one part that we can use one way or 
    // the other. For simple cases, this is certainly OK.
    private void createResponseDeserializer(MessageInfo outputMessage) {
        List<MessagePartInfo> parts = outputMessage.getMessageParts();
        if (parts.size() != 1) {
            unsupportedConstruct("MULTIPLE_OUTPUTS", outputMessage.getName().toString());
        }
        List<ParticleInfo> elements = new ArrayList<ParticleInfo>();
        String functionName = outputDeserializerFunctionName(outputMessage);
        code.append("function " + functionName + "(cxfjsutils, partElement) {\n");
        getElementsForParts(outputMessage, elements);
        ParticleInfo element = elements.get(0);
        XmlSchemaType type = null;
        
        if (isRPC) {
            utils.appendLine("cxfjsutils.trace('rpc element: ' + cxfjsutils.traceElementName(partElement));");
            utils.appendLine("partElement = cxfjsutils.getFirstElementChild(partElement);");
            utils.appendLine("cxfjsutils.trace('rpc element: ' + cxfjsutils.traceElementName(partElement));");
        }
        
        type = element.getType();
        
        if (!element.isEmpty()) {
            if (type instanceof XmlSchemaComplexType) {
                String typeObjectName = nameManager.getJavascriptName(element.getControllingName());
                utils
                    .appendLine("var returnObject = " 
                                + typeObjectName 
                                + "_deserialize (cxfjsutils, partElement);\n");
            } else if (type instanceof XmlSchemaSimpleType) {
                XmlSchemaSimpleType simpleType = (XmlSchemaSimpleType)type;
                utils.appendLine("var returnText = cxfjsutils.getNodeText(partElement);");
                utils.appendLine("var returnObject = " 
                                 + utils.javascriptParseExpression(simpleType, "returnText") + ";");
            } 
            utils.appendLine("return returnObject;");
        }
        code.append("}\n");
    }
    
    private String getElementObjectName(ParticleInfo element) {
        XmlSchemaType type = element.getType();

        if (!element.isEmpty()) {
            if (type instanceof XmlSchemaComplexType) {
                return nameManager.getJavascriptName(element.getControllingName());
            } else {
                return "type " + type.getQName(); // could it be anonymous?
            } 
        } else {
            return "empty element?";
        }
    }

    private void createInputSerializer() {
        
        // If are working on a wrapped method, then we use the wrapper element.
        // If we are working on an unwrapped method, we will have to work from the unwrapped parts.
        
        MessageInfo message = currentOperation.getInput();
        String serializerFunctionGlobalName = getFunctionGlobalName(message.getName(), "serializeInput");
        String serializerFunctionPropertyName = 
            getFunctionPropertyName(inputMessagesWithNameConflicts, message, message.getName())
            + "_serializeInput";

        code.append("function " + serializerFunctionGlobalName + "(cxfjsutils, args) {\n");

        String wrapperXmlElementName = null; 
        // for the wrapped case, we can name the object for Javascript after whatever we like.
        // we could use the wrapped part, or we could use a conventional name.
        if (isWrapped) {
            wrapperXmlElementName = 
                prefixAccumulator.xmlElementString(inputWrapperPartInfo.getConcreteName());
            utils.appendLine("var wrapperObj = new " + inputWrapperClassName + "();");
            int px = 0;
            for (String param : inputParameterNames) {
                utils.appendLine("wrapperObj.set" + StringUtils.capitalize(param) + "(args[" + px
                                 + "]);");
                px++;
            }
        }

        if (soapBindingInfo != null) {
            SoapVersion soapVersion = soapBindingInfo.getSoapVersion();
            assert soapVersion.getVersion() == 1.1;
            utils.appendLine("var xml;");
            utils.appendLine("xml = cxfjsutils.beginSoap11Message(\"" + prefixAccumulator.getAttributes()
                             + "\");");
        } else {
            // other alternative is XML, which isn't really all here yet.
            unsupportedConstruct("XML_BINDING", currentInterfaceClassName, xmlBindingInfo.getName());
        }

        utils.setXmlStringAccumulator("xml");
        
        if (isWrapped) {
            ParticleInfo elementInfo = ParticleInfo.forPartElement(inputWrapperElement,
                                                                 xmlSchemaCollection,
                                                                 "wrapperObj",
                                                                 wrapperXmlElementName);

            elementInfo.setContainingType(null);
            utils.generateCodeToSerializeElement(elementInfo, "", xmlSchemaCollection);
        } else {
            String operationXmlElement = null;
            if (isRPC) {
                operationXmlElement = 
                    prefixAccumulator.xmlElementString(currentOperation.getName());

                // RPC has a level of element for the entire operation.
                // we might have some schema to model this, but the following seems
                // sufficient.
                utils.appendString("<" + operationXmlElement + ">");                
            }
            int px = 0;
            // Multiple parts for document violates WS-I, but we can still do them.
            // They are normal for RPC.
            // Parts are top-level elements. As such, they cannot, directly, be arrays.
            // If a part is declared as an array type, the schema has a non-array element
            // with a complex type consisting of an element with array bounds. We don't 
            // want the JavasSript programmer to have to concoct an extra level of object
            // (though if the same sort of thing happens elsewhere due to an XmlRootElement,
            // the JavaScript programmer is stuck with the situation). 
            for (ParticleInfo ean : unwrappedElementsAndNames) {
                String savedjsName = ean.getJavascriptName();
                try {
                    ean.setJavascriptName("args[" + px + "]");
                    utils.generateCodeToSerializeElement(ean, "", xmlSchemaCollection);
                    px++;
                } finally {
                    ean.setJavascriptName(savedjsName);
                    
                }
            }
            if (isRPC) {
                utils.appendString("</" + operationXmlElement + ">");                
            }
        }

        utils.appendLine("xml = xml + cxfjsutils.endSoap11Message();");
        utils.appendLine("return xml;");
        code.append("}\n\n");
        code.append(currentInterfaceClassName + ".prototype."
                    + serializerFunctionPropertyName 
                    + " = "
                    + serializerFunctionGlobalName + ";\n\n");
    }
    
    private XmlSchemaSequence getTypeSequence(XmlSchemaComplexType type, 
                                              QName parentName) {
        if (!(type.getParticle() instanceof XmlSchemaSequence)) {
            unsupportedConstruct("NON_SEQUENCE_PARTICLE", 
                                 type.getQName() != null ? type.getQName() 
                                     :
                                     parentName);
        }
        return (XmlSchemaSequence)type.getParticle();
    }
    
    private boolean isEmptyType(XmlSchemaType type, QName parentName) {
        if (type instanceof XmlSchemaComplexType) {
            XmlSchemaComplexType complexType = (XmlSchemaComplexType)type;
            if (complexType.getParticle() == null) {
                return true;
            } 
            XmlSchemaSequence sequence = getTypeSequence(complexType, parentName);
            if (sequence.getItems().getCount() == 0) {
                return true;
            }
        }
        return false;
    }
    
    private XmlSchemaParticle getBuriedElement(XmlSchemaComplexType type, 
                                              QName parentName) {
        XmlSchemaSequence sequence = getTypeSequence(type, parentName);
        XmlSchemaObjectCollection insides = sequence.getItems();
        if (insides.getCount() == 1) {
            XmlSchemaObject item = insides.getItem(0);
            if (item instanceof XmlSchemaElement || item instanceof XmlSchemaAny) {
                return (XmlSchemaParticle) item;
            }
        }
        return null;
    }

    /**
     * Collect information about the parts of an unwrapped message.
     * @param parts 
     * @param elements
     */
    private void getElementsForParts(MessageInfo message, List<ParticleInfo> elements) {
        for (MessagePartInfo mpi : message.getMessageParts()) {
            XmlSchemaElement element = null;
            XmlSchemaType type = null; 
            QName diagnosticName = mpi.getName();
            if (mpi.isElement()) {
                element = (XmlSchemaElement)mpi.getXmlSchema();
                if (element == null) {
                    element = XmlSchemaUtils.findElementByRefName(xmlSchemaCollection, mpi.getElementQName(),
                                                                  serviceInfo.getTargetNamespace());
                }
                diagnosticName = element.getQName();
                type = element.getSchemaType();
                if (type == null) {
                    type = XmlSchemaUtils.getElementType(xmlSchemaCollection, 
                                                         null, 
                                                         element, 
                                                         null);
                }
            } else {
                // RPC (!isElement)
                type = (XmlSchemaType)mpi.getXmlSchema();
                if (type == null) {
                    type = xmlSchemaCollection.getTypeByQName(mpi.getTypeQName());
                    diagnosticName = type.getQName();
                }
               
            }
            
            boolean empty = isEmptyType(type, diagnosticName);
            // There's something funny about doc/bare. Since it's doc, there is no
            // element in the part. There is a type. However, for some reason,
            // it tends to be an anonymous complex type containing an element, and that
            // element corresponds to the type of the parameter. So, we refocus on that.
            if (!empty 
                && type instanceof XmlSchemaComplexType 
                && type.getName() == null 
                && !isWrapped) {
                XmlSchemaParticle betterElement = getBuriedElement((XmlSchemaComplexType) type,
                                                                  diagnosticName);
                if (betterElement instanceof XmlSchemaElement) {
                    element = (XmlSchemaElement)betterElement;
                    if (element.getSchemaType() == null) {
                        element.setSchemaType(xmlSchemaCollection
                                                  .getTypeByQName(element.getSchemaTypeName()));
                    }
                    type = element.getSchemaType();
                }
            }
            
            String partJavascriptVar = 
                JavascriptUtils.javaScriptNameToken(mpi.getConcreteName().getLocalPart());
            String elementXmlRef = prefixAccumulator.xmlElementString(mpi.getConcreteName());
            ParticleInfo elementInfo = ParticleInfo.forPartElement(element,
                                                                 xmlSchemaCollection,
                                                                 partJavascriptVar, 
                                                                 elementXmlRef);        
            // the type may have been recalculated above.
            elementInfo.setType(type);
            elementInfo.setEmpty(empty);
            elements.add(elementInfo);
        }
    }
    
    // This function finds all the information for the wrapper.
    private void collectWrapperElementInfo() {
        
        if (currentOperation.getInput() != null) {
            inputWrapperPartInfo = currentOperation.getInput().getMessagePart(0);

            List<MessagePartInfo> unwrappedParts = 
                currentOperation.getUnwrappedOperation().getInput().getMessageParts();

            for (MessagePartInfo mpi : unwrappedParts) {
                String jsParamName = JavascriptUtils.javaScriptNameToken(mpi.getName().getLocalPart());
                inputParameterNames.add(jsParamName);
            }

            inputWrapperPartInfo = currentOperation.getInput().getMessagePart(0);
            assert inputWrapperPartInfo.isElement();

            inputWrapperElement = (XmlSchemaElement)inputWrapperPartInfo.getXmlSchema();
            if (inputWrapperElement == null) {
                inputWrapperElement = 
                    XmlSchemaUtils.findElementByRefName(xmlSchemaCollection, 
                                                        inputWrapperPartInfo.getElementQName(),
                                                        serviceInfo.getTargetNamespace());
            }
            inputWrapperComplexType = (XmlSchemaComplexType)inputWrapperElement.getSchemaType();
            // the null name is probably something awful in RFSB.
            if (inputWrapperComplexType == null) {
                inputWrapperComplexType = (XmlSchemaComplexType)
                    XmlSchemaUtils.getElementType(xmlSchemaCollection, 
                                                  serviceInfo.getTargetNamespace(), 
                                                  inputWrapperElement, 
                                                  null);
            }
            if (inputWrapperComplexType == null) {
                unsupportedConstruct("MISSING_WRAPPER_TYPE",
                                     currentOperation.getInterface().getName(),
                                     currentOperation.getName(), 
                                     inputWrapperPartInfo.getName());
            }
            
            if (inputWrapperComplexType.getQName() == null) {
                // we should be ignoring this for zero-argument wrappers.
                if (inputWrapperPartInfo.isElement()) {
                    inputWrapperClassName = nameManager.
                        getJavascriptName(inputWrapperPartInfo.getElementQName());
                } else {
                    unsupportedConstruct("NON_ELEMENT_ANON_TYPE_PART", 
                                         inputWrapperPartInfo.getMessageInfo().getName(),
                                         inputWrapperPartInfo.getName());
                }
            } else {
                inputWrapperClassName = nameManager.getJavascriptName(inputWrapperComplexType.getQName());
            }
            
        }

        if (currentOperation.getOutput() != null) {
            outputWrapperPartInfo = currentOperation.getOutput().getMessagePart(0);
            assert outputWrapperPartInfo.isElement();

            outputWrapperElement = (XmlSchemaElement)outputWrapperPartInfo.getXmlSchema();
            if (outputWrapperElement == null) {
                outputWrapperElement = 
                    XmlSchemaUtils.findElementByRefName(xmlSchemaCollection, 
                                                        outputWrapperPartInfo.getElementQName(),
                                                        serviceInfo.getTargetNamespace());
            }
            outputWrapperComplexType = (XmlSchemaComplexType)outputWrapperElement.getSchemaType();
            if (outputWrapperComplexType == null) {
                outputWrapperComplexType = (XmlSchemaComplexType)
                    XmlSchemaUtils.getElementType(xmlSchemaCollection, 
                                                  serviceInfo.getTargetNamespace(), 
                                                  outputWrapperElement, 
                                                  null);
            }
        }
    }

    @Override
    public void begin(ServiceInfo service) {
        
        code.append("//\n");
        code.append("// Definitions for service: " + service.getName().toString() + "\n");
        code.append("//\n");

        BindingInfo xml = null;
        // assume only one soap binding.
        // until further consideration.
        // hypothetically, we could generate two different JavaScript classes,
        // one for each.
        for (BindingInfo bindingInfo : service.getBindings()) {
            // there is a JIRA about the confusion / profusion of URLS here.
            if (SoapBindingConstants.SOAP11_BINDING_ID.equals(bindingInfo.getBindingId())
                || SoapBindingConstants.SOAP12_BINDING_ID.equals(bindingInfo.getBindingId())
                || SoapBindingFactory.SOAP_11_BINDING.equals(bindingInfo.getBindingId())
                || SoapBindingFactory.SOAP_12_BINDING.equals(bindingInfo.getBindingId())
                ) {
                SoapBindingInfo sbi = (SoapBindingInfo)bindingInfo;
                if (WSDLConstants.NS_SOAP11_HTTP_TRANSPORT.equals(sbi.getTransportURI())
                    || WSDLConstants.NS_SOAP12_HTTP_TRANSPORT.equals(sbi.getTransportURI())
                    // we may want this for testing.
                    || LocalTransportFactory.TRANSPORT_ID.equals(sbi.getTransportURI())) {
                    soapBindingInfo = sbi;
                    break;
                }
            } else if (WSDLConstants.NS_BINDING_XML.equals(bindingInfo.getBindingId())) {
                xml = bindingInfo;
            }
        }

        // For now, we use soap if its available, and XML if it isn't.\
        if (soapBindingInfo == null && xml == null) {
            unsupportedConstruct("NO_USABLE_BINDING", service.getName());
        }

        if (soapBindingInfo != null) {
            isRPC = WSDLConstants.RPC.equals(soapBindingInfo.getStyle());
        } else if (xml != null) {
            xmlBindingInfo = xml;
        }
    }

    @Override
    public void end(FaultInfo fault) {
    }

    @Override
    public void end(InterfaceInfo intf) {
    }

    @Override
    public void end(MessageInfo msg) {
    }

    @Override
    public void end(MessagePartInfo part) {
    }

    @Override
    public void end(ServiceInfo service) {
        LOG.finer(getCode());
    }

    private void unsupportedConstruct(String messageKey, Object... args) {
        Message message = new Message(messageKey, LOG, args);
        throw new UnsupportedConstruct(message);
    }

    @Override
    public void begin(OperationInfo op) {
        if (op.isUnwrapped()) {
            isInUnwrappedOperation = true;
            return;
        }
        currentOperation = op;
        OperationInfo conflict = localOperationsNameMap.get(op.getName().getLocalPart());
        if (conflict != null) {
            operationsWithNameConflicts.add(conflict);
            operationsWithNameConflicts.add(op);
        }
        localOperationsNameMap.put(op.getName().getLocalPart(), op);
        opFunctionPropertyName = getFunctionPropertyName(operationsWithNameConflicts, op, op.getName());
        opFunctionGlobalName = getFunctionGlobalName(op.getName(), "op");
    }
    
    @Override
    public void begin(MessageInfo msg) {
        if (isInUnwrappedOperation) {
            return;
        }
        LOG.fine("Message " + msg.getName().toString());
        Map<String, MessageInfo> nameMap;
        Set<MessageInfo> conflicts;
        if (msg.getType() == MessageInfo.Type.INPUT) {
            nameMap = localInputMessagesNameMap;
            conflicts = inputMessagesWithNameConflicts;
        } else {
            nameMap = localOutputMessagesNameMap;
            conflicts = outputMessagesWithNameConflicts;

        }
        MessageInfo conflict = nameMap.get(msg.getName().getLocalPart());
        if (conflict != null) {
            conflicts.add(conflict);
            conflicts.add(msg);
        }
        nameMap.put(msg.getName().getLocalPart(), msg);
    }

    @Override
    public void begin(EndpointInfo endpointInfo) {
        String address = endpointInfo.getAddress();
        String portClassName = currentInterfaceClassName + "_" 
            + nameManager.getJavascriptName(endpointInfo.getName());
        code.append("function " + portClassName + " () {\n");
        code.append("  this.url = '" + address + "';\n");
        code.append("}\n");
        code.append(portClassName + ".prototype = new " + currentInterfaceClassName + ";\n");
    }


}
