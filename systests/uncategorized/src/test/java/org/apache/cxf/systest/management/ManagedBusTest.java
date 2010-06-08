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

package org.apache.cxf.systest.management;

import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.management.jmx.InstrumentationManagerImpl;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.junit.Assert;
import org.junit.Test;

public class ManagedBusTest extends Assert {
    public static final String JMX_PORT1 = TestUtil.getPortNumber("Server.1");
    public static final String JMX_PORT2 = TestUtil.getPortNumber(ManagedBusTest.class, 3);

    @Test
    public void testManagedSpringBus() throws Exception {
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus();        
        InstrumentationManager im = bus.getExtension(InstrumentationManager.class);
        assertNotNull(im);
                
        InstrumentationManagerImpl imi = (InstrumentationManagerImpl)im;
        assertEquals("service:jmx:rmi:///jndi/rmi://localhost:9913/jmxrmi", 
                     imi.getJMXServiceURL());
        assertTrue(!imi.isEnabled());
        assertNull(imi.getMBeanServer());
        
        //Test that registering without an MBeanServer is a no-op
        im.register(imi, new ObjectName("org.apache.cxf:foo=bar"));                        
        
        bus.shutdown(true);
    }
        
    @Test
    public void testManagedBusWithTransientId() throws Exception {
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus("org/apache/cxf/systest/management/managed-spring.xml", true);
        doManagedBusTest(bus, bus.getId(), "cxf_managed_bus_test", Integer.parseInt(JMX_PORT1));
    }
    
    @Test
    public void testManagedBusWithPersistentId() throws Exception {
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus("org/apache/cxf/systest/management/persistent-id.xml", true);
        doManagedBusTest(bus, "cxf_managed_bus_test", bus.getId(), Integer.parseInt(JMX_PORT2));
    }
    
    private void doManagedBusTest(Bus bus, String expect, String reject, int port) throws Exception {
        InstrumentationManager im = bus.getExtension(InstrumentationManager.class);
        assertNotNull(im);
        InstrumentationManagerImpl imi = (InstrumentationManagerImpl)im;
        assertEquals("service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi",
                     imi.getJMXServiceURL());
        assertTrue(imi.isEnabled());
        assertNotNull(imi.getMBeanServer());

        WorkQueueManager manager = bus.getExtension(WorkQueueManager.class);
                
        MBeanServer mbs = im.getMBeanServer();      
        ObjectName name = new ObjectName(ManagementConstants.DEFAULT_DOMAIN_NAME 
                                         + ":type=WorkQueueManager,*");
        Set s = mbs.queryNames(name, null);
        StringBuilder b = new StringBuilder();
        for (ObjectName o : CastUtils.cast(s, ObjectName.class)) {
            b.append(o.toString());
            assertTrue("expected " + expect + " in object name: " + o,
                       o.toString().indexOf("bus.id=" + expect + ",") != -1);
            assertTrue("unexpected " + reject + " in object name: " + o,
                       o.toString().indexOf("bus.id=" + reject + ",") == -1);
            b.append("\n");
        }
        assertEquals("Size is wrong: " + b.toString(), 1, s.size());
        
        assertNotNull(manager.getNamedWorkQueue("testQueue"));
        manager.getAutomaticWorkQueue();

        name = new ObjectName(ManagementConstants.DEFAULT_DOMAIN_NAME 
                             + ":type=WorkQueues,*");
        s = mbs.queryNames(name, null);
        b = new StringBuilder();
        for (ObjectName o : CastUtils.cast(s, ObjectName.class)) {
            b.append(o.toString());
            assertTrue("expected " + expect + " in object name: " + o,
                       o.toString().indexOf("bus.id=" + expect + ",") != -1);
            assertTrue("unexpected " + reject + " in object name: " + o,
                       o.toString().indexOf("bus.id=" + reject + ",") == -1);
            b.append("\n");
        }
        assertEquals("Size is wrong: " + b.toString(), 2, s.size());
        
        Iterator it = s.iterator();
        while (it.hasNext()) {
            ObjectName n = (ObjectName)it.next();            
            Long result = 
                (Long)mbs.invoke(n, "getWorkQueueMaxSize", new Object[0], new String[0]);
            assertEquals(result, Long.valueOf(256));                

            Integer hwm = 
                (Integer)mbs.invoke(n, "getHighWaterMark", new Object[0], new String[0]);

            if (n.toString().contains("testQueue")) {
                assertEquals(hwm, Integer.valueOf(50));
            } else {
                assertEquals(hwm, Integer.valueOf(25));
            }
        }

        name = new ObjectName(ManagementConstants.DEFAULT_DOMAIN_NAME 
                                         + ":type=Bus,*");
        s = mbs.queryNames(name, null);
        assertTrue(s.size() == 1);
        it = s.iterator();
        while (it.hasNext()) {
            ObjectName n = (ObjectName)it.next();
            Object[] params = {Boolean.FALSE};
            String[] sig = {"boolean"};
            mbs.invoke(n, "shutdown", params, sig);            
        }        
        
        bus.shutdown(true);
    }
}
