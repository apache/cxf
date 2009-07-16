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


import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.xml.namespace.QName;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.Detail;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.Soap11FaultInInterceptor;
import org.apache.cxf.binding.soap.interceptor.Soap12FaultInInterceptor;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.headers.Header;
import org.apache.cxf.headers.HeaderManager;
import org.apache.cxf.headers.HeaderProcessor;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;

/**
 * Builds a SAAJ tree from the Document fragment inside the message which contains
 * the SOAP headers and from the XMLStreamReader.
 */
public class SAAJInInterceptor extends AbstractSoapInterceptor {
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(SAAJInInterceptor.class);
    
    private MessageFactory factory11;
    private MessageFactory factory12;
    
    
    public SAAJInInterceptor() {
        super(Phase.PRE_PROTOCOL);
    }
    public SAAJInInterceptor(String phase) {
        super(phase);
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
        try {
            MessageFactory factory = getFactory(message);
            SOAPMessage soapMessage = factory.createMessage();
            message.setContent(SOAPMessage.class, soapMessage);
            
            SOAPPart part = soapMessage.getSOAPPart();
            
            Document node = (Document) message.getContent(Node.class);
            DOMSource source = new DOMSource(node);
            part.setContent(source);
            Collection<Attachment> atts = message.getAttachments();
            if (atts != null) {
                for (Attachment a : atts) {
                    AttachmentPart ap = soapMessage.createAttachmentPart(a.getDataHandler());
                    ap.setContentId(a.getId());
                    Iterator<String> i = a.getHeaderNames();
                    while (i != null && i.hasNext()) {
                        String h = i.next();
                        String val = a.getHeader(h);
                        ap.addMimeHeader(h, val);
                    }
                    soapMessage.addAttachmentPart(ap);
                }
            }
            
            //replace header element if necessary
            if (message.hasHeaders()) {
                replaceHeaders(soapMessage, message);
            }
            if (soapMessage.getSOAPHeader() == null) {
                soapMessage.getSOAPPart().getEnvelope().addHeader();
            }
            
            XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);

            if (hasFault(message, xmlReader)) {
                SOAPFault soapFault = 
                    soapMessage.getSOAPPart().getEnvelope().getBody().addFault();
                SoapFault fault = 
                    message.getVersion() instanceof Soap11 
                    ? Soap11FaultInInterceptor.unmarshalFault(message, xmlReader)
                    : Soap12FaultInInterceptor.unmarshalFault(message, xmlReader);
                if (fault.getFaultCode() != null) {
                    soapFault.setFaultCode(fault.getFaultCode());
                }
                if (fault.getMessage() != null) {
                    soapFault.setFaultString(fault.getMessage());
                }
                if (fault.getRole() != null) {
                    soapFault.setFaultActor(fault.getRole());
                }
                if (fault.getDetail() != null
                    && fault.getDetail().getFirstChild() != null) {
                    
                    Detail detail = null;
                    Node child = fault.getDetail().getFirstChild();
                    while (child != null) {
                        if (Node.ELEMENT_NODE == child.getNodeType()) {
                            if (detail == null) {
                                detail = soapFault.addDetail();
                            }
                            Node importedChild = soapMessage.getSOAPPart().importNode(child, true);
                            detail.appendChild(importedChild);
                        }
                        child = child.getNextSibling();
                    }
                }

                DOMSource bodySource = new DOMSource(soapFault);
                xmlReader = StaxUtils.createXMLStreamReader(bodySource);
            } else { 
                StaxUtils.readDocElements(soapMessage.getSOAPBody(), xmlReader, true);
                DOMSource bodySource = new DOMSource(soapMessage.getSOAPPart().getEnvelope().getBody());
                xmlReader = StaxUtils.createXMLStreamReader(bodySource);
                xmlReader.nextTag();
                xmlReader.nextTag(); // move past body tag
            }
            message.setContent(XMLStreamReader.class, xmlReader);           
        } catch (SOAPException soape) {
            throw new SoapFault(new org.apache.cxf.common.i18n.Message(
                    "SOAPHANDLERINTERCEPTOR_EXCEPTION", BUNDLE), soape,
                    message.getVersion().getSender());
        } catch (XMLStreamException e) {
            throw new SoapFault(new org.apache.cxf.common.i18n.Message(
                    "SOAPHANDLERINTERCEPTOR_EXCEPTION", BUNDLE), e, message
                    .getVersion().getSender());
        }
    }

    public static void replaceHeaders(SOAPMessage soapMessage, SoapMessage message) throws SOAPException {
        SOAPHeader header = soapMessage.getSOAPHeader();
        if (header == null) {
            return;
        }
        Element elem = DOMUtils.getFirstElement(header);
        while (elem != null) {
            Bus b = message.getExchange().get(Bus.class);
            HeaderProcessor p =  null;
            if (b != null && b.getExtension(HeaderManager.class) != null) {
                p = b.getExtension(HeaderManager.class).getHeaderProcessor(elem.getNamespaceURI());
            }
                
            Object obj;
            DataBinding dataBinding = null;
            if (p == null || p.getDataBinding() == null) {
                obj = elem;
            } else {
                obj = p.getDataBinding().createReader(Node.class).read(elem);
            }
            //TODO - add the interceptors
                
            SoapHeader shead = new SoapHeader(new QName(elem.getNamespaceURI(),
                                                        elem.getLocalName()),
                                               obj,
                                               dataBinding);
            shead.setDirection(SoapHeader.Direction.DIRECTION_IN);
                
            String mu = elem.getAttributeNS(message.getVersion().getNamespace(),
                    message.getVersion().getAttrNameMustUnderstand());
            String act = elem.getAttributeNS(message.getVersion().getNamespace(),
                    message.getVersion().getAttrNameRole());
                
            shead.setActor(act);
            shead.setMustUnderstand(Boolean.valueOf(mu) || "1".equals(mu));
            Header oldHdr = message.getHeader(
                    new QName(elem.getNamespaceURI(), elem.getLocalName()));
            if (oldHdr != null) {
                message.getHeaders().remove(oldHdr);
            } 
            message.getHeaders().add(shead);                        
            
            elem = DOMUtils.getNextElement(elem);
        }
    }


    private static boolean hasFault(SoapMessage message, 
                                    XMLStreamReader xmlReader) {
        try {
            QName name = xmlReader.getName();
            return message.getVersion().getFault().equals(name);
        } catch (Exception e) {
            return false;
        }
    }
}
