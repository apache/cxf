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
import org.apache.cxf.ws.addressing.VersionTransformer.Names200408;
import org.apache.cxf.ws.rm.v200702.AckRequestedType;
import org.apache.cxf.ws.rm.v200702.CloseSequenceType;
import org.apache.cxf.ws.rm.v200702.CreateSequenceResponseType;
import org.apache.cxf.ws.rm.v200702.CreateSequenceType;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.v200702.SequenceType;
import org.apache.cxf.ws.rm.v200702.TerminateSequenceType;

/**
 * WS-ReliableMessaging 1.0 encoding and decoding. This converts between the standard WS-RM objects and the
 * 1.0 representation using the WS-Addressing 200408 namespace specified in the WS-RM 1.0 recommendation.
 */
public final class EncoderDecoder10Impl implements EncoderDecoder {
    
    public static final EncoderDecoder10Impl INSTANCE = new EncoderDecoder10Impl();

    private static JAXBContext jaxbContext;

    private static final Logger LOG = LogUtils.getL7dLogger(EncoderDecoder10Impl.class);
    
    private EncoderDecoder10Impl() {
    }
    
    public String getWSRMNamespace() {
        return RM10Constants.NAMESPACE_URI;
    }

    public String getWSANamespace() {
        return Names200408.WSA_NAMESPACE_NAME;
    }

    public RMConstants getConstants() {
        return RM10Constants.INSTANCE;
    }

    public Class getCreateSequenceType() {
        return org.apache.cxf.ws.rm.v200502.CreateSequenceType.class;
    }

    public Class getCreateSequenceResponseType() {
        return org.apache.cxf.ws.rm.v200502.CreateSequenceResponseType.class;
    }

    public Class getTerminateSequenceType() {
        return org.apache.cxf.ws.rm.v200502.TerminateSequenceType.class;
    }

