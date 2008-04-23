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

package org.apache.cxf.binding.corba.interceptors;

import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.binding.corba.CorbaMessage;
import org.apache.cxf.binding.corba.CorbaStreamable;
import org.apache.cxf.binding.corba.CorbaTypeMap;
import org.apache.cxf.binding.corba.runtime.CorbaStreamWriter;
import org.apache.cxf.binding.corba.types.CorbaHandlerUtils;
import org.apache.cxf.binding.corba.types.CorbaObjectHandler;
import org.apache.cxf.binding.corba.utils.ContextUtils;
import org.apache.cxf.binding.corba.utils.CorbaUtils;
import org.apache.cxf.binding.corba.wsdl.ArgType;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.ModeType;
import org.apache.cxf.binding.corba.wsdl.OperationType;
import org.apache.cxf.binding.corba.wsdl.ParamType;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;

public class CorbaStreamOutEndingInterceptor extends AbstractPhaseInterceptor<Message> {

    private org.omg.CORBA.ORB orb;
    private CorbaTypeMap typeMap;
    private ServiceInfo service;

    public CorbaStreamOutEndingInterceptor() {
        super(Phase.USER_STREAM);
    }

    public void handleMessage(Message msg) {
        CorbaMessage message = (CorbaMessage) msg;
        orb = (org.omg.CORBA.ORB) message.get(CorbaConstants.ORB);
        Exchange exchange = message.getExchange();
        BindingOperationInfo boi = exchange.get(BindingOperationInfo.class);
        service = exchange.get(ServiceInfo.class);
        typeMap = message.getCorbaTypeMap();

        if (ContextUtils.isRequestor(message)) {
            handleOutBoundMessage(message, boi);
        } else {
            handleInBoundMessage(message, boi);
        }

    }

    private void handleOutBoundMessage(CorbaMessage message, BindingOperationInfo boi) {
        OperationInfo opInfo = boi.getOperationInfo();
        OperationType opType = boi.getExtensor(OperationType.class);
        List<ParamType> paramTypes = opType.getParam();

        MessageInfo outMsgInfo = opInfo.getOutput();
        String wrapNSUri = null;
        boolean wrap = false;
        if (boi.isUnwrappedCapable()) {
            wrap = true;
            if (outMsgInfo != null) {
                wrapNSUri = getWrappedParamNamespace(outMsgInfo);
                if (!CorbaUtils.isElementFormQualified(service, wrapNSUri)) {
                    wrapNSUri = "";
                }
            }
        }
        CorbaStreamWriter writer = (CorbaStreamWriter) message.getContent(XMLStreamWriter.class);
        CorbaObjectHandler[] objs = writer.getCorbaObjects();

        int count = 0;
        int msgIndex = 0;

        ArgType returnParam = opType.getReturn();

        if (returnParam != null) {
            QName retName;
            if (wrap) {
                retName = new QName(wrapNSUri, returnParam.getName());
            } else {
                retName = getMessageParamQName(outMsgInfo, returnParam.getName(), msgIndex);
            }
            QName retIdlType = returnParam.getIdltype();
            CorbaObjectHandler obj = CorbaHandlerUtils
                .initializeObjectHandler(orb, retName, retIdlType, typeMap, service);
            CorbaStreamable streamable = message.createStreamableObject(obj, retName);
            message.setStreamableReturn(streamable);
            msgIndex++;
        }

        for (Iterator<ParamType> iter = paramTypes.iterator(); iter.hasNext();) {
            ParamType param = iter.next();
            QName idlType = param.getIdltype();
            
            QName paramName;
            CorbaObjectHandler obj = null;
            if (param.getMode().equals(ModeType.OUT)) {
                if (wrap) {
                    paramName = new QName(wrapNSUri, param.getName());
                } else {
                    paramName = getMessageParamQName(outMsgInfo, param.getName(), msgIndex);
                }
                obj = CorbaHandlerUtils.initializeObjectHandler(orb,
                                                                paramName,
                                                                idlType,
                                                                typeMap,
                                                                service);
                msgIndex++;
            } else {
                obj = objs[count++];
                paramName = obj.getName();
            }
            CorbaStreamable streamable = message.createStreamableObject(obj, paramName);
            ModeType paramMode = param.getMode();
            if (paramMode.value().equals("in")) {
                streamable.setMode(org.omg.CORBA.ARG_IN.value);
            } else if (paramMode.value().equals("inout")) {
                streamable.setMode(org.omg.CORBA.ARG_INOUT.value);
            } // default mode is out
            message.addStreamableArgument(streamable);
        }
    }

