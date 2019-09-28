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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.rm.RMConfiguration;
import org.apache.cxf.ws.rm.RMEndpoint;
import org.apache.cxf.ws.rm.RMException;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.RMMessageConstants;
import org.apache.cxf.ws.rm.RMProperties;
import org.apache.cxf.ws.rm.Source;
import org.apache.cxf.ws.rm.SourceSequence;
import org.apache.cxf.ws.rm.manager.RetryPolicyType;
import org.apache.cxf.ws.rm.manager.SourcePolicyType;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceType;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Test resend logic.
 */
public class RetransmissionQueueImplTest {
    private static final Long ONE = Long.valueOf(1);
    private static final Long TWO = Long.valueOf(2);
    private static final Long TEN = Long.valueOf(10);

    private IMocksControl control;
    private RMManager manager;
    private RMEndpoint endpoint;
    private Executor executor;
    private RetransmissionQueueImpl queue;
    private TestResender resender;
    private List<Message> messages = new ArrayList<>();
    private List<RMProperties> properties = new ArrayList<>();
    private List<SequenceType> sequences = new ArrayList<>();
    private List<Identifier> identifiers = new ArrayList<>();
    private List<Object> mocks = new ArrayList<>();

    @Before
    public void setUp() throws RMException {
        control = EasyMock.createNiceControl();
        manager = createMock(RMManager.class);
        endpoint = createMock(RMEndpoint.class);
        EasyMock.expect(manager.getReliableEndpoint(EasyMock.anyObject(Message.class))).andReturn(endpoint).anyTimes();
        queue = new RetransmissionQueueImpl(manager);
        resender = new TestResender();
        queue.replaceResender(resender);
        executor = createMock(Executor.class);
        assertNotNull(executor);
    }

    @After
    public void tearDown() {
        control.verify();
        messages.clear();
        properties.clear();
        sequences.clear();
        mocks.clear();
        control.reset();
    }

    @Test
    public void testCtor() {
        ready(false);
        assertNotNull("expected unacked map", queue.getUnacknowledged());
        assertEquals("expected empty unacked map",
                     0,
                     queue.getUnacknowledged().size());

        queue = new RetransmissionQueueImpl(null);
        assertNull(queue.getManager());
        queue.setManager(manager);
        assertSame("Unexpected RMManager", manager, queue.getManager());
    }

    @Test
    public void testResendCandidateCtor() {
        SoapMessage message = createMock(SoapMessage.class);
        setupMessagePolicies(message);
        control.replay();
        long now = System.currentTimeMillis();
        RetransmissionQueueImpl.ResendCandidate candidate = queue.createResendCandidate(message);
        assertSame(message, candidate.getMessage());
        assertEquals(0, candidate.getRetries());
        Date refDate = new Date(now + 5000);
        assertFalse(candidate.getNext().before(refDate));
        refDate = new Date(now + 7000);
        assertFalse(candidate.getNext().after(refDate));
        assertFalse(candidate.isPending());
    }

    @Test
    public void testResendCandidateAttempted() {
        SoapMessage message = createMock(SoapMessage.class);
        setupMessagePolicies(message);
        ready(true);
        long now = System.currentTimeMillis();
        RetransmissionQueueImpl.ResendCandidate candidate = queue.createResendCandidate(message);
        candidate.attempted();
        assertEquals(1, candidate.getRetries());
        Date refDate = new Date(now + 15000);
        assertFalse(candidate.getNext().before(refDate));
        refDate = new Date(now + 17000);
        assertFalse(candidate.getNext().after(refDate));
        assertFalse(candidate.isPending());
    }

