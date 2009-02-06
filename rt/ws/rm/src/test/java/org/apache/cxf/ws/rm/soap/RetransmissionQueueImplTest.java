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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;

import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.rm.Identifier;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.RMMessageConstants;
import org.apache.cxf.ws.rm.RMProperties;
import org.apache.cxf.ws.rm.SequenceType;
import org.apache.cxf.ws.rm.SourceSequence;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.policy.RMAssertion;
import org.easymock.IMocksControl;
import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * Test resend logic.
 */
public class RetransmissionQueueImplTest extends Assert {

    private IMocksControl control;
    private RMManager manager;
    private Executor executor;
    private RetransmissionQueueImpl queue;
    private TestResender resender;
    private List<Message> messages =
        new ArrayList<Message>();
    private List<RMProperties> properties =
        new ArrayList<RMProperties>();
    private List<SequenceType> sequences =
        new ArrayList<SequenceType>();
    private List<Identifier> identifiers =
        new ArrayList<Identifier>();
    private List<Object> mocks =
        new ArrayList<Object>();
    private RMAssertion rma;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        manager = createMock(RMManager.class);
        queue = new RetransmissionQueueImpl(manager);
        resender = new TestResender();
        queue.replaceResender(resender);
        executor = createMock(Executor.class);
        rma = createMock(RMAssertion.class);
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
        Message message = createMock(Message.class);
        setupMessagePolicies(message);
        control.replay();
        long now = System.currentTimeMillis();
        RetransmissionQueueImpl.ResendCandidate candidate = queue.createResendCandidate(message);
        assertSame(message, candidate.getMessage());
        assertEquals(0, candidate.getResends());
        Date refDate = new Date(now + 5000);
        assertTrue(!candidate.getNext().before(refDate));
        refDate = new Date(now + 7000);
        assertTrue(!candidate.getNext().after(refDate));
        assertTrue(!candidate.isPending());
    }
    
    @Test
    public void testResendCandidateAttempted() {
        Message message = createMock(Message.class);
        setupMessagePolicies(message);
        ready(true);
        long now = System.currentTimeMillis();
        RetransmissionQueueImpl.ResendCandidate candidate = queue.createResendCandidate(message);
        candidate.attempted();
        assertEquals(1, candidate.getResends());
        Date refDate = new Date(now + 15000);
        assertTrue(!candidate.getNext().before(refDate));
        refDate = new Date(now + 17000);
        assertTrue(!candidate.getNext().after(refDate));
        assertTrue(!candidate.isPending());        
    }
    
    @Test
    public void testCacheUnacknowledged() {
        Message message1 = setUpMessage("sequence1");
        Message message2 = setUpMessage("sequence2");
        Message message3 = setUpMessage("sequence1");
        
        setupMessagePolicies(message1);
        setupMessagePolicies(message2);
        setupMessagePolicies(message3);
        
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
        BigInteger[] messageNumbers = {BigInteger.TEN, BigInteger.ONE};
        SourceSequence sequence = setUpSequence("sequence1",
                                          messageNumbers, 
                                          new boolean[] {true, false});
        List<RetransmissionQueueImpl.ResendCandidate> sequenceList =
            new ArrayList<RetransmissionQueueImpl.ResendCandidate>();
        queue.getUnacknowledged().put("sequence1", sequenceList);
        Message message1 =
            setUpMessage("sequence1", messageNumbers[0]);
        setupMessagePolicies(message1);        
        Message message2 =
            setUpMessage("sequence1", messageNumbers[1]);
        setupMessagePolicies(message2);
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
        BigInteger[] messageNumbers = {BigInteger.TEN, BigInteger.ONE};
        SourceSequence sequence = setUpSequence("sequence1",
                                           messageNumbers, 
                                           new boolean[] {false, false});
        List<RetransmissionQueueImpl.ResendCandidate> sequenceList =
            new ArrayList<RetransmissionQueueImpl.ResendCandidate>();
        queue.getUnacknowledged().put("sequence1", sequenceList);
        Message message1 =
            setUpMessage("sequence1", messageNumbers[0]);
        setupMessagePolicies(message1);        
        Message message2 =
            setUpMessage("sequence1", messageNumbers[1]);
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
        BigInteger[] messageNumbers = {BigInteger.TEN, BigInteger.ONE};
        SourceSequence sequence = setUpSequence("sequence1",
                                          messageNumbers,
                                          new boolean[] {true, true});
        List<RetransmissionQueueImpl.ResendCandidate> sequenceList =
            new ArrayList<RetransmissionQueueImpl.ResendCandidate>();
        queue.getUnacknowledged().put("sequence1", sequenceList);
        Message message1 =
            setUpMessage("sequence1", messageNumbers[0]);
        setupMessagePolicies(message1);
        Message message2 =
            setUpMessage("sequence1", messageNumbers[1]);
        setupMessagePolicies(message2);
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
        assertTrue("queue is not empty" , queue.isEmpty());
    }

    @Test
    public void testCountUnacknowledged() {
        BigInteger[] messageNumbers = {BigInteger.TEN, BigInteger.ONE};
        SourceSequence sequence = setUpSequence("sequence1",
                                          messageNumbers, 
                                          null);
        List<RetransmissionQueueImpl.ResendCandidate> sequenceList =
            new ArrayList<RetransmissionQueueImpl.ResendCandidate>();
        
        queue.getUnacknowledged().put("sequence1", sequenceList);
        Message message1 =
            setUpMessage("sequence1", messageNumbers[0], false);
        setupMessagePolicies(message1);        
        Message message2 =
            setUpMessage("sequence1", messageNumbers[1], false);
        setupMessagePolicies(message2);
        ready(false);
        
        sequenceList.add(queue.createResendCandidate(message1));
        sequenceList.add(queue.createResendCandidate(message2));

        assertEquals("unexpected unacked count", 
                     2,
                     queue.countUnacknowledged(sequence));
        assertTrue("queue is empty", !queue.isEmpty());
    }
    
    @Test
    public void testCountUnacknowledgedUnknownSequence() {
        BigInteger[] messageNumbers = {BigInteger.TEN, BigInteger.ONE};
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
    
    private Message setUpMessage(String sid) {
        return setUpMessage(sid, null);
    }

    private Message setUpMessage(String sid,
                                        BigInteger messageNumber) {
        return setUpMessage(sid, messageNumber, true);
    }

    private Message setUpMessage(String sid,
                                        BigInteger messageNumber,
                                        boolean storeSequence) {
        Message message =
            createMock(Message.class);
        if (storeSequence) {
            setUpSequenceType(message, sid, messageNumber);
        }
        messages.add(message);
        
        return message;
    }
    
    private void setupMessagePolicies(Message message) {
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(null);
        EasyMock.expect(manager.getRMAssertion()).andReturn(rma);
        RMAssertion.BaseRetransmissionInterval bri = 
            createMock(RMAssertion.BaseRetransmissionInterval.class);
        EasyMock.expect(rma.getBaseRetransmissionInterval()).andReturn(bri);
        EasyMock.expect(bri.getMilliseconds()).andReturn(new BigInteger("5000"));
        RMAssertion.ExponentialBackoff eb = createMock(RMAssertion.ExponentialBackoff.class);
        EasyMock.expect(rma.getExponentialBackoff()).andReturn(eb);        
    }
    

    private void ready(boolean doStart) {
        control.replay();
        if (doStart) {
            queue.start();
        }
    }
    
    private SequenceType setUpSequenceType(Message message,
                                           String sid,
                                           BigInteger messageNumber) {
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
            sequence.getMessageNumber();
            EasyMock.expectLastCall().andReturn(messageNumber);
        } else {
            Identifier id = createMock(Identifier.class);
            sequence.getIdentifier();
            EasyMock.expectLastCall().andReturn(id);
            id.getValue();
            EasyMock.expectLastCall().andReturn(sid);
            identifiers.add(id);
        }
        sequences.add(sequence);
        return sequence;
    }
    
    private SourceSequence setUpSequence(String sid, 
                                   BigInteger[] messageNumbers,
                                   boolean[] isAcked) {
        SourceSequence sequence = createMock(SourceSequence.class);
        Identifier id = createMock(Identifier.class);
        sequence.getIdentifier();
        EasyMock.expectLastCall().andReturn(id);
        id.getValue();
        EasyMock.expectLastCall().andReturn(sid);
        identifiers.add(id);
        boolean includesAcked = false;
        for (int i = 0; isAcked != null && i < isAcked.length; i++) {
            sequence.isAcknowledged(messageNumbers[i]);
            EasyMock.expectLastCall().andReturn(isAcked[i]);
            if (isAcked[i]) {
                includesAcked = true;
            }
        }
        if (includesAcked) {
            // Will be called once or twice depending on whether any more
            // unacknowledged messages are left for this sequence
            sequence.getIdentifier();
            EasyMock.expectLastCall().andReturn(id).times(1, 2);

            // Would be called only when there are no more
            // unacknowledged messages left for this sequence
            id.getValue();
            EasyMock.expectLastCall().andReturn(sid).times(0, 1);

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
    
    private static class TestResender implements RetransmissionQueueImpl.Resender {
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
