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

import java.io.IOException;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.ws.addressing.v200408.EndpointReferenceType;
import org.apache.cxf.ws.rm.DestinationSequence;
import org.apache.cxf.ws.rm.Identifier;
import org.apache.cxf.ws.rm.RMConstants;
import org.apache.cxf.ws.rm.RMUtils;
import org.apache.cxf.ws.rm.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.SourceSequence;
import org.apache.cxf.ws.rm.persistence.RMMessage;
import org.apache.cxf.ws.rm.persistence.RMStoreException;
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
        range.setLower(BigInteger.ONE);
        range.setUpper(BigInteger.ONE);
        ack1.getAcknowledgementRange().add(range);
        
        ack2 = new SequenceAcknowledgement();
        range = new SequenceAcknowledgement.AcknowledgementRange();
        range.setLower(BigInteger.ONE);
        range.setUpper(BigInteger.ONE);
        ack2.getAcknowledgementRange().add(range);
        range = new SequenceAcknowledgement.AcknowledgementRange();
        range.setLower(new BigInteger("3"));
        range.setUpper(BigInteger.TEN);
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
        Identifier sid1 = RMUtils.getWSRMFactory().createIdentifier();
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
        Identifier sid2 = RMUtils.getWSRMFactory().createIdentifier();
        sid2.setValue("sequence2");
        EasyMock.expect(seq.getIdentifier()).andReturn(sid2);  
        EasyMock.expect(seq.getExpires()).andReturn(new Date());
        Identifier sid3 = RMUtils.getWSRMFactory().createIdentifier();
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
        Identifier sid1 = RMUtils.getWSRMFactory().createIdentifier();
        sid1.setValue("sequence1");
        EndpointReferenceType epr = RMUtils.createAnonymousReference2004();
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
        Identifier sid2 = RMUtils.getWSRMFactory().createIdentifier();
        sid2.setValue("sequence2");
        EasyMock.expect(seq.getIdentifier()).andReturn(sid2); 
        epr = RMUtils.createReference2004(NON_ANON_ACKS_TO);
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
        Identifier sid1 = RMUtils.getWSRMFactory().createIdentifier();
        sid1.setValue("sequence1");
        EasyMock.expect(msg.getMessageNumber()).andReturn(BigInteger.ONE).times(2); 
        byte[] bytes = new byte[89];
        EasyMock.expect(msg.getContent()).andReturn(bytes).times(2);
        
        control.replay();
        store.beginTransaction();
        store.storeMessage(sid1, msg, true);
        store.storeMessage(sid1, msg, false);
        store.commit();
        control.verify();
        
        control.reset();
        EasyMock.expect(msg.getMessageNumber()).andReturn(BigInteger.ONE); 
        EasyMock.expect(msg.getContent()).andReturn(bytes);
        
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
        EasyMock.expect(msg.getMessageNumber()).andReturn(BigInteger.TEN).times(2); 
        EasyMock.expect(msg.getContent()).andReturn(bytes).times(2); 
        
        control.replay();
        store.beginTransaction();
        store.storeMessage(sid1, msg, true);
        store.storeMessage(sid1, msg, false);
        store.commit();
        control.verify();
        
        Collection<BigInteger> messageNrs = new ArrayList<BigInteger>();
        messageNrs.add(BigInteger.ZERO);
        messageNrs.add(BigInteger.TEN);
        messageNrs.add(BigInteger.ONE);
        messageNrs.add(BigInteger.TEN);
        
        store.removeMessages(sid1, messageNrs, true);
        store.removeMessages(sid1, messageNrs, false);
        
        Identifier sid2 = RMUtils.getWSRMFactory().createIdentifier();
        sid1.setValue("sequence2");
        store.removeMessages(sid2, messageNrs, true);
    }
    
    @Test
    public void testUpdateDestinationSequence() throws SQLException, IOException {
        DestinationSequence seq = control.createMock(DestinationSequence.class);
        Identifier sid1 = RMUtils.getWSRMFactory().createIdentifier();
        sid1.setValue("sequence1");
        EndpointReferenceType epr = RMUtils.createAnonymousReference2004();
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
        EasyMock.expect(seq.getLastMessageNumber()).andReturn(BigInteger.TEN);
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
        Identifier sid1 = RMUtils.getWSRMFactory().createIdentifier();
        sid1.setValue("sequence1");
        EasyMock.expect(seq.getIdentifier()).andReturn(sid1);
        EasyMock.expect(seq.getExpires()).andReturn(null);
        EasyMock.expect(seq.getOfferingSequenceIdentifier()).andReturn(null);
        EasyMock.expect(seq.getEndpointIdentifier()).andReturn(CLIENT_ENDPOINT_ID);
        
        control.replay();
        store.createSourceSequence(seq);   
        control.verify();        
        
        control.reset();
        EasyMock.expect(seq.getCurrentMessageNr()).andReturn(BigInteger.ONE);
        EasyMock.expect(seq.isLastMessage()).andReturn(false);
        EasyMock.expect(seq.getIdentifier()).andReturn(sid1);   
        
        control.replay();
        store.beginTransaction();
        store.updateSourceSequence(seq);
        store.abort();
        
        control.reset();
        EasyMock.expect(seq.getCurrentMessageNr()).andReturn(BigInteger.TEN);
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
    public void testGetSourceSequences() throws SQLException, IOException {
        
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
    public void testGetMessages() throws SQLException, IOException {
        
        Identifier sid1 = RMUtils.getWSRMFactory().createIdentifier();
        sid1.setValue("sequence1");
        Identifier sid2 = RMUtils.getWSRMFactory().createIdentifier();
        sid2.setValue("sequence2");
        
        Collection<RMMessage> out = store.getMessages(sid1, true);
        assertEquals(0, out.size());
        Collection<RMMessage> in = store.getMessages(sid1, false);
        assertEquals(0, out.size());
        
        try {
            setupMessage(sid1, BigInteger.ONE, null, true);
            setupMessage(sid1, BigInteger.ONE, null, false);

            out = store.getMessages(sid1, true);
            assertEquals(1, out.size());
            checkRecoveredMessages(out);
            
            in = store.getMessages(sid1, false);
            assertEquals(1, in.size());
            checkRecoveredMessages(in);
            
            setupMessage(sid1, BigInteger.TEN, NON_ANON_ACKS_TO, true);
            setupMessage(sid1, BigInteger.TEN, NON_ANON_ACKS_TO, false);
            
            out = store.getMessages(sid1, true);
            assertEquals(2, out.size());
            checkRecoveredMessages(out);
            
            in = store.getMessages(sid1, false);
            assertEquals(2, in.size());
            checkRecoveredMessages(in);
        } finally {
            Collection<BigInteger> msgNrs = new ArrayList<BigInteger>();
            msgNrs.add(BigInteger.ONE);
            msgNrs.add(BigInteger.TEN);
         
            store.removeMessages(sid1, msgNrs, true);
            store.removeMessages(sid1, msgNrs, false);
        }
    }
    
    private Identifier setupDestinationSequence(String s) throws IOException, SQLException {
        DestinationSequence seq = control.createMock(DestinationSequence.class);
        
        Identifier sid = RMUtils.getWSRMFactory().createIdentifier();
        sid.setValue(s);
        EndpointReferenceType epr = RMUtils.createAnonymousReference2004();
        
        SequenceAcknowledgement ack = ack1;
        BigInteger lmn = null;
         
        if ("sequence2".equals(s)) {
            ack = ack2;
            lmn = BigInteger.TEN;
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
    
    private Identifier setupSourceSequence(String s) throws IOException, SQLException {
        SourceSequence seq = control.createMock(SourceSequence.class);        
        Identifier sid = RMUtils.getWSRMFactory().createIdentifier();
        sid.setValue(s);      
            
        Date expiry = null;
        Identifier osid = null;
        BigInteger cmn = BigInteger.ONE;
        boolean lm = false;
        
        if ("sequence2".equals(s)) {
            expiry = new Date(System.currentTimeMillis() + 3600 * 1000);
            osid = RMUtils.getWSRMFactory().createIdentifier();
            osid.setValue("offeringSequence");
            cmn = BigInteger.TEN;
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
    
    private void setupMessage(Identifier sid, BigInteger mn, String to, boolean outbound) 
        throws IOException, SQLException  {
        RMMessage msg = control.createMock(RMMessage.class);
        EasyMock.expect(msg.getMessageNumber()).andReturn(mn);
        EasyMock.expect(msg.getTo()).andReturn(to);
        String value = "Message " + mn.longValue();
        EasyMock.expect(msg.getContent()).andReturn(value.getBytes());
        
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
            assertEquals(RMConstants.getAnonymousAddress(), recovered.getAcksTo().getAddress().getValue());
            if ("sequence1".equals(recovered.getIdentifier().getValue())) {                      
                assertNull(recovered.getLastMessageNumber());                
                assertEquals(1, recovered.getAcknowledgment().getAcknowledgementRange().size());
                SequenceAcknowledgement.AcknowledgementRange r = 
                    recovered.getAcknowledgment().getAcknowledgementRange().get(0);
                assertEquals(BigInteger.ONE, r.getLower());
                assertEquals(BigInteger.ONE, r.getUpper());
            } else {
                assertEquals(BigInteger.TEN, recovered.getLastMessageNumber());
                assertEquals(2, recovered.getAcknowledgment().getAcknowledgementRange().size());
                SequenceAcknowledgement.AcknowledgementRange r = 
                    recovered.getAcknowledgment().getAcknowledgementRange().get(0);
                assertEquals(BigInteger.ONE, r.getLower());
                assertEquals(BigInteger.ONE, r.getUpper());
                r = recovered.getAcknowledgment().getAcknowledgementRange().get(1);
                assertEquals(new BigInteger("3"), r.getLower());
                assertEquals(BigInteger.TEN, r.getUpper());                
            }
        }
    }
    
    private void checkRecoveredSourceSequences(Collection<SourceSequence> seqs) {
        
        for (SourceSequence recovered : seqs) {
            assertTrue("sequence1".equals(recovered.getIdentifier().getValue())
                                          || "sequence2".equals(recovered.getIdentifier().getValue()));
            if ("sequence1".equals(recovered.getIdentifier().getValue())) {                      
                assertFalse(recovered.isLastMessage());
                assertEquals(BigInteger.ONE, recovered.getCurrentMessageNr());  
                assertNull(recovered.getExpires());
                assertNull(recovered.getOfferingSequenceIdentifier());
            } else {
                assertTrue(recovered.isLastMessage());
                assertEquals(BigInteger.TEN, recovered.getCurrentMessageNr()); 
                assertNotNull(recovered.getExpires());
                assertEquals("offeringSequence", recovered.getOfferingSequenceIdentifier().getValue());
            }
        }
    }
    
    private void checkRecoveredMessages(Collection<RMMessage> msgs) {
        for (RMMessage msg : msgs) {
            BigInteger mn = msg.getMessageNumber();
            assertTrue(BigInteger.ONE.equals(mn) || BigInteger.TEN.equals(mn));
            if (BigInteger.TEN.equals(mn)) {
                assertEquals(NON_ANON_ACKS_TO, msg.getTo());
            } else {
                assertNull(msg.getTo());
            }
            byte[] actual = msg.getContent();
            assertEquals(new String("Message " + mn.longValue()), IOUtils.newStringFromBytes(actual));
        }
    }
    
    

}