    private void handleInBoundMessage(CorbaMessage message, BindingOperationInfo boi) {
        OperationInfo opInfo = boi.getOperationInfo();
        OperationType opType = boi.getExtensor(OperationType.class);
        List<ParamType> paramTypes = opType.getParam();

        MessageInfo msgInInfo = opInfo.getInput();
        String wrapNSUri = null;
        boolean wrap = false;
        if (boi.isUnwrappedCapable()) {
            wrap = true;
            if (msgInInfo != null) {
                wrapNSUri = getWrappedParamNamespace(msgInInfo);
                if (!CorbaUtils.isElementFormQualified(service, wrapNSUri)) {
                    wrapNSUri = "";
                }
            }
        }
        CorbaStreamWriter writer = (CorbaStreamWriter) message.getContent(XMLStreamWriter.class);
        CorbaObjectHandler[] objs = writer.getCorbaObjects();
        int count = 0;
        int msgIndex = 0;
        ArgType returnParam = opType.getReturn();
        if (returnParam != null) {
            CorbaObjectHandler obj = objs[count++];
            QName retName = obj.getName();
            CorbaStreamable streamable = message.createStreamableObject(obj, retName);
            message.setStreamableReturn(streamable);
        }
        for (Iterator<ParamType> iter = paramTypes.iterator(); iter.hasNext();) {
            ParamType param = iter.next();
            QName idlType = param.getIdltype();
            QName paramName;
            CorbaObjectHandler obj = null;
            if (param.getMode().equals(ModeType.IN)) {
                if (wrap) {
                    paramName = new QName(wrapNSUri, param.getName());
                } else {
                    paramName = getMessageParamQName(msgInInfo, param.getName(), msgIndex);
                }
                obj = CorbaHandlerUtils.initializeObjectHandler(orb,
                                                                paramName,
                                                                idlType,
                                                                typeMap,
                                                                service);
                msgIndex++;
            } else {
                obj = objs[count++];
                paramName = obj.getName();
            }
            CorbaStreamable streamable = message.createStreamableObject(obj, paramName);
            ModeType paramMode = param.getMode();
            if (paramMode.value().equals("in")) {
                streamable.setMode(org.omg.CORBA.ARG_IN.value);
            } else if (paramMode.value().equals("inout")) {
                streamable.setMode(org.omg.CORBA.ARG_INOUT.value);
            } else if (paramMode.value().equals("out")) {
                streamable.setMode(org.omg.CORBA.ARG_OUT.value);
            }
            message.addStreamableArgument(streamable);
        }
        
    }

    protected QName getMessageParamQName(MessageInfo msgInfo,
                                         String paramName,
                                         int index) {
        QName paramQName;
        MessagePartInfo part = msgInfo.getMessageParts().get(index);
        if (part != null && part.isElement()) {
            paramQName = part.getElementQName();
        } else {
            paramQName = part.getName();
        }
        return paramQName;
    }

    protected String getWrappedParamNamespace(MessageInfo msgInfo) {    
        MessagePartInfo part = msgInfo.getMessageParts().get(0);
        if (part.isElement()) {
            return part.getElementQName().getNamespaceURI();
        } else {
            return part.getName().getNamespaceURI();
        }
    }

}
