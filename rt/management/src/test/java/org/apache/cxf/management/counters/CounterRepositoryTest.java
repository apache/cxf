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

import java.util.ArrayList;
import java.util.List;

import javax.management.ObjectName;

import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.message.Message;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CounterRepositoryTest {
    private Bus bus;
    private CounterRepository cr;
    private List<Interceptor<? extends Message>> inlist = new ArrayList<>();
    private List<Interceptor<? extends Message>> outlist = new ArrayList<>();
    private List<Interceptor<? extends Message>> faultlist = new ArrayList<>();
    //private InstrumentationManager im;
    private ObjectName serviceCounter;
    private ObjectName operationCounter;

    @Before
    public void setUp() throws Exception {
        inlist.clear();
        outlist.clear();

        serviceCounter = new ObjectName("tandoori:type=counter,service=help");
        operationCounter = new ObjectName("tandoori:type=counter,service=help,operation=me");
        bus = EasyMock.createMock(Bus.class);
        EasyMock.expect(bus.getInInterceptors()).andReturn(inlist).anyTimes();
        EasyMock.expect(bus.getOutInterceptors()).andReturn(outlist).anyTimes();
        EasyMock.expect(bus.getOutFaultInterceptors()).andReturn(faultlist).anyTimes();
        bus.getExtension(InstrumentationManager.class);
        EasyMock.expectLastCall().andReturn(null).anyTimes();

        cr = new CounterRepository();
        bus.setExtension(cr, CounterRepository.class);
        EasyMock.expectLastCall().once();

        EasyMock.replay(bus);
        cr.setBus(bus);
    }

    @Test
    public void testIncreaseOneWayResponseCounter() throws Exception {

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

        verifyBus();
        EasyMock.verify(mhtr);
    }

    @Test
    public void testIncreaseOneWayNoResponseCounter() throws Exception {

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

        verifyBus();
        EasyMock.verify(mhtr);
    }

    @Test
    public void testIncreaseResponseCounter() throws Exception {

        MessageHandlingTimeRecorder mhtr1 = EasyMock.createMock(MessageHandlingTimeRecorder.class);
        EasyMock.expect(mhtr1.isOneWay()).andReturn(false).anyTimes();
        EasyMock.expect(mhtr1.getHandlingTime()).andReturn((long)1000).anyTimes();
        EasyMock.expect(mhtr1.getFaultMode()).andReturn(null).anyTimes();
        EasyMock.replay(mhtr1);
        cr.createCounter(operationCounter);
        cr.increaseCounter(serviceCounter, mhtr1);
        cr.increaseCounter(operationCounter, mhtr1);
        ResponseTimeCounter opCounter = (ResponseTimeCounter) cr.getCounter(operationCounter);
        ResponseTimeCounter sCounter = (ResponseTimeCounter) cr.getCounter(serviceCounter);

        assertEquals("The operation counter isn't increased", opCounter.getNumInvocations(), 1);
        assertEquals("The operation counter's AvgResponseTime is wrong ",
                     opCounter.getAvgResponseTime(), (long)1000);
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
                     opCounter.getAvgResponseTime(), (long)1500);
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
        assertTrue(opCounter.getMinResponseTime().intValue() == 0);
        assertTrue(opCounter.getMaxResponseTime().intValue() == 0);
        assertTrue(opCounter.getAvgResponseTime().intValue() == 0);

        verifyBus();
        EasyMock.verify(mhtr1);
        EasyMock.verify(mhtr2);
    }


    private void verifyBus() {
        EasyMock.verify(bus);

        // the numbers should match the implementation of CounterRepository
        assertEquals(2, inlist.size());
        assertEquals(1, outlist.size());
    }

}