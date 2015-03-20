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

import java.io.IOException;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.http.HttpDestinationFactory;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;
import org.apache.cxf.transport.websocket.atmosphere.AtmosphereWebSocketJettyDestination;
import org.apache.cxf.transport.websocket.atmosphere.AtmosphereWebSocketServletDestination;
import org.apache.cxf.transport.websocket.jetty.JettyWebSocketDestination;
import org.apache.cxf.transport.websocket.jetty.JettyWebSocketServletDestination;

@NoJSR250Annotations()
public class WebSocketDestinationFactory implements HttpDestinationFactory {
    private static final boolean ATMOSPHERE_AVAILABLE = probeClass("org.atmosphere.cpr.ApplicationConfig");
    
    private static boolean probeClass(String name) {
        try {
            Class.forName(name, true, WebSocketDestinationFactory.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
    
    public AbstractHTTPDestination createDestination(EndpointInfo endpointInfo, Bus bus,
                                                     DestinationRegistry registry) throws IOException {
        if (endpointInfo.getAddress().startsWith("ws")) {
            // for the embedded mode, we stick to jetty
            JettyHTTPServerEngineFactory serverEngineFactory = bus
                .getExtension(JettyHTTPServerEngineFactory.class);
            if (ATMOSPHERE_AVAILABLE) {
                // use atmosphere if available
                return new AtmosphereWebSocketJettyDestination(bus, registry, endpointInfo, serverEngineFactory);
            } else {
                return new JettyWebSocketDestination(bus, registry, endpointInfo, serverEngineFactory);
            }
        } else {
            //REVISIT other way of getting the registry of http so that the plain cxf servlet finds the destination?
            registry = getDestinationRegistry(bus);
            
            // choose atmosphere if available, otherwise assume jetty is available
            if (ATMOSPHERE_AVAILABLE) {
                // use atmosphere if available
                return new AtmosphereWebSocketServletDestination(bus, registry,
                                                                 endpointInfo, endpointInfo.getAddress());
            } else {
                // use jetty-websocket
                return new JettyWebSocketServletDestination(bus, registry,
                                                            endpointInfo, endpointInfo.getAddress());
            }
        }
    }

    private static DestinationRegistry getDestinationRegistry(Bus bus) {
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        try {
            DestinationFactory df = dfm
                .getDestinationFactory("http://cxf.apache.org/transports/http/configuration");
            if (df instanceof HTTPTransportFactory) {
                HTTPTransportFactory transportFactory = (HTTPTransportFactory)df;
                return transportFactory.getRegistry();
            }
        } catch (Exception e) {
            // why are we throwing a busexception if the DF isn't found?
        }
        return null;
    }
    

}
