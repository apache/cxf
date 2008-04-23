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

package org.apache.cxf.binding.xml.interceptor;

import java.util.ResourceBundle;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.bindings.xformat.XMLBindingMessageFormat;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.BareOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.WrappedOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.staxutils.StaxUtils;

public class XMLMessageOutInterceptor extends AbstractOutDatabindingInterceptor {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(XMLMessageOutInterceptor.class);

    public XMLMessageOutInterceptor() {
        this(Phase.MARSHAL);
    }
    public XMLMessageOutInterceptor(String phase) {
        super(phase);
        addAfter(WrappedOutInterceptor.class.getName());
    }

    public void handleMessage(Message message) throws Fault {
        BindingOperationInfo boi = message.getExchange().get(BindingOperationInfo.class);
        MessageInfo mi;
        BindingMessageInfo bmi;
        if (isRequestor(message)) {
            mi = boi.getOperationInfo().getInput();
            bmi = boi.getInput();
        } else {
            mi = boi.getOperationInfo().getOutput();
            bmi = boi.getOutput();
        }
        XMLBindingMessageFormat xmf = bmi.getExtensor(XMLBindingMessageFormat.class);
        QName rootInModel = null;
        if (xmf != null) {
            rootInModel = xmf.getRootNode();
        }
        if (boi.isUnwrapped() 
            || mi.getMessageParts().size() == 1) {
            // wrapper out interceptor created the wrapper
            // or if bare-one-param
            new BareOutInterceptor().handleMessage(message);
        } else {
            if (rootInModel == null) {
                rootInModel = boi.getName();
            }
            if (mi.getMessageParts().size() == 0 && !boi.isUnwrapped()) {
                // write empty operation qname
                writeMessage(message, rootInModel, false);
            } else {
                // multi param, bare mode, needs write root node
                writeMessage(message, rootInModel, true);
            }
        }
        // in the end we do flush ;)
        XMLStreamWriter writer = message.getContent(XMLStreamWriter.class);
        try {
            writer.flush();
        } catch (XMLStreamException e) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("STAX_WRITE_EXC", BUNDLE, e));
        }
    }

    private void writeMessage(Message message, QName name, boolean executeBare) {
        XMLStreamWriter xmlWriter = message.getContent(XMLStreamWriter.class);
        try {
            String pfx = name.getPrefix();
            if (StringUtils.isEmpty(pfx)) {
                pfx = "ns1";
            }
            StaxUtils.writeStartElement(xmlWriter,
                                        pfx,
                                        name.getLocalPart(),
                                        name.getNamespaceURI());
            if (executeBare) {
                new BareOutInterceptor().handleMessage(message);
            }
            xmlWriter.writeEndElement();
        } catch (XMLStreamException e) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("STAX_WRITE_EXC", BUNDLE, e));
        }

    }
}
