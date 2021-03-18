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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.rm.RMConfiguration.DeliveryAssurance;
import org.apache.cxf.ws.rm.manager.AcksPolicyType;
import org.apache.cxf.ws.rm.persistence.PersistenceUtils;
import org.apache.cxf.ws.rm.persistence.RMMessage;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement.AcknowledgementRange;
import org.apache.cxf.ws.rm.v200702.SequenceType;

public class DestinationSequence extends AbstractSequence {

    private static final Logger LOG = LogUtils.getL7dLogger(DestinationSequence.class);

    private Destination destination;
    private EndpointReferenceType acksTo;
    private long lastMessageNumber;
    private SequenceMonitor monitor;
    private volatile boolean acknowledgeOnNextOccasion;
    private boolean terminated;
    private List<DeferredAcknowledgment> deferredAcknowledgments;
    private SequenceTermination scheduledTermination;
    private String correlationID;
    private volatile long inProcessNumber;
    private volatile long highNumberCompleted;
    private long nextInOrder;
    //be careful, must be used in sync block
    private Map<Long, Continuation> continuations = new TreeMap<>();
    // this map is used for robust and redelivery tracking. for redelivery it holds the beingDeliverd messages
    private Set<Long> deliveringMessageNumbers = new HashSet<>();

    public DestinationSequence(Identifier i, EndpointReferenceType a, Destination d, ProtocolVariation pv) {
        this(i, a, 0, false, null, pv);
        destination = d;
    }

    public DestinationSequence(Identifier i, EndpointReferenceType a,
                              long lmn, SequenceAcknowledgement ac, ProtocolVariation pv) {
        this(i, a, lmn, false, ac, pv);
    }

    public DestinationSequence(Identifier i, EndpointReferenceType a,
                              long lmn, boolean t, SequenceAcknowledgement ac, ProtocolVariation pv) {
        super(i, pv);
        acksTo = a;
        lastMessageNumber = lmn;
        terminated = t;
        acknowledgement = ac;
        if (null == acknowledgement) {
            acknowledgement = new SequenceAcknowledgement();
            acknowledgement.setIdentifier(id);
        }
        monitor = new SequenceMonitor();
    }

    /**
     * @return the acksTo address for the sequence
     */
    public EndpointReferenceType getAcksTo() {
        return acksTo;
    }

    /**
     * @return the message number of the last message or 0 if the last message had not been received.
     */
    public long getLastMessageNumber() {
        return lastMessageNumber;
    }

    /**
     * @return the sequence acknowledgement presenting the sequences thus far received by a destination
     */
    public SequenceAcknowledgement getAcknowledgment() {
        return acknowledgement;
    }

    /**
     * @return the identifier of the rm destination
     */
    public String getEndpointIdentifier() {
        return destination.getName();
    }

    public void acknowledge(Message message) throws SequenceFault {
        RMProperties rmps = RMContextUtils.retrieveRMProperties(message, false);
        SequenceType st = rmps.getSequence();
        long messageNumber = st.getMessageNumber();
        LOG.fine("Acknowledging message: " + messageNumber);
        if (0L != lastMessageNumber && messageNumber > lastMessageNumber) {
            RMConstants consts = getProtocol().getConstants();
            SequenceFaultFactory sff = new SequenceFaultFactory(consts);
            throw sff.createSequenceTerminatedFault(st.getIdentifier(), false);
        }

        monitor.acknowledgeMessage();
        boolean updated = false;

        synchronized (this) {
            boolean done = false;
            int i = 0;
            for (; i < acknowledgement.getAcknowledgementRange().size(); i++) {
                AcknowledgementRange r = acknowledgement.getAcknowledgementRange().get(i);
                if (r.getLower().compareTo(messageNumber) <= 0
                    && r.getUpper().compareTo(messageNumber) >= 0) {
                    done = true;
                    break;
                }
                long diff = r.getLower() - messageNumber;
                if (diff == 1L) {
                    r.setLower(messageNumber);
                    updated = true;
                    done = true;
                } else if (diff > 0L) {
                    break;
                } else if (messageNumber - r.getUpper() == 1L) {
                    r.setUpper(messageNumber);
                    updated = true;
                    done = true;
                    break;
                }
            }

            if (!done) {

                // need new acknowledgement range
                AcknowledgementRange range = new AcknowledgementRange();
                range.setLower(messageNumber);
                range.setUpper(messageNumber);
                updated = true;
                acknowledgement.getAcknowledgementRange().add(i, range);
                if (acknowledgement.getAcknowledgementRange().size() > 1) {

                    // acknowledge out-of-order at first opportunity
                    scheduleImmediateAcknowledgement();

                }
            }
            mergeRanges();
        }

        if (updated) {
            RMStore store = destination.getManager().getStore();
            if (null != store && !MessageUtils.getContextualBoolean(message, Message.ROBUST_ONEWAY)) {
                try {
                    RMMessage msg = new RMMessage();
                    CachedOutputStream cos = (CachedOutputStream)message
                        .get(RMMessageConstants.SAVED_CONTENT);
                    msg.setMessageNumber(st.getMessageNumber());
                    msg.setCreatedTime(rmps.getCreatedTime());
                    // in case no attachments are available, cos can be saved directly
                    if (message.getAttachments() == null) {
                        msg.setContent(cos);
                        msg.setContentType((String)message.get(Message.CONTENT_TYPE));
                    } else {
                        InputStream is = cos.getInputStream();
                        PersistenceUtils.encodeRMContent(msg, message, is);
                    }
                    store.persistIncoming(this, msg);
                } catch (IOException e) {
                    throw new Fault(e);
                }
            }
        }
        deliveringMessageNumbers.add(messageNumber);

        RMEndpoint reliableEndpoint = destination.getReliableEndpoint();
        RMConfiguration cfg = reliableEndpoint.getConfiguration();

        if (null == rmps.getCloseSequence()) {
            scheduleAcknowledgement(cfg.getAcknowledgementIntervalTime());
        }
        long inactivityTimeout = cfg.getInactivityTimeoutTime();
        scheduleSequenceTermination(inactivityTimeout);
    }

