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

package org.apache.cxf.ws.rm.soap;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.staxutils.StaxSource;
import org.apache.cxf.ws.rm.DestinationSequence;
import org.apache.cxf.ws.rm.RMCaptureInInterceptor;
import org.apache.cxf.ws.rm.RMContextUtils;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.RMMessageConstants;
import org.apache.cxf.ws.rm.RMProperties;
import org.apache.cxf.ws.rm.RedeliveryQueue;
import org.apache.cxf.ws.rm.RetryStatus;
import org.apache.cxf.ws.rm.manager.RetryPolicyType;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceType;

/**
 *
 */
public class RedeliveryQueueImpl implements RedeliveryQueue {
    private static final Logger LOG = LogUtils.getL7dLogger(RedeliveryQueueImpl.class);

    private Map<String, List<RedeliverCandidate>> candidates =
        new HashMap<>();
    private Map<String, List<RedeliverCandidate>> suspendedCandidates =
        new HashMap<>();

    private RMManager manager;

    private int undeliveredCount;

    public RedeliveryQueueImpl(RMManager m) {
        manager = m;
    }

    public RMManager getManager() {
        return manager;
    }

    public void setManager(RMManager m) {
        manager = m;
    }

    public void addUndelivered(Message message) {
        cacheUndelivered(message);
    }

    /**
     * @param seq the sequence under consideration
     * @return the number of undelivered messages for that sequence
     */
    public synchronized int countUndelivered(DestinationSequence seq) {
        List<RedeliverCandidate> sequenceCandidates = getSequenceCandidates(seq);
        return sequenceCandidates == null ? 0 : sequenceCandidates.size();
    }

    public int countUndelivered() {
        return undeliveredCount;
    }

    public boolean isEmpty() {
        return getUndelivered().isEmpty();
    }
    public void purgeAll(DestinationSequence seq) {
        Collection<Long> purged = new ArrayList<>();
        synchronized (this) {
            LOG.fine("Start purging redeliver candidates.");
            List<RedeliverCandidate> sequenceCandidates = getSequenceCandidates(seq);
            if (null != sequenceCandidates) {
                for (int i = sequenceCandidates.size() - 1; i >= 0; i--) {
                    RedeliverCandidate candidate = sequenceCandidates.get(i);
                    long m = candidate.getNumber();
                    sequenceCandidates.remove(i);
                    candidate.resolved();
                    undeliveredCount--;
                    purged.add(m);
                }
                if (sequenceCandidates.isEmpty()) {
                    candidates.remove(seq.getIdentifier().getValue());
                }
            }
            LOG.fine("Completed purging redeliver candidates.");
        }
        if (!purged.isEmpty()) {
            RMStore store = manager.getStore();
            if (null != store) {
                store.removeMessages(seq.getIdentifier(), purged, false);
            }
        }
    }

    public List<Long> getUndeliveredMessageNumbers(DestinationSequence seq) {
        List<Long> undelivered = new ArrayList<>();
        List<RedeliverCandidate> sequenceCandidates = getSequenceCandidates(seq);
        if (null != sequenceCandidates) {
            for (int i = 0; i < sequenceCandidates.size(); i++) {
                RedeliverCandidate candidate = sequenceCandidates.get(i);
                RMProperties properties = RMContextUtils.retrieveRMProperties(candidate.getMessage(),
                                                                              false);
                SequenceType st = properties.getSequence();
                undelivered.add(st.getMessageNumber());
            }
        }
        return undelivered;
    }

    /**
     * @param seq the sequence under consideration
     * @return the list of resend candidates for that sequence
     * @pre called with mutex held
     */
    protected List<RedeliverCandidate> getSequenceCandidates(DestinationSequence seq) {
        return getSequenceCandidates(seq.getIdentifier().getValue());
    }

    /**
     * @param key the sequence identifier under consideration
     * @return the list of resend candidates for that sequence
     * @pre called with mutex held
     */
    protected List<RedeliverCandidate> getSequenceCandidates(String key) {
        List<RedeliverCandidate> sc = candidates.get(key);
        if (null == sc) {
            sc = suspendedCandidates.get(key);
        }
        return sc;
    }

    /**
     * @param key the sequence identifier under consideration
     * @return true if the sequence is currently suspended; false otherwise
     * @pre called with mutex held
     */
    protected boolean isSequenceSuspended(String key) {
        return suspendedCandidates.containsKey(key);
    }

    public RetryStatus getRedeliveryStatus(DestinationSequence seq, long num) {
        List<RedeliverCandidate> sequenceCandidates = getSequenceCandidates(seq);
        if (null != sequenceCandidates) {
            for (int i = 0; i < sequenceCandidates.size(); i++) {
                RedeliverCandidate candidate = sequenceCandidates.get(i);
                RMProperties properties = RMContextUtils.retrieveRMProperties(candidate.getMessage(),
                                                                              false);
                SequenceType st = properties.getSequence();
                if (num == st.getMessageNumber()) {
                    return candidate;
                }
            }
        }
        return null;
    }


