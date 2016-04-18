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

import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.OutboundSseEvent.Builder;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseContext;
import javax.ws.rs.sse.SseEventOutput;

import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.sse.OutboundSseEventBodyWriter;
import org.apache.cxf.jaxrs.sse.OutboundSseEventImpl;
import org.apache.cxf.jaxrs.sse.SseBroadcasterImpl;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.atmosphere.cpr.AtmosphereResource;

public class SseAtmosphereResourceContext implements SseContext {
    private final AtmosphereResource resource;
    private final ServerProviderFactory factory;

    SseAtmosphereResourceContext(final ServerProviderFactory factory, final AtmosphereResource resource) {
        this.factory = factory;
        this.resource = resource;
    }
    
    @Override
    public SseEventOutput newOutput() {
        final MessageBodyWriter<OutboundSseEvent> writer = new OutboundSseEventBodyWriter(factory, 
            JAXRSUtils.getCurrentMessage().getExchange());
        return new SseAtmosphereEventOutputImpl(writer, resource);
    }

    @Override
    public Builder newEvent() {
        return new OutboundSseEventImpl.BuilderImpl();
    }

    @Override
    public SseBroadcaster newBroadcaster() {
        return new SseBroadcasterImpl();
    }
}
