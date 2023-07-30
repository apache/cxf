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

package org.apache.cxf.ws.rm.persistence.jdbc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.rm.DestinationSequence;
import org.apache.cxf.ws.rm.ProtocolVariation;
import org.apache.cxf.ws.rm.RMUtils;
import org.apache.cxf.ws.rm.SourceSequence;
import org.apache.cxf.ws.rm.persistence.RMMessage;
import org.apache.cxf.ws.rm.persistence.RMStoreException;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public abstract class RMTxStoreTestBase {
    protected static RMTxStore store;

    protected static final String CLIENT_ENDPOINT_ID =
        "celtix.{http://celtix.objectweb.org/greeter_control}GreeterService/GreeterPort";
    private static final String SERVER_ENDPOINT_ID =
        "celtix.{http://celtix.objectweb.org/greeter_control}GreeterService";
    private static final String NON_ANON_ACKS_TO =
        "http://localhost:9999/decoupled_endpoint";

    private static final long ZERO = 0L;
    private static final long ONE = 1L;
    private static final long TEN = 10L;

    private static SequenceAcknowledgement ack1;
    private static SequenceAcknowledgement ack2;

    private static final long TIME = System.currentTimeMillis();

    public static void setUpOnce() {
        ack1 = new SequenceAcknowledgement();
        SequenceAcknowledgement.AcknowledgementRange range =
            new SequenceAcknowledgement.AcknowledgementRange();

        range.setLower(ONE);
        range.setUpper(ONE);
        ack1.getAcknowledgementRange().add(range);

        ack2 = new SequenceAcknowledgement();
        range = new SequenceAcknowledgement.AcknowledgementRange();
        range.setLower(ONE);
        range.setUpper(ONE);
        ack2.getAcknowledgementRange().add(range);
        range = new SequenceAcknowledgement.AcknowledgementRange();
        range.setLower(Long.valueOf(3));
        range.setUpper(TEN);
        ack2.getAcknowledgementRange().add(range);
    }

    protected abstract Connection getConnection();

    protected abstract void releaseConnection(Connection con);

    @Test
    public void testCreateTables() throws SQLException {
        // tables should  have been created during initialisation
        // but verify the operation is idempotent
        store.createTables();
    }

    @Test
    public void testCreateDeleteSrcSequences() {
        SourceSequence seq = mock(SourceSequence.class);
        Identifier sid1 = new Identifier();
        sid1.setValue("sequence1");
        when(seq.getIdentifier()).thenReturn(sid1);
        when(seq.getExpires()).thenReturn(null);
        when(seq.getOfferingSequenceIdentifier()).thenReturn(null);
        when(seq.getEndpointIdentifier()).thenReturn(CLIENT_ENDPOINT_ID);
        when(seq.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);

        store.createSourceSequence(seq);

        when(seq.getIdentifier()).thenReturn(sid1);
        when(seq.getExpires()).thenReturn(null);
        when(seq.getOfferingSequenceIdentifier()).thenReturn(null);
        when(seq.getEndpointIdentifier()).thenReturn(CLIENT_ENDPOINT_ID);
        when(seq.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);

        try {
            store.createSourceSequence(seq);
            fail("Expected RMStoreException was not thrown.");
        } catch (RMStoreException ex) {
            SQLException se = (SQLException)ex.getCause();
            // duplicate key value
            assertEquals("23505", se.getSQLState());
        }

        Identifier sid2 = new Identifier();
        sid2.setValue("sequence2");
        when(seq.getIdentifier()).thenReturn(sid2);
        when(seq.getExpires()).thenReturn(new Date());
        Identifier sid3 = new Identifier();
        sid3.setValue("offeringSequence3");
        when(seq.getOfferingSequenceIdentifier()).thenReturn(sid3);
        when(seq.getEndpointIdentifier()).thenReturn(SERVER_ENDPOINT_ID);
        when(seq.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);

        store.createSourceSequence(seq);

        store.removeSourceSequence(sid1);
        store.removeSourceSequence(sid2);

        // deleting once again is a no-op
        store.removeSourceSequence(sid2);
    }

    @Test
    public void testCreateDeleteDestSequences() {
        DestinationSequence seq = mock(DestinationSequence.class);
        Identifier sid1 = new Identifier();
        sid1.setValue("sequence1");
        EndpointReferenceType epr = RMUtils.createAnonymousReference();
        when(seq.getIdentifier()).thenReturn(sid1);
        when(seq.getAcksTo()).thenReturn(epr);
        when(seq.getEndpointIdentifier()).thenReturn(SERVER_ENDPOINT_ID);
        when(seq.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);

        store.createDestinationSequence(seq);

        when(seq.getIdentifier()).thenReturn(sid1);
        when(seq.getAcksTo()).thenReturn(epr);
        when(seq.getEndpointIdentifier()).thenReturn(SERVER_ENDPOINT_ID);
        when(seq.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);

        try {
            store.createDestinationSequence(seq);
            fail("Expected RMStoreException was not thrown.");
        } catch (RMStoreException ex) {
            SQLException se = (SQLException)ex.getCause();
            // duplicate key value
            assertEquals("23505", se.getSQLState());
        }

        Identifier sid2 = new Identifier();
        sid2.setValue("sequence2");
        when(seq.getIdentifier()).thenReturn(sid2);
        epr = RMUtils.createReference(NON_ANON_ACKS_TO);
        when(seq.getAcksTo()).thenReturn(epr);
        when(seq.getEndpointIdentifier()).thenReturn(CLIENT_ENDPOINT_ID);
        when(seq.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);

        store.createDestinationSequence(seq);

        store.removeDestinationSequence(sid1);
        store.removeDestinationSequence(sid2);

        // deleting once again is a no-op
        store.removeDestinationSequence(sid2);
    }

    @Test
    public void testCreateDeleteMessages() throws IOException, SQLException  {
        RMMessage msg1 = mock(RMMessage.class);
        RMMessage msg2 = mock(RMMessage.class);
        Identifier sid1 = new Identifier();
        sid1.setValue("sequence1");

        when(msg1.getMessageNumber()).thenReturn(ONE);
        when(msg2.getMessageNumber()).thenReturn(ONE);
        byte[] bytes = new byte[89];
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        CachedOutputStream cos = new CachedOutputStream();
        IOUtils.copy(bais, cos);
        cos.flush();
        bais.close();
        when(msg1.getContent()).thenReturn(cos);
        when(msg2.getContent()).thenReturn(cos);
        when(msg1.getContentType()).thenReturn("text/xml");

        Connection con = getConnection();
        try {
            store.beginTransaction();
            store.storeMessage(con, sid1, msg1, true);
            store.storeMessage(con, sid1, msg2, false);
            store.commit(con);
        } finally {
            releaseConnection(con);
        }

        verify(msg1, times(1)).getContentType();

        when(msg1.getMessageNumber()).thenReturn(ONE);
        when(msg1.getContent()).thenReturn(cos);

        con = getConnection();
        try {
            store.beginTransaction();
            store.storeMessage(con, sid1, msg1, true);
        } catch (SQLException ex) {
            assertEquals("23505", ex.getSQLState());
            store.abort(con);
        } finally {
            releaseConnection(con);
        }

        when(msg1.getMessageNumber()).thenReturn(TEN);
        when(msg2.getMessageNumber()).thenReturn(TEN);
        when(msg1.getContent()).thenReturn(cos);
        when(msg2.getContent()).thenReturn(cos);

        con = getConnection();
        try {
            store.beginTransaction();
            store.storeMessage(con, sid1, msg1, true);
            store.storeMessage(con, sid1, msg2, false);
            store.commit(con);
        } finally {
            releaseConnection(con);
        }

        Collection<Long> messageNrs = Arrays.asList(
            ZERO, TEN, ONE, TEN);

        store.removeMessages(sid1, messageNrs, true);
        store.removeMessages(sid1, messageNrs, false);

        Identifier sid2 = new Identifier();
        sid1.setValue("sequence2");
        store.removeMessages(sid2, messageNrs, true);
    }

    @Test
    public void testUpdateDestinationSequence() throws SQLException, IOException {
        DestinationSequence seq = mock(DestinationSequence.class);
        Identifier sid1 = new Identifier();
        sid1.setValue("sequence1");
        EndpointReferenceType epr = RMUtils.createAnonymousReference();
        when(seq.getIdentifier()).thenReturn(sid1);
        when(seq.getAcksTo()).thenReturn(epr);
        when(seq.getEndpointIdentifier()).thenReturn(SERVER_ENDPOINT_ID);
        when(seq.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);

        store.createDestinationSequence(seq);

        when(seq.getLastMessageNumber()).thenReturn(Long.valueOf(0));
        when(seq.getAcknowledgment()).thenReturn(ack1);
        when(seq.getIdentifier()).thenReturn(sid1);
        when(seq.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);

        Connection con = getConnection();
        try {
            store.beginTransaction();
            store.updateDestinationSequence(con, seq);
            store.abort(con);
        } finally {
            releaseConnection(con);
        }

        when(seq.getLastMessageNumber()).thenReturn(TEN);
        when(seq.getAcknowledgment()).thenReturn(ack1);
        when(seq.getIdentifier()).thenReturn(sid1);
        when(seq.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);

        con = getConnection();
        try {
            store.beginTransaction();
            store.updateDestinationSequence(con, seq);
            store.abort(con);
        } finally {
            releaseConnection(con);
        }

        store.removeDestinationSequence(sid1);
    }

    @Test
    public void testUpdateSourceSequence() throws SQLException {
        SourceSequence seq = mock(SourceSequence.class);
        Identifier sid1 = new Identifier();
        sid1.setValue("sequence1");
        when(seq.getIdentifier()).thenReturn(sid1);
        when(seq.getExpires()).thenReturn(null);
        when(seq.getOfferingSequenceIdentifier()).thenReturn(null);
        when(seq.getEndpointIdentifier()).thenReturn(CLIENT_ENDPOINT_ID);
        when(seq.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);

        store.createSourceSequence(seq);

        when(seq.getCurrentMessageNr()).thenReturn(ONE);
        when(seq.isLastMessage()).thenReturn(false);
        when(seq.getIdentifier()).thenReturn(sid1);
        when(seq.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);

        Connection con = getConnection();
        try {
            store.beginTransaction();
            store.updateSourceSequence(con, seq);
            store.abort(con);
        } finally {
            releaseConnection(con);
        }

        when(seq.getCurrentMessageNr()).thenReturn(TEN);
        when(seq.isLastMessage()).thenReturn(true);
        when(seq.getIdentifier()).thenReturn(sid1);
        when(seq.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);

        con = getConnection();
        try {
            store.beginTransaction();
            store.updateSourceSequence(con, seq);
            store.abort(con);
        } finally {
            releaseConnection(con);
        }

        store.removeSourceSequence(sid1);

    }

    @Test
    public void testGetDestinationSequences() throws SQLException, IOException {

        Identifier sid1 = null;
        Identifier sid2 = null;

        Collection<DestinationSequence> seqs =
            store.getDestinationSequences("unknown");
        assertEquals(0, seqs.size());

        try {
            sid1 = setupDestinationSequence("sequence1");

            seqs = store.getDestinationSequences(SERVER_ENDPOINT_ID);
            assertEquals(1, seqs.size());
            checkRecoveredDestinationSequences(seqs);

            sid2 = setupDestinationSequence("sequence2");
            seqs = store.getDestinationSequences(SERVER_ENDPOINT_ID);
            assertEquals(2, seqs.size());
            checkRecoveredDestinationSequences(seqs);
        } finally {
            if (null != sid1) {
                store.removeDestinationSequence(sid1);
            }
            if (null != sid2) {
                store.removeDestinationSequence(sid2);
            }
        }
    }

    @Test
    public void testGetSourceSequences() throws SQLException {

        Identifier sid1 = null;
        Identifier sid2 = null;

        Collection<SourceSequence> seqs =
            store.getSourceSequences("unknown");
        assertEquals(0, seqs.size());

        try {
            sid1 = setupSourceSequence("sequence1");

            seqs = store.getSourceSequences(CLIENT_ENDPOINT_ID);
            assertEquals(1, seqs.size());
            checkRecoveredSourceSequences(seqs);

            sid2 = setupSourceSequence("sequence2");
            seqs = store.getSourceSequences(CLIENT_ENDPOINT_ID);
            assertEquals(2, seqs.size());
            checkRecoveredSourceSequences(seqs);
        } finally {
            if (null != sid1) {
                store.removeSourceSequence(sid1);
            }
            if (null != sid2) {
                store.removeSourceSequence(sid2);
            }
        }
    }

    @Test
    public void testGetDestinationSequence() throws SQLException, IOException {

        Identifier sid1 = null;
        Identifier sid2 = null;

        DestinationSequence seq =
            store.getDestinationSequence(new Identifier());
        assertNull(seq);

        try {
            sid1 = setupDestinationSequence("sequence1");

            seq = store.getDestinationSequence(sid1);
            assertNotNull(seq);
            verifyDestinationSequence("sequence1", seq);

            sid2 = setupDestinationSequence("sequence2");
            seq = store.getDestinationSequence(sid2);
            assertNotNull(seq);
            verifyDestinationSequence("sequence2", seq);
        } finally {
            if (null != sid1) {
                store.removeDestinationSequence(sid1);
            }
            if (null != sid2) {
                store.removeDestinationSequence(sid2);
            }
        }
    }

    @Test
    public void testGetSourceSequence() throws SQLException, IOException {

        Identifier sid1 = null;
        Identifier sid2 = null;

        SourceSequence seq =
            store.getSourceSequence(new Identifier());
        assertNull(seq);

        try {
            sid1 = setupSourceSequence("sequence1");

            seq = store.getSourceSequence(sid1);
            assertNotNull(seq);
            verifySourceSequence("sequence1", seq);

            sid2 = setupSourceSequence("sequence2");
            seq = store.getSourceSequence(sid2);
            assertNotNull(seq);
            verifySourceSequence("sequence2", seq);
        } finally {
            if (null != sid1) {
                store.removeSourceSequence(sid1);
            }
            if (null != sid2) {
                store.removeSourceSequence(sid2);
            }
        }
    }

    @Test
    public void testGetMessages() throws SQLException, IOException {

        Identifier sid1 = new Identifier();
        sid1.setValue("sequence1");
        Identifier sid2 = new Identifier();
        sid2.setValue("sequence2");

        Collection<RMMessage> out = store.getMessages(sid1, true);
        assertEquals(0, out.size());
        Collection<RMMessage> in = store.getMessages(sid1, false);
        assertEquals(0, in.size());

        try {
            setupMessage(sid1, ONE, null, true);
            setupMessage(sid1, ONE, null, false);

            out = store.getMessages(sid1, true);
            assertEquals(1, out.size());
            checkRecoveredMessages(out);

            in = store.getMessages(sid1, false);
            assertEquals(1, in.size());
            checkRecoveredMessages(in);

            setupMessage(sid1, TEN, NON_ANON_ACKS_TO, true);
            setupMessage(sid1, TEN, NON_ANON_ACKS_TO, false);

            out = store.getMessages(sid1, true);
            assertEquals(2, out.size());
            checkRecoveredMessages(out);

            in = store.getMessages(sid1, false);
            assertEquals(2, in.size());
            checkRecoveredMessages(in);
        } finally {
            Collection<Long> msgNrs = Arrays.asList(
                ONE, TEN);

            store.removeMessages(sid1, msgNrs, true);
            store.removeMessages(sid1, msgNrs, false);
        }
    }

    @Test
    public void testCreateSequenceStoreOutboundMessage() throws SQLException, IOException {
        Identifier sid1 = null;
        try {
            SourceSequence seq = mock(SourceSequence.class);
            sid1 = new Identifier();
            sid1.setValue("sequence1");
            when(seq.getIdentifier()).thenReturn(sid1);
            when(seq.getExpires()).thenReturn(null);
            when(seq.getOfferingSequenceIdentifier()).thenReturn(null);
            when(seq.getEndpointIdentifier()).thenReturn(CLIENT_ENDPOINT_ID);
            when(seq.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);

            store.createSourceSequence(seq);

            Collection<SourceSequence> seqs = store.getSourceSequences(CLIENT_ENDPOINT_ID);
            assertEquals(1, seqs.size());
            SourceSequence rseq = seqs.iterator().next();
            assertFalse(rseq.isLastMessage());

            Collection<RMMessage> out = store.getMessages(sid1, true);
            assertEquals(0, out.size());

            // set the last message flag
            when(seq.getIdentifier()).thenReturn(sid1);
            when(seq.isLastMessage()).thenReturn(true);

            setupOutboundMessage(seq, 1L, null);
            out = store.getMessages(sid1, true);
            assertEquals(1, out.size());
            checkRecoveredMessages(out);

            // verify the updated sequence
            seqs = store.getSourceSequences(CLIENT_ENDPOINT_ID);
            assertEquals(1, seqs.size());
            rseq = seqs.iterator().next();

            assertTrue(rseq.isLastMessage());

            // set the last message flag
            when(seq.getIdentifier()).thenReturn(sid1);
            when(seq.getCurrentMessageNr()).thenReturn(2L);
            when(seq.isLastMessage()).thenReturn(true);

            store.persistOutgoing(seq, null);

            seqs = store.getSourceSequences(CLIENT_ENDPOINT_ID);
            assertEquals(1, seqs.size());
            rseq = seqs.iterator().next();

            assertEquals(2, rseq.getCurrentMessageNr());
        } finally {
            if (null != sid1) {
                store.removeSourceSequence(sid1);
            }
            Collection<Long> msgNrs = Arrays.asList(
                ONE);
            store.removeMessages(sid1, msgNrs, true);
        }
    }

    @Test
    public void testCreateSequenceStoreInboundMessage() throws SQLException, IOException {
        Identifier sid1 = null;
        try {
            DestinationSequence seq = mock(DestinationSequence.class);
            sid1 = new Identifier();
            sid1.setValue("sequence1");
            EndpointReferenceType epr = RMUtils.createAnonymousReference();
            when(seq.getIdentifier()).thenReturn(sid1);
            when(seq.getAcksTo()).thenReturn(epr);
            when(seq.getEndpointIdentifier()).thenReturn(SERVER_ENDPOINT_ID);
            when(seq.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);

            store.createDestinationSequence(seq);


            Collection<DestinationSequence> seqs = store.getDestinationSequences(SERVER_ENDPOINT_ID);
            assertEquals(1, seqs.size());
            DestinationSequence rseq = seqs.iterator().next();
            assertFalse(rseq.isAcknowledged(1));

            Collection<RMMessage> in = store.getMessages(sid1, false);
            assertEquals(0, in.size());

            when(seq.getIdentifier()).thenReturn(sid1);
            when(seq.getAcknowledgment()).thenReturn(ack1);
            when(seq.getAcksTo()).thenReturn(epr);

            setupInboundMessage(seq, 1L, null);
            in = store.getMessages(sid1, false);
            assertEquals(1, in.size());
            checkRecoveredMessages(in);

            seqs = store.getDestinationSequences(SERVER_ENDPOINT_ID);
            assertEquals(1, seqs.size());
            rseq = seqs.iterator().next();
            assertTrue(rseq.isAcknowledged(1));
            assertFalse(rseq.isAcknowledged(10));

            when(seq.getIdentifier()).thenReturn(sid1);
            when(seq.getAcknowledgment()).thenReturn(ack2);
            when(seq.getAcksTo()).thenReturn(epr);

            store.persistIncoming(seq, null);

            seqs = store.getDestinationSequences(SERVER_ENDPOINT_ID);
            assertEquals(1, seqs.size());
            rseq = seqs.iterator().next();
            assertTrue(rseq.isAcknowledged(10));

        } finally {
            if (null != sid1) {
                store.removeDestinationSequence(sid1);
            }
            Collection<Long> msgNrs = Arrays.asList(
                ONE);
            store.removeMessages(sid1, msgNrs, false);
        }
    }

    private Identifier setupDestinationSequence(String s) throws IOException, SQLException {
        DestinationSequence seq = mock(DestinationSequence.class);

        Identifier sid = new Identifier();
        sid.setValue(s);
        EndpointReferenceType epr = RMUtils.createAnonymousReference();

        SequenceAcknowledgement ack = ack1;
        Long lmn = ZERO;
        ProtocolVariation pv = ProtocolVariation.RM10WSA200408;

        if ("sequence2".equals(s)) {
            ack = ack2;
            lmn = TEN;
            pv = ProtocolVariation.RM11WSA200508;
        }

        when(seq.getIdentifier()).thenReturn(sid);
        when(seq.getAcksTo()).thenReturn(epr);
        when(seq.getEndpointIdentifier()).thenReturn(SERVER_ENDPOINT_ID);
        when(seq.getLastMessageNumber()).thenReturn(lmn);
        when(seq.getAcknowledgment()).thenReturn(ack);
        when(seq.getIdentifier()).thenReturn(sid);
        when(seq.getProtocol()).thenReturn(pv);

        store.createDestinationSequence(seq);
        Connection con = getConnection();
        try {
            store.beginTransaction();
            store.updateDestinationSequence(con, seq);
            store.commit(con);
        } finally {
            releaseConnection(con);
        }

        return sid;
    }

    private Identifier setupSourceSequence(String s) throws SQLException {
        SourceSequence seq = mock(SourceSequence.class);
        Identifier sid = new Identifier();
        sid.setValue(s);

        Date expiry = null;
        Identifier osid = null;
        Long cmn = ONE;
        boolean lm = false;
        ProtocolVariation pv = ProtocolVariation.RM10WSA200408;

        if ("sequence2".equals(s)) {
            expiry = new Date(System.currentTimeMillis() + 3600 * 1000);
            osid = new Identifier();
            osid.setValue("offeringSequence");
            cmn = TEN;
            lm = true;
            pv = ProtocolVariation.RM11WSA200508;
        }

        when(seq.getIdentifier()).thenReturn(sid);
        when(seq.getExpires()).thenReturn(expiry);
        when(seq.getOfferingSequenceIdentifier()).thenReturn(osid);
        when(seq.getEndpointIdentifier()).thenReturn(CLIENT_ENDPOINT_ID);
        when(seq.getCurrentMessageNr()).thenReturn(cmn);
        when(seq.isLastMessage()).thenReturn(lm);
        when(seq.getIdentifier()).thenReturn(sid);
        when(seq.getProtocol()).thenReturn(pv);

        store.createSourceSequence(seq);
        Connection con = getConnection();
        try {
            store.beginTransaction();
            store.updateSourceSequence(con, seq);
            store.commit(con);
        } finally {
            releaseConnection(con);
        }

        return sid;
    }

    private void verifyDestinationSequence(String s, DestinationSequence seq) {
        Identifier sid = seq.getIdentifier();
        assertNotNull(sid);
        assertEquals(s, sid.getValue());
        if ("sequence1".equals(s)) {
            assertEquals(0, seq.getLastMessageNumber());
            SequenceAcknowledgement sa = seq.getAcknowledgment();
            assertNotNull(sa);
            verifyAcknowledgementRanges(sa.getAcknowledgementRange(), new long[]{1, 1});
            assertEquals(ProtocolVariation.RM10WSA200408, seq.getProtocol());
        } else if ("sequence2".equals(s)) {
            assertEquals(10, seq.getLastMessageNumber());
            SequenceAcknowledgement sa = seq.getAcknowledgment();
            assertNotNull(sa);
            verifyAcknowledgementRanges(sa.getAcknowledgementRange(), new long[]{1, 1, 3, 10});
            assertEquals(ProtocolVariation.RM11WSA200508, seq.getProtocol());
        }
    }

    private void verifySourceSequence(String s, SourceSequence seq) {
        Identifier sid = seq.getIdentifier();
        assertNotNull(sid);
        assertEquals(s, sid.getValue());
        if ("sequence1".equals(s)) {
            assertNull(seq.getExpires());
            assertEquals(1, seq.getCurrentMessageNr());
            assertFalse(seq.isLastMessage());
            assertEquals(ProtocolVariation.RM10WSA200408, seq.getProtocol());
        } else if ("sequence2".equals(s)) {
            Date expires = seq.getExpires();
            assertNotNull(expires);
            expires.after(new Date());
            assertEquals(10, seq.getCurrentMessageNr());
            assertTrue(seq.isLastMessage());
            assertEquals(ProtocolVariation.RM11WSA200508, seq.getProtocol());
        }
    }

    private void verifyAcknowledgementRanges(List<SequenceAcknowledgement.AcknowledgementRange> ranges,
                                             long[] values) {
        assertNotNull(ranges);
        assertEquals(values.length / 2, ranges.size());

        int v = 0;
        for (SequenceAcknowledgement.AcknowledgementRange range : ranges) {
            assertEquals(values[v++], (long)range.getLower());
            assertEquals(values[v++], (long)range.getUpper());
        }
    }

    private void setupMessage(Identifier sid, Long mn, String to, boolean outbound)
        throws IOException, SQLException  {
        RMMessage msg = createRMMessage(mn, to);

        Connection con = getConnection();
        try {
            store.beginTransaction();
            store.storeMessage(con, sid, msg, outbound);
            store.commit(con);
        } finally {
            releaseConnection(con);
        }
    }

    private void setupOutboundMessage(SourceSequence seq, long mn, String to)
        throws IOException, SQLException  {
        RMMessage msg = createRMMessage(ONE, to);
        store.persistOutgoing(seq, msg);
    }

    private void setupInboundMessage(DestinationSequence seq, long mn, String to)
        throws IOException, SQLException  {
        RMMessage msg = createRMMessage(ONE, to);
        store.persistIncoming(seq, msg);
    }

    private RMMessage createRMMessage(Long mn, String to) throws IOException {
        RMMessage msg = mock(RMMessage.class);
        when(msg.getMessageNumber()).thenReturn(mn);
        when(msg.getTo()).thenReturn(to);

        when(msg.getContentType()).thenReturn("text/xml");
        when(msg.getCreatedTime()).thenReturn(TIME);
        byte[] value = ("Message " + mn.longValue()).getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream(value);
        CachedOutputStream cos = new CachedOutputStream();
        IOUtils.copy(bais, cos);
        cos.flush();
        bais.close();
        when(msg.getContent()).thenReturn(cos);
        return msg;
    }

    private void checkRecoveredDestinationSequences(Collection<DestinationSequence> seqs) {

        for (DestinationSequence recovered : seqs) {
            assertTrue("sequence1".equals(recovered.getIdentifier().getValue())
                                          || "sequence2".equals(recovered.getIdentifier().getValue()));
            assertEquals(Names.WSA_ANONYMOUS_ADDRESS, recovered.getAcksTo().getAddress().getValue());
            if ("sequence1".equals(recovered.getIdentifier().getValue())) {
                assertEquals(0, recovered.getLastMessageNumber());
                assertEquals(1, recovered.getAcknowledgment().getAcknowledgementRange().size());
                SequenceAcknowledgement.AcknowledgementRange r =
                    recovered.getAcknowledgment().getAcknowledgementRange().get(0);
                assertEquals(ONE, r.getLower().longValue());
                assertEquals(ONE, r.getUpper().longValue());
                assertEquals(ProtocolVariation.RM10WSA200408, recovered.getProtocol());
            } else {
                assertEquals(10, recovered.getLastMessageNumber());
                assertEquals(2, recovered.getAcknowledgment().getAcknowledgementRange().size());
                SequenceAcknowledgement.AcknowledgementRange r =
                    recovered.getAcknowledgment().getAcknowledgementRange().get(0);
                assertEquals(ONE, r.getLower().longValue());
                assertEquals(ONE, r.getUpper().longValue());
                r = recovered.getAcknowledgment().getAcknowledgementRange().get(1);
                assertEquals(Long.valueOf(3), r.getLower());
                assertEquals(TEN, r.getUpper().longValue());
                assertEquals(ProtocolVariation.RM11WSA200508, recovered.getProtocol());
            }
        }
    }

    private void checkRecoveredSourceSequences(Collection<SourceSequence> seqs) {

        for (SourceSequence recovered : seqs) {
            assertTrue("sequence1".equals(recovered.getIdentifier().getValue())
                                          || "sequence2".equals(recovered.getIdentifier().getValue()));
            if ("sequence1".equals(recovered.getIdentifier().getValue())) {
                assertFalse(recovered.isLastMessage());
                assertEquals(1, recovered.getCurrentMessageNr());
                assertNull(recovered.getExpires());
                assertNull(recovered.getOfferingSequenceIdentifier());
                assertEquals(ProtocolVariation.RM10WSA200408, recovered.getProtocol());
            } else {
                assertTrue(recovered.isLastMessage());
                assertEquals(10, recovered.getCurrentMessageNr());
                assertNotNull(recovered.getExpires());
                assertEquals("offeringSequence", recovered.getOfferingSequenceIdentifier().getValue());
                assertEquals(ProtocolVariation.RM11WSA200508, recovered.getProtocol());
            }
        }
    }

    private void checkRecoveredMessages(Collection<RMMessage> msgs) {
        for (RMMessage msg : msgs) {
            long mn = msg.getMessageNumber();
            assertTrue(mn == 1 || mn == 10);
            if (mn == 10) {
                assertEquals(NON_ANON_ACKS_TO, msg.getTo());
            } else {
                assertNull(msg.getTo());
            }
            assertEquals(TIME, msg.getCreatedTime());
            try {
                InputStream actual = msg.getContent().getInputStream();
                assertEquals(new String("Message " + mn), IOUtils.readStringFromStream(actual));
            } catch (IOException e) {
                fail("failed to get the input stream");
            }
        }
    }
}