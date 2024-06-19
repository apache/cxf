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

package org.apache.cxf.bus.managers;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.DummyServer;
import org.apache.cxf.endpoint.Server;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ServerRegistryImpTest {

    @Test
    public void testServerRegistryPreShutdown() {
        ServerRegistryImpl serverRegistryImpl = new ServerRegistryImpl();
        Server server = new DummyServer(serverRegistryImpl);
        server.start();
        assertEquals("The serverList should have one service", serverRegistryImpl.serversList.size(), 1);
        serverRegistryImpl.preShutdown();
        assertEquals("The serverList should be clear ", serverRegistryImpl.serversList.size(), 0);
        serverRegistryImpl.postShutdown();
        assertEquals("The serverList should be clear ", serverRegistryImpl.serversList.size(), 0);
    }

    @Test
    public void testServerRegistry() {
        ServerRegistryImpl serverRegistryImpl = new ServerRegistryImpl();
        assertEquals(0, serverRegistryImpl.getServers().size());
    }

    @Test
    public void testBus() {
        ServerRegistryImpl serverRegistryImpl = new ServerRegistryImpl();
        Bus bus = serverRegistryImpl.getBus();
        Bus differentBus = BusFactory.getDefaultBus();
        serverRegistryImpl.setBus(differentBus);
        assertNotEquals(bus, differentBus);
        assertEquals(differentBus, serverRegistryImpl.getBus());
    }
}
