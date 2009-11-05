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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.wsdl.OperationType;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxb.JAXBUtils;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.apache.cxf.tools.common.model.JavaParameter;
import org.apache.cxf.tools.common.model.JavaReturn;
import org.apache.cxf.tools.common.model.JavaType;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.customization.JAXWSBinding;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.customization.JAXWSParameter;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator.WebParamAnnotator;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator.XmlJavaTypeAdapterAnnotator;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator.XmlListAnotator;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.mapper.ParameterMapper;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeList;
import org.apache.ws.commons.schema.constants.Constants;

public class ParameterProcessor extends AbstractProcessor {
    public static final String HEADER = "messagepart.isheader";
    public static final String OUT_OF_BAND_HEADER = "messagepart.is_out_of_band_header";

    private DataBindingProfile dataBinding;

    public ParameterProcessor(ToolContext penv) {
        super(penv);
        dataBinding = context.get(DataBindingProfile.class);
    }

    private boolean isRequestResponse(JavaMethod method) {
        return method.getStyle() == OperationType.REQUEST_RESPONSE;
    }

    public void process(JavaMethod method,
                        MessageInfo inputMessage,
                        MessageInfo outputMessage,
                        List<String> parameterOrder) throws ToolException {

        if (!StringUtils.isEmpty(parameterOrder)
            && isValidOrdering(parameterOrder, inputMessage, outputMessage)
            && !method.isWrapperStyle()) {

            buildParamModelsWithOrdering(method,
                                         inputMessage,
                                         outputMessage,
                                         parameterOrder);
        } else {
            buildParamModelsWithoutOrdering(method,
                                            inputMessage,
                                            outputMessage);
        }
    }

    /**
     * This method will be used by binding processor to change existing
     * generated java method of porttype
     *
     * @param method
     * @param part
     * @param style
     * @throws ToolException
     */
    public JavaParameter addParameterFromBinding(JavaMethod method,
                                                 MessagePartInfo part,
                                                 JavaType.Style style)
        throws ToolException {
        return addParameter(method, getParameterFromPart(method, part, style));
    }

    private JavaParameter getParameterFromPart(JavaMethod jm, MessagePartInfo part, JavaType.Style style) {
        return ParameterMapper.map(jm, part, style, context);
    }

    protected JavaParameter addParameter(JavaMethod method, JavaParameter parameter) throws ToolException {
        if (parameter == null) {
            return null;
        }
        String name = parameter.getName();
        int count = 0;
        while (method.getParameter(parameter.getName()) != null
            && context.optionSet(ToolConstants.CFG_AUTORESOLVE)
            && parameter.getStyle() != JavaType.Style.INOUT) {
            parameter.setName(name + (++count));
        }
        
        parameter.setMethod(method);
        parameter.annotate(new WebParamAnnotator());
        method.addParameter(parameter);

        return parameter;
    }

    private void processReturn(JavaMethod method, MessagePartInfo part) {
        String name = part == null ? "return" : part.getName().getLocalPart();
        String type = part == null ? "void" : ProcessorUtil.resolvePartType(part, context);

        String namespace = part == null ? null : ProcessorUtil.resolvePartNamespace(part);

        JavaReturn returnType = new JavaReturn(name, type, namespace);
        if (part != null) {
            returnType.setDefaultValueWriter(ProcessorUtil.getDefaultValueWriter(part, context));
        }

        returnType.setQName(ProcessorUtil.getElementName(part));
        returnType.setStyle(JavaType.Style.OUT);
        if (namespace != null && type != null && !"void".equals(type)) {
            returnType.setClassName(ProcessorUtil.getFullClzName(part, context, false));
        }
        
        if (part != null && part.getXmlSchema() instanceof XmlSchemaSimpleType) {
            processXmlSchemaSimpleType((XmlSchemaSimpleType)part.getXmlSchema(), method, part);
        } else if (part != null && part.getXmlSchema() instanceof XmlSchemaElement) {
            XmlSchemaElement element = (XmlSchemaElement)part.getXmlSchema();
            if (element.getSchemaType() instanceof XmlSchemaSimpleType) {
                processXmlSchemaSimpleType((XmlSchemaSimpleType)element.getSchemaType(), method, part);
            }
        }
        
        method.setReturn(returnType);
    }
    
