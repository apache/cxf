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

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import javax.jws.WebParam;

import org.apache.cxf.jaxb.JAXBUtils;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JAnnotation;
import org.apache.cxf.tools.common.model.JAnnotationElement;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.apache.cxf.tools.common.model.JavaParameter;
import org.apache.cxf.tools.common.model.JavaReturn;
import org.apache.cxf.tools.common.model.JavaType;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.customization.JAXWSBinding;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator.WSActionAnnotator;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator.WebMethodAnnotator;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator.WebResultAnnotator;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator.WrapperAnnotator;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.mapper.MethodMapper;

public class OperationProcessor  extends AbstractProcessor {

    private JavaParameter wrapperRequest;
    private JavaParameter wrapperResponse;
    public OperationProcessor(ToolContext c) {
        super(c);
    }

    public void process(JavaInterface intf, OperationInfo operation) throws ToolException {
        JavaMethod method = new MethodMapper().map(operation);
        method.setInterface(intf);
        
        processMethod(method, operation);
        
        Collection<FaultInfo> faults = operation.getFaults();
        FaultProcessor faultProcessor = new FaultProcessor(context);
        faultProcessor.process(method, faults);

        method.annotate(new WSActionAnnotator(operation));

        intf.addMethod(method);
    }

    void processMethod(JavaMethod method, 
                       OperationInfo operation) throws ToolException {
        if (isAsyncMethod(method)) {
            return;
        }
        MessageInfo inputMessage = operation.getInput();
        MessageInfo outputMessage = operation.getOutput();

        if (inputMessage == null) {
            LOG.log(Level.WARNING, "NO_INPUT_MESSAGE", new Object[] {operation.getName()});
            org.apache.cxf.common.i18n.Message msg
                = new org.apache.cxf.common.i18n.Message("INVALID_MEP", LOG,
                                                               new Object[] {operation.getName()});
            throw new ToolException(msg);
        }

        ParameterProcessor paramProcessor = new ParameterProcessor(context);
        method.clear();
        
        JAXWSBinding opBinding = (JAXWSBinding)operation.getExtensor(JAXWSBinding.class);
        JAXWSBinding ptBinding = operation.getInterface().getExtensor(JAXWSBinding.class);
        JAXWSBinding defBinding = operation.getInterface().getService()
            .getDescription().getExtensor(JAXWSBinding.class);
        
        boolean enableAsync = false;
        boolean enableMime = false;
        boolean enableWrapper = method.isWrapperStyle();
        if (defBinding != null) {
            if (defBinding.isSetEnableMime()) {
                enableMime = defBinding.isEnableMime();
            }
            if (defBinding.isSetEnableAsyncMapping()) {
                enableAsync = defBinding.isEnableAsyncMapping();
            }
            if (defBinding.isSetEnableWrapperStyle()) {
                enableWrapper = defBinding.isEnableWrapperStyle();
            }
        }
        if (ptBinding != null) {
            if (ptBinding.isSetEnableMime()) {
                enableMime = ptBinding.isEnableMime();
            }
            if (ptBinding.isSetEnableAsyncMapping()) {
                enableAsync = ptBinding.isEnableAsyncMapping();
            }
            if (ptBinding.isSetEnableWrapperStyle()) {
                enableWrapper = ptBinding.isEnableWrapperStyle();
            }
        }
        if (opBinding != null) {
            if (opBinding.isSetEnableMime()) {
                enableMime = opBinding.isEnableMime();
            }
            if (opBinding.isSetEnableAsyncMapping()) {
                enableAsync = opBinding.isEnableAsyncMapping();
            }
            if (opBinding.isSetEnableWrapperStyle()) {
                enableWrapper = opBinding.isEnableWrapperStyle();
            }
        }
        method.setWrapperStyle(enableWrapper && method.isWrapperStyle());
        
        
        paramProcessor.process(method,
                               inputMessage,
                               outputMessage,
                               operation.getParameterOrdering());

        if (method.isWrapperStyle()) {
            setWrapper(operation);
            method.annotate(new WrapperAnnotator(wrapperRequest, wrapperResponse));
        }

        method.annotate(new WebMethodAnnotator());


        method.annotate(new WebResultAnnotator());


        if (!method.isOneWay()
            && enableAsync && !isAddedAsycMethod(method)) {
            addAsyncMethod(method);
        }
        if (enableMime) {
            method.setMimeEnable(true);
        }
    }


