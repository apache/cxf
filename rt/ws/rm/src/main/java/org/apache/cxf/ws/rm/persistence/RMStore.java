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

package org.apache.cxf.ws.rm.persistence;

import java.math.BigInteger;
import java.util.Collection;

import org.apache.cxf.ws.rm.DestinationSequence;
import org.apache.cxf.ws.rm.Identifier;
import org.apache.cxf.ws.rm.SourceSequence;

public interface RMStore {
 
    
    /**
     * Create a source sequence in the persistent store, with the sequence attributes as specified in the
     * <code>RMSourceSequence</code> object.
     * @param seq the sequence
     */
    void createSourceSequence(SourceSequence seq);
    
    /**
     * Create a destination sequence in the persistent store, with the sequence attributes as specified in the
     * <code>RMSDestinationSequence</code> object.
     * @param seq the sequence
     */
    void createDestinationSequence(DestinationSequence seq);
    
    /**
     * Remove the source sequence with the specified identifier from persistent store. 
     * @param seq the sequence
     */
    void removeSourceSequence(Identifier seq);
    
    /**
     * Remove the destination sequence with the specified identifier from persistent store. 
     * @param seq the sequence
     */
    void removeDestinationSequence(Identifier seq);
    
    /**
     * Retrieves all sequences managed by the identified RM source endpoint 
     * from persistent store.
     * 
     * @param endpointIdentifier the identifier for the source
     * @return the collection of sequences
     */    
    Collection<SourceSequence> getSourceSequences(String endpointIdentifier);
    
    /**
     * Retrieves all sequences managed by the identified RM destination endpoint 
     * from persistent store.
     * 
     * @param endpointIdentifier the identifier for the destination
     * @return the collection of sequences
     */    
    Collection<DestinationSequence> getDestinationSequences(String endpointIdentifier);
    
    /**
     * Retrieves the outbound/inbound messages stored for the source/destination sequence with 
     * the given identifier.
     * @param sid the source sequence identifier
     * @param outbound true if the message is outbound
     * @return the collection of messages
     * * 
     */
    Collection<RMMessage> getMessages(Identifier sid, boolean outbound);
    
    /**
     * Called by an RM source upon processing an outbound message. The <code>RMMessage</code>
     * parameter is null for non application (RM protocol) messages.
     * 
     * @param seq the source sequence 
     * @param msg the outgoing message
     */
    void persistOutgoing(SourceSequence seq, RMMessage msg);
    
   /**
    * Called by an RM source upon processing an outbound message. The <code>RMMessage</code>
     * parameter is null for non application (RM protocol) messages.
     * 
    * @param seq the destination sequence
    * @param msg the incoming message
    */
    void persistIncoming(DestinationSequence seq, RMMessage msg);
  
    /**
     * Removes the messages with the given message numbers and identifiers from the store of
     * outbound/inbound messages.
     * 
     * @param sid the identifier of the source sequence
     * @param messageNrs the collection of message numbers
     * @param outbound true if the message is outbound
     */
    void removeMessages(Identifier sid, Collection<BigInteger> messageNrs, boolean outbound);
}
