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
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientLifeCycleListener;
import org.apache.cxf.endpoint.ClientLifeCycleManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ClientLifeCycleManagerImplTest {

    private Bus bus;

    private ClientLifeCycleManagerImpl clientLifeCycleManager;

    @Before
    public void setup() {
        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
        bus = BusFactory.newInstance().createBus();

        clientLifeCycleManager = new ClientLifeCycleManagerImpl(bus);
    }

    @Test
    public void testListeners() {
        Client client = Mockito.mock(Client.class);
        ClientLifeCycleListener listener = Mockito.mock(ClientLifeCycleListener.class);
        clientLifeCycleManager.registerListener(listener);
        clientLifeCycleManager.clientCreated(client);
        verify(listener, times(1)).clientCreated(client);
        clientLifeCycleManager.clientDestroyed(client);
        verify(listener, times(1)).clientDestroyed(client);
        clientLifeCycleManager.unRegisterListener(listener);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testGetRegistrationType() {
        ClientLifeCycleManagerImpl impl = new ClientLifeCycleManagerImpl();
        assertEquals("ClientLifeCycleManagerImpl is of type ClientLifeCycleManager.class",
                ClientLifeCycleManager.class, impl.getRegistrationType());
    }

}
