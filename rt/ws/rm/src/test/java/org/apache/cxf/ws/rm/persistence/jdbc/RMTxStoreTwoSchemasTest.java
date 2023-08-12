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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
/**
 *
 */
public class RMTxStoreTwoSchemasTest {
    private static final String TEST_DB_NAME = "rmdbts";

    private static final String CLIENT_ENDPOINT_ID =
        "{http://apache.org/greeter_control}GreeterService/GreeterPort";

    private static RMTxStore store1;
    private static RMTxStore store2;

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

    @Test
    public void testSetCurrentSchema() throws Exception {
        // schema should  have been set during initialisation
        // but verify the operation is idempotent
        store1.setCurrentSchema();
    }

    @Test
    public void testStoreIsolation() throws Exception {
        SourceSequence seq = mock(SourceSequence.class);
        Identifier sid1 = new Identifier();
        sid1.setValue("sequence1");
        when(seq.getIdentifier()).thenReturn(sid1);
        when(seq.getExpires()).thenReturn(null);
        when(seq.getOfferingSequenceIdentifier()).thenReturn(null);
        when(seq.getEndpointIdentifier()).thenReturn(CLIENT_ENDPOINT_ID);
        when(seq.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);

        store1.createSourceSequence(seq);

        SourceSequence rseq = store1.getSourceSequence(sid1);
        assertNotNull(rseq);

        rseq = store2.getSourceSequence(sid1);
        assertNull(rseq);

        when(seq.getIdentifier()).thenReturn(sid1);
        when(seq.getExpires()).thenReturn(null);
        when(seq.getOfferingSequenceIdentifier()).thenReturn(null);
        when(seq.getEndpointIdentifier()).thenReturn(CLIENT_ENDPOINT_ID);
        when(seq.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);

        store2.createSourceSequence(seq);

        rseq = store2.getSourceSequence(sid1);
        assertNotNull(rseq);

        // create another store
        RMTxStore store3 = createStore(null);
        store3.init();

        rseq = store3.getSourceSequence(sid1);
        assertNull(rseq);

        // switch to the store1's schema
        store3.setSchemaName(store1.getSchemaName());
        store3.init();

        rseq = store3.getSourceSequence(sid1);
        assertNotNull(rseq);
    }
}