    private static JAXBContext getContext() throws JAXBException {
        synchronized (EncoderDecoder10Impl.class) {
            if (jaxbContext == null) {
                Class clas = RMUtils.getWSRM200502Factory().getClass();
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
            org.apache.cxf.ws.rm.v200502.SequenceType toseq = VersionTransformer.convert200502(seq);
            if (rmps.isLastMessage()) {
                toseq.setLastMessage(new org.apache.cxf.ws.rm.v200502.SequenceType.LastMessage());
            }
            JAXBElement element = RMUtils.getWSRM200502Factory().createSequence(toseq);
            marshaller.marshal(element, header);
        } 
        Collection<SequenceAcknowledgement> acks = rmps.getAcks();
        if (null != acks) {
            LOG.log(Level.FINE, "encoding sequence acknowledgement(s) into RM header");
            for (SequenceAcknowledgement ack : acks) {
                marshaller.marshal(VersionTransformer.convert200502(ack), header);
            }
        }
        Collection<AckRequestedType> reqs = rmps.getAcksRequested();
        if (null != reqs) {
            LOG.log(Level.FINE, "encoding acknowledgement request(s) into RM header");
            for (AckRequestedType req : reqs) {
                marshaller.marshal(RMUtils.getWSRM200502Factory()
                    .createAckRequested(VersionTransformer.convert200502(req)), header);
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
        attr.setValue(RM10Constants.NAMESPACE_URI);
        header.setAttributeNodeNS(attr);

        Marshaller marshaller = getContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        QName fqname = RM10Constants.SEQUENCE_FAULT_QNAME;
        org.apache.cxf.ws.rm.v200502.SequenceFaultType flt =
            new org.apache.cxf.ws.rm.v200502.SequenceFaultType();
        flt.setFaultCode(sf.getFaultCode());
        Object detail = sf.getDetail();
        if (detail instanceof Element) {
            flt.getAny().add(detail);
        } else if (detail instanceof Identifier) {
            marshaller.marshal(VersionTransformer.convert200502((Identifier)detail), doc);
        } else if (detail instanceof SequenceAcknowledgement) {
            marshaller.marshal(VersionTransformer.convert200502((SequenceAcknowledgement)detail), doc);
        }
        Element data = doc.getDocumentElement();
        if (data != null) {
            flt.getAny().add(data);
        }
        marshaller.marshal(new JAXBElement<org.apache.cxf.ws.rm.v200502.SequenceFaultType>(fqname,
            org.apache.cxf.ws.rm.v200502.SequenceFaultType.class, flt), header);
        return header;
    }

    public Element encodeSequenceAcknowledgement(SequenceAcknowledgement ack) throws JAXBException {
        Document doc = DOMUtils.createDocument();
        Marshaller marshaller = getContext().createMarshaller();
        marshaller.marshal(VersionTransformer.convert200502(ack), doc);
        return (Element)doc.getFirstChild();
    }

    public Element encodeIdentifier(Identifier id) throws JAXBException {
        Document doc = DOMUtils.createDocument();
        Marshaller marshaller = getContext().createMarshaller();
        marshaller.marshal(VersionTransformer.convert200502(id), doc);
        return (Element)doc.getFirstChild();
    }

    public SequenceType decodeSequenceType(Element elem) throws JAXBException {
        Unmarshaller unmarshaller = getContext().createUnmarshaller();
        JAXBElement<org.apache.cxf.ws.rm.v200502.SequenceType> jaxbElement
            = unmarshaller.unmarshal(elem, org.apache.cxf.ws.rm.v200502.SequenceType.class);
        return VersionTransformer.convert(jaxbElement.getValue());
    }
    
    public CloseSequenceType decodeSequenceTypeCloseSequence(Element elem) throws JAXBException {
        Unmarshaller unmarshaller = getContext().createUnmarshaller();
        JAXBElement<org.apache.cxf.ws.rm.v200502.SequenceType> jaxbElement
            = unmarshaller.unmarshal(elem, org.apache.cxf.ws.rm.v200502.SequenceType.class);
        org.apache.cxf.ws.rm.v200502.SequenceType seq = jaxbElement.getValue();
        if (seq.isSetLastMessage()) {
            CloseSequenceType close = new CloseSequenceType();
            close.setIdentifier(VersionTransformer.convert(seq.getIdentifier()));
            close.setLastMsgNumber(seq.getMessageNumber());
            return close;
        } else {
            return null;
        }
    }

    public SequenceAcknowledgement decodeSequenceAcknowledgement(Element elem) throws JAXBException {
        Unmarshaller unmarshaller = getContext().createUnmarshaller();
        org.apache.cxf.ws.rm.v200502.SequenceAcknowledgement ack
            = (org.apache.cxf.ws.rm.v200502.SequenceAcknowledgement)unmarshaller.unmarshal(elem);
        return VersionTransformer.convert(ack);
    }

    public AckRequestedType decodeAckRequestedType(Element elem) throws JAXBException {
        Unmarshaller unmarshaller = getContext().createUnmarshaller();
        JAXBElement<org.apache.cxf.ws.rm.v200502.AckRequestedType> jaxbElement
            = unmarshaller.unmarshal(elem, org.apache.cxf.ws.rm.v200502.AckRequestedType.class);
        return VersionTransformer.convert(jaxbElement.getValue());
    }

    public Object convertToSend(CreateSequenceType create) {
        return VersionTransformer.convert200502(create);
    }

    public Object convertToSend(CreateSequenceResponseType create) {
        return VersionTransformer.convert200502(create);
    }
    
    public Object convertToSend(TerminateSequenceType term) {
        return VersionTransformer.convert200502(term);
    }

    public CreateSequenceType convertReceivedCreateSequence(Object create) {
        return VersionTransformer.convert((org.apache.cxf.ws.rm.v200502.CreateSequenceType)create);
    }

    public CreateSequenceResponseType convertReceivedCreateSequenceResponse(Object response) {
        return VersionTransformer.convert((org.apache.cxf.ws.rm.v200502.CreateSequenceResponseType)response);
    }

    public TerminateSequenceType convertReceivedTerminateSequence(Object term) {
        return VersionTransformer.convert((org.apache.cxf.ws.rm.v200502.TerminateSequenceType)term);
    }
}