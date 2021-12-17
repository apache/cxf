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

import javax.xml.namespace.QName;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.rm.v200702.AckRequestedType;
import org.apache.cxf.ws.rm.v200702.CloseSequenceType;
import org.apache.cxf.ws.rm.v200702.CreateSequenceResponseType;
import org.apache.cxf.ws.rm.v200702.CreateSequenceType;
import org.apache.cxf.ws.rm.v200702.DetailType;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.v200702.SequenceFaultType;
import org.apache.cxf.ws.rm.v200702.SequenceType;
import org.apache.cxf.ws.rm.v200702.TerminateSequenceType;

/**
 * WS-ReliableMessaging 1.1/1.2 encoding and decoding. This just works with the standard internal form of the
 * WS-RM data structures.
 */
public final class EncoderDecoder11Impl extends EncoderDecoder {

    public static final EncoderDecoder11Impl INSTANCE = new EncoderDecoder11Impl();

    private static AtomicReference<JAXBContext> jaxbContextReference = new AtomicReference<>();

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

    public Class<?> getCreateSequenceType() {
        return org.apache.cxf.ws.rm.v200702.CreateSequenceType.class;
    }

    public Class<?> getCreateSequenceResponseType() {
        return org.apache.cxf.ws.rm.v200702.CreateSequenceResponseType.class;
    }

    public Class<?> getTerminateSequenceType() {
        return org.apache.cxf.ws.rm.v200702.TerminateSequenceType.class;
    }

    public Class<?> getTerminateSequenceResponseType() {
        return org.apache.cxf.ws.rm.v200702.TerminateSequenceResponseType.class;
    }

    protected JAXBContext getContext() throws JAXBException {
        JAXBContext jaxbContext = jaxbContextReference.get();
        if (jaxbContext == null) {
            synchronized (EncoderDecoder11Impl.class) {
                jaxbContext = jaxbContextReference.get();
                if (jaxbContext == null) {
                    Class<?> clas = RMUtils.getWSRMFactory().getClass();
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
            JAXBElement<SequenceType> element = RMUtils.getWSRMFactory().createSequence(seq);
            headers.add(new SoapHeader(element.getName(), element, getDataBinding(), true));
        }
        if (null != acks) {
            LOG.log(Level.FINE, "encoding sequence acknowledgement(s) into RM header");
            for (SequenceAcknowledgement ack : acks) {
                headers.add(new SoapHeader(new QName(getConstants().getWSRMNamespace(), 
                                                     RMConstants.SEQUENCE_ACK_NAME),
                                           ack, getDataBinding()));
            }
        }
        if (null != reqs) {
            LOG.log(Level.FINE, "encoding acknowledgement request(s) into RM header");
            for (AckRequestedType req : reqs) {
                headers.add(new SoapHeader(new QName(getConstants().getWSRMNamespace(), 
                                                     RMConstants.ACK_REQUESTED_NAME),
                                           RMUtils.getWSRMFactory().createAckRequested(req),
                                           getDataBinding()));
            }
        }
    }

    @Override
    protected Object buildHeaderFaultObject(SequenceFault sf) {
        SequenceFaultType flt = new SequenceFaultType();
        flt.setFaultCode(sf.getFaultCode());
        Object detail = sf.getDetail();
        flt.getAny().add(detail);
        Element data = sf.getExtraDetail();
        if (data != null) {
            addDetail(flt, data);
        }
        return flt;
    }

    private static void addDetail(SequenceFaultType sft, Element data) {
        if (!sft.isSetDetail()) {
            sft.setDetail(new DetailType());
        }
        sft.getDetail().getAny().add(data);
    }

    public Element encodeSequenceAcknowledgement(SequenceAcknowledgement ack) throws JAXBException {
        DocumentFragment doc = DOMUtils.getEmptyDocument().createDocumentFragment();
        Marshaller marshaller = getContext().createMarshaller();
        marshaller.marshal(ack, doc);
        return (Element)doc.getFirstChild();
    }

    public Element encodeIdentifier(Identifier id) throws JAXBException {
        DocumentFragment doc = DOMUtils.getEmptyDocument().createDocumentFragment();
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