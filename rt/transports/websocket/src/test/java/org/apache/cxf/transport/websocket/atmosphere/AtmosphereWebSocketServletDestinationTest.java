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

package org.apache.cxf.transport.websocket.atmosphere;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.atmosphere.cpr.AtmosphereInterceptor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 *
 */
public class AtmosphereWebSocketServletDestinationTest {
    private static final String ENDPOINT_ADDRESS = "/websocket/nada";
    private static final QName ENDPOINT_NAME = new QName("urn:websocket:probe", "nada");

    @Test
    public void testRegisteration() throws Exception {
        Bus bus = new ExtensionManagerBus();
        DestinationRegistry registry = new HTTPTransportFactory().getRegistry();
        EndpointInfo endpoint = new EndpointInfo();
        endpoint.setAddress(ENDPOINT_ADDRESS);
        endpoint.setName(ENDPOINT_NAME);

        TestAtmosphereWebSocketServletDestination dest =
            new TestAtmosphereWebSocketServletDestination(bus, registry, endpoint, ENDPOINT_ADDRESS);

        dest.activate();

        assertNotNull(registry.getDestinationForPath(ENDPOINT_ADDRESS));

        dest.deactivate();

        assertNull(registry.getDestinationForPath(ENDPOINT_ADDRESS));
    }

    @Test
    public void testUseCXFDefaultAtmoosphereInterceptor() throws Exception {
        Bus bus = new ExtensionManagerBus();
        DestinationRegistry registry = new HTTPTransportFactory().getRegistry();
        EndpointInfo endpoint = new EndpointInfo();
        endpoint.setAddress(ENDPOINT_ADDRESS);
        endpoint.setName(ENDPOINT_NAME);

        AtmosphereWebSocketServletDestination dest =
            new AtmosphereWebSocketServletDestination(bus, registry, endpoint, ENDPOINT_ADDRESS);

        List<AtmosphereInterceptor> ais = dest.getAtmosphereFramework().interceptors();
        int added = 0;
        for (AtmosphereInterceptor a : ais) {
            if (DefaultProtocolInterceptor.class.equals(a.getClass())) {
                added++;
                break;
            }
        }
        assertEquals(1, added);
    }

    @Test
    public void testUseCustomAtmoosphereInterceptor() throws Exception {
        Bus bus = new ExtensionManagerBus();
        bus.setProperty("atmosphere.interceptors", new CustomInterceptor1());
        DestinationRegistry registry = new HTTPTransportFactory().getRegistry();
        EndpointInfo endpoint = new EndpointInfo();
        endpoint.setAddress(ENDPOINT_ADDRESS);
        endpoint.setName(ENDPOINT_NAME);

        AtmosphereWebSocketServletDestination dest =
            new AtmosphereWebSocketServletDestination(bus, registry, endpoint, ENDPOINT_ADDRESS);

        List<AtmosphereInterceptor> ais = dest.getAtmosphereFramework().interceptors();
        int added = 0;
        for (AtmosphereInterceptor a : ais) {
            if (CustomInterceptor1.class.equals(a.getClass())) {
                added++;
                break;
            }
        }
        assertEquals(1, added);
    }

    @Test
    public void testUseCustomAtmoosphereInterceptors() throws Exception {
        Bus bus = new ExtensionManagerBus();
        bus.setProperty("atmosphere.interceptors", Arrays.asList(new CustomInterceptor1(), new CustomInterceptor2()));
        DestinationRegistry registry = new HTTPTransportFactory().getRegistry();
        EndpointInfo endpoint = new EndpointInfo();
        endpoint.setAddress(ENDPOINT_ADDRESS);
        endpoint.setName(ENDPOINT_NAME);

        AtmosphereWebSocketServletDestination dest =
            new AtmosphereWebSocketServletDestination(bus, registry, endpoint, ENDPOINT_ADDRESS);

        List<AtmosphereInterceptor> ais = dest.getAtmosphereFramework().interceptors();
        int added = 0;
        for (AtmosphereInterceptor a : ais) {
            if (CustomInterceptor1.class.equals(a.getClass())) {
                added++;
            } else if (CustomInterceptor2.class.equals(a.getClass())) {
                added++;
                break;
            }
        }
        assertEquals(2, added);
    }

    private static class TestAtmosphereWebSocketServletDestination extends AtmosphereWebSocketServletDestination {


        TestAtmosphereWebSocketServletDestination(Bus bus, DestinationRegistry registry,
                                                  EndpointInfo ei, String path) throws IOException {
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

    private static final class CustomInterceptor1 extends DefaultProtocolInterceptor {
    }
    private static final class CustomInterceptor2 extends DefaultProtocolInterceptor {
    }
}