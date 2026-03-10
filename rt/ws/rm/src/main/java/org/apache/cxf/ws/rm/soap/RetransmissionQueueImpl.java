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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.DeferredConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedOutputStreamCallback;
import org.apache.cxf.io.WriteOnCloseOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseChainCache;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.PartialXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.workqueue.SynchronousExecutor;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.soap.MAPCodec;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertion;
import org.apache.cxf.ws.rm.ProtocolVariation;
import org.apache.cxf.ws.rm.RMCaptureOutInterceptor;
import org.apache.cxf.ws.rm.RMConfiguration;
import org.apache.cxf.ws.rm.RMContextUtils;
import org.apache.cxf.ws.rm.RMEndpoint;
import org.apache.cxf.ws.rm.RMException;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.RMMessageConstants;
import org.apache.cxf.ws.rm.RMProperties;
import org.apache.cxf.ws.rm.RMUtils;
import org.apache.cxf.ws.rm.RetransmissionQueue;
import org.apache.cxf.ws.rm.RetryStatus;
import org.apache.cxf.ws.rm.SourceSequence;
import org.apache.cxf.ws.rm.manager.RetryPolicyType;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceType;
import org.apache.cxf.ws.rmp.v200502.RMAssertion;

/**
 *
 */
public class RetransmissionQueueImpl implements RetransmissionQueue {

    private static final Logger LOG = LogUtils.getL7dLogger(RetransmissionQueueImpl.class);

    private final Map<String, List<ResendCandidate>> candidates = new HashMap<>();
    private final Map<String, List<ResendCandidate>> suspendedCandidates = new HashMap<>();
    private Resender resender;
    private RMManager manager;

    private int unacknowledgedCount;

    public RetransmissionQueueImpl(RMManager m) {
        manager = m;
    }

    public RMManager getManager() {
        return manager;
    }

    public void setManager(RMManager m) {
        manager = m;
    }

    public void addUnacknowledged(Message message) {
        cacheUnacknowledged(message);
    }

    /**
     * @param seq the sequence under consideration
     * @return the number of unacknowledged messages for that sequence
     */
    public synchronized int countUnacknowledged(SourceSequence seq) {
        List<ResendCandidate> sequenceCandidates = getSequenceCandidates(seq);
        return sequenceCandidates == null ? 0 : sequenceCandidates.size();
    }

    public int countUnacknowledged() {
        return unacknowledgedCount;
    }

    /**
     * @return true if there are no unacknowledged messages in the queue
     */
    public boolean isEmpty() {
        return getUnacknowledged().isEmpty();
    }

    /**
     * Purge all candidates for the given sequence that have been acknowledged.
     *
     * @param seq the sequence object.
     */
    public void purgeAcknowledged(SourceSequence seq) {
        purgeCandidates(seq, false);
    }

    /**
     * Purge all candidates for the given sequence. This method is used to
     * terminate the sequence by force and release the resource associated
     * with the sequence.
     *
     * @param seq the sequence object.
     */
    public void purgeAll(SourceSequence seq) {
        purgeCandidates(seq, true);
    }

    private void purgeCandidates(SourceSequence seq, boolean any) {
        Collection<Long> purged = new ArrayList<>();
        Collection<ResendCandidate> resends = new ArrayList<>();
        Identifier sid = seq.getIdentifier();
        synchronized (this) {
            LOG.fine("Start purging resend candidates.");
            List<ResendCandidate> sequenceCandidates = getSequenceCandidates(seq);
            if (null != sequenceCandidates) {
                for (int i = sequenceCandidates.size() - 1; i >= 0; i--) {
                    ResendCandidate candidate = sequenceCandidates.get(i);
                    long m = candidate.getNumber();
                    if (any || seq.isAcknowledged(m)) {
                        sequenceCandidates.remove(i);
                        candidate.resolved();
                        unacknowledgedCount--;
                        purged.add(m);
                        resends.add(candidate);
                    }
                }
                if (sequenceCandidates.isEmpty()) {
                    candidates.remove(sid.getValue());
                }
            }
            LOG.fine("Completed purging resend candidates.");
        }
        if (!purged.isEmpty()) {
            RMStore store = manager.getStore();
            if (null != store) {
                store.removeMessages(sid, purged, true);
            }
            RMEndpoint rmEndpoint = seq.getSource().getReliableEndpoint();
            for (ResendCandidate resend: resends) {
                rmEndpoint.handleAcknowledgment(sid.getValue(), resend.getNumber(), resend.getMessage());
            }
        }
    }