    @Test
    public void testResendCandidateMaxRetries() {
        SoapMessage message = createMock(SoapMessage.class);
        setupMessagePolicies(message);
        setupRetryPolicy(message);

        ready(true);
        RetransmissionQueueImpl.ResendCandidate candidate = queue.createResendCandidate(message);

        assertEquals(3, candidate.getMaxRetries());
        Date next = null;
        for (int i = 1; i < 3; i++) {
            next = candidate.getNext();
            candidate.attempted();
            assertEquals(i, candidate.getRetries());
            // the next time must advance
            assertTrue(candidate.getNext().after(next));
        }
        next = candidate.getNext();
        candidate.attempted();
        // reaches the max retries
        assertEquals(3, candidate.getRetries());
        // the next time must not advance
        assertFalse(candidate.getNext().after(next));
    }

    @Test
    public void testCacheUnacknowledged() {
        SoapMessage message1 = setUpMessage("sequence1", ONE);
        SoapMessage message2 = setUpMessage("sequence2", ONE);
        SoapMessage message3 = setUpMessage("sequence1", TWO);

        setupMessagePolicies(message1);
        setupMessagePolicies(message2);
        setupMessagePolicies(message3);

        endpoint.handleAccept("sequence1", 1, message1);
        EasyMock.expectLastCall();
        endpoint.handleAccept("sequence2", 1, message2);
        EasyMock.expectLastCall();
        endpoint.handleAccept("sequence1", 2, message3);
        EasyMock.expectLastCall();

        ready(false);

        assertNotNull("expected resend candidate",
                      queue.cacheUnacknowledged(message1));
        assertEquals("expected non-empty unacked map",
                     1,
                     queue.getUnacknowledged().size());
        List<RetransmissionQueueImpl.ResendCandidate> sequence1List =
            queue.getUnacknowledged().get("sequence1");
        assertNotNull("expected non-null context list", sequence1List);
        assertSame("expected context list entry",
                   message1,
                   sequence1List.get(0).getMessage());

        assertNotNull("expected resend candidate",
                      queue.cacheUnacknowledged(message2));
        assertEquals("unexpected unacked map size",
                     2,
                     queue.getUnacknowledged().size());
        List<RetransmissionQueueImpl.ResendCandidate> sequence2List =
            queue.getUnacknowledged().get("sequence2");
        assertNotNull("expected non-null context list", sequence2List);
        assertSame("expected context list entry",
                   message2,
                   sequence2List.get(0).getMessage());

        assertNotNull("expected resend candidate",
                      queue.cacheUnacknowledged(message3));
        assertEquals("un expected unacked map size",
                     2,
                     queue.getUnacknowledged().size());
        sequence1List =
            queue.getUnacknowledged().get("sequence1");
        assertNotNull("expected non-null context list", sequence1List);
        assertSame("expected context list entry",
                   message3,
                   sequence1List.get(1).getMessage());
    }

    @Test
    public void testPurgeAcknowledgedSome() {
        Long[] messageNumbers = {TEN, ONE};
        SourceSequence sequence = setUpSequence("sequence1",
                                          messageNumbers,
                                          new boolean[] {true, false});
        List<RetransmissionQueueImpl.ResendCandidate> sequenceList =
            new ArrayList<>();
        queue.getUnacknowledged().put("sequence1", sequenceList);
        SoapMessage message1 = setUpMessage("sequence1", messageNumbers[0]);
        setupMessagePolicies(message1);
        SoapMessage message2 = setUpMessage("sequence1", messageNumbers[1]);
        setupMessagePolicies(message2);

        endpoint.handleAcknowledgment("sequence1", TEN, message1);
        EasyMock.expectLastCall();
        ready(false);

        sequenceList.add(queue.createResendCandidate(message1));
        sequenceList.add(queue.createResendCandidate(message2));

        queue.purgeAcknowledged(sequence);
        assertEquals("unexpected unacked map size",
                     1,
                     queue.getUnacknowledged().size());
        assertEquals("unexpected unacked list size",
                     1,
                     sequenceList.size());
    }