    public Map<Long, RetryStatus> getRedeliveryStatuses(DestinationSequence seq) {
        Map<Long, RetryStatus> cp = new HashMap<>();
        List<RedeliverCandidate> sequenceCandidates = getSequenceCandidates(seq);
        if (null != sequenceCandidates) {
            for (int i = 0; i < sequenceCandidates.size(); i++) {
                RedeliverCandidate candidate = sequenceCandidates.get(i);
                RMProperties properties = RMContextUtils.retrieveRMProperties(candidate.getMessage(),
                                                                              false);
                SequenceType st = properties.getSequence();
                cp.put(st.getMessageNumber(), candidate);
            }
        }
        return cp;
    }


    public void start() {

    }


    public void stop(DestinationSequence seq) {
        synchronized (this) {
            List<RedeliverCandidate> sequenceCandidates = getSequenceCandidates(seq);
            if (null != sequenceCandidates) {
                for (int i = sequenceCandidates.size() - 1; i >= 0; i--) {
                    RedeliverCandidate candidate = sequenceCandidates.get(i);
                    candidate.cancel();
                }
                LOG.log(Level.FINE, "Cancelled redeliveriss for sequence {0}.",
                        seq.getIdentifier().getValue());
            }
        }
    }


    public void suspend(DestinationSequence seq) {
        synchronized (this) {
            String key = seq.getIdentifier().getValue();
            List<RedeliverCandidate> sequenceCandidates = candidates.remove(key);
            if (null != sequenceCandidates) {
                for (int i = sequenceCandidates.size() - 1; i >= 0; i--) {
                    RedeliverCandidate candidate = sequenceCandidates.get(i);
                    candidate.suspend();
                }
                suspendedCandidates.put(key, sequenceCandidates);
                LOG.log(Level.FINE, "Suspended redeliveris for sequence {0}.", key);
            }
        }
    }


    public void resume(DestinationSequence seq) {
        synchronized (this) {
            String key = seq.getIdentifier().getValue();
            List<RedeliverCandidate> sequenceCandidates = suspendedCandidates.remove(key);
            if (null != sequenceCandidates) {
                for (int i = 0; i < sequenceCandidates.size(); i++) {
                    RedeliverCandidate candidate = sequenceCandidates.get(i);
                    candidate.resume();
                }
                candidates.put(key, sequenceCandidates);
                LOG.log(Level.FINE, "Resumed redeliveries for sequence {0}.", key);
            }
        }
    }

    /**
     * Accepts a new resend candidate.
     *
     * @param message the message.
     * @return ResendCandidate
     */
    protected RedeliverCandidate cacheUndelivered(Message message) {
        RMProperties rmps = RMContextUtils.retrieveRMProperties(message, false);
        SequenceType st = rmps.getSequence();
        Identifier sid = st.getIdentifier();
        String key = sid.getValue();

        RedeliverCandidate candidate;

        synchronized (this) {
            List<RedeliverCandidate> sequenceCandidates = getSequenceCandidates(key);
            if (null == sequenceCandidates) {
                sequenceCandidates = new ArrayList<>();
                candidates.put(key, sequenceCandidates);
            }
            candidate = getRedeliverCandidate(st, sequenceCandidates);
            if (candidate == null) {
                candidate = new RedeliverCandidate(message);
                if (isSequenceSuspended(key)) {
                    candidate.suspend();
                }
                sequenceCandidates.add(candidate);
                undeliveredCount++;
            }
        }
        LOG.fine("Cached undelivered message.");
        return candidate;
    }

    private RedeliverCandidate getRedeliverCandidate(SequenceType st, List<RedeliverCandidate> rcs) {
        // assume the size of candidates to be relatively small; otherwise we should use message numbers as keys
        for (RedeliverCandidate rc : rcs) {
            if (st.getMessageNumber() == rc.getNumber()) {
                return rc;
            }
        }
        return null;
    }

    protected void purgeDelivered(RedeliverCandidate candidate) {
        RMProperties rmps = RMContextUtils.retrieveRMProperties(candidate.getMessage(), false);
        SequenceType st = rmps.getSequence();
        Identifier sid = st.getIdentifier();
        String key = sid.getValue();

        synchronized (this) {
            List<RedeliverCandidate> sequenceCandidates = getSequenceCandidates(key);
            if (null != sequenceCandidates) {
                // TODO use a constant op instead of this inefficient linear op
                sequenceCandidates.remove(candidate);
                undeliveredCount--;
            }
            if (sequenceCandidates.isEmpty()) {
                candidates.remove(sid.getValue());
            }

        }
        LOG.fine("Purged delivered message.");

    }