    public List<Long> getUnacknowledgedMessageNumbers(SourceSequence seq) {
        List<Long> unacknowledged = new ArrayList<>();
        List<ResendCandidate> sequenceCandidates = getSequenceCandidates(seq);
        if (null != sequenceCandidates) {
            for (int i = 0; i < sequenceCandidates.size(); i++) {
                ResendCandidate candidate = sequenceCandidates.get(i);
                unacknowledged.add(candidate.getNumber());
            }
        }
        return unacknowledged;
    }

    public RetryStatus getRetransmissionStatus(SourceSequence seq, long num) {
        List<ResendCandidate> sequenceCandidates = getSequenceCandidates(seq);
        if (null != sequenceCandidates) {
            for (int i = 0; i < sequenceCandidates.size(); i++) {
                ResendCandidate candidate = sequenceCandidates.get(i);
                if (num == candidate.getNumber()) {
                    return candidate;
                }
            }
        }
        return null;
    }

    public Map<Long, RetryStatus> getRetransmissionStatuses(SourceSequence seq) {
        Map<Long, RetryStatus> cp = new HashMap<>();
        List<ResendCandidate> sequenceCandidates = getSequenceCandidates(seq);
        if (null != sequenceCandidates) {
            for (int i = 0; i < sequenceCandidates.size(); i++) {
                ResendCandidate candidate = sequenceCandidates.get(i);
                cp.put(candidate.getNumber(), candidate);
            }
        }
        return cp;
    }

    /**
     * Initiate resends.
     */
    public void start() {
        if (null != resender) {
            return;
        }
        LOG.fine("Starting retransmission queue");

        // setup resender

        resender = getDefaultResender();
    }

    /**
     * Stops resending messages for the specified source sequence.
     */
    public void stop(SourceSequence seq) {
        synchronized (this) {
            List<ResendCandidate> sequenceCandidates = getSequenceCandidates(seq);
            if (null != sequenceCandidates) {
                for (int i = sequenceCandidates.size() - 1; i >= 0; i--) {
                    ResendCandidate candidate = sequenceCandidates.get(i);
                    candidate.cancel();
                }
                LOG.log(Level.FINE, "Cancelled resends for sequence {0}.", seq.getIdentifier().getValue());
            }
        }
    }

    void stop() {

    }

    public void suspend(SourceSequence seq) {
        synchronized (this) {
            String key = seq.getIdentifier().getValue();
            List<ResendCandidate> sequenceCandidates = candidates.remove(key);
            if (null != sequenceCandidates) {
                for (int i = sequenceCandidates.size() - 1; i >= 0; i--) {
                    ResendCandidate candidate = sequenceCandidates.get(i);
                    candidate.suspend();
                }
                suspendedCandidates.put(key, sequenceCandidates);
                LOG.log(Level.FINE, "Suspended resends for sequence {0}.", key);
            }
        }
    }

    public void resume(SourceSequence seq) {
        synchronized (this) {
            String key = seq.getIdentifier().getValue();
            List<ResendCandidate> sequenceCandidates = suspendedCandidates.remove(key);
            if (null != sequenceCandidates) {
                for (int i = 0; i < sequenceCandidates.size(); i++) {
                    ResendCandidate candidate = sequenceCandidates.get(i);
                    candidate.resume();
                }
                candidates.put(key, sequenceCandidates);
                LOG.log(Level.FINE, "Resumed resends for sequence {0}.", key);
            }
        }
    }

    /**
     * @return the exponential backoff
     */
    protected int getExponentialBackoff() {
        return DEFAULT_EXPONENTIAL_BACKOFF;
    }

    /**
     * @param message the message context
     * @return a ResendCandidate
     */
    protected ResendCandidate createResendCandidate(Message message) {
        return new ResendCandidate(message);
    }

