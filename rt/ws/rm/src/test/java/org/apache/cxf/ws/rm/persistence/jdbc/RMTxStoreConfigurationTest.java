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

package org.apache.cxf.ws.rm.persistence.jdbc;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.ws.rm.RMManager;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 */
public class RMTxStoreConfigurationTest extends Assert {
    
    @Test     
    public void testTxStoreBean() {
        // connect exception only results in a log message 
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus("org/apache/cxf/ws/rm/persistence/jdbc/txstore-bean.xml");
        RMManager manager = bus.getExtension(RMManager.class);
        assertNotNull(manager);
        RMTxStore store = (RMTxStore)manager.getStore();
        assertNotNull(store);
        assertNull("Connection should be null", store.getConnection());
        assertEquals("org.apache.derby.jdbc.NoDriver", store.getDriverClassName());
        assertEquals("scott", store.getUserName());
        assertEquals("tiger", store.getPassword());
        assertEquals("jdbc:derby://localhost:1527/rmdb;create=true", store.getUrl());
    }
   
}
