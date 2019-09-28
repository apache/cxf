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
import java.util.Collection;
import java.util.List;
import java.util.Timer;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.rm.RMConfiguration.DeliveryAssurance;
import org.apache.cxf.ws.rm.manager.AcksPolicyType;
import org.apache.cxf.ws.rm.manager.DestinationPolicyType;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.ObjectFactory;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement.AcknowledgementRange;
import org.apache.cxf.ws.rm.v200702.SequenceType;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DestinationSequenceTest {

    private IMocksControl control;
    private ObjectFactory factory;
    private Identifier id;
    private EndpointReferenceType ref;
    private Destination destination;
    private RMManager manager;
    private RMEndpoint endpoint;
    private RMConfiguration config;
    private AcksPolicyType ap;
    private DestinationPolicyType dp;

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        control.makeThreadSafe(true);
        factory = new ObjectFactory();
        ref = control.createMock(EndpointReferenceType.class);
        id = factory.createIdentifier();
        id.setValue("seq");
    }

    @After
    public void tearDown() {
        ref = null;
        destination = null;
        manager = null;
        config = null;
        dp = null;
        ap = null;

    }

    @Test
    public void testConstructors() {

        Identifier otherId = factory.createIdentifier();
        otherId.setValue("otherSeq");

        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        assertEquals(id, seq.getIdentifier());
        assertEquals(0L, seq.getLastMessageNumber());
        assertSame(ref, seq.getAcksTo());
        assertNotNull(seq.getAcknowledgment());
        assertNotNull(seq.getMonitor());

        SequenceAcknowledgement ack = new SequenceAcknowledgement();
        seq = new DestinationSequence(id, ref, 10L, ack, ProtocolVariation.RM10WSA200408);
        assertEquals(id, seq.getIdentifier());
        assertEquals(10L, seq.getLastMessageNumber());
        assertSame(ref, seq.getAcksTo());
        assertSame(ack, seq.getAcknowledgment());
        assertNotNull(seq.getMonitor());

    }

    @Test
    public void testEqualsAndHashCode() {

        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        DestinationSequence otherSeq = null;
        assertFalse(seq.equals(otherSeq));
        otherSeq = new DestinationSequence(id, ref, destination, ProtocolVariation.RM10WSA200408);
        assertEquals(seq, otherSeq);
        assertEquals(seq.hashCode(), otherSeq.hashCode());
        Identifier otherId = factory.createIdentifier();
        otherId.setValue("otherSeq");
        otherSeq = new DestinationSequence(otherId, ref, destination, ProtocolVariation.RM10WSA200408);
        assertFalse(seq.equals(otherSeq));
        assertNotEquals(seq.hashCode(), otherSeq.hashCode());
    }

    @Test
    public void testGetSetDestination() {
        control.replay();
        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        seq.setDestination(destination);
        assertSame(destination, seq.getDestination());
    }

    @Test
    public void testGetEndpointIdentifier() {
        setUpDestination();
        String name = "abc";
        EasyMock.expect(destination.getName()).andReturn(name);
        control.replay();

        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        assertEquals("Unexpected endpoint identifier", name, seq.getEndpointIdentifier());
        control.verify();
    }

    @Test
    public void testAcknowledgeBasic() throws SequenceFault {
        Timer timer = control.createMock(Timer.class);
        setUpDestination(timer, null);
        Message message1 = setUpMessage("1");
        Message message2 = setUpMessage("2");
        control.replay();

        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        List<AcknowledgementRange> ranges = seq.getAcknowledgment().getAcknowledgementRange();
        assertEquals(0, ranges.size());

        seq.acknowledge(message1);
        assertEquals(1, ranges.size());
        AcknowledgementRange r1 = ranges.get(0);
        assertEquals(1, r1.getLower().intValue());
        assertEquals(1, r1.getUpper().intValue());

        seq.acknowledge(message2);
        assertEquals(1, ranges.size());
        r1 = ranges.get(0);
        assertEquals(1, r1.getLower().intValue());
        assertEquals(2, r1.getUpper().intValue());

        control.verify();
    }

