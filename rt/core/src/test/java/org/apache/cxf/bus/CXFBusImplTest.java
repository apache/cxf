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
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.event.EventProcessor;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.wsdl.WSDLManager;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Test;

public class CXFBusImplTest extends Assert {

    @Test
    public void testConstructionWithoutExtensions() throws BusException {
        
        CXFBusImpl bus = new ExtensionManagerBus();
        assertNotNull(bus.getExtension(BindingFactoryManager.class));
        assertNotNull(bus.getExtension(ConduitInitiatorManager.class));   
        assertNotNull(bus.getExtension(DestinationFactoryManager.class));
        assertNotNull(bus.getExtension(WSDLManager.class));
        assertNotNull(bus.getExtension(PhaseManager.class));
    }
    
    @Test
    public void testConstructionWithExtensions() throws BusException {
        
        IMocksControl control;
        BindingFactoryManager bindingFactoryManager;
        WSDLManager wsdlManager;
        EventProcessor eventProcessor;
        InstrumentationManager instrumentationManager;
        PhaseManager phaseManager;
        
        control = EasyMock.createNiceControl();
        
        Map<Class, Object> extensions = new HashMap<Class, Object>();
        bindingFactoryManager = control.createMock(BindingFactoryManager.class);
        wsdlManager = control.createMock(WSDLManager.class);
        eventProcessor = control.createMock(EventProcessor.class);
        instrumentationManager = control.createMock(InstrumentationManager.class);
        phaseManager = control.createMock(PhaseManager.class);
        
        extensions.put(BindingFactoryManager.class, bindingFactoryManager);
        extensions.put(WSDLManager.class, wsdlManager);
        extensions.put(EventProcessor.class, eventProcessor);
        extensions.put(InstrumentationManager.class, instrumentationManager);
        extensions.put(PhaseManager.class, phaseManager);
        
        CXFBusImpl bus = new CXFBusImpl(extensions);
        
        assertSame(bindingFactoryManager, bus.getExtension(BindingFactoryManager.class));
        assertSame(wsdlManager, bus.getExtension(WSDLManager.class));
        assertSame(eventProcessor, bus.getExtension(EventProcessor.class));
        assertSame(instrumentationManager, bus.getExtension(InstrumentationManager.class));
        assertSame(phaseManager, bus.getExtension(PhaseManager.class));
  
    }

    @Test
    public void testExtensions() {
        CXFBusImpl bus = new CXFBusImpl();
        String extension = "CXF";
        bus.setExtension(extension, String.class);
        assertSame(extension, bus.getExtension(String.class));
    }
    
    @Test
    public void testBusID() {
        CXFBusImpl bus = new CXFBusImpl();
        String id = bus.getId();
        assertEquals("The bus id should be cxf", id, Bus.DEFAULT_BUS_ID + bus.hashCode());
        bus.setId("test");
        assertEquals("The bus id should be changed", bus.getId(), "test");
    }
    
    @Test
    public void testRun() {
        final CXFBusImpl bus = new CXFBusImpl();
        Thread t = new Thread() {
            public void run() {
                bus.run();
            }
        };
        t.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            // ignore;
        }
        try {
            t.join(400);
        } catch (InterruptedException ex) {
            // ignore
        }
        assertEquals(BusState.RUNNING, bus.getState());
    }
    
    @Test
    public void testShutdown() {
        final CXFBusImpl bus = new CXFBusImpl();
        Thread t = new Thread() {
            public void run() {
                bus.run();
            }
        };
        t.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            // ignore;
        }
        bus.shutdown(true);
        try {
            t.join();
        } catch (InterruptedException ex) {
            // ignore
        }
        assertEquals(BusState.SHUTDOWN, bus.getState());
        
    }
    
    @Test
    public void testShutdownWithBusLifecycle() {
        final CXFBusImpl bus = new ExtensionManagerBus();
        BusLifeCycleManager lifeCycleManager = bus.getExtension(BusLifeCycleManager.class);
        BusLifeCycleListener listener = EasyMock.createMock(BusLifeCycleListener.class);
        EasyMock.reset(listener);
        listener.preShutdown();
        EasyMock.expectLastCall();
        listener.postShutdown();
        EasyMock.expectLastCall();        
        EasyMock.replay(listener);        
        lifeCycleManager.registerLifeCycleListener(listener);
        bus.shutdown(true);
        EasyMock.verify(listener);
        
    }

}
