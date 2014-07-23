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

package org.apache.cxf.transport.websocket.jetty;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPTransportFactory;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 */
public class JettyWebSocketServletDestinationTest extends Assert {
    private static final String ENDPOINT_ADDRESS = "/websocket/nada";
    private static final QName ENDPOINT_NAME = new QName("urn:websocket:probe", "nada");

    @Test
    public void testRegisteration() throws Exception {
        Bus bus = new ExtensionManagerBus();        
        DestinationRegistry registry = new HTTPTransportFactory().getRegistry();
        EndpointInfo endpoint = new EndpointInfo();
        endpoint.setAddress(ENDPOINT_ADDRESS);
        endpoint.setName(ENDPOINT_NAME);

        TestJettyWebSocketServletDestination dest = 
            new TestJettyWebSocketServletDestination(bus, registry, endpoint, ENDPOINT_ADDRESS);

        dest.activate();
        
        assertNotNull(registry.getDestinationForPath(ENDPOINT_ADDRESS));
        
        dest.deactivate();

        assertNull(registry.getDestinationForPath(ENDPOINT_ADDRESS));
    }
    
    private static class TestJettyWebSocketServletDestination extends JettyWebSocketServletDestination {

        public TestJettyWebSocketServletDestination(Bus bus, DestinationRegistry registry, EndpointInfo ei,
                                                    String path) throws IOException {
            super(bus, registry, ei, path);
        }

        @Override
        public void activate() {
            super.activate();
        }

        @Override
        public void deactivate() {
            super.deactivate();
        }
    }
}
