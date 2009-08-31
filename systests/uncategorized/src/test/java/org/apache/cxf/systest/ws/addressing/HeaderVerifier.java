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

package org.apache.cxf.systest.ws.addressing;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.addressing.soap.VersionTransformer;
import org.apache.cxf.ws.addressing.v200408.AttributedURI;

import static org.apache.cxf.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_INBOUND;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_OUTBOUND;


/**
 * Verifies presence of expected SOAP headers.
 */
public class HeaderVerifier extends AbstractSoapInterceptor {
    VerificationCache verificationCache;
    String currentNamespaceURI;
    
    public HeaderVerifier() {
        super(Phase.POST_PROTOCOL);
    }
    
    public Set<QName> getUnderstoodHeaders() {
        return Names.HEADERS;
    }

    public void handleMessage(SoapMessage message) {
        mediate(message);
    }

    public void handleFault(SoapMessage message) {
        mediate(message);
    }
    
    private void mediate(SoapMessage message) {
        boolean outgoingPartialResponse = isOutgoingPartialResponse(message);
        if (outgoingPartialResponse) {
            addPartialResponseHeader(message);
        }
        verify(message, outgoingPartialResponse);
    }

    private void addPartialResponseHeader(SoapMessage message) {
        try {
            // add piggybacked wsa:From header to partial response
            List<Header> header = message.getHeaders();
            Document doc = DOMUtils.createDocument();
            SoapVersion ver = message.getVersion();
            Element hdr = doc.createElementNS(ver.getHeader().getNamespaceURI(), 
                ver.getHeader().getLocalPart());
            hdr.setPrefix(ver.getHeader().getPrefix());
            
            marshallFrom("urn:piggyback_responder", hdr, getMarshaller());
            Element elem = DOMUtils.getFirstElement(hdr);
            while (elem != null) {
                Header holder = new Header(
                        new QName(elem.getNamespaceURI(), elem.getLocalName()), 
                        elem, null);
                header.add(holder);
                
                elem = DOMUtils.getNextElement(elem);
            }
            
        } catch (Exception e) {
            verificationCache.put("SOAP header addition failed: " + e);
            e.printStackTrace();
        }
    }

    private void verify(SoapMessage message, boolean outgoingPartialResponse) {
        try {
            List<String> wsaHeaders = new ArrayList<String>();
            List<Header> headers = message.getHeaders();
            if (headers != null) {
                recordWSAHeaders(headers,
                                 wsaHeaders,
                                 Names.WSA_NAMESPACE_NAME);
                recordWSAHeaders(headers,
                                 wsaHeaders,
                                 VersionTransformer.Names200408.WSA_NAMESPACE_NAME);
                recordWSAHeaders(headers,
                                 wsaHeaders,
                                 MAPTestBase.CUSTOMER_NAME.getNamespaceURI());
            }
            boolean partialResponse = isIncomingPartialResponse(message)
                                      || outgoingPartialResponse;
            verificationCache.put(MAPTest.verifyHeaders(wsaHeaders, 
                                                        partialResponse,
                                                        isRequestLeg(message)));
        } catch (SOAPException se) {
            verificationCache.put("SOAP header verification failed: " + se);
        }
    }

    private void recordWSAHeaders(List<Header> headers,
                                  List<String> wsaHeaders,
                                  String namespaceURI) {
        Iterator<Header> iter = headers.iterator();
        while (iter.hasNext()) {
            Object obj = iter.next().getObject();
            if (obj instanceof Element) {
                Element hdr = (Element) obj;
                if (namespaceURI.equals(hdr.getNamespaceURI())) {
                    if (namespaceURI.endsWith("addressing")) {
                        currentNamespaceURI = namespaceURI;
                        wsaHeaders.add(hdr.getLocalName());
                    } else if (MAPTestBase.CUSTOMER_NAME.getNamespaceURI().equals(namespaceURI)) {
                        String headerText = hdr.getTextContent();
                        if (MAPTestBase.CUSTOMER_KEY.equals(headerText)) {
                            wsaHeaders.add(hdr.getLocalName());
                        }
                    }
                }
            }
            
        }
    }

    private boolean isRequestLeg(SoapMessage message) {
        return (ContextUtils.isRequestor(message) && ContextUtils.isOutbound(message))
               || (!ContextUtils.isRequestor(message) && !ContextUtils.isOutbound(message));     
    }

    private boolean isOutgoingPartialResponse(SoapMessage message) {
        AddressingProperties maps = 
            (AddressingProperties)message.get(SERVER_ADDRESSING_PROPERTIES_OUTBOUND);
        return ContextUtils.isOutbound(message)
               && !ContextUtils.isRequestor(message)
               && maps != null
               && Names.WSA_ANONYMOUS_ADDRESS.equals(maps.getTo().getValue());
    }
    
    private boolean isIncomingPartialResponse(SoapMessage message) 
        throws SOAPException {
        AddressingProperties maps = 
            (AddressingProperties)message.get(CLIENT_ADDRESSING_PROPERTIES_INBOUND);
        return !ContextUtils.isOutbound(message)
               && ContextUtils.isRequestor(message)
               && maps != null
               && Names.WSA_ANONYMOUS_ADDRESS.equals(maps.getTo().getValue());
    }
    
    private Marshaller getMarshaller() throws JAXBException {
        JAXBContext jaxbContext =
            VersionTransformer.getExposedJAXBContext(currentNamespaceURI);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        return marshaller;
    }

    private void marshallFrom(String from, Element header, Marshaller marshaller) 
        throws JAXBException {
        if (Names.WSA_NAMESPACE_NAME.equals(currentNamespaceURI)) {
            String u = "urn:piggyback_responder";
            AttributedURIType value =
                org.apache.cxf.ws.addressing.ContextUtils.getAttributedURI(u);
            marshaller.marshal(
                new JAXBElement<AttributedURIType>(Names.WSA_FROM_QNAME,
                                                   AttributedURIType.class,
                                                   value),
                header);
        } else if (VersionTransformer.Names200408.WSA_NAMESPACE_NAME.equals(
                                                      currentNamespaceURI)) {
            AttributedURI value =
                VersionTransformer.Names200408.WSA_OBJECT_FACTORY.createAttributedURI();
            value.setValue(from);
            QName qname = new QName(VersionTransformer.Names200408.WSA_NAMESPACE_NAME, 
                                    Names.WSA_FROM_NAME);
            marshaller.marshal(
                new JAXBElement<AttributedURI>(qname,
                                               AttributedURI.class,
                                               value),
                header);
        }                                                                    
    }
    
    public void setVerificationCache(VerificationCache cache) {
        verificationCache = cache;
    }
}