    private static void processXmlSchemaSimpleType(XmlSchemaSimpleType xmlSchema, JavaMethod method,
                                                   MessagePartInfo part) {
        if (xmlSchema.getContent() instanceof XmlSchemaSimpleTypeList
            && (!part.isElement() || !method.isWrapperStyle())) {
            method.annotate(new XmlListAnotator(method.getInterface()));
        }
        if (Constants.XSD_HEXBIN.equals(xmlSchema.getQName())
            && (!part.isElement() || !method.isWrapperStyle())) {
            method.annotate(new XmlJavaTypeAdapterAnnotator(method.getInterface(), HexBinaryAdapter.class));
        }
    }

    private boolean isOutOfBandHeader(final MessagePartInfo part) {
        return Boolean.TRUE.equals(part.getProperty(OUT_OF_BAND_HEADER));
    }

    private boolean requireOutOfBandHeader() {
        String value = (String)context.get(ToolConstants.CFG_EXTRA_SOAPHEADER);
        if (StringUtils.isEmpty(value)) {
            return false;
        }
        return Boolean.valueOf(value).booleanValue();
    }

    private int countOutOfBandHeader(MessageInfo message) {
        int count = 0;
        for (MessagePartInfo part : message.getMessageParts()) {
            if (isOutOfBandHeader(part)) {
                count++;
            }
        }
        return count;
    }

    private boolean messagePartsNotUnique(final MessageInfo message) {
        int count = countOutOfBandHeader(message);
        return message.getMessageParts().size() - count > 1;
    }

    private void processInput(JavaMethod method, MessageInfo inputMessage) throws ToolException {
        if (requireOutOfBandHeader()) {
            try {
                Class.forName("org.apache.cxf.binding.soap.SoapBindingFactory");
            } catch (Exception e) {
                LOG.log(Level.WARNING, new Message("SOAP_MISSING", LOG).toString());
            }
        }

        JAXWSBinding mBinding = inputMessage.getOperation().getExtensor(JAXWSBinding.class);
        for (MessagePartInfo part : inputMessage.getMessageParts()) {
            if (isOutOfBandHeader(part) && !requireOutOfBandHeader()) {
                continue;
            }
            JavaParameter param = getParameterFromPart(method, part, JavaType.Style.IN);
            if (mBinding != null && mBinding.getJaxwsParas() != null) {
                for (JAXWSParameter jwp : mBinding.getJaxwsParas()) {
                    if (part.getName().getLocalPart().equals(jwp.getPart())) {
                        param.setName(jwp.getName());
                    }
                }
            }
            addParameter(method, param);
        }
    }

    private void processWrappedInput(JavaMethod method, MessageInfo inputMessage) throws ToolException {
        List<MessagePartInfo> inputParts = inputMessage.getMessageParts();

        if (messagePartsNotUnique(inputMessage)) {
            processInput(method, inputMessage);
            return;
        } else if (inputParts.isEmpty()) {
            return;
        }
        MessagePartInfo part = inputParts.iterator().next();

        List<QName> wrappedElements = ProcessorUtil.getWrappedElementQNames(context, part.getElementQName());
        if ((wrappedElements == null || wrappedElements.size() == 0) 
            && countOutOfBandHeader(inputMessage) == 0) {
            return;
        }
        JAXWSBinding mBinding = inputMessage.getOperation().getExtensor(JAXWSBinding.class);
        for (QName item : wrappedElements) {
            JavaParameter jp = getParameterFromQName(part.getElementQName(),
                                  item, JavaType.Style.IN, part);
            if (mBinding != null && mBinding.getJaxwsParas() != null) {
                for (JAXWSParameter jwsp : mBinding.getJaxwsParas()) {
                    if (item.equals(jwsp.getElementName())) {
                        jp.setName(jwsp.getName());
                    }
                }
            }
            
            if (StringUtils.isEmpty(part.getConcreteName().getNamespaceURI())) { 
                jp.setTargetNamespace("");
            }

            addParameter(method, jp);
        }

        // Adding out of band headers
        if (requireOutOfBandHeader() && countOutOfBandHeader(inputMessage) > 0) {
            for (MessagePartInfo hpart : inputMessage.getMessageParts()) {
                if (!isOutOfBandHeader(hpart)) {
                    continue;
                }
                addParameter(method, getParameterFromPart(method, hpart, JavaType.Style.IN));
            }
        }
    }

