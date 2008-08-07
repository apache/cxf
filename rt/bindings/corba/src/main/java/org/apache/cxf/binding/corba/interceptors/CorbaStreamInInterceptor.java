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
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.binding.corba.CorbaDestination;
import org.apache.cxf.binding.corba.CorbaMessage;
import org.apache.cxf.binding.corba.CorbaStreamable;
import org.apache.cxf.binding.corba.CorbaTypeMap;
import org.apache.cxf.binding.corba.runtime.CorbaStreamReader;
import org.apache.cxf.binding.corba.types.CorbaHandlerUtils;
import org.apache.cxf.binding.corba.types.CorbaObjectHandler;
import org.apache.cxf.binding.corba.types.CorbaTypeEventProducer;
import org.apache.cxf.binding.corba.types.HandlerIterator;
import org.apache.cxf.binding.corba.types.ParameterEventProducer;
import org.apache.cxf.binding.corba.types.WrappedParameterSequenceEventProducer;
import org.apache.cxf.binding.corba.utils.ContextUtils;
import org.apache.cxf.binding.corba.utils.CorbaAnyHelper;
import org.apache.cxf.binding.corba.utils.CorbaUtils;
import org.apache.cxf.binding.corba.wsdl.ModeType;
import org.apache.cxf.binding.corba.wsdl.OperationType;
import org.apache.cxf.binding.corba.wsdl.ParamType;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.omg.CORBA.Any;
import org.omg.CORBA.NVList;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ServerRequest;

public class CorbaStreamInInterceptor extends AbstractPhaseInterceptor<Message> {



    public CorbaStreamInInterceptor() {
        super(Phase.PRE_STREAM);
    }   

    public void handleMessage(Message message) throws Fault {
        if (ContextUtils.isRequestor(message)) {
            handleReply(message);
        } else {
            handleRequest(message);
        }
    }

    private void handleReply(Message msg) {
        ORB orb;
        ServiceInfo service;
        CorbaDestination destination;
        if (msg.getDestination() != null) {
            destination = (CorbaDestination)msg.getDestination();
        } else {
            destination = (CorbaDestination)msg.getExchange().getDestination();
        }
        service = destination.getBindingInfo().getService();

        CorbaMessage message = (CorbaMessage)msg;
        if (message.getStreamableException() != null || message.getSystemException() != null) {
            Endpoint ep = message.getExchange().get(Endpoint.class);
            message.getInterceptorChain().abort();
            if (ep.getInFaultObserver() != null) {
                ep.getInFaultObserver().onMessage(message);
                return;
            }
        }

        CorbaMessage outMessage = (CorbaMessage)message.getExchange().getOutMessage();
        orb = (ORB)message.getExchange().get(ORB.class);
        HandlerIterator paramIterator = new HandlerIterator(outMessage, false);

        CorbaTypeEventProducer eventProducer = null;
        Exchange exchange = message.getExchange();
        BindingOperationInfo bindingOpInfo = exchange.get(BindingOperationInfo.class);  
        BindingMessageInfo msgInfo = bindingOpInfo.getOutput();

        boolean wrap = false;
        if (bindingOpInfo.isUnwrappedCapable()) {
            wrap = true;
        }

        if (wrap) {
            // wrapper element around our args
            // REVISIT, bravi, message name same as the element name
            QName wrapperElementQName = msgInfo.getMessageInfo().getName();
            eventProducer = new WrappedParameterSequenceEventProducer(wrapperElementQName,
                                                                      paramIterator,
                                                                      service,
                                                                      orb);
        } else {
            eventProducer = new ParameterEventProducer(paramIterator,
                                                       service,
                                                       orb);
        }
        CorbaStreamReader reader = new CorbaStreamReader(eventProducer);
        message.setContent(XMLStreamReader.class, reader);
    }

    private void handleRequest(Message msg) {
        ORB orb;
        ServiceInfo service;
        CorbaDestination destination;
        if (msg.getDestination() != null) {
            destination = (CorbaDestination)msg.getDestination();
        } else {
            destination = (CorbaDestination)msg.getExchange().getDestination();
        }
        service = destination.getBindingInfo().getService();

        CorbaMessage message = (CorbaMessage) msg;

        Exchange exchange = message.getExchange();

        CorbaTypeMap typeMap = message.getCorbaTypeMap();

        BindingInfo bInfo = destination.getBindingInfo();              
        InterfaceInfo info = bInfo.getInterface();
        String opName = exchange.get(String.class);
        Iterator i = bInfo.getOperations().iterator();
        OperationType opType = null;
        BindingOperationInfo bopInfo = null;
        QName opQName = null;
        while (i.hasNext()) {
            bopInfo = (BindingOperationInfo)i.next();
            if (bopInfo.getName().getLocalPart().equals(opName)) {
                opType = bopInfo.getExtensor(OperationType.class);
                opQName = bopInfo.getName();
                break;
            }
        }

        if (opType == null) {
            throw new RuntimeException("Couldn't find the binding operation for " + opName);
        }

        orb = (ORB)exchange.get(ORB.class);

        ServerRequest request = exchange.get(ServerRequest.class);
        NVList list = prepareArguments(message, info, opType, 
                                       opQName, typeMap,
                                       destination, service);
        request.arguments(list);
        message.setList(list);

        HandlerIterator paramIterator = new HandlerIterator(message, true);

        CorbaTypeEventProducer eventProducer = null;
        BindingMessageInfo msgInfo = bopInfo.getInput();
        boolean wrap = false;
        if (bopInfo.isUnwrappedCapable()) {
            wrap = true;
        }

        if (wrap) {
            // wrapper element around our args
            QName wrapperElementQName = msgInfo.getMessageInfo().getName();
            eventProducer = new WrappedParameterSequenceEventProducer(wrapperElementQName,
                                                                      paramIterator,
                                                                      service,
                                                                      orb);
        } else {
            eventProducer = new ParameterEventProducer(paramIterator,
                                                       service,
                                                       orb);
        }
        CorbaStreamReader reader = new CorbaStreamReader(eventProducer);
        message.setContent(XMLStreamReader.class, reader);
    }

