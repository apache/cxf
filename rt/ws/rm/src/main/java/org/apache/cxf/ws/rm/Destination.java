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

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.v200702.AckRequestedType;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceType;

public class Destination extends AbstractEndpoint {

    private static final Logger LOG = LogUtils.getL7dLogger(Destination.class);

    private Map<String, DestinationSequence> map;

    Destination(RMEndpoint reliableEndpoint) {
        super(reliableEndpoint);
        map = new ConcurrentHashMap<>();
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
        processingSequenceCount.incrementAndGet();
    }

    // this method ensures to keep the sequence until all the messages are delivered
    public void terminateSequence(DestinationSequence seq) {
        terminateSequence(seq, false);
    }
    public void terminateSequence(DestinationSequence seq, boolean forceRemove) {
        seq.terminate();
        if (forceRemove || seq.allAcknowledgedMessagesDelivered()) {
            removeSequence(seq);
        }
    }

    public void removeSequence(DestinationSequence seq) {
        DestinationSequence o;
        o = map.remove(seq.getIdentifier().getValue());
        RMStore store = getReliableEndpoint().getManager().getStore();
        if (null != store) {
            store.removeDestinationSequence(seq.getIdentifier());
        }
        if (o != null) {
            processingSequenceCount.decrementAndGet();
            completedSequenceCount.incrementAndGet();
        }
    }

    /**
     * Acknowledges receipt of a message. If the message is the last in the
     * sequence, sends an out-of-band SequenceAcknowledgement unless there a
     * response will be sent to the acksTo address onto which the acknowldegment
     * can be piggybacked.
     *
     * @param message the message to be acknowledged
     * @throws SequenceFault if the sequence specified in
     *             <code>sequenceType</code> does not exist
     */
    public void acknowledge(Message message) throws SequenceFault, RMException {
        RMProperties rmps = RMContextUtils.retrieveRMProperties(message, false);
        SequenceType sequenceType = rmps.getSequence();
        if (null == sequenceType) {
            return;
        }

        DestinationSequence seq = getSequence(sequenceType.getIdentifier());

        if (null != seq) {
            if (seq.applyDeliveryAssurance(sequenceType.getMessageNumber(), message)) {
                if (PropertyUtils.isTrue(message.get(RMMessageConstants.DELIVERING_ROBUST_ONEWAY))) {
                    return;
                }

                seq.acknowledge(message);

                if (null != rmps.getCloseSequence()) {
                    seq.setLastMessageNumber(sequenceType.getMessageNumber());
                    ackImmediately(seq, message);
                }
            } else {
                try {
                    message.getInterceptorChain().abort();
                    if (seq.sendAcknowledgement()) {
                        ackImmediately(seq, message);
                    }
                    Exchange exchange = message.getExchange();
                    Conduit conduit = exchange.getDestination().getBackChannel(message);
                    if (conduit != null) {
                        //for a one-way, the back channel could be
                        //null if it knows it cannot send anything.
                        if (seq.sendAcknowledgement()) {
                            AddressingProperties maps = RMContextUtils.retrieveMAPs(message, false, false);
                            InternalContextUtils.rebaseResponse(null, maps, message);
                        } else {
                            Message response = createMessage(exchange);
                            response.setExchange(exchange);
                            response.remove(Message.CONTENT_TYPE);
                            conduit.prepare(response);
                            conduit.close(response);
                        }
                    }
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, e.getMessage());
                    throw new RMException(e);
                }
            }
        } else {
            ProtocolVariation protocol = RMContextUtils.getProtocolVariation(message);
            RMConstants consts = protocol.getConstants();
            SequenceFaultFactory sff = new SequenceFaultFactory(consts);
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

        AddressingProperties maps = RMContextUtils.retrieveMAPs(message, false, false);
        String replyToAddress = null;
        if (null != maps.getReplyTo()) {
            replyToAddress = maps.getReplyTo().getAddress().getValue();
        }
        if (!(seq.getAcksTo().getAddress().getValue().equals(replyToAddress) || seq
            .canPiggybackAckOnPartialResponse())) {
            getReliableEndpoint().getProxy().acknowledge(seq);
        }
    }

    void processingComplete(Message message) {
        SequenceType sequenceType = RMContextUtils.retrieveRMProperties(message, false).getSequence();
        if (null == sequenceType) {
            return;
        }

        DestinationSequence seq = getSequence(sequenceType.getIdentifier());

        if (null != seq) {
            long mn = sequenceType.getMessageNumber().longValue();
            seq.processingComplete(mn);
            seq.purgeAcknowledged(mn);
            // remove acknowledged undelivered message
            seq.removeDeliveringMessageNumber(mn);
            if (seq.isTerminated() && seq.allAcknowledgedMessagesDelivered()) {
                removeSequence(seq);
            }
        }
        CachedOutputStream saved = (CachedOutputStream)message.remove(RMMessageConstants.SAVED_CONTENT);
        if (saved != null) {
            saved.releaseTempFileHold();
            try {
                saved.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    void releaseDeliveringStatus(Message message) {
        RMProperties rmps = RMContextUtils.retrieveRMProperties(message, false);
        SequenceType sequenceType = rmps.getSequence();
        if (null != sequenceType) {
            DestinationSequence seq = getSequence(sequenceType.getIdentifier());
            if (null != seq) {
                seq.removeDeliveringMessageNumber(sequenceType.getMessageNumber());
            }
        }
    }

    private static Message createMessage(Exchange exchange) {
        Endpoint ep = exchange.getEndpoint();
        Message msg = null;
        if (ep != null) {
            msg = new MessageImpl();
            msg.setExchange(exchange);
            msg = ep.getBinding().createMessage(msg);
        }
        return msg;
    }
}
