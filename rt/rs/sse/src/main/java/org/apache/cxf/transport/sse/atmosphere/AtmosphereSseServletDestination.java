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

package org.apache.cxf.transport.sse.atmosphere;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.sse.SseFeature;
import org.apache.cxf.jaxrs.sse.atmosphere.SseAtmosphereInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.Headers;
import org.apache.cxf.transport.servlet.ServletDestination;
import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereRequestImpl;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResponseImpl;
import org.atmosphere.handler.AbstractReflectorAtmosphereHandler;

public class AtmosphereSseServletDestination extends ServletDestination {
    private static final Logger LOG = LogUtils.getL7dLogger(AtmosphereSseServletDestination.class);

    private AtmosphereFramework framework;

    public AtmosphereSseServletDestination(Bus bus, DestinationRegistry registry,
            EndpointInfo ei, String path) throws IOException {
        super(bus, registry, ei, path);

        framework = new AtmosphereFramework(true, false);
        framework.interceptor(new SseAtmosphereInterceptor());
        framework.addInitParameter(ApplicationConfig.PROPERTY_NATIVE_COMETSUPPORT, "true");
        framework.addInitParameter(ApplicationConfig.WEBSOCKET_SUPPORT, "true");
        framework.addInitParameter(ApplicationConfig.DISABLE_ATMOSPHEREINTERCEPTOR, "true");
        framework.addInitParameter(ApplicationConfig.CLOSE_STREAM_ON_CANCEL, "true");
        framework.setBroadcasterCacheClassName(UUIDBroadcasterCache.class.getName());
        framework.addAtmosphereHandler("/", new DestinationHandler());
        framework.init();

        bus.getFeatures().add(new SseFeature());
    }

    @Override
    public void invoke(ServletConfig config, ServletContext context, HttpServletRequest req,
            HttpServletResponse resp) throws IOException {
        try {
            framework.doCometSupport(AtmosphereRequestImpl.wrap(req), AtmosphereResponseImpl.wrap(resp));
        } catch (ServletException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void shutdown() {
        try {
            framework.destroy();
        } catch (Exception ex) {
            LOG.warning("Graceful shutdown was not successful: " + ex.getMessage());
        } finally {
            super.shutdown();
        }
    }

    private class DestinationHandler extends AbstractReflectorAtmosphereHandler {
        @Override
        public void onRequest(final AtmosphereResource resource) throws IOException {
            LOG.fine("onRequest");
            try {
                AtmosphereSseServletDestination.super.invoke(null, resource.getRequest().getServletContext(),
                    resource.getRequest(), resource.getResponse());
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to invoke service", e);
            }
        }
    }

    @Override
    protected OutputStream flushHeaders(Message outMessage, boolean getStream) throws IOException {
        adjustContentLength(outMessage);
        return super.flushHeaders(outMessage, getStream);
    }

    @Override
    protected OutputStream flushHeaders(Message outMessage) throws IOException {
        adjustContentLength(outMessage);
        return super.flushHeaders(outMessage);
    }

    /**
     * It has been noticed that Jetty checks the "Content-Length" header and completes the
     * response if its value is 0 (or matches the number of bytes written). However, in case
     * of SSE the content length is unknown so we are setting it to -1 before flushing the
     * response. Otherwise, only the first event is going to be sent and response is going to
     * be closed.
     */
    private void adjustContentLength(Message outMessage) {
        final String contentType = (String)outMessage.get(Message.CONTENT_TYPE);

        if (MediaType.SERVER_SENT_EVENTS.equalsIgnoreCase(contentType)) {
            final Map<String, List<String>> headers = Headers.getSetProtocolHeaders(outMessage);
            headers.put(HttpHeaders.CONTENT_LENGTH, Collections.singletonList("-1"));
        }
    }
}
