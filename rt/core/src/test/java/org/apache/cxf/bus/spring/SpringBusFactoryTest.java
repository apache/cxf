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

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.bus.CXFBusImpl;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.phase.PhaseManagerImpl;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.apache.cxf.wsdl.WSDLManager;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class SpringBusFactoryTest extends Assert {

    @After
    public void tearDown() {
        BusFactory.setDefaultBus(null);
    }
    
    @Test
    public void testDefault() {
        Bus bus = new SpringBusFactory().createBus();
        assertNotNull(bus);
        BindingFactoryManager bfm = bus.getExtension(BindingFactoryManager.class);  
        assertNotNull("No binding factory manager", bfm);
        assertNotNull("No configurer", bus.getExtension(Configurer.class));
        assertNotNull("No resource manager", bus.getExtension(ResourceManager.class));
        assertNotNull("No destination factory manager", bus.getExtension(DestinationFactoryManager.class));
        assertNotNull("No conduit initiator manager", bus.getExtension(ConduitInitiatorManager.class));
        assertNotNull("No wsdl manager", bus.getExtension(WSDLManager.class));
        assertNotNull("No phase manager", bus.getExtension(PhaseManager.class));
        assertNotNull("No workqueue manager", bus.getExtension(WorkQueueManager.class));
        assertNotNull("No lifecycle manager", bus.getExtension(BusLifeCycleManager.class));
        assertNotNull("No service registry", bus.getExtension(ServerRegistry.class));
        
        try {
            bfm.getBindingFactory("http://cxf.apache.org/unknown");
        } catch (BusException ex) {
            // expected
        }
        
        assertEquals("Unexpected interceptors", 0, bus.getInInterceptors().size());
        assertEquals("Unexpected interceptors", 0, bus.getInFaultInterceptors().size());
        assertEquals("Unexpected interceptors", 0, bus.getOutInterceptors().size());
        assertEquals("Unexpected interceptors", 0, bus.getOutFaultInterceptors().size());

    }
    
    @Test
    public void testCustomFileName() {
        String cfgFile = "org/apache/cxf/bus/spring/resources/bus-overwrite.xml";
        Bus bus = new SpringBusFactory().createBus(cfgFile, true);
        checkCustomerConfiguration(bus);
    }
    
    @Test
    public void testCustomFileURLFromSystemProperty() {
        URL cfgFileURL = this.getClass().getResource("resources/bus-overwrite.xml");        
        System.setProperty(Configurer.USER_CFG_FILE_PROPERTY_URL, cfgFileURL.toString());
        Bus bus = new SpringBusFactory().createBus((String)null, true);
        checkCustomerConfiguration(bus);
        System.clearProperty(Configurer.USER_CFG_FILE_PROPERTY_URL);
    }
    
    @Test
    public void testCustomFileURL() {
        URL cfgFileURL = this.getClass().getResource("resources/bus-overwrite.xml");
        Bus bus = new SpringBusFactory().createBus(cfgFileURL, true);
        checkCustomerConfiguration(bus);
    }
    
    private void checkCustomerConfiguration(Bus bus) {
        assertNotNull(bus);
        List<Interceptor> interceptors = bus.getInInterceptors();
        assertEquals("Unexpected number of interceptors", 2, interceptors.size());
        assertEquals("Unexpected interceptor", "in-a", interceptors.get(0).toString());
        assertEquals("Unexpected interceptor", "in-b", interceptors.get(1).toString());
        interceptors = bus.getInFaultInterceptors();
        assertEquals("Unexpected number of interceptors", 1, interceptors.size());
        assertEquals("Unexpected interceptor", "in-fault", interceptors.get(0).toString());
        interceptors = bus.getOutFaultInterceptors();
        assertEquals("Unexpected number of interceptors", 1, interceptors.size());
        assertEquals("Unexpected interceptor", "out-fault", interceptors.get(0).toString());
        interceptors = bus.getOutInterceptors();
        assertEquals("Unexpected number of interceptors", 1, interceptors.size());
        assertEquals("Unexpected interceptor", "out", interceptors.get(0).toString());
    }
    
    @Test
    public void testForLifeCycle() {
        BusLifeCycleListener bl = EasyMock.createMock(BusLifeCycleListener.class);
        Bus bus = new SpringBusFactory().createBus();
        BusLifeCycleManager lifeCycleManager = bus.getExtension(BusLifeCycleManager.class);
        lifeCycleManager.registerLifeCycleListener(bl);
        
        bl.preShutdown();
        EasyMock.expectLastCall();
        bl.postShutdown();
        EasyMock.expectLastCall();
        EasyMock.replay(bl);
        bus.shutdown(true);
        EasyMock.verify(bl);
        
    }

    @Test
    public void testPhases() {
        Bus bus = new SpringBusFactory().createBus();
        PhaseManager cxfPM = bus.getExtension(PhaseManager.class);
        PhaseManager defaultPM = new PhaseManagerImpl();
        SortedSet<Phase> cxfPhases = cxfPM.getInPhases();
        SortedSet<Phase> defaultPhases = defaultPM.getInPhases();
        assertEquals(defaultPhases.size(), cxfPhases.size());
        assertTrue(cxfPhases.equals(defaultPhases));
        cxfPhases = cxfPM.getOutPhases();
        defaultPhases = defaultPM.getOutPhases();
        assertEquals(defaultPhases.size(), cxfPhases.size());
        assertTrue(cxfPhases.equals(defaultPhases));
    }

    @Test
    public void testJsr250() {
        Bus bus = new SpringBusFactory().createBus("org/apache/cxf/bus/spring/testjsr250.xml");
        TestExtension te = bus.getExtension(TestExtension.class);
        assertTrue("@PostConstruct annotated method has not been called.", te.postConstructMethodCalled);
        assertTrue("@PreDestroy annoated method has been called already.", !te.preDestroyMethodCalled);
        bus.shutdown(true);
        assertTrue("@PreDestroy annoated method has not been called.", te.preDestroyMethodCalled);
        
    }

    @Test
    public void testInitialisation() {
        Bus bus = new SpringBusFactory().createBus("org/apache/cxf/bus/spring/init.xml");
        assertNotNull(bus.getExtension(TestListener.class));           
        assertSame(bus, bus.getExtension(BusApplicationContext.class).getBean("cxf"));
    }

    
    static class TestInterceptor implements Interceptor {

        private String name;
        
        public TestInterceptor() {            
        }
        
        public void setName(String n) {
            name = n;
        }
               
        @Override
        public String toString() {
            return name;
        }
        
        public void handleFault(Message message) {  
        }

        public void handleMessage(Message message) throws Fault {   
        }
        
        public void postHandleMessage(Message message) throws Fault {            
        }
        
    }

    static class TestExtension {

        boolean postConstructMethodCalled;
        boolean preDestroyMethodCalled;

        public TestExtension(Bus bus) {
            bus.setExtension(this, TestExtension.class);
        }
 
        @PostConstruct
        void postConstructMethod() {
            postConstructMethodCalled = true;
        }

        @PreDestroy
        void preDestroyMethod() {
            preDestroyMethodCalled = true;
        }
    }
     
    static class TestFeature extends AbstractFeature {
        boolean initialised;
        public TestFeature() {
            //nothing
        }

        @Override
        public void initialize(Bus bus) {
            initialised = true;
        }   
    }
    
    static class TestListener implements BusLifeCycleListener {

        Bus bus;
 
        @Resource
        public void setBus(Bus b) {        
            bus = b;        
        }
        
        @PostConstruct
        public void register() {
            bus.getExtension(BusLifeCycleManager.class).registerLifeCycleListener(this);            
        }
        
        public void initComplete() {
            assertNull(bus.getExtension(TestFeature.class));
            Collection<AbstractFeature> features = ((CXFBusImpl)bus).getFeatures();
            assertEquals(1, features.size());
            TestFeature tf = (TestFeature)features.iterator().next();
            assertTrue(tf.initialised);
            bus.setExtension(this, TestListener.class);          
        }

        public void postShutdown() {
        }

        public void preShutdown() {
        }
        
    }    
}
