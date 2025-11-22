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

package org.apache.cxf.transport.websocket.jetty12;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http_jetty.JettyHTTPDestination;
import org.apache.cxf.transport.http_jetty.JettyHTTPHandler;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;
import org.apache.cxf.transport.websocket.InvalidPathException;
import org.apache.cxf.transport.websocket.WebSocketConstants;
import org.apache.cxf.transport.websocket.WebSocketDestinationService;
import org.apache.cxf.transport.websocket.WebSocketUtils;
import org.apache.cxf.transport.websocket.jetty.WebSocketServletHolder;
import org.apache.cxf.transport.websocket.jetty.WebSocketVirtualServletRequest;
import org.apache.cxf.transport.websocket.jetty.WebSocketVirtualServletResponse;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.eclipse.jetty.ee11.servlet.ServletApiRequest;
import org.eclipse.jetty.ee11.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.ee11.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.ee11.websocket.server.JettyWebSocketCreator;
import org.eclipse.jetty.ee11.websocket.server.JettyWebSocketServerContainer;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.server.internal.CompletedUpgradeRequest;


/**
 *
 */
public class Jetty12WebSocketDestination extends JettyHTTPDestination implements
    WebSocketDestinationService {
    private static final Logger LOG = LogUtils.getL7dLogger(Jetty12WebSocketDestination.class);

    //REVISIT make these keys configurable
    private static final String REQUEST_ID_KEY = WebSocketConstants.DEFAULT_REQUEST_ID_KEY;
    private static final String RESPONSE_ID_KEY = WebSocketConstants.DEFAULT_RESPONSE_ID_KEY;

    private final Executor executor;
    private JettyWebSocketServerContainer webSocketServerContainer;
    private Object address;

    public Jetty12WebSocketDestination(Bus bus, DestinationRegistry registry, EndpointInfo ei,
                                     JettyHTTPServerEngineFactory serverEngineFactory) throws IOException {
        super(bus, registry, ei,
              serverEngineFactory == null ? null : new URL(getNonWSAddress(ei)),
              serverEngineFactory);
        executor = bus.getExtension(WorkQueueManager.class).getAutomaticWorkQueue();
    }

    @Override
    public void invokeInternal(ServletConfig config, ServletContext context, HttpServletRequest req,
                               HttpServletResponse resp) throws IOException {
        super.invoke(config, context, req, resp);
    }
    public void invoke(final ServletConfig config,
                       final ServletContext context,
                       final HttpServletRequest request,
                       final HttpServletResponse response) throws IOException {

        JettyWebSocketServerContainer wssc = getWebSocketContainer(context);
        JettyWebSocketCreator creator = getCreator();
        address = request.getAttribute("org.apache.cxf.transport.endpoint.address");
        try {
            if (wssc.upgrade(creator, request, response)) {
                return;
            }
        } catch (Exception ex) {
            //do nothing
        }
        if (address != null) {
            request.setAttribute("org.apache.cxf.transport.endpoint.address", address);
        }
        super.invoke(config, context, request, response);
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

   
    
    public JettyWebSocketCreator getCreator() {
        return new Creator();
    }

    public synchronized JettyWebSocketServerContainer getWebSocketContainer(ServletContext context) {
        if (webSocketServerContainer == null) {
            webSocketServerContainer = JettyWebSocketServerContainer.getContainer(context);
            if (webSocketServerContainer == null) {
                webSocketServerContainer = JettyWebSocketServerContainer.ensureContainer(context);
            }
            return webSocketServerContainer;
        }
        return webSocketServerContainer;
    }

    @Override
    protected JettyHTTPHandler createJettyHTTPHandler(JettyHTTPDestination jhd, boolean cmExact) {
        return new JettyWebSocketHandler(jhd, cmExact, this);
    }
    
    /**
     * Activate receipt of incoming messages.
     */
    protected void activate() {
        synchronized (this) {
            if (registry != null) {
                registry.addDestination(this);
            }
        }
        LOG.log(Level.FINE, "Activating receipt of incoming messages");
        // pick the handler supporting websocket if jetty-websocket is available otherwise pick the default handler.

        if (engine != null) {
            handler = createJettyHTTPHandler(this, contextMatchOnExact());
            engine.addServant(nurl, handler);
            ((JettyWebSocketHandler)handler).initHandler(engine.getServer());
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    private void invoke(final ServletApiRequest req, final byte[] data, final int offset, 
            final int length, final Session session) {
        // invoke the service asynchronously as the jetty websocket's onMessage is synchronously blocked
        // make sure the byte array passed to this method is immutable, as the websocket framework
        // may corrupt the byte array after this method is returned (i.e., before the data is returned in
        // the executor's thread.
        executeServiceTask(new Runnable() {
            @Override
            public void run() {
                HttpServletResponse response = null;
                try {
                    WebSocketServletHolder holder = new Jetty12WebSocketHolder(session);
                    response = createServletResponse(holder);
                    HttpServletRequest request = createServletRequest(req, data, offset, length, holder, session);
                    String reqid = request.getHeader(REQUEST_ID_KEY);
                    if (reqid != null) {
                        if (WebSocketUtils.isContainingCRLF(reqid)) {
                            LOG.warning("Invalid characters (CR/LF) in header " + REQUEST_ID_KEY);
                        } else {
                            response.setHeader(RESPONSE_ID_KEY, reqid);
                        }
                    }
                    invoke(null, null, request, response);
                } catch (InvalidPathException ex) {
                    reportErrorStatus(session, 400, response);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to invoke service", e);
                    reportErrorStatus(session, 500, response);
                }
            }

        });
    }

    private void executeServiceTask(Runnable r) {
        try {
            executor.execute(r);
        } catch (RejectedExecutionException e) {
            LOG.warning(
                "Executor queue is full, run the service invocation task in caller thread."
                + "  Users can specify a larger executor queue to avoid this.");
            r.run();
        }
    }

    private void reportErrorStatus(Session session, int i, HttpServletResponse resp) {
        try {
            resp.sendError(i);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private WebSocketVirtualServletRequest createServletRequest(ServletApiRequest req, byte[] data, 
            int offset, int length, WebSocketServletHolder holder, Session session) throws IOException {
        return new WebSocketVirtualServletRequest(req, holder, new ByteArrayInputStream(data, offset, length), session);
    }

    private WebSocketVirtualServletResponse createServletResponse(WebSocketServletHolder holder) throws IOException {
        return new WebSocketVirtualServletResponse(holder);
    }

    
    private final class Creator implements JettyWebSocketCreator {

        @Override
        public Object createWebSocket(JettyServerUpgradeRequest req, JettyServerUpgradeResponse resp) {
            return new Jetty12WebSocket(req);
        }

    }

    class Jetty12WebSocketHolder implements WebSocketServletHolder {
        final Session session;
        Jetty12WebSocketHolder(Session s) {
            session = s;
        }
        public String getAuthType() {
            return null;
        }
        public String getContextPath() {
            return getContextPath(session.getUpgradeRequest());
        }
        public String getLocalAddr() {
            return null;
        }
        public String getLocalName() {
            return null;
        }
        public int getLocalPort() {
            return 0;
        }
        public Locale getLocale() {
            return null;
        }
        public Enumeration<Locale> getLocales() {
            return null;
        }
        public String getProtocol() {
            return null;
        }
        public String getRemoteAddr() {
            return null;
        }
        public String getRemoteHost() {
            return null;
        }
        public int getRemotePort() {
            return 0;
        }
        public String getRequestURI() {
            return session.getUpgradeRequest().getRequestURI().getPath();
        }
        public StringBuffer getRequestURL() {
            return new StringBuffer(session.getUpgradeRequest().getRequestURI().toString());
        }
        public DispatcherType getDispatcherType() {
            return null;
        }
        public boolean isSecure() {
            return false;
        }
        public String getPathInfo() {
            return session.getUpgradeRequest().getRequestURI().getPath();
        }
        public String getPathTranslated() {
            return session.getUpgradeRequest().getRequestURI().getPath();
        }
        public String getScheme() {
            return "ws";
        }
        public String getServerName() {
            return null;
        }
        public String getServletPath() {
            return "";
        }
        public ServletContext getServletContext() {
            return null;
        }
        public int getServerPort() {
            return ((InetSocketAddress)session.getLocalSocketAddress()).getPort();
        }
        public Principal getUserPrincipal() {
            return null;
        }
        public Object getAttribute(String name) {
            try {
                final UpgradeRequest upgradeRequest = session.getUpgradeRequest();
                return getAttribute(upgradeRequest, name);
            } catch (Exception ex) {
                if (name.equals("org.apache.cxf.transport.endpoint.address")) {
                    return address;
                } else {
                    return null;
                }
            }
        }
        @Override
        public void write(byte[] data, int offset, int length) throws IOException {
            session.sendBinary(ByteBuffer.wrap(data,  offset, length), null);
        }
        
        private Object getAttribute(final UpgradeRequest upgradeRequest, final String name) {
            if (upgradeRequest instanceof JettyServerUpgradeRequest r) {
                return r.getHttpServletRequest().getAttribute(name);
            } else if (upgradeRequest instanceof CompletedUpgradeRequest r) {
                if (name.equals("org.apache.cxf.transport.endpoint.address")) {
                    return address;
                } else {
                    return null; /* no request attributes */
                }
            } else {
                throw new IllegalStateException("Unsupported upgrade request class: " + upgradeRequest.getClass());
            }
        }

        private String getContextPath(final UpgradeRequest upgradeRequest) {
            if (upgradeRequest instanceof JettyServerUpgradeRequest r) {
                return r.getHttpServletRequest().getContextPath();
            } else if (upgradeRequest instanceof CompletedUpgradeRequest r) {
                return r.getRequestURI().getPath();
            } else {
                throw new IllegalStateException("Unsupported upgrade request class: " + upgradeRequest.getClass());
            }
        }
    }

    @org.eclipse.jetty.websocket.api.annotations.WebSocket
    public class Jetty12WebSocket {
        volatile Session session;
        private final ServletApiRequest req;

        public Jetty12WebSocket(JettyServerUpgradeRequest req) {
            this.req = (ServletApiRequest) req.getServletAttributes()
                .get(org.eclipse.jetty.websocket.core.WebSocketConstants.WEBSOCKET_WRAPPED_REQUEST_ATTRIBUTE);
        }

        @org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen
        public void onOpen(Session sess) {
            this.session = sess;
        }

        @org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
        public void onMessage(String message) {
            try {
                byte[] bdata = message.getBytes("utf-8");
                onBinaryMessage(ByteBuffer.wrap(bdata), null);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        
        @org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
        public void onBinaryMessage(ByteBuffer message, Callback callback) {
            byte[] payload = new byte[message.remaining()];
            message.get(payload);
            invoke(req, payload, 0, payload.length, session);
        }


        @org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
        public void onClose(int code, String message) {
            // members.remove(this);
        }
    }
}
