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

import java.sql.Connection;
import java.sql.SQLException;
import org.apache.cxf.ws.rm.ProtocolVariation;
import org.apache.cxf.ws.rm.RMUtils;
import org.apache.cxf.ws.rm.SourceSequence;
import org.apache.cxf.ws.rm.persistence.RMStoreException;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.easymock.EasyMock;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class RMTxStoreTest extends RMTxStoreTestBase {
    @BeforeClass 
    public static void setUpOnce() {
        RMTxStoreTestBase.setUpOnce();
        
        RMTxStore.deleteDatabaseFiles();

        store = new RMTxStore();
        store.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
        store.init();
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
    
    
    @Test
    public void testReconnect() throws Exception {
        // set the initial reconnect delay to 100 msec for testing
        long ird = store.getInitialReconnectDelay();
        store.setInitialReconnectDelay(100);
        
        SourceSequence seq = control.createMock(SourceSequence.class);
        Identifier sid1 = RMUtils.getWSRMFactory().createIdentifier();
        sid1.setValue("sequence1");
        EasyMock.expect(seq.getIdentifier()).andReturn(sid1);
        EasyMock.expect(seq.getExpires()).andReturn(null);
        EasyMock.expect(seq.getOfferingSequenceIdentifier()).andReturn(null);
        EasyMock.expect(seq.getEndpointIdentifier()).andReturn(CLIENT_ENDPOINT_ID);
        EasyMock.expect(seq.getProtocol()).andReturn(ProtocolVariation.RM10WSA200408);
        
        // intentionally invalidate the connection
        try {
            store.getConnection().close();
        } catch (SQLException ex) {
            // ignore
        }
        
        control.replay();
        try {
            store.createSourceSequence(seq);  
            fail("Expected RMStoreException was not thrown.");
        } catch (RMStoreException ex) {
            SQLException se = (SQLException)ex.getCause();
            // expects a transient or non-transient connection exception
            assertTrue(se.getSQLState().startsWith("08"));
        }
        
        // wait 200 msecs to make sure an reconnect is attempted
        Thread.sleep(200);
        
        control.reset();
        EasyMock.expect(seq.getIdentifier()).andReturn(sid1);
        EasyMock.expect(seq.getExpires()).andReturn(null);
        EasyMock.expect(seq.getOfferingSequenceIdentifier()).andReturn(null);
        EasyMock.expect(seq.getEndpointIdentifier()).andReturn(CLIENT_ENDPOINT_ID);
        EasyMock.expect(seq.getProtocol()).andReturn(ProtocolVariation.RM10WSA200408);
        
        control.replay();
        store.createSourceSequence(seq);
        control.verify();
        
        // revert to the old initial reconnect delay
        store.setInitialReconnectDelay(ird);
        
        store.removeSourceSequence(sid1);
    }

    @Override
    protected Connection getConnection() {
        return store.verifyConnection();
    }

    @Override
    protected void releaseConnection(Connection con) {
        // the connection is held in the store, so not close it until the store is disposed.
    }
}