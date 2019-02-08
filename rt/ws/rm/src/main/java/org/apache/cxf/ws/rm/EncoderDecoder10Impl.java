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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.headers.Header;
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
public final class EncoderDecoder10Impl extends EncoderDecoder {

    public static final EncoderDecoder10Impl INSTANCE = new EncoderDecoder10Impl();

    private static AtomicReference<JAXBContext> jaxbContextReference = new AtomicReference<>();

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

    public Class<?> getCreateSequenceType() {
        return org.apache.cxf.ws.rm.v200502.CreateSequenceType.class;
    }

    public Class<?> getCreateSequenceResponseType() {
        return org.apache.cxf.ws.rm.v200502.CreateSequenceResponseType.class;
    }

    public Class<?> getTerminateSequenceType() {
        return org.apache.cxf.ws.rm.v200502.TerminateSequenceType.class;
    }

    public Class<?> getTerminateSequenceResponseType() {
        return null;
    }

    protected JAXBContext getContext() throws JAXBException {
        JAXBContext jaxbContext = jaxbContextReference.get();
        if (jaxbContext == null) {
            synchronized (EncoderDecoder10Impl.class) {
                jaxbContext = jaxbContextReference.get();
                if (jaxbContext == null) {
                    Class<?> clas = RMUtils.getWSRM200502Factory().getClass();
                    jaxbContext = JAXBContext.newInstance(PackageUtils.getPackageName(clas),
                                                          clas.getClassLoader());
                    jaxbContextReference.set(jaxbContext);
                }
            }
        }
        return jaxbContext;
    }

    @Override
    protected void buildHeaders(SequenceType seq, Collection<SequenceAcknowledgement> acks,
        Collection<AckRequestedType> reqs, boolean last, List<Header> headers) throws JAXBException {       
        if (null != seq) {
            LOG.log(Level.FINE, "encoding sequence into RM header");
            org.apache.cxf.ws.rm.v200502.SequenceType toseq = VersionTransformer.convert200502(seq);
            if (last) {
                toseq.setLastMessage(new org.apache.cxf.ws.rm.v200502.SequenceType.LastMessage());
            }
            JAXBElement<?> element = RMUtils.getWSRM200502Factory().createSequence(toseq);
            headers.add(new SoapHeader(element.getName(), element, getDataBinding(), true));
        }
        if (null != acks) {
            LOG.log(Level.FINE, "encoding sequence acknowledgement(s) into RM header");
            for (SequenceAcknowledgement ack : acks) {
                headers.add(new SoapHeader(new QName(getConstants().getWSRMNamespace(), 
                                                     RMConstants.SEQUENCE_ACK_NAME),
                                           VersionTransformer.convert200502(ack),
                                           getDataBinding()));
            }
        }
        if (null != reqs) {
            LOG.log(Level.FINE, "encoding acknowledgement request(s) into RM header");
            for (AckRequestedType req : reqs) {
                headers.add(new SoapHeader(new QName(getConstants().getWSRMNamespace(), 
                                                     RMConstants.ACK_REQUESTED_NAME),
                                           RMUtils.getWSRM200502Factory()
                                               .createAckRequested(VersionTransformer.convert200502(req)),
                                           getDataBinding()));
            }
        }
        
    }

    @Override
    protected Object buildHeaderFaultObject(SequenceFault sf) {
        org.apache.cxf.ws.rm.v200502.SequenceFaultType flt = new org.apache.cxf.ws.rm.v200502.SequenceFaultType();
        flt.setFaultCode(sf.getFaultCode());
        Object detail = sf.getDetail();
        if (detail instanceof Element) {
            flt.getAny().add(detail);
        } else if (detail instanceof Identifier) {
            flt.getAny().add(VersionTransformer.convert200502((Identifier)detail));
        } else if (detail instanceof SequenceAcknowledgement) {
            flt.getAny().add(VersionTransformer.convert200502((SequenceAcknowledgement)detail));
        }
        Element data = sf.getExtraDetail();
        if (data != null) {
            flt.getAny().add(data);
        }
        return flt;
    }

    public Element encodeSequenceAcknowledgement(SequenceAcknowledgement ack) throws JAXBException {
        DocumentFragment doc = DOMUtils.getEmptyDocument().createDocumentFragment();
        Marshaller marshaller = getContext().createMarshaller();
        marshaller.marshal(VersionTransformer.convert200502(ack), doc);
        return (Element)doc.getFirstChild();
    }

    public Element encodeIdentifier(Identifier id) throws JAXBException {
        DocumentFragment doc = DOMUtils.getEmptyDocument().createDocumentFragment();
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
        }
        return null;
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