    private void processOutput(JavaMethod method, MessageInfo inputMessage, MessageInfo outputMessage)
        throws ToolException {
                        
        Map<QName, MessagePartInfo> inputPartsMap = inputMessage.getMessagePartsMap();
        List<MessagePartInfo> outputParts =
            outputMessage == null ? new ArrayList<MessagePartInfo>() : outputMessage.getMessageParts();
        // figure out output parts that are not present in input parts
        List<MessagePartInfo> outParts = new ArrayList<MessagePartInfo>();
        int numHeader = 0;
        if (isRequestResponse(method)) {
            for (MessagePartInfo outpart : outputParts) {
                boolean oob = false;
                if (isOutOfBandHeader(outpart)) {
                    if (!requireOutOfBandHeader()) {
                        continue;
                    }
                    oob = true;
                }
                
                MessagePartInfo inpart = inputPartsMap.get(outpart.getName());
                if (inpart == null) {
                    outParts.add(outpart);
                    if (oob) {
                        numHeader++;
                    }
                    continue;
                } else if (isSamePart(inpart, outpart)) {
                    addParameter(method, getParameterFromPart(method, outpart, JavaType.Style.INOUT));
                    continue;
                } else if (!isSamePart(inpart, outpart)) {
                    if (oob) {
                        numHeader++;
                    }
                    outParts.add(outpart);
                    continue;
                }
            }
        }

        if (isRequestResponse(method)) {
            if (outParts.size() - numHeader == 1
                && !isHeader(outParts.get(0))) {
                processReturn(method, outParts.get(0));
                outParts.remove(0);
            } else {
                processReturn(method, null);
            }
            JAXWSBinding mBinding = outputMessage.getOperation().getExtensor(JAXWSBinding.class);
            for (MessagePartInfo part : outParts) {

                JavaParameter param = getParameterFromPart(method, part, JavaType.Style.OUT);
                if (mBinding != null && mBinding.getJaxwsParas() != null) {
                    for (JAXWSParameter jwp : mBinding.getJaxwsParas()) {
                        if (part.getName().getLocalPart().equals(jwp.getPart())) {
                            param.setName(jwp.getName());
                        }
                    }
                }

                addParameter(method, param);
            }
        } else {
            processReturn(method, null);
        }
    }

    private void processWrappedOutput(JavaMethod method,
                                      MessageInfo inputMessage,
                                      MessageInfo outputMessage) throws ToolException {

        processWrappedAbstractOutput(method, inputMessage, outputMessage);

        // process out of band headers
        if (countOutOfBandHeader(outputMessage) > 0) {
            for (MessagePartInfo hpart : outputMessage.getMessageParts()) {
                if (!isOutOfBandHeader(hpart) || !requireOutOfBandHeader()) {
                    continue;
                }
                addParameter(method, getParameterFromPart(method, hpart, JavaType.Style.OUT));
            }
        }
    }

