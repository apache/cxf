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

package org.apache.cxf.ws.rm;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class RMUtilsTest extends Assert {

    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    }

    @After
    public void tearDown() {
        control.verify();
    }
    
    @Test
    public void testGetName() {
        // no bus given
        Endpoint e = control.createMock(Endpoint.class);
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        EasyMock.expect(e.getEndpointInfo()).andReturn(ei).times(2);
        QName eqn = new QName("ns2", "endpoint");
        EasyMock.expect(ei.getName()).andReturn(eqn);
        ServiceInfo si = control.createMock(ServiceInfo.class);
        EasyMock.expect(ei.getService()).andReturn(si);
        QName sqn = new QName("ns1", "service");
        EasyMock.expect(si.getName()).andReturn(sqn);
        control.replay();
        assertEquals("{ns1}service.{ns2}endpoint@" + Bus.DEFAULT_BUS_ID, 
                     RMUtils.getEndpointIdentifier(e));

        // a named bus
        control.reset();
        EasyMock.expect(e.getEndpointInfo()).andReturn(ei).times(2);
        EasyMock.expect(ei.getName()).andReturn(eqn);
        EasyMock.expect(ei.getService()).andReturn(si);
        EasyMock.expect(si.getName()).andReturn(sqn);
        Bus b = control.createMock(Bus.class);
        EasyMock.expect(b.getId()).andReturn("mybus");
        control.replay();
        assertEquals("{ns1}service.{ns2}endpoint@mybus", RMUtils.getEndpointIdentifier(e, b));

        // this test makes sure that an automatically generated id will be
        // mapped to the static default bus name "cxf".
        control.reset();
        EasyMock.expect(e.getEndpointInfo()).andReturn(ei).times(2);
        EasyMock.expect(ei.getName()).andReturn(eqn);
        EasyMock.expect(ei.getService()).andReturn(si);
        EasyMock.expect(si.getName()).andReturn(sqn);
        control.replay();
        assertEquals("{ns1}service.{ns2}endpoint@" + Bus.DEFAULT_BUS_ID, 
                     RMUtils.getEndpointIdentifier(e, BusFactory.getDefaultBus()));

        // a generated bundle artifact bus
        control.reset();
        EasyMock.expect(e.getEndpointInfo()).andReturn(ei).times(2);
        EasyMock.expect(ei.getName()).andReturn(eqn);
        EasyMock.expect(ei.getService()).andReturn(si);
        EasyMock.expect(si.getName()).andReturn(sqn);
        EasyMock.expect(b.getId()).andReturn("mybus-" + Bus.DEFAULT_BUS_ID + "12345");
        control.replay();
        assertEquals("{ns1}service.{ns2}endpoint@mybus-" + Bus.DEFAULT_BUS_ID, 
                     RMUtils.getEndpointIdentifier(e, b));

        // look like a generated bundle artifact bus but not
        control.reset();
        EasyMock.expect(e.getEndpointInfo()).andReturn(ei).times(2);
        EasyMock.expect(ei.getName()).andReturn(eqn);
        EasyMock.expect(ei.getService()).andReturn(si);
        EasyMock.expect(si.getName()).andReturn(sqn);
        EasyMock.expect(b.getId()).andReturn("mybus." + Bus.DEFAULT_BUS_ID + ".foo");
        control.replay();
        assertEquals("{ns1}service.{ns2}endpoint@mybus." + Bus.DEFAULT_BUS_ID + ".foo", 
                     RMUtils.getEndpointIdentifier(e, b));
        
    } 
}
