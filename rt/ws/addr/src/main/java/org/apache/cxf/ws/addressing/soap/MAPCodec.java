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

package org.apache.cxf.ws.addressing.soap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AddressingPropertiesImpl;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.addressing.RelatesToType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;


/**
 * SOAP interceptor responsible for {en|de}coding the Message Addressing 
 * Properties for {outgo|incom}ing messages.
 */
public class MAPCodec extends AbstractSoapInterceptor {

    private static final Logger LOG = LogUtils.getL7dLogger(MAPCodec.class);
    private static final String IS_REFERENCE_PARAM_ATTR_NAME = "IsReferenceParameter";

    /**
     * REVISIT: map usage that the *same* interceptor instance 
     * is used in all chains.
     */
    protected final Map<String, Exchange> uncorrelatedExchanges 
        = new ConcurrentHashMap<String, Exchange>();

    private VersionTransformer transformer;
    private HeaderFactory headerFactory;
    
    /**
     * Constructor.
     */
    public MAPCodec() {
        super(Phase.PRE_PROTOCOL);
        addBefore("org.apache.cxf.jaxws.handler.soap.SOAPHandlerInterceptor");
        transformer = new VersionTransformer(this);
    } 

    /**
     * @return the set of SOAP headers understood by this handler 
     */
    public Set<QName> getUnderstoodHeaders() {
        return VersionTransformer.HEADERS;
    }
    
    /**
     * Invoked for normal processing of inbound and outbound messages.
     *
     * @param message the messsage
     */
    public void handleMessage(SoapMessage message) {
        mediate(message);
    }

    /**
     * Invoked when unwinding normal interceptor chain when a fault occurred.
     *
     * @param message the messsage message
     */
    public void handleFault(SoapMessage message) {
        AddressingProperties maps = ContextUtils.retrieveMAPs(message, false, true, false);
        if (ContextUtils.isRequestor(message) 
            && !message.getExchange().isOneWay()
            && maps != null) {
            //fault occured trying to sent the message, remove it
            uncorrelatedExchanges.remove(maps.getMessageID().getValue());
        }
    }

    /**
     * Mediate message flow, peforming MAP {en|de}coding.
     * 
     * @param message the messsage message
     */     
    private void mediate(SoapMessage message) {
        if (ContextUtils.isOutbound(message)) {
            encode(message, ContextUtils.retrieveMAPs(message, false, true));
        } else if (null == ContextUtils.retrieveMAPs(message, false, false, false)) {            
            AddressingProperties maps = decode(message);
            ContextUtils.storeMAPs(maps, message, false);
            markPartialResponse(message, maps);
            restoreExchange(message, maps);     
        }
    }