    /**
     * Accepts a new resend candidate.
     *
     * @param message the message object.
     * @return ResendCandidate
     */
    protected ResendCandidate cacheUnacknowledged(Message message) {
        RMProperties rmps = RMContextUtils.retrieveRMProperties(message, true);
        SequenceType st = rmps.getSequence();
        Identifier sid = st.getIdentifier();
        String key = sid.getValue();

        final ResendCandidate candidate;

        synchronized (this) {
            List<ResendCandidate> sequenceCandidates = getSequenceCandidates(key);
            if (null == sequenceCandidates) {
                sequenceCandidates = new ArrayList<>();
                candidates.put(key, sequenceCandidates);
            }
            candidate = createResendCandidate(message);
            if (isSequenceSuspended(key)) {
                candidate.suspend();
            }
            sequenceCandidates.add(candidate);
            unacknowledgedCount++;
        }
        LOG.fine("Cached unacknowledged message.");
        try {
            RMEndpoint rme = manager.getReliableEndpoint(message);
            rme.handleAccept(key, st.getMessageNumber(), message);
        } catch (RMException e) {
            LOG.log(Level.WARNING, "Could not find reliable endpoint for message");
        }
        return candidate;
    }

    /**
     * @return a map relating sequence ID to a lists of un-acknowledged messages
     *         for that sequence
     */
    protected Map<String, List<ResendCandidate>> getUnacknowledged() {
        return candidates;
    }

    /**
     * @param seq the sequence under consideration
     * @return the list of resend candidates for that sequence
     * @pre called with mutex held
     */
    protected List<ResendCandidate> getSequenceCandidates(SourceSequence seq) {
        return getSequenceCandidates(seq.getIdentifier().getValue());
    }

