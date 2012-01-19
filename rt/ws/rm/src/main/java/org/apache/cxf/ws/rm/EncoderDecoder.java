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

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.ws.rm.v200702.AckRequestedType;
import org.apache.cxf.ws.rm.v200702.CloseSequenceType;
import org.apache.cxf.ws.rm.v200702.CreateSequenceResponseType;
import org.apache.cxf.ws.rm.v200702.CreateSequenceType;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.v200702.SequenceType;
import org.apache.cxf.ws.rm.v200702.TerminateSequenceType;

/**
 * Interface for converting WS-ReliableMessaging structures to and from XML. Implementations of this interface
 * provide version-specific encoding and decoding.
 */
public interface EncoderDecoder {
    
    /**
     * Get the WS-ReliableMessaging namespace used by this encoder/decoder.
     * 
     * @return URI
     */
    String getWSRMNamespace();
    
    /**
     * Get the WS-Addressing namespace used by this encoder/decoder.
     * 
     * @return URI
     */
    String getWSANamespace();
    
    /**
     * Get the WS-ReliableMessaging constants used by this encoder/decoder.
     * 
     * @return
     */
    RMConstants getConstants();
    
    /**
     * Get the class used for the CreateSequenceType.
     * 
     * @return class
     */
    Class getCreateSequenceType();
    
    /**
     * Get the class used for the CreateSequenceResponseType.
     * 
     * @return class
     */
    Class getCreateSequenceResponseType();
    
    /**
     * Get the class used for the TerminateSequenceType.
     * 
     * @return class
     */
    Class getTerminateSequenceType();
    
    /**
     * Builds an element containing WS-RM headers. This adds the appropriate WS-RM and WS-A namespace
     * declarations to the element, and then adds any WS-RM headers set in the supplied properties as child
     * elements.
     * 
     * @param rmps
     * @param qname constructed element name
     * @return element
     */
    Element buildHeaders(RMProperties rmps, QName qname) throws JAXBException;
    
    /**
     * Builds an element containing a WS-RM Fault. This adds the appropriate WS-RM namespace declaration to
     * the element, and then adds the Fault as a child element.
     * 
     * @param sf
     * @param qname constructed element name
     * @return element
     */
    Element buildHeaderFault(SequenceFault sf, QName qname) throws JAXBException;
    
    /**
     * Marshals a SequenceAcknowledgement to the appropriate external form.
     * 
     * @param ack
     * @return element
     * @throws JAXBException
     */
    Element encodeSequenceAcknowledgement(SequenceAcknowledgement ack) throws JAXBException;
    
    /**
     * Marshals an Identifier to the appropriate external form.
     * 
     * @param id
     * @return element
     * @throws JAXBException
     */
    Element encodeIdentifier(Identifier id) throws JAXBException;
    
    /**
     * Unmarshals a SequenceType, converting it if necessary to the internal form.
     * 
     * @param elem
     * @return
     * @throws JAXBException
     */
    SequenceType decodeSequenceType(Element elem) throws JAXBException;
    
    /**
     * Generates a CloseSequenceType if a SequenceType represents a last message state.
     * 
     * @param elem
     * @return CloseSequenceType if last message state, else <code>null</code>
     * @throws JAXBException
     */
    CloseSequenceType decodeSequenceTypeCloseSequence(Element elem) throws JAXBException;
    
    /**
     * Unmarshals a SequenceAcknowledgement, converting it if necessary to the internal form.
     * 
     * @param elem
     * @return
     * @throws JAXBException
     */
    SequenceAcknowledgement decodeSequenceAcknowledgement(Element elem) throws JAXBException;
    
    /**
     * Unmarshals a AckRequestedType, converting it if necessary to the internal form.
     * 
     * @param elem
     * @return
     * @throws JAXBException
     */
    AckRequestedType decodeAckRequestedType(Element elem) throws JAXBException;
    
    /**
     * Convert a CreateSequence message to the correct format for transmission.
     * 
     * @param create
     * @return converted
     */
    Object convertToSend(CreateSequenceType create);
    
    /**
     * Convert a CreateSequenceResponse message to the correct format for transmission.
     * 
     * @param create
     * @return converted
     */
    Object convertToSend(CreateSequenceResponseType create);
    
    /**
     * Convert a TerminateSequence message to the correct format for transmission.
     * 
     * @param term
     * @return converted
     */
    Object convertToSend(TerminateSequenceType term);
    
    /**
     * Convert a received TerminateSequence message to internal form.
     * 
     * @param term
     * @return converted
     */
    TerminateSequenceType convertReceivedTerminateSequence(Object term);
    
    /**
     * Convert a received CreateSequence message to internal form.
     * 
     * @param create
     * @return converted
     */
    CreateSequenceType convertReceivedCreateSequence(Object create);
    
    /**
     * Convert a received CreateSequenceResponse message to internal form.
     * 
     * @param create
     * @return converted
     */
    CreateSequenceResponseType convertReceivedCreateSequenceResponse(Object create);
}