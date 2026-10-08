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

package org.apache.cxf.endpoint;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ClientImplTest {

    private static final QName PORT_NAME = new QName("http://cxf.apache.org/test", "TestPort");

    @Test
    public void testEndpointRegisteredWithService() throws Exception {
        Bus bus = new ExtensionManagerBus();
        Service svc = createService();

        Client client = new ClientImpl(bus, svc, PORT_NAME, null);

        assertEquals(1, svc.getEndpoints().size());
        assertSame(client.getEndpoint(), svc.getEndpoints().get(PORT_NAME));
        assertSame(client.getEndpoint(), client.getEndpoint().getService().getEndpoints().get(PORT_NAME));
    }

    @Test
    public void testEndpointFromFactoryRegisteredWithService() throws Exception {
        Bus bus = new ExtensionManagerBus();
        Service svc = createService();

        Client client = new ClientImpl(bus, svc, PORT_NAME, SimpleEndpointImplFactory.getSingleton());

        assertEquals(1, svc.getEndpoints().size());
        assertSame(client.getEndpoint(), svc.getEndpoints().get(PORT_NAME));
    }

    private static Service createService() {
        ServiceInfo si = new ServiceInfo();
        si.setName(new QName("http://cxf.apache.org/test", "TestService"));
        EndpointInfo ei = new EndpointInfo(si, "http://schemas.xmlsoap.org/soap/http");
        ei.setName(PORT_NAME);
        ei.setAddress("http://localhost:9000/test");
        si.addEndpoint(ei);
        return new ServiceImpl(si);
    }
}