/*    @Test
    public void testAcknowledgeLastMessageNumberExceeded() throws SequenceFault {
        Timer timer = control.createMock(Timer.class);
        RMEndpoint rme = EasyMock.createMock(RMEndpoint.class);
        EasyMock.expect(rme.getEncoderDecoder()).andReturn(EncoderDecoder10Impl.INSTANCE).anyTimes();
        setUpDestination(timer, rme);
        Message message1 = setUpMessage("1");
        Message message2 = setUpMessage("2", true);
        control.replay();

        DestinationSequence seq = new DestinationSequence(id, ref, destination);

        seq.acknowledge(message1);
        seq.setLastMessageNumber(1);
        try {
            seq.acknowledge(message2);
            fail("Expected SequenceFault not thrown.");
        } catch (SequenceFault sf) {
            assertEquals("SequenceTerminated", sf.getSequenceFault().getFaultCode().getLocalPart());
        }

        control.verify();
    }   */

    @Test
    public void testAcknowledgeAppendRange() throws SequenceFault {
        Timer timer = control.createMock(Timer.class);
        setUpDestination(timer, null);
        Message[] messages = new Message [] {
            setUpMessage("1"),
            setUpMessage("2"),
            setUpMessage("5"),
            setUpMessage("4"),
            setUpMessage("6")
        };

        control.replay();

        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        List<AcknowledgementRange> ranges = seq.getAcknowledgment().getAcknowledgementRange();
        for (int i = 0; i < messages.length; i++) {
            seq.acknowledge(messages[i]);
        }
        assertEquals(2, ranges.size());
        AcknowledgementRange r = ranges.get(0);
        assertEquals(1, r.getLower().intValue());
        assertEquals(2, r.getUpper().intValue());
        r = ranges.get(1);
        assertEquals(4, r.getLower().intValue());
        assertEquals(6, r.getUpper().intValue());

        control.verify();
    }

    @Test
    public void testAcknowledgeInsertRange() throws SequenceFault {
        Timer timer = control.createMock(Timer.class);
        setUpDestination(timer, null);
        Message[] messages = new Message [] {
            setUpMessage("1"),
            setUpMessage("2"),
            setUpMessage("9"),
            setUpMessage("10"),
            setUpMessage("4"),
            setUpMessage("9"),
            setUpMessage("2")
        };
        control.replay();

        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        List<AcknowledgementRange> ranges = seq.getAcknowledgment().getAcknowledgementRange();
        for (int i = 0; i < messages.length; i++) {
            seq.acknowledge(messages[i]);
        }

        assertEquals(3, ranges.size());
        AcknowledgementRange r = ranges.get(0);
        assertEquals(1, r.getLower().intValue());
        assertEquals(2, r.getUpper().intValue());
        r = ranges.get(1);
        assertEquals(4, r.getLower().intValue());
        assertEquals(4, r.getUpper().intValue());
        r = ranges.get(2);
        assertEquals(9, r.getLower().intValue());
        assertEquals(10, r.getUpper().intValue());

        control.verify();
    }

    @Test
    public void testAcknowledgePrependRange() throws SequenceFault {
        Timer timer = control.createMock(Timer.class);
        setUpDestination(timer, null);
        Message[] messages = new Message [] {
            setUpMessage("4"),
            setUpMessage("5"),
            setUpMessage("6"),
            setUpMessage("4"),
            setUpMessage("2"),
            setUpMessage("2")
        };
        control.replay();

        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        List<AcknowledgementRange> ranges = seq.getAcknowledgment().getAcknowledgementRange();
        for (int i = 0; i < messages.length; i++) {
            seq.acknowledge(messages[i]);
        }
        assertEquals(2, ranges.size());
        AcknowledgementRange r = ranges.get(0);
        assertEquals(2, r.getLower().intValue());
        assertEquals(2, r.getUpper().intValue());
        r = ranges.get(1);
        assertEquals(4, r.getLower().intValue());
        assertEquals(6, r.getUpper().intValue());

        control.verify();
    }

    @Test
    public void testMerge() {
        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        List<AcknowledgementRange> ranges = seq.getAcknowledgment().getAcknowledgementRange();
        AcknowledgementRange r;
        for (int i = 0; i < 5; i++) {
            r = new AcknowledgementRange();
            r.setLower(Long.valueOf(3 * i + 1));
            r.setUpper(Long.valueOf(3 * i + 3));
            ranges.add(r);
        }
        seq.mergeRanges();
        assertEquals(1, ranges.size());
        r = ranges.get(0);
        assertEquals(Long.valueOf(1), r.getLower());
        assertEquals(Long.valueOf(15), r.getUpper());
        ranges.clear();
        for (int i = 0; i < 5; i++) {
            r = new AcknowledgementRange();
            r.setLower(Long.valueOf(3 * i + 1));
            r.setUpper(Long.valueOf(3 * i + 2));
            ranges.add(r);
        }
        seq.mergeRanges();
        assertEquals(5, ranges.size());
        ranges.clear();
        for (int i = 0; i < 5; i++) {
            if (i != 2) {
                r = new AcknowledgementRange();
                r.setLower(Long.valueOf(3 * i + 1));
                r.setUpper(Long.valueOf(3 * i + 3));
                ranges.add(r);
            }
        }
        seq.mergeRanges();
        assertEquals(2, ranges.size());
        r = ranges.get(0);
        assertEquals(Long.valueOf(1), r.getLower());
        assertEquals(Long.valueOf(6), r.getUpper());
        r = ranges.get(1);
        assertEquals(Long.valueOf(10), r.getLower());
        assertEquals(Long.valueOf(15), r.getUpper());
    }

    @Test
    public void testMonitor() throws SequenceFault, InterruptedException {
        Timer timer = control.createMock(Timer.class);
        setUpDestination(timer, null);
        Message[] messages = new Message[15];
        for (int i = 0; i < messages.length; i++) {
            messages[i] = setUpMessage(Integer.toString(i + 1));
        }
        control.replay();

        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        SequenceMonitor monitor = seq.getMonitor();
        assertNotNull(monitor);
        monitor.setMonitorInterval(500L);

        assertEquals(0, monitor.getMPM());

        for (int i = 0; i < 10; i++) {
            seq.acknowledge(messages[i]);
            Thread.sleep(55L);
        }
        int mpm1 = monitor.getMPM();
        assertTrue("unexpected MPM: " + mpm1, mpm1 > 0);

        for (int i = 10; i < messages.length; i++) {
            seq.acknowledge(messages[i]);
            Thread.sleep(110L);
        }
        int mpm2 = monitor.getMPM();
        assertTrue(mpm2 > 0);
        assertTrue(mpm1 > mpm2);

        control.verify();
    }

    @Test
    public void testAcknowledgeImmediate() throws SequenceFault {
        Timer timer = control.createMock(Timer.class);
        setUpDestination(timer, null);
        Message message = setUpMessage("1");
        control.replay();

        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        assertFalse(seq.sendAcknowledgement());

        seq.acknowledge(message);

        assertTrue(seq.sendAcknowledgement());
        seq.acknowledgmentSent();
        assertFalse(seq.sendAcknowledgement());

        control.verify();
    }

    @Test
    public void testAcknowledgeDeferred() throws SequenceFault, RMException, InterruptedException {
        Timer timer = new Timer();
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        setUpDestination(timer, rme);

        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        Proxy proxy = control.createMock(Proxy.class);
        EasyMock.expect(rme.getProxy()).andReturn(proxy).anyTimes();
        proxy.acknowledge(seq);
        EasyMock.expectLastCall();

        Message[] messages = new Message[] {
            setUpMessage("1"),
            setUpMessage("2"),
            setUpMessage("3")
        };
        control.replay();

        ap.setIntraMessageThreshold(0);
        config.setAcknowledgementInterval(200L);

        assertFalse(seq.sendAcknowledgement());

        for (int i = 0; i < messages.length; i++) {
            seq.acknowledge(messages[i]);
        }

        assertFalse(seq.sendAcknowledgement());

        Thread.sleep(250L);

        assertTrue(seq.sendAcknowledgement());
        seq.acknowledgmentSent();
        assertFalse(seq.sendAcknowledgement());

        control.verify();
    }

    @Test
    public void testCorrelationID() {
        setUpDestination();
        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        String correlationID = "abdc1234";
        assertNull("unexpected correlation ID", seq.getCorrelationID());
        seq.setCorrelationID(correlationID);
        assertEquals("unexpected correlation ID",
                     correlationID,
                     seq.getCorrelationID());
    }

    @Test
    public void testApplyDeliveryAssuranceAtMostOnce() throws RMException {
        setUpDestination();

        long mn = 10;
        SequenceAcknowledgement ack = control.createMock(SequenceAcknowledgement.class);
        List<AcknowledgementRange> ranges = new ArrayList<>();
        AcknowledgementRange r = control.createMock(AcknowledgementRange.class);
        EasyMock.expect(ack.getAcknowledgementRange()).andReturn(ranges);
        config.setDeliveryAssurance(DeliveryAssurance.AT_MOST_ONCE);

        control.replay();
        DestinationSequence ds = new DestinationSequence(id, ref, 0, ack, ProtocolVariation.RM10WSA200408);
        ds.setDestination(destination);
        ds.applyDeliveryAssurance(mn, null);
        control.verify();

        control.reset();
        ranges.add(r);
        EasyMock.expect(destination.getReliableEndpoint()).andReturn(endpoint);
        EasyMock.expect(endpoint.getConfiguration()).andReturn(config).anyTimes();
        EasyMock.expect(ack.getAcknowledgementRange()).andReturn(ranges);
        EasyMock.expect(r.getLower()).andReturn(Long.valueOf(5));
        EasyMock.expect(r.getUpper()).andReturn(Long.valueOf(15));
        control.replay();
        ds.applyDeliveryAssurance(mn, null);
        control.verify();

    }

    @Test
    public void testInOrderWait() throws InterruptedException {
        setUpDestination();
        Message[] messages = new Message[5];
        for (int i = 0; i < messages.length; i++) {
            messages[i] = setUpMessage(Integer.toString(i + 1));
        }

        config.setDeliveryAssurance(DeliveryAssurance.AT_LEAST_ONCE);

        SequenceAcknowledgement ack = factory.createSequenceAcknowledgement();
        List<AcknowledgementRange> ranges = new ArrayList<>();

        final AcknowledgementRange r =
            factory.createSequenceAcknowledgementAcknowledgementRange();
        r.setUpper(Long.valueOf(messages.length));
        ranges.add(r);
        final DestinationSequence ds = new DestinationSequence(id, ref, 0, ack,
            ProtocolVariation.RM10WSA200408);
        ds.setDestination(destination);

        class Acknowledger extends Thread {
            Message message;
            long messageNr;

            Acknowledger(Message m, long mn) {
                message = m;
                messageNr = mn;
            }

            public void run() {
                try {
                    ds.acknowledge(message);
                    ds.applyDeliveryAssurance(messageNr, message);
                } catch (Exception ex) {
                    // ignore
                }
            }
        }

        control.replay();

        Thread[] threads = new Thread[messages.length];
        for (int i = messages.length - 1; i >= 0; i--) {
            threads[i] = new Acknowledger(messages[i], i + 1);
            threads[i].start();
            Thread.sleep(100L);
        }

        boolean timedOut = false;
        for (int i = 0; i < messages.length; i++) {
            try {
                threads[i].join(1000L);
            } catch (InterruptedException ex) {
                timedOut = true;
            }
        }
        assertFalse("timed out waiting for messages to be processed in order", timedOut);

        control.verify();



    }

    @Test
    public void testScheduleSequenceTermination() throws SequenceFault, InterruptedException {
        Timer timer = new Timer();
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(rme.getProxy()).andReturn(control.createMock(Proxy.class)).anyTimes();
        setUpDestination(timer, rme);

        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        destination.terminateSequence(seq, true);
        EasyMock.expectLastCall();

        Message message = setUpMessage("1");

        long arrival = System.currentTimeMillis();
        EasyMock.expect(rme.getLastApplicationMessage()).andReturn(arrival);

        control.replay();
        config.setInactivityTimeout(200L);

        seq.acknowledge(message);

        Thread.sleep(250L);

        control.verify();
    }

    @Test
    public void testSequenceTermination() {
        destination = control.createMock(Destination.class);
        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(destination.getReliableEndpoint()).andReturn(rme);
        DestinationSequence.SequenceTermination st = seq.new SequenceTermination();
        st.updateInactivityTimeout(30000L);
        long lastAppMessage = System.currentTimeMillis() - 30000L;
        EasyMock.expect(rme.getLastControlMessage()).andReturn(0L);
        EasyMock.expect(rme.getLastApplicationMessage()).andReturn(lastAppMessage);
        destination.terminateSequence(seq, true);
        EasyMock.expectLastCall();
        control.replay();
        st.run();
        control.verify();
    }

    @Test
    public void testSequenceTerminationNotNecessary() {
        destination = control.createMock(Destination.class);
        manager = control.createMock(RMManager.class);
        EasyMock.expect(destination.getManager()).andReturn(manager);
        Timer t = new Timer();
        EasyMock.expect(manager.getTimer()).andReturn(t);
        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(destination.getReliableEndpoint()).andReturn(rme).anyTimes();
        DestinationSequence.SequenceTermination st = seq.new SequenceTermination();
        st.updateInactivityTimeout(30000L);
        long lastAppMessage = System.currentTimeMillis() - 1000L;
        EasyMock.expect(rme.getLastControlMessage()).andReturn(0L);
        EasyMock.expect(rme.getLastApplicationMessage()).andReturn(lastAppMessage);
        EasyMock.expectLastCall();
        control.replay();
        st.run();
        control.verify();
    }

    @Test
    public void testCanPiggybackAckOnPartialResponse() {
        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        AttributedURIType uri = control.createMock(AttributedURIType.class);
        EasyMock.expect(ref.getAddress()).andReturn(uri);
        String addr = "http://localhost:9999/reponses";
        EasyMock.expect(uri.getValue()).andReturn(addr);
        control.replay();
        assertFalse(seq.canPiggybackAckOnPartialResponse());
        control.verify();
        control.reset();
        EasyMock.expect(ref.getAddress()).andReturn(uri);
        EasyMock.expect(uri.getValue()).andReturn(Names.WSA_ANONYMOUS_ADDRESS);
        control.replay();
        assertTrue(seq.canPiggybackAckOnPartialResponse());
        control.verify();
    }

    @Test
    public void testPurgeAcknowledged() {
        destination = control.createMock(Destination.class);
        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        manager = control.createMock(RMManager.class);
        EasyMock.expect(destination.getManager()).andReturn(manager);
        RMStore store = control.createMock(RMStore.class);
        EasyMock.expect(manager.getStore()).andReturn(store);
        store.removeMessages(EasyMock.eq(id),
            CastUtils.cast(EasyMock.isA(Collection.class), Long.class), EasyMock.eq(false));
        EasyMock.expectLastCall();
        control.replay();
        seq.purgeAcknowledged(1);
        control.verify();
    }

    @Test
    public void testCancelDeferredAcknowledgements() {
        destination = control.createMock(Destination.class);
        manager = control.createMock(RMManager.class);
        EasyMock.expect(destination.getManager()).andReturn(manager);
        Timer t = new Timer();
        EasyMock.expect(manager.getTimer()).andReturn(t);
        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        control.replay();
        seq.scheduleDeferredAcknowledgement(30000L);
        seq.cancelDeferredAcknowledgments();
        seq.cancelDeferredAcknowledgments();
        t.cancel();
        control.verify();

    }

    @Test
    public void testCancelTermination() {
        destination = control.createMock(Destination.class);
        manager = control.createMock(RMManager.class);
        EasyMock.expect(destination.getManager()).andReturn(manager);
        Timer t = new Timer();
        EasyMock.expect(manager.getTimer()).andReturn(t);
        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        control.replay();
        seq.scheduleSequenceTermination(30000L);
        seq.cancelTermination();
        t.cancel();
        control.verify();
    }

    private void setUpDestination() {
        setUpDestination(null, null);
    }

    private void setUpDestination(Timer timer, RMEndpoint rme) {

        manager = control.createMock(RMManager.class);

        org.apache.cxf.ws.rm.manager.ObjectFactory cfgFactory =
            new org.apache.cxf.ws.rm.manager.ObjectFactory();
        dp = cfgFactory.createDestinationPolicyType();
        ap = cfgFactory.createAcksPolicyType();
        dp.setAcksPolicy(ap);

        config = new RMConfiguration();
        config.setBaseRetransmissionInterval(3000L);
        EasyMock.expect(manager.getConfiguration()).andReturn(config).anyTimes();
        endpoint = rme;
        if (endpoint == null) {
            endpoint = control.createMock(RMEndpoint.class);
        }
        EasyMock.expect(endpoint.getConfiguration()).andReturn(config).anyTimes();

        EasyMock.expect(manager.getDestinationPolicy()).andReturn(dp).anyTimes();
        EasyMock.expect(manager.getStore()).andReturn(null).anyTimes();

        destination = control.createMock(Destination.class);
        EasyMock.expect(destination.getManager()).andReturn(manager).anyTimes();
        EasyMock.expect(destination.getReliableEndpoint()).andReturn(endpoint).anyTimes();

        if (null != timer) {
            EasyMock.expect(manager.getTimer()).andReturn(timer).anyTimes();
        }

    }

    private Message setUpMessage(String messageNr) {
        return setUpMessage(messageNr, false);
    }

    private Message setUpMessage(String messageNr, boolean useuri) {
        Message message = control.createMock(Message.class);
        Exchange exchange = control.createMock(Exchange.class);
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        EasyMock.expect(exchange.getOutMessage()).andReturn(null);
        EasyMock.expect(exchange.getOutFaultMessage()).andReturn(null);
        RMProperties rmps = control.createMock(RMProperties.class);
        EasyMock.expect(message.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).andReturn(rmps);
        SequenceType st = control.createMock(SequenceType.class);
        EasyMock.expect(rmps.getSequence()).andReturn(st);
        Long val = Long.valueOf(messageNr);
        EasyMock.expect(st.getMessageNumber()).andReturn(val);
        if (useuri) {
            EasyMock.expect(rmps.getNamespaceURI()).andReturn(RM10Constants.NAMESPACE_URI);
        }
        return message;
    }
}
