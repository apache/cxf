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

package org.apache.cxf.transport.udp;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractTransportFactory;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

@NoJSR250Annotations
public class UDPTransportFactory extends AbstractTransportFactory
    implements DestinationFactory, ConduitInitiator {

    public static final String TRANSPORT_ID = "http://cxf.apache.org/transports/udp";
    public static final List<String> DEFAULT_NAMESPACES = Collections.singletonList(TRANSPORT_ID);

    private static final Logger LOG = LogUtils.getL7dLogger(UDPTransportFactory.class);
    private static final Set<String> URI_PREFIXES = Collections.singleton("udp://");

    private Set<String> uriPrefixes = new HashSet<>(URI_PREFIXES);

    public UDPTransportFactory() {
        super(DEFAULT_NAMESPACES);
    }

    public Destination getDestination(EndpointInfo ei, Bus bus) throws IOException {
        return getDestination(ei, null, bus);
    }

    protected Destination getDestination(EndpointInfo ei,
                                         EndpointReferenceType reference,
                                         Bus bus)
        throws IOException {
        if (reference == null) {
            reference = createReference(ei);
        }
        return new UDPDestination(bus, reference, ei);
    }


    public Conduit getConduit(EndpointInfo ei, Bus bus) throws IOException {
        return getConduit(ei, null, bus);
    }

    public Conduit getConduit(EndpointInfo ei, EndpointReferenceType target, Bus bus) throws IOException {
        LOG.log(Level.FINE, "Creating conduit for {0}", ei.getAddress());
        if (target == null) {
            target = createReference(ei);
        }
        return new UDPConduit(target, bus);
    }


    public Set<String> getUriPrefixes() {
        return uriPrefixes;
    }
    public void setUriPrefixes(Set<String> s) {
        uriPrefixes = s;
    }
    static EndpointReferenceType createReference(EndpointInfo ei) {
        EndpointReferenceType epr = new EndpointReferenceType();
        AttributedURIType address = new AttributedURIType();
        address.setValue(ei.getAddress());
        epr.setAddress(address);
        return epr;
    }

}
