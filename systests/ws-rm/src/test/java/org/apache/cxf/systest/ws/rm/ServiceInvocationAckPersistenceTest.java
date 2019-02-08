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

import org.apache.cxf.ws.rm.persistence.jdbc.RMTxStore;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Tests the acknowledgement delivery back to the non-decoupled port when there is some
 * error at the provider side and how its behavior is affected by the robust in-only mode setting.
 */
public class ServiceInvocationAckPersistenceTest extends ServiceInvocationAckBase {
    static final String PORT = allocatePort(ServiceInvocationAckPersistenceTest.class);

    @BeforeClass
    public static void startServers() throws Exception {
        RMTxStore.deleteDatabaseFiles("synack", true);
        RMTxStore.deleteDatabaseFiles("synack-server", true);
        startServer(PORT, "synack");
    }
    public String getPort() {
        return PORT;
    }
    public String getPrefix() {
        return "synack";
    }
    @AfterClass
    public static void cleanUpDerby() throws Exception {
        RMTxStore.deleteDatabaseFiles("synack", true);
        RMTxStore.deleteDatabaseFiles("synack-server", true);
    }

    protected void setupGreeter() throws Exception {
        setupGreeter("org/apache/cxf/systest/ws/rm/sync-ack-persistent-server.xml");
    }
}
