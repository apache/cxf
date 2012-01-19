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

import java.util.Set;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.management.ManagementConstants;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * 
 */
public class BusRegistrationTest extends Assert {
    private Bus serverBus;
    private Bus clientBus;
    private InstrumentationManager serverIM;
    private InstrumentationManager clientIM;
    private boolean ready;
    private boolean running;
    
    @Before
    public void setUp() throws Exception {
    }
    
    @After
    public void tearDown() throws Exception {
        if (clientBus != null) {
            clientBus.shutdown(true);
        }
        if (serverBus != null) {
            serverBus.shutdown(true);
        }
    }

    @Test
    public void testRegisterMultipleBuses() throws Exception {
        // classic external IM-bean
        testRegisterMultipleBuses("managed-spring.xml");
    }

    @Test
    public void testRegisterMultipleBuses2() throws Exception {
        // integrated IM configuration in bus
        testRegisterMultipleBuses("managed-spring2.xml");
    }
    
    private void testRegisterMultipleBuses(String conf) throws Exception {
        final SpringBusFactory factory = new SpringBusFactory();
        serverBus =  factory.createBus(conf);
        assertEquals("CXF-Test-Bus", serverBus.getId());
        serverIM = serverBus.getExtension(InstrumentationManager.class);
        assertTrue("Instrumentation Manager should not be null", serverIM != null);
        Thread t = new Thread(new Runnable() {
            public void run() {
                clientBus = factory.createBus("no-connector-spring.xml");
                clientIM = clientBus.getExtension(InstrumentationManager.class);
                assertTrue("Instrumentation Manager should not be null", clientIM != null);
                ready = true;
                running = true;
                while (running) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // ignore and leave
                        running = false;
                    }
                }
            }
        });
        
        t.start();
        while (!ready) {
            Thread.sleep(1000);
        }
        
        try {
            MBeanServer mbs = serverIM.getMBeanServer();
            assertNotNull("MBeanServer should be available.", mbs);
            MBeanServer mbs2 = clientIM.getMBeanServer();
            assertNotNull("MBeanServer should be available.", mbs2);
            
            // check if these servers refer to the same server
            assertEquals("There should be one MBeanServer", mbs, mbs2);
            
            // check both server and client bus can be found from this server
            Set s;
            ObjectName serverName = getObjectName(serverBus);
            s = mbs.queryNames(serverName, null);
            assertTrue("sever-side bus should be found", s.size() == 1);

            ObjectName clientName = getObjectName(clientBus);
            s = mbs.queryNames(clientName, null);
            assertTrue("client-side bus should be found", s.size() == 1);
        } finally {
            running = false;            
        }
    }
    
        
    private static ObjectName getObjectName(Bus bus) throws JMException {
        String busId = bus.getId();
        return getObjectName(busId);
    }

    private static ObjectName getObjectName(String id) throws JMException {
        StringBuilder buffer = new StringBuilder(ManagementConstants.DEFAULT_DOMAIN_NAME + ":");
        buffer.append(ManagementConstants.BUS_ID_PROP + "=" +  id + ",");
        buffer.append(ManagementConstants.TYPE_PROP + "=Bus");
        
        return new ObjectName(buffer.toString());
    }
}