    private void setWrapper(OperationInfo operation) {
        MessagePartInfo inputPart = null;
        if (operation.getInput() != null && operation.getInput().getMessageParts() != null) {
            inputPart = operation.getInput().getMessageParts().iterator().next();
        }
        MessagePartInfo outputPart = null;
        if (operation.getOutput() != null && operation.getOutput().getMessageParts() != null) {
            outputPart = operation.getOutput().getMessageParts().iterator().next();
        }

        if (inputPart != null) {
            wrapperRequest = new JavaParameter();
            wrapperRequest.setName(ProcessorUtil.resolvePartName(inputPart));
            wrapperRequest.setType(ProcessorUtil.getPartType(inputPart));
            wrapperRequest.setTargetNamespace(ProcessorUtil.resolvePartNamespace(inputPart));

            wrapperRequest.setClassName(ProcessorUtil.getFullClzName(inputPart,
                                                                     context, false));

        }
        if (outputPart != null) {
            wrapperResponse = new JavaParameter();
            wrapperResponse.setName(ProcessorUtil.resolvePartName(outputPart));
            wrapperResponse.setType(ProcessorUtil.getPartType(outputPart));
            wrapperResponse.setTargetNamespace(ProcessorUtil.resolvePartNamespace(outputPart));

            wrapperResponse.setClassName(ProcessorUtil.getFullClzName(outputPart,
                                                                      context, false));
        }
    }

    private boolean isAsyncMethod(JavaMethod method) {
        if (method.getName().endsWith(ToolConstants.ASYNC_METHOD_SUFFIX)
            && method.getReturn() != null
            && method.getReturn().getClassName() != null) {
            if (method.getReturn().getClassName().startsWith("Response<")) {
                return true;
            } else if (method.getParameterCount() > 0
                && method.getParameters().get(method.getParameterCount() - 1)
                    .getClassName().startsWith("AsyncHandler<")) {
                return true;
            }
        }
        return false;
    }

    private void addAsyncMethod(JavaMethod method) throws ToolException {
        addPollingMethod(method);
        addCallbackMethod(method);
        method.getInterface().addImport("javax.xml.ws.AsyncHandler");
        method.getInterface().addImport("java.util.concurrent.Future");
        method.getInterface().addImport("javax.xml.ws.Response");
    }

    private void addCallbackMethod(JavaMethod method) throws ToolException {
        JavaMethod callbackMethod = new JavaMethod(method.getInterface());
        callbackMethod.setAsync(true);
        callbackMethod.setName(method.getName() + ToolConstants.ASYNC_METHOD_SUFFIX);
        callbackMethod.setStyle(method.getStyle());
        callbackMethod.setWrapperStyle(method.isWrapperStyle());
        callbackMethod.setSoapAction(method.getSoapAction());
        callbackMethod.setOperationName(method.getOperationName());

        JavaReturn future = new JavaReturn();
        future.setClassName("Future<?>");
        callbackMethod.setReturn(future);

        // REVISIT: test the operation name in the annotation
        callbackMethod.annotate(new WebMethodAnnotator());
        callbackMethod.addAnnotation("ResponseWrapper", method.getAnnotationMap().get("ResponseWrapper"));
        callbackMethod.addAnnotation("RequestWrapper", method.getAnnotationMap().get("RequestWrapper"));
        callbackMethod.addAnnotation("SOAPBinding", method.getAnnotationMap().get("SOAPBinding"));

        boolean convertOutToAsync = !method.isWrapperStyle() 
            && "void".equals(method.getReturn().getClassName());
        String asyncCname = null;
        for (JavaParameter param : method.getParameters()) {
            if (convertOutToAsync) {
                if (param.isHolder()) {
                    if (param.isINOUT()) {
                        JavaParameter p2 = new JavaParameter();
                        
                        p2.setName(param.getName());
                        p2.setClassName(param.getHolderName());
                        p2.setStyle(JavaType.Style.IN);
                        callbackMethod.addParameter(p2);
                        for (String s : param.getAnnotationTags()) {
                            JAnnotation ann = param.getAnnotation(s);
                            p2.addAnnotation(s, ann);
                        }
                    } else if (!param.isHeader() && asyncCname == null) {
                        asyncCname = param.getClassName();
                    }
                } else {
                    callbackMethod.addParameter(param);
                }
            } else {
                callbackMethod.addParameter(param);
            }
        }
        JavaParameter asyncHandler = new JavaParameter();
        
        asyncHandler.setName("asyncHandler");
        asyncHandler.setCallback(true);
        asyncHandler.setClassName(getAsyncClassName(method, 
                                                    "AsyncHandler",
                                                    asyncCname));
        asyncHandler.setStyle(JavaType.Style.IN);
        
        callbackMethod.addParameter(asyncHandler);
        
        JAnnotation asyncHandlerAnnotation = new JAnnotation(WebParam.class);
        asyncHandlerAnnotation.addElement(new JAnnotationElement("name", "asyncHandler"));
        asyncHandlerAnnotation.addElement(new JAnnotationElement("targetNamespace", ""));
        asyncHandler.addAnnotation("WebParam", asyncHandlerAnnotation);                

        method.getInterface().addImport("javax.jws.WebParam");
        method.getInterface().addMethod(callbackMethod);
    }