    /**
     * Encode the current MAPs in protocol-specific headers.
     *
     * @param message the messsage message
     * @param maps the MAPs to encode
     */
    private void encode(SoapMessage message, 
                        AddressingPropertiesImpl maps) {
        if (maps != null) { 
            cacheExchange(message, maps);
            LOG.log(Level.FINE, "Outbound WS-Addressing headers");
            try {
                List<Header> header = message.getHeaders();
                discardMAPs(header, maps);

                Element hdr = getHeaderFactory().getHeader(message.getVersion());                
                JAXBContext jaxbContext = 
                    VersionTransformer.getExposedJAXBContext(
                                                     maps.getNamespaceURI());
                Marshaller marshaller = jaxbContext.createMarshaller();
                QName duplicate = maps.getDuplicate();
                marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
                encodeAsExposed(maps,
                                message,
                                maps.getAction(), 
                                Names.WSA_ACTION_QNAME,
                                AttributedURIType.class, 
                                hdr, 
                                marshaller);
                if (Names.WSA_ACTION_QNAME.equals(duplicate)) {
                    encodeAsExposed(maps,
                                    message,
                                    maps.getAction(), 
                                    Names.WSA_ACTION_QNAME,
                                    AttributedURIType.class, 
                                    hdr, 
                                    marshaller);
                }
                encodeAsExposed(maps,
                                message,
                                maps.getMessageID(), 
                                Names.WSA_MESSAGEID_QNAME,
                                AttributedURIType.class, 
                                hdr, 
                                marshaller);
                if (Names.WSA_MESSAGEID_QNAME.equals(duplicate)) {
                    encodeAsExposed(maps,
                                    message,
                                    maps.getMessageID(), 
                                    Names.WSA_MESSAGEID_QNAME,
                                    AttributedURIType.class, 
                                    hdr, 
                                    marshaller);
                }
                encodeAsExposed(maps,
                                message,
                                maps.getTo(), 
                                Names.WSA_TO_QNAME,
                                AttributedURIType.class,  
                                hdr, 
                                marshaller);
                if (Names.WSA_TO_QNAME.equals(duplicate)) {
                    encodeAsExposed(maps,
                                    message,
                                    maps.getTo(), 
                                    Names.WSA_TO_QNAME,
                                    AttributedURIType.class,  
                                    hdr, 
                                    marshaller);
                }
                encodeAsExposed(maps,
                                message,
                                maps.getReplyTo(), 
                                Names.WSA_REPLYTO_QNAME, 
                                EndpointReferenceType.class,
                                hdr,
                                marshaller);
                if (Names.WSA_REPLYTO_QNAME.equals(duplicate)) {
                    encodeAsExposed(maps,
                                    message,
                                    maps.getReplyTo(), 
                                    Names.WSA_REPLYTO_QNAME, 
                                    EndpointReferenceType.class,
                                    hdr,
                                    marshaller);
                }

                encodeAsExposed(maps,
                                message,
                                maps.getRelatesTo(),
                                Names.WSA_RELATESTO_QNAME,
                                RelatesToType.class,
                                hdr,
                                marshaller);
                if (Names.WSA_RELATESTO_QNAME.equals(duplicate)) {
                    encodeAsExposed(maps,
                                    message,
                                    maps.getRelatesTo(),
                                    Names.WSA_RELATESTO_QNAME,
                                    RelatesToType.class,
                                    hdr,
                                    marshaller);
                }
                encodeAsExposed(maps,
                                message,
                                maps.getFrom(), 
                                Names.WSA_FROM_QNAME,
                                EndpointReferenceType.class,  
                                hdr, 
                                marshaller);
                if (Names.WSA_FROM_QNAME.equals(duplicate)) {
                    encodeAsExposed(maps,
                                    message,
                                    maps.getFrom(), 
                                    Names.WSA_FROM_QNAME,
                                    EndpointReferenceType.class,  
                                    hdr, 
                                    marshaller);
                }
                if (maps.getFaultTo() != null
                    && maps.getFaultTo().getAddress() != null
                    && maps.getFaultTo().getAddress().getValue() != null
                    && !maps.getFaultTo().getAddress().getValue()
                        .equals(maps.getReplyTo().getAddress().getValue())) {
                    encodeAsExposed(maps,
                                    message,
                                    maps.getFaultTo(), 
                                    Names.WSA_FAULTTO_QNAME, 
                                    EndpointReferenceType.class,
                                    hdr,
                                    marshaller);
                    if (Names.WSA_FAULTTO_QNAME.equals(duplicate)) {
                        encodeAsExposed(maps,
                                        message,
                                        maps.getFaultTo(), 
                                        Names.WSA_FAULTTO_QNAME, 
                                        EndpointReferenceType.class,
                                        hdr,
                                        marshaller);
                    }
                }
                encodeReferenceParameters(maps, hdr, marshaller);
                
                Node childNode = hdr.getFirstChild();
                
                while (childNode != null) {
                    Header holder = new Header(
                                               new QName(childNode.getNamespaceURI(), 
                                                         childNode.getLocalName()), 
                                                         childNode);
                    header.add(holder);
                    childNode = childNode.getNextSibling();
                }
                ((AddressingPropertiesImpl)maps).setDuplicate(null);
                
                propogateAction(maps.getAction(), message);
                applyMAPValidation(message);
            } catch (JAXBException je) {
                LOG.log(Level.WARNING, "SOAP_HEADER_ENCODE_FAILURE_MSG", je);
            }
        }
    }

    private void encodeReferenceParameters(AddressingProperties maps, Element header, 
                                           Marshaller marshaller) throws JAXBException {
        EndpointReferenceType toEpr = maps.getToEndpointReference();
        if (null != toEpr) {
            ReferenceParametersType params = toEpr.getReferenceParameters();
            if (null != params) {
                for (Object o : params.getAny()) {
                    if (o instanceof Element || o instanceof JAXBElement) {
                        JAXBElement jaxbEl = null;
                        if (o instanceof Element) {
                            Element e = (Element)o;
                            QName elQn = new QName(e.getNamespaceURI(), e.getLocalName());
                            jaxbEl = new JAXBElement<String>(elQn,
                                String.class, 
                                e.getTextContent());
                        } else {
                            jaxbEl = (JAXBElement) o;
                        }
                        marshaller.marshal(jaxbEl, header);
                        
                        Element lastAdded = (Element)header.getLastChild();
                        addIsReferenceParameterMarkerAttribute(lastAdded, maps.getNamespaceURI());
                    } else {
                        LOG.log(Level.WARNING, "IGNORE_NON_ELEMENT_REF_PARAM_MSG", o);
                    }
                }
            }
        }
    }
 