    @Test
    public void testPurgeAcknowledgedNone() {
        Long[] messageNumbers = {TEN, ONE};
        SourceSequence sequence = setUpSequence("sequence1",
                                           messageNumbers,
                                           new boolean[] {false, false});
        List<RetransmissionQueueImpl.ResendCandidate> sequenceList =
            new ArrayList<>();
        queue.getUnacknowledged().put("sequence1", sequenceList);
        SoapMessage message1 = setUpMessage("sequence1", messageNumbers[0]);
        setupMessagePolicies(message1);
        SoapMessage message2 = setUpMessage("sequence1", messageNumbers[1]);
        setupMessagePolicies(message2);
        ready(false);

        sequenceList.add(queue.createResendCandidate(message1));
        sequenceList.add(queue.createResendCandidate(message2));

        queue.purgeAcknowledged(sequence);
        assertEquals("unexpected unacked map size",
                     1,
                     queue.getUnacknowledged().size());
        assertEquals("unexpected unacked list size",
                     2,
                     sequenceList.size());
    }

    @Test
    public void testPurgeAcknowledgedAll() {
        Long[] messageNumbers = {TEN, ONE};
        SourceSequence sequence = setUpSequence("sequence1",
                                          messageNumbers,
                                          new boolean[] {true, true});
        List<RetransmissionQueueImpl.ResendCandidate> sequenceList =
            new ArrayList<>();
        queue.getUnacknowledged().put("sequence1", sequenceList);
        SoapMessage message1 = setUpMessage("sequence1", messageNumbers[0]);
        setupMessagePolicies(message1);
        SoapMessage message2 = setUpMessage("sequence1", messageNumbers[1]);
        setupMessagePolicies(message2);

        endpoint.handleAcknowledgment("sequence1", TEN, message1);
        EasyMock.expectLastCall();
        endpoint.handleAcknowledgment("sequence1", ONE, message2);
        EasyMock.expectLastCall();
        ready(false);

        sequenceList.add(queue.createResendCandidate(message1));
        sequenceList.add(queue.createResendCandidate(message2));
        queue.purgeAcknowledged(sequence);
        assertEquals("unexpected unacked map size",
                     0,
                     queue.getUnacknowledged().size());
        assertEquals("unexpected unacked list size",
                     0,
                     sequenceList.size());
    }

    @Test
    public void testIsEmpty() {
        ready(false);
        assertTrue("queue is not empty", queue.isEmpty());
    }

    @Test
    public void testCountUnacknowledged() {
        Long[] messageNumbers = {TEN, ONE};
        SourceSequence sequence = setUpSequence("sequence1",
                                          messageNumbers,
                                          null);
        List<RetransmissionQueueImpl.ResendCandidate> sequenceList =
            new ArrayList<>();

        queue.getUnacknowledged().put("sequence1", sequenceList);
        SoapMessage message1 = setUpMessage("sequence1", messageNumbers[0], false);
        setupMessagePolicies(message1);
        SoapMessage message2 = setUpMessage("sequence1", messageNumbers[1], false);
        setupMessagePolicies(message2);
        ready(false);

        sequenceList.add(queue.createResendCandidate(message1));
        sequenceList.add(queue.createResendCandidate(message2));

        assertEquals("unexpected unacked count",
                     2,
                     queue.countUnacknowledged(sequence));
        assertFalse("queue is empty", queue.isEmpty());
    }

    @Test
    public void testCountUnacknowledgedUnknownSequence() {
        Long[] messageNumbers = {TEN, ONE};
        SourceSequence sequence = setUpSequence("sequence1",
                                          messageNumbers,
                                          null);
        ready(false);

        assertEquals("unexpected unacked count",
                     0,
                     queue.countUnacknowledged(sequence));
    }

    @Test
    public void testStartStop() {
        control.replay();
        queue.start();
    }

    private SoapMessage setUpMessage(String sid, Long messageNumber) {
        return setUpMessage(sid, messageNumber, true);
    }

