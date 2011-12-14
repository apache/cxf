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
package org.apache.cxf.transport.http_jaxws_spi;

import javax.xml.ws.spi.http.HttpContext;
import javax.xml.ws.spi.http.HttpHandler;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JAXWSHttpSpiTransportFactoryTest extends Assert {
    
    private IMocksControl control; 
    private HttpContext context;
    private JAXWSHttpSpiTransportFactory factory;
    private Bus bus;

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        context = control.createMock(HttpContext.class);
        bus = control.createMock(Bus.class);
        factory = new JAXWSHttpSpiTransportFactory(bus, context);
    }
    
    @After
    public void tearDown() {
        factory = null;
        context = null;
        bus = null;
    }
    
    @Test
    public void testGetDestination1() throws Exception {
        getDestination("/bar");
    }
    
    @Test
    public void testGetDestination2() throws Exception {
        getDestination("http://localhost:8080/foo/bar");
    }
    
    public void getDestination(String endpointAddress) throws Exception {
        context.setHandler(EasyMock.isA(HttpHandler.class));
        control.replay();
        
        EndpointInfo endpoint = new EndpointInfo();
        endpoint.setAddress(endpointAddress);
        
        Destination destination = factory.getDestination(endpoint);
        assertNotNull(destination);
        assertNotNull(destination.getAddress());
        assertNotNull(destination.getAddress().getAddress());
        assertEquals(endpointAddress, destination.getAddress().getAddress().getValue());
        assertEquals(endpointAddress, endpoint.getAddress());
        control.verify();
    }
    
}
