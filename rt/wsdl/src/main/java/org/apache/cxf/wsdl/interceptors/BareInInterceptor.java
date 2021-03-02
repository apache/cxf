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

package org.apache.cxf.wsdl.interceptors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.AbstractInDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
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
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;

public class BareInInterceptor extends AbstractInDatabindingInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(BareInInterceptor.class);

    public BareInInterceptor() {
        super(Phase.UNMARSHAL);
    }

    public void handleMessage(Message message) {
        if (isGET(message) && message.getContent(List.class) != null) {
            LOG.fine("BareInInterceptor skipped in HTTP GET method");
            return;
        }

        DepthXMLStreamReader xmlReader = getXMLStreamReader(message);
        Exchange exchange = message.getExchange();

        DataReader<XMLStreamReader> dr = getDataReader(message);
        MessageContentsList parameters = new MessageContentsList();

        Endpoint ep = exchange.getEndpoint();
        BindingOperationInfo bop = exchange.getBindingOperationInfo();
        ServiceInfo si = ep.getEndpointInfo().getService();
        BindingMessageInfo msgInfo = null;

        boolean client = isRequestor(message);

        Collection<OperationInfo> ops = null;
        if (bop == null) {
            ops = new ArrayList<>();
            ops.addAll(si.getInterface().getOperations());
            if (xmlReader.getEventType() == XMLStreamConstants.END_ELEMENT && !client) {
                //empty input
                //TO DO : check duplicate operation with no input
                for (OperationInfo op : ops) {
                    MessageInfo bmsg = op.getInput();
                    if (bmsg.getMessagePartsNumber() == 0) {
                        BindingOperationInfo boi = ep.getEndpointInfo().getBinding().getOperation(op);
                        exchange.put(BindingOperationInfo.class, boi);
                        exchange.setOneWay(op.isOneWay());
                    }
                }

            }
        } else {
            getMessageInfo(message, bop);
            if (client) {
                msgInfo = bop.getOutput();
            } else {
                msgInfo = bop.getInput();
            }
        }

        int paramNum = 0;


        while (StaxUtils.toNextElement(xmlReader)) {
            QName elName = xmlReader.getName();

            final MessagePartInfo p;
            if (msgInfo != null && msgInfo.getMessageParts() != null) {
                assert msgInfo.getMessageParts().size() > paramNum;
                p = msgInfo.getMessageParts().get(paramNum);
            } else {
                p = findMessagePart(exchange, ops, elName, client, paramNum, message);
            }

            if (p == null) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("NO_PART_FOUND", LOG, elName),
                                Fault.FAULT_CODE_CLIENT);
            }

            final Object o;
            try {
                o = dr.read(p, xmlReader);
            } catch (Fault fault) {
                if (!isRequestor(message)) {
                    fault.setFaultCode(Fault.FAULT_CODE_CLIENT);
                }
                throw fault;
            }

            if (o != null) {
                parameters.put(p, o);
            }
            paramNum++;
        }
        if (!parameters.isEmpty()) {
            message.setContent(List.class, parameters);
        }
    }
}
