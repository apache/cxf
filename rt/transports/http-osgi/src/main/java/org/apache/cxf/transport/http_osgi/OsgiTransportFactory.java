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
package org.apache.cxf.transport.http_osgi;

import java.io.IOException;
import java.net.URI;

import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.http.AbstractHTTPTransportFactory;

public class OsgiTransportFactory extends AbstractHTTPTransportFactory implements DestinationFactory {

    private OsgiDestinationRegistryIntf registry;

    public void setRegistry(OsgiDestinationRegistryIntf registry) {
        this.registry = registry;
    }

    public Destination getDestination(EndpointInfo endpointInfo) throws IOException {
        if (URI.create(endpointInfo.getAddress()).isAbsolute()) {
            throw new IllegalStateException("Endpoint address should be a relative URI "
                                             + "wrt to the servlet address (use '/xxx' for example)");
        }
        OsgiDestination d = registry.getDestinationForPath(endpointInfo.getAddress());
        if (d == null) {
            String path = OsgiDestinationRegistry.getTrimmedPath(endpointInfo.getAddress());
            d = new OsgiDestination(getBus(), endpointInfo, registry, path);
            registry.addDestination(path, d);
        }
        return d;
    }
    
    public void init() {
        
    }

}
