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
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.addressing.AddressingPropertiesImpl;
import org.apache.cxf.ws.rm.persistence.RMStore;

public class Destination extends AbstractEndpoint {

    private Map<String, DestinationSequence> map;

    Destination(RMEndpoint reliableEndpoint) {
        super(reliableEndpoint);
        map = new HashMap<String, DestinationSequence>();
    }

    public DestinationSequence getSequence(Identifier id) {
        return map.get(id.getValue());
    }

    public Collection<DestinationSequence> getAllSequences() {
        return CastUtils.cast(map.values());
    }

    public void addSequence(DestinationSequence seq) {
        addSequence(seq, true);
    }

    public void addSequence(DestinationSequence seq, boolean persist) {
        seq.setDestination(this);
        map.put(seq.getIdentifier().getValue(), seq);
        if (persist) {
            RMStore store = getReliableEndpoint().getManager().getStore();
            if (null != store) {
                store.createDestinationSequence(seq);
            }
        }
    }

    public void removeSequence(DestinationSequence seq) {
        map.remove(seq.getIdentifier().getValue());
        RMStore store = getReliableEndpoint().getManager().getStore();
        if (null != store) {
            store.removeDestinationSequence(seq.getIdentifier());
        }
    }

    /**
     * Acknowledges receipt of a message. If the message is the last in the
     * sequence, sends an out-of-band SequenceAcknowledgement unless there a
     * response will be sent to the acksTo address onto which the acknowldegment
     * can be piggybacked.
     * 
     * @param sequenceType the sequenceType object that includes identifier and
     *            message number (and possibly a lastMessage element) for the
     *            message to be acknowledged)
     * @param replyToAddress the replyTo address of the message that carried
     *            this sequence information
     * @throws SequenceFault if the sequence specified in
     *             <code>sequenceType</code> does not exist
     */
    public void acknowledge(Message message) throws SequenceFault, RMException {
        SequenceType sequenceType = RMContextUtils.retrieveRMProperties(message, false).getSequence();
        if (null == sequenceType) {
            return;
        }
        
        DestinationSequence seq = getSequence(sequenceType.getIdentifier());

        if (null != seq) {
            seq.applyDeliveryAssurance(sequenceType.getMessageNumber());
            seq.acknowledge(message);

            if (null != sequenceType.getLastMessage()) {
                seq.setLastMessageNumber(sequenceType.getMessageNumber());
                ackImmediately(seq, message);
            }
        } else {
            SequenceFaultFactory sff = new SequenceFaultFactory();
            throw sff.createUnknownSequenceFault(sequenceType.getIdentifier());
        }
    }
    
    void ackRequested(Message message) throws SequenceFault, RMException {
        // TODO
        Collection<AckRequestedType> ars = RMContextUtils.retrieveRMProperties(message, false)
            .getAcksRequested();
        if (null == ars) {
            return;
        }
        for (AckRequestedType ar : ars) {
            Identifier id = ar.getIdentifier();
            DestinationSequence seq = getSequence(id);
            if (null == seq) {
                continue;
            }
            ackImmediately(seq, message);
        }
    }
    
    void ackImmediately(DestinationSequence seq, Message message) throws RMException {
        
        seq.scheduleImmediateAcknowledgement();

        // if we cannot expect an outgoing message to which the
        // acknowledgement
        // can be added we need to send an out-of-band
        // SequenceAcknowledgement message

        AddressingPropertiesImpl maps = RMContextUtils.retrieveMAPs(message, false, false);
        String replyToAddress = null;
        if (null != maps.getReplyTo()) {
            replyToAddress = maps.getReplyTo().getAddress().getValue();
        }
        if (!(seq.getAcksTo().getAddress().getValue().equals(replyToAddress) || seq
            .canPiggybackAckOnPartialResponse())) { 
            getReliableEndpoint().getProxy().acknowledge(seq);                    
        }
    }

}
