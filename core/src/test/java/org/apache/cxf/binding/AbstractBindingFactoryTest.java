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

package org.apache.cxf.binding;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Destination;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractBindingFactoryTest {

    private Bus bus;

    private ConcreateBindingFactory concreateBindingFactory;

    static class ConcreateBindingFactory extends AbstractBindingFactory {

        ConcreateBindingFactory() {
            super();
        }

        ConcreateBindingFactory(Collection<String> ns) {
            super(ns);
        }

        ConcreateBindingFactory(Bus b, Collection<String> ns) {
            super(b, ns);
        }

        ConcreateBindingFactory(Bus b) {
            super(b);
        }

        @Override
        public Binding createBinding(BindingInfo binding) {
            return null;
        }
    }

    @Before
    public void setup() {
        bus = BusFactory.getDefaultBus();
    }

    @Test
    public void testCreateBindingInfo() {
        concreateBindingFactory = new ConcreateBindingFactory();
        ServiceInfo serviceInfo = Mockito.mock(ServiceInfo.class);
        String namespace = "ns1";
        BindingInfo actual = concreateBindingFactory.createBindingInfo(serviceInfo, namespace, null);

        BindingInfo expected = new BindingInfo(serviceInfo, namespace);
        assertEquals(expected.toString(), actual.toString());

        Service service = Mockito.mock(Service.class);
        when(service.getServiceInfos()).thenReturn(List.of(serviceInfo));
        QName qName = new QName("http://cxf.com/FOO", "localPart");
        when(service.getName()).thenReturn(qName);
        actual = concreateBindingFactory.createBindingInfo(service, namespace, null);
        assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testGetBus() {
        concreateBindingFactory = new ConcreateBindingFactory(bus);
        assertEquals(bus, concreateBindingFactory.getBus());
        Bus differentBus = BusFactory.newInstance().createBus();
        concreateBindingFactory.setBus(differentBus);
        assertEquals(differentBus, concreateBindingFactory.getBus());
    }

    @Test
    public void testActivationNamespaces() {
        Collection<String> ns = List.of("ns1", "ns2", "ns3");
        concreateBindingFactory = new ConcreateBindingFactory(ns);
        assertEquals(ns, concreateBindingFactory.getActivationNamespaces());

        Collection<String> differentNs = List.of("ns4", "ns5", "ns6");
        concreateBindingFactory.setActivationNamespaces(differentNs);
        assertEquals(differentNs, concreateBindingFactory.getActivationNamespaces());
    }

    @Test
    public void testBusAndActivationNamespaces() {
        Collection<String> ns = List.of("ns1", "ns2", "ns3");
        concreateBindingFactory = new ConcreateBindingFactory(bus, ns);
        assertEquals(ns, concreateBindingFactory.getActivationNamespaces());
    }

    @Test
    public void testAddListener() {
        concreateBindingFactory = new ConcreateBindingFactory(bus);
        Destination destination = Mockito.mock(Destination.class);
        Endpoint endpoint = Mockito.mock(Endpoint.class);
        concreateBindingFactory.addListener(destination, endpoint);
        verify(destination, times(1)).setMessageObserver(any());
    }
}
