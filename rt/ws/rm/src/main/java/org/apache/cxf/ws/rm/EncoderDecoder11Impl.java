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

package org.apache.cxf.ws.rm;

import java.util.Collection;
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

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.rm.v200702.AckRequestedType;
import org.apache.cxf.ws.rm.v200702.CloseSequenceType;
import org.apache.cxf.ws.rm.v200702.CreateSequenceResponseType;
import org.apache.cxf.ws.rm.v200702.CreateSequenceType;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.v200702.SequenceFaultType;
import org.apache.cxf.ws.rm.v200702.SequenceType;
import org.apache.cxf.ws.rm.v200702.TerminateSequenceType;

/**
 * WS-ReliableMessaging 1.1 encoding and decoding. This just works with the standard internal form of the
 * WS-RM data structures.
 */
public final class EncoderDecoder11Impl implements EncoderDecoder {
    
    public static final EncoderDecoder11Impl INSTANCE = new EncoderDecoder11Impl();

    private static JAXBContext jaxbContext;

    private static final Logger LOG = LogUtils.getL7dLogger(EncoderDecoder11Impl.class);
    
    private EncoderDecoder11Impl() {
    }
    
    public String getWSRMNamespace() {
        return RM11Constants.NAMESPACE_URI;
    }

    public String getWSANamespace() {
        return Names.WSA_NAMESPACE_NAME;
    }

    public RMConstants getConstants() {
        return RM11Constants.INSTANCE;
    }

    public Class getCreateSequenceType() {
        return org.apache.cxf.ws.rm.v200702.CreateSequenceType.class;
    }

    public Class getCreateSequenceResponseType() {
        return org.apache.cxf.ws.rm.v200702.CreateSequenceResponseType.class;
    }

    public Class getTerminateSequenceType() {
        return org.apache.cxf.ws.rm.v200702.TerminateSequenceType.class;
    }

    private static JAXBContext getContext() throws JAXBException {
        synchronized (EncoderDecoder11Impl.class) {
            if (jaxbContext == null) {
                Class clas = RMUtils.getWSRMFactory().getClass();
                jaxbContext = JAXBContext.newInstance(PackageUtils.getPackageName(clas),
                    clas.getClassLoader());
            }
        }
        return jaxbContext;
    }
    
    public Element buildHeaders(RMProperties rmps, QName qname) throws JAXBException {
        
        Document doc = DOMUtils.createDocument();
        Element header = doc.createElementNS(qname.getNamespaceURI(), qname.getLocalPart());
        // add WSRM namespace declaration to header, instead of
        // repeating in each individual child node
        Attr attr = doc.createAttributeNS("http://www.w3.org/2000/xmlns/", 
            "xmlns:" + RMConstants.NAMESPACE_PREFIX);
        attr.setValue(RM10Constants.NAMESPACE_URI);
        header.setAttributeNodeNS(attr);

        Marshaller marshaller = getContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
       
        SequenceType seq = rmps.getSequence();
        if (null != seq) {
            LOG.log(Level.FINE, "encoding sequence into RM header");
            JAXBElement element = RMUtils.getWSRMFactory().createSequence(seq);
            marshaller.marshal(element, header);
        } 
        Collection<SequenceAcknowledgement> acks = rmps.getAcks();
        if (null != acks) {
            LOG.log(Level.FINE, "encoding sequence acknowledgement(s) into RM header");
            for (SequenceAcknowledgement ack : acks) {
                marshaller.marshal(ack, header);
            }
        }
        Collection<AckRequestedType> reqs = rmps.getAcksRequested();
        if (null != reqs) {
            LOG.log(Level.FINE, "encoding acknowledgement request(s) into RM header");
            for (AckRequestedType req : reqs) {
                marshaller.marshal(RMUtils.getWSRMFactory().createAckRequested(req), header);
            }
        }
        return header;
    }

    public Element buildHeaderFault(SequenceFault sf, QName qname) throws JAXBException {
        
        Document doc = DOMUtils.createDocument();
        Element header = doc.createElementNS(qname.getNamespaceURI(), qname.getLocalPart());
        // add WSRM namespace declaration to header, instead of
        // repeating in each individual child node
        
        Attr attr = doc.createAttributeNS("http://www.w3.org/2000/xmlns/", 
            "xmlns:" + RMConstants.NAMESPACE_PREFIX);
        attr.setValue(RM11Constants.NAMESPACE_URI);
        header.setAttributeNodeNS(attr);

        Marshaller marshaller = getContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        QName fqname = RM11Constants.SEQUENCE_FAULT_QNAME;
        SequenceFaultType flt = new SequenceFaultType();
        flt.setFaultCode(sf.getFaultCode());
        Object detail = sf.getDetail();
        if (detail instanceof Element) {
            flt.getAny().add(detail);
        } else if (detail instanceof Identifier) {
            marshaller.marshal(detail, doc);
        } else if (detail instanceof SequenceAcknowledgement) {
            marshaller.marshal(detail, doc);
        }
        Element data = doc.getDocumentElement();
        if (data != null) {
            flt.getDetail().getAny().add(data);
        }
        data = sf.getExtraDetail();
        if (data != null) {
            flt.getDetail().getAny().add(data);
        }
        marshaller.marshal(new JAXBElement<SequenceFaultType>(fqname, SequenceFaultType.class, flt), header);
        return header;
    }

    public Element encodeSequenceAcknowledgement(SequenceAcknowledgement ack) throws JAXBException {
        Document doc = DOMUtils.createDocument();
        Marshaller marshaller = getContext().createMarshaller();
        marshaller.marshal(ack, doc);
        return (Element)doc.getFirstChild();
    }

    public Element encodeIdentifier(Identifier id) throws JAXBException {
        Document doc = DOMUtils.createDocument();
        Marshaller marshaller = getContext().createMarshaller();
        marshaller.marshal(id, doc);
        return (Element)doc.getFirstChild();
    }

    public SequenceType decodeSequenceType(Element elem) throws JAXBException {
        Unmarshaller unmarshaller = getContext().createUnmarshaller();
        JAXBElement<SequenceType> jaxbElement = unmarshaller.unmarshal(elem, SequenceType.class);
        return jaxbElement.getValue();
    }
    
    public CloseSequenceType decodeSequenceTypeCloseSequence(Element elem) throws JAXBException {
        return null;
    }

    public SequenceAcknowledgement decodeSequenceAcknowledgement(Element elem) throws JAXBException {
        Unmarshaller unmarshaller = getContext().createUnmarshaller();
        return (SequenceAcknowledgement)unmarshaller.unmarshal(elem);
    }

    public AckRequestedType decodeAckRequestedType(Element elem) throws JAXBException {
        Unmarshaller unmarshaller = getContext().createUnmarshaller();
        JAXBElement<AckRequestedType> jaxbElement = unmarshaller.unmarshal(elem, AckRequestedType.class);
        return jaxbElement.getValue();
    }

    public Object convertToSend(CreateSequenceType create) {
        return create;
    }

    public Object convertToSend(CreateSequenceResponseType create) {
        return create;
    }
    
    public Object convertToSend(TerminateSequenceType term) {
        return term;
    }

    public CreateSequenceType convertReceivedCreateSequence(Object create) {
        return (CreateSequenceType)create;
    }

    public CreateSequenceResponseType convertReceivedCreateSequenceResponse(Object create) {
        return (CreateSequenceResponseType)create;
    }

    public TerminateSequenceType convertReceivedTerminateSequence(Object term) {
        return (TerminateSequenceType)term;
    }
}