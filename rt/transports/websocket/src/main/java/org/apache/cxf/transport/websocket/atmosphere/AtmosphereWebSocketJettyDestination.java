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
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http_jetty.JettyHTTPDestination;
import org.apache.cxf.transport.http_jetty.JettyHTTPHandler;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;
import org.apache.cxf.transport.websocket.WebSocketDestinationService;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.handler.AbstractReflectorAtmosphereHandler;
import org.atmosphere.util.Utils;
import org.eclipse.jetty.server.Request;


/**
 * 
 */
public class AtmosphereWebSocketJettyDestination extends JettyHTTPDestination implements 
    WebSocketDestinationService {
    private static final Logger LOG = LogUtils.getL7dLogger(AtmosphereWebSocketJettyDestination.class);
    private AtmosphereFramework framework;
    private Executor executor;
    
    public AtmosphereWebSocketJettyDestination(Bus bus, DestinationRegistry registry, EndpointInfo ei,
                                     JettyHTTPServerEngineFactory serverEngineFactory) throws IOException {
        super(bus, registry, ei, serverEngineFactory);
        framework = new AtmosphereFramework(false, true);
        framework.setUseNativeImplementation(false);
        framework.addInitParameter(ApplicationConfig.PROPERTY_NATIVE_COMETSUPPORT, "true");
        framework.addInitParameter(ApplicationConfig.PROPERTY_SESSION_SUPPORT, "true");
        framework.addInitParameter(ApplicationConfig.WEBSOCKET_SUPPORT, "true");
        framework.interceptor(AtmosphereUtils.getInterceptor(bus));
        framework.addAtmosphereHandler("/", new DestinationHandler());
        framework.init();

        // the executor for decoupling the service invocation from websocket's onMessage call which is
        // synchronously blocked
        executor = bus.getExtension(WorkQueueManager.class).getAutomaticWorkQueue();
    }
    
    @Override
    public void invokeInternal(ServletConfig config, ServletContext context, HttpServletRequest req,
                               HttpServletResponse resp) throws IOException {
        super.invoke(config, context, req, resp);
    }

    @Override
    protected String getAddress(EndpointInfo endpointInfo) {
        String address = endpointInfo.getAddress();
        if (address.startsWith("ws")) {
            address = "http" + address.substring(2);
        }
        return address;
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
        public AtmosphereJettyWebSocketHandler(JettyHTTPDestination jhd, boolean cmExact) {
            super(jhd, cmExact);
        }
        
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request,
                           HttpServletResponse response) throws IOException, ServletException {
            if (Utils.webSocketEnabled(request)) {
                try {
                    framework.doCometSupport(AtmosphereRequest.wrap(request), 
                                             AtmosphereResponse.wrap(response));
                    baseRequest.setHandled(true);
                } catch (ServletException e) {
                    throw new IOException(e);
                }
                return;
            } else {
                super.handle(target, baseRequest, request, response);
            }
        }
    }

    private class DestinationHandler extends AbstractReflectorAtmosphereHandler {

        @Override
        public void onRequest(final AtmosphereResource resource) throws IOException {
            LOG.fine("onRequest");
            executeHandlerTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        invokeInternal(null, 
                            resource.getRequest().getServletContext(), resource.getRequest(), resource.getResponse());
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Failed to invoke service", e);
                    }
                }
            });
        }
    }
    
    private void executeHandlerTask(Runnable r) {
        try {
            executor.execute(r);
        } catch (RejectedExecutionException e) {
            LOG.warning(
                "Executor queue is full, run the service invocation task in caller thread." 
                + "  Users can specify a larger executor queue to avoid this.");
            r.run();
        }
    }
}
