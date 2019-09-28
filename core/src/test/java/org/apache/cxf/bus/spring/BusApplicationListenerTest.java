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
package org.apache.cxf.bus.spring;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.managers.CXFBusLifeCycleManager;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.springframework.context.support.AbstractRefreshableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.easymock.EasyMock;
import org.junit.Test;



public class BusApplicationListenerTest {

    @Test
    public void testParentApplicationEvent() {
        AbstractRefreshableApplicationContext parent = new ClassPathXmlApplicationContext();
        parent.refresh();
        SpringBusFactory factory = new SpringBusFactory(parent);
        Bus bus = factory.createBus();
        CXFBusLifeCycleManager manager = bus.getExtension(CXFBusLifeCycleManager.class);
        BusLifeCycleListener listener = EasyMock.createMock(BusLifeCycleListener.class);
        manager.registerLifeCycleListener(listener);
        EasyMock.reset(listener);
        listener.preShutdown();
        EasyMock.expectLastCall().times(1);
        listener.postShutdown();
        EasyMock.expectLastCall().times(1);
        EasyMock.replay(listener);
        parent.close();
        EasyMock.verify(listener);
    }

}
