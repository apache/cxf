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
package org.apache.cxf.transport.http;

import java.io.IOException;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.service.model.EndpointInfo;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HTTPTransportFactoryTest {
    @Test
    public void testGetDestination() {
        Bus bus = BusFactory.getDefaultBus();
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress("http://nowhere.com/bar/foo");
        HTTPTransportFactory factory = bus.getExtension(HTTPTransportFactory.class);
        if (factory != null) {
            try {
                factory.getDestination(ei, bus);
                fail("Expect exception here.");
            } catch (IOException ex) {
                assertTrue("We should find some exception related to the HttpDestination",
                       ex.getMessage().indexOf("HttpDestinationFactory") > 0);
            }
        }
    }

}