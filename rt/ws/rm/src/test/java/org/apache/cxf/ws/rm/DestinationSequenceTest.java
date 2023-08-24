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

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.cxf.ws.addressing.WSAddressingFeature.WSAddressingFeatureApplier;
import org.apache.cxf.ws.rm.RMConfiguration.DeliveryAssurance;
import org.apache.cxf.ws.rm.manager.AcksPolicyType;
import org.apache.cxf.ws.rm.manager.DestinationPolicyType;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.ObjectFactory;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement.AcknowledgementRange;
import org.apache.cxf.ws.rm.v200702.SequenceType;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DestinationSequenceTest {

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
        factory = new ObjectFactory();
        ref = mock(EndpointReferenceType.class);
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
        assertNotEquals(seq, otherSeq);
        otherSeq = new DestinationSequence(id, ref, destination, ProtocolVariation.RM10WSA200408);
        assertEquals(seq, otherSeq);
        assertEquals(seq.hashCode(), otherSeq.hashCode());
        Identifier otherId = factory.createIdentifier();
        otherId.setValue("otherSeq");
        otherSeq = new DestinationSequence(otherId, ref, destination, ProtocolVariation.RM10WSA200408);
        assertNotEquals(seq, otherSeq);
        assertNotEquals(seq.hashCode(), otherSeq.hashCode());
    }

    @Test
    public void testGetSetDestination() {
        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        seq.setDestination(destination);
        assertSame(destination, seq.getDestination());
    }

    @Test
    public void testGetEndpointIdentifier() {
        setUpDestination();
        String name = "abc";
        when(destination.getName()).thenReturn(name);

        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        assertEquals("Unexpected endpoint identifier", name, seq.getEndpointIdentifier());
    }

    @Test
    public void testAcknowledgeBasic() throws SequenceFault {
        Timer timer = mock(Timer.class);
        setUpDestination(timer, null);
        Message message1 = setUpMessage("1");
        Message message2 = setUpMessage("2");

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
    }

