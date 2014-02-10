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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.rm.v200702.AckRequestedType;
import org.apache.cxf.ws.rm.v200702.CloseSequenceType;
import org.apache.cxf.ws.rm.v200702.CreateSequenceResponseType;
import org.apache.cxf.ws.rm.v200702.CreateSequenceType;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.v200702.SequenceType;
import org.apache.cxf.ws.rm.v200702.TerminateSequenceType;

/**
 * Base class for converting WS-ReliableMessaging structures to and from XML. Subclasses provide version-specific
 * encoding and decoding.
 */
public abstract class EncoderDecoder {

    /**
     * Get context for JAXB marshalling/unmarshalling.
     * 
     * @return context
     * @throws JAXBException
     */
    protected abstract JAXBContext getContext() throws JAXBException;
    
    /**
     * Get the WS-ReliableMessaging namespace used by this encoder/decoder.
     * 
     * @return URI
     */
    public abstract String getWSRMNamespace();
    
    /**
     * Get the WS-Addressing namespace used by this encoder/decoder.
     * 
     * @return URI
     */
    public abstract String getWSANamespace();
    
    /**
     * Get the WS-ReliableMessaging constants used by this encoder/decoder.
     * 
     * @return
     */
    public abstract RMConstants getConstants();
    
    /**
     * Get the class used for the CreateSequenceType.
     * 
     * @return class
     */
    public abstract Class<?> getCreateSequenceType();
    
    /**
     * Get the class used for the CreateSequenceResponseType.
     * 
     * @return class
     */
    public abstract Class<?> getCreateSequenceResponseType();
    
    /**
     * Get the class used for the TerminateSequenceType.
     * 
     * @return class
     */
    public abstract Class<?> getTerminateSequenceType();
    
    /**
     * Get the class used for the TerminateSequenceResponseType.
     * 
     * @return class
     */
    public abstract Class<?> getTerminateSequenceResponseType();
    
    /**
     * Builds an element containing WS-RM headers. This adds the appropriate WS-RM namespace declaration to the element,
     * and then adds any WS-RM headers set in the supplied properties as child elements.
     * 
     * @param rmps
     * @param qname constructed element name
     * @return element (<code>null</code> if none)
     */
    public Element buildHeaders(RMProperties rmps, QName qname) throws JAXBException {
        
        // check if there's anything to insert
        SequenceType seq = rmps.getSequence();
        Collection<SequenceAcknowledgement> acks = rmps.getAcks();
        Collection<AckRequestedType> reqs = rmps.getAcksRequested();
        if (seq == null && acks == null && reqs == null) {
            return null;
        }
        
        // create element with namespace declaration included
        Document doc = DOMUtils.createDocument();
        Element header = doc.createElementNS(qname.getNamespaceURI(), qname.getLocalPart());
        addNamespaceDecl(header);
        
        // build individual headers
        Marshaller marshaller = getContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        buildHeaders(seq, acks, reqs, rmps.isLastMessage(), header, marshaller);
        return header;
    }
    
    /**
     * Add WS-RM namespace declaration to element.
     * 
     * @param element
     */
    protected void addNamespaceDecl(Element element) {
        Attr attr = element.getOwnerDocument().createAttributeNS("http://www.w3.org/2000/xmlns/", 
            "xmlns:" + RMConstants.NAMESPACE_PREFIX);
        attr.setValue(getWSRMNamespace());
        element.setAttributeNodeNS(attr);
    }
    
    /**
     * Builds an element containing a WS-RM Fault. This adds the appropriate WS-RM namespace declaration to
     * the element, and then adds the Fault as a child element.
     * 
     * @param sf
     * @param qname constructed element name
     * @return element
     */
    public Element buildHeaderFault(SequenceFault sf, QName qname) throws JAXBException {
        
        // create element with namespace declaration included
        Document doc = DOMUtils.createDocument();
        Element header = doc.createElementNS(qname.getNamespaceURI(), qname.getLocalPart());
        addNamespaceDecl(header);
        
        // insert the actual fault
        Marshaller marshaller = getContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        buildHeaderFault(sf, header, marshaller);
        return header;
    }

    /**
     * Build all required headers, using the correct protocol variation.
     * 
     * @param seq
     * @param acks
     * @param reqs
     * @param last
     * @param header
     * @param marshaller
     * @throws JAXBException
     */
    protected abstract void buildHeaders(SequenceType seq, Collection<SequenceAcknowledgement> acks,
        Collection<AckRequestedType> reqs, boolean last, Element header, Marshaller marshaller) throws JAXBException;

    /**
     * Build a header fault, using the correct protocol variation.
     * 
     * @param sf
     * @param header
     * @param marshaller
     * @throws JAXBException
     */
    protected abstract void buildHeaderFault(SequenceFault sf, Element header, Marshaller marshaller)
        throws JAXBException;
    
    /**
     * Marshals a SequenceAcknowledgement to the appropriate external form.
     * 
     * @param ack
     * @return element
     * @throws JAXBException
     */
    public abstract Element encodeSequenceAcknowledgement(SequenceAcknowledgement ack) throws JAXBException;
    
    /**
     * Marshals an Identifier to the appropriate external form.
     * 
     * @param id
     * @return element
     * @throws JAXBException
     */
    public abstract Element encodeIdentifier(Identifier id) throws JAXBException;
    
    /**
     * Unmarshals a SequenceType, converting it if necessary to the internal form.
     * 
     * @param elem
     * @return
     * @throws JAXBException
     */
    public abstract SequenceType decodeSequenceType(Element elem) throws JAXBException;
    
    /**
     * Generates a CloseSequenceType if a SequenceType represents a last message state.
     * 
     * @param elem
     * @return CloseSequenceType if last message state, else <code>null</code>
     * @throws JAXBException
     */
    public abstract CloseSequenceType decodeSequenceTypeCloseSequence(Element elem) throws JAXBException;
    
    /**
     * Unmarshals a SequenceAcknowledgement, converting it if necessary to the internal form.
     * 
     * @param elem
     * @return
     * @throws JAXBException
     */
    public abstract SequenceAcknowledgement decodeSequenceAcknowledgement(Element elem) throws JAXBException;
    
    /**
     * Unmarshals a AckRequestedType, converting it if necessary to the internal form.
     * 
     * @param elem
     * @return
     * @throws JAXBException
     */
    public abstract AckRequestedType decodeAckRequestedType(Element elem) throws JAXBException;
    
    /**
     * Convert a CreateSequence message to the correct format for transmission.
     * 
     * @param create
     * @return converted
     */
    public abstract Object convertToSend(CreateSequenceType create);
    
    /**
     * Convert a CreateSequenceResponse message to the correct format for transmission.
     * 
     * @param create
     * @return converted
     */
    public abstract Object convertToSend(CreateSequenceResponseType create);
    
    /**
     * Convert a TerminateSequence message to the correct format for transmission.
     * 
     * @param term
     * @return converted
     */
    public abstract Object convertToSend(TerminateSequenceType term);
    
    /**
     * Convert a received TerminateSequence message to internal form.
     * 
     * @param term
     * @return converted
     */
    public abstract TerminateSequenceType convertReceivedTerminateSequence(Object term);
    
    /**
     * Convert a received CreateSequence message to internal form.
     * 
     * @param create
     * @return converted
     */
    public abstract CreateSequenceType convertReceivedCreateSequence(Object create);
    
    /**
     * Convert a received CreateSequenceResponse message to internal form.
     * 
     * @param create
     * @return converted
     */
    public abstract CreateSequenceResponseType convertReceivedCreateSequenceResponse(Object create);
}