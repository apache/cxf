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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class RMUtilsTest {
    @Test
    public void testGetName() {
        // no bus given
        Endpoint e = mock(Endpoint.class);
        EndpointInfo ei = mock(EndpointInfo.class);
        when(e.getEndpointInfo()).thenReturn(ei);
        QName eqn = new QName("ns2", "endpoint");
        when(ei.getName()).thenReturn(eqn);
        ServiceInfo si = mock(ServiceInfo.class);
        when(ei.getService()).thenReturn(si);
        QName sqn = new QName("ns1", "service");
        when(si.getName()).thenReturn(sqn);

        assertEquals("{ns1}service.{ns2}endpoint@" + Bus.DEFAULT_BUS_ID,
                     RMUtils.getEndpointIdentifier(e));
        verify(e, times(2)).getEndpointInfo();

        // a named bus
        reset(e);
        when(e.getEndpointInfo()).thenReturn(ei);
        when(ei.getName()).thenReturn(eqn);
        when(ei.getService()).thenReturn(si);
        when(si.getName()).thenReturn(sqn);
        Bus b = mock(Bus.class);
        when(b.getId()).thenReturn("mybus");
        assertEquals("{ns1}service.{ns2}endpoint@mybus", RMUtils.getEndpointIdentifier(e, b));
        verify(e, times(2)).getEndpointInfo();

        // this test makes sure that an automatically generated id will be
        // mapped to the static default bus name "cxf".
        // System.out.println("bus: " + BusFactory.getThreadDefaultBus(false));
        reset(e);
        when(e.getEndpointInfo()).thenReturn(ei);
        when(ei.getName()).thenReturn(eqn);
        when(ei.getService()).thenReturn(si);
        when(si.getName()).thenReturn(sqn);

        Bus bus = BusFactory.getDefaultBus();
        assertEquals("{ns1}service.{ns2}endpoint@" + Bus.DEFAULT_BUS_ID,
                     RMUtils.getEndpointIdentifier(e, bus));
        bus.shutdown(true);
        verify(e, times(2)).getEndpointInfo();

        // a generated bundle artifact bus
        reset(e);
        when(e.getEndpointInfo()).thenReturn(ei);
        when(ei.getName()).thenReturn(eqn);
        when(ei.getService()).thenReturn(si);
        when(si.getName()).thenReturn(sqn);
        when(b.getId()).thenReturn("mybus-" + Bus.DEFAULT_BUS_ID + "12345");

        assertEquals("{ns1}service.{ns2}endpoint@mybus-" + Bus.DEFAULT_BUS_ID,
                     RMUtils.getEndpointIdentifier(e, b));
        verify(e, times(2)).getEndpointInfo();

        // look like a generated bundle artifact bus but not
        reset(e);
        when(e.getEndpointInfo()).thenReturn(ei);
        when(ei.getName()).thenReturn(eqn);
        when(ei.getService()).thenReturn(si);
        when(si.getName()).thenReturn(sqn);
        when(b.getId()).thenReturn("mybus." + Bus.DEFAULT_BUS_ID + ".foo");

        assertEquals("{ns1}service.{ns2}endpoint@mybus." + Bus.DEFAULT_BUS_ID + ".foo",
                     RMUtils.getEndpointIdentifier(e, b));

        verify(e, times(2)).getEndpointInfo();
    }
}