    private void addPollingMethod(JavaMethod method) throws ToolException {
        JavaMethod pollingMethod = new JavaMethod(method.getInterface());
        pollingMethod.setAsync(true);
        pollingMethod.setName(method.getName() + ToolConstants.ASYNC_METHOD_SUFFIX);
        pollingMethod.setStyle(method.getStyle());
        pollingMethod.setWrapperStyle(method.isWrapperStyle());
        pollingMethod.setSoapAction(method.getSoapAction());
        pollingMethod.setOperationName(method.getOperationName());


        boolean convertOutToAsync = !method.isWrapperStyle() 
            && "void".equals(method.getReturn().getClassName());
        String asyncCname = null;
        for (JavaParameter param : method.getParameters()) {
            if (convertOutToAsync) {
                if (param.isHolder()) {
                    if (param.isINOUT()) {
                        JavaParameter p2 = new JavaParameter();
                        
                        p2.setName(param.getName());
                        p2.setClassName(param.getHolderName());
                        p2.setStyle(JavaType.Style.IN);
                        pollingMethod.addParameter(p2);
                        for (String s : param.getAnnotationTags()) {
                            JAnnotation ann = param.getAnnotation(s);
                            p2.addAnnotation(s, ann);
                        }
                    } else if (!param.isHeader() && asyncCname == null) {
                        asyncCname = param.getClassName();
                    }
                } else {
                    pollingMethod.addParameter(param);
                }
            } else {
                pollingMethod.addParameter(param);
            }
        }

        JavaReturn response = new JavaReturn();
        response.setClassName(getAsyncClassName(method, "Response", asyncCname));
        pollingMethod.setReturn(response);

        // REVISIT: test the operation name in the annotation
        pollingMethod.annotate(new WebMethodAnnotator());
        pollingMethod.addAnnotation("RequestWrapper", method.getAnnotationMap().get("RequestWrapper"));
        pollingMethod.addAnnotation("ResponseWrapper", method.getAnnotationMap().get("ResponseWrapper"));
        pollingMethod.addAnnotation("SOAPBinding", method.getAnnotationMap().get("SOAPBinding"));

        method.getInterface().addMethod(pollingMethod);
    }

    private String getAsyncClassName(JavaMethod method, String clzName, String name) {
        String response = name;
        if (response == null) {
            if (wrapperResponse != null) {
                response = wrapperResponse.getClassName();
            } else {
                response = method.getReturn().getClassName();
            }
            Class<?> mappedClass = JAXBUtils.holderClass(response);
            if (mappedClass != null) {
                response = mappedClass.getName();
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(clzName);
        sb.append("<");
        if ("void".equals(response)) {
            sb.append('?');
        } else {
            sb.append(response);
        }
        sb.append(">");
        return sb.toString();
    }

    private boolean isAddedAsycMethod(JavaMethod method) {
        List<JavaMethod> jmethods = method.getInterface().getMethods();
        int counter = 0;
        for (JavaMethod jm : jmethods) {
            if (jm.getOperationName().equals(method.getOperationName())) {
                counter++;
            }
        }
        return counter > 1 ? true : false;
    }

}
