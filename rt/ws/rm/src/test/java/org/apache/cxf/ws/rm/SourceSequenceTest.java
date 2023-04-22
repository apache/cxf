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

import java.util.Date;

import javax.xml.datatype.Duration;

import org.apache.cxf.jaxb.DatatypeFactory;
import org.apache.cxf.ws.rm.manager.SequenceTerminationPolicyType;
import org.apache.cxf.ws.rm.manager.SourcePolicyType;
import org.apache.cxf.ws.rm.v200702.Expires;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.ObjectFactory;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SourceSequenceTest {

    private IMocksControl control;
    private ObjectFactory factory;
    private Identifier id;

    private Source source;
    private RMManager manager;
    private SourcePolicyType sp;
    private SequenceTerminationPolicyType stp;
    private RetransmissionQueue rq;

    @Before
    public void setUp() {
        factory = new ObjectFactory();
        id = factory.createIdentifier();
        id.setValue("seq");

        control = EasyMock.createNiceControl();
    }

    @After
    public void tearDown() {
        source = null;
        manager = null;
        sp = null;
        stp = null;
        rq = null;
    }

    protected void setUpSource() {
        source = control.createMock(Source.class);
        manager = control.createMock(RMManager.class);
        EasyMock.expect(source.getManager()).andReturn(manager).anyTimes();
        rq = control.createMock(RetransmissionQueue.class);
        EasyMock.expect(manager.getRetransmissionQueue()).andReturn(rq).anyTimes();

        // default termination policy

        org.apache.cxf.ws.rm.manager.ObjectFactory cfgFactory =
            new org.apache.cxf.ws.rm.manager.ObjectFactory();
        sp = cfgFactory.createSourcePolicyType();
        stp = cfgFactory
            .createSequenceTerminationPolicyType();
        sp.setSequenceTerminationPolicy(stp);
        EasyMock.expect(manager.getSourcePolicy()).andReturn(sp).anyTimes();
    }

    @Test
    public void testConstructors() {

        Identifier otherId = factory.createIdentifier();
        otherId.setValue("otherSeq");

        SourceSequence seq = new SourceSequence(id, ProtocolVariation.RM10WSA200408);
        assertEquals(id, seq.getIdentifier());
        assertFalse(seq.isLastMessage());
        assertFalse(seq.isExpired());
        assertEquals(0, seq.getCurrentMessageNr());
        assertNotNull(seq.getAcknowledgement());
        assertEquals(0, seq.getAcknowledgement().getAcknowledgementRange().size());
        assertFalse(seq.allAcknowledged());
        assertFalse(seq.offeredBy(otherId));

        Date expiry = new Date(System.currentTimeMillis() + 3600 * 1000);

        seq = new SourceSequence(id, expiry, null, ProtocolVariation.RM10WSA200408);
        assertEquals(id, seq.getIdentifier());
        assertFalse(seq.isLastMessage());
        assertFalse(seq.isExpired());
        assertEquals(0, seq.getCurrentMessageNr());
        assertNotNull(seq.getAcknowledgement());
        assertEquals(0, seq.getAcknowledgement().getAcknowledgementRange().size());
        assertFalse(seq.allAcknowledged());
        assertFalse(seq.offeredBy(otherId));

        seq = new SourceSequence(id, expiry, otherId, ProtocolVariation.RM10WSA200408);
        assertTrue(seq.offeredBy(otherId));
        assertFalse(seq.offeredBy(id));
    }

    @Test
    public void testSetExpires() {
        SourceSequence seq = new SourceSequence(id, ProtocolVariation.RM10WSA200408);

        Expires expires = factory.createExpires();
        seq.setExpires(expires);

        assertFalse(seq.isExpired());

        Duration d = DatatypeFactory.PT0S;
        expires.setValue(d);
        seq.setExpires(expires);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            assertFalse(seq.isExpired());
        }

        d = DatatypeFactory.createDuration("PT1S");
        expires.setValue(d);
        seq.setExpires(expires);
        assertFalse(seq.isExpired());

        d = DatatypeFactory.createDuration("-PT1S");
        expires.setValue(d);
        seq.setExpires(expires);
        assertTrue(seq.isExpired());
    }

    @Test
    public void testEqualsAndHashCode() {
        SourceSequence seq = new SourceSequence(id, ProtocolVariation.RM10WSA200408);
        SourceSequence otherSeq = null;
        assertFalse(seq.equals(otherSeq));
        otherSeq = new SourceSequence(id, ProtocolVariation.RM10WSA200408);
        assertEquals(seq, otherSeq);
        assertEquals(seq.hashCode(), otherSeq.hashCode());
        Identifier otherId = factory.createIdentifier();
        otherId.setValue("otherSeq");
        otherSeq = new SourceSequence(otherId, ProtocolVariation.RM10WSA200408);
        assertFalse(seq.equals(otherSeq));
        assertTrue(seq.hashCode() != otherSeq.hashCode());
    }

    @Test
    public void testSetAcknowledged() throws RMException {
        SourceSequence seq = new SourceSequence(id, ProtocolVariation.RM10WSA200408);
        setUpSource();
        seq.setSource(source);

        SequenceAcknowledgement ack = factory.createSequenceAcknowledgement();
        SequenceAcknowledgement.AcknowledgementRange r =
            factory.createSequenceAcknowledgementAcknowledgementRange();
        r.setLower(Long.valueOf(1));
        r.setUpper(Long.valueOf(2));
        ack.getAcknowledgementRange().add(r);
        r = factory.createSequenceAcknowledgementAcknowledgementRange();
        r.setLower(Long.valueOf(4));
        r.setUpper(Long.valueOf(6));
        ack.getAcknowledgementRange().add(r);
        r = factory.createSequenceAcknowledgementAcknowledgementRange();
        r.setLower(Long.valueOf(8));
        r.setUpper(Long.valueOf(10));
        ack.getAcknowledgementRange().add(r);
        rq.purgeAcknowledged(seq);
        EasyMock.expectLastCall();

        control.replay();
        seq.setAcknowledged(ack);
        assertSame(ack, seq.getAcknowledgement());
        assertEquals(3, ack.getAcknowledgementRange().size());
        assertFalse(seq.isAcknowledged(3));
        assertTrue(seq.isAcknowledged(5));
        control.verify();
    }

    @Test
    public void testAllAcknowledged() throws RMException {

        SourceSequence seq = new SourceSequence(id, null, null, 4, false,
                                                ProtocolVariation.RM10WSA200408);
        setUpSource();
        seq.setSource(source);

        assertFalse(seq.allAcknowledged());
        seq.setLastMessage(true);
        assertFalse(seq.allAcknowledged());
        SequenceAcknowledgement ack = factory.createSequenceAcknowledgement();
        SequenceAcknowledgement.AcknowledgementRange r =
            factory.createSequenceAcknowledgementAcknowledgementRange();
        r.setLower(Long.valueOf(1));
        r.setUpper(Long.valueOf(2));
        ack.getAcknowledgementRange().add(r);
        rq.purgeAcknowledged(seq);
        EasyMock.expectLastCall();

        control.replay();
        seq.setAcknowledged(ack);
        assertFalse(seq.allAcknowledged());
        r.setUpper(Long.valueOf(4));
        assertTrue(seq.allAcknowledged());
        control.verify();
    }

    @Test
    public void testNextMessageNumber() throws RMException {
        setUpSource();
        rq.purgeAcknowledged(EasyMock.isA(SourceSequence.class));
        EasyMock.expectLastCall().anyTimes();
        control.replay();

        // default termination policy

        SourceSequence seq = new SourceSequence(id, ProtocolVariation.RM10WSA200408);
        seq.setSource(source);
        assertFalse(nextMessages(seq, 10));
        control.verify();

        // termination policy max length = 1

        seq = new SourceSequence(id, ProtocolVariation.RM10WSA200408);
        seq.setSource(source);
        stp.setMaxLength(1);
        assertTrue(nextMessages(seq, 10));
        assertEquals(1, seq.getCurrentMessageNr());
        control.verify();

        // termination policy max length = 5
        seq = new SourceSequence(id, ProtocolVariation.RM10WSA200408);
        seq.setSource(source);
        stp.setMaxLength(5);
        assertFalse(nextMessages(seq, 2));
        control.verify();

        // termination policy max range exceeded

        seq = new SourceSequence(id, ProtocolVariation.RM10WSA200408);
        seq.setSource(source);
        stp.setMaxLength(0);
        stp.setMaxRanges(3);
        acknowledge(seq, 1, 2, 4, 5, 6, 8, 9, 10);
        assertTrue(nextMessages(seq, 10));
        assertEquals(1, seq.getCurrentMessageNr());
        control.verify();

        // termination policy max range not exceeded

        seq = new SourceSequence(id, ProtocolVariation.RM10WSA200408);
        seq.setSource(source);
        stp.setMaxLength(0);
        stp.setMaxRanges(4);
        acknowledge(seq, 1, 2, 4, 5, 6, 8, 9, 10);
        assertFalse(nextMessages(seq, 10));
        control.verify();

        // termination policy max unacknowledged
    }

    @Test
    public void testGetEndpointIdentfier() {
        setUpSource();
        String name = "abc";
        EasyMock.expect(source.getName()).andReturn(name);
        control.replay();

        SourceSequence seq = new SourceSequence(id, ProtocolVariation.RM10WSA200408);
        seq.setSource(source);
        assertEquals("Unexpected endpoint identifier", name, seq.getEndpointIdentifier());
        control.verify();
    }

    @Test
    public void testCheckOfferingSequenceClosed() {
        setUpSource();

        RMEndpoint rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(source.getReliableEndpoint()).andReturn(rme).anyTimes();
        Destination destination = control.createMock(Destination.class);
        EasyMock.expect(rme.getDestination()).andReturn(destination).anyTimes();
        DestinationSequence dseq = control.createMock(DestinationSequence.class);
        Identifier did = control.createMock(Identifier.class);
        EasyMock.expect(destination.getSequence(did)).andReturn(dseq).anyTimes();
        EasyMock.expect(dseq.getLastMessageNumber()).andReturn(Long.valueOf(1)).anyTimes();
        EasyMock.expect(did.getValue()).andReturn("dseq").anyTimes();

        control.replay();

        SourceSequence seq = new SourceSequence(id, null, did, ProtocolVariation.RM10WSA200408);
        seq.setSource(source);
        seq.nextMessageNumber(did, 1, false);
        assertTrue(seq.isLastMessage());

        control.verify();
    }

    private boolean nextMessages(SourceSequence seq,
                                 int n) {
        int i = 0;
        while ((i < n) && !seq.isLastMessage()) {
            seq.nextMessageNumber();
            i++;
        }
        return seq.isLastMessage();
    }

    protected void acknowledge(SourceSequence seq, int... messageNumbers) throws RMException {
        SequenceAcknowledgement ack = factory.createSequenceAcknowledgement();
        int i = 0;
        while (i < messageNumbers.length) {
            SequenceAcknowledgement.AcknowledgementRange r =
                factory.createSequenceAcknowledgementAcknowledgementRange();
            Long l = Long.valueOf(messageNumbers[i]);
            r.setLower(l);
            i++;

            while (i < messageNumbers.length && (messageNumbers[i] - messageNumbers[i - 1]) == 1) {
                i++;
            }
            Long u = Long.valueOf(messageNumbers[i - 1]);
            r.setUpper(u);
            ack.getAcknowledgementRange().add(r);
        }
        seq.setAcknowledged(ack);
    }
}