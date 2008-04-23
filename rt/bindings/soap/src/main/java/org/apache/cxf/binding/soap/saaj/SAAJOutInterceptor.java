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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ResourceBundle;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;


import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;
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

    public SAAJOutInterceptor() {
        super(Phase.PRE_PROTOCOL);
    }
    
    public void handleMessage(SoapMessage message) throws Fault {
        SOAPMessage saaj = message.getContent(SOAPMessage.class);
        if (saaj == null) {
            SoapVersion version = message.getVersion();
            try {
                MessageFactory factory = null;
                if (version.getVersion() == 1.1) {
                    factory = MessageFactory.newInstance();
                } else {
                    factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
                }
                SOAPMessage soapMessage = factory.createMessage();

                SOAPPart soapPart = soapMessage.getSOAPPart();
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
            try {
                XMLStreamWriter dummyWriter = StaxUtils.getXMLOutputFactory()
                    .createXMLStreamWriter(new ByteArrayOutputStream());
                message.setContent(XMLStreamWriter.class, dummyWriter);
            } catch (XMLStreamException e) {
                // do nothing
            }
        }
        
        // Add a final interceptor to write the message
        message.getInterceptorChain().add(new SAAJOutEndingInterceptor());
    }
    
    public class SAAJOutEndingInterceptor extends AbstractSoapInterceptor {
        public SAAJOutEndingInterceptor() {
            super(SAAJOutEndingInterceptor.class.getName(), Phase.PRE_PROTOCOL_ENDING);
        }

        public void handleMessage(SoapMessage message) throws Fault {
            SOAPMessage soapMessage = message.getContent(SOAPMessage.class);
 
            if (soapMessage != null) {
                
                OutputStream os = message.getContent(OutputStream.class);
                try {
                    setMessageContent(message, soapMessage);
                    soapMessage.writeTo(os);
                    os.flush();
                } catch (IOException e) {
                    throw new SoapFault(new Message("SOAPEXCEPTION", BUNDLE), e, message.getVersion()
                        .getSender());
                } catch (SOAPException e) {
                    throw new SoapFault(new Message("SOAPEXCEPTION", BUNDLE), e, message.getVersion()
                        .getSender());
                }
            }
        }

        private void setMessageContent(SoapMessage message, SOAPMessage soapMessage) 
            throws SOAPException {
            
            if (soapMessage.getAttachments().hasNext()) {
                StringBuffer sb = new StringBuffer();
                for (String str : soapMessage.getMimeHeaders().getHeader("Content-Type")) {
                    sb.append(str);
                }
                String contentType = sb.toString();
                if (contentType != null && contentType.length() > 0) {
                    message.put(org.apache.cxf.message.Message.CONTENT_TYPE, contentType);
                }
                    
            }
            
        }

    }
}
