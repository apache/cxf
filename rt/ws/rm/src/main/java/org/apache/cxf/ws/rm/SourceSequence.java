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

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.datatype.Duration;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxb.DatatypeFactory;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.rm.manager.SequenceTerminationPolicyType;
import org.apache.cxf.ws.rm.v200702.Expires;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement.AcknowledgementRange;

// TODO: handle lastMessage
public class SourceSequence extends AbstractSequence {

    private static final Logger LOG = LogUtils.getL7dLogger(SourceSequence.class);

    private Date expires;
    private Source source;
    private long currentMessageNumber;
    private boolean lastMessage;
    private Identifier offeringId;
    private EndpointReferenceType target;

    public SourceSequence(Identifier i, ProtocolVariation pv) {
        this(i, null, null, pv);
    }

    public SourceSequence(Identifier i, Date e, Identifier oi, ProtocolVariation pv) {
        this(i, e, oi, 0, false, pv);
    }

    public SourceSequence(Identifier i, Date e, Identifier oi, long cmn, boolean lm, ProtocolVariation pv) {
        super(i, pv);
        expires = e;

        offeringId = oi;

        currentMessageNumber = cmn;
        lastMessage = lm;
        acknowledgement = new SequenceAcknowledgement();
        acknowledgement.setIdentifier(id);
    }

    /**
     * @return the message number assigned to the most recent outgoing
     *         application message.
     */
    public long getCurrentMessageNr() {
        return currentMessageNumber;
    }

    /**
     * @return true if the last message had been sent for this sequence.
     */
    public boolean isLastMessage() {
        return lastMessage;
    }

    /**
     * @return the identifier of the sequence that was created on behalf of the
     *         CreateSequence request that included this sequence as an offer
     */
    public Identifier getOfferingSequenceIdentifier() {
        return offeringId;
    }

    /**
     * @return the identifier of the rm source
     */
    public String getEndpointIdentifier() {
        return source.getName().toString();
    }

    /**
     * @return the expiry data of this sequence
     */
    public Date getExpires() {
        return expires;
    }

    /**
     * Returns true if this sequence was constructed from an offer for an
     * inbound sequence includes in the CreateSequenceRequest in response to
     * which the sequence with the specified identifier was created.
     * 
     * @param id the sequence identifier
     * @return true if the sequence was constructed from an offer.
     */
    public boolean offeredBy(Identifier sid) {
        return null != offeringId && offeringId.getValue().equals(sid.getValue());
    }

    /**
     * Returns true if a last message had been sent for this sequence and if all
     * messages for this sequence have been acknowledged.
     * 
     * @return true if all messages have been acknowledged.
     */
    public boolean allAcknowledged() {
        if (!lastMessage) {
            return false;
        }

        if (acknowledgement.getAcknowledgementRange().size() == 1) {
            AcknowledgementRange r = acknowledgement.getAcknowledgementRange().get(0);
            return r.getLower().longValue() == 1 && r.getUpper().longValue() == currentMessageNumber;
        }
        return false;
    }

    /**
     * Used by the RM source to cache received acknowledgements for this
     * sequence.
     * 
     * @param acknowledgement an acknowledgement for this sequence
     */
    public void setAcknowledged(SequenceAcknowledgement a) throws RMException {
        acknowledgement = a;
        source.getManager().getRetransmissionQueue().purgeAcknowledged(this);
        if (allAcknowledged()) {
            if (null == target || RMUtils.getAddressingConstants().getAnonymousURI().equals(
                target.getAddress().getValue())) {
                LOG.log(Level.WARNING, "STANDALONE_ANON_TERMINATE_SEQUENCE_MSG");
                // keep the sequence and let RMOutInterceptor remove it after building the TS message
                // if we remove the sequence here, RMOutInterceptor should check for a null sequence
            } else {
                RMEndpoint rme = source.getReliableEndpoint();
                Proxy proxy = rme.getProxy();
                proxy.terminate(this);
                source.removeSequence(this);
            }
        }
    }

