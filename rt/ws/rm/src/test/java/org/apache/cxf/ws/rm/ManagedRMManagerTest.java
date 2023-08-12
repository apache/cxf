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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.wsdl.WSDLConstants;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ManagedRMManagerTest {
    private static final String TEST_URI = "http://nowhere.com/bar/foo";

    private Bus bus;
    private InstrumentationManager im;
    private RMManager manager;
    private Endpoint endpoint;

    @After
    public void tearDown() throws Exception {
        if (bus != null) {
            bus.shutdown(true);
        }
    }

    @Test
    public void testManagedRMManager() throws Exception {
        final SpringBusFactory factory = new SpringBusFactory();
        bus = factory.createBus("org/apache/cxf/ws/rm/managed-manager-bean.xml");
        im = bus.getExtension(InstrumentationManager.class);
        manager = bus.getExtension(RMManager.class);
        endpoint = createTestEndpoint();
        assertNotNull("Instrumentation Manager should not be null", im);
        assertNotNull("RMManager should not be null", manager);

        MBeanServer mbs = im.getMBeanServer();
        assertNotNull("MBeanServer should be available.", mbs);

        ObjectName managerName = RMUtils.getManagedObjectName(manager);
        Set<ObjectInstance> mbset = mbs.queryMBeans(managerName, null);
        assertEquals("ManagedRMManager should be found", 1, mbset.size());

        Object o;
        o = mbs.getAttribute(managerName, "UsingStore");
        assertTrue(o instanceof Boolean);
        assertFalse("Store attribute is false", (Boolean)o);

        o = mbs.invoke(managerName, "getEndpointIdentifiers", null, null);
        assertTrue(o instanceof String[]);
        assertEquals("No Endpoint", 0, ((String[])o).length);

        RMEndpoint rme = createTestRMEndpoint();

        ObjectName endpointName = RMUtils.getManagedObjectName(rme);
        mbset = mbs.queryMBeans(endpointName, null);
        assertEquals("ManagedRMEndpoint should be found", 1, mbset.size());

        o = mbs.invoke(managerName, "getEndpointIdentifiers", null, null);
        assertEquals("One Endpoint", 1, ((String[])o).length);
        assertEquals("Endpoint identifier must match",
                     RMUtils.getEndpointIdentifier(endpoint, bus), ((String[])o)[0]);

        // test some endpoint methods
        o = mbs.getAttribute(endpointName, "Address");
        assertTrue(o instanceof String);
        assertEquals("Endpoint address must match", TEST_URI, o);

        o = mbs.getAttribute(endpointName, "LastApplicationMessage");
        assertNull(o);

        o = mbs.getAttribute(endpointName, "LastControlMessage");
        assertNull(o);

        o = mbs.invoke(endpointName, "getDestinationSequenceIds", null, null);
        assertTrue(o instanceof String[]);
        assertEquals("No sequence", 0, ((String[])o).length);

        o = mbs.invoke(endpointName, "getDestinationSequences", null, null);
        assertTrue(o instanceof CompositeData[]);
        assertEquals("No sequence", 0, ((CompositeData[])o).length);

        o = mbs.invoke(endpointName, "getSourceSequenceIds", new Object[]{true}, new String[]{"boolean"});
        assertTrue(o instanceof String[]);
        assertEquals("No sequence", 0, ((String[])o).length);

        o = mbs.invoke(endpointName, "getSourceSequences", new Object[]{true}, new String[]{"boolean"});
        assertTrue(o instanceof CompositeData[]);
        assertEquals("No sequence", 0, ((CompositeData[])o).length);

        o = mbs.invoke(endpointName, "getDeferredAcknowledgementTotalCount", null, null);
        assertTrue(o instanceof Integer);
        assertEquals("No deferred acks", 0, o);

        o = mbs.invoke(endpointName, "getQueuedMessageTotalCount",
                       new Object[]{true}, new String[]{"boolean"});
        assertTrue(o instanceof Integer);
        assertEquals("No queued messages", 0, o);
    }

    @Test
    public void testManagedRMEndpointGetQueuedCount() throws Exception {
        ManagedRMEndpoint managedEndpoint = createTestManagedRMEndpoint();

        int n = managedEndpoint.getQueuedMessageTotalCount(true);
        assertEquals(3, n);

        n = managedEndpoint.getQueuedMessageCount("seq1", true);
        assertEquals(2, n);
    }

    @Test
    public void testGetUnAcknowledgedMessageIdentifiers() throws Exception {
        ManagedRMEndpoint managedEndpoint = createTestManagedRMEndpoint();

        Long[] numbers = managedEndpoint.getUnAcknowledgedMessageIdentifiers("seq1");
        assertEquals(2, numbers.length);
        assertTrue(2L == numbers[0] && 4L == numbers[1]);
    }

    @Test
    public void testRemoveSequence() throws Exception {
        manager = new RMManager();
        RMEndpoint rme = mock(RMEndpoint.class);
        EndpointReferenceType ref = RMUtils.createReference(TEST_URI);
        Source source = new Source(rme);
        Destination destination = new Destination(rme);

        RetransmissionQueue rq = new TestRetransmissionQueue();
        manager.setRetransmissionQueue(rq);
        manager.initialise();

        SourceSequence ss1 = createTestSourceSequence(source, "seq1", ref,
                                                     ProtocolVariation.RM10WSA200408, new long[]{1L, 1L, 3L, 3L});
        SourceSequence ss3 = createTestSourceSequence(source, "seq3", ref,
                                                     ProtocolVariation.RM10WSA200408, new long[]{1L, 5L});

        when(rme.getManager()).thenReturn(manager);
        when(rme.getSource()).thenReturn(source);
        when(rme.getDestination()).thenReturn(destination);

        setCurrentMessageNumber(ss1, 5L);
        setCurrentMessageNumber(ss3, 5L);
        source.addSequence(ss1);
        source.addSequence(ss3);
        source.setCurrent(ss3);

        ManagedRMEndpoint managedEndpoint = new ManagedRMEndpoint(rme);

        // for those sequences without any unacknowledged messages
        CompositeData cd = managedEndpoint.getSourceSequence("seq3");
        assertNotNull(cd);

        managedEndpoint.removeSourceSequence("seq3");
        try {
            managedEndpoint.getSourceSequence("seq3");
            fail("sequnce not removed");
        } catch (Exception e) {
            // ok
        }

        // for those sequences with some unacknowledged messages
        cd = managedEndpoint.getSourceSequence("seq1");
        assertNotNull(cd);

        try {
            managedEndpoint.removeSourceSequence("seq1");
            fail("sequnce may not be removed");
        } catch (Exception e) {
            // ok
        }
        cd = managedEndpoint.getSourceSequence("seq1");
        assertNotNull(cd);

        managedEndpoint.purgeUnAcknowledgedMessages("seq1");
        managedEndpoint.removeSourceSequence("seq1");

        try {
            managedEndpoint.getSourceSequence("seq1");
            fail("sequnce not removed");
        } catch (Exception e) {
            // ok
        }
    }

    @Test
    public void testGetSourceSequenceAcknowledgedRange() throws Exception {
        ManagedRMEndpoint managedEndpoint = createTestManagedRMEndpoint();

        Long[] ranges = managedEndpoint.getSourceSequenceAcknowledgedRange("seq1");
        assertEquals(4, ranges.length);
        assertTrue(1L == ranges[0] && 1L == ranges[1] && 3L == ranges[2] && 3L == ranges[3]);
    }

    @Test
    public void testGetSourceSequences() throws Exception {
        ManagedRMEndpoint managedEndpoint = createTestManagedRMEndpoint();

        String[] sids = managedEndpoint.getSourceSequenceIds(true);
        assertEquals(2, sids.length);
        assertTrue(("seq1".equals(sids[0]) || "seq1".equals(sids[1]))
                   && ("seq2".equals(sids[0]) || "seq2".equals(sids[1])));

        String sid = managedEndpoint.getCurrentSourceSequenceId();
        assertEquals("seq2", sid);

        CompositeData[] sequences = managedEndpoint.getSourceSequences(true);
        assertEquals(2, sequences.length);
        verifySourceSequence(sequences[0]);
        verifySourceSequence(sequences[1]);
    }

    @Test
    public void testGetDestinationSequences() throws Exception {
        ManagedRMEndpoint managedEndpoint = createTestManagedRMEndpoint();

        String[] sids = managedEndpoint.getDestinationSequenceIds();
        assertEquals(2, sids.length);
        assertTrue(("seq3".equals(sids[0]) || "seq3".equals(sids[1]))
                   && ("seq4".equals(sids[0]) || "seq4".equals(sids[1])));

        CompositeData[] sequences = managedEndpoint.getDestinationSequences();
        assertEquals(2, sequences.length);
        verifyDestinationSequence(sequences[0]);
        verifyDestinationSequence(sequences[1]);
    }

    @Test
    public void testGetRetransmissionStatus() throws Exception {
        ManagedRMEndpoint managedEndpoint = createTestManagedRMEndpoint();
        TestRetransmissionQueue rq = (TestRetransmissionQueue)manager.getRetransmissionQueue();

        CompositeData status = managedEndpoint.getRetransmissionStatus("seq1", 3L);
        assertNull(status);

        status = managedEndpoint.getRetransmissionStatus("seq1", 2L);
        assertNotNull(status);
        verifyRetransmissionStatus(status, 2L, rq.getRetransmissionStatus());
    }

    @Test
    public void testSuspendAndResumeSourceQueue() throws Exception {
        ManagedRMEndpoint managedEndpoint = createTestManagedRMEndpoint();
        TestRetransmissionQueue rq = (TestRetransmissionQueue)manager.getRetransmissionQueue();

        assertFalse(rq.isSuspended("seq1"));

        managedEndpoint.suspendSourceQueue("seq1");
        assertTrue(rq.isSuspended("seq1"));

        managedEndpoint.resumeSourceQueue("seq1");
        assertFalse(rq.isSuspended("seq1"));
    }

    private void verifySourceSequence(CompositeData cd) {
        Object key = cd.get("sequenceId");
        if ("seq1".equals(key)) {
            verifySourceSequence(cd, "seq1", 5L, 2);
        } else if ("seq2".equals(key)) {
            verifySourceSequence(cd, "seq2", 4L, 1);
        } else {
            fail("Unexpected sequence: " + key);
        }
    }

    private void verifySourceSequence(CompositeData cd, String sid, long num, int qsize) {
        assertTrue(sid.equals(cd.get("sequenceId"))
                   && num == ((Long)cd.get("currentMessageNumber")).longValue()
                   && qsize == ((Integer)cd.get("queuedMessageCount")).intValue());
    }

    private void verifyDestinationSequence(CompositeData cd) {
        Object key = cd.get("sequenceId");
        assertTrue("seq3".equals(key) || "seq4".equals(key));
    }

    private void verifyRetransmissionStatus(CompositeData cd, long num, RetryStatus status) {
        assertEquals(num, cd.get("messageNumber"));
        assertEquals(status.getRetries(), cd.get("retries"));
        assertEquals(status.getNext(), cd.get("next"));
        assertEquals(status.getPrevious(), cd.get("previous"));
        assertEquals(status.getNextInterval(), cd.get("nextInterval"));
        assertEquals(status.getBackoff(), cd.get("backOff"));
    }

    private ManagedRMEndpoint createTestManagedRMEndpoint() {
        manager = new RMManager();
        RMEndpoint rme = mock(RMEndpoint.class);
        EndpointReferenceType ref = RMUtils.createReference(TEST_URI);
        Source source = new Source(rme);
        Destination destination = new Destination(rme);

        RetransmissionQueue rq = new TestRetransmissionQueue();
        manager.setRetransmissionQueue(rq);
        manager.initialise();

        List<SourceSequence> sss = createTestSourceSequences(source, ref);
        List<DestinationSequence> dss = createTestDestinationSequences(destination, ref);

        when(rme.getManager()).thenReturn(manager);
        when(rme.getSource()).thenReturn(source);
        when(rme.getDestination()).thenReturn(destination);

        setCurrentMessageNumber(sss.get(0), 5L);
        setCurrentMessageNumber(sss.get(1), 4L);
        source.addSequence(sss.get(0));
        source.addSequence(sss.get(1));

        source.setCurrent(sss.get(1));

        destination.addSequence(dss.get(0));
        destination.addSequence(dss.get(1));
        return new ManagedRMEndpoint(rme);
    }

    private void setCurrentMessageNumber(SourceSequence ss, long num) {
        for (int i = 0; i < num; i++) {
            ss.nextMessageNumber();
        }
    }

    private List<SourceSequence> createTestSourceSequences(Source source,
                                                           EndpointReferenceType to) {
        List<SourceSequence> sss = new ArrayList<>();
        sss.add(createTestSourceSequence(source, "seq1", to,
                                         ProtocolVariation.RM10WSA200408, new long[]{1L, 1L, 3L, 3L}));
        sss.add(createTestSourceSequence(source, "seq2", to,
                                         ProtocolVariation.RM11WSA200508, new long[]{1L, 1L, 3L, 3L}));

        return sss;
    }

    private List<DestinationSequence> createTestDestinationSequences(Destination destination,
                                                                     EndpointReferenceType to) {
        List<DestinationSequence> dss = new ArrayList<>();
        dss.add(createTestDestinationSequence(destination, "seq3", to,
                                              ProtocolVariation.RM10WSA200408, new long[]{1L, 1L, 3L, 3L}));
        dss.add(createTestDestinationSequence(destination, "seq4", to,
                                              ProtocolVariation.RM11WSA200508, new long[]{1L, 1L, 3L, 3L}));

        return dss;
    }

    private SourceSequence createTestSourceSequence(Source source, String sid,
                                                    EndpointReferenceType to,
                                                    ProtocolVariation protocol, long[] acked) {
        Identifier identifier = RMUtils.getWSRMFactory().createIdentifier();
        identifier.setValue(sid);
        SourceSequence ss = new SourceSequence(identifier, protocol);
        ss.setSource(source);
        ss.setTarget(to);
        List<SequenceAcknowledgement.AcknowledgementRange> ranges =
            ss.getAcknowledgement().getAcknowledgementRange();
        for (int i = 0; i < acked.length; i += 2) {
            ranges.add(createAcknowledgementRange(acked[i], acked[i + 1]));
        }
        return ss;
    }

    private DestinationSequence createTestDestinationSequence(Destination destination, String sid,
                                                              EndpointReferenceType to,
                                                              ProtocolVariation protocol, long[] acked) {
        Identifier identifier = RMUtils.getWSRMFactory().createIdentifier();
        identifier.setValue(sid);
        DestinationSequence ds = new DestinationSequence(identifier, to, null, protocol);
        ds.setDestination(destination);

        List<SequenceAcknowledgement.AcknowledgementRange> ranges =
            ds.getAcknowledgment().getAcknowledgementRange();
        for (int i = 0; i < acked.length; i += 2) {
            ranges.add(createAcknowledgementRange(acked[i], acked[i + 1]));
        }
        return ds;
    }

    private SequenceAcknowledgement.AcknowledgementRange createAcknowledgementRange(long l, long u) {
        SequenceAcknowledgement.AcknowledgementRange range =
            new SequenceAcknowledgement.AcknowledgementRange();
        range.setLower(l);
        range.setUpper(u);
        return range;
    }

    private Endpoint createTestEndpoint() throws Exception {
        ServiceInfo svci = new ServiceInfo();
        svci.setName(new QName(TEST_URI, "testService"));
        Service svc = new ServiceImpl(svci);
        SoapBindingInfo binding = new SoapBindingInfo(svci, WSDLConstants.NS_SOAP11);
        binding.setTransportURI(WSDLConstants.NS_SOAP_HTTP_TRANSPORT);
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(TEST_URI);
        ei.setName(new QName(TEST_URI, "testPort"));
        ei.setBinding(binding);
        ei.setService(svci);
        return new EndpointImpl(bus, svc, ei);
    }

    private RMEndpoint createTestRMEndpoint() throws Exception {
        Message message = mock(Message.class);
        Exchange exchange = mock(Exchange.class);

        when(message.getExchange()).thenReturn(exchange);
        when(exchange.getEndpoint()).thenReturn(endpoint);

        return manager.getReliableEndpoint(message);
    }

    private class TestRetransmissionQueue implements RetransmissionQueue {
        private Set<String> suspended = new HashSet<>();
        private RetryStatus status = new TestRetransmissionStatus();
        private Map<String, List<Long>> numlists = new HashMap<>();

        TestRetransmissionQueue() {
            numlists.put("seq1", new ArrayList<>());
            numlists.put("seq2", new ArrayList<>());
            Collections.addAll(numlists.get("seq1"), 2L, 4L);
            Collections.addAll(numlists.get("seq2"), 3L);
        }

        public int countUnacknowledged(SourceSequence seq) {
            final String key = seq.getIdentifier().getValue();
            List<Long> list = numlists.get(key);
            return list != null ? list.size() : 0;
        }

        public boolean isEmpty() {
            return false;
        }

        public void addUnacknowledged(Message message) {
        }

        public void purgeAcknowledged(SourceSequence seq) {
        }

        public void purgeAll(SourceSequence seq) {
            final String key = seq.getIdentifier().getValue();
            List<Long> list = numlists.get(key);
            if (list != null) {
                list.clear();
            }
        }

        public List<Long> getUnacknowledgedMessageNumbers(SourceSequence seq) {
            final String key = seq.getIdentifier().getValue();
            List<Long> list = numlists.get(key);
            return list != null ? list : new ArrayList<>();
        }

        public RetryStatus getRetransmissionStatus(SourceSequence seq, long num) {
            final String key = seq.getIdentifier().getValue();
            List<Long> list = numlists.get(key);
            return list.contains(num) ? status : null;
        }

        public Map<Long, RetryStatus> getRetransmissionStatuses(SourceSequence seq) {
            return null;
        }

        public void start() {
        }

        public void stop(SourceSequence seq) {
        }

        public void suspend(SourceSequence seq) {
            suspended.add(seq.getIdentifier().getValue());
        }

        public void resume(SourceSequence seq) {
            suspended.remove(seq.getIdentifier().getValue());
        }

        boolean isSuspended(String sid) {
            return suspended.contains(sid);
        }

        RetryStatus getRetransmissionStatus() {
            return status;
        }

        public int countUnacknowledged() {
            return numlists.get("seq1").size() + numlists.get("seq2").size();
        }
    }

    private static final class TestRetransmissionStatus implements RetryStatus {
        private long interval = 300000L;
        private Date next = new Date(System.currentTimeMillis() + interval / 2);
        private Date previous = new Date(next.getTime() - interval);

        public Date getNext() {
            return next;
        }

        public Date getPrevious() {
            return previous;
        }

        public int getRetries() {
            return 2;
        }

        public int getMaxRetries() {
            return -1;
        }

        public long getNextInterval() {
            return interval;
        }

        public long getBackoff() {
            return 1L;
        }

        public boolean isPending() {
            return false;
        }

        public boolean isSuspended() {
            return false;
        }
    }
}