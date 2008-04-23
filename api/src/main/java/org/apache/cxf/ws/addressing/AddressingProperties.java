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

package org.apache.cxf.ws.addressing;


/**
 * Abstraction of Message Addressing Properties. 
 */
public interface AddressingProperties extends AddressingType {
    /**
     * Accessor for the <b>To</b> property.
     * @return current value of To property
     */
    EndpointReferenceType getToEndpointReference();
    
    /**
     * Accessor for the <b>To</b> property.
     * @return current value of To property
     */
    AttributedURIType getTo();

    /**
     * Mutator for the <b>To</b> property.
     * @param epr new value for To property
     */
    void setTo(EndpointReferenceType epr);

    /**
     * Accessor for the <b>From</b> property.
     * @return current value of From property
     */
    EndpointReferenceType getFrom();

    /**
     * Mutator for the <b>From</b> property.
     * @param epr new value for From property
     */
    void setFrom(EndpointReferenceType epr);

    /**
     * Accessor for the <b>MessageID</b> property.
     * @return current value of MessageID property
     */
    AttributedURIType getMessageID();

    /**
     * Mutator for the <b>MessageID</b> property.
     * @param iri new value for MessageID property
     */
    void setMessageID(AttributedURIType iri);

    /**
     * Accessor for the <b>ReplyTo</b> property.
     * @return current value of ReplyTo property
     */
    EndpointReferenceType getReplyTo();

    /**
     * Mutator for the <b>ReplyTo</b> property.
     * @param ref new value for ReplyTo property
     */
    void setReplyTo(EndpointReferenceType ref);
    
    /**
     * Accessor for the <b>FaultTo</b> property.
     * @return current value of FaultTo property
     */
    EndpointReferenceType getFaultTo();

    /**
     * Mutator for the <b>FaultTo</b> property.
     * @param ref new value for FaultTo property
     */
    void setFaultTo(EndpointReferenceType ref);


    /**
     * Accessor for the <b>RelatesTo</b> property.
     * @return current value of RelatesTo property
     */
    RelatesToType getRelatesTo();

    /**
     * Mutator for the <b>RelatesTo</b> property.
     * @param relatesTo new value for RelatesTo property
     */
    void setRelatesTo(RelatesToType relatesTo);
    
    /**
     * Accessor for the <b>Action</b> property.
     * @return current value of Action property
     */
    AttributedURIType getAction();

    /**
     * Mutator for the <b>Action</b> property.
     * @param iri new value for Action property
     */
    void setAction(AttributedURIType iri);
}
