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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;

public class WrappedInInterceptor extends AbstractInDatabindingInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(WrappedInInterceptor.class);

    public WrappedInInterceptor() {
        super(Phase.UNMARSHAL);
        addAfter(URIMappingInterceptor.class.getName());
    }

    public void handleMessage(Message message) {
        if (isGET(message) && message.getContent(List.class) != null) {
            LOG.fine("WrappedInInterceptor skipped in HTTP GET method");
            return;
        }
        
        DepthXMLStreamReader xmlReader = getXMLStreamReader(message);

        // Trying to find the operation name from the XML.
        if (!StaxUtils.toNextElement(xmlReader)) {
            // body may be empty for partial response to decoupled request
            return;
        }

        BindingOperationInfo operation = message.getExchange().get(BindingOperationInfo.class);
        boolean requestor = isRequestor(message);

        if (operation == null) {
            String local = xmlReader.getLocalName();
            if (requestor && local.endsWith("Response")) {
                local = local.substring(0, local.length() - 8);
            }

            // TODO: Allow overridden methods.
            operation = ServiceModelUtil.getOperation(message.getExchange(), local);
            if (operation == null) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("NO_OPERATION", LOG, local),
                                Fault.FAULT_CODE_CLIENT);
            }

        }

        ServiceInfo si = operation.getBinding().getService();

        DataReader<XMLStreamReader> dr = getDataReader(message);
        MessageContentsList objects = new MessageContentsList();

        MessageInfo msgInfo = setMessage(message, operation, requestor, si);
        
        // Determine if there is a wrapper class
        if (operation.isUnwrappedCapable()
            && msgInfo.getMessageParts().get(0).getTypeClass() != null) {
            Object wrappedObject = dr.read(msgInfo.getMessageParts().get(0), xmlReader);
            objects.put(msgInfo.getMessageParts().get(0), wrappedObject);

        } else {
            // Unwrap each part individually if we don't have a wrapper

            if (operation.isUnwrappedCapable()) {
                operation = operation.getUnwrappedOperation();
            }

            msgInfo = setMessage(message, operation, requestor, si);
            List<MessagePartInfo> messageParts = msgInfo.getMessageParts();
            Iterator<MessagePartInfo> itr = messageParts.iterator();

            // advance just past the wrapped element so we don't get stuck
            if (xmlReader.getEventType() == XMLStreamConstants.START_ELEMENT) {
                StaxUtils.nextEvent(xmlReader);
            }

            // loop through each child element
            while (StaxUtils.toNextElement(xmlReader)) {
                MessagePartInfo part = itr.next();
                objects.put(part, dr.read(part, xmlReader));
            }
        }

        message.setContent(List.class, objects);
    }

    private MessageInfo setMessage(Message message, BindingOperationInfo operation,
                                   boolean requestor, ServiceInfo si) {
        MessageInfo msgInfo = getMessageInfo(message, operation, requestor);
        message.put(MessageInfo.class, msgInfo);

        message.getExchange().put(BindingOperationInfo.class, operation);
        message.getExchange().put(OperationInfo.class, operation.getOperationInfo());
        message.getExchange().setOneWay(operation.getOperationInfo().isOneWay());

        //Set standard MessageContext properties required by JAX_WS, but not specific to JAX_WS.
        message.put(Message.WSDL_OPERATION, operation.getName());
        
        QName serviceQName = si.getName();
        message.put(Message.WSDL_SERVICE, serviceQName);

        QName interfaceQName = si.getInterface().getName();
        message.put(Message.WSDL_INTERFACE, interfaceQName);

        EndpointInfo endpointInfo = message.getExchange().get(Endpoint.class).getEndpointInfo();
        QName portQName = endpointInfo.getName();
        message.put(Message.WSDL_PORT, portQName);

        String address = endpointInfo.getAddress();
        URI wsdlDescription = null;
        try {
            wsdlDescription = new URI(address + "?wsdl");
        } catch (URISyntaxException e) {
            //do nothing
        }
        message.put(Message.WSDL_DESCRIPTION, wsdlDescription);

        return msgInfo;
    }
}