    private void addIsReferenceParameterMarkerAttribute(Element lastAdded, String namespaceURI) {
        String pfx = lastAdded.lookupPrefix(namespaceURI);
        if (StringUtils.isEmpty(pfx)) {
            //attributes cannot be in empty namespace...
            if (lastAdded.lookupNamespaceURI(JAXWSAConstants.WSA_PREFIX) == null) {
                pfx = JAXWSAConstants.WSA_PREFIX;
                Attr attr = lastAdded.getOwnerDocument()
                    .createAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:wsa");
                attr.setValue(namespaceURI);
                lastAdded.setAttributeNodeNS(attr);
            } else if (lastAdded.lookupNamespaceURI(JAXWSAConstants.WSA_PREFIX).equals(namespaceURI)) {
                pfx = JAXWSAConstants.WSA_PREFIX;
            } else {
                int cnt = 1;
                while (lastAdded.lookupNamespaceURI(JAXWSAConstants.WSA_PREFIX + cnt) != null) {
                    cnt++;
                }
                pfx = JAXWSAConstants.WSA_PREFIX + cnt;
                Attr attr = lastAdded.getOwnerDocument()
                    .createAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:wsa" + cnt);
                attr.setValue(namespaceURI);
                lastAdded.setAttributeNodeNS(attr);
            }
        }
        Attr isRefParamAttr = 
            lastAdded.getOwnerDocument().createAttributeNS(namespaceURI, 
                                                           pfx + ":" + IS_REFERENCE_PARAM_ATTR_NAME);
        isRefParamAttr.setTextContent("1");
        lastAdded.setAttributeNodeNS(isRefParamAttr);
    }

    private void addMustUnderstandAttribute(Element header,
                                            QName name,
                                            SoapMessage msg,
                                            AddressingPropertiesImpl maps) {
        if (maps.getMustUnderstand().contains(name)) {
            Element lastAdded = (Element)header.getLastChild();
            String pfx = msg.getVersion().getPrefix();
            if (msg.hasAdditionalEnvNs()) {
                String ns = msg.getVersion().getNamespace();
                Map<String, String> nsMap = msg.getEnvelopeNs();
                for (Map.Entry<String, String> entry : nsMap.entrySet()) {
                    if (ns.equals(entry.getValue())) {
                        pfx = entry.getKey();
                    }
                }
            }
            
            Attr mustUnderstandAttr = 
                lastAdded.getOwnerDocument().createAttributeNS(
                    msg.getVersion().getNamespace(),
                    pfx + ":mustUnderstand");
            mustUnderstandAttr.setTextContent("1");
            lastAdded.setAttributeNodeNS(mustUnderstandAttr);
        }
    }
    
    /**
     * Encode message in exposed version.
     * 
     * @param maps the MAPs, where getNamespceURI() specifies the WS-Addressing
     *  version to expose
     * @param value the value to encode
     * @param name the QName for the header 
     * @param clz the class
     * @param header the SOAP header element
     * @param marshaller the JAXB marshaller to use
     */
    private <T> void encodeAsExposed(AddressingPropertiesImpl maps,
                                     SoapMessage message,
                                     T value,
                                     QName name,
                                     Class<T> clz,
                                     Element header,
                                     Marshaller marshaller) throws JAXBException {
        if (value != null) {
            LOG.log(Level.FINE,
                    "{0} : {1}",
                    new Object[] {name.getLocalPart(), getLogText(value)});
            transformer.encodeAsExposed(maps.getNamespaceURI(),
                                        value,
                                        name.getLocalPart(),
                                        clz,
                                        header,
                                        marshaller);
        }
        addMustUnderstandAttribute(header,
                                   name,
                                   message,
                                   maps);
    }
    
