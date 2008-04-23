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

import javax.management.JMException;
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
        HelloWorld hw = new HelloWorld();
        ObjectName name = new ObjectName("org.apache.cxf:type=foo,name=bar");
        manager.register(hw, name);
        String result = 
            (String)manager.getMBeanServer().invoke(name, "sayHi", new Object[0], new String[0]);    
        assertEquals("Wazzzuuup!", result);
    }
    
}