    /**
     * @param key the sequence identifier under consideration
     * @return the list of resend candidates for that sequence
     * @pre called with mutex held
     */
    protected List<ResendCandidate> getSequenceCandidates(String key) {
        List<ResendCandidate> sc = candidates.get(key);
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

    /**
     * Represents a candidate for resend, i.e. an unacked outgoing message.
     */
    protected class ResendCandidate implements Runnable, RetryStatus {
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
        private boolean includeAckRequested;

        /**
         * @param m the unacked message
         */
        protected ResendCandidate(Message m) {
            message = m;
            retries = 0;
            RMConfiguration cfg = manager.getEffectiveConfiguration(message);
            long baseRetransmissionInterval =
                cfg.getBaseRetransmissionInterval().longValue();
            backoff = cfg.isExponentialBackoff()  ? RetransmissionQueue.DEFAULT_EXPONENTIAL_BACKOFF : 1;
            next = new Date(System.currentTimeMillis() + baseRetransmissionInterval);
            nextInterval = baseRetransmissionInterval * backoff;
            RetryPolicyType rmrp = null != manager.getSourcePolicy()
                ? manager.getSourcePolicy().getRetryPolicy() : null;
            maxRetries = null != rmrp ? rmrp.getMaxRetries() : -1;

            AddressingProperties maps = RMContextUtils.retrieveMAPs(message, false, true);
            AttributedURIType to = null;
            if (null != maps) {
                to = maps.getTo();
                maps.exposeAs(cfg.getAddressingNamespace());
            }
            if (to != null  && RMUtils.getAddressingConstants().getAnonymousURI().equals(to.getValue())) {
                LOG.log(Level.INFO, "Cannot resend to anonymous target.  Not scheduling a resend.");
                return;
            }
            RMProperties rmprops = RMContextUtils.retrieveRMProperties(message, true);
            if (null != rmprops) {
                number = rmprops.getSequence().getMessageNumber();
            }
            if (null != manager.getTimer() && maxRetries != 0) {
                schedule();
            }
        }

        /**
         * Initiate resend asynchronsly.
         *
         * @param requestAcknowledge true if a AckRequest header is to be sent
         *            with resend
         */
        protected void initiate(boolean requestAcknowledge) {
            includeAckRequested = requestAcknowledge;
            pending = true;
            Endpoint ep = message.getExchange().getEndpoint();
            Executor executor = ep.getExecutor();
            if (null == executor) {
                executor = ep.getService().getExecutor();
                if (executor == null) {
                    executor = SynchronousExecutor.getInstance();
                } else {
                    LOG.log(Level.FINE, "Using service executor {0}", executor.getClass().getName());
                }
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
                // ensure ACK wasn't received while this task was enqueued
                // on executor
                if (isPending()) {
                    resender.resend(message, includeAckRequested);
                    includeAckRequested = false;
                }
            } finally {
                attempted();
            }
        }

        public long getNumber() {
            return number;
        }

        /**
         * @return number of resend attempts
         */
        public int getRetries() {
            return retries;
        }

        /**
         * @return number of max resend attempts
         */
        public int getMaxRetries() {
            return maxRetries;
        }

        /**
         * @return date of next resend
         */
        public Date getNext() {
            return next;
        }

        /**
         * @return date of previous resend or null if no attempt is yet taken
         */
        public Date getPrevious() {
            if (retries > 0) {
                return new Date(next.getTime() - nextInterval / backoff);
            }
            return null;
        }

        public long getNextInterval() {
            return nextInterval;
        }

        public long getBackoff() {
            return backoff;
        }

        public boolean isSuspended() {
            return suspended;
        }

        /**
         * @return if resend attempt is pending
         */
        public synchronized boolean isPending() {
            return pending;
        }

        /**
         * ACK has been received for this candidate.
         */
        protected synchronized void resolved() {
            pending = false;
            next = null;
            if (null != nextTask) {
                nextTask.cancel();
                releaseSavedMessage();
            }
        }

        /**
         * Cancel further resend (although no ACK has been received).
         */
        protected synchronized void cancel() {
            if (null != nextTask) {
                nextTask.cancel();
                releaseSavedMessage();
            }
        }

        protected synchronized void suspend() {
            suspended = true;
            pending = false;
            //TODO release the message and later reload it upon resume
            //cancel();
            if (null != nextTask) {
                nextTask.cancel();
            }
        }

        protected synchronized void resume() {
            suspended = false;
            next = new Date(System.currentTimeMillis());
            attempted();
        }

        private void releaseSavedMessage() {
            CachedOutputStream cos = (CachedOutputStream)message.get(RMMessageConstants.SAVED_CONTENT);
            if (cos != null) {
                cos.releaseTempFileHold();
                try {
                    cos.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            // REVISIT -- When reference holder is not needed anymore, code can be removed.
            Closeable closeable = (Closeable)message.get(RMMessageConstants.ATTACHMENTS_CLOSEABLE);
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    // ignore
                }
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
            class ResendTask extends TimerTask {
                ResendCandidate candidate;

                ResendTask(ResendCandidate c) {
                    candidate = c;
                }

                @Override
                public void run() {
                    if (!candidate.isPending()) {
                        candidate.initiate(includeAckRequested);
                    }
                }
            }
            nextTask = new ResendTask(this);
            try {
                manager.getTimer().schedule(nextTask, next);
            } catch (IllegalStateException ex) {
                LOG.log(Level.WARNING, "SCHEDULE_RESEND_FAILED_MSG", ex);
            }
        }
    }

    /**
     * Encapsulates actual resend logic (pluggable to facilitate unit testing)
     */
    public interface Resender {
        /**
         * Resend mechanics.
         *
         * @param message
         * @param requestAcknowledge if a AckRequest should be included
         */
        void resend(Message message, boolean requestAcknowledge);
    }

    /**
     * Create default Resender logic.
     *
     * @return default Resender
     */
    protected final Resender getDefaultResender() {
        return new Resender() {
            public void resend(Message message, boolean requestAcknowledge) {
                RMProperties properties = RMContextUtils.retrieveRMProperties(message, true);
                SequenceType st = properties.getSequence();
                if (st != null) {
                    LOG.log(Level.INFO, "RESEND_MSG", st.getMessageNumber());
                }
                if (message instanceof SoapMessage) {
                    doResend((SoapMessage)message);
                } else {
                    doResend(new SoapMessage(message));
                }
            }
        };
    }

    /**
     * Plug in replacement resend logic (facilitates unit testing).
     *
     * @param replacement resend logic
     */
    protected void replaceResender(Resender replacement) {
        resender = replacement;
    }

    @SuppressWarnings("unchecked")
    protected JaxbAssertion<RMAssertion> getAssertion(AssertionInfo ai) {
        return (JaxbAssertion<RMAssertion>)ai.getAssertion();
    }

    private void readHeaders(XMLStreamReader xmlReader, SoapMessage message) throws XMLStreamException {

        // read header portion of SOAP document into DOM
        SoapVersion version = message.getVersion();
        XMLStreamReader filteredReader = new PartialXMLStreamReader(xmlReader, version.getBody());
        Node nd = message.getContent(Node.class);
        W3CDOMStreamWriter writer = message.get(W3CDOMStreamWriter.class);
        final Document doc;
        if (writer != null) {
            StaxUtils.copy(filteredReader, writer);
            doc = writer.getDocument();
        } else if (nd instanceof Document) {
            doc = (Document)nd;
            StaxUtils.readDocElements(doc, doc, filteredReader, false, false);
        } else {
            doc = StaxUtils.read(filteredReader);
            message.setContent(Node.class, doc);
        }

        // get the actual SOAP header
        Element element = doc.getDocumentElement();
        QName header = version.getHeader();
        List<Element> elemList =
            DOMUtils.findAllElementsByTagNameNS(element, header.getNamespaceURI(), header.getLocalPart());
        for (Element elem : elemList) {

            // set all child elements as headers for message transmission
            Element hel = DOMUtils.getFirstElement(elem);
            while (hel != null) {
                SoapHeader sheader = new SoapHeader(DOMUtils.getElementQName(hel), hel);
                message.getHeaders().add(sheader);
                hel = DOMUtils.getNextElement(hel);
            }
        }
    }

    private void doResend(SoapMessage message) {
        InputStream is = null;
        try {

            // initialize copied interceptor chain for message
            PhaseInterceptorChain retransmitChain = manager.getRetransmitChain(message);
            ProtocolVariation protocol = RMContextUtils.getProtocolVariation(message);
            Endpoint endpoint = manager.getReliableEndpoint(message).getEndpoint(protocol);
            PhaseChainCache cache = new PhaseChainCache();
            boolean after = true;
            if (retransmitChain == null) {

                // no saved retransmit chain, so construct one from scratch (won't work for WS-Security on server, so
                //  need to fix)
                retransmitChain = buildRetransmitChain(endpoint, cache);
                after = false;

            }
            message.setInterceptorChain(retransmitChain);

            // clear flag for SOAP out interceptor so envelope will be written
            message.remove(SoapOutInterceptor.WROTE_ENVELOPE_START);

            // discard all saved content
            Set<Class<?>> formats = message.getContentFormats();
            List<CachedOutputStreamCallback> callbacks = null;
            for (Class<?> clas: formats) {
                Object content = message.getContent(clas);
                if (content != null) {
                    LOG.info("Removing " + clas.getName() + " content of actual type " + content.getClass().getName());
                    message.removeContent(clas);
                    if (clas == OutputStream.class && content instanceof WriteOnCloseOutputStream) {
                        callbacks = ((WriteOnCloseOutputStream)content).getCallbacks();
                    }
                }
            }

            // read SOAP headers from saved input stream
            CachedOutputStream cos = (CachedOutputStream)message.get(RMMessageConstants.SAVED_CONTENT);
            cos.holdTempFile(); // CachedOutputStream is hold until delivering was successful
            is = cos.getInputStream(); // instance is needed to close input stream later on
            XMLStreamReader reader = StaxUtils.createXMLStreamReader(is, StandardCharsets.UTF_8.name());
            message.getHeaders().clear();
            if (reader.getEventType() != XMLStreamConstants.START_ELEMENT
                && reader.nextTag() != XMLStreamConstants.START_ELEMENT) {
                throw new IllegalStateException("No document found");
            }
            readHeaders(reader, message);
            int event;
            while ((event = reader.nextTag()) != XMLStreamConstants.START_ELEMENT) {
                if (event == XMLStreamConstants.END_ELEMENT) {
                    throw new IllegalStateException("No body content present");
                }
            }

            // set message addressing properties
            AddressingProperties maps = MAPCodec.getInstance(message.getExchange().getBus()).unmarshalMAPs(message);
            RMContextUtils.storeMAPs(maps, message, true, MessageUtils.isRequestor(message));
            AttributedURIType to = null;
            if (null != maps) {
                to = maps.getTo();
            }
            if (null == to) {
                LOG.log(Level.SEVERE, "NO_ADDRESS_FOR_RESEND_MSG");
                return;
            }
            if (RMUtils.getAddressingConstants().getAnonymousURI().equals(to.getValue())) {
                LOG.log(Level.FINE, "Cannot resend to anonymous target");
                return;
            }

            // initialize conduit for new message
            Conduit c = message.getExchange().getConduit(message);
            if (c == null) {
                c = buildConduit(message, endpoint, to);
            }
            c.prepare(message);

            // replace standard message marshaling with copy from saved stream
            ListIterator<Interceptor<? extends Message>> iterator = retransmitChain.getIterator();
            while (iterator.hasNext()) {
                Interceptor<? extends Message> incept = iterator.next();

                // remove JAX-WS interceptors which handle message modes and such
                if (incept.getClass().getName().startsWith("org.apache.cxf.jaxws.interceptors")) {
                    retransmitChain.remove(incept);
                } else if (incept instanceof PhaseInterceptor
                    && Phase.MARSHAL.equals(((PhaseInterceptor<?>)incept).getPhase())) {

                    // remove any interceptors from the marshal phase
                    retransmitChain.remove(incept);
                }
            }
            retransmitChain.add(new CopyOutInterceptor(reader));

            // restore callbacks on output stream
            if (callbacks != null) {
                OutputStream os = message.getContent(OutputStream.class);
                if (os != null) {
                    WriteOnCloseOutputStream woc;
                    if (os instanceof WriteOnCloseOutputStream) {
                        woc = (WriteOnCloseOutputStream)os;
                    } else {
                        woc = new WriteOnCloseOutputStream(os);
                        message.setContent(OutputStream.class, woc);
                    }
                    for (CachedOutputStreamCallback cb: callbacks) {
                        woc.registerCallback(cb);
                    }
                }
            }

            // send the message
            message.put(RMMessageConstants.RM_RETRANSMISSION, Boolean.TRUE);
            if (after) {
                retransmitChain.doInterceptStartingAfter(message, RMCaptureOutInterceptor.class.getName());
            } else {
                retransmitChain.doIntercept(message);
            }
            if (LOG.isLoggable(Level.INFO)) {
                RMProperties rmps = RMContextUtils.retrieveRMProperties(message, true);
                SequenceType seq = rmps.getSequence();
                LOG.log(Level.INFO, "Retransmitted message " + seq.getMessageNumber() + " in sequence "
                    + seq.getIdentifier().getValue());
            }

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "RESEND_FAILED_MSG", ex);
        } finally {
            // make sure to always close InputStreams of the CachedOutputStream to avoid leaving temp files undeleted
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * @param message
     * @param endpoint
     * @param to
     * @return
     */
    protected Conduit buildConduit(SoapMessage message, final Endpoint endpoint, AttributedURIType to) {
        Conduit c;
        final String address = to.getValue();
        DeferredConduitSelector cs = new DeferredConduitSelector() {
            @Override
            public synchronized Conduit selectConduit(Message message) {
                final Conduit conduit;
                EndpointInfo endpointInfo = endpoint.getEndpointInfo();
                EndpointReferenceType original = endpointInfo.getTarget();
                try {
                    if (null != address) {
                        endpointInfo.setAddress(address);
                    }
                    conduit = super.selectConduit(message);
                } finally {
                    endpointInfo.setAddress(original);
                }
                conduits.clear();
                return conduit;
            }
        };

        cs.setEndpoint(endpoint);
        c = cs.selectConduit(message);
        // REVISIT
        // use application endpoint message observer instead?
        c.setMessageObserver(new MessageObserver() {
            public void onMessage(Message message) {
                LOG.fine("Ignoring response to resent message.");
            }
        });
        cs.close();
        message.getExchange().setConduit(c);
        return c;
    }

    /**
     * @param endpoint
     * @param cache
     * @return
     */
    protected PhaseInterceptorChain buildRetransmitChain(final Endpoint endpoint, PhaseChainCache cache) {
        PhaseInterceptorChain retransmitChain;
        Bus bus = getManager().getBus();
        List<Interceptor<? extends Message>> i1 = bus.getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by bus: " + i1);
        }
        List<Interceptor<? extends Message>> i2 = endpoint.getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by endpoint: " + i2);
        }
        List<Interceptor<? extends Message>> i3 = endpoint.getBinding().getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by binding: " + i3);
        }
        PhaseManager pm = bus.getExtension(PhaseManager.class);
        retransmitChain = cache.get(pm.getOutPhases(), i1, i2, i3);
        return retransmitChain;
    }

    public static class CopyOutInterceptor extends AbstractOutDatabindingInterceptor {
        private final XMLStreamReader reader;

        public CopyOutInterceptor(XMLStreamReader rdr) {
            super(Phase.MARSHAL);
            reader = rdr;
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            try {
                XMLStreamWriter writer = message.getContent(XMLStreamWriter.class);
                StaxUtils.copy(reader, writer);
            } catch (XMLStreamException e) {
                throw new Fault("COULD_NOT_READ_XML_STREAM", LOG, e);
            }
        }
    }
}
