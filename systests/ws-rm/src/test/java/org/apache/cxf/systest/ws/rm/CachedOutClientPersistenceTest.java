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

package org.apache.cxf.systest.ws.rm;

import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.ws.rm.persistence.jdbc.RMTxStore;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A simulated-large message version of ClientPersistenceTest.
 */
public class CachedOutClientPersistenceTest extends AbstractClientPersistenceTest {
    private static final String PORT = allocatePort(CachedOutClientPersistenceTest.class);
    
    @BeforeClass
    public static void startServers() throws Exception {
        startServers(PORT, "cocpt");
        CachedOutputStream.setDefaultThreshold(16);
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        CachedOutputStream.setDefaultThreshold(-1);
        RMTxStore.deleteDatabaseFiles("cocpt-server", true);
        RMTxStore.deleteDatabaseFiles("cocpt-client", true);
    }
    
    @Test 
    public void testRecovery() throws Exception {
        super.testRecovery();
    }

    @Override
    public String getPort() {
        return PORT;
    }

    @Override
    public String getPrefix() {
        return "cocpt";
    }

}