    protected NVList prepareArguments(CorbaMessage corbaMsg,
                                      InterfaceInfo info,
                                      OperationType opType,
                                      QName opQName,
                                      CorbaTypeMap typeMap,
                                      CorbaDestination destination,
                                      ServiceInfo service) {        
        BindingInfo bInfo = destination.getBindingInfo();                              
        EndpointInfo eptInfo = destination.getEndPointInfo();
        BindingOperationInfo bOpInfo = bInfo.getOperation(opQName);
        OperationInfo opInfo = bOpInfo.getOperationInfo();        
        Exchange exg = corbaMsg.getExchange();
        exg.put(BindingInfo.class, bInfo);
        exg.put(InterfaceInfo.class, info);
        exg.put(EndpointInfo.class, eptInfo);
        exg.put(EndpointReferenceType.class, destination.getAddress());
        exg.put(ServiceInfo.class, service);
        exg.put(BindingOperationInfo.class, bOpInfo);        
        exg.put(OperationInfo.class, opInfo);
        exg.put(MessageInfo.class, opInfo.getInput());
        exg.put(String.class, opQName.getLocalPart());        
        exg.setInMessage(corbaMsg);

        corbaMsg.put(MessageInfo.class, opInfo.getInput());
                       
        List<ParamType> paramTypes = opType.getParam();       
        CorbaStreamable[] arguments = new CorbaStreamable[paramTypes.size()];                               
        NVList list = prepareDIIArgsList(corbaMsg, bOpInfo, 
                                         arguments, paramTypes, 
                                         typeMap,
                                         exg.get(ORB.class), service);         
        
        return list;
        
    }
    
    protected NVList prepareDIIArgsList(CorbaMessage corbaMsg,
                                        BindingOperationInfo boi,
                                        CorbaStreamable[] streamables, 
                                        List<ParamType> paramTypes,
                                        CorbaTypeMap map,
                                        ORB orb,
                                        ServiceInfo service) {
        try {
            // Build the list of DII arguments, returns, and exceptions        
            NVList list = orb.create_list(streamables.length);        

            OperationInfo opInfo = boi.getOperationInfo();
            MessageInfo input = opInfo.getInput();          
            MessageInfo output = opInfo.getOutput();
        
            String inWrapNSUri = null;
            String outWrapNSUri = null;

            boolean wrap = false;
            if (boi.isUnwrappedCapable()) {
                wrap = true;
                if (input != null) {
                    inWrapNSUri = getWrappedParamNamespace(input);
                    if (!CorbaUtils.isElementFormQualified(service, inWrapNSUri)) {
                        inWrapNSUri = "";
                    }
                }
                if (output != null) {
                    outWrapNSUri = getWrappedParamNamespace(output);
                    if (!CorbaUtils.isElementFormQualified(service, outWrapNSUri)) {
                        outWrapNSUri = "";
                    }
                }
            }

            int inMsgIndex = 0;
            int outMsgIndex = 0;
            for (int i = 0; i < paramTypes.size(); i++) {
                ParamType param = paramTypes.get(i);
                QName paramIdlType = param.getIdltype();
                QName paramName;
                ModeType paramMode = param.getMode();
                if (paramMode.value().equals("in")) {
                    if (wrap) {
                        paramName = new QName(inWrapNSUri, param.getName());
                    } else {
                        paramName = getMessageParamQName(input, param.getName(), inMsgIndex);
                        inMsgIndex++;
                    }
                } else {
                    if (wrap) {
                        paramName = new QName(outWrapNSUri, param.getName());
                    } else {
                        paramName = getMessageParamQName(output, param.getName(), outMsgIndex);
                        outMsgIndex++;
                    }
                }
                CorbaObjectHandler obj = 
                    CorbaHandlerUtils.initializeObjectHandler(orb, paramName, paramIdlType, map, service);
                streamables[i] = corbaMsg.createStreamableObject(obj, paramName);

                Any value = CorbaAnyHelper.createAny(orb);
                if (paramMode.value().equals("in")) {
                    streamables[i].setMode(org.omg.CORBA.ARG_IN.value);
                    streamables[i].getObject().setIntoAny(value, streamables[i], false);
                } else if (paramMode.value().equals("out")) {
                    streamables[i].setMode(org.omg.CORBA.ARG_OUT.value);
                    streamables[i].getObject().setIntoAny(value, streamables[i], true);
                } else {
                    streamables[i].setMode(org.omg.CORBA.ARG_INOUT.value);
                    streamables[i].getObject().setIntoAny(value, streamables[i], false);
                }

                list.add_value(streamables[i].getName(), value, streamables[i].getMode());              
                corbaMsg.addStreamableArgument(streamables[i]);
            }
            return list;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
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