    void mergeRanges() {
        List<AcknowledgementRange> ranges = acknowledgement.getAcknowledgementRange();
        for (int i = ranges.size() - 1; i > 0; i--) {
            AcknowledgementRange current = ranges.get(i);
            AcknowledgementRange previous = ranges.get(i - 1);
            if (current.getLower().longValue() - previous.getUpper().longValue() == 1) {
                previous.setUpper(current.getUpper());
                ranges.remove(i);
            }
        }
    }

    void setDestination(Destination d) {
        destination = d;
    }

    Destination getDestination() {
        return destination;
    }

    /**
     * Returns the monitor for this sequence.
     *
     * @return the sequence monitor.
     */
    SequenceMonitor getMonitor() {
        return monitor;
    }

    void setLastMessageNumber(long lmn) {
        lastMessageNumber = lmn;
    }

    boolean canPiggybackAckOnPartialResponse() {
        // TODO: should also check if we allow breaking the WI Profile rule by which no headers
        // can be included in a HTTP response
        return getAcksTo().getAddress().getValue().equals(RMUtils.getAddressingConstants().getAnonymousURI());
    }

    /**
     * Ensures that the delivery assurance is honored.
     * If the delivery assurance includes either AtLeastOnce or ExactlyOnce, combined with InOrder, this
     * queues out-of-order messages for processing after the missing messages have been received.
     *
     * @param mn message number
     * @return <code>true</code> if message processing to continue, <code>false</code> if to be dropped
     */
    boolean applyDeliveryAssurance(long mn, Message message) {
        Continuation cont = getContinuation(message);
        RMConfiguration config = destination.getReliableEndpoint().getConfiguration();
        DeliveryAssurance da = config.getDeliveryAssurance();
        boolean canSkip = da != DeliveryAssurance.AT_LEAST_ONCE && da != DeliveryAssurance.EXACTLY_ONCE;
        boolean robust = false;
        boolean robustDelivering = false;
        boolean inOrder = mn - nextInOrder == 1;
        if (message != null) {
            robust = MessageUtils.getContextualBoolean(message, Message.ROBUST_ONEWAY);
            if (robust) {
                robustDelivering =
                    PropertyUtils.isTrue(message.get(RMMessageConstants.DELIVERING_ROBUST_ONEWAY));
            }
        }
        if (robust && !robustDelivering) {
            // no check performed if in robust and not in delivering
            removeDeliveringMessageNumber(mn);
            if (inOrder) {
                nextInOrder++;
            }
            return true;
        }
        if (inOrder) {
            nextInOrder++;
        } else {

            // message out of order, schedule acknowledgement to update sender
            scheduleImmediateAcknowledgement();
            if (nextInOrder < mn) {
                nextInOrder = mn + 1;
            }
        }
        if (cont != null && config.isInOrder() && !cont.isNew()) {
            return waitInQueue(mn, canSkip, message, cont);
        }
        if ((da == DeliveryAssurance.EXACTLY_ONCE || da == DeliveryAssurance.AT_MOST_ONCE)
            && (isAcknowledged(mn) || (robustDelivering && deliveringMessageNumbers.contains(mn)))) {
            org.apache.cxf.common.i18n.Message msg = new org.apache.cxf.common.i18n.Message(
                "MESSAGE_ALREADY_DELIVERED_EXC", LOG, mn, getIdentifier().getValue());
            LOG.log(Level.INFO, msg.toString());
            return false;
        }
        if (robustDelivering) {
            addDeliveringMessageNumber(mn);
        }
        if (config.isInOrder()) {
            return waitInQueue(mn, canSkip, message, cont);
        }
        return true;
    }

