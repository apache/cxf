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
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngine;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;
import org.apache.cxf.transport.websocket.jetty12.Jetty12WebSocketDestination;
import org.eclipse.jetty.server.Server;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
/**
 *
 */
public class JettyWebSocketDestinationTest {
    private static final String ENDPOINT_ADDRESS = "ws://localhost:9001/websocket/nada";
    private static final QName ENDPOINT_NAME = new QName("urn:websocket:probe", "nada");

    @Test
    public void testRegisteration() throws Exception {
        Bus bus = new ExtensionManagerBus();
        DestinationRegistry registry = new HTTPTransportFactory().getRegistry();
        EndpointInfo endpoint = new EndpointInfo();
        endpoint.setAddress(ENDPOINT_ADDRESS);
        endpoint.setName(ENDPOINT_NAME);
        JettyHTTPServerEngine engine = mock(JettyHTTPServerEngine.class);
        engine.setServer(new Server());
        TestJettyWebSocketDestination dest = new TestJettyWebSocketDestination(bus, registry, endpoint, null, engine);

        dest.activate();

        assertNotNull(registry.getDestinationForPath(ENDPOINT_ADDRESS));

        dest.deactivate();

        assertNull(registry.getDestinationForPath(ENDPOINT_ADDRESS));
    }

    private static class TestJettyWebSocketDestination extends Jetty12WebSocketDestination {
        TestJettyWebSocketDestination(Bus bus, DestinationRegistry registry, EndpointInfo ei,
                                      JettyHTTPServerEngineFactory serverEngineFactory,
                                      JettyHTTPServerEngine engine) throws IOException {
            super(bus, registry, ei, serverEngineFactory);
            this.engine = engine;
        }

        @Override
        public void activate() {
            synchronized (this) {
                if (registry != null) {
                    registry.addDestination(this);
                }
            }
        }

        @Override
        public void deactivate() {
            super.deactivate();                
        }
    }
}
