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
package org.apache.cxf.transport.http.asyncclient.hc5;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractTransportFactory;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * The transport factory is the same as for Apache HttpClient 4.x, sharing the same namespaces and 
 * URIs. 
 */
public class AsyncHttpTransportFactory extends AbstractTransportFactory implements ConduitInitiator {

    public static final List<String> DEFAULT_NAMESPACES = Collections.unmodifiableList(Arrays
        .asList("http://cxf.apache.org/transports/http/http-client"));

    /**
     * This constant holds the prefixes served by this factory.
     */
    private static final Set<String> URI_PREFIXES = new HashSet<>();

    static {
        URI_PREFIXES.add("hc://");
        URI_PREFIXES.add("hc5://");
    }

    private AsyncHTTPConduitFactory factory = new AsyncHTTPConduitFactory();

    public AsyncHttpTransportFactory() {
        super(DEFAULT_NAMESPACES);
    }

    public void setAsyncHTTPConduitFactory(AsyncHTTPConduitFactory f) {
        factory = f;
    }

    /**
     * This call is used by CXF ExtensionManager to inject the activationNamespaces
     * @param ans The transport ids.
     */
    public void setActivationNamespaces(Collection<String> ans) {
        setTransportIds(new ArrayList<>(ans));
    }

    public Set<String> getUriPrefixes() {
        return URI_PREFIXES;
    }

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

    protected String getAddress(EndpointInfo endpointInfo) {
        String address = endpointInfo.getAddress();
        if (address.startsWith("hc://")) {
            address = address.substring(5);
        } else if (address.startsWith("hc5://")) {
            address = address.substring(6);
        }
        return address;
    }

    @Override
    public Conduit getConduit(EndpointInfo endpointInfo, Bus bus) throws IOException {
        return getConduit(endpointInfo, endpointInfo.getTarget(), bus);
    }

    @Override
    public Conduit getConduit(EndpointInfo endpointInfo, EndpointReferenceType target, Bus bus)
            throws IOException {

        // need to updated the endpointInfo
        endpointInfo.setAddress(getAddress(endpointInfo));
        
        AsyncHTTPConduitFactory fact = bus.getExtension(AsyncHTTPConduitFactory.class);
        if (fact == null) {
            fact = factory;
        }
        HTTPConduit conduit = fact.createConduit(bus, endpointInfo, target);

        // Spring configure the conduit.
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
}