    /**
     * Decode the MAPs from protocol-specific headers.
     *  
     * @param message the SOAP message
     * @param the decoded MAPs
     * @exception SOAPFaultException if decoded MAPs are invalid 
     */
    public AddressingProperties unmarshalMAPs(SoapMessage message) {
        // REVISIT generate MessageAddressingHeaderRequired fault if an
        // expected header is missing 
        AddressingPropertiesImpl maps = null;
        try {
            List<Header> header = message.getHeaders();
            if (header != null) {
                LOG.log(Level.FINE, "Inbound WS-Addressing headers");
                Unmarshaller unmarshaller = null;
                Set<Element> referenceParameterHeaders = null;

                Iterator<Header> iter = header.iterator();
                while (iter.hasNext()) {
                    Header hdr = iter.next();
                    if (hdr.getObject() instanceof Element) {
                        Element headerElement = (Element)hdr.getObject();
                        String headerURI = headerElement.getNamespaceURI();
                        // Need to check the uri before getting unmarshaller else
                        // would get wrong unmarshaller and fail to process required
                        // headers.
                        if (transformer.isSupported(headerURI)) {
                            if (unmarshaller == null) {
                                JAXBContext jaxbContext = 
                                    VersionTransformer.getExposedJAXBContext(headerURI);
                                unmarshaller = 
                                    jaxbContext.createUnmarshaller();
                            }
                            if (maps == null) {
                                maps = new AddressingPropertiesImpl();
                                maps.exposeAs(headerURI);
                            }
                            String localName = headerElement.getLocalName();
                            if (Names.WSA_MESSAGEID_NAME.equals(localName)) {
                                maps.setMessageID(decodeAsNative(
                                                       headerURI,
                                                       AttributedURIType.class,
                                                       headerElement, 
                                                       unmarshaller));
                            } else if (Names.WSA_TO_NAME.equals(localName)) {
                                AttributedURIType addr = decodeAsNative(
                                                       headerURI,
                                                       AttributedURIType.class,
                                                       headerElement, 
                                                       unmarshaller);
                                maps.setTo(EndpointReferenceUtils.getEndpointReference(addr));
                            } else if (Names.WSA_FROM_NAME.equals(localName)) {
                                maps.setFrom(decodeAsNative(
                                                       headerURI,
                                                       EndpointReferenceType.class,
                                                       headerElement, 
                                                       unmarshaller));
                            } else if (Names.WSA_REPLYTO_NAME.equals(localName)) {
                                maps.setReplyTo(decodeAsNative(
                                                       headerURI,
                                                       EndpointReferenceType.class,
                                                       headerElement, 
                                                       unmarshaller));
                            } else if (Names.WSA_FAULTTO_NAME.equals(localName)) {
                                maps.setFaultTo(decodeAsNative(
                                                       headerURI,
                                                       EndpointReferenceType.class,
                                                       headerElement, 
                                                       unmarshaller));
                            } else if (Names.WSA_RELATESTO_NAME.equals(localName)) {
                                maps.setRelatesTo(decodeAsNative(
                                                       headerURI,
                                                       RelatesToType.class,
                                                       headerElement, 
                                                       unmarshaller));
                            } else if (Names.WSA_ACTION_NAME.equals(localName)) {
                                maps.setAction(decodeAsNative(
                                                       headerURI,
                                                       AttributedURIType.class,
                                                       headerElement, 
                                                       unmarshaller));
                            }
                        } else if (null != headerElement.getAttribute(IS_REFERENCE_PARAM_ATTR_NAME)) {
                            if (null == referenceParameterHeaders) {
                                referenceParameterHeaders = new HashSet<Element>();
                            }
                            referenceParameterHeaders.add(headerElement); 
                        } else if (headerURI.contains(Names.WSA_NAMESPACE_PATTERN)) {
                            LOG.log(Level.WARNING, 
                                    "UNSUPPORTED_VERSION_MSG",
                                    headerURI);
                        }
                    }
                }
                if (null != referenceParameterHeaders && null != maps) {
                    decodeReferenceParameters(referenceParameterHeaders, maps, unmarshaller);
                }
            }
        } catch (JAXBException je) {
            LOG.log(Level.WARNING, "SOAP_HEADER_DECODE_FAILURE_MSG", je); 
        }
        return maps;
    }
    
