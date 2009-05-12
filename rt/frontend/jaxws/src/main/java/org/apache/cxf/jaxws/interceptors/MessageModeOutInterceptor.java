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

package org.apache.cxf.jaxws.interceptors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.activation.DataSource;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;

import org.apache.cxf.attachment.AttachmentDeserializer;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor.SAAJOutEndingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.staxutils.OverlayW3CDOMStreamWriter;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;

public class MessageModeOutInterceptor extends AbstractPhaseInterceptor<Message> {
    MessageModeOutInterceptorInternal internal;
    SAAJOutInterceptor saajOut;
    Class<?> type;
    QName bindingName;

    public MessageModeOutInterceptor(SAAJOutInterceptor saajOut, QName bname) {
        super(Phase.PREPARE_SEND);
        this.saajOut = saajOut;
        this.bindingName = bname;
        internal = new MessageModeOutInterceptorInternal();
    }
    public MessageModeOutInterceptor(Class<?> t, QName bname) {
        super(Phase.PREPARE_SEND);
        type = t;
        this.bindingName = bname;
    }
    public void handleMessage(Message message) throws Fault {
        if (!bindingName.equals(message.getExchange().get(BindingOperationInfo.class)
                                .getBinding().getName())) {
            return;
        }
        if (saajOut != null) {
            doSoap(message);
        } else if (DataSource.class.isAssignableFrom(type)) {
            //datasource stuff, must check if multi-source
            MessageContentsList list = (MessageContentsList)message.getContent(List.class);
            DataSource ds = (DataSource)list.get(0);
            String ct = ds.getContentType();
            if (ct.toLowerCase().contains("multipart/related")) {
                Message msg = new MessageImpl();
                msg.setExchange(message.getExchange());
                msg.put(Message.CONTENT_TYPE, ct);
                try {
                    msg.setContent(InputStream.class, ds.getInputStream());
                    AttachmentDeserializer deser = new AttachmentDeserializer(msg);
                    deser.initializeAttachments();
                } catch (IOException ex) {
                    throw new Fault(ex);
                }
                message.setAttachments(msg.getAttachments());
                final InputStream in = msg.getContent(InputStream.class);
                final String ct2 = (String)msg.get(Message.CONTENT_TYPE);
                list.set(0, new DataSource() {

                    public String getContentType() {
                        return ct2;
                    }

                    public InputStream getInputStream() throws IOException {
                        return in;
                    }

                    public String getName() {
                        return ct2;
                    }

                    public OutputStream getOutputStream() throws IOException {
                        // TODO Auto-generated method stub
                        return null;
                    }
                    
                });
            }
        }
        
    }
    
    
    private void doSoap(Message message) {
        MessageContentsList list = (MessageContentsList)message.getContent(List.class);
        Object o = list.get(0);
        if (o instanceof SOAPMessage) {
            SOAPMessage soapMessage = (SOAPMessage)o;
            if (soapMessage.countAttachments() > 0) {
                message.put("write.attachments", Boolean.TRUE);
            }
        }
        message.getInterceptorChain().add(internal);
    }
    
    private class MessageModeOutInterceptorInternal extends AbstractSoapInterceptor {
        MessageModeOutInterceptorInternal() {
            super(Phase.PRE_PROTOCOL);
            addBefore(SAAJOutInterceptor.class.getName());
        }
        
        public void handleMessage(SoapMessage message) throws Fault {
            MessageContentsList list = (MessageContentsList)message.getContent(List.class);
            Object o = list.get(0);
            SOAPMessage soapMessage = null;
            
            if (o instanceof SOAPMessage) {
                soapMessage = (SOAPMessage)o;
                if (soapMessage.countAttachments() > 0) {
                    message.put("write.attachments", Boolean.TRUE);
                }
            } else {
                try {
                    MessageFactory factory = saajOut.getFactory(message);
                    soapMessage = factory.createMessage();
                    SOAPPart part = soapMessage.getSOAPPart();
                    if (o instanceof Source) {
                        StaxUtils.copy((Source)o, new W3CDOMStreamWriter(part));
                    }
                } catch (SOAPException e) {
                    throw new SoapFault("Error creating SOAPMessage", e, 
                                        message.getVersion().getSender());
                } catch (XMLStreamException e) {
                    throw new SoapFault("Error creating SOAPMessage", e, 
                                        message.getVersion().getSender());
                }
            }
            message.setContent(SOAPMessage.class, soapMessage);
            
            if (!message.containsKey(SAAJOutInterceptor.ORIGINAL_XML_WRITER)) {
                XMLStreamWriter origWriter = message.getContent(XMLStreamWriter.class);
                message.put(SAAJOutInterceptor.ORIGINAL_XML_WRITER, origWriter);
            }
            W3CDOMStreamWriter writer = new OverlayW3CDOMStreamWriter(soapMessage.getSOAPPart());
            // Replace stax writer with DomStreamWriter
            message.setContent(XMLStreamWriter.class, writer);
            message.setContent(SOAPMessage.class, soapMessage);
            
            DocumentFragment frag = soapMessage.getSOAPPart().createDocumentFragment();
            try {
                Node body = soapMessage.getSOAPBody();
                Node nd = body.getFirstChild();
                while (nd != null) {
                    body.removeChild(nd);
                    frag.appendChild(nd);
                    nd = soapMessage.getSOAPBody().getFirstChild();
                }
                list.set(0, frag);
            } catch (Exception ex) {
                throw new Fault(ex);
            }
            BindingOperationInfo bop = message.getExchange().get(BindingOperationInfo.class);
            if (bop != null && bop.isUnwrapped()) {
                bop = bop.getWrappedOperation();
                message.getExchange().put(BindingOperationInfo.class, bop);
            }
            
            // Add a final interceptor to write the message
            message.getInterceptorChain().add(SAAJOutEndingInterceptor.INSTANCE);
        }
    }
        
}