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

import java.sql.SQLException;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.ws.rm.RMManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class RMTxStoreConfigurationTest {

    @Test
    public void testTxStoreBean() {
        // connect exception only results in a log message
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus("org/apache/cxf/ws/rm/persistence/jdbc/txstore-bean.xml");
        RMManager manager = bus.getExtension(RMManager.class);
        assertNotNull(manager);
        RMTxStore store = (RMTxStore)manager.getStore();
        assertNotNull(store);
        assertNotNull("Connection should be null", store.getConnection());
        assertEquals("org.apache.derby.jdbc.EmbeddedDriver", store.getDriverClassName());
        assertEquals("scott", store.getUserName());
        assertEquals("tiger", store.getPassword());
        assertEquals("jdbc:derby:target/wsrmdb3;create=true", store.getUrl());
        assertNull("schema should be unset", store.getSchemaName());
    }

    @Test
    public void testSetCustomTableExistsState() {
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus("org/apache/cxf/ws/rm/persistence/jdbc/txstore-custom-error-bean.xml");
        RMManager manager = bus.getExtension(RMManager.class);
        assertNotNull(manager);
        RMTxStore store = (RMTxStore)manager.getStore();

        assertTrue(store.isTableExistsError(new SQLException("Table exists", "I6000", 288)));

        assertFalse(store.isTableExistsError(new SQLException("Unknown error", "00000", -1)));
    }

    @Test
    public void testSetCustomTableExistsState2() {
        RMTxStore.deleteDatabaseFiles("target/wsrmdb5", true);
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus("org/apache/cxf/ws/rm/persistence/jdbc/txstore-custom-error-bean2.xml");
        RMManager manager = bus.getExtension(RMManager.class);
        assertNotNull(manager);
        RMTxStore store = (RMTxStore)manager.getStore();

        assertTrue(store.isTableExistsError(new SQLException("Table exists", "I6000", 288)));

        assertFalse(store.isTableExistsError(new SQLException("Unknown error", "00000", -1)));
    }

    @Test
    public void testTxStoreWithDataSource() {
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus("org/apache/cxf/ws/rm/persistence/jdbc/txstore-ds-bean.xml");
        RMManager manager = bus.getExtension(RMManager.class);
        assertNotNull(manager);
        RMTxStore store = (RMTxStore)manager.getStore();

        assertNotNull(store.getDataSource());

        assertNotNull(store.getConnection());
    }

    @Test
    public void testTxStoreWithDataSource2() {
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus("org/apache/cxf/ws/rm/persistence/jdbc/txstore-ds-bean2.xml");
        RMManager manager = bus.getExtension(RMManager.class);
        assertNotNull(manager);
        RMTxStore store = (RMTxStore)manager.getStore();

        assertNotNull(store.getDataSource());

        assertNotNull(store.getConnection());
    }

}