    void removeDeliveringMessageNumber(long mn) {
        synchronized (deliveringMessageNumbers) {
            deliveringMessageNumbers.remove(mn);
        }
    }
    void addDeliveringMessageNumber(long mn) {
        synchronized (deliveringMessageNumbers) {
            deliveringMessageNumbers.add(mn);
        }
    }

    // this method is only used for redelivery
    boolean allAcknowledgedMessagesDelivered() {
        return deliveringMessageNumbers.isEmpty();
    }

    private Continuation getContinuation(Message message) {
        if (message == null) {
            return null;
        }
        return message.get(Continuation.class);
    }

    synchronized boolean waitInQueue(long mn, boolean canSkip,
                                     Message message, Continuation continuation) {
        while (true) {

            // can process now if no other in process and this one is next
            if (inProcessNumber == 0) {
                long diff = mn - highNumberCompleted;
                if (diff == 1 || (canSkip && diff > 0)) {
                    inProcessNumber = mn;
                    return true;
                }
            }

            // can abort now if same message in process or already processed
            if (mn == inProcessNumber || isAcknowledged(mn)) {
                return false;
            }
            if (continuation == null) {
                ContinuationProvider p = message.get(ContinuationProvider.class);
                if (p != null) {
                    boolean isOneWay = message.getExchange().isOneWay();
                    message.getExchange().setOneWay(false);
                    continuation = p.getContinuation();
                    message.getExchange().setOneWay(isOneWay);
                    message.put(Continuation.class, continuation);
                }
            }

            if (continuation != null) {
                continuation.setObject(message);
                if (continuation.suspend(-1)) {
                    continuations.put(mn, continuation);
                    throw new SuspendedInvocationException();
                }
            }
            try {
                //if we get here, there isn't a continuation available
                //so we need to block/wait
                wait();
            } catch (InterruptedException ie) {
                // ignore
            }
        }
    }
    synchronized void wakeupNext(long i) {
        try {
            Continuation c = continuations.remove(i + 1);
            if (c != null) {
                //next was found, don't resume everything, just the next one
                c.resume();
                return;
            }
            //next wasn't found, nothing to resume, assume it will come in later...
        } finally {
            notifyAll();
        }
    }

    synchronized void processingComplete(long mn) {
        inProcessNumber = 0;
        highNumberCompleted = mn;
        wakeupNext(mn);
    }

    void purgeAcknowledged(long messageNr) {
        RMStore store = destination.getManager().getStore();
        if (null == store) {
            return;
        }
        store.removeMessages(getIdentifier(), Collections.singleton(messageNr), false);
    }

    /**
     * Called after an acknowledgement header for this sequence has been added to an outgoing message.
     */
    void acknowledgmentSent() {
        acknowledgeOnNextOccasion = false;
    }

    public boolean sendAcknowledgement() {
        return acknowledgeOnNextOccasion;
    }

    List<DeferredAcknowledgment> getDeferredAcknowledgements() {
        return deferredAcknowledgments;
    }

    /**
     * The correlation of the incoming CreateSequence call used to create this
     * sequence is recorded so that in the absence of an offer, the corresponding
     * outgoing CreateSeqeunce can be correlated.
     */
    void setCorrelationID(String cid) {
        correlationID = cid;
    }

    String getCorrelationID() {
        return correlationID;
    }

    void scheduleAcknowledgement(long acknowledgementInterval) {
        AcksPolicyType ap = destination.getManager().getDestinationPolicy().getAcksPolicy();

        if (acknowledgementInterval > 0 && getMonitor().getMPM() >= (ap == null ? 10 : ap.getIntraMessageThreshold())) {
            LOG.fine("Schedule deferred acknowledgment");
            scheduleDeferredAcknowledgement(acknowledgementInterval);
        } else {
            LOG.fine("Schedule immediate acknowledgment");
            scheduleImmediateAcknowledgement();

            destination.getManager().getTimer().schedule(
                new ImmediateFallbackAcknowledgment(), ap == null ? 1000L : ap.getImmediaAcksTimeout());

        }
    }


