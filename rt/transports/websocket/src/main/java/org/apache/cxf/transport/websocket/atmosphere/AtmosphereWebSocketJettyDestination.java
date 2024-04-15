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
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http_jetty.JettyHTTPDestination;
import org.apache.cxf.transport.http_jetty.JettyHTTPHandler;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;
import org.apache.cxf.transport.websocket.WebSocketDestinationService;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereRequestImpl;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResponseImpl;
import org.atmosphere.handler.AbstractReflectorAtmosphereHandler;
import org.atmosphere.util.VoidServletConfig;
import org.springframework.util.ClassUtils;


/**
 *
 */
public class AtmosphereWebSocketJettyDestination extends JettyHTTPDestination implements
    WebSocketDestinationService {
    private static final Logger LOG = LogUtils.getL7dLogger(AtmosphereWebSocketJettyDestination.class);
    private AtmosphereFramework framework;
    private final Map<String, String> initParams = new HashMap<>();

    public AtmosphereWebSocketJettyDestination(Bus bus, DestinationRegistry registry, EndpointInfo ei,
                                     JettyHTTPServerEngineFactory serverEngineFactory)
        throws IOException {
        super(bus, registry, ei, 
              serverEngineFactory == null ? null : new URL(getNonWSAddress(ei)),
              serverEngineFactory);

        framework = new AtmosphereFramework(false, true);
        framework.setUseNativeImplementation(false);
        addInitParameter(framework, initParams, ApplicationConfig.PROPERTY_NATIVE_COMETSUPPORT, "true");
        addInitParameter(framework, initParams, ApplicationConfig.PROPERTY_SESSION_SUPPORT, "true");
        addInitParameter(framework, initParams, ApplicationConfig.WEBSOCKET_SUPPORT, "true");
        addInitParameter(framework, initParams, ApplicationConfig.WEBSOCKET_PROTOCOL_EXECUTION, "true");
        // workaround for atmosphere's jsr356 initialization requiring servletConfig
        addInitParameter(framework, initParams, ApplicationConfig.WEBSOCKET_SUPPRESS_JSR356, "true");
        AtmosphereUtils.addInterceptors(framework, bus);
        framework.addAtmosphereHandler("/", new DestinationHandler());        
    }
    
    protected void activate() {
        super.activate();

        if (handler.getServletContext().getAttribute("org.eclipse.jetty.util.DecoratedObjectFactory") == null) {
            try {
                Class<?> dcc = ClassUtils.forName("org.eclipse.jetty.util.DecoratedObjectFactory", 
                                                  getClass().getClassLoader());
                handler.getServletContext().setAttribute("org.eclipse.jetty.util.DecoratedObjectFactory",
                                                         dcc.getDeclaredConstructor().newInstance());
            } catch (ClassNotFoundException | LinkageError | InstantiationException | IllegalAccessException 
                | IllegalArgumentException | InvocationTargetException | NoSuchMethodException 
                | SecurityException e) {
                //ignore, old version of Jetty
            }            
        }
        ServletConfig config = new VoidServletConfig(initParams) {
            @Override
            public ServletContext getServletContext() {
                return handler.getServletContext();
            }
        };

        try {
            framework.init(config);
        } catch (ServletException e) {
            throw new Fault(new Message("Could not initialize WebSocket Framework", LOG, e.getMessage()), e);
        }
    }
    
    private static void addInitParameter(AtmosphereFramework fw, Map<String, String> initParams,
                                  String name, String value) {
        fw.addInitParameter(name,  value);
        initParams.put(name,  value);
    }
    @Override
    public void invokeInternal(ServletConfig config, ServletContext context, HttpServletRequest req,
                               HttpServletResponse resp) throws IOException {
        super.invoke(config, context, req, resp);
    }
    
    private static String getNonWSAddress(EndpointInfo endpointInfo) {
        String address = endpointInfo.getAddress();
        if (address.startsWith("ws")) {
            address = "http" + address.substring(2);
        }
        return address;
    }

    @Override
    protected String getAddress(EndpointInfo endpointInfo) {
        return getNonWSAddress(endpointInfo);
    }


    @Override
    protected String getBasePath(String contextPath) throws IOException {
        if (StringUtils.isEmpty(endpointInfo.getAddress())) {
            return "";
        }
        return new URL(getAddress(endpointInfo)).getPath();
    }

    @Override
    protected JettyHTTPHandler createJettyHTTPHandler(JettyHTTPDestination jhd, boolean cmExact) {
        return new AtmosphereJettyWebSocketHandler(jhd, cmExact);
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

    private class AtmosphereJettyWebSocketHandler extends JettyHTTPHandler {
        AtmosphereJettyWebSocketHandler(JettyHTTPDestination jhd, boolean cmExact) {
            super(jhd, cmExact);
        }

        
        @Override
        protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
            if (AtmosphereUtils.useAtmosphere(request)) {
                try {
                    framework.doCometSupport(AtmosphereRequestImpl.wrap(request),
                                             AtmosphereResponseImpl.wrap(response));

                } catch (ServletException e) {
                    throw new IOException(e);
                }
                return;
            }
            super.service(request, response);
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
