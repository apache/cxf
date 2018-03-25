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
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.http.HttpDestinationFactory;
import org.apache.cxf.transport.sse.atmosphere.AtmosphereSseServletDestination;

public class SseDestinationFactory implements HttpDestinationFactory {
    private static final Logger LOG = LogUtils.getL7dLogger(SseDestinationFactory.class);
    
    @Override
    public AbstractHTTPDestination createDestination(EndpointInfo endpointInfo, Bus bus, 
            DestinationRegistry registry) throws IOException {
        return new AtmosphereSseServletDestination(bus, getDestinationRegistry(bus), endpointInfo, 
            endpointInfo.getAddress());
    } 
    
    /**
     * The destination factory should be taken from HTTP transport as a workaround to 
     * register the destinations properly in the OSGi container.
     */
    private static DestinationRegistry getDestinationRegistry(Bus bus) {
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        
        try {
            DestinationFactory df = dfm.getDestinationFactory("http://cxf.apache.org/transports/http/configuration");
            if (df instanceof HTTPTransportFactory) {
                HTTPTransportFactory transportFactory = (HTTPTransportFactory)df;
                return transportFactory.getRegistry();
            }
        } catch (final Exception ex) {
            LOG.warning("Unable to deduct the destination registry from HTTP transport: " + ex.getMessage());
        }
        
        return null;
    }


}
