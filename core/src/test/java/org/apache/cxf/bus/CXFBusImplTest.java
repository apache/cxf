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

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CXFBusImplTest {

    @Test
    public void testThreadBus() throws BusException {
        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
        Bus bus = BusFactory.newInstance().createBus();
        Bus b2 = BusFactory.getThreadDefaultBus(false);
        assertSame(bus, b2);
        bus.shutdown(true);
    }
    @Test
    public void testConstructionWithoutExtensions() throws BusException {

        Bus bus = new ExtensionManagerBus();
        assertNotNull(bus.getExtension(BindingFactoryManager.class));
        assertNotNull(bus.getExtension(ConduitInitiatorManager.class));
        assertNotNull(bus.getExtension(DestinationFactoryManager.class));
        assertNotNull(bus.getExtension(PhaseManager.class));
        bus.shutdown(true);
    }

    @Test
    public void testConstructionWithExtensions() throws BusException {

        BindingFactoryManager bindingFactoryManager;
        InstrumentationManager instrumentationManager;
        PhaseManager phaseManager;

        Map<Class<?>, Object> extensions = new HashMap<>();
        bindingFactoryManager = mock(BindingFactoryManager.class);
        instrumentationManager = mock(InstrumentationManager.class);
        phaseManager = mock(PhaseManager.class);

        extensions.put(BindingFactoryManager.class, bindingFactoryManager);
        extensions.put(InstrumentationManager.class, instrumentationManager);
        extensions.put(PhaseManager.class, phaseManager);

        Bus bus = new ExtensionManagerBus(extensions);

        assertSame(bindingFactoryManager, bus.getExtension(BindingFactoryManager.class));
        assertSame(instrumentationManager, bus.getExtension(InstrumentationManager.class));
        assertSame(phaseManager, bus.getExtension(PhaseManager.class));

    }

    @Test
    public void testExtensions() {
        Bus bus = new ExtensionManagerBus();
        String extension = "CXF";
        bus.setExtension(extension, String.class);
        assertSame(extension, bus.getExtension(String.class));
        bus.shutdown(true);
    }

    @Test
    public void testBusID() {
        Bus bus = new ExtensionManagerBus();
        String id = bus.getId();
        assertEquals("The bus id should be cxf", id, Bus.DEFAULT_BUS_ID + Math.abs(bus.hashCode()));
        bus.setId("test");
        assertEquals("The bus id should be changed", "test", bus.getId());
        bus.shutdown(true);
    }

    @Test
    public void testShutdownWithBusLifecycle() {
        final Bus bus = new ExtensionManagerBus();
        BusLifeCycleManager lifeCycleManager = bus.getExtension(BusLifeCycleManager.class);
        BusLifeCycleListener listener = mock(BusLifeCycleListener.class);
        
        lifeCycleManager.registerLifeCycleListener(listener);
        bus.shutdown(true);
        
        verify(listener).preShutdown();
        verify(listener).postShutdown();

        bus.shutdown(true);
    }

}