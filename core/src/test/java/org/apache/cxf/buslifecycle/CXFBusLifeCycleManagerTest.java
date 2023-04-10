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

package org.apache.cxf.buslifecycle;

import org.apache.cxf.bus.managers.CXFBusLifeCycleManager;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class CXFBusLifeCycleManagerTest {

    @Test
    public void testListenerNotRegistered() {

        BusLifeCycleListener listener1 = mock(BusLifeCycleListener.class);
        CXFBusLifeCycleManager mgr = new CXFBusLifeCycleManager();

        mgr.initComplete();
        verifyNoInteractions(listener1);

        mgr.preShutdown();
        verifyNoInteractions(listener1);

        mgr.postShutdown();
        verifyNoInteractions(listener1);
    }

    @Test
    public void testSingleListenerRegistration() {

        BusLifeCycleListener listener1 = mock(BusLifeCycleListener.class);
        CXFBusLifeCycleManager mgr = new CXFBusLifeCycleManager();

        mgr.registerLifeCycleListener(listener1);

        mgr.initComplete();
        verify(listener1).initComplete();

        mgr.preShutdown();
        verify(listener1).preShutdown();

        mgr.postShutdown();
        verify(listener1).postShutdown();
    }

    @Test
    public void testMultipleListeners() {
        BusLifeCycleListener listener1 = mock(BusLifeCycleListener.class);
        BusLifeCycleListener listener2 = mock(BusLifeCycleListener.class);
        CXFBusLifeCycleManager mgr = new CXFBusLifeCycleManager();

        mgr.registerLifeCycleListener(listener1);
        mgr.registerLifeCycleListener(listener2);

        mgr.initComplete();
        verify(listener1).initComplete();
        verify(listener2).initComplete();

        mgr.preShutdown();
        verify(listener1).preShutdown();
        verify(listener2).preShutdown();

        mgr.postShutdown();
        verify(listener1).postShutdown();
        verify(listener2).postShutdown();
    }

    @Test
    public void testDeregistration() {
        BusLifeCycleListener listener1 = mock(BusLifeCycleListener.class);
        BusLifeCycleListener listener2 = mock(BusLifeCycleListener.class);
        CXFBusLifeCycleManager mgr = new CXFBusLifeCycleManager();

        mgr.registerLifeCycleListener(listener2);
        mgr.registerLifeCycleListener(listener1);
        mgr.unregisterLifeCycleListener(listener2);

        mgr.initComplete();
        verify(listener1).initComplete();
        verifyNoInteractions(listener2);

        mgr.preShutdown();
        verify(listener1).preShutdown();
        verifyNoInteractions(listener2);

        mgr.postShutdown();
        verify(listener1).postShutdown();
        verifyNoInteractions(listener2);
    }
}
