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

package org.apache.cxf.interceptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.ws.commons.schema.XmlSchemaElement;

public class DocLiteralInInterceptor extends AbstractInDatabindingInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(DocLiteralInInterceptor.class);

    public DocLiteralInInterceptor() {
        super(Phase.UNMARSHAL);
        addAfter(URIMappingInterceptor.class.getName());
        addBefore(WrappedInInterceptor.class.getName());
    }

    public void handleMessage(Message message) {
        if (isGET(message) && message.getContent(List.class) != null) {
            LOG.fine("DocLiteralInInterceptor skipped in HTTP GET method");
            return;
        }

        DepthXMLStreamReader xmlReader = getXMLStreamReader(message);
        DataReader<XMLStreamReader> dr = getDataReader(message);
        MessageContentsList parameters = new MessageContentsList();

        Exchange exchange = message.getExchange();
        BindingOperationInfo bop = exchange.get(BindingOperationInfo.class);

        boolean client = isRequestor(message);

        //if body is empty and we have BindingOperationInfo, we do not need to match 
        //operation anymore, just return
        if (!StaxUtils.toNextElement(xmlReader) && bop != null) {
            // body may be empty for partial response to decoupled request
            return;
        }

        //bop might be a unwrapped, wrap it back so that we can get correct info 
        if (bop != null && bop.isUnwrapped()) {
            bop = bop.getWrappedOperation();
        }

        if (bop == null) {
            QName startQName = xmlReader.getName();
            bop = getBindingOperationInfo(exchange, startQName, client);
        }

        try {
            if (bop != null && bop.isUnwrappedCapable()) {
                ServiceInfo si = bop.getBinding().getService();
                // Wrapped case
                MessageInfo msgInfo = setMessage(message, bop, client, si);
    
                // Determine if there is a wrapper class
                if (msgInfo.getMessageParts().get(0).getTypeClass() != null) {
                    Object wrappedObject = dr.read(msgInfo.getMessageParts().get(0), xmlReader);
                    parameters.put(msgInfo.getMessageParts().get(0), wrappedObject);
                } else {
                    // Unwrap each part individually if we don't have a wrapper
    
                    bop = bop.getUnwrappedOperation();
    
                    msgInfo = setMessage(message, bop, client, si);
                    List<MessagePartInfo> messageParts = msgInfo.getMessageParts();
                    Iterator<MessagePartInfo> itr = messageParts.iterator();
    
                    // advance just past the wrapped element so we don't get
                    // stuck
                    if (xmlReader.getEventType() == XMLStreamConstants.START_ELEMENT) {
                        StaxUtils.nextEvent(xmlReader);
                    }
    
                    // loop through each child element
                    getPara(xmlReader, dr, parameters, itr, message);
                }
    
            } else {
                //Bare style
                BindingMessageInfo msgInfo = null;

    
                Endpoint ep = exchange.get(Endpoint.class);
                ServiceInfo si = ep.getEndpointInfo().getService();
                if (bop != null) { //for xml binding or client side
                    if (client) {
                        msgInfo = bop.getOutput();
                    } else {
                        msgInfo = bop.getInput();
                        if (bop.getOutput() == null) {
                            exchange.setOneWay(true);
                        }
                    }
                    if (msgInfo == null) {
                        return;
                    }
                    setMessage(message, bop, client, si, msgInfo.getMessageInfo());
                }
    
                Collection<OperationInfo> operations = null;
                operations = new ArrayList<OperationInfo>();
                operations.addAll(si.getInterface().getOperations());
    
                if (!StaxUtils.toNextElement(xmlReader)) {
                    // empty input
    
                    // TO DO : check duplicate operation with no input
                    for (OperationInfo op : operations) {
                        MessageInfo bmsg = op.getInput();
                        if (bmsg.getMessageParts().size() == 0) {
                            BindingOperationInfo boi = ep.getEndpointInfo().getBinding().getOperation(op);
                            exchange.put(BindingOperationInfo.class, boi);
                            exchange.put(OperationInfo.class, op);
                            exchange.setOneWay(op.isOneWay());
                        }
                    }
                    return;
                }
    
                int paramNum = 0;
    
                do {
                    QName elName = xmlReader.getName();
                    Object o = null;
    
                    MessagePartInfo p;
                    if (!client && msgInfo != null && msgInfo.getMessageParts() != null 
                        && msgInfo.getMessageParts().size() == 0) {
                        //no input messagePartInfo
                        return;
                    }
                    
                    if (msgInfo != null && msgInfo.getMessageParts() != null 
                        && msgInfo.getMessageParts().size() > 0) {
                        assert msgInfo.getMessageParts().size() > paramNum;
                        p = msgInfo.getMessageParts().get(paramNum);
                    } else {
                        p = findMessagePart(exchange, operations, elName, client, paramNum, message);
                    }
    
                    if (p == null) {
                        throw new Fault(new org.apache.cxf.common.i18n.Message("NO_PART_FOUND", LOG, elName),
                                        Fault.FAULT_CODE_CLIENT);
                    }
    
                    o = dr.read(p, xmlReader);
                    parameters.put(p, o);
                    
                    paramNum++;
                    if (message.getContent(XMLStreamReader.class) == null || o == xmlReader) {
                        xmlReader = null;
                    }
                } while (xmlReader != null && StaxUtils.toNextElement(xmlReader));
    
            }
    
            message.setContent(List.class, parameters);
        } catch (Fault f) {
            if (!isRequestor(message)) {
                f.setFaultCode(Fault.FAULT_CODE_CLIENT);
            }
            throw f;
        }
    }
    
    private void getPara(DepthXMLStreamReader xmlReader,
                         DataReader<XMLStreamReader> dr,
                         MessageContentsList parameters,
                         Iterator<MessagePartInfo> itr,
                         Message message) {
        
        boolean hasNext = true;
        while (itr.hasNext()) {
            MessagePartInfo part = itr.next();
            if (hasNext) {
                hasNext = StaxUtils.toNextElement(xmlReader);
            }
            Object obj = null;
            if (hasNext) {
                QName rname = xmlReader.getName();
                while (part != null 
                    && !rname.equals(part.getConcreteName())) {
                    if (part.getXmlSchema() instanceof XmlSchemaElement) {
                        //TODO - should check minOccurs=0 and throw validation exception
                        //thing if the part needs to be here
                        parameters.put(part, null);
                    } 

                    if (itr.hasNext()) {
                        part = itr.next();
                    } else {
                        part = null;
                    }                
                }
                if (part == null) {
                    return;
                }
                if (rname.equals(part.getConcreteName())) {
                    obj = dr.read(part, xmlReader);
                }
            }
            parameters.put(part, obj);
        }
    }


    private MessageInfo setMessage(Message message, BindingOperationInfo operation,
                                   boolean requestor, ServiceInfo si) {
        MessageInfo msgInfo = getMessageInfo(message, operation, requestor);
        return setMessage(message, operation, requestor, si, msgInfo);
    }

    
    protected BindingOperationInfo getBindingOperationInfo(Exchange exchange, QName name,
                                                           boolean client) {
        BindingOperationInfo bop = ServiceModelUtil.getOperationForWrapperElement(exchange, name, client);
        if (bop == null) {
            bop = super.getBindingOperationInfo(exchange, name, client);
        }
            
        if (bop != null) {
            exchange.put(BindingOperationInfo.class, bop);
            exchange.put(OperationInfo.class, bop.getOperationInfo());
        }
        return bop;
    }
}