    /**
     * @return a map relating sequence ID to a lists of un-acknowledged messages
     *         for that sequence
     */
    protected Map<String, List<RedeliverCandidate>> getUndelivered() {
        return candidates;
    }

    private static InterceptorChain getRedeliveryInterceptorChain(Message m, String phase) {
        Exchange exchange = m.getExchange();
        Endpoint ep = exchange.getEndpoint();
        Bus bus = exchange.getBus();

        PhaseManager pm = bus.getExtension(PhaseManager.class);
        SortedSet<Phase> phases = new TreeSet<>(pm.getInPhases());
        for (Iterator<Phase> it = phases.iterator(); it.hasNext();) {
            Phase p = it.next();
            if (phase.equals(p.getName())) {
                break;
            }
            it.remove();
        }
        PhaseInterceptorChain chain = new PhaseInterceptorChain(phases);
        List<Interceptor<? extends Message>> il = ep.getInInterceptors();
        addInterceptors(chain, il);
        il = ep.getService().getInInterceptors();
        addInterceptors(chain, il);
        il = ep.getBinding().getInInterceptors();
        addInterceptors(chain, il);
        il = bus.getInInterceptors();
        addInterceptors(chain, il);
        if (ep.getService().getDataBinding() instanceof InterceptorProvider) {
            il = ((InterceptorProvider)ep.getService().getDataBinding()).getInInterceptors();
            addInterceptors(chain, il);
        }

        return chain;
    }

    private static void addInterceptors(PhaseInterceptorChain chain,
                                        List<Interceptor<? extends Message>> il) {
        for (Interceptor<? extends Message> i : il) {
            final String iname = i.getClass().getSimpleName();
            if ("OneWayProcessorInterceptor".equals(iname)
                || "MAPAggregatorImpl".equals(iname)
                || "RMInInterceptor".equals(iname)) {
                continue;
            }
            chain.add(i);
        }
    }

    //TODO refactor this class to unify its functionality with that of ResendCandidate
    protected class RedeliverCandidate implements Runnable, RetryStatus {
        private Message message;
        private long number;
        private Date next;
        private TimerTask nextTask;
        private int retries;
        private int maxRetries;
        private long nextInterval;
        private long backoff;
        private boolean pending;
        private boolean suspended;

        protected RedeliverCandidate(Message m) {
            message = m;
            if (message instanceof SoapMessage) {
                // remove old message headers like WSS headers
                ((SoapMessage)message).getHeaders().clear();
            }
            RetryPolicyType rmrp = null != manager.getDestinationPolicy()
                ? manager.getDestinationPolicy().getRetryPolicy() : null;
            long baseRedeliveryInterval = Long.parseLong(DEFAULT_BASE_REDELIVERY_INTERVAL);
            if (null != rmrp && rmrp.getInterval() > 0L) {
                baseRedeliveryInterval = rmrp.getInterval();
            }
            if (rmrp == null || "ExponentialBackoff".equals(rmrp.getAlgorithm())) {
                backoff = RedeliveryQueue.DEFAULT_EXPONENTIAL_BACKOFF;
            } else {
                backoff = 1;
            }
            next = new Date(System.currentTimeMillis() + baseRedeliveryInterval);
            nextInterval = baseRedeliveryInterval * backoff;
            maxRetries = null != rmrp ? rmrp.getMaxRetries() : 0;

            RMProperties rmprops = RMContextUtils.retrieveRMProperties(message, false);
            if (null != rmprops) {
                number = rmprops.getSequence().getMessageNumber();
            }

            if (null != manager.getTimer() && maxRetries != 0) {
                schedule();
            }

        }

        /**
         * Initiate redelivery asynchronsly.
         *
         */
        protected void initiate() {
            pending = true;
            Endpoint ep = message.getExchange().get(Endpoint.class);
            Executor executor = ep.getExecutor();
            if (null == executor) {
                executor = ep.getService().getExecutor();
                LOG.log(Level.FINE, "Using service executor {0}", executor.getClass().getName());
            } else {
                LOG.log(Level.FINE, "Using endpoint executor {0}", executor.getClass().getName());
            }

            try {
                executor.execute(this);
            } catch (RejectedExecutionException ex) {
                LOG.log(Level.SEVERE, "RESEND_INITIATION_FAILED_MSG", ex);
            }
        }

        public void run() {
            try {
                if (isPending()) {
                    // redeliver
                    redeliver();
                    purgeDelivered(this);
                    resolved();
                }
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "redelivery failed", ex);
            } finally {
                attempted();
            }
        }