    private void processWrappedAbstractOutput(JavaMethod method,
                                              MessageInfo inputMessage,
                                              MessageInfo outputMessage) throws ToolException {

        List<MessagePartInfo> outputParts = outputMessage.getMessageParts();
        List<MessagePartInfo> inputParts = inputMessage.getMessageParts();

        if (messagePartsNotUnique(inputMessage) || messagePartsNotUnique(outputMessage)) {
            processOutput(method, inputMessage, outputMessage);
            return;
        }
        if (outputParts.size() == 0) {
            addVoidReturn(method);
            return;
        }

        MessagePartInfo inputPart = inputParts.size() > 0 ? inputParts.iterator().next() : null;
        MessagePartInfo outputPart = outputParts.size() > 0 ? outputParts.iterator().next() : null;

        List<QName> inputWrapElement = null;
        List<QName> outputWrapElement = null;

        if (inputPart != null) {
            inputWrapElement = ProcessorUtil.getWrappedElementQNames(context, inputPart.getElementQName());
        }

        if (outputPart != null) {
            outputWrapElement = ProcessorUtil.getWrappedElementQNames(context, outputPart.getElementQName());
        }

        if (inputWrapElement == null || outputWrapElement.size() == 0) {
            addVoidReturn(method);
            return;
        }
        method.setReturn(null);
        boolean qualified = ProcessorUtil.isSchemaFormQualified(context, outputPart.getElementQName());

        if (outputWrapElement.size() == 1 && inputWrapElement != null) {
            QName outElement = outputWrapElement.iterator().next();
            boolean sameWrapperChild = false;
            if (outputWrapElement.size() > 1) {
                for (QName inElement : inputWrapElement) {
                    if (isSameWrapperChild(inElement, outElement)) {
                        JavaParameter jpIn = null;
                        for (JavaParameter j : method.getParameters()) {
                            if (inElement.equals(j.getQName())) {
                                jpIn = j;
                            }
                        }
                        JavaParameter jp = getParameterFromQName(outputPart.getElementQName(), outElement,
                                                                 JavaType.Style.INOUT, outputPart);
                        if (!qualified) {
                            jp.setTargetNamespace("");
                        }
                        if (!jpIn.getClassName().equals(jp.getClassName())) {
                            jp.setStyle(JavaType.Style.OUT);
                        } 
                        addParameter(method, jp);
                        sameWrapperChild = true;

                        if (method.getReturn() == null) {
                            addVoidReturn(method);
                        }
                        break;
                    }
                }
            }
            if (!sameWrapperChild) {
                JavaReturn jreturn = getReturnFromQName(outElement, outputPart);
                if (!qualified) {
                    jreturn.setTargetNamespace("");
                }
                method.setReturn(jreturn);
                return;
            }

        }

        for (QName outElement : outputWrapElement) {
            if ("return".equals(outElement.getLocalPart())) {
                if (method.getReturn() != null) {
                    org.apache.cxf.common.i18n.Message msg =
                        new org.apache.cxf.common.i18n.Message("WRAPPER_STYLE_TWO_RETURN_TYPES", LOG);
                    throw new ToolException(msg);
                }
                JavaReturn jreturn = getReturnFromQName(outElement, outputPart);
                if (!qualified) {
                    jreturn.setTargetNamespace("");
                }
                method.setReturn(jreturn);
                continue;
            }
            boolean sameWrapperChild = false;
            if (inputWrapElement != null) {
                for (QName inElement : inputWrapElement) {
                    if (isSameWrapperChild(inElement, outElement)) {
                        
                        JavaParameter jpIn = null;
                        for (JavaParameter j : method.getParameters()) {
                            if (inElement.equals(j.getQName())) {
                                jpIn = j;
                            }
                        }
                        JavaParameter jp = getParameterFromQName(outputPart.getElementQName(), outElement,
                                                                 JavaType.Style.INOUT, outputPart);
                        if (!qualified) {
                            jp.setTargetNamespace("");
                        }
                        if (!jpIn.getClassName().equals(jp.getClassName())) {
                            jp.setStyle(JavaType.Style.OUT);
                        }
                        addParameter(method, jp);
                        sameWrapperChild = true;
                        break;
                    }
                }
            }
            if (!sameWrapperChild) {
                JavaParameter  jp = getParameterFromQName(outputPart.getElementQName(), outElement,
                                                          JavaType.Style.OUT, outputPart);
                if (!qualified) {
                    jp.setTargetNamespace("");
                }
                
                JAXWSBinding mBinding = outputMessage.getOperation().getExtensor(JAXWSBinding.class);
                if (mBinding != null && mBinding.getJaxwsParas() != null) {
                    for (JAXWSParameter jwsp : mBinding.getJaxwsParas()) {
                        if (outElement.equals(jwsp.getElementName())) {
                            jp.setName(jwsp.getName());
                        }
                    }
                }

                addParameter(method, jp);
            }
        }
        if (method.getReturn() == null) {
            addVoidReturn(method);
        }
    }

    private void addVoidReturn(JavaMethod method) {
        JavaReturn returnType = new JavaReturn("return", "void", null);
        method.setReturn(returnType);
    }

    private boolean isSameWrapperChild(QName in, QName out) {
        if (!in.getLocalPart().equals(out.getLocalPart())) {
            return false;
        }

        if (!in.getNamespaceURI().equals(out.getNamespaceURI())) {
            return false;
        }
        return true;
    }

    private JavaParameter getParameterFromQName(QName wrapperElement, QName item, JavaType.Style style,
                                                MessagePartInfo part) {

        String fullJavaName = "";

        fullJavaName = this.dataBinding.getWrappedElementType(wrapperElement, item);

        String targetNamespace = item.getNamespaceURI();

        String jpname = ProcessorUtil.mangleNameToVariableName(item.getLocalPart());
        JavaParameter parameter = new JavaParameter(jpname, fullJavaName, targetNamespace);
        parameter.setStyle(style);
        parameter.setQName(item);

        parameter.setDefaultValueWriter(ProcessorUtil.getDefaultValueWriterForWrappedElement(part,
                                                                                             context,
                                                                                             item));

        if (style == JavaType.Style.OUT || style == JavaType.Style.INOUT) {
            parameter.setHolder(true);
            parameter.setHolderName(javax.xml.ws.Holder.class.getName());
            String holderClass = fullJavaName;
            if (JAXBUtils.holderClass(fullJavaName) != null) {
                holderClass = JAXBUtils.holderClass(fullJavaName).getName();
            }
            parameter.setClassName(holderClass);
        }
        return parameter;

    }