    private SoapMessage setUpMessage(String sid, Long messageNumber, boolean storeSequence) {
        SoapMessage message = createMock(SoapMessage.class);
        if (storeSequence) {
            setUpSequenceType(message, sid, messageNumber);
        }
        messages.add(message);

        return message;
    }

    private void setupMessagePolicies(Message message) {
        RMConfiguration cfg = new RMConfiguration();
        EasyMock.expect(manager.getEffectiveConfiguration(message)).andReturn(cfg);
        cfg.setBaseRetransmissionInterval(Long.valueOf(5000));
        cfg.setExponentialBackoff(true);
    }

    private void setupRetryPolicy(Message message) {

        SourcePolicyType spt = control.createMock(SourcePolicyType.class);
        EasyMock.expect(manager.getSourcePolicy()).andReturn(spt).anyTimes();
        RetryPolicyType rpt = control.createMock(RetryPolicyType.class);
        EasyMock.expect(spt.getRetryPolicy()).andReturn(rpt);
        EasyMock.expect(rpt.getMaxRetries()).andReturn(3);
    }

    private void ready(boolean doStart) {
        control.replay();
        if (doStart) {
            queue.start();
        }
    }

    private SequenceType setUpSequenceType(Message message, String sid, Long messageNumber) {
        RMProperties rmps = createMock(RMProperties.class);
        if (message != null) {
            message.get(RMMessageConstants.RM_PROPERTIES_OUTBOUND);
            EasyMock.expectLastCall().andReturn(rmps);
        }
        properties.add(rmps);
        SequenceType sequence = createMock(SequenceType.class);
        if (message != null) {
            rmps.getSequence();
            EasyMock.expectLastCall().andReturn(sequence);
        }
        if (messageNumber != null) {
            EasyMock.expect(sequence.getMessageNumber()).andReturn(messageNumber).anyTimes();
        }
        Identifier id = createMock(Identifier.class);
        EasyMock.expect(sequence.getIdentifier()).andReturn(id).anyTimes();
        EasyMock.expect(id.getValue()).andReturn(sid).anyTimes();
        identifiers.add(id);
        sequences.add(sequence);
        return sequence;
    }

    private SourceSequence setUpSequence(String sid,  Long[] messageNumbers, boolean[] isAcked) {
        SourceSequence sequence = createMock(SourceSequence.class);
        Identifier id = createMock(Identifier.class);
        sequence.getIdentifier();
        EasyMock.expectLastCall().andReturn(id).anyTimes();
        id.getValue();
        EasyMock.expectLastCall().andReturn(sid).anyTimes();
        identifiers.add(id);
        Source source = createMock(Source.class);
        sequence.getSource();
        EasyMock.expectLastCall().andReturn(source).anyTimes();
        source.getReliableEndpoint();
        EasyMock.expectLastCall().andReturn(endpoint).anyTimes();
        boolean includesAcked = false;
        for (int i = 0; isAcked != null && i < isAcked.length; i++) {
            sequence.isAcknowledged(messageNumbers[i]);
            EasyMock.expectLastCall().andReturn(isAcked[i]);
            if (isAcked[i]) {
                includesAcked = true;
            }
        }
        if (includesAcked) {
            RMStore store = createMock(RMStore.class);
            manager.getStore();
            EasyMock.expectLastCall().andReturn(store);
        }
        return sequence;
    }

    /**
     * Creates a mock object ensuring it remains referenced, so as to
     * avoid garbage collection and attendant issues with finalizer
     * calls on mocks.
     *
     * @param toMock the class to mock up
     * @return the mock object
     */
    <T> T createMock(Class<T> toMock) {
        T ret = control.createMock(toMock);
        mocks.add(ret);
        return ret;
    }

    static class TestResender implements RetransmissionQueueImpl.Resender {
        Message message;
        boolean includeAckRequested;

        public void resend(Message ctx, boolean requestAcknowledge) {
            message = ctx;
            includeAckRequested = requestAcknowledge;
        }

        void clear() {
            message = null;
            includeAckRequested = false;
        }
    };
}
