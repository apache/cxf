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
package org.apache.cxf.jaxrs.sse.atmosphere;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.sse.SseContext;

import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;

@Provider
public class SseAtmosphereContextProvider implements ContextProvider<SseContext> {
    @Override
    public SseContext createContext(Message message) {
        final HttpServletRequest request = (HttpServletRequest)message.get(AbstractHTTPDestination.HTTP_REQUEST);
        if (request == null) {
            throw new IllegalStateException("Unable to retrieve HTTP request from the context");
        }
        
        final AtmosphereResource resource = (AtmosphereResource)request
            .getAttribute(AtmosphereResource.class.getName());
        if (resource == null) {
            throw new IllegalStateException("AtmosphereResource is not present, "
                    + "is AtmosphereServlet configured properly?");
        }
        
        final Broadcaster broadcaster = resource.getAtmosphereConfig()
            .getBroadcasterFactory()
            .lookup(resource.uuid(), true);
        
        resource.removeFromAllBroadcasters();
        resource.setBroadcaster(broadcaster);
        
        return new SseAtmosphereResourceContext(ServerProviderFactory.getInstance(message), resource);
    }
} 