    private JavaReturn getReturnFromQName(QName element, MessagePartInfo part) {

        String fullJavaName = "";
        String simpleJavaName = "";
        fullJavaName = this.dataBinding.getWrappedElementType(part.getElementQName(), element);
        simpleJavaName = fullJavaName;

        int index = fullJavaName.lastIndexOf(".");

        if (index > -1) {
            simpleJavaName = fullJavaName.substring(index);
        }

        String targetNamespace = "";
        if (isHeader(part)) {
            targetNamespace = part.getMessageInfo().getOperation().getInterface().
            getService().getTargetNamespace();
        }  else {
            targetNamespace = element.getNamespaceURI();
        }

        String jpname = ProcessorUtil.mangleNameToVariableName(simpleJavaName);
        JavaReturn returnType = new JavaReturn(jpname, fullJavaName , targetNamespace);

        returnType.setDefaultValueWriter(
            ProcessorUtil.getDefaultValueWriterForWrappedElement(part, context, element));

        returnType.setQName(element);
        returnType.setStyle(JavaType.Style.OUT);
        return returnType;
    }

    private boolean isHeader(final MessagePartInfo part) {
        return Boolean.TRUE.equals(part.getProperty(HEADER));
    }

    private void buildParamModelsWithoutOrdering(JavaMethod method,
                                                 MessageInfo inputMessage,
                                                 MessageInfo outputMessage) throws ToolException {
        boolean wrapped = method.isWrapperStyle();
        if (wrapped) {
            //check if really can be wrapper style....

            if (inputMessage != null) {
                List<MessagePartInfo> inputParts = inputMessage.getMessageParts();
                MessagePartInfo inputPart = inputParts.size() > 0 ? inputParts.iterator().next() : null;
                List<QName> inputWrapElement = null;
                if (inputPart != null) {
                    inputWrapElement = ProcessorUtil.getWrappedElementQNames(context, 
                                                                             inputPart.getElementQName());
                }
                if (inputWrapElement != null) {
                    for (QName item : inputWrapElement) {
                        String fullJavaName = dataBinding
                            .getWrappedElementType(inputPart.getElementQName(), item);
                        if (StringUtils.isEmpty(fullJavaName)) {
                            wrapped = false;
                            break;
                        }
                    }                
                }
            }
            if (outputMessage != null) {
                List<MessagePartInfo> outputParts = outputMessage.getMessageParts();
                MessagePartInfo outputPart = outputParts.size() > 0 ? outputParts.iterator().next() : null;
                List<QName> outputWrapElement = null;
                if (outputPart != null) {
                    outputWrapElement = ProcessorUtil.getWrappedElementQNames(context, 
                                                                              outputPart.getElementQName());
                }
                if (outputWrapElement != null) {
                    for (QName item : outputWrapElement) {
                        String fullJavaName = dataBinding
                            .getWrappedElementType(outputPart.getElementQName(), item);
                        if (StringUtils.isEmpty(fullJavaName)) {
                            wrapped = false;
                            break;
                        }
                    }
                }
            }
            if (!wrapped) {
                //could not map one of the parameters to a java type, need to drop down to bare style
                method.setWrapperStyle(false);
            }
        }
        
        if (inputMessage != null) {
            if (method.isWrapperStyle()) {
                processWrappedInput(method, inputMessage);
            } else {
                processInput(method, inputMessage);
            }
        }
        if (outputMessage == null) {
            processReturn(method, null);
        } else {
            if (method.isWrapperStyle()) {
                processWrappedOutput(method, inputMessage, outputMessage);
            } else {
                processOutput(method, inputMessage, outputMessage);
            }
        }
    }

