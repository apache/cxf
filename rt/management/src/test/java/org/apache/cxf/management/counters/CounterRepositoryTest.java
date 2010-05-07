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
package org.apache.cxf.management.counters;

import javax.management.ObjectName;

import org.apache.cxf.Bus;
import org.apache.cxf.management.InstrumentationManager;
import org.easymock.classextension.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CounterRepositoryTest extends Assert {
    private Bus bus;
    //private InstrumentationManager im;
    private ObjectName serviceCounter;
    private ObjectName operationCounter;
    
    @Before
    public void setUp() throws Exception {
        
        serviceCounter = new ObjectName("tandoori:type=counter,service=help");
        operationCounter = new ObjectName("tandoori:type=counter,service=help,operation=me");
        bus = EasyMock.createMock(Bus.class);        
        bus.getExtension(InstrumentationManager.class);
        EasyMock.expectLastCall().andReturn(null).anyTimes();
        EasyMock.replay(bus);
    }
    
    @Test
    public void testIncreaseOneWayResponseCounter() throws Exception {        
        
        CounterRepository cr = new CounterRepository();
        cr.setBus(bus);
        //cr.createCounter(operationCounter, true);
        MessageHandlingTimeRecorder mhtr = EasyMock.createMock(MessageHandlingTimeRecorder.class);
        EasyMock.expect(mhtr.isOneWay()).andReturn(true).anyTimes();
        EasyMock.expect(mhtr.getEndTime()).andReturn((long)100000000).anyTimes();
        EasyMock.expect(mhtr.getHandlingTime()).andReturn((long)1000).anyTimes();
        EasyMock.expect(mhtr.getFaultMode()).andReturn(null).anyTimes();
        EasyMock.replay(mhtr);
        cr.increaseCounter(serviceCounter, mhtr);
        cr.increaseCounter(operationCounter, mhtr);
        ResponseTimeCounter opCounter = (ResponseTimeCounter) cr.getCounter(operationCounter);
        ResponseTimeCounter sCounter = (ResponseTimeCounter) cr.getCounter(serviceCounter);
        
        assertEquals("The operation counter isn't increased", opCounter.getNumInvocations(), 1);
        assertEquals("The Service counter isn't increased", sCounter.getNumInvocations(), 1);
        
        EasyMock.verify(bus);
        EasyMock.verify(mhtr);        
    }
    
    @Test
    public void testIncreaseOneWayNoResponseCounter() throws Exception {        
        
        CounterRepository cr = new CounterRepository();
        cr.setBus(bus);
        //cr.createCounter(operationCounter, true);
        MessageHandlingTimeRecorder mhtr = EasyMock.createMock(MessageHandlingTimeRecorder.class);
        EasyMock.expect(mhtr.isOneWay()).andReturn(true).anyTimes();
        EasyMock.expect(mhtr.getEndTime()).andReturn((long)0).anyTimes();
        EasyMock.expect(mhtr.getFaultMode()).andReturn(null).anyTimes();
        EasyMock.replay(mhtr);
        cr.increaseCounter(serviceCounter, mhtr);
        cr.increaseCounter(operationCounter, mhtr);
        ResponseTimeCounter opCounter = (ResponseTimeCounter) cr.getCounter(operationCounter);
        ResponseTimeCounter sCounter = (ResponseTimeCounter) cr.getCounter(serviceCounter);
        
        assertEquals("The operation counter isn't increased", opCounter.getNumInvocations(), 1);
        assertEquals("The Service counter isn't increased", sCounter.getNumInvocations(), 1);
        
        EasyMock.verify(bus);
        EasyMock.verify(mhtr);        
    }
    
    @Test
    public void testIncreaseResponseCounter() throws Exception {
        CounterRepository cr = new CounterRepository();
        cr.setBus(bus);
        
        MessageHandlingTimeRecorder mhtr1 = EasyMock.createMock(MessageHandlingTimeRecorder.class);
        EasyMock.expect(mhtr1.isOneWay()).andReturn(false).anyTimes();
        EasyMock.expect(mhtr1.getHandlingTime()).andReturn((long)1000).anyTimes();
        EasyMock.expect(mhtr1.getFaultMode()).andReturn(null).anyTimes();
        EasyMock.replay(mhtr1);
        cr.createCounter(operationCounter, mhtr1);
        cr.increaseCounter(serviceCounter, mhtr1);
        cr.increaseCounter(operationCounter, mhtr1);
        ResponseTimeCounter opCounter = (ResponseTimeCounter) cr.getCounter(operationCounter);
        ResponseTimeCounter sCounter = (ResponseTimeCounter) cr.getCounter(serviceCounter);
        
        assertEquals("The operation counter isn't increased", opCounter.getNumInvocations(), 1);
        assertEquals("The operation counter's AvgResponseTime is wrong ",
                     opCounter.getAvgResponseTime(), 1000);
        assertEquals("The operation counter's MaxResponseTime is wrong ", 
                     opCounter.getMaxResponseTime(), (long)1000);
        assertEquals("The operation counter's MinResponseTime is wrong ", 
                     opCounter.getMinResponseTime(), (long)1000);
        assertEquals("The Service counter isn't increased", sCounter.getNumInvocations(), 1);
        
        MessageHandlingTimeRecorder mhtr2 = EasyMock.createMock(MessageHandlingTimeRecorder.class);
        EasyMock.expect(mhtr2.isOneWay()).andReturn(false).anyTimes();
        EasyMock.expect(mhtr2.getHandlingTime()).andReturn((long)2000).anyTimes();
        EasyMock.expect(mhtr2.getFaultMode()).andReturn(null).anyTimes();
        EasyMock.replay(mhtr2);
        cr.increaseCounter(serviceCounter, mhtr2);
        cr.increaseCounter(operationCounter, mhtr2);
        assertEquals("The operation counter isn't increased", opCounter.getNumInvocations(), 2);
        assertEquals("The operation counter's AvgResponseTime is wrong ",
                     opCounter.getAvgResponseTime(), 1500);
        assertEquals("The operation counter's MaxResponseTime is wrong ",
                     opCounter.getMaxResponseTime(), (long)2000);
        assertEquals("The operation counter's MinResponseTime is wrong ",
                     opCounter.getMinResponseTime(), (long)1000);
        assertEquals("The Service counter isn't increased", sCounter.getNumInvocations(), 2);
        
        opCounter.reset();
        assertTrue(opCounter.getNumCheckedApplicationFaults().intValue() == 0);
        assertTrue(opCounter.getNumInvocations().intValue() == 0);
        assertTrue(opCounter.getNumLogicalRuntimeFaults().intValue() == 0);
        assertTrue(opCounter.getNumRuntimeFaults().intValue() == 0);
        assertTrue(opCounter.getNumUnCheckedApplicationFaults().intValue() == 0);
        assertTrue(opCounter.getTotalHandlingTime().intValue() == 0);
        assertTrue(opCounter.getMinResponseTime().longValue() == Integer.MAX_VALUE);
        assertTrue(opCounter.getMaxResponseTime().intValue() == 0);
        
        EasyMock.verify(bus);
        EasyMock.verify(mhtr1);
        EasyMock.verify(mhtr2);
    }
   
}
