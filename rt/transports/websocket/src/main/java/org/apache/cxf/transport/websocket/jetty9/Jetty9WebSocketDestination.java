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

package org.apache.cxf.transport.websocket.jetty9;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.DispatcherType;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http_jetty.JettyHTTPDestination;
import org.apache.cxf.transport.http_jetty.JettyHTTPHandler;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;
import org.apache.cxf.transport.websocket.InvalidPathException;
import org.apache.cxf.transport.websocket.WebSocketConstants;
import org.apache.cxf.transport.websocket.WebSocketDestinationService;
import org.apache.cxf.transport.websocket.jetty.WebSocketServletHolder;
import org.apache.cxf.transport.websocket.jetty.WebSocketVirtualServletRequest;
import org.apache.cxf.transport.websocket.jetty.WebSocketVirtualServletResponse;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

/**
 * 
 */
public class Jetty9WebSocketDestination extends JettyHTTPDestination implements 
    WebSocketDestinationService {
    private static final Logger LOG = LogUtils.getL7dLogger(Jetty9WebSocketDestination.class);

    //REVISIT make these keys configurable
    private String requestIdKey = WebSocketConstants.DEFAULT_REQUEST_ID_KEY;
    private String responseIdKey = WebSocketConstants.DEFAULT_RESPONSE_ID_KEY;

    private WebSocketServletFactory webSocketFactory;
    private final Executor executor;

    public Jetty9WebSocketDestination(Bus bus, DestinationRegistry registry, EndpointInfo ei,
                                     JettyHTTPServerEngineFactory serverEngineFactory) throws IOException {
        super(bus, registry, ei, serverEngineFactory);
        try {
            webSocketFactory = (WebSocketServletFactory)ClassLoaderUtils
                .loadClass("org.eclipse.jetty.websocket.server.WebSocketServerFactory",
                           WebSocketServletFactory.class).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        webSocketFactory.setCreator(new Creator());
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
        if (webSocketFactory.isUpgradeRequest(request, response)
            && webSocketFactory.acceptWebSocket(request, response)) {
            ((Request)request).setHandled(true);
            return;
        }
        super.invoke(config, context, request, response);
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
    protected JettyHTTPHandler createJettyHTTPHandler(JettyHTTPDestination jhd, boolean cmExact) {
        return new JettyWebSocketHandler(jhd, cmExact, webSocketFactory);
    }
    
    @Override
    public void shutdown() {
        try {
            webSocketFactory.cleanup();
        } catch (Exception e) {
            // ignore
        } finally {
            super.shutdown();
        }
    }
    
    private void invoke(final byte[] data, final int offset, final int length, final Session session) {
        // invoke the service asynchronously as the jetty websocket's onMessage is synchronously blocked
        // make sure the byte array passed to this method is immutable, as the websocket framework
        // may corrupt the byte array after this method is returned (i.e., before the data is returned in
        // the executor's thread.
        executeServiceTask(new Runnable() {
            @Override
            public void run() {
                HttpServletRequest request = null;
                HttpServletResponse response = null;
                try {
                    WebSocketServletHolder holder = new Jetty9WebSocketHolder(session);
                    response = createServletResponse(holder);
                    request = createServletRequest(data, offset, length, holder);
                    String reqid = request.getHeader(requestIdKey);
                    if (reqid != null) {
                        response.setHeader(responseIdKey, reqid);
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
    private WebSocketVirtualServletRequest createServletRequest(byte[] data, int offset, int length,
                                                                WebSocketServletHolder holder) 
        throws IOException {
        return new WebSocketVirtualServletRequest(holder, new ByteArrayInputStream(data, offset, length));
    }

    private WebSocketVirtualServletResponse createServletResponse(WebSocketServletHolder holder) throws IOException {
        return new WebSocketVirtualServletResponse(holder);
    }

    // hide this jetty9 interface here to avoid CNFE on WebSocketCreator
    private class Creator implements WebSocketCreator {

        @Override
        public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
            return new WebSocketAdapter() {
                Session session;
                @Override
                public void onWebSocketConnect(Session session) {
                    this.session = session;
                }
                @Override
                public void onWebSocketBinary(byte[] payload, int offset, int len) {
                    invoke(payload, offset, len, session);
                }
                @Override
                public void onWebSocketText(String message) {
                    //TODO may want use string directly instead of converting it to byte[]
                    try {
                        byte[] bdata = message.getBytes("utf-8");
                        onWebSocketBinary(bdata, 0, bdata.length);
                    } catch (UnsupportedEncodingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            };
        }
        
    }
    
    class Jetty9WebSocketHolder implements WebSocketServletHolder {
        final Session session;
        public Jetty9WebSocketHolder(Session s) {
            session = s;
        }
        public String getAuthType() {
            return null;
        }
        public String getContextPath() {
            return ((ServletUpgradeRequest)session.getUpgradeRequest()).getHttpServletRequest().getContextPath();
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
            return session.getLocalAddress().getPort();
        }
        public Principal getUserPrincipal() {
            return null;
        }
        public Object getAttribute(String name) {
            return ((ServletUpgradeRequest)session.getUpgradeRequest()).getHttpServletRequest().getAttribute(name);
        }
        @Override
        public void write(byte[] data, int offset, int length) throws IOException {
            try {
                session.getRemote().sendBytesByFuture(ByteBuffer.wrap(data,  offset, length)).get();
            } catch (InterruptedException | ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
