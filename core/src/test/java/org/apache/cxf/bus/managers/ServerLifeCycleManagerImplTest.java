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
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerLifeCycleListener;
import org.apache.cxf.endpoint.ServerLifeCycleManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ServerLifeCycleManagerImplTest {

    private Bus bus;

    private ServerLifeCycleManagerImpl serverLifeCycleManager;

    @Before
    public void setup() {
        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
        bus = BusFactory.newInstance().createBus();

        serverLifeCycleManager = new ServerLifeCycleManagerImpl(bus);
    }

    @Test
    public void testLifeCycle() {

        Server server = Mockito.mock(Server.class);
        ServerLifeCycleListener listener = Mockito.mock(ServerLifeCycleListener.class);

        serverLifeCycleManager.registerListener(listener);
        serverLifeCycleManager.startServer(server);
        verify(listener, times(1)).startServer(server);
        serverLifeCycleManager.stopServer(server);
        verify(listener, times(1)).stopServer(server);
        serverLifeCycleManager.unRegisterListener(listener);
        verifyNoMoreInteractions(server);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testGetRegistrationType() {
        ServerLifeCycleManagerImpl impl = new ServerLifeCycleManagerImpl();
        assertEquals("ServerLifeCycleManagerImpl is of type ServerLifeCycleManager.class",
                ServerLifeCycleManager.class, impl.getRegistrationType());
    }

}
