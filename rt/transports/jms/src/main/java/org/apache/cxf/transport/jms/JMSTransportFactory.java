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

package org.apache.cxf.transport.jms;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractTransportFactory;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

@NoJSR250Annotations
public class JMSTransportFactory extends AbstractTransportFactory implements ConduitInitiator,
    DestinationFactory {

    public static final List<String> DEFAULT_NAMESPACES
        = Collections.unmodifiableList(Arrays.asList(
            "http://cxf.apache.org/transports/jms",
            "http://cxf.apache.org/transports/jms/configuration"
        ));

    private static final Set<String> URI_PREFIXES = new HashSet<>();
    static {
        URI_PREFIXES.add("jms://");
        URI_PREFIXES.add("jms:");
    }


    public JMSTransportFactory() {
        super(DEFAULT_NAMESPACES);
    }

    public Conduit getConduit(EndpointInfo endpointInfo, Bus b) throws IOException {
        return getConduit(endpointInfo, endpointInfo.getTarget(), b);
    }

    /**
     * {@inheritDoc}
     */
    public Conduit getConduit(EndpointInfo endpointInfo, EndpointReferenceType target, Bus bus)
        throws IOException {
        JMSConfiguration jmsConf = JMSConfigFactory.createFromEndpointInfo(bus, endpointInfo, target);
        return new JMSConduit(target, jmsConf, bus);
    }

    /**
     * {@inheritDoc}
     */
    public Destination getDestination(EndpointInfo endpointInfo, Bus bus) throws IOException {
        JMSConfiguration jmsConf = JMSConfigFactory.createFromEndpointInfo(bus, endpointInfo, null);
        return new JMSDestination(bus, endpointInfo, jmsConf);
    }

    public Set<String> getUriPrefixes() {
        return URI_PREFIXES;
    }

}
