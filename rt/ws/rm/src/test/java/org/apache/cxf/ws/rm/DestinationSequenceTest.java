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
import java.util.Timer;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.addressing.v200408.AttributedURI;
import org.apache.cxf.ws.addressing.v200408.EndpointReferenceType;
import org.apache.cxf.ws.rm.SequenceAcknowledgement.AcknowledgementRange;
import org.apache.cxf.ws.rm.manager.AcksPolicyType;
import org.apache.cxf.ws.rm.manager.DeliveryAssuranceType;
import org.apache.cxf.ws.rm.manager.DestinationPolicyType;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.policy.RMAssertion;
import org.apache.cxf.ws.rm.policy.RMAssertion.AcknowledgementInterval;
import org.apache.cxf.ws.rm.policy.RMAssertion.BaseRetransmissionInterval;
import org.apache.cxf.ws.rm.policy.RMAssertion.InactivityTimeout;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DestinationSequenceTest extends Assert {

    private IMocksControl control;
    private ObjectFactory factory;
    private Identifier id;
    private EndpointReferenceType ref;
    private Destination destination;
    private RMManager manager;
    private RMAssertion rma;
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
        rma = null;
        dp = null;
        ap = null;
        
    }

    @Test
    public void testConstructors() {
  
        Identifier otherId = factory.createIdentifier();
        otherId.setValue("otherSeq");
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        assertEquals(id, seq.getIdentifier());
        assertNull(seq.getLastMessageNumber());
        assertSame(ref, seq.getAcksTo());
        assertNotNull(seq.getAcknowledgment());
        assertNotNull(seq.getMonitor());   
        
        SequenceAcknowledgement ack = RMUtils.getWSRMFactory().createSequenceAcknowledgement();        
        seq = new DestinationSequence(id, ref, BigInteger.TEN, ack);
        assertEquals(id, seq.getIdentifier());
        assertEquals(BigInteger.TEN, seq.getLastMessageNumber());
        assertSame(ref, seq.getAcksTo());
        assertSame(ack, seq.getAcknowledgment());
        assertNotNull(seq.getMonitor());  

    }
    
    @Test
    public void testEqualsAndHashCode() {     
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        DestinationSequence otherSeq = null;
        assertTrue(!seq.equals(otherSeq));
        otherSeq = new DestinationSequence(id, ref, destination);
        assertEquals(seq, otherSeq);
        assertEquals(seq.hashCode(), otherSeq.hashCode());
        Identifier otherId = factory.createIdentifier();
        otherId.setValue("otherSeq");
        otherSeq = new DestinationSequence(otherId, ref, destination);
        assertTrue(!seq.equals(otherSeq));
        assertTrue(seq.hashCode() != otherSeq.hashCode()); 
        assertTrue(!seq.equals(this));
    }
    
    @Test
    public void testGetSetDestination() {
        control.replay();
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        seq.setDestination(destination);
        assertSame(destination, seq.getDestination());
    }
    
    @Test
    public void testGetEndpointIdentifier() {
        setUpDestination();
        String name = "abc";
        EasyMock.expect(destination.getName()).andReturn(name);
        control.replay();
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        assertEquals("Unexpected endpoint identifier", name, seq.getEndpointIdentifier());
        control.verify();
    }
    
    @Test
    public void testAcknowledgeBasic() throws SequenceFault {
        setUpDestination();
        Message message1 = setUpMessage("1");
        Message message2 = setUpMessage("2");
        control.replay();
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
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
    
    @Test
    public void testAcknowledgeLastMessageNumberExceeded() throws SequenceFault {  
        setUpDestination();
        Message message1 = setUpMessage("1");
        Message message2 = setUpMessage("2");
        control.replay();
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        
        seq.acknowledge(message1);
        seq.setLastMessageNumber(BigInteger.ONE);
        try {
            seq.acknowledge(message2);
            fail("Expected SequenceFault not thrown.");
        } catch (SequenceFault sf) {
            assertEquals("LastMessageNumberExceeded", sf.getSequenceFault().getFaultCode().getLocalPart());
        }
        
        control.verify();
    }
    
    @Test
    public void testAcknowledgeAppendRange() throws SequenceFault {
        setUpDestination();
        Message[] messages = new Message [] {
            setUpMessage("1"),
            setUpMessage("2"),
            setUpMessage("5"),
            setUpMessage("4"),
            setUpMessage("6")
        };

        control.replay();
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
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
        setUpDestination();
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
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
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
        setUpDestination();
        Message[] messages = new Message [] {
            setUpMessage("4"),
            setUpMessage("5"),
            setUpMessage("6"),
            setUpMessage("4"),
            setUpMessage("2"),
            setUpMessage("2")
        };
        control.replay();
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
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
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        List<AcknowledgementRange> ranges = seq.getAcknowledgment().getAcknowledgementRange();
        AcknowledgementRange r;
        for (int i = 0; i < 5; i++) {
            r = new AcknowledgementRange();
            r.setLower(new BigInteger(Integer.toString(3 * i + 1)));
            r.setUpper(new BigInteger(Integer.toString(3 * i + 3)));
            ranges.add(r);
        }
        seq.mergeRanges();
        assertEquals(1, ranges.size());
        r = ranges.get(0);
        assertEquals(BigInteger.ONE, r.getLower());
        assertEquals(new BigInteger("15"), r.getUpper());
        ranges.clear();
        for (int i = 0; i < 5; i++) {
            r = new AcknowledgementRange();
            r.setLower(new BigInteger(Integer.toString(3 * i + 1)));
            r.setUpper(new BigInteger(Integer.toString(3 * i + 2)));
            ranges.add(r);
        }
        seq.mergeRanges();
        assertEquals(5, ranges.size());
        ranges.clear();
        for (int i = 0; i < 5; i++) {
            if (i != 2) {
                r = new AcknowledgementRange();
                r.setLower(new BigInteger(Integer.toString(3 * i + 1)));
                r.setUpper(new BigInteger(Integer.toString(3 * i + 3)));
                ranges.add(r);
            }
        }
        seq.mergeRanges();
        assertEquals(2, ranges.size());
        r = ranges.get(0);
        assertEquals(BigInteger.ONE, r.getLower());
        assertEquals(new BigInteger("6"), r.getUpper());
        r = ranges.get(1);
        assertEquals(BigInteger.TEN, r.getLower());
        assertEquals(new BigInteger("15"), r.getUpper());        
    }
    
    @Test
    public void testMonitor() throws SequenceFault {
        setUpDestination();
        Message[] messages = new Message[15];
        for (int i = 0; i < messages.length; i++) {
            messages[i] = setUpMessage(Integer.toString(i + 1));
        }
        control.replay();
                
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        SequenceMonitor monitor = seq.getMonitor();
        assertNotNull(monitor);
        monitor.setMonitorInterval(500);
        
        assertEquals(0, monitor.getMPM());
        
        for (int i = 0; i < 10; i++) {
            seq.acknowledge(messages[i]);
            try {
                Thread.sleep(55);
            } catch (InterruptedException ex) {
                // ignore
            }
        }
        int mpm1 = monitor.getMPM();
        assertTrue("unexpected MPM: " + mpm1, mpm1 > 0);
        
        for (int i = 10; i < messages.length; i++) {
            seq.acknowledge(messages[i]);
            try {
                Thread.sleep(110);
            } catch (InterruptedException ex) {
                // ignore
            }
        }
        int mpm2 = monitor.getMPM();
        assertTrue(mpm2 > 0);
        assertTrue(mpm1 > mpm2);
        
        control.verify();
    }
    
    @Test
    public void testAcknowledgeImmediate() throws SequenceFault {
        setUpDestination();
        Message message = setUpMessage("1");
        control.replay();
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        assertTrue(!seq.sendAcknowledgement());
              
        seq.acknowledge(message); 
        
        assertTrue(seq.sendAcknowledgement());
        seq.acknowledgmentSent();
        assertFalse(seq.sendAcknowledgement());
        
        control.verify();
    }
    
    @Test
    public void testAcknowledgeDeferred() throws SequenceFault, RMException {
        Timer timer = new Timer();
        setUpDestination(timer);
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(destination.getReliableEndpoint()).andReturn(rme).anyTimes();
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
        AcknowledgementInterval ai = new org.apache.cxf.ws.rm.policy.ObjectFactory()
            .createRMAssertionAcknowledgementInterval();
        ai.setMilliseconds(new BigInteger("200"));
        rma.setAcknowledgementInterval(ai);        

        assertTrue(!seq.sendAcknowledgement());   
              
        for (int i = 0; i < messages.length; i++) {
            seq.acknowledge(messages[i]);
        }
        
        assertFalse(seq.sendAcknowledgement());
        
        try {
            Thread.sleep(250);
        } catch (InterruptedException ex) {
            // ignore
        }
        assertTrue(seq.sendAcknowledgement());
        seq.acknowledgmentSent();
        assertFalse(seq.sendAcknowledgement());
        
        control.verify();
    }
    
    @Test
    public void testCorrelationID() {
        setUpDestination();
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
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
        
        BigInteger mn = BigInteger.TEN;        
        SequenceAcknowledgement ack = control.createMock(SequenceAcknowledgement.class);
        List<AcknowledgementRange> ranges = new ArrayList<AcknowledgementRange>();
        AcknowledgementRange r = control.createMock(AcknowledgementRange.class);
        EasyMock.expect(ack.getAcknowledgementRange()).andReturn(ranges);
        DeliveryAssuranceType da = control.createMock(DeliveryAssuranceType.class);
        EasyMock.expect(manager.getDeliveryAssurance()).andReturn(da);
        EasyMock.expect(da.isSetAtMostOnce()).andReturn(true);                    
        
        control.replay();        
        DestinationSequence ds = new DestinationSequence(id, ref, null, ack);
        ds.setDestination(destination);
        ds.applyDeliveryAssurance(mn);
        control.verify();
        
        control.reset();
        ranges.add(r);
        EasyMock.expect(destination.getManager()).andReturn(manager);
        EasyMock.expect(manager.getDeliveryAssurance()).andReturn(da);
        EasyMock.expect(da.isSetAtMostOnce()).andReturn(true);            
        EasyMock.expect(ack.getAcknowledgementRange()).andReturn(ranges);
        EasyMock.expect(r.getLower()).andReturn(new BigInteger("5"));
        EasyMock.expect(r.getUpper()).andReturn(new BigInteger("15"));
        control.replay();     
        try {
            ds.applyDeliveryAssurance(mn);
            fail("Expected Fault not thrown.");
        } catch (RMException ex) {
            assertEquals("MESSAGE_ALREADY_DELIVERED_EXC", ex.getCode());
        }
          
        control.verify();

    }
    
    @Test
    public void testInOrderNoWait() throws RMException {
        setUpDestination();

        BigInteger mn = BigInteger.TEN;
        
        DeliveryAssuranceType da = control.createMock(DeliveryAssuranceType.class);
        EasyMock.expect(manager.getDeliveryAssurance()).andReturn(da);
        EasyMock.expect(da.isSetAtMostOnce()).andReturn(false);
        EasyMock.expect(da.isSetAtLeastOnce()).andReturn(true);
        EasyMock.expect(da.isSetInOrder()).andReturn(true); 
        
        SequenceAcknowledgement ack = control.createMock(SequenceAcknowledgement.class);
        List<AcknowledgementRange> ranges = new ArrayList<AcknowledgementRange>();
        AcknowledgementRange r = control.createMock(AcknowledgementRange.class);
        ranges.add(r);
        EasyMock.expect(ack.getAcknowledgementRange()).andReturn(ranges).times(3);
        EasyMock.expect(r.getLower()).andReturn(BigInteger.ONE);
        EasyMock.expect(r.getUpper()).andReturn(new BigInteger("15"));
        
        control.replay(); 
        
        DestinationSequence ds = new DestinationSequence(id, ref, null, ack);
        ds.setDestination(destination);
        ds.applyDeliveryAssurance(mn);
        control.verify();
    }
    
    @Test
    public void testInOrderWait() {
        setUpDestination();
        Message[] messages = new Message[5];
        for (int i = 0; i < messages.length; i++) {
            messages[i] = setUpMessage(Integer.toString(i + 1));                                           
        }
        
        DeliveryAssuranceType da = control.createMock(DeliveryAssuranceType.class);
        EasyMock.expect(manager.getDeliveryAssurance()).andReturn(da).anyTimes();
        EasyMock.expect(da.isSetAtMostOnce()).andReturn(false).anyTimes();
        EasyMock.expect(da.isSetAtLeastOnce()).andReturn(true).anyTimes();
        EasyMock.expect(da.isSetInOrder()).andReturn(true).anyTimes(); 
        
        SequenceAcknowledgement ack = factory.createSequenceAcknowledgement();
        List<AcknowledgementRange> ranges = new ArrayList<AcknowledgementRange>();
        
        final AcknowledgementRange r = 
            factory.createSequenceAcknowledgementAcknowledgementRange();
        r.setUpper(new BigInteger(Integer.toString(messages.length)));
        ranges.add(r);
        final DestinationSequence ds = new DestinationSequence(id, ref, null, ack);
        ds.setDestination(destination);
          
        class Acknowledger extends Thread {
            Message message;
            BigInteger messageNr;
            
            Acknowledger(Message m, BigInteger mn) {
                message = m;
                messageNr = mn;
            }
            
            public void run() {
                try {
                    ds.acknowledge(message);
                    ds.applyDeliveryAssurance(messageNr);
                } catch (Exception ex) {
                    // ignore
                }
            }            
        }
 
        control.replay(); 
        
        Thread[] threads = new Thread[messages.length];
        for (int i = messages.length - 1; i >= 0; i--) {
            threads[i] = new Acknowledger(messages[i], new BigInteger(Integer.toString(i + 1)));
            threads[i].start();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                // ignore
            }
        }
        
        boolean timedOut = false;
        for (int i = 0; i < messages.length; i++) {
            try {
                threads[i].join(1000); 
            } catch (InterruptedException ex) {
                timedOut = true;
            }
        }
        assertTrue("timed out waiting for messages to be processed in order", !timedOut);
        
        control.verify();
    }
    
    @Test
    public void testAllPredecessorsAcknowledged() {

        SequenceAcknowledgement ack = control.createMock(SequenceAcknowledgement.class);
        List<AcknowledgementRange> ranges = new ArrayList<AcknowledgementRange>();
        AcknowledgementRange r = control.createMock(AcknowledgementRange.class);
        EasyMock.expect(ack.getAcknowledgementRange()).andReturn(ranges);
        control.replay();
        DestinationSequence ds = new DestinationSequence(id, ref, null, ack);
        ds.setDestination(destination);
        assertTrue("all predecessors acknowledged", !ds.allPredecessorsAcknowledged(BigInteger.TEN));
        control.verify();
        
        control.reset();
        ranges.add(r);
        EasyMock.expect(ack.getAcknowledgementRange()).andReturn(ranges).times(2);
        EasyMock.expect(r.getLower()).andReturn(BigInteger.TEN);
        control.replay();
        assertTrue("all predecessors acknowledged", !ds.allPredecessorsAcknowledged(BigInteger.TEN));
        control.verify();
        
        control.reset();
        EasyMock.expect(ack.getAcknowledgementRange()).andReturn(ranges).times(3);
        EasyMock.expect(r.getLower()).andReturn(BigInteger.ONE);
        EasyMock.expect(r.getUpper()).andReturn(new BigInteger("5"));
        control.replay();
        assertTrue("all predecessors acknowledged", !ds.allPredecessorsAcknowledged(BigInteger.TEN));
        control.verify();
        
        control.reset();
        EasyMock.expect(ack.getAcknowledgementRange()).andReturn(ranges).times(3);
        EasyMock.expect(r.getLower()).andReturn(BigInteger.ONE);
        EasyMock.expect(r.getUpper()).andReturn(BigInteger.TEN);
        control.replay();
        assertTrue("not all predecessors acknowledged", ds.allPredecessorsAcknowledged(BigInteger.TEN));
        control.verify();
        
        ranges.add(r);
        control.reset();
        EasyMock.expect(ack.getAcknowledgementRange()).andReturn(ranges);
        control.replay();
        assertTrue("all predecessors acknowledged", !ds.allPredecessorsAcknowledged(BigInteger.TEN));
        control.verify();
    }
    
    @Test
    public void testScheduleSequenceTermination() throws SequenceFault {
        Timer timer = new Timer();
        setUpDestination(timer);
        
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        destination.removeSequence(seq);
        EasyMock.expectLastCall();
        
        Message message = setUpMessage("1");
        
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(destination.getReliableEndpoint()).andReturn(rme);
        long arrival = System.currentTimeMillis();
        EasyMock.expect(rme.getLastApplicationMessage()).andReturn(arrival);

        control.replay();
        InactivityTimeout iat = new RMAssertion.InactivityTimeout();
        iat.setMilliseconds(new BigInteger("200"));
        rma.setInactivityTimeout(iat); 
        
        seq.acknowledge(message);
        
        try {
            Thread.sleep(250);
        } catch (InterruptedException ex) {
            // ignore
        }
        
        control.verify();
    }

    @Test
    public void testSequenceTermination() {
        destination = control.createMock(Destination.class);
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(destination.getReliableEndpoint()).andReturn(rme);
        DestinationSequence.SequenceTermination st = seq.new SequenceTermination();
        st.updateInactivityTimeout(30000L);
        long lastAppMessage = System.currentTimeMillis() - 30000L;
        EasyMock.expect(rme.getLastControlMessage()).andReturn(0L);
        EasyMock.expect(rme.getLastApplicationMessage()).andReturn(lastAppMessage);
        destination.removeSequence(seq);
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
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(destination.getReliableEndpoint()).andReturn(rme);
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
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        AttributedURI uri = control.createMock(AttributedURI.class);
        EasyMock.expect(ref.getAddress()).andReturn(uri);
        String addr = "http://localhost:9999/reponses";
        EasyMock.expect(uri.getValue()).andReturn(addr);
        control.replay();
        assertTrue(!seq.canPiggybackAckOnPartialResponse());
        control.verify();
        control.reset();
        EasyMock.expect(ref.getAddress()).andReturn(uri);
        EasyMock.expect(uri.getValue()).andReturn(RMConstants.getAnonymousAddress());
        control.replay();
        assertTrue(seq.canPiggybackAckOnPartialResponse());
        control.verify();
    }
    
    @Test
    public void testPurgeAcknowledged() {
        destination = control.createMock(Destination.class);
        DestinationSequence seq = new DestinationSequence(id, ref, destination);        
        manager = control.createMock(RMManager.class);
        EasyMock.expect(destination.getManager()).andReturn(manager);
        RMStore store = control.createMock(RMStore.class);
        EasyMock.expect(manager.getStore()).andReturn(store);
        store.removeMessages(EasyMock.eq(id), 
            CastUtils.cast(EasyMock.isA(Collection.class), BigInteger.class), EasyMock.eq(false));
        EasyMock.expectLastCall();
        control.replay();
        seq.purgeAcknowledged(BigInteger.ONE);
        control.verify();
    }
    
    @Test
    public void testCancelDeferredAcknowledgements() {
        destination = control.createMock(Destination.class);
        manager = control.createMock(RMManager.class);
        EasyMock.expect(destination.getManager()).andReturn(manager);
        Timer t = new Timer();
        EasyMock.expect(manager.getTimer()).andReturn(t);
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
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
        DestinationSequence seq = new DestinationSequence(id, ref, destination);
        control.replay();
        seq.scheduleSequenceTermination(30000L);
        seq.cancelTermination();
        t.cancel();
        control.verify();
    }
    
    private void setUpDestination() {
        setUpDestination(null);
    }
    
    private void setUpDestination(Timer timer) {
        
        manager = control.createMock(RMManager.class);

        org.apache.cxf.ws.rm.manager.ObjectFactory cfgFactory =
            new org.apache.cxf.ws.rm.manager.ObjectFactory();
        dp = cfgFactory.createDestinationPolicyType();
        ap = cfgFactory.createAcksPolicyType();
        dp.setAcksPolicy(ap);
        
        org.apache.cxf.ws.rm.policy.ObjectFactory policyFactory =
            new org.apache.cxf.ws.rm.policy.ObjectFactory();
        rma = policyFactory.createRMAssertion();
        BaseRetransmissionInterval bri =
            policyFactory.createRMAssertionBaseRetransmissionInterval();
        bri.setMilliseconds(new BigInteger("3000"));
        rma.setBaseRetransmissionInterval(bri);  

        EasyMock.expect(manager.getRMAssertion()).andReturn(rma).anyTimes();
        EasyMock.expect(manager.getDestinationPolicy()).andReturn(dp).anyTimes();
        EasyMock.expect(manager.getStore()).andReturn(null).anyTimes();
        
        destination = control.createMock(Destination.class);
        EasyMock.expect(destination.getManager()).andReturn(manager).anyTimes();
        
        if (null != timer) {
            EasyMock.expect(manager.getTimer()).andReturn(timer).anyTimes();
        }

    }
    
    private Message setUpMessage(String messageNr) {
        Message message = control.createMock(Message.class);        
        Exchange exchange = control.createMock(Exchange.class);
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        EasyMock.expect(exchange.getOutMessage()).andReturn(null);
        EasyMock.expect(exchange.getOutFaultMessage()).andReturn(null);
        RMProperties rmps = control.createMock(RMProperties.class);
        EasyMock.expect(message.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).andReturn(rmps);
        SequenceType st = control.createMock(SequenceType.class);
        EasyMock.expect(rmps.getSequence()).andReturn(st);
        BigInteger val = new BigInteger(messageNr);
        EasyMock.expect(st.getMessageNumber()).andReturn(val);
        return message;        
    }


}