    private void decodeReferenceParameters(Set<Element> referenceParameterHeaders, 
                                           AddressingPropertiesImpl maps, 
                                           Unmarshaller unmarshaller) 
        throws JAXBException {
        EndpointReferenceType toEpr = maps.getToEndpointReference();
        if (null != toEpr) {
            for (Element e : referenceParameterHeaders) {
                JAXBElement<String> el = unmarshaller.unmarshal(e, String.class);
                ContextUtils.applyReferenceParam(toEpr, el);
            }
        }
    }

    /**
     * Decodes a MAP from a exposed version.
     *
     * @param encodedAs specifies the encoded version
     * @param clz the class
     * @param headerElement the SOAP header element
     * @param marshaller the JAXB marshaller to use
     * @return the decoded value
     */
    public <T> T decodeAsNative(String encodedAs,
                                Class<T> clz,
                                Element headerElement,
                                Unmarshaller unmarshaller) 
        throws JAXBException {
        T value = clz.cast(transformer.decodeAsNative(encodedAs,
                                              clz,
                                              headerElement,
                                              unmarshaller));
        LOG.log(Level.FINE,
                "{0} : {1}",
                new Object[] {headerElement.getLocalName(), getLogText(value)});
        return value;
    }
    
    /**
     * Return a text representation of a header value for logging.
     * 
     * @param <T> header type
     * @param value header value
     * @return
     */
    private <T> String getLogText(T value) {
        String text = "unknown";
        if (value == null) {
            text = "null";
        } else if (value instanceof AttributedURIType) {
            text = ((AttributedURIType)value).getValue();
        } else if (value instanceof EndpointReferenceType) {
            text = ((EndpointReferenceType)value).getAddress() != null
                   ? ((EndpointReferenceType)value).getAddress().getValue()
                   : "null";
        } else if (value instanceof RelatesToType) {
            text = ((RelatesToType)value).getValue();
        }
        return text;
    }

    
    /**
     * Decode the MAPs from protocol-specific headers.
     *  
     * @param message the messsage
     * @param the decoded MAPs
     * @exception SOAPFaultException if decoded MAPs are invalid 
     */
    private AddressingProperties decode(SoapMessage message) {
        // REVISIT generate MessageAddressingHeaderRequired fault if an
        // expected header is missing 
        return unmarshalMAPs(message);
    }

    /**
     * Encodes an MAP as a SOAP header.
     *
     * @param value the value to encode
     * @param qname the QName for the header 
     * @param clz the class
     * @param header the SOAP header element
     * @param marshaller the JAXB marshaller to use
     */
    protected <T> void encodeMAP(T value,
                                 QName qname,
                                 Class<T> clz,
                                 Element header,
                                 Marshaller marshaller) throws JAXBException {
        if (value != null) {
            marshaller.marshal(new JAXBElement<T>(qname, clz, value), header);
        }
    }

    /**
     * Decodes a MAP from a SOAP header.
     *
     * @param clz the class
     * @param headerElement the SOAP header element
     * @param marshaller the JAXB marshaller to use
     * @return the decoded value
     */
    protected <T> T decodeMAP(Class<T> clz,
                              Element headerElement,
                              Unmarshaller unmarshaller) throws JAXBException {
        JAXBElement<T> element =
            unmarshaller.unmarshal(headerElement, clz);
        return element.getValue();
    }

    /**
     * Discard any pre-existing MAP headers - this may occur if the runtime
     * re-uses a SOAP message.
     *
     * @param header the SOAP header
     * @param maps the current MAPs
     */
    private void discardMAPs(List<Header> header, AddressingProperties maps) {
        Iterator<Header> iter = header.iterator();
        while (iter.hasNext()) {
            Header hdr = iter.next();
            if (Names.WSA_NAMESPACE_NAME.equals(hdr.getName().getNamespaceURI())) {
                iter.remove();
            }
        }
    }

    /**
     * Propogate action to SOAPAction header
     *
     * @param action the Action property
     * @param message the SOAP message
     */
    private void propogateAction(AttributedURIType action, 
                                 SoapMessage message) {
        if (!(action == null || "".equals(action.getValue()))) {
            Map<String, List<String>> mimeHeaders = CastUtils.cast((Map<?, ?>)
                message.get(Message.MIME_HEADERS));
            if (mimeHeaders != null) {
                List<String> soapActionHeaders =
                    mimeHeaders.get(Names.SOAP_ACTION_HEADER);
                // only propogate to SOAPAction header if currently non-empty
                if (!(soapActionHeaders == null
                      || soapActionHeaders.size() == 0
                      || "".equals(soapActionHeaders.get(0)))) {
                    LOG.log(Level.FINE, 
                            "encoding wsa:Action in SOAPAction header {0}",
                            action.getValue());
                    soapActionHeaders.clear();
                    soapActionHeaders.add("\"" + action.getValue() + "\"");
                }
            }
        }
    }

