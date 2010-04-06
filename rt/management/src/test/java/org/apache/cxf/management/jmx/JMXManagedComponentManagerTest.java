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

package org.apache.cxf.management.jmx;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.cxf.management.jmx.export.AnnotationTestInstrumentation;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JMXManagedComponentManagerTest extends Assert {
       
    private static final String NAME_ATTRIBUTE = "Name";    
    private InstrumentationManagerImpl manager;
    
    @Before
    public void setUp() throws Exception {
        manager = new InstrumentationManagerImpl(); 
        manager.setDaemon(false);
        manager.setThreaded(true);
        manager.setEnabled(true);
        manager.setJMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:9913/jmxrmi");
        manager.init();
        //Wait for MBeanServer connector to be initialized on separate thread.
        Thread.sleep(2000);
    }
    
    @After
    public void tearDown() throws Exception {
        manager.shutdown();
    }
    
    @Test
    public void testRegisterInstrumentation() throws Exception {
        //manager.setDaemon(false);
        //manager.setThreaded(false);
        //manager.setJMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:9913/jmxrmi");
        //manager.init();

        AnnotationTestInstrumentation im = new AnnotationTestInstrumentation();
        ObjectName name = new ObjectName("org.apache.cxf:type=foo,name=bar");        
        im.setName("John Smith");          
        manager.register(im, name);
        
        Object val = manager.getMBeanServer().getAttribute(name, NAME_ATTRIBUTE);
        assertEquals("Incorrect result", "John Smith", val);
        
        try {
            manager.register(im, name);
            fail("Registering with existing name should fail.");
        } catch (JMException jmex) {
            //Expected
        }
        
        manager.register(im, name, true);                
        
        val = manager.getMBeanServer().getAttribute(name, NAME_ATTRIBUTE);
        assertEquals("Incorrect result", "John Smith", val);
        manager.unregister(name);
        
        im.setName("Foo Bar");  
        name = manager.register(im);

        val = manager.getMBeanServer().getAttribute(name, NAME_ATTRIBUTE);
        assertEquals("Incorrect result", "Foo Bar", val);
        
        try {
            manager.register(im);
            fail("Registering with existing name should fail.");
        } catch (JMException jmex) {
            //Expected
        }        
        
        name = manager.register(im, true);
        
        val = manager.getMBeanServer().getAttribute(name, NAME_ATTRIBUTE);
        assertEquals("Incorrect result", "Foo Bar", val);
        
        manager.unregister(im);
    }

    @Test
    public void testRegisterStandardMBean() throws Exception {
        ObjectName name = this.registerStandardMBean("yo!");
        String result = 
            (String)manager.getMBeanServer().invoke(name, "sayHi", new Object[0], new String[0]);    
        assertEquals("Wazzzuuup yo!", result);
    }
    
    /**
     * Simulate repeated startup and shutdown of the CXF Bus in an environment
     * where the container and MBeanServer are not shutdown between CXF restarts.
     */
    @Test
    public void testBusLifecycleListener() throws Exception {
        // We need to destroy the manager that is automatically setup by the test.
        this.tearDown();
        
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        
        this.manager = new InstrumentationManagerImpl(); 
        this.manager.setDaemon(false);
        // Turn threading off so that we get the exception in this thread
        // and the manager is set into a failed state if the connector
        // cannot be created.
        this.manager.setThreaded(false);
        this.manager.setEnabled(true);
        this.manager.setJMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:9913/jmxrmi");
        this.manager.setServer(server);
        this.manager.register();
        this.manager.init();
        
        ObjectName name = this.registerStandardMBean("yo!");
        String result = 
            (String)manager.getMBeanServer().invoke(name, "sayHi", new Object[0], new String[0]);    
        assertEquals("Wazzzuuup yo!", result);
        
        try {
            name = this.registerStandardMBean("yo!");
            fail("registered duplicate MBean");
        } catch (InstanceAlreadyExistsException e) {
            // expected
        }
        
        this.manager.preShutdown();
        this.manager.postShutdown();
        
        try {
            this.manager.getMBeanServer().invoke(name, "sayHi", new Object[0], new String[0]);
            fail("MBean not unregistered on shutdown.");
        } catch (InstanceNotFoundException e) {
            // expected
        }
        
        this.manager = new InstrumentationManagerImpl(); 
        this.manager.setDaemon(false);
        // Turn threading off so that we get the exception in this thread
        // and the manager is set into a failed state if the connector
        // cannot be created.
        this.manager.setThreaded(false);
        this.manager.setEnabled(true);
        this.manager.setJMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:9913/jmxrmi");
        this.manager.setServer(server);
        this.manager.register();
        this.manager.init();
        
        name = this.registerStandardMBean("yoyo!");
        result = 
            (String)manager.getMBeanServer().invoke(name, "sayHi", new Object[0], new String[0]);    
        assertEquals("Wazzzuuup yoyo!", result);
    }
    
    private ObjectName registerStandardMBean(String name) throws Exception {
        final HelloWorld hw = new HelloWorld(name);
        final ObjectName oName = new ObjectName("org.apache.cxf:type=foo,name=bar");
        this.manager.register(hw, oName);
        return oName;
    }
}