    /**
     * Returns the source associated with this source sequence.
     * 
     * @return the source.
     */
    public Source getSource() {
        return source;
    }

    void setSource(Source s) {
        source = s;
    }

    void setLastMessage(boolean lm) {
        lastMessage = lm;
    }

    /**
     * Returns true if the sequence is expired.
     * 
     * @return true if the sequence is expired.
     */

    boolean isExpired() {
        return expires == null ? false : new Date().after(expires);

    }

    public void setExpires(Expires ex) {
        Duration d = null;
        expires = null;
        if (null != ex) {
            d = ex.getValue();
        }

        if (null != d && !d.equals(DatatypeFactory.PT0S)) {
            Date now = new Date();
            expires = new Date(now.getTime() + ex.getValue().getTimeInMillis(now));
        }
    }

    /**
     * Returns the next message number and increases the message number.
     * 
     * @return the next message number.
     */
    long nextMessageNumber() {
        return nextMessageNumber(null, 0, false);
    }

    /**
     * Returns the next message number and increases the message number. The
     * parameters, if not null, indicate that this message is being sent as a
     * response to the message with the specified message number in the sequence
     * specified by the by the identifier, and are used to decide if this
     * message should be the last in this sequence.
     * 
     * @return the next message number.
     */
    public long nextMessageNumber(Identifier inSeqId, long inMsgNumber, boolean last) {
        assert !lastMessage;

        long result = 0;
        synchronized (this) {
            currentMessageNumber++;
            if (last) {
                lastMessage = true;
            } else { 
                checkLastMessage(inSeqId, inMsgNumber);
            } 
            result = currentMessageNumber;
        }
        return result;
    }
    
    SequenceAcknowledgement getAcknowledgement() {
        return acknowledgement;
    }

    /**
     * The target for the sequence is the first non-anonymous address that a
     * message is sent to as part of this sequence. It is subsequently used for
     * as the target of out-of-band protocol messages related to that sequence
     * that originate from the sequnce source (i.e. TerminateSequence and
     * LastMessage, but not AckRequested or SequenceAcknowledgement as these are
     * orignate from the sequence destination).
     * 
     * @param to
     */
    synchronized void setTarget(EndpointReferenceType to) {
        if (target == null && !ContextUtils.isGenericAddress(to)) {
            target = to;
        }
    }

    synchronized EndpointReferenceType getTarget() {
        return target;
    }

    /**
     * Checks if the current message should be the last message in this sequence
     * and if so sets the lastMessageNumber property.
     */
    private void checkLastMessage(Identifier inSeqId, long inMsgNumber) {

        // check if this is a response to a message that was is the last message
        // in the sequence
        // that included this sequence as an offer

        if (null != inSeqId && 0 != inMsgNumber) {
            Destination destination = source.getReliableEndpoint().getDestination();
            DestinationSequence inSeq = null;
            if (null != destination) {
                inSeq = destination.getSequence(inSeqId);
            }

            if (null != inSeq && offeredBy(inSeqId) && inMsgNumber == inSeq.getLastMessageNumber()) {
                lastMessage = true;
            }
        }

        if (!lastMessage) {
            SequenceTerminationPolicyType stp = source.getManager().getSourcePolicy()
                .getSequenceTerminationPolicy();

            assert null != stp;

            if ((stp.getMaxLength() != 0 && stp.getMaxLength() <= currentMessageNumber)
                || (stp.getMaxRanges() > 0 && acknowledgement.getAcknowledgementRange().size() >= stp
                    .getMaxRanges())
                || (stp.getMaxUnacknowledged() > 0 && source.getManager().getRetransmissionQueue()
                    .countUnacknowledged(this) >= stp.getMaxUnacknowledged())) {
                lastMessage = true;
            }
        }

        if (LOG.isLoggable(Level.FINE) && lastMessage) {
            LOG.fine(currentMessageNumber + " should be the last message in this sequence.");
        }
    }
}
