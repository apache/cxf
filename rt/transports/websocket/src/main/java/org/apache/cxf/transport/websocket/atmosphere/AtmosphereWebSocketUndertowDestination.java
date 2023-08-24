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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http_undertow.UndertowHTTPDestination;
import org.apache.cxf.transport.http_undertow.UndertowHTTPHandler;
import org.apache.cxf.transport.http_undertow.UndertowHTTPServerEngineFactory;
import org.apache.cxf.transport.websocket.WebSocketConstants;
import org.apache.cxf.transport.websocket.WebSocketDestinationService;
import org.apache.cxf.transport.websocket.WebSocketUtils;
import org.apache.cxf.transport.websocket.undertow.WebSocketUndertowServletRequest;
import org.apache.cxf.transport.websocket.undertow.WebSocketUndertowServletResponse;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereRequestImpl;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResponseImpl;
import org.atmosphere.handler.AbstractReflectorAtmosphereHandler;
import org.xnio.StreamConnection;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.HttpUpgradeListener;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import io.undertow.servlet.spec.HttpServletResponseImpl;
import io.undertow.servlet.spec.ServletContextImpl;
import io.undertow.util.Methods;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.protocol.Handshake;
import io.undertow.websockets.core.protocol.version07.Hybi07Handshake;
import io.undertow.websockets.core.protocol.version08.Hybi08Handshake;
import io.undertow.websockets.core.protocol.version13.Hybi13Handshake;
import io.undertow.websockets.spi.AsyncWebSocketHttpServerExchange;

/**
 *
 */
public class AtmosphereWebSocketUndertowDestination extends UndertowHTTPDestination
    implements WebSocketDestinationService {
    private static final Logger LOG = LogUtils.getL7dLogger(AtmosphereWebSocketUndertowDestination.class);
    private final Executor executor;
    private AtmosphereFramework framework;

    public AtmosphereWebSocketUndertowDestination(Bus bus, DestinationRegistry registry, EndpointInfo ei,
                                                  UndertowHTTPServerEngineFactory serverEngineFactory)
                                                      throws IOException {
        super(bus, registry, ei, serverEngineFactory);
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
        executor = bus.getExtension(WorkQueueManager.class).getAutomaticWorkQueue();
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
    protected UndertowHTTPHandler createUndertowHTTPHandler(UndertowHTTPDestination jhd, boolean cmExact) {
        return new AtmosphereUndertowWebSocketHandler(jhd, cmExact);
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

    private class AtmosphereUndertowWebSocketHandler extends UndertowHTTPHandler {
        private final Set<Handshake> handshakes;
        private final Set<WebSocketChannel> peerConnections = Collections
            .newSetFromMap(new ConcurrentHashMap<WebSocketChannel, Boolean>());

        AtmosphereUndertowWebSocketHandler(UndertowHTTPDestination jhd, boolean cmExact) {
            super(jhd, cmExact);
            handshakes = new HashSet<>();
            handshakes.add(new Hybi13Handshake());
            handshakes.add(new Hybi08Handshake());
            handshakes.add(new Hybi07Handshake());
        }

        @Override
        public void handleRequest(HttpServerExchange undertowExchange) throws Exception {
            if (undertowExchange.isInIoThread()) {
                undertowExchange.dispatch(this);
                return;
            }
            if (!undertowExchange.getRequestMethod().equals(Methods.GET)) {
                // Only GET is supported to start the handshake
                handleNormalRequest(undertowExchange);
                return;
            }
            final AsyncWebSocketHttpServerExchange facade = new AsyncWebSocketHttpServerExchange(undertowExchange,
                                                                                                 peerConnections);
            Handshake handshaker = null;
            for (Handshake method : handshakes) {
                if (method.matches(facade)) {
                    handshaker = method;
                    break;
                }
            }

            if (handshaker == null) {
                handleNormalRequest(undertowExchange);
            } else {
                final Handshake selected = handshaker;
                undertowExchange.upgradeChannel(new HttpUpgradeListener() {
                    @Override
                    public void handleUpgrade(StreamConnection streamConnection,
                                              HttpServerExchange exchange) {
                        try {

                            WebSocketChannel channel = selected.createChannel(facade, streamConnection,
                                                                              facade.getBufferPool());
                            peerConnections.add(channel);
                            channel.getReceiveSetter().set(new AbstractReceiveListener() {
                                @Override
                                protected void onFullTextMessage(WebSocketChannel channel,
                                                                 BufferedTextMessage message) {
                                    handleReceivedMessage(channel, message, exchange);

                                }

                                protected void onFullBinaryMessage(WebSocketChannel channel,
                                                                   BufferedBinaryMessage message)
                                                                       throws IOException {

                                    handleReceivedMessage(channel, message, exchange);

                                }
                            });
                            channel.resumeReceives();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                handshaker.handshake(facade);
            }

        }

        public void handleNormalRequest(HttpServerExchange undertowExchange) throws Exception {
            HttpServletResponseImpl response = new HttpServletResponseImpl(undertowExchange,
                                                                           (ServletContextImpl)servletContext);
            HttpServletRequestImpl request = new HttpServletRequestImpl(undertowExchange,
                                                                        (ServletContextImpl)servletContext);
            ServletRequestContext servletRequestContext = new ServletRequestContext(((ServletContextImpl)servletContext)
                .getDeployment(), request, response, null);

            undertowExchange.putAttachment(ServletRequestContext.ATTACHMENT_KEY, servletRequestContext);

            try {
                framework.doCometSupport(AtmosphereRequestImpl.wrap(request),
                                         AtmosphereResponseImpl.wrap(response));

            } catch (ServletException e) {
                throw new IOException(e);
            }
        }

        public void handleNormalRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

            try {
                framework.doCometSupport(AtmosphereRequestImpl.wrap(request),
                                         AtmosphereResponseImpl.wrap(response));

            } catch (ServletException e) {
                throw new IOException(e);
            }

        }

        private void handleReceivedMessage(WebSocketChannel channel, Object message, HttpServerExchange exchange) {
            executor.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        HttpServletRequest request = new WebSocketUndertowServletRequest(channel, message, exchange);
                        HttpServletResponse response = new WebSocketUndertowServletResponse(channel);
                        if (request.getHeader(WebSocketConstants.DEFAULT_REQUEST_ID_KEY) != null) {
                            String headerValue = request.getHeader(WebSocketConstants.DEFAULT_REQUEST_ID_KEY);
                            if (WebSocketUtils.isContainingCRLF(headerValue)) {
                                LOG.warning("Invalid characters (CR/LF) in header "
                                    + WebSocketConstants.DEFAULT_REQUEST_ID_KEY);
                            } else {
                                response.setHeader(WebSocketConstants.DEFAULT_RESPONSE_ID_KEY, headerValue);
                            }
                        }
                        handleNormalRequest(request, response);
                    } catch (Exception ex) {
                        LOG.log(Level.WARNING, "Failed to invoke service", ex);
                    }

                }

            });

        }
    }

    private final class DestinationHandler extends AbstractReflectorAtmosphereHandler {

        @Override
        public void onRequest(final AtmosphereResource resource) throws IOException {
            LOG.fine("onRequest");
            try {
                invokeInternal(null, resource.getRequest().getServletContext(), resource.getRequest(),
                               resource.getResponse());
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
