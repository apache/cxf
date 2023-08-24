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
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
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
 * WebSocket Servlet Destination based on Atmosphere
 */
public class AtmosphereWebSocketServletDestination extends ServletDestination implements
    WebSocketDestinationService {
    private static final Logger LOG = LogUtils.getL7dLogger(AtmosphereWebSocketServletDestination.class);

    private AtmosphereFramework framework;

    public AtmosphereWebSocketServletDestination(Bus bus, DestinationRegistry registry, EndpointInfo ei,
                                                 String path) throws IOException {
        super(bus, registry, ei, path);
        framework = create(bus);
    }

    private AtmosphereFramework create(Bus bus) {
        final AtmosphereFramework instance = new AtmosphereFramework(false, true);
        
        instance.setUseNativeImplementation(false);
        instance.addInitParameter(ApplicationConfig.PROPERTY_NATIVE_COMETSUPPORT, "true");
        instance.addInitParameter(ApplicationConfig.PROPERTY_SESSION_SUPPORT, "true");
        instance.addInitParameter(ApplicationConfig.WEBSOCKET_SUPPORT, "true");
        instance.addInitParameter(ApplicationConfig.WEBSOCKET_PROTOCOL_EXECUTION, "true");
        // workaround for atmosphere's jsr356 initialization requiring servletConfig
        instance.addInitParameter(ApplicationConfig.WEBSOCKET_SUPPRESS_JSR356, "true");
        AtmosphereUtils.addInterceptors(instance, bus);
        instance.addAtmosphereHandler("/", new DestinationHandler());
        
        return instance;
    }
    
    @Override
    public void finalizeConfig() {
        final ServletContext ctx = bus.getExtension(ServletContext.class);
        if (ctx != null) {
            try {
                framework.init(new ServletConfig() {
                    @Override
                    public String getServletName() {
                        return null;
                    }
                    @Override
                    public ServletContext getServletContext() {
                        return ctx;
                    }
                    @Override
                    public String getInitParameter(String name) {
                        return null;
                    }

                    @Override
                    public Enumeration<String> getInitParameterNames() {
                        return null;
                    }
                });
            } catch (ServletException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            framework.init();
        }
    }

    @Override
    public void onServletConfigAvailable(ServletConfig config) throws ServletException {
        // Very likely there is JSR-356 implementation available, let us reconfigure the Atmosphere framework
        // to use it since ServletConfig instance is already available.
        final Object container = config.getServletContext()
            .getAttribute("jakarta.websocket.server.ServerContainer");

        if (container != null) {
            if (framework.initialized()) {
                framework.destroy();
            }
            
            framework = create(getBus());
            framework.addInitParameter(ApplicationConfig.PROPERTY_NATIVE_COMETSUPPORT, "false");
            framework.addInitParameter(ApplicationConfig.WEBSOCKET_SUPPRESS_JSR356, "false");
            
            framework.init(config);
        }
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
    protected void setupMessage(Message inMessage, ServletConfig config, ServletContext context, 
            HttpServletRequest req, HttpServletResponse resp) throws IOException {

        super.setupMessage(inMessage, config, context, req, resp);
        
        // There are some complications with detecting a full request URL in JSR-356 spec, so
        // every WS Container has different interpretation.
        // 
        //   https://bz.apache.org/bugzilla/show_bug.cgi?id=56573
        //   https://java.net/jira/browse/WEBSOCKET_SPEC-228
        //
        // We have do manually inject the transport endpoint address, otherwise the
        // JAX-RS resources won't be found.
        final Object address = req.getAttribute("org.apache.cxf.transport.endpoint.address");
        if (address == null) {
            String basePath = (String)inMessage.get(Message.BASE_PATH);
            req.setAttribute("org.apache.cxf.transport.endpoint.address", basePath);
        }
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

    private final class DestinationHandler extends AbstractReflectorAtmosphereHandler {

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
