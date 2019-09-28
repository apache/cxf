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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractTransportFactory;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.DestinationRegistryImpl;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.apache.cxf.transport.http.HttpDestinationFactory;
import org.apache.cxf.transport.websocket.ahc.AhcWebSocketConduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

@NoJSR250Annotations
public class WebSocketTransportFactory extends AbstractTransportFactory implements ConduitInitiator,
    DestinationFactory {

    public static final List<String> DEFAULT_NAMESPACES
        = Collections.unmodifiableList(Arrays.asList(
            "http://cxf.apache.org/transports/websocket",
            "http://cxf.apache.org/transports/websocket/configuration"
        ));

    private static final Set<String> URI_PREFIXES = new HashSet<>();
    static {
        URI_PREFIXES.add("ws://");
        URI_PREFIXES.add("wss://");
    }

    protected final DestinationRegistry registry;

    protected final HttpDestinationFactory factory = new WebSocketDestinationFactory();

    public WebSocketTransportFactory() {
        this(new DestinationRegistryImpl());
    }

    public WebSocketTransportFactory(DestinationRegistry registry) {
        super(DEFAULT_NAMESPACES);
        if (registry == null) {
            registry = new DestinationRegistryImpl();
        }
        this.registry = registry;
    }
    public DestinationRegistry getRegistry() {
        return registry;
    }

    /**
     * This call uses the Configurer from the bus to configure
     * a bean.
     *
     * @param bean
     */
    protected void configure(Bus b, Object bean) {
        configure(b, bean, null, null);
    }

    protected void configure(Bus bus, Object bean, String name, String extraName) {
        Configurer configurer = bus.getExtension(Configurer.class);
        if (null != configurer) {
            configurer.configureBean(name, bean);
            if (extraName != null) {
                configurer.configureBean(extraName, bean);
            }
        }
    }


    public Conduit getConduit(EndpointInfo endpointInfo, Bus b) throws IOException {
        return getConduit(endpointInfo, endpointInfo.getTarget(), b);
    }

    /**
     * {@inheritDoc}
     */
    public Conduit getConduit(EndpointInfo endpointInfo, EndpointReferenceType target, Bus bus)
        throws IOException {
        HTTPConduit conduit = new AhcWebSocketConduit(bus, endpointInfo, target);

        String address = conduit.getAddress();
        if (address != null && address.indexOf('?') != -1) {
            address = address.substring(0, address.indexOf('?'));
        }
        HTTPConduitConfigurer c1 = bus.getExtension(HTTPConduitConfigurer.class);
        if (c1 != null) {
            c1.configure(conduit.getBeanName(), address, conduit);
        }
        configure(bus, conduit, conduit.getBeanName(), address);
        conduit.finalizeConfig();
        return conduit;
    }

    /**
     * {@inheritDoc}
     */
    public Destination getDestination(EndpointInfo endpointInfo, Bus bus) throws IOException {
        if (endpointInfo == null) {
            throw new IllegalArgumentException("EndpointInfo cannot be null");
        }
        synchronized (registry) {
            AbstractHTTPDestination d = registry.getDestinationForPath(endpointInfo.getAddress());
            if (d == null) {
                d = factory.createDestination(endpointInfo, bus, registry);
                if (d == null) {
                    String error = "No destination available. The CXF websocket transport needs either the "
                        + "Jetty WebSocket or Atmosphere dependencies to be available";
                    throw new IOException(error);
                }
                registry.addDestination(d);
                configure(bus, d);
                d.finalizeConfig();
            }
            return d;
        }
    }

    public Set<String> getUriPrefixes() {
        return URI_PREFIXES;
    }

}
