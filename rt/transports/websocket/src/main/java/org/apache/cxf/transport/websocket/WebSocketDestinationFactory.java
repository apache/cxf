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
import java.lang.reflect.Constructor;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.http.HttpDestinationFactory;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;
import org.apache.cxf.transport.http_undertow.UndertowHTTPServerEngineFactory;
import org.apache.cxf.transport.websocket.atmosphere.AtmosphereWebSocketServletDestination;
//import org.apache.cxf.transport.websocket.jetty.JettyWebSocketServletDestination;

@NoJSR250Annotations()
public class WebSocketDestinationFactory implements HttpDestinationFactory {
    private static final boolean ATMOSPHERE_AVAILABLE = probeClass("org.atmosphere.cpr.ApplicationConfig");
    private static final boolean JETTY_AVAILABLE = 
        probeClass("org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory");
    private static final boolean UNDERTOW_AVAILABLE = 
        probeClass("org.apache.cxf.transport.http_undertow.UndertowHTTPServerEngineFactory");
    private static final Constructor<?> JETTY11_WEBSOCKET_DESTINATION_CTR =
        probeConstructor("org.apache.cxf.transport.websocket.jetty12.Jetty12WebSocketDestination");
    private static final Constructor<?> UNDERTOW_WEBSOCKET_DESTINATION_CTR =
        probeUndertowConstructor("org.apache.cxf.transport.websocket.undertow.UndertowWebSocketDestination");
    private static final Constructor<?> ATMOSPHERE_WEBSOCKET_JETTY_DESTINATION_CTR =
        probeConstructor("org.apache.cxf.transport.websocket.atmosphere.AtmosphereWebSocketJettyDestination");
    private static final Constructor<?> ATMOSPHERE_WEBSOCKET_UNDERTOW_DESTINATION_CTR =
        probeUndertowConstructor(
            "org.apache.cxf.transport.websocket.atmosphere.AtmosphereWebSocketUndertowDestination");

    private final boolean atmosphereDisabled = Boolean.parseBoolean(SystemPropertyAction
        .getPropertyOrNull("org.apache.cxf.transport.websocket.atmosphere.disabled"));

    private static boolean probeClass(String name) {
        try {
            Class.forName(name, true, WebSocketDestinationFactory.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Constructor<?> probeConstructor(String name) {
        try {
            Class<?> clz = Class.forName(name, true, WebSocketDestinationFactory.class.getClassLoader());
            return clz.getConstructor(Bus.class, DestinationRegistry.class, EndpointInfo.class,
                                      JettyHTTPServerEngineFactory.class);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Constructor<?> probeUndertowConstructor(String name) {
        try {
            Class<?> clz = Class.forName(name, true, WebSocketDestinationFactory.class.getClassLoader());
            return clz.getConstructor(Bus.class, DestinationRegistry.class, EndpointInfo.class,
                                      UndertowHTTPServerEngineFactory.class);
        } catch (Throwable t) {
            return null;
        }
    }

    public AbstractHTTPDestination createDestination(EndpointInfo endpointInfo, Bus bus,
                                                     DestinationRegistry registry) throws IOException {
        if (endpointInfo.getAddress().startsWith("ws")) {

            if (ATMOSPHERE_AVAILABLE && !atmosphereDisabled) {
                // use atmosphere if available
                if (JETTY_AVAILABLE) {
                    // for the embedded mode, we stick to jetty
                    JettyHTTPServerEngineFactory serverEngineFactory = bus
                        .getExtension(JettyHTTPServerEngineFactory.class);
                    return createJettyHTTPDestination(ATMOSPHERE_WEBSOCKET_JETTY_DESTINATION_CTR, bus,
                                                      registry, endpointInfo, serverEngineFactory);
                } else if (UNDERTOW_AVAILABLE) {
                    // use AtmosphereWebSocketUndertowDestination
                    UndertowHTTPServerEngineFactory undertowServerEngineFactory = bus
                        .getExtension(UndertowHTTPServerEngineFactory.class);
                    return createUndertowHTTPDestination(ATMOSPHERE_WEBSOCKET_UNDERTOW_DESTINATION_CTR, bus,
                                                         registry, endpointInfo, undertowServerEngineFactory);
                }
                return null;
            }
            if (JETTY_AVAILABLE) {
            // for the embedded mode, we stick to jetty if jetty is available
                JettyHTTPServerEngineFactory serverEngineFactory = bus
                    .getExtension(JettyHTTPServerEngineFactory.class);
                return createJettyHTTPDestination(JETTY11_WEBSOCKET_DESTINATION_CTR, bus, registry,
                                              endpointInfo, serverEngineFactory);
            } else if (UNDERTOW_AVAILABLE) {
                // use UndertowWebSocketDestination
                UndertowHTTPServerEngineFactory undertowServerEngineFactory = bus
                    .getExtension(UndertowHTTPServerEngineFactory.class);
                return createUndertowHTTPDestination(UNDERTOW_WEBSOCKET_DESTINATION_CTR, bus,
                                                     registry, endpointInfo, undertowServerEngineFactory);
            }
            return null;
        }
        // REVISIT other way of getting the registry of http so that the plain cxf servlet finds the
        // destination?
        registry = getDestinationRegistry(bus);

        // choose atmosphere if available, otherwise assume jetty is available
        if (ATMOSPHERE_AVAILABLE && !atmosphereDisabled) {
            // use atmosphere if available
            return new AtmosphereWebSocketServletDestination(bus, registry, endpointInfo,
                                                             endpointInfo.getAddress());
        }
        // use jetty-websocket
        return createJettyHTTPDestination(JETTY11_WEBSOCKET_DESTINATION_CTR, bus, registry,
                                          endpointInfo, null);
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

    private AbstractHTTPDestination createJettyHTTPDestination(Constructor<?> ctr, Bus bus,
                                                               DestinationRegistry registry, EndpointInfo ei,
                                                               JettyHTTPServerEngineFactory jhsef)
                                                                   throws IOException {
        if (ctr != null) {
            try {
                return (AbstractHTTPDestination)ctr.newInstance(bus, registry, ei, jhsef);
            } catch (Throwable t) {
                // log
                t.printStackTrace();
            }
        }
        return null;
    }

    private AbstractHTTPDestination createUndertowHTTPDestination(Constructor<?> ctr, Bus bus,
                                                                  DestinationRegistry registry,
                                                                  EndpointInfo ei,
                                                                  UndertowHTTPServerEngineFactory jhsef)
                                                                      throws IOException {
        if (ctr != null) {
            try {
                return (AbstractHTTPDestination)ctr.newInstance(bus, registry, ei, jhsef);
            } catch (Throwable t) {
                // log
                t.printStackTrace();
            }
        }
        return null;
    }
}
