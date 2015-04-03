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

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.atmosphere.cpr.AtmosphereInterceptor;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 */
public class AtmosphereWebSocketJettyDestinationTest extends Assert {
    private static final String ENDPOINT_ADDRESS = "ws://localhost:8080/websocket/nada";
    private static final QName ENDPOINT_NAME = new QName("urn:websocket:probe", "nada");

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
        assertEquals(1, ais.size());
        assertEquals(DefaultProtocolInterceptor.class, ais.get(0).getClass());
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
        assertEquals(2, ais.size());
        assertEquals(CustomInterceptor1.class, ais.get(0).getClass());
        assertEquals(CustomInterceptor2.class, ais.get(1).getClass());
    }
    
    private static class CustomInterceptor1 extends DefaultProtocolInterceptor {
    }
    private static class CustomInterceptor2 extends DefaultProtocolInterceptor {
    }
}
