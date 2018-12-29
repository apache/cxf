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

package org.apache.cxf.transport.websocket;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class WebSocketTransportFactoryTest {

    @Test
    public void testGetDestination() throws Exception {
        Bus bus = BusFactory.getDefaultBus();
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress("ws://localhost:8888/bar/foo");
        WebSocketTransportFactory factory = bus.getExtension(WebSocketTransportFactory.class);
        assertNotNull(factory);
        Destination dest = factory.getDestination(ei, bus);
        assertNotNull(dest);
    }

}