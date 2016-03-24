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

package org.apache.cxf.transport.websocket.atmosphere;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.servlet.ServletDestination;
import org.apache.cxf.transport.websocket.WebSocketDestinationService;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereRequestImpl;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResponseImpl;
import org.atmosphere.handler.AbstractReflectorAtmosphereHandler;

/**
 * 
 */
public class AtmosphereWebSocketServletDestination extends ServletDestination implements
    WebSocketDestinationService {
    private static final Logger LOG = LogUtils.getL7dLogger(AtmosphereWebSocketServletDestination.class);

    private AtmosphereFramework framework;

    public AtmosphereWebSocketServletDestination(Bus bus, DestinationRegistry registry, EndpointInfo ei, 
                                                 String path) throws IOException {
        super(bus, registry, ei, path);
        framework = new AtmosphereFramework(false, true);
        framework.setUseNativeImplementation(false);
        framework.addInitParameter(ApplicationConfig.PROPERTY_NATIVE_COMETSUPPORT, "true");
        framework.addInitParameter(ApplicationConfig.PROPERTY_SESSION_SUPPORT, "true");
        framework.addInitParameter(ApplicationConfig.WEBSOCKET_SUPPORT, "true");
        framework.addInitParameter(ApplicationConfig.WEBSOCKET_PROTOCOL_EXECUTION, "true");
        // workaround for atmosphere's jsr356 initialization requiring servletConfig
        framework.addInitParameter(ApplicationConfig.WEBSOCKET_SUPPRESS_JSR356, "true");
        AtmosphereUtils.addInterceptors(framework, bus);
        framework.addAtmosphereHandler("/", new DestinationHandler());
        framework.init();
    }

    @Override
    public void invoke(ServletConfig config, ServletContext context, HttpServletRequest req,
                       HttpServletResponse resp) throws IOException {
        if (AtmosphereUtils.useAtmosphere(req)) {
            try {
                framework.doCometSupport(AtmosphereRequestImpl.wrap(req), 
                                         AtmosphereResponseImpl.wrap(resp));
            } catch (ServletException e) {
                throw new IOException(e);
            }
            return;
        }
        super.invoke(config, context, req, resp);
    }

    @Override
    public void invokeInternal(ServletConfig config, ServletContext context, HttpServletRequest req,
                               HttpServletResponse resp) throws IOException {
        super.invoke(config, context, req, resp);
    }

    @Override
    public void shutdown() {
        try {
            framework.destroy();
        } catch (Exception e) {
            // ignore
        } finally {
            super.shutdown();
        }
    }

    private class DestinationHandler extends AbstractReflectorAtmosphereHandler {

        @Override
        public void onRequest(final AtmosphereResource resource) throws IOException {
            LOG.fine("onRequest");
            try {
                invokeInternal(null, 
                    resource.getRequest().getServletContext(), resource.getRequest(), resource.getResponse());
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to invoke service", e);
            }
        }
    }
    
    // used for internal tests
    AtmosphereFramework getAtmosphereFramework() {
        return framework;
    }
}