/*    @Test
    public void testAcknowledgeLastMessageNumberExceeded() throws SequenceFault {
        Timer timer = mock(Timer.class);
        RMEndpoint rme = mock(RMEndpoint.class);
        when(rme.getEncoderDecoder()).thenReturn(EncoderDecoder10Impl.INSTANCE);
        setUpDestination(timer, rme);
        Message message1 = setUpMessage("1");
        Message message2 = setUpMessage("2", true);

        DestinationSequence seq = new DestinationSequence(id, ref, destination);

        seq.acknowledge(message1);
        seq.setLastMessageNumber(1);
        try {
            seq.acknowledge(message2);
            fail("Expected SequenceFault not thrown.");
        } catch (SequenceFault sf) {
            assertEquals("SequenceTerminated", sf.getSequenceFault().getFaultCode().getLocalPart());
        }
    }   */

    @Test
    public void testAcknowledgeAppendRange() throws SequenceFault {
        Timer timer = mock(Timer.class);
        setUpDestination(timer, null);
        Message[] messages = new Message [] {
            setUpMessage("1"),
            setUpMessage("2"),
            setUpMessage("5"),
            setUpMessage("4"),
            setUpMessage("6")
        };

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
    }

    @Test
    public void testAcknowledgeInsertRange() throws SequenceFault {
        Timer timer = mock(Timer.class);
        setUpDestination(timer, null);

        Bus bus = mock(Bus.class);
        when(manager.getBus()).thenReturn(bus);

        Message[] messages = new Message [] {
            setUpMessage("1"),
            setUpMessage("2"),
            setUpMessage("9"),
            setUpMessage("10"),
            setUpMessage("4"),
            setUpMessage("9"),
            setUpMessage("2")
        };

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
    }

    @Test
    public void testAcknowledgePrependRange() throws SequenceFault {
        Timer timer = mock(Timer.class);
        setUpDestination(timer, null);
        Message[] messages = new Message [] {
            setUpMessage("4"),
            setUpMessage("5"),
            setUpMessage("6"),
            setUpMessage("4"),
            setUpMessage("2"),
            setUpMessage("2")
        };

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
        Timer timer = mock(Timer.class);
        setUpDestination(timer, null);
        Message[] messages = new Message[15];
        for (int i = 0; i < messages.length; i++) {
            messages[i] = setUpMessage(Integer.toString(i + 1));
        }

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
    }

    @Test
    public void testAcknowledgeImmediate() throws SequenceFault {
        Timer timer = mock(Timer.class);
        setUpDestination(timer, null);
        Message message = setUpMessage("1");

        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        assertFalse(seq.sendAcknowledgement());

        seq.acknowledge(message);

        assertTrue(seq.sendAcknowledgement());
        seq.acknowledgmentSent();
        assertFalse(seq.sendAcknowledgement());
    }

    @Test
    public void testAcknowledgeDeferred() throws SequenceFault, RMException, InterruptedException {
        Timer timer = new Timer();
        RMEndpoint rme = mock(RMEndpoint.class);
        setUpDestination(timer, rme);

        Bus bus = mock(Bus.class);
        when(manager.getBus()).thenReturn(bus);

        when(bus.getExtension(WSAddressingFeatureApplier.class))
        .thenReturn(new WSAddressingFeatureApplier() {
            @Override
            public void initializeProvider(WSAddressingFeature feature, InterceptorProvider provider, Bus bus) {
            }
        });

        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        
        AttributedURIType uri = mock(AttributedURIType.class);
        when(ref.getAddress()).thenReturn(uri);

        Proxy proxy = spy(new Proxy(rme));
        when(rme.getProxy()).thenReturn(proxy);
        doCallRealMethod().when(proxy).acknowledge(seq);
        when(destination.getReliableEndpoint()).thenReturn(endpoint);

        InterfaceInfo ii = mock(InterfaceInfo.class);
        when(ii.getOperation(ProtocolVariation.RM10WSA200408.getConstants().getSequenceAckOperationName()))
            .thenReturn(new OperationInfo());

        ServiceInfo si = new ServiceInfo();
        si.setInterface(ii);

        BindingInfo bi = new BindingInfo(si, null);
        Endpoint ae = mock(Endpoint.class);
        when(ae.getEndpointInfo()).thenReturn(new EndpointInfo(si, RM10Constants.NAMESPACE_URI));
        when(endpoint.getBindingInfo(ProtocolVariation.RM10WSA200408)).thenReturn(bi);
        when(endpoint.getEndpoint(ProtocolVariation.RM10WSA200408)).thenReturn(ae);
        when(endpoint.getManager()).thenReturn(manager);

        Message[] messages = new Message[] {
            setUpMessage("1"),
            setUpMessage("2"),
            setUpMessage("3")
        };

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
        SequenceAcknowledgement ack = mock(SequenceAcknowledgement.class);
        List<AcknowledgementRange> ranges = new ArrayList<>();
        AcknowledgementRange r = mock(AcknowledgementRange.class);
        when(ack.getAcknowledgementRange()).thenReturn(ranges);
        config.setDeliveryAssurance(DeliveryAssurance.AT_MOST_ONCE);

        DestinationSequence ds = new DestinationSequence(id, ref, 0, ack, ProtocolVariation.RM10WSA200408);
        ds.setDestination(destination);
        ds.applyDeliveryAssurance(mn, null);

        ranges.add(r);
        when(destination.getReliableEndpoint()).thenReturn(endpoint);
        when(endpoint.getConfiguration()).thenReturn(config);
        when(ack.getAcknowledgementRange()).thenReturn(ranges);
        when(r.getLower()).thenReturn(Long.valueOf(5));
        when(r.getUpper()).thenReturn(Long.valueOf(15));

        ds.applyDeliveryAssurance(mn, null);
    }

    @Test
    public void testInOrderWait() throws InterruptedException {
        setUpDestination();
        
        Bus bus = mock(Bus.class);
        when(manager.getBus()).thenReturn(bus);

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
    }

    @Test
    public void testScheduleSequenceTermination() throws SequenceFault, InterruptedException {
        Timer timer = new Timer();
        RMEndpoint rme = mock(RMEndpoint.class);
        when(rme.getProxy()).thenReturn(mock(Proxy.class));
        setUpDestination(timer, rme);

        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        doCallRealMethod().when(destination).terminateSequence(seq, true);

        Message message = setUpMessage("1");

        long arrival = System.currentTimeMillis();
        when(rme.getLastApplicationMessage()).thenReturn(arrival);

        config.setInactivityTimeout(200L);

        seq.acknowledge(message);

        Thread.sleep(250L);
    }

    @Test
    public void testSequenceTermination() {
        destination = mock(Destination.class);
        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        RMEndpoint rme = mock(RMEndpoint.class);
        when(destination.getReliableEndpoint()).thenReturn(rme);
        manager = mock(RMManager.class);
        RMStore store = mock(RMStore.class);
        when(manager.getStore()).thenReturn(store);
        when(destination.getManager()).thenReturn(manager);
        DestinationSequence.SequenceTermination st = seq.new SequenceTermination();
        st.updateInactivityTimeout(30000L);
        long lastAppMessage = System.currentTimeMillis() - 30000L;
        when(rme.getLastControlMessage()).thenReturn(0L);
        when(rme.getLastApplicationMessage()).thenReturn(lastAppMessage);
        doCallRealMethod().when(destination).terminateSequence(seq, true);

        st.run();
    }

    @Test
    public void testSequenceTerminationNotNecessary() {
        destination = mock(Destination.class);
        manager = mock(RMManager.class);
        when(destination.getManager()).thenReturn(manager);
        Timer t = new Timer();
        when(manager.getTimer()).thenReturn(t);
        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        RMEndpoint rme = mock(RMEndpoint.class);
        when(destination.getReliableEndpoint()).thenReturn(rme);
        DestinationSequence.SequenceTermination st = seq.new SequenceTermination();
        st.updateInactivityTimeout(30000L);
        long lastAppMessage = System.currentTimeMillis() - 1000L;
        when(rme.getLastControlMessage()).thenReturn(0L);
        when(rme.getLastApplicationMessage()).thenReturn(lastAppMessage);

        st.run();
    }

    @Test
    public void testCanPiggybackAckOnPartialResponse() {
        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        AttributedURIType uri = mock(AttributedURIType.class);
        when(ref.getAddress()).thenReturn(uri);
        String addr = "http://localhost:9999/reponses";
        when(uri.getValue()).thenReturn(addr);
        assertFalse(seq.canPiggybackAckOnPartialResponse());

        when(ref.getAddress()).thenReturn(uri);
        when(uri.getValue()).thenReturn(Names.WSA_ANONYMOUS_ADDRESS);
        assertTrue(seq.canPiggybackAckOnPartialResponse());
    }

    @Test
    public void testPurgeAcknowledged() {
        destination = mock(Destination.class);
        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);
        manager = mock(RMManager.class);
        when(destination.getManager()).thenReturn(manager);
        RMStore store = mock(RMStore.class);
        when(manager.getStore()).thenReturn(store);

        seq.purgeAcknowledged(1);
        verify(store).removeMessages(eq(id),
             CastUtils.cast(isA(Collection.class), Long.class), eq(false));
    }

    @Test
    public void testCancelDeferredAcknowledgements() {
        destination = mock(Destination.class);
        manager = mock(RMManager.class);
        when(destination.getManager()).thenReturn(manager);
        Timer t = new Timer();
        when(manager.getTimer()).thenReturn(t);
        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);

        seq.scheduleDeferredAcknowledgement(30000L);
        seq.cancelDeferredAcknowledgments();
        seq.cancelDeferredAcknowledgments();
        t.cancel();
    }

    @Test
    public void testCancelTermination() {
        destination = mock(Destination.class);
        manager = mock(RMManager.class);
        when(destination.getManager()).thenReturn(manager);
        Timer t = new Timer();
        when(manager.getTimer()).thenReturn(t);
        DestinationSequence seq = new DestinationSequence(id, ref, destination,
            ProtocolVariation.RM10WSA200408);

        seq.scheduleSequenceTermination(30000L);
        seq.cancelTermination();
        t.cancel();
    }

    private void setUpDestination() {
        setUpDestination(null, null);
    }

    private void setUpDestination(Timer timer, RMEndpoint rme) {

        manager = mock(RMManager.class);

        org.apache.cxf.ws.rm.manager.ObjectFactory cfgFactory =
            new org.apache.cxf.ws.rm.manager.ObjectFactory();
        dp = cfgFactory.createDestinationPolicyType();
        ap = cfgFactory.createAcksPolicyType();
        dp.setAcksPolicy(ap);

        config = new RMConfiguration();
        config.setBaseRetransmissionInterval(3000L);
        when(manager.getConfiguration()).thenReturn(config);
        endpoint = rme;
        if (endpoint == null) {
            endpoint = mock(RMEndpoint.class);
        }
        when(endpoint.getConfiguration()).thenReturn(config);

        when(manager.getDestinationPolicy()).thenReturn(dp);
        when(manager.getStore()).thenReturn(null);

        destination = mock(Destination.class);
        when(destination.getManager()).thenReturn(manager);
        when(destination.getReliableEndpoint()).thenReturn(endpoint);

        if (null != timer) {
            when(manager.getTimer()).thenReturn(timer);
        }

    }

    private Message setUpMessage(String messageNr) {
        return setUpMessage(messageNr, false);
    }

    private Message setUpMessage(String messageNr, boolean useuri) {
        Message message = mock(Message.class);
        Exchange exchange = mock(Exchange.class);
        when(message.getExchange()).thenReturn(exchange);
        when(exchange.getOutMessage()).thenReturn(null);
        when(exchange.getOutFaultMessage()).thenReturn(null);
        RMProperties rmps = mock(RMProperties.class);
        when(message.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).thenReturn(rmps);
        SequenceType st = mock(SequenceType.class);
        when(rmps.getSequence()).thenReturn(st);
        Long val = Long.valueOf(messageNr);
        when(st.getMessageNumber()).thenReturn(val);
        if (useuri) {
            when(rmps.getNamespaceURI()).thenReturn(RM10Constants.NAMESPACE_URI);
        }
        return message;
    }
}
