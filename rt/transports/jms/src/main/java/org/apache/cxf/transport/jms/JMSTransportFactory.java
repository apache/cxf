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
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractTransportFactory;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public class JMSTransportFactory extends AbstractTransportFactory implements ConduitInitiator,
    DestinationFactory {

    private static final Set<String> URI_PREFIXES = new HashSet<String>();
    static {
        URI_PREFIXES.add("jms://");
    }

    private Bus bus;

    @Resource(name = "cxf")
    public void setBus(Bus b) {
        bus = b;
    }

    public Bus getBus() {
        return bus;
    }

    public Conduit getConduit(EndpointInfo endpointInfo) throws IOException {
        return getConduit(endpointInfo, endpointInfo.getTarget());
    }

    /**
     * {@inheritDoc}
     */
    public Conduit getConduit(EndpointInfo endpointInfo, EndpointReferenceType target) throws IOException {
        JMSOldConfigHolder old = new JMSOldConfigHolder();
        JMSConfiguration jmsConf = old.createJMSConfigurationFromEndpointInfo(bus, endpointInfo, true);
        return new JMSConduit(endpointInfo, target, jmsConf);
    }

    /**
     * {@inheritDoc}
     */
    public Destination getDestination(EndpointInfo endpointInfo) throws IOException {
        JMSOldConfigHolder old = new JMSOldConfigHolder();
        JMSConfiguration jmsConf = old.createJMSConfigurationFromEndpointInfo(bus, endpointInfo, false);
        return new JMSDestination(bus, endpointInfo, jmsConf);
    }
    
    public Set<String> getUriPrefixes() {
        return URI_PREFIXES;
    }

}
