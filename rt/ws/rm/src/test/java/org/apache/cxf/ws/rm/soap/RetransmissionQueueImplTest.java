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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test resend logic.
 */
public class RetransmissionQueueImplTest {
    private static final Long ONE = Long.valueOf(1);
    private static final Long TWO = Long.valueOf(2);
    private static final Long TEN = Long.valueOf(10);

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
        manager = createMock(RMManager.class);
        endpoint = createMock(RMEndpoint.class);
        when(manager.getReliableEndpoint(any(Message.class))).thenReturn(endpoint);
        queue = new RetransmissionQueueImpl(manager);
        resender = new TestResender();
        queue.replaceResender(resender);
        executor = createMock(Executor.class);
        assertNotNull(executor);
    }

    @After
    public void tearDown() {
        messages.clear();
        properties.clear();
        sequences.clear();
        mocks.clear();
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
        Date next;
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

        doCallRealMethod().when(endpoint).handleAccept("sequence1", 1, message1);
        doCallRealMethod().when(endpoint).handleAccept("sequence2", 1, message2);
        doCallRealMethod().when(endpoint).handleAccept("sequence1", 2, message3);

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

        doCallRealMethod().when(endpoint).handleAcknowledgment("sequence1", TEN, message1);
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

        doCallRealMethod().when(endpoint).handleAcknowledgment("sequence1", TEN, message1);
        doCallRealMethod().when(endpoint).handleAcknowledgment("sequence1", ONE, message2);
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
        when(manager.getEffectiveConfiguration(message)).thenReturn(cfg);
        cfg.setBaseRetransmissionInterval(Long.valueOf(5000));
        cfg.setExponentialBackoff(true);
    }

    private void setupRetryPolicy(Message message) {

        SourcePolicyType spt = mock(SourcePolicyType.class);
        when(manager.getSourcePolicy()).thenReturn(spt);
        RetryPolicyType rpt = mock(RetryPolicyType.class);
        when(spt.getRetryPolicy()).thenReturn(rpt);
        when(rpt.getMaxRetries()).thenReturn(3);
    }

    private void ready(boolean doStart) {
        if (doStart) {
            queue.start();
        }
    }

    private SequenceType setUpSequenceType(Message message, String sid, Long messageNumber) {
        RMProperties rmps = createMock(RMProperties.class);
        if (message != null) {
            when(message.get(RMMessageConstants.RM_PROPERTIES_OUTBOUND)).thenReturn(rmps);
        }
        properties.add(rmps);
        SequenceType sequence = createMock(SequenceType.class);
        if (message != null) {
            when(rmps.getSequence()).thenReturn(sequence);
        }
        if (messageNumber != null) {
            when(sequence.getMessageNumber()).thenReturn(messageNumber);
        }
        Identifier id = createMock(Identifier.class);
        when(sequence.getIdentifier()).thenReturn(id);
        when(id.getValue()).thenReturn(sid);
        identifiers.add(id);
        sequences.add(sequence);
        return sequence;
    }

    private SourceSequence setUpSequence(String sid,  Long[] messageNumbers, boolean[] isAcked) {
        SourceSequence sequence = createMock(SourceSequence.class);
        Identifier id = createMock(Identifier.class);
        when(sequence.getIdentifier()).thenReturn(id);
        when(id.getValue()).thenReturn(sid);
        identifiers.add(id);
        Source source = createMock(Source.class);
        when(sequence.getSource()).thenReturn(source);
        when(source.getReliableEndpoint()).thenReturn(endpoint);
        boolean includesAcked = false;
        for (int i = 0; isAcked != null && i < isAcked.length; i++) {
            when(sequence.isAcknowledged(messageNumbers[i])).thenReturn(isAcked[i]);
            if (isAcked[i]) {
                includesAcked = true;
            }
        }
        if (includesAcked) {
            RMStore store = createMock(RMStore.class);
            when(manager.getStore()).thenReturn(store);
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
        T ret = mock(toMock);
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
