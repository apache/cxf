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

package org.apache.cxf.bus;

import javax.management.ObjectName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ManagedBusTest {

    private Bus bus;
    private ManagedBus managedBus;

    @Before
    public void setup() {
        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
        bus = BusFactory.newInstance().createBus();
        managedBus = new ManagedBus(bus);
    }

    @Test
    public void testShutdown() {

        managedBus.shutdown(false);
        assertEquals("", Bus.BusState.SHUTDOWN, bus.getState());
    }

    @Test
    public void testGetObjectName() throws Exception {
        String fullInstanceId = bus.getId();
        String instanceId = Integer.toString(bus.hashCode());
        ObjectName objectName = managedBus.getObjectName();

        StringBuilder sb = new StringBuilder(100)
                .append("org.apache.cxf:bus.id=")
                .append(fullInstanceId)
                .append(",type=Bus,instance.id=")
                .append(instanceId);

        assertEquals("Should return this bus instance.",
                sb.toString(), objectName.toString());
    }
}
