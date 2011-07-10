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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.rm.DestinationSequence;
import org.apache.cxf.ws.rm.RMUtils;
import org.apache.cxf.ws.rm.SourceSequence;
import org.apache.cxf.ws.rm.persistence.RMMessage;
import org.apache.cxf.ws.rm.persistence.RMStoreException;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class RMTxStoreTest extends Assert {
    
    private static final String CLIENT_ENDPOINT_ID = 
        "celtix.{http://celtix.objectweb.org/greeter_control}GreeterService/GreeterPort";
    private static final String SERVER_ENDPOINT_ID = 
        "celtix.{http://celtix.objectweb.org/greeter_control}GreeterService";
    private static final String NON_ANON_ACKS_TO = 
        "http://localhost:9999/decoupled_endpoint";
    
    private static final Long ZERO = new Long(0);
    private static final Long ONE = new Long(1);
    private static final Long TEN = new Long(10);
    
    private static RMTxStore store;    
    private static SequenceAcknowledgement ack1;
    private static SequenceAcknowledgement ack2;
    
    private IMocksControl control;
    
    @BeforeClass 
    public static void setUpOnce() {
        
        RMTxStore.deleteDatabaseFiles();

        store = new RMTxStore();
        store.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
        store.init();
        
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
        range.setLower(new Long(3));
        range.setUpper(TEN);
        ack2.getAcknowledgementRange().add(range);
    }
    
    @AfterClass
    public static void tearDownOnce() {
        /*
        try {
            store.getConnection().close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        */
        RMTxStore.deleteDatabaseFiles(RMTxStore.DEFAULT_DATABASE_NAME, false);
    }
    
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();        
    }
       
    @Test
    public void testCreateTables() throws SQLException {
        // tables should  have been created during initialisation
        // but verify the operation is idempotent
        store.createTables();     
    }
    
    @Test
    public void testCreateDeleteSrcSequences() {
        SourceSequence seq = control.createMock(SourceSequence.class);
        Identifier sid1 = new Identifier();
        sid1.setValue("sequence1");
        EasyMock.expect(seq.getIdentifier()).andReturn(sid1);
        EasyMock.expect(seq.getExpires()).andReturn(null);
        EasyMock.expect(seq.getOfferingSequenceIdentifier()).andReturn(null);
        EasyMock.expect(seq.getEndpointIdentifier()).andReturn(CLIENT_ENDPOINT_ID);
        
        control.replay();
        store.createSourceSequence(seq);   
        control.verify();
        
        control.reset();
        EasyMock.expect(seq.getIdentifier()).andReturn(sid1);
        EasyMock.expect(seq.getExpires()).andReturn(null);
        EasyMock.expect(seq.getOfferingSequenceIdentifier()).andReturn(null);
        EasyMock.expect(seq.getEndpointIdentifier()).andReturn(CLIENT_ENDPOINT_ID);
        
        control.replay();
        try {
            store.createSourceSequence(seq);  
            fail("Expected RMStoreException was not thrown.");
        } catch (RMStoreException ex) {
            SQLException se = (SQLException)ex.getCause();
            // duplicate key value 
            assertEquals("23505", se.getSQLState());
        }
        control.verify();
        
        control.reset();
        Identifier sid2 = new Identifier();
        sid2.setValue("sequence2");
        EasyMock.expect(seq.getIdentifier()).andReturn(sid2);  
        EasyMock.expect(seq.getExpires()).andReturn(new Date());
        Identifier sid3 = new Identifier();
        sid3.setValue("offeringSequence3");
        EasyMock.expect(seq.getOfferingSequenceIdentifier()).andReturn(sid3);
        EasyMock.expect(seq.getEndpointIdentifier()).andReturn(SERVER_ENDPOINT_ID);
        
         
        control.replay();
        store.createSourceSequence(seq);   
        control.verify();
            
        store.removeSourceSequence(sid1);
        store.removeSourceSequence(sid2);
        
        // deleting once again is a no-op
        store.removeSourceSequence(sid2);       
    }
    
    @Test
    public void testCreateDeleteDestSequences() {
        DestinationSequence seq = control.createMock(DestinationSequence.class);
        Identifier sid1 = new Identifier();
        sid1.setValue("sequence1");
        EndpointReferenceType epr = RMUtils.createAnonymousReference();
        EasyMock.expect(seq.getIdentifier()).andReturn(sid1);
        EasyMock.expect(seq.getAcksTo()).andReturn(epr);        
        EasyMock.expect(seq.getEndpointIdentifier()).andReturn(SERVER_ENDPOINT_ID);
        
        control.replay();
        store.createDestinationSequence(seq);   
        control.verify();
        
        control.reset();
        EasyMock.expect(seq.getIdentifier()).andReturn(sid1);
        EasyMock.expect(seq.getAcksTo()).andReturn(epr);        
        EasyMock.expect(seq.getEndpointIdentifier()).andReturn(SERVER_ENDPOINT_ID);
        
        control.replay();
        try {
            store.createDestinationSequence(seq);  
            fail("Expected RMStoreException was not thrown.");
        } catch (RMStoreException ex) {
            SQLException se = (SQLException)ex.getCause();
            // duplicate key value 
            assertEquals("23505", se.getSQLState());
        }
        control.verify();
        
        control.reset();
        Identifier sid2 = new Identifier();
        sid2.setValue("sequence2");
        EasyMock.expect(seq.getIdentifier()).andReturn(sid2); 
        epr = RMUtils.createReference(NON_ANON_ACKS_TO);
        EasyMock.expect(seq.getAcksTo()).andReturn(epr);
        EasyMock.expect(seq.getEndpointIdentifier()).andReturn(CLIENT_ENDPOINT_ID);
        
        control.replay();
        store.createDestinationSequence(seq);   
        control.verify();
            
        store.removeDestinationSequence(sid1);
        store.removeDestinationSequence(sid2);
        
        // deleting once again is a no-op
        store.removeDestinationSequence(sid2);        
    }
    
    @Test
    public void testCreateDeleteMessages() throws IOException, SQLException  {
        RMMessage msg = control.createMock(RMMessage.class);
        Identifier sid1 = new Identifier();
        sid1.setValue("sequence1");
        EasyMock.expect(msg.getMessageNumber()).andReturn(ONE).times(2); 
        byte[] bytes = new byte[89];
        EasyMock.expect(msg.getInputStream()).andReturn(new ByteArrayInputStream(bytes));
        EasyMock.expect(msg.getSize()).andReturn(bytes.length);
        
        control.replay();
        store.beginTransaction();
        store.storeMessage(sid1, msg, true);
        store.storeMessage(sid1, msg, false);
        store.commit();
        control.verify();
        
        control.reset();
        EasyMock.expect(msg.getMessageNumber()).andReturn(ONE); 
        EasyMock.expect(msg.getInputStream()).andReturn(new ByteArrayInputStream(bytes));
        EasyMock.expect(msg.getSize()).andReturn(bytes.length);
        
        control.replay();
        store.beginTransaction();
        try {
            store.storeMessage(sid1, msg, true);
        } catch (SQLException ex) {
            assertEquals("23505", ex.getSQLState());
        }
        store.abort();
        control.verify();
        
        control.reset();
        EasyMock.expect(msg.getMessageNumber()).andReturn(TEN).times(2); 
        EasyMock.expect(msg.getInputStream()).andReturn(new ByteArrayInputStream(bytes)); 
        EasyMock.expect(msg.getSize()).andReturn(bytes.length);
        
        control.replay();
        store.beginTransaction();
        store.storeMessage(sid1, msg, true);
        store.storeMessage(sid1, msg, false);
        store.commit();
        control.verify();
        
        Collection<Long> messageNrs = new ArrayList<Long>();
        messageNrs.add(ZERO);
        messageNrs.add(TEN);
        messageNrs.add(ONE);
        messageNrs.add(TEN);
        
        store.removeMessages(sid1, messageNrs, true);
        store.removeMessages(sid1, messageNrs, false);
        
        Identifier sid2 = new Identifier();
        sid1.setValue("sequence2");
        store.removeMessages(sid2, messageNrs, true);
    }
    
    @Test
    public void testUpdateDestinationSequence() throws SQLException, IOException {
        DestinationSequence seq = control.createMock(DestinationSequence.class);
        Identifier sid1 = new Identifier();
        sid1.setValue("sequence1");
        EndpointReferenceType epr = RMUtils.createAnonymousReference();
        EasyMock.expect(seq.getIdentifier()).andReturn(sid1);
        EasyMock.expect(seq.getAcksTo()).andReturn(epr);        
        EasyMock.expect(seq.getEndpointIdentifier()).andReturn(SERVER_ENDPOINT_ID);
        
        control.replay();
        store.createDestinationSequence(seq);   
        control.verify();
        
        control.reset();
        EasyMock.expect(seq.getLastMessageNumber()).andReturn(null);
        EasyMock.expect(seq.getAcknowledgment()).andReturn(ack1);        
        EasyMock.expect(seq.getIdentifier()).andReturn(sid1);
        
        control.replay();
        store.beginTransaction();
        store.updateDestinationSequence(seq);
        store.abort();
        
        control.reset();
        EasyMock.expect(seq.getLastMessageNumber()).andReturn(TEN);
        EasyMock.expect(seq.getAcknowledgment()).andReturn(ack1);        
        EasyMock.expect(seq.getIdentifier()).andReturn(sid1);
        
        control.replay();
        store.beginTransaction();
        store.updateDestinationSequence(seq);
        store.abort();
        
        store.removeDestinationSequence(sid1);
    }
    
    @Test
    public void testUpdateSourceSequence() throws SQLException {
        SourceSequence seq = control.createMock(SourceSequence.class);
        Identifier sid1 = new Identifier();
        sid1.setValue("sequence1");
        EasyMock.expect(seq.getIdentifier()).andReturn(sid1);
        EasyMock.expect(seq.getExpires()).andReturn(null);
        EasyMock.expect(seq.getOfferingSequenceIdentifier()).andReturn(null);
        EasyMock.expect(seq.getEndpointIdentifier()).andReturn(CLIENT_ENDPOINT_ID);
        
        control.replay();
        store.createSourceSequence(seq);   
        control.verify();        
        
        control.reset();
        EasyMock.expect(seq.getCurrentMessageNr()).andReturn(ONE);
        EasyMock.expect(seq.isLastMessage()).andReturn(false);
        EasyMock.expect(seq.getIdentifier()).andReturn(sid1);   
        
        control.replay();
        store.beginTransaction();
        store.updateSourceSequence(seq);
        store.abort();
        
        control.reset();
        EasyMock.expect(seq.getCurrentMessageNr()).andReturn(TEN);
        EasyMock.expect(seq.isLastMessage()).andReturn(true);  
        EasyMock.expect(seq.getIdentifier()).andReturn(sid1);
        
        control.replay();
        store.beginTransaction();
        store.updateSourceSequence(seq);
        store.abort();
        
        store.removeSourceSequence(sid1);
        
    }

    @Test
    public void testGetDestinationSequences() throws SQLException, IOException {
        
        Identifier sid1 = null;
        Identifier sid2 = null;
        
        Collection<DestinationSequence> seqs = store.getDestinationSequences("unknown");
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
        
        Collection<SourceSequence> seqs = store.getSourceSequences("unknown");
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
        
        DestinationSequence seq = store.getDestinationSequence(new Identifier());
        assertNull(seq);

        try {
            sid1 = setupDestinationSequence("sequence1");

            seq = store.getDestinationSequence(sid1);
            assertNotNull(seq);

            sid2 = setupDestinationSequence("sequence2");
            seq = store.getDestinationSequence(sid2);
            assertNotNull(seq);
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
    public void testGetSourceSequence() throws SQLException {
        
        Identifier sid1 = null;
        Identifier sid2 = null;
        
        SourceSequence seq = store.getSourceSequence(new Identifier());
        assertNull(seq);
        
        try {
            sid1 = setupSourceSequence("sequence1");

            seq = store.getSourceSequence(sid1);
            assertNotNull(seq);

            sid2 = setupSourceSequence("sequence2");
            seq = store.getSourceSequence(sid2);
            assertNotNull(seq);
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
        assertEquals(0, out.size());
        
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
            Collection<Long> msgNrs = new ArrayList<Long>();
            msgNrs.add(ONE);
            msgNrs.add(TEN);
         
            store.removeMessages(sid1, msgNrs, true);
            store.removeMessages(sid1, msgNrs, false);
        }
    }
    
    private Identifier setupDestinationSequence(String s) throws IOException, SQLException {
        DestinationSequence seq = control.createMock(DestinationSequence.class);
        
        Identifier sid = new Identifier();
        sid.setValue(s);
        EndpointReferenceType epr = RMUtils.createAnonymousReference();
        
        SequenceAcknowledgement ack = ack1;
        Long lmn = ZERO;
         
        if ("sequence2".equals(s)) {
            ack = ack2;
            lmn = TEN;
        }
        
        EasyMock.expect(seq.getIdentifier()).andReturn(sid);
        EasyMock.expect(seq.getAcksTo()).andReturn(epr);
        EasyMock.expect(seq.getEndpointIdentifier()).andReturn(SERVER_ENDPOINT_ID);
        EasyMock.expect(seq.getLastMessageNumber()).andReturn(lmn);
        EasyMock.expect(seq.getAcknowledgment()).andReturn(ack);
        EasyMock.expect(seq.getIdentifier()).andReturn(sid);
        
        control.replay();
        store.createDestinationSequence(seq);           
        store.beginTransaction();
        store.updateDestinationSequence(seq);        
        store.commit();
        control.reset();
        
        return sid;
    }
    
    private Identifier setupSourceSequence(String s) throws SQLException {
        SourceSequence seq = control.createMock(SourceSequence.class);        
        Identifier sid = new Identifier();
        sid.setValue(s);      
            
        Date expiry = null;
        Identifier osid = null;
        Long cmn = ONE;
        boolean lm = false;
        
        if ("sequence2".equals(s)) {
            expiry = new Date(System.currentTimeMillis() + 3600 * 1000);
            osid = new Identifier();
            osid.setValue("offeringSequence");
            cmn = TEN;
            lm = true;            
        } 
        
        EasyMock.expect(seq.getIdentifier()).andReturn(sid);
        EasyMock.expect(seq.getExpires()).andReturn(expiry);
        EasyMock.expect(seq.getOfferingSequenceIdentifier()).andReturn(osid);
        EasyMock.expect(seq.getEndpointIdentifier()).andReturn(CLIENT_ENDPOINT_ID);
        EasyMock.expect(seq.getCurrentMessageNr()).andReturn(cmn);
        EasyMock.expect(seq.isLastMessage()).andReturn(lm);
        EasyMock.expect(seq.getIdentifier()).andReturn(sid);
        
        control.replay();
        store.createSourceSequence(seq);           
        store.beginTransaction();
        store.updateSourceSequence(seq); 
        store.commit();
        control.reset();
        
        return sid;
    }
    
    private void setupMessage(Identifier sid, Long mn, String to, boolean outbound) 
        throws IOException, SQLException  {
        RMMessage msg = control.createMock(RMMessage.class);
        EasyMock.expect(msg.getMessageNumber()).andReturn(mn);
        EasyMock.expect(msg.getTo()).andReturn(to);
        byte[] value = ("Message " + mn.longValue()).getBytes();
        EasyMock.expect(msg.getInputStream()).andReturn(new ByteArrayInputStream(value));
        EasyMock.expect(msg.getSize()).andReturn(value.length);
        
        control.replay();
        store.beginTransaction();
        store.storeMessage(sid, msg, outbound);        
        store.commit();
        control.reset();
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
                assertEquals(ONE, r.getLower());
                assertEquals(ONE, r.getUpper());
            } else {
                assertEquals(10, recovered.getLastMessageNumber());
                assertEquals(2, recovered.getAcknowledgment().getAcknowledgementRange().size());
                SequenceAcknowledgement.AcknowledgementRange r = 
                    recovered.getAcknowledgment().getAcknowledgementRange().get(0);
                assertEquals(ONE, r.getLower());
                assertEquals(ONE, r.getUpper());
                r = recovered.getAcknowledgment().getAcknowledgementRange().get(1);
                assertEquals(new Long(3), r.getLower());
                assertEquals(TEN, r.getUpper());                
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
            } else {
                assertTrue(recovered.isLastMessage());
                assertEquals(10, recovered.getCurrentMessageNr()); 
                assertNotNull(recovered.getExpires());
                assertEquals("offeringSequence", recovered.getOfferingSequenceIdentifier().getValue());
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
            try {
                InputStream actual = msg.getInputStream();
                assertEquals(new String("Message " + mn), IOUtils.readStringFromStream(actual));
            } catch (IOException e) {
                fail("failed to get the input stream");
            }
        }
    }
}