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
package org.apache.cxf.transport.sse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.sse.atmosphere.AtmosphereSseServletDestination;

@NoJSR250Annotations
public class SseHttpTransportFactory extends HTTPTransportFactory  
        implements ConduitInitiator, DestinationFactory {
    
    public static final String TRANSPORT_ID = "http://cxf.apache.org/transports/http/sse";
    public static final List<String> DEFAULT_NAMESPACES = Arrays.asList(
        TRANSPORT_ID,
        "http://cxf.apache.org/transports/http/sse/configuration"
    );
    
    public SseHttpTransportFactory() {
        this(null);
    }
    
    public SseHttpTransportFactory(DestinationRegistry registry) {
        super(DEFAULT_NAMESPACES, registry);
    }
    
    @Override
    public Destination getDestination(EndpointInfo endpointInfo, Bus bus) throws IOException {
        return new AtmosphereSseServletDestination(bus, getRegistry(), endpointInfo, endpointInfo.getAddress());
    }
}