    private void buildParamModelsWithOrdering(JavaMethod method,
                                              MessageInfo inputMessage,
                                              MessageInfo outputMessage,
                                              List<String> parameterList) throws ToolException {
        Map<QName, MessagePartInfo> inputPartsMap = inputMessage.getMessagePartsMap();

        Map<QName, MessagePartInfo> outputPartsMap = new LinkedHashMap<QName, MessagePartInfo>();
        
        if (outputMessage != null) {
            outputPartsMap = outputMessage.getMessagePartsMap();
        }

        List<MessagePartInfo> inputParts = inputMessage.getMessageParts();
        List<MessagePartInfo> outputParts = new ArrayList<MessagePartInfo>();

        if (outputMessage != null) {
            outputParts = outputMessage.getMessageParts();
        }

        List<MessagePartInfo> inputUnlistedParts = new ArrayList<MessagePartInfo>();
        List<MessagePartInfo> outputUnlistedParts = new ArrayList<MessagePartInfo>();

        for (MessagePartInfo part : inputParts) {
            if (!parameterList.contains(part.getName().getLocalPart())) {
                inputUnlistedParts.add(part);
            }
        }

        if (isRequestResponse(method)) {
            for (MessagePartInfo part : outputParts) {
                if (!parameterList.contains(part.getName().getLocalPart())) {
                    MessagePartInfo inpart = inputMessage.getMessagePart(part.getName());
                    if (inpart == null || (inpart != null && !isSamePart(inpart, part))) {
                        outputUnlistedParts.add(part);
                    }
                }
            }

            if (outputUnlistedParts.size() == 1) {
                processReturn(method, outputUnlistedParts.get(0));
                outputPartsMap.remove(outputUnlistedParts.get(0));
                outputUnlistedParts.clear();
            } else {
                processReturn(method, null);
            }
        } else {
            processReturn(method, null);
        }

        // now create list of paramModel with parts
        // first for the ordered list
        int index = 0;
        int size = parameterList.size();

        while (index < size) {
            String partName = parameterList.get(index);
            MessagePartInfo part = inputPartsMap.get(inputMessage.getMessagePartQName(partName));
            JavaType.Style style = JavaType.Style.IN;
            if (part == null) {
                part = outputPartsMap.get(inputMessage.getMessagePartQName(partName));
                style = JavaType.Style.OUT;
            } else if (outputPartsMap.get(inputMessage.getMessagePartQName(partName)) != null
                && isSamePart(part, outputPartsMap.get(inputMessage.getMessagePartQName(partName)))) {
                
                style = JavaType.Style.INOUT;
            }
            if (part != null) {
                addParameter(method, getParameterFromPart(method, part, style));
            }
            index++;
        }
        // now from unlisted input parts
        for (MessagePartInfo part : inputUnlistedParts) {
            addParameter(method, getParameterFromPart(method, part, JavaType.Style.IN));
        }
        // now from unlisted output parts
        for (MessagePartInfo part : outputUnlistedParts) {
            addParameter(method, getParameterFromPart(method, part, JavaType.Style.INOUT));
        }
    }

    private boolean isSamePart(MessagePartInfo part1, MessagePartInfo part2) {
        QName qname1 = part1.getElementQName();
        QName qname2 = part2.getElementQName();
        QName tname1 = part1.getTypeQName();
        QName tname2 = part2.getTypeQName();
        if (qname1 != null && qname2 != null) {
            return qname1.equals(qname2) && (tname1 == null || tname1.equals(tname2));
        }
        if (tname1 != null && tname2 != null) {
            return tname1.equals(tname2);
        }
        return false;
    }

    private boolean isValidOrdering(List<String> parameterOrder,
                                    MessageInfo inputMessage, MessageInfo outputMessage) {
        Iterator<String> params = parameterOrder.iterator();

        List<MessagePartInfo> inputParts = inputMessage.getMessageParts();
        List<MessagePartInfo> outputParts = new ArrayList<MessagePartInfo>();

        if (outputMessage != null) {
            outputParts = outputMessage.getMessageParts();
        }

        while (params.hasNext()) {
            String param = params.next();
            MessagePartInfo inPart = null;
            MessagePartInfo outPart = null;
            for (MessagePartInfo part : inputParts) {
                if (param.equals(part.getName().getLocalPart())) {
                    inPart = part;
                    break;
                }
            }
            //check output parts
            for (MessagePartInfo part : outputParts) {
                if (param.equals(part.getName().getLocalPart())) {
                    outPart = part;
                    break;
                }
            }
            if (inPart == null && outPart == null) {
                return false;
            } else if (inPart != null 
                && outPart != null) {
                if (inPart.isElement() != outPart.isElement()) {
                    return false;
                }
                if (inPart.isElement()
                    && !inPart.getElementQName().equals(outPart.getElementQName())) {
                    return false;
                } else if (!inPart.isElement()
                    && !inPart.getTypeQName().equals(outPart.getTypeQName())) {
                    return false;                    
                }
            }
            
        }
        return true;
    }
}
