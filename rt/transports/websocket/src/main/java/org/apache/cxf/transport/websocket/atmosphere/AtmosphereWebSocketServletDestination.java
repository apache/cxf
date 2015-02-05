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
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.servlet.ServletDestination;
import org.apache.cxf.transport.websocket.WebSocketDestinationService;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereInterceptor;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.handler.AbstractReflectorAtmosphereHandler;
import org.atmosphere.util.Utils;

/**
 * 
 */
public class AtmosphereWebSocketServletDestination extends ServletDestination implements
    WebSocketDestinationService {
    private static final Logger LOG = LogUtils.getL7dLogger(AtmosphereWebSocketServletDestination.class);

    private AtmosphereFramework framework;
    private Executor executor;

    public AtmosphereWebSocketServletDestination(Bus bus, DestinationRegistry registry, EndpointInfo ei, 
                                                 String path) throws IOException {
        super(bus, registry, ei, path);
        this.framework = new AtmosphereFramework(false, true);
        framework.setUseNativeImplementation(false);
        framework.addInitParameter(ApplicationConfig.PROPERTY_NATIVE_COMETSUPPORT, "true");
        framework.addInitParameter(ApplicationConfig.PROPERTY_SESSION_SUPPORT, "true");
        framework.addInitParameter(ApplicationConfig.WEBSOCKET_SUPPORT, "true");
        framework.interceptor(getInterceptor(bus));
        framework.addAtmosphereHandler("/", new DestinationHandler());
        framework.init();

        // the executor for decoupling the service invocation from websocket's onMessage call which is
        // synchronously blocked
        executor = bus.getExtension(WorkQueueManager.class).getAutomaticWorkQueue();
    }

    @Override
    public void invoke(ServletConfig config, ServletContext context, HttpServletRequest req,
                       HttpServletResponse resp) throws IOException {
        if (Utils.webSocketEnabled(req)) {
            try {
                framework.doCometSupport(AtmosphereRequest.wrap(req), 
                                         AtmosphereResponse.wrap(resp));
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

    Executor getExecutor() {
        return executor;
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

    //FIXME a temporary workaround until we decide how to customize atmosphere using cxf's destination configuration
    private AtmosphereInterceptor getInterceptor(Bus bus) {
        AtmosphereInterceptor ai = (AtmosphereInterceptor)bus.getProperty("atmosphere.interceptor");
        if (ai == null) {
            ai = new DefaultProtocolInterceptor(); 
        }
        LOG.info("AtmosphereInterceptor: " + ai);
        return ai;
    }
}