    void scheduleImmediateAcknowledgement() {
        acknowledgeOnNextOccasion = true;
    }

    synchronized void scheduleSequenceTermination(long inactivityTimeout) {
        if (inactivityTimeout <= 0) {
            return;
        }
        boolean scheduled = null != scheduledTermination;
        if (null == scheduledTermination) {
            scheduledTermination = new SequenceTermination();
        }
        scheduledTermination.updateInactivityTimeout(inactivityTimeout);
        if (!scheduled) {
            destination.getManager().getTimer().schedule(scheduledTermination, inactivityTimeout);
        }
    }

    synchronized void scheduleDeferredAcknowledgement(long delay) {

        if (null == deferredAcknowledgments) {
            deferredAcknowledgments = new ArrayList<>();
        }
        long now = System.currentTimeMillis();
        long expectedExecutionTime = now + delay;
        for (DeferredAcknowledgment da : deferredAcknowledgments) {
            if (da.scheduledExecutionTime() <= expectedExecutionTime) {
                return;
            }
        }
        DeferredAcknowledgment da = new DeferredAcknowledgment();
        deferredAcknowledgments.add(da);
        destination.getManager().getTimer().schedule(da, delay);
        LOG.fine("Scheduled acknowledgment to be sent in " + delay + " ms");
    }

    synchronized void cancelDeferredAcknowledgments() {
        if (null == deferredAcknowledgments) {
            return;
        }
        for (int i = deferredAcknowledgments.size() - 1; i >= 0; i--) {
            DeferredAcknowledgment da = deferredAcknowledgments.get(i);
            da.cancel();
        }
    }

    synchronized void cancelTermination() {
        if (null != scheduledTermination) {
            scheduledTermination.cancel();
        }
    }

    final class DeferredAcknowledgment extends TimerTask {

        public void run() {
            LOG.fine("timer task: send acknowledgment.");
            DestinationSequence.this.scheduleImmediateAcknowledgement();

            try {
                RMEndpoint rme = destination.getReliableEndpoint();
                Proxy proxy = rme.getProxy();
                proxy.acknowledge(DestinationSequence.this);
            } catch (RMException ex) {
                // already logged
            } finally {
                synchronized (DestinationSequence.this) {
                    DestinationSequence.this.deferredAcknowledgments.remove(this);
                }

            }

        }
    }

    final class ImmediateFallbackAcknowledgment extends TimerTask {
        public void run() {
            LOG.fine("timer task: send acknowledgment.");
            if (!sendAcknowledgement()) {
                //Acknowledgment already get send out
                return;
            }

            try {
                destination.getReliableEndpoint().getProxy().acknowledge(DestinationSequence.this);
            } catch (RMException ex) {
                // already logged
            }
        }
    }

    void terminate() {
        if (!terminated) {
            terminated = true;
            RMStore store = destination.getManager().getStore();
            if (null == store) {
                return;
            }
            // only updating the sequence
            store.persistIncoming(this, null);
        }
    }

    public boolean isTerminated() {
        return terminated;
    }

    final class SequenceTermination extends TimerTask {

        private long maxInactivityTimeout;

        void updateInactivityTimeout(long timeout) {
            maxInactivityTimeout = Math.max(maxInactivityTimeout, timeout);
        }

        public void run() {
            synchronized (DestinationSequence.this) {
                DestinationSequence.this.scheduledTermination = null;
                RMEndpoint rme = destination.getReliableEndpoint();
                long lat = Math.max(rme.getLastControlMessage(), rme.getLastApplicationMessage());
                if (0 == lat) {
                    return;
                }
                long now = System.currentTimeMillis();
                if (now - lat >= maxInactivityTimeout) {

                    // terminate regardless outstanding acknowledgments - as we assume that the client is
                    // gone there is no point in sending a SequenceAcknowledgment
                    LogUtils.log(LOG, Level.WARNING, "TERMINATING_INACTIVE_SEQ_MSG",
                                 DestinationSequence.this.getIdentifier().getValue());
                    DestinationSequence.this.destination.terminateSequence(DestinationSequence.this, true);
                    Source source = rme.getSource();
                    if (source != null) {
                        SourceSequence ss = source.getAssociatedSequence(DestinationSequence.this.getIdentifier());
                        if (ss != null) {
                            source.removeSequence(ss);
                        }
                    }
                } else {
                   // reschedule
                    SequenceTermination st = new SequenceTermination();
                    st.updateInactivityTimeout(maxInactivityTimeout);
                    DestinationSequence.this.destination.getManager().getTimer()
                        .schedule(st, maxInactivityTimeout);
                }
            }
        }
    }
}