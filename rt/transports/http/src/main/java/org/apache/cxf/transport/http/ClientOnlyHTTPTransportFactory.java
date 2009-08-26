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
package org.apache.cxf.transport.http;

import java.io.IOException;

import javax.annotation.Resource;


import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public class ClientOnlyHTTPTransportFactory extends AbstractHTTPTransportFactory
    implements ConduitInitiator {

    public ClientOnlyHTTPTransportFactory() {
    }
    
    @Resource 
    public void setBus(Bus b) {
        super.setBus(b);
    }

    /**
     * This call creates a new HTTPConduit for the endpoint. It is equivalent
     * to calling getConduit without an EndpointReferenceType.
     */
    public Conduit getConduit(EndpointInfo endpointInfo) throws IOException {
        return getConduit(endpointInfo, endpointInfo.getTarget());
    }

    /**
     * This call creates a new HTTP Conduit based on the EndpointInfo and
     * EndpointReferenceType.
     * TODO: What are the formal constraints on EndpointInfo and 
     * EndpointReferenceType values?
     */
    public Conduit getConduit(
            EndpointInfo endpointInfo,
            EndpointReferenceType target
    ) throws IOException {
        HTTPConduit conduit = target == null
            ? new HTTPConduit(bus, endpointInfo)
            : new HTTPConduit(bus, endpointInfo, target);
        // Spring configure the conduit.  
        String address = conduit.getAddress();
        if (address != null && address.indexOf('?') != -1) {
            address = address.substring(0, address.indexOf('?'));
        }
        configure(conduit, conduit.getBeanName(), address);
        conduit.finalizeConfig();
        return conduit;
    }
    
}
