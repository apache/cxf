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

import javax.management.JMException;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.workqueue.AutomaticWorkQueueImpl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WorkQueueImplMBeanWrapperTest {

    private Bus bus;
    private String fullInstanceId;
    private WorkQueueImplMBeanWrapper nonShared;
    private WorkQueueImplMBeanWrapper shared;
    private AutomaticWorkQueueImpl nonSharedworkQueue;
    private AutomaticWorkQueueImpl sharedworkQueue;
    private WorkQueueManagerImpl workQueueManager;

    @Before
    public void setup() {
        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
        bus = BusFactory.newInstance().createBus();
        fullInstanceId = bus.getId();
        nonSharedworkQueue = Mockito.mock(AutomaticWorkQueueImpl.class);
        when(nonSharedworkQueue.isShared()).thenReturn(false);
        sharedworkQueue = Mockito.mock(AutomaticWorkQueueImpl.class);
        when(sharedworkQueue.isShared()).thenReturn(true);

        workQueueManager = Mockito.mock(WorkQueueManagerImpl.class);
        when(workQueueManager.getBus()).thenReturn(bus);

        nonShared = new WorkQueueImplMBeanWrapper(nonSharedworkQueue, workQueueManager);
        shared = new WorkQueueImplMBeanWrapper(sharedworkQueue, workQueueManager);
    }

    @Test
    public void testMockAutomaticWorkQueueImplPassThrough() {
        nonShared.getWorkQueueMaxSize();
        verify(nonSharedworkQueue, times(1)).getMaxSize();
        nonShared.getWorkQueueSize();
        verify(nonSharedworkQueue, times(1)).getSize();
        nonShared.getLargestPoolSize();
        verify(nonSharedworkQueue, times(1)).getLargestPoolSize();
        nonShared.getPoolSize();
        verify(nonSharedworkQueue, times(1)).getPoolSize();
        nonShared.getActiveCount();
        verify(nonSharedworkQueue, times(1)).getActiveCount();
        nonShared.isEmpty();
        verify(nonSharedworkQueue, times(1)).isEmpty();
        nonShared.isFull();
        verify(nonSharedworkQueue, times(1)).isFull();
        nonShared.getHighWaterMark();
        verify(nonSharedworkQueue, times(1)).getHighWaterMark();
        nonShared.setHighWaterMark(1);
        verify(nonSharedworkQueue, times(1)).setHighWaterMark(eq(1));
        nonShared.getLowWaterMark();
        verify(nonSharedworkQueue, times(1)).getLowWaterMark();
        nonShared.setLowWaterMark(1);
        verify(nonSharedworkQueue, times(1)).setLowWaterMark(eq(1));
    }

    @Test
    public void testGetObjectName() throws JMException {
        StringBuilder nonSharedObjectName = new StringBuilder(100)
                .append("org.apache.cxf:bus.id=")
                .append(fullInstanceId)
                .append(",WorkQueueManager=Bus.WorkQueueManager,type=WorkQueues,name=null,instance.id=")
                .append(nonSharedworkQueue.hashCode());
        assertEquals(nonSharedObjectName.toString(), nonShared.getObjectName().toString());

        StringBuilder sharedObjectName = new StringBuilder(100)
                .append("org.apache.cxf:bus.id=Shared,type=WorkQueues,name=null,instance.id=")
                .append(sharedworkQueue.hashCode());
        assertEquals(sharedObjectName.toString(), shared.getObjectName().toString());
    }
}
