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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.addressing.v200408.EndpointReferenceType;
import org.apache.cxf.ws.rm.SequenceAcknowledgement.AcknowledgementRange;
import org.apache.cxf.ws.rm.manager.AcksPolicyType;
import org.apache.cxf.ws.rm.manager.DeliveryAssuranceType;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.policy.PolicyUtils;
import org.apache.cxf.ws.rm.policy.RMAssertion;
import org.apache.cxf.ws.rm.policy.RMAssertion.AcknowledgementInterval;
import org.apache.cxf.ws.rm.policy.RMAssertion.InactivityTimeout;

public class DestinationSequence extends AbstractSequence {
    
    private static final Logger LOG = LogUtils.getL7dLogger(DestinationSequence.class);

    private Destination destination;
    private EndpointReferenceType acksTo;
    private BigInteger lastMessageNumber;
    private SequenceMonitor monitor;
    private boolean acknowledgeOnNextOccasion;
    private List<DeferredAcknowledgment> deferredAcknowledgments;
    private SequenceTermination scheduledTermination;
    private String correlationID;
    
    public DestinationSequence(Identifier i, EndpointReferenceType a, Destination d) {
        this(i, a, null, null);
        destination = d;
    }
    
    public DestinationSequence(Identifier i, EndpointReferenceType a,
                              BigInteger lmn, SequenceAcknowledgement ac) {
        super(i);
        acksTo = a;
        lastMessageNumber = lmn;
        acknowledgement = ac;
        if (null == acknowledgement) {
            acknowledgement = RMUtils.getWSRMFactory().createSequenceAcknowledgement();
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
     * @return the message number of the last message or null if the last message had not been received.
     */
    public BigInteger getLastMessageNumber() {
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
        SequenceType st = RMContextUtils.retrieveRMProperties(message, false).getSequence();
        BigInteger messageNumber = st.getMessageNumber();
        LOG.fine("Acknowledging message: " + messageNumber);
        if (null != lastMessageNumber && messageNumber.compareTo(lastMessageNumber) > 0) {
            throw new SequenceFaultFactory().createLastMessageNumberExceededFault(st.getIdentifier());
        }        
        
        monitor.acknowledgeMessage();
        
        synchronized (this) {
            boolean done = false;
            int i = 0;
            for (; i < acknowledgement.getAcknowledgementRange().size(); i++) {
                AcknowledgementRange r = acknowledgement.getAcknowledgementRange().get(i);
                if (r.getLower().compareTo(messageNumber) <= 0 
                    && r.getUpper().compareTo(messageNumber) >= 0) {
                    done = true;
                    break;
                } else {
                    BigInteger diff = r.getLower().subtract(messageNumber);
                    if (diff.signum() == 1) {
                        if (diff.equals(BigInteger.ONE)) {
                            r.setLower(messageNumber);
                            done = true;
                        }
                        break;
                    } else if (messageNumber.subtract(r.getUpper()).equals(BigInteger.ONE)) {
                        r.setUpper(messageNumber);
                        done = true;
                        break;
                    }
                }
            }

            if (!done) {
                AcknowledgementRange range = RMUtils.getWSRMFactory()
                    .createSequenceAcknowledgementAcknowledgementRange();
                range.setLower(messageNumber);
                range.setUpper(messageNumber);
                acknowledgement.getAcknowledgementRange().add(i, range);
            }
            mergeRanges();
            notifyAll();
        }
        
        purgeAcknowledged(messageNumber);
        
        RMAssertion rma = PolicyUtils.getRMAssertion(destination.getManager().getRMAssertion(), message);
        long acknowledgementInterval = 0;
        AcknowledgementInterval ai = rma.getAcknowledgementInterval();
        if (null != ai) {
            BigInteger val = ai.getMilliseconds(); 
            if (null != val) {
                acknowledgementInterval = val.longValue();
            }
        }
        
        scheduleAcknowledgement(acknowledgementInterval);
       
        long inactivityTimeout = 0;
        InactivityTimeout iat = rma.getInactivityTimeout();
        if (null != iat) {
            BigInteger val = iat.getMilliseconds(); 
            if (null != val) {
                inactivityTimeout = val.longValue();
            }
        }
        scheduleSequenceTermination(inactivityTimeout);
        
    }
    
    void mergeRanges() {
        List<AcknowledgementRange> ranges = acknowledgement.getAcknowledgementRange();
        for (int i = ranges.size() - 1; i > 0; i--) {
            AcknowledgementRange current = ranges.get(i);
            AcknowledgementRange previous = ranges.get(i - 1);
            if (current.getLower().subtract(previous.getUpper()).equals(BigInteger.ONE)) {
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
    
    void setLastMessageNumber(BigInteger lmn) {
        lastMessageNumber = lmn;
    }
      
    boolean canPiggybackAckOnPartialResponse() {
        // TODO: should also check if we allow breaking the WI Profile rule by which no headers
        // can be included in a HTTP response
        return getAcksTo().getAddress().getValue().equals(RMConstants.getAnonymousAddress());
    }
       
    /**
     * Ensures that the delivery assurance is honored, e.g. by throwing an 
     * exception if the message had already been delivered and the delivery
     * assurance is AtMostOnce.
     * This method blocks in case the delivery assurance is 
     * InOrder and and not all messages with lower message numbers have been 
     * delivered.
     * 
     * @param s the SequenceType object including identifier and message number
     * @throws Fault if message had already been acknowledged
     */
    void applyDeliveryAssurance(BigInteger mn) throws RMException {
        DeliveryAssuranceType da = destination.getManager().getDeliveryAssurance();
        if (da.isSetAtMostOnce() && isAcknowledged(mn)) {            
            org.apache.cxf.common.i18n.Message msg = new org.apache.cxf.common.i18n.Message(
                "MESSAGE_ALREADY_DELIVERED_EXC", LOG, mn, getIdentifier().getValue());
            LOG.log(Level.SEVERE, msg.toString());
            throw new RMException(msg);
        } 
        if (da.isSetInOrder() && da.isSetAtLeastOnce()) {
            synchronized (this) {
                boolean ok = allPredecessorsAcknowledged(mn);
                while (!ok) {
                    try {
                        wait();                        
                        ok = allPredecessorsAcknowledged(mn);
                    } catch (InterruptedException ie) {
                        // ignore
                    }
                }
            }
        }
    }
    
    synchronized boolean allPredecessorsAcknowledged(BigInteger mn) {
        return acknowledgement.getAcknowledgementRange().size() == 1
            && acknowledgement.getAcknowledgementRange().get(0).getLower().equals(BigInteger.ONE)
            && acknowledgement.getAcknowledgementRange().get(0).getUpper().subtract(mn).signum() >= 0;
    }
    
    void purgeAcknowledged(BigInteger messageNr) {
        RMStore store = destination.getManager().getStore();
        if (null == store) {
            return;
        }
        Collection<BigInteger> messageNrs = new ArrayList<BigInteger>();
        messageNrs.add(messageNr);
        store.removeMessages(getIdentifier(), messageNrs, false);
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
 
        if (acknowledgementInterval > 0 && getMonitor().getMPM() >= ap.getIntraMessageThreshold()) {
            LOG.fine("Schedule deferred acknowledgment");
            scheduleDeferredAcknowledgement(acknowledgementInterval);
        } else {
            LOG.fine("Schedule immediate acknowledgment");
            scheduleImmediateAcknowledgement();
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
            deferredAcknowledgments = new ArrayList<DeferredAcknowledgment>();
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
                    DestinationSequence.this.destination.removeSequence(DestinationSequence.this);

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
