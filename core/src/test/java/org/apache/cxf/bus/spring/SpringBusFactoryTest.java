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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.bus.managers.PhaseManagerImpl;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.springframework.beans.factory.xml.NamespaceHandlerResolver;
import org.springframework.context.ApplicationContext;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class SpringBusFactoryTest {

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
    public void testNamespaceHandlerResolver() {
        NamespaceHandlerResolver r = Mockito.mock(NamespaceHandlerResolver.class);
        SpringBusFactory factory = new SpringBusFactory(r);
        Bus bus = factory.createBus();
        bus.shutdown(true);
        verifyNoInteractions(r);

        NamespaceHandlerResolver differentResolver = Mockito.mock(NamespaceHandlerResolver.class);
        factory.setNamespaceHandlerResolver(differentResolver);
        verifyNoInteractions(differentResolver);
    }

    @Test
    public void testApplicationContext() {
        ApplicationContext context = Mockito.mock(ApplicationContext.class);
        SpringBusFactory factory = new SpringBusFactory(context);
        assertEquals(context, factory.getApplicationContext());
    }

    @Test
    public void testCustomFileName() {
        String cfgFile = "org/apache/cxf/bus/spring/resources/bus-overwrite.xml";
        Bus bus = new SpringBusFactory().createBus(cfgFile, true);
        checkCustomerConfiguration(bus);
    }

    @Test
    public void testCustomFileNames() {
        String cfgFile = "org/apache/cxf/bus/spring/resources/bus-overwrite.xml";
        String[] cfgFiles = new String[1];
        cfgFiles[0] = cfgFile;
        Bus bus = new SpringBusFactory().createBus(cfgFiles);
        checkCustomerConfiguration(bus);
    }

    @Test
    public void testCustomerBusShutdown() {
        String cfgFile = "org/apache/cxf/bus/spring/customerBus.xml";
        Bus bus = new SpringBusFactory().createBus(cfgFile, true);
        // We have three bus here, which should be closed rightly
        bus.shutdown(true);
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
        List<Interceptor<? extends Message>> interceptors = bus.getInInterceptors();
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
        BusLifeCycleListener bl = mock(BusLifeCycleListener.class);
        Bus bus = new SpringBusFactory().createBus();
        BusLifeCycleManager lifeCycleManager = bus.getExtension(BusLifeCycleManager.class);
        lifeCycleManager.registerLifeCycleListener(bl);
        bus.shutdown(true);

        verify(bl).preShutdown();
        verify(bl).postShutdown();
    }

    @Test
    public void testPhases() {
        Bus bus = new SpringBusFactory().createBus();
        PhaseManager cxfPM = bus.getExtension(PhaseManager.class);
        PhaseManager defaultPM = new PhaseManagerImpl();
        SortedSet<Phase> cxfPhases = cxfPM.getInPhases();
        SortedSet<Phase> defaultPhases = defaultPM.getInPhases();
        assertEquals(defaultPhases.size(), cxfPhases.size());
        assertEquals(cxfPhases, defaultPhases);
        cxfPhases = cxfPM.getOutPhases();
        defaultPhases = defaultPM.getOutPhases();
        assertEquals(defaultPhases.size(), cxfPhases.size());
        assertEquals(cxfPhases, defaultPhases);
    }

    @Test
    public void testJsr250() {
        Bus bus = new SpringBusFactory().createBus("org/apache/cxf/bus/spring/testjsr250.xml");
        TestExtension te = bus.getExtension(TestExtension.class);
        assertTrue("@PostConstruct annotated method has not been called.", te.postConstructMethodCalled);
        assertFalse("@PreDestroy annoated method has been called already.", te.preDestroyMethodCalled);
        bus.shutdown(true);
        assertTrue("@PreDestroy annotated method has not been called.", te.preDestroyMethodCalled);

    }

    @Test
    public void testInitialisation() {
        Bus bus = new SpringBusFactory().createBus("org/apache/cxf/bus/spring/init.xml");
        assertNotNull(bus.getExtension(TestListener.class));
        assertSame(bus, bus.getExtension(BusApplicationContext.class).getBean("cxf"));
    }


    static class TestInterceptor implements Interceptor<Message> {

        private String name;

        TestInterceptor() {
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

        public void handleMessage(Message message) {
        }

        public void postHandleMessage(Message message) {
        }

    }

    static class TestExtension {

        boolean postConstructMethodCalled;
        boolean preDestroyMethodCalled;

        TestExtension(Bus bus) {
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
        TestFeature() {
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
            Collection<Feature> features = bus.getFeatures();
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
