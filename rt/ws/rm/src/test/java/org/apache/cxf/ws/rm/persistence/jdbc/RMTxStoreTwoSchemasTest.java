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

import java.text.MessageFormat;

import org.apache.cxf.ws.rm.ProtocolVariation;
import org.apache.cxf.ws.rm.SourceSequence;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class RMTxStoreTwoSchemasTest extends Assert {
    private static final String TEST_DB_NAME = "rmdb3";

    private static final String CLIENT_ENDPOINT_ID = 
        "{http://apache.org/greeter_control}GreeterService/GreeterPort";
    
    private static RMTxStore store1;
    private static RMTxStore store2;

    private IMocksControl control;
    
    @BeforeClass 
    public static void setUpOnce() {
        RMTxStore.deleteDatabaseFiles(TEST_DB_NAME, true);

        store1 = createStore("ONE");
        store2 = createStore("TWO");
    }
    
    @AfterClass
    public static void tearDownOnce() {
        RMTxStore.deleteDatabaseFiles(TEST_DB_NAME, false);
    }

    private static RMTxStore createStore(String sn) {
        RMTxStore store = new RMTxStore();
        store.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
        
        // workaround for the db file deletion problem during the tests
        store.setUrl(MessageFormat.format("jdbc:derby:{0};create=true", TEST_DB_NAME));

        // use the specified schema 
        store.setSchemaName(sn);
        store.init();
        
        return store;
    }

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();        
    }
    
    @Test
    public void testSetCurrentSchema() throws Exception {
        // schema should  have been set during initialisation
        // but verify the operation is idempotent
        store1.setCurrentSchema();
    }
    
    @Test
    public void testStoreIsolation() throws Exception {
        SourceSequence seq = control.createMock(SourceSequence.class);
        Identifier sid1 = new Identifier();
        sid1.setValue("sequence1");
        EasyMock.expect(seq.getIdentifier()).andReturn(sid1);
        EasyMock.expect(seq.getExpires()).andReturn(null);
        EasyMock.expect(seq.getOfferingSequenceIdentifier()).andReturn(null);
        EasyMock.expect(seq.getEndpointIdentifier()).andReturn(CLIENT_ENDPOINT_ID);
        // for 2.5.x, this is not called
//        EasyMock.expect(seq.getProtocol()).andReturn(ProtocolVariation.RM10WSA200408);
        
        control.replay();
        store1.createSourceSequence(seq);   
        control.verify();        
        
        SourceSequence rseq = store1.getSourceSequence(sid1, ProtocolVariation.RM10WSA200408);
        assertNotNull(rseq);
        
        rseq = store2.getSourceSequence(sid1, ProtocolVariation.RM10WSA200408);
        assertNull(rseq);
        
        control.reset();
        EasyMock.expect(seq.getIdentifier()).andReturn(sid1);
        EasyMock.expect(seq.getExpires()).andReturn(null);
        EasyMock.expect(seq.getOfferingSequenceIdentifier()).andReturn(null);
        EasyMock.expect(seq.getEndpointIdentifier()).andReturn(CLIENT_ENDPOINT_ID);
        // for 2.5.x, this is not called
//        EasyMock.expect(seq.getProtocol()).andReturn(ProtocolVariation.RM10WSA200408);
        
        control.replay();
        store2.createSourceSequence(seq);   
        control.verify();
        
        rseq = store2.getSourceSequence(sid1, ProtocolVariation.RM10WSA200408);
        assertNotNull(rseq);
 
        // create another store
        RMTxStore store3 = createStore(null);
        store3.init();

        rseq = store3.getSourceSequence(sid1, ProtocolVariation.RM10WSA200408);
        assertNull(rseq);

        // switch to the store1's schema
        store3.setSchemaName(store1.getSchemaName());
        store3.init();
        
        rseq = store3.getSourceSequence(sid1, ProtocolVariation.RM10WSA200408);
        assertNotNull(rseq);
    }
}