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
import org.apache.cxf.headers.HeaderProcessor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.when;

public class HeaderManagerImplTest {

    private Bus bus;

    private HeaderManagerImpl headerManager;

    @Before
    public void setup() {
        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
        bus = BusFactory.newInstance().createBus();

        headerManager = new HeaderManagerImpl(bus);
    }

    @Test
    public void testBuses() {

        Bus initialBus = headerManager.getBus();
        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
        Bus differentBus = BusFactory.newInstance().createBus();
        headerManager.setBus(differentBus);
        assertNotEquals("Buses must be different.",
                initialBus, differentBus);
        assertEquals("HeaderManger has set bus",
                differentBus, headerManager.getBus());
    }

    @Test
    public void testProcessors() {
        headerManager = new HeaderManagerImpl();
        HeaderProcessor processor = Mockito.mock(HeaderProcessor.class);
        when(processor.getNamespace()).thenReturn("ns1");

        headerManager.registerHeaderProcessor(processor);

        HeaderProcessor actual = headerManager.getHeaderProcessor("ns1");
        assertEquals("Should get registered processor.", processor, actual);
    }
}
