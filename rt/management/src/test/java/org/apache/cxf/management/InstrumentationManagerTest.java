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
package org.apache.cxf.management;

import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.management.counters.CounterRepository;
import org.apache.cxf.management.jmx.InstrumentationManagerImpl;
import org.apache.cxf.workqueue.WorkQueueManagerImpl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class InstrumentationManagerTest extends Assert {
    InstrumentationManager im;
    Bus bus;
    
    @Before
    public void setUp() throws Exception {

    }
    
    @After
    public void tearDown() throws Exception {
        //test case had done the bus.shutdown
        if (bus != null) {
            bus.shutdown(true);
        }
    }
    
    @Test
    public void testInstrumentationNotEnabled() {
        SpringBusFactory factory = new SpringBusFactory();
        bus =  factory.createBus();
        im = bus.getExtension(InstrumentationManager.class);
        assertTrue("Instrumentation Manager should not be null", im != null);
        MBeanServer mbs = im.getMBeanServer();
        assertNull("MBeanServer should not be available.", mbs);
    }
    
    @Test
    public void testInstrumentationEnabledSetBeforeBusSet() {
        SpringBusFactory factory = new SpringBusFactory();
        bus =  factory.createBus("managed-spring3.xml", true);
        im = bus.getExtension(InstrumentationManager.class);
        assertTrue("Instrumentation Manager should not be null", im != null);
        MBeanServer mbs = im.getMBeanServer();
        assertNotNull("MBeanServer should be available.", mbs);
    }
    
    @Test
    // try to get WorkQueue information
    public void testWorkQueueInstrumentation() throws Exception {
        SpringBusFactory factory = new SpringBusFactory();
        bus =  factory.createBus("managed-spring.xml", true);
        im = bus.getExtension(InstrumentationManager.class);
        assertTrue("Instrumentation Manager should not be null", im != null);
        WorkQueueManagerImpl wqm = new WorkQueueManagerImpl();
        wqm.setBus(bus);
        wqm.getAutomaticWorkQueue();
        
        MBeanServer mbs = im.getMBeanServer();
        assertNotNull("MBeanServer should be available.", mbs);
        ObjectName name = new ObjectName(ManagementConstants.DEFAULT_DOMAIN_NAME 
                                         + ":type=WorkQueues,*");
        Set s = mbs.queryNames(name, null);
        assertEquals(2, s.size());
        Iterator it = s.iterator();
        while (it.hasNext()) {
            ObjectName n = (ObjectName)it.next();
            Long result = 
                (Long)mbs.invoke(n, "getWorkQueueMaxSize", new Object[0], new String[0]);
            assertEquals(result, Long.valueOf(256));
            Integer hwm = 
                (Integer)mbs.invoke(n, "getHighWaterMark", new Object[0], new String[0]);
            if (n.getCanonicalName().contains("test-wq")) {
                assertEquals(10, hwm.intValue());
            } else {
                assertEquals(15, hwm.intValue());
            }
        }

        bus.shutdown(true);
    }

    @Test
    public void testInstrumentTwoBuses() {
        ClassPathXmlApplicationContext context = null;
        Bus cxf1 = null;
        Bus cxf2 = null;
        try {
            context = new ClassPathXmlApplicationContext("managed-spring-twobuses.xml");

            cxf1 = (Bus)context.getBean("cxf1");
            InstrumentationManager im1 = cxf1.getExtension(InstrumentationManager.class);
            assertNotNull("Instrumentation Manager of cxf1 should not be null", im1);
            CounterRepository cr1 = cxf1.getExtension(CounterRepository.class);
            assertNotNull("CounterRepository of cxf1 should not be null", cr1);
            assertEquals("CounterRepository of cxf1 has the wrong bus", cxf1, cr1.getBus());
            
            cxf2 = (Bus)context.getBean("cxf2");
            InstrumentationManager im2 = cxf2.getExtension(InstrumentationManager.class);
            assertNotNull("Instrumentation Manager of cxf2 should not be null", im2);
            CounterRepository cr2 = cxf2.getExtension(CounterRepository.class);
            assertNotNull("CounterRepository of cxf2 should not be null", cr2);
            assertEquals("CounterRepository of cxf2 has the wrong bus", cxf2, cr2.getBus());

        } finally {
            if (cxf1 != null) {
                cxf1.shutdown(true);
            }
            if (cxf2 != null) {
                cxf2.shutdown(true);
            }
            if (context != null) {
                context.close();
            }
        }
    }
    
    @Test
    public void testInstrumentBusWithBusProperties() {
        ClassPathXmlApplicationContext context = null;
        Bus cxf1 = null;
        Bus cxf2 = null;
        try {
            context = new ClassPathXmlApplicationContext("managed-spring-twobuses2.xml");

            cxf1 = (Bus)context.getBean("cxf1");
            InstrumentationManagerImpl im1 = 
                (InstrumentationManagerImpl)cxf1.getExtension(InstrumentationManager.class);
            assertNotNull("Instrumentation Manager of cxf1 should not be null", im1);
            
            assertTrue(im1.isEnabled());
            assertEquals("service:jmx:rmi:///jndi/rmi://localhost:9914/jmxrmi", im1.getJMXServiceURL());
            
            cxf2 = (Bus)context.getBean("cxf2");
            InstrumentationManagerImpl im2 = 
                (InstrumentationManagerImpl)cxf2.getExtension(InstrumentationManager.class);
            assertNotNull("Instrumentation Manager of cxf2 should not be null", im2);

            assertFalse(im2.isEnabled());
            assertEquals("service:jmx:rmi:///jndi/rmi://localhost:9913/jmxrmi", im2.getJMXServiceURL());
            
        } finally {
            if (cxf1 != null) {
                cxf1.shutdown(true);
            }
            if (cxf2 != null) {
                cxf2.shutdown(true);
            }
            if (context != null) {
                context.close();
            }
        }
    }

}