        private void redeliver() throws Exception {
            LOG.log(Level.INFO, "Redelivering ... for " + (1 + retries));
            String restartingPhase;
            if (message.getContent(Exception.class) != null) {
                message.removeContent(Exception.class);
                message.getExchange().put(Exception.class, null);

                // clean-up message for redelivery
                closeStreamResources();
                message.removeContent(Node.class);
            }

            CachedOutputStream cos = (CachedOutputStream)message.get(RMMessageConstants.SAVED_CONTENT);
            InputStream is = cos.getInputStream();
            message.setContent(InputStream.class, is);
            message = message.getExchange().getEndpoint().getBinding().createMessage(message);
            restartingPhase = Phase.POST_STREAM;
            // skip some interceptor chain phases for redelivery
            InterceptorChain chain = getRedeliveryInterceptorChain(message, restartingPhase);
            ListIterator<Interceptor<? extends Message>> iterator = chain.getIterator();
            while (iterator.hasNext()) {
                Interceptor<? extends Message> incept = iterator.next();
                if (incept.getClass().getName().equals(RMCaptureInInterceptor.class.getName())) {
                    chain.remove(incept);
                }
            }
            message.getExchange().setInMessage(message);
            message.setInterceptorChain(chain);
            chain.doIntercept(message);
            Exception ex = message.getContent(Exception.class);
            if (null != ex) {
                throw ex;
            }
        }

        public long getNumber() {
            return number;
        }

        public Date getNext() {
            return next;
        }

        public Date getPrevious() {
            if (retries > 0) {
                return new Date(next.getTime() - nextInterval / backoff);
            }
            return null;
        }

        public int getRetries() {
            return retries;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public long getNextInterval() {
            return nextInterval;
        }

        public long getBackoff() {
            return backoff;
        }

        public boolean isPending() {
            return pending;
        }

        public boolean isSuspended() {
            return suspended;
        }

        /**
         * the message has been delivered to the application
         */
        protected synchronized void resolved() {
            pending = false;
            next = null;
            if (null != nextTask) {
                nextTask.cancel();
            }
        }

        /**
         * Cancel further redelivery (although not successfully delivered).
         */
        protected void cancel() {
            if (null != nextTask) {
                nextTask.cancel();
                closeStreamResources();
                releaseSavedMessage();
            }
        }

        protected void suspend() {
            suspended = true;
            pending = false;
            //TODO release the message and later reload it upon resume
            //cancel();
            if (null != nextTask) {
                nextTask.cancel();
            }

        }

        protected void resume() {
            suspended = false;
            next = new Date(System.currentTimeMillis());
            attempted();
        }

        private void releaseSavedMessage() {
            CachedOutputStream saved = (CachedOutputStream)message.remove(RMMessageConstants.SAVED_CONTENT);
            if (saved != null) {
                saved.releaseTempFileHold();
                try {
                    saved.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            // Any unclosed resources must be closed to release the temp files.
            Closeable closeable = (Closeable)message.get(RMMessageConstants.ATTACHMENTS_CLOSEABLE);
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        /*
         * Close all stream-like resources stored in the message
         */
        private void closeStreamResources() {
            InputStream oin = message.getContent(InputStream.class);
            if (oin != null) {
                try {
                    oin.close();
                } catch (Exception e) {
                    // ignore
                }
                message.removeContent(InputStream.class);
            }
            XMLStreamReader oreader = message.getContent(XMLStreamReader.class);
            if (oreader != null) {
                try {
                    oreader.close();
                } catch (Exception e) {
                    // ignore
                }
                message.removeContent(XMLStreamReader.class);
            }
            List<?> olist = message.getContent(List.class);
            if (olist != null && olist.size() == 1) {
                Object o = olist.get(0);
                if (o instanceof XMLStreamReader) {
                    oreader = (XMLStreamReader)o;
                } else if (o instanceof StaxSource) {
                    oreader = ((StaxSource)o).getXMLStreamReader();
                }

                if (oreader != null) {
                    try {
                        oreader.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
                message.removeContent(List.class);
            }
        }

        /**
         * @return associated message context
         */
        protected Message getMessage() {
            return message;
        }

        /**
         * A resend has been attempted. Schedule the next attempt.
         */
        protected synchronized void attempted() {
            pending = false;
            retries++;
            if (null != next && maxRetries != retries) {
                next = new Date(next.getTime() + nextInterval);
                nextInterval *= backoff;
                schedule();
            }
        }

        protected final synchronized void schedule() {
            if (null == manager.getTimer()) {
                return;
            }
            class RedeliverTask extends TimerTask {
                RedeliverCandidate candidate;

                RedeliverTask(RedeliverCandidate c) {
                    candidate = c;
                }

                @Override
                public void run() {
                    if (!candidate.isPending()) {
                        candidate.initiate();
                    }
                }
            }
            nextTask = new RedeliverTask(this);
            try {
                manager.getTimer().schedule(nextTask, next);
            } catch (IllegalStateException ex) {
                LOG.log(Level.WARNING, "SCHEDULE_RESEND_FAILED_MSG", ex);
            }
        }
    }
}
