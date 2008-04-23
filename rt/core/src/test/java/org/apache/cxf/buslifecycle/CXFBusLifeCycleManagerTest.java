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

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.junit.Test;

public class CXFBusLifeCycleManagerTest extends Assert {

    @Test
    public void testListenerNotRegistered() {

        BusLifeCycleListener listener1 = EasyMock.createMock(BusLifeCycleListener.class);
        CXFBusLifeCycleManager mgr = new CXFBusLifeCycleManager();

        EasyMock.reset(listener1);
        EasyMock.replay(listener1);
        mgr.initComplete();
        EasyMock.verify(listener1);

        EasyMock.reset(listener1);
        EasyMock.replay(listener1);
        mgr.preShutdown();
        EasyMock.verify(listener1);

        EasyMock.reset(listener1);
        EasyMock.replay(listener1);
        mgr.postShutdown();
        EasyMock.verify(listener1);
    }
    
    @Test
    public void testSingleListenerRegistration() {

        BusLifeCycleListener listener1 = EasyMock.createMock(BusLifeCycleListener.class);
        CXFBusLifeCycleManager mgr = new CXFBusLifeCycleManager();
        
        mgr.registerLifeCycleListener(listener1);

        EasyMock.reset(listener1);
        listener1.initComplete();
        EasyMock.replay(listener1);
        mgr.initComplete();
        EasyMock.verify(listener1);

        EasyMock.reset(listener1);
        listener1.preShutdown();
        EasyMock.replay(listener1);
        mgr.preShutdown();
        EasyMock.verify(listener1);

        EasyMock.reset(listener1);
        listener1.postShutdown();
        EasyMock.replay(listener1);
        mgr.postShutdown();
        EasyMock.verify(listener1);        
    }
    
    @Test
    public void testDuplicateRegistration() {
        
        BusLifeCycleListener listener1 = EasyMock.createMock(BusLifeCycleListener.class);
        CXFBusLifeCycleManager mgr = new CXFBusLifeCycleManager();

        mgr.registerLifeCycleListener(listener1);
        mgr.registerLifeCycleListener(listener1);

        EasyMock.reset(listener1);
        listener1.initComplete();
        EasyMock.expectLastCall().times(2);
        EasyMock.replay(listener1);
        mgr.initComplete();
        EasyMock.verify(listener1);

        EasyMock.reset(listener1);
        listener1.preShutdown();
        EasyMock.expectLastCall().times(2);
        EasyMock.replay(listener1);
        mgr.preShutdown();
        EasyMock.verify(listener1);

        EasyMock.reset(listener1);
        listener1.postShutdown();
        EasyMock.expectLastCall().times(2);
        EasyMock.replay(listener1);
        mgr.postShutdown();
        EasyMock.verify(listener1);
    }
    
    @Test
    public void testMultipleListeners() {
       
        IMocksControl ctrl = EasyMock.createStrictControl();
        
        BusLifeCycleListener listener1 = ctrl.createMock(BusLifeCycleListener.class);
        BusLifeCycleListener listener2 = ctrl.createMock(BusLifeCycleListener.class);
        CXFBusLifeCycleManager mgr = new CXFBusLifeCycleManager();

        mgr.registerLifeCycleListener(listener1);
        mgr.registerLifeCycleListener(listener2);
        
        ctrl.reset();
        listener1.initComplete();
        listener2.initComplete();
        ctrl.replay();
        mgr.initComplete();
        ctrl.verify();
        
        ctrl.reset();
        listener1.preShutdown();
        listener2.preShutdown();
        ctrl.replay();
        mgr.preShutdown();
        ctrl.verify();
        
        ctrl.reset();
        listener1.postShutdown();
        listener2.postShutdown();
        ctrl.replay();
        mgr.postShutdown();
        ctrl.verify();
    }
    
    @Test
    public void testDeregistration() {
        
        IMocksControl ctrl = EasyMock.createStrictControl();
        
        BusLifeCycleListener listener1 = ctrl.createMock(BusLifeCycleListener.class);
        BusLifeCycleListener listener2 = ctrl.createMock(BusLifeCycleListener.class);
        CXFBusLifeCycleManager mgr = new CXFBusLifeCycleManager();

        mgr.registerLifeCycleListener(listener2);
        mgr.registerLifeCycleListener(listener1);
        mgr.unregisterLifeCycleListener(listener2);
        
        ctrl.reset();
        listener1.initComplete();
        ctrl.replay();
        mgr.initComplete();
        ctrl.verify();
        
        ctrl.reset();
        listener1.preShutdown();
        ctrl.replay();
        mgr.preShutdown();
        ctrl.verify();
        
        ctrl.reset();
        listener1.postShutdown();
        ctrl.replay();
        mgr.postShutdown();
        ctrl.verify();
    }
}