    /**
     * Apply results of validation of incoming MAPs.
     *
     * @param message the message
     * @exception SOAPFaultException if the MAPs are invalid
     * @exception SOAPException if SOAPFault cannot be constructed
     */
    private void applyMAPValidation(SoapMessage message) {
        String faultName = ContextUtils.retrieveMAPFaultName(message);
        if (faultName != null) {
            String reason = ContextUtils.retrieveMAPFaultReason(message);
            throw createSOAPFaut(faultName, 
                                           Names.WSA_NAMESPACE_NAME,
                                           reason);
        }
    }

    /**
     * Create a SoapFault.
     *
     * @param localName the fault local name
     * @param prefix the fault prefix
     * @param namespace the fault namespace
     * @param reason the fault reason
     * @return a new SoapFault
     */ 
    private SoapFault createSOAPFaut(String localName, String namespace, String reason) {
        return new SoapFault(reason, new QName(namespace, localName));
    }
    
    /**
     * Cache exchange for correlated response
     * 
     * @param message the current message
     * @param maps the addressing properties
     */
    private void cacheExchange(SoapMessage message, AddressingProperties maps) {
        if (ContextUtils.isRequestor(message) && !message.getExchange().isOneWay()) {
            uncorrelatedExchanges.put(maps.getMessageID().getValue(),
                                      message.getExchange());
        }
    }
    
    /**
     * Restore exchange for correlated response
     * 
     * @param message the current message
     * @param maps the addressing properties
     */
    private void restoreExchange(SoapMessage message, AddressingProperties maps) {
        if (maps != null
            && maps.getRelatesTo() != null
            && isRelationshipReply(maps.getRelatesTo())) { 
            Exchange correlatedExchange =
                uncorrelatedExchanges.remove(maps.getRelatesTo().getValue());
            if (correlatedExchange != null) {
                synchronized (correlatedExchange) {
                    message.setExchange(correlatedExchange);
                }
            } else {
                if (ContextUtils.retrieveDeferUncorrelatedMessageAbort(message)) {
                    LOG.fine("deferring uncorrelated message abort");
                    ContextUtils.storeDeferredUncorrelatedMessageAbort(message);
                } else {
                    LOG.log(Level.WARNING, "CORRELATION_FAILURE_MSG");
                    message.getInterceptorChain().abort();
                }
            }
        } else if (maps == null && isRequestor(message)) {
            Message m = message.getExchange().getOutMessage();
            maps = ContextUtils.retrieveMAPs(m, false, true, false);
            if (maps != null) {
                Exchange ex = uncorrelatedExchanges.get(maps.getMessageID().getValue());
                if (ex == message.getExchange()) {
                    uncorrelatedExchanges.remove(maps.getMessageID().getValue());
                    LOG.log(Level.WARNING, "RESPONSE_NOT_USING_WSADDRESSING");
                }
            }
        }
        
    }

    /** 
     * @param relatesTo the current RelatesTo
     * @return true iff the relationship type is reply
     */
    private boolean isRelationshipReply(RelatesToType relatesTo) {
        return Names.WSA_RELATIONSHIP_REPLY.equals(relatesTo.getRelationshipType());
    }
 
    /**
     * Marks a message as partial response
     * 
     * @param message the current message
     */
    private void markPartialResponse(SoapMessage message, AddressingProperties maps) {
        if (ContextUtils.isRequestor(message) && null != maps && null == maps.getRelatesTo()) {
            message.put(Message.PARTIAL_RESPONSE_MESSAGE, Boolean.TRUE);
        } 
    }
    
    protected HeaderFactory getHeaderFactory() {
        if (headerFactory == null) {
            headerFactory = new HeaderFactory() {
                public Element getHeader(SoapVersion soapversion) {
                    Document doc = DOMUtils.createDocument();
                    return doc.createElementNS(soapversion.getHeader().getNamespaceURI(),
                            soapversion.getHeader().getLocalPart());
                }
            };
        }
        return headerFactory;
    }
    
    protected void setHeaderFactory(HeaderFactory factory) {
        headerFactory = factory;
    }

    public interface HeaderFactory {
        Element getHeader(SoapVersion soapversion);
    }
}







