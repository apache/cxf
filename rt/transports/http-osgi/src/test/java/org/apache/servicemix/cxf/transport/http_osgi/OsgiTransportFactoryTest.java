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

package org.apache.servicemix.cxf.transport.http_osgi;

import java.util.Arrays;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactoryManager;

import org.easymock.classextension.IMocksControl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.classextension.EasyMock.*;


public class OsgiTransportFactoryTest extends Assert {

    private static final List<String> TRANSPORT_IDS = (List<String>) 
        Arrays.asList("http://cxf.apache.org/bindings/xformat",
                      "http://schemas.xmlsoap.org/soap/http",
                      "http://schemas.xmlsoap.org/wsdl/http/",
                      "http://schemas.xmlsoap.org/wsdl/soap/http",
                      "http://www.w3.org/2003/05/soap/bindings/HTTP/",
                      "http://cxf.apache.org/transports/http/configuration");

    private IMocksControl control; 
    private OsgiTransportFactory factory;
    private Bus bus;
    private DestinationFactoryManager dfm;
    private OsgiDestinationRegistryIntf registry;

    @Before
    public void setUp() {
        control = createNiceControl();
        factory = new OsgiTransportFactory();
        registry = control.createMock(OsgiDestinationRegistryIntf.class);
        bus = control.createMock(Bus.class);
        dfm = control.createMock(DestinationFactoryManager.class);
    }

    @After
    public void tearDown() {
        factory = null;
        registry = null;
        bus = null;
        dfm = null;
    }

    @Test
    public void testInit() throws Exception {
        try {
            factory.init();
            fail("expected  IllegalStateException on null bus");
        } catch (IllegalStateException ise) {
            // expected
        }
        factory.setBus(bus);

        try {
            factory.init();
            fail("expected  IllegalStateException on null registry");
        } catch (IllegalStateException ise) {
            // expected
        }
        factory.setRegistry(registry);
        
        bus.getExtension(DestinationFactoryManager.class);
        expectLastCall().andReturn(dfm);
        factory.setTransportIds(TRANSPORT_IDS);
        for (String ns : TRANSPORT_IDS) {
            dfm.registerDestinationFactory(ns, factory);
            expectLastCall();
        }
        control.replay();

        factory.init();

        control.verify();
    }
    
    @Test
    public void testGetDestination() throws Exception {
        factory.setBus(bus);
        factory.setRegistry(registry);

        EndpointInfo endpoint = new EndpointInfo();
        endpoint.setAddress("http://bar/snafu");
        try {
            factory.getDestination(endpoint);
            fail("expected  IllegalStateException on absolute URI");
        } catch (IllegalStateException ise) {
            // expected
        }
 
        endpoint.setAddress("snafu");
        registry.addDestination(eq("/snafu"), isA(OsgiDestination.class));

        control.replay();

        Destination d = factory.getDestination(endpoint);
        assertNotNull(d);
        assertNotNull(d.getAddress());
        assertNotNull(d.getAddress().getAddress());
        assertEquals("snafu", d.getAddress().getAddress().getValue());

        control.verify();
    }
}
