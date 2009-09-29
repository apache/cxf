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
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactoryManager;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OsgiTransportFactoryTest extends Assert {

    private IMocksControl control; 
    private OsgiTransportFactory factory;
    private Bus bus;
    private OsgiDestinationRegistryIntf registry;

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        factory = new OsgiTransportFactory();
        registry = control.createMock(OsgiDestinationRegistryIntf.class);
        bus = control.createMock(Bus.class);
        bus.getExtension(DestinationFactoryManager.class);
        EasyMock.expectLastCall().andReturn(null).anyTimes();
    }

    @After
    public void tearDown() {
        factory = null;
        registry = null;
        bus = null;
    }

    
    @Test
    public void testGetDestination() throws Exception {
        registry.addDestination(EasyMock.eq("/snafu"), EasyMock.isA(OsgiDestination.class));
        control.replay();
        
        factory.setRegistry(registry);
        factory.setBus(bus);

        EndpointInfo endpoint = new EndpointInfo();
        endpoint.setAddress("http://bar/snafu");
        try {
            factory.getDestination(endpoint);
            fail("expected  IllegalStateException on absolute URI");
        } catch (IllegalStateException ise) {
            // expected
        }
 
        endpoint.setAddress("snafu");

        Destination d = factory.getDestination(endpoint);
        assertNotNull(d);
        assertNotNull(d.getAddress());
        assertNotNull(d.getAddress().getAddress());
        assertEquals("snafu", d.getAddress().getAddress().getValue());

        control.verify();
    }
}
