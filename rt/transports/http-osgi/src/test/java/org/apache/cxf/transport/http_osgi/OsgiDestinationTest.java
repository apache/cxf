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

package org.apache.cxf.transport.http_osgi;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class OsgiDestinationTest extends Assert {

    private static final String ADDRESS = "http://bar/snafu";
    private IMocksControl control; 
    private Bus bus;
    private DestinationRegistry registry;
    private EndpointInfo endpoint;

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        bus = control.createMock(Bus.class);
        registry = control.createMock(DestinationRegistry.class);
        endpoint = new EndpointInfo();
        endpoint.setAddress(ADDRESS);
    }

    @After
    public void tearDown() {
        bus = null;
        registry = null;
    }

    @Test
    public void testCtor() throws Exception {
        OsgiDestination destination = 
            new OsgiDestination(bus, endpoint, registry, "snafu");

        assertNull(destination.getMessageObserver());
        assertNotNull(destination.getAddress());
        assertNotNull(destination.getAddress().getAddress());
        assertEquals(ADDRESS, 
                     destination.getAddress().getAddress().getValue());
    }

    @Test
    public void testShutdown() throws Exception {
        registry.removeDestination("snafu");
        EasyMock.expectLastCall();
        control.replay();

        OsgiDestination destination = 
            new OsgiDestination(bus, endpoint, registry, "snafu");

        destination.shutdown();
         
        control.verify();
    }

}
