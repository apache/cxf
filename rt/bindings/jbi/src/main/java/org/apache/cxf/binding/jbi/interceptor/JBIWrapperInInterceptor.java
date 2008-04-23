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
package org.apache.cxf.binding.jbi.interceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.binding.jbi.JBIBindingInfo;
import org.apache.cxf.binding.jbi.JBIConstants;
import org.apache.cxf.binding.jbi.JBIFault;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.AbstractInDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;

public class JBIWrapperInInterceptor extends AbstractInDatabindingInterceptor {

    private static final Logger LOG = LogUtils.getL7dLogger(JBIWrapperInInterceptor.class);

    private static final ResourceBundle BUNDLE = LOG.getResourceBundle();

    public JBIWrapperInInterceptor() {
        super(Phase.UNMARSHAL);
    }

    public void handleMessage(Message message) throws Fault {
        if (isGET(message)) {
            LOG.info("JbiMessageInInterceptor skipped in HTTP GET method");
            return;
        }
        XMLStreamReader xsr = message.getContent(XMLStreamReader.class);

        DepthXMLStreamReader reader = new DepthXMLStreamReader(xsr);

        Endpoint ep = message.getExchange().get(Endpoint.class);
        BindingInfo binding = ep.getEndpointInfo().getBinding();
        if (!(binding instanceof JBIBindingInfo)) {
            throw new IllegalStateException(
                  new org.apache.cxf.common.i18n.Message("NEED_JBIBINDING", BUNDLE).toString());
        }

        if (!StaxUtils.toNextElement(reader)) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("NO_OPERATION_ELEMENT", BUNDLE));
        }

        Exchange ex = message.getExchange();
        QName startQName = reader.getName();

        // handling jbi fault message
        if (startQName.getLocalPart().equals(JBIFault.JBI_FAULT_ROOT)) {
            message.getInterceptorChain().abort();

            if (ep.getInFaultObserver() != null) {
                ep.getInFaultObserver().onMessage(message);
                return;
            }
        }


        // handling xml normal inbound message
        if (!startQName.equals(JBIConstants.JBI_WRAPPER_MESSAGE)) {
            throw new Fault(new org.apache.cxf.common.i18n.Message(
                    "NO_JBI_MESSAGE_ELEMENT", BUNDLE));
        }

        try {
            BindingOperationInfo bop = ex.get(BindingOperationInfo.class);
            DataReader<XMLStreamReader> dr = getDataReader(message);
            List<Object> parameters = new ArrayList<Object>();
            reader.next();
            BindingMessageInfo messageInfo = !isRequestor(message) ? bop.getInput() : bop.getOutput();
            message.put(MessageInfo.class, messageInfo.getMessageInfo());
            for (MessagePartInfo part : messageInfo.getMessageParts()) {
                if (!StaxUtils.skipToStartOfElement(reader)) {
                    throw new Fault(new org.apache.cxf.common.i18n.Message(
                            "NOT_ENOUGH_PARTS", BUNDLE));
                }
                startQName = reader.getName();
                if (!startQName.equals(JBIConstants.JBI_WRAPPER_PART)) {
                    throw new Fault(new org.apache.cxf.common.i18n.Message(
                            "NO_JBI_PART_ELEMENT", BUNDLE));
                }
                if (part.isElement()) {
                    reader.next();
                    if (!StaxUtils.toNextElement(reader)) {
                        throw new Fault(new org.apache.cxf.common.i18n.Message(
                                "EXPECTED_ELEMENT_IN_PART", BUNDLE));
                    }
                }
                parameters.add(dr.read(part, reader));
                // skip end element
                if (part.isElement()) {
                    reader.next();
                }
            }
            int ev = reader.getEventType();
            while (ev != XMLStreamConstants.END_ELEMENT 
                    && ev != XMLStreamConstants.START_ELEMENT
                    && ev != XMLStreamConstants.END_DOCUMENT) {
                ev = reader.next();
            }
            message.setContent(List.class, parameters);
        } catch (XMLStreamException e) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("STAX_READ_EXC", BUNDLE), e);
        }
    }

}
