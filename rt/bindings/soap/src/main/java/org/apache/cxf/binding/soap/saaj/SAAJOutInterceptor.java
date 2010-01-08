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

package org.apache.cxf.binding.soap.saaj;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;


import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamReader;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;



/**
 * Sets up the outgoing chain to build a SAAJ tree instead of writing
 * directly to the output stream. First it will replace the XMLStreamWriter
 * with one which writes to a SOAPMessage. Then it will add an interceptor
 * at the end of the chain in the SEND phase which writes the resulting
 * SOAPMessage.
 */
public class SAAJOutInterceptor extends AbstractSoapInterceptor {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(SAAJOutInterceptor.class);
    private static final String ORIGINAL_XML_WRITER 
        = SAAJOutInterceptor.class.getName() + ".original.xml.writer";
    private MessageFactory factory11;
    private MessageFactory factory12;
    
    public SAAJOutInterceptor() {
        super(Phase.PRE_PROTOCOL);
    }
    private synchronized MessageFactory getFactory(SoapMessage message) throws SOAPException {
        if (message.getVersion() instanceof Soap11) {
            if (factory11 == null) { 
                factory11 = MessageFactory.newInstance();
            } 
            return factory11;
        }
        if (factory12 == null) {
            factory12 = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
        }
        return factory12;
    }
    public void handleMessage(SoapMessage message) throws Fault {
        SOAPMessage saaj = message.getContent(SOAPMessage.class);
        if (saaj == null) {
            SoapVersion version = message.getVersion();
            try {
                MessageFactory factory = getFactory(message);
                SOAPMessage soapMessage = factory.createMessage();

                SOAPPart soapPart = soapMessage.getSOAPPart();
                
                XMLStreamWriter origWriter = message.getContent(XMLStreamWriter.class);
                message.put(ORIGINAL_XML_WRITER, origWriter);
                W3CDOMStreamWriter writer = new W3CDOMStreamWriter(soapPart);
                // Replace stax writer with DomStreamWriter
                message.setContent(XMLStreamWriter.class, writer);
                message.setContent(SOAPMessage.class, soapMessage);
                
                
            } catch (SOAPException e) {
                throw new SoapFault(new Message("SOAPEXCEPTION", BUNDLE), e, version.getSender());
            }
        } else {
            //as the SOAPMessage already has everything in place, we do not need XMLStreamWriter to write
            //anything for us, so we just set XMLStreamWriter's output to a dummy output stream.         

            XMLStreamWriter origWriter = message.getContent(XMLStreamWriter.class);
            message.put(ORIGINAL_XML_WRITER, origWriter);
            
            XMLStreamWriter dummyWriter = StaxUtils.createXMLStreamWriter(new OutputStream() {
                    public void write(int b) throws IOException {
                    }
                    public void write(byte b[], int off, int len) throws IOException {
                    }                        
                });
            message.setContent(XMLStreamWriter.class, dummyWriter);
        }
        
        // Add a final interceptor to write the message
        message.getInterceptorChain().add(new SAAJOutEndingInterceptor());
    }
    @Override
    public void handleFault(SoapMessage message) {
        super.handleFault(message);
        //need to clear these so the fault writing will work correctly
        message.removeContent(SOAPMessage.class);
        message.remove(SoapOutInterceptor.WROTE_ENVELOPE_START);
        XMLStreamWriter writer = (XMLStreamWriter)message.get(ORIGINAL_XML_WRITER);
        if (writer != null) {
            message.setContent(XMLStreamWriter.class, writer);
        }
    }

    
    public class SAAJOutEndingInterceptor extends AbstractSoapInterceptor {
        public SAAJOutEndingInterceptor() {
            super(SAAJOutEndingInterceptor.class.getName(), Phase.PRE_PROTOCOL_ENDING);
        }

        public void handleMessage(SoapMessage message) throws Fault {
            SOAPMessage soapMessage = message.getContent(SOAPMessage.class);
 
            if (soapMessage != null) {
                if (soapMessage.countAttachments() > 0) {
                    if (message.getAttachments() == null) {
                        message.setAttachments(new ArrayList<Attachment>(soapMessage
                                .countAttachments()));
                    }
                    Iterator<AttachmentPart> it = CastUtils.cast(soapMessage.getAttachments());
                    while (it.hasNext()) {
                        AttachmentPart part = it.next();
                        AttachmentImpl att = new AttachmentImpl(part.getContentId());
                        try {
                            att.setDataHandler(part.getDataHandler());
                        } catch (SOAPException e) {
                            throw new Fault(e);
                        }
                        Iterator<MimeHeader> it2 = CastUtils.cast(part.getAllMimeHeaders());
                        while (it2.hasNext()) {
                            MimeHeader header = it2.next();
                            att.setHeader(header.getName(), header.getValue());
                        }
                        message.getAttachments().add(att);
                    }
                }
                
                XMLStreamWriter writer = (XMLStreamWriter)message.get(ORIGINAL_XML_WRITER);
                try {
                    if (writer != null) {
                        StaxUtils.copy(new W3CDOMStreamReader(soapMessage.getSOAPPart()), writer);
                        writer.flush();
                        message.setContent(XMLStreamWriter.class, writer);
                    }
                } catch (XMLStreamException e) {
                    throw new SoapFault(new Message("SOAPEXCEPTION", BUNDLE), e, message.getVersion()
                                        .getSender());
                }
            }
        }

    }
}
