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

import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.binding.jbi.JBIConstants;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.StaxUtils;

public class JBIWrapperOutInterceptor extends AbstractOutDatabindingInterceptor {

    private static final Logger LOG = LogUtils.getL7dLogger(JBIWrapperOutInterceptor.class);

    private static final ResourceBundle BUNDLE = LOG.getResourceBundle();

    public JBIWrapperOutInterceptor() {
        super(Phase.MARSHAL);
    }

    public void handleMessage(Message message) throws Fault {
        BindingOperationInfo bop = message.getExchange().get(BindingOperationInfo.class);
        XMLStreamWriter xmlWriter = getXMLStreamWriter(message);
        Service service = message.getExchange().get(Service.class);
        
        DataWriter<XMLStreamWriter> dataWriter = getDataWriter(message, service, XMLStreamWriter.class);

        try {
            xmlWriter.setPrefix("jbi", JBIConstants.NS_JBI_WRAPPER);
            xmlWriter.writeStartElement(JBIConstants.NS_JBI_WRAPPER, 
                                        JBIConstants.JBI_WRAPPER_MESSAGE.getLocalPart());
            xmlWriter.writeNamespace("jbi", JBIConstants.NS_JBI_WRAPPER);
            setTypeAttr(xmlWriter, message);
            List<MessagePartInfo> parts = null;
            
            if (!isRequestor(message)) {
                parts = bop.getOutput().getMessageParts();
            } else {
                parts = bop.getInput().getMessageParts();
            }
            List<?> objs = (List<?>) message.getContent(List.class);                
            if (objs.size() < parts.size()) {
                throw new Fault(new org.apache.cxf.common.i18n.Message(
                        "NOT_EQUAL_ARG_NUM", BUNDLE));
            }
            for (int idx = 0; idx < parts.size(); idx++) {
                MessagePartInfo part = parts.get(idx);
                Object obj = objs.get(part.getIndex());
                if (!part.isElement()) {
                    if (part.getTypeClass() == String.class) {
                        xmlWriter.writeStartElement(JBIConstants.NS_JBI_WRAPPER, 
                                                    JBIConstants.JBI_WRAPPER_PART.getLocalPart());
                        writeWrapper(message, bop, xmlWriter);
                        xmlWriter.writeCharacters(obj.toString());
                        writeWrapperEnding(bop, xmlWriter);
                        xmlWriter.writeEndElement();
                    } else {
                        part = new MessagePartInfo(part.getName(), part.getMessageInfo());
                        part.setElement(false);
                        part.setConcreteName(JBIConstants.JBI_WRAPPER_PART);
                        dataWriter.write(obj, part, xmlWriter);
                    }
                } else {
                    xmlWriter.writeStartElement(JBIConstants.NS_JBI_WRAPPER, 
                                                JBIConstants.JBI_WRAPPER_PART.getLocalPart());
                    writeWrapper(message, bop, xmlWriter);
                    dataWriter.write(obj, part, xmlWriter);
                    writeWrapperEnding(bop, xmlWriter);
                    xmlWriter.writeEndElement();
                }
            }
            xmlWriter.writeEndElement();
            
        
        } catch (XMLStreamException e) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("STAX_WRITE_EXC", BUNDLE), e);
        }
    }

    private void writeWrapperEnding(BindingOperationInfo bop, XMLStreamWriter xmlWriter) 
        throws XMLStreamException {
        if (bop.isUnwrapped()) {
            xmlWriter.writeEndElement();
        }
    }

    private void writeWrapper(Message message, BindingOperationInfo bop, XMLStreamWriter xmlWriter) {
        if (bop.isUnwrapped()) {
            MessageInfo messageInfo;
            if (isRequestor(message)) {
                messageInfo = bop.getWrappedOperation().getOperationInfo().getInput();
            } else {
                messageInfo = bop.getWrappedOperation().getOperationInfo().getOutput();
            }

            MessagePartInfo outPart = messageInfo.getMessageParts().get(0);
            QName name = outPart.getConcreteName();

            try {
                String pfx = StaxUtils.getUniquePrefix(xmlWriter, name.getNamespaceURI());
                xmlWriter.setPrefix(pfx, name.getNamespaceURI());
                xmlWriter.writeStartElement(pfx, name.getLocalPart(), name.getNamespaceURI());
                xmlWriter.writeNamespace(pfx, name.getNamespaceURI());
            } catch (XMLStreamException e) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("STAX_WRITE_EXC", BUNDLE), e);
            }
        }
    }
    
    private void setTypeAttr(XMLStreamWriter xmlWriter, Message message) throws XMLStreamException {
        BindingOperationInfo wsdlOperation = getOperation(message);
        BindingMessageInfo wsdlMessage = isRequestor(message)
            ? wsdlOperation.getInput() : wsdlOperation.getOutput();
        
        String typeNamespace = wsdlMessage.getMessageInfo().getName().getNamespaceURI();
        if (typeNamespace == null || typeNamespace.length() == 0) {
            throw new IllegalArgumentException("messageType namespace is null or empty");
        }
        xmlWriter.writeAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":" + "msg",
                          typeNamespace);
        String typeLocalName = wsdlMessage.getMessageInfo().getName().getLocalPart();
        if (typeLocalName == null || typeLocalName.length() == 0) {
            throw new IllegalArgumentException("messageType local name is null or empty");
        }
        xmlWriter.writeAttribute("type", "msg" + ":" + typeLocalName);
        
    }
    
    protected BindingOperationInfo getOperation(Message message) {
        BindingOperationInfo operation = message.getExchange().get(BindingOperationInfo.class);
        if (operation == null) {
            throw new Fault(new Exception("Operation not bound on this message"));
        }
        return operation;
    }
    
    protected boolean isRequestor(org.apache.cxf.message.Message message) {
        return Boolean.TRUE.equals(message.containsKey(
            org.apache.cxf.message.Message.REQUESTOR_ROLE));
    }  

}
