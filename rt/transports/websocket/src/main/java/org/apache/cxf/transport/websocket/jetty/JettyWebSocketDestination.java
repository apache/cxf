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

package org.apache.cxf.transport.websocket.jetty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
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
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http_jetty.JettyHTTPDestination;
import org.apache.cxf.transport.http_jetty.JettyHTTPHandler;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;
import org.apache.cxf.transport.websocket.InvalidPathException;
import org.apache.cxf.transport.websocket.WebSocketConstants;
import org.apache.cxf.transport.websocket.WebSocketDestinationService;

import org.apache.cxf.workqueue.WorkQueueManager;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketFactory;

/**
 * 
 */
public class JettyWebSocketDestination extends JettyHTTPDestination implements 
    WebSocketDestinationService {
    private static final Logger LOG = LogUtils.getL7dLogger(JettyWebSocketDestination.class);

    //REVISIT make these keys configurable
    private String requestIdKey = WebSocketConstants.DEFAULT_REQUEST_ID_KEY;
    private String responseIdKey = WebSocketConstants.DEFAULT_RESPONSE_ID_KEY;

    private WebSocketFactory webSocketFactory;
    private final Executor executor;

    public JettyWebSocketDestination(Bus bus, DestinationRegistry registry, EndpointInfo ei,
                                     JettyHTTPServerEngineFactory serverEngineFactory) throws IOException {
        super(bus, registry, ei, serverEngineFactory);
        webSocketFactory = new WebSocketFactory(new Acceptor(), 8192);
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
        if (webSocketFactory.acceptWebSocket(request, response)) {
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
            webSocketFactory.stop();
        } catch (Exception e) {
            // ignore
        } finally {
            super.shutdown();
        }
    }

    private class Acceptor implements WebSocketFactory.Acceptor {

        @Override
        public boolean checkOrigin(HttpServletRequest request, String protocol) {
            return true;
        }

        @Override
        public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
            return new JettyWebSocket(request, protocol);
        }
    }

    private class JettyWebSocket implements WebSocket.OnBinaryMessage, WebSocket.OnTextMessage {
        private Connection webSocketConnection;
        private WebSocketServletHolder webSocketHolder;
//        private String protocol;

        JettyWebSocket(HttpServletRequest request, String protocol) {
//            this.protocol = protocol;
            this.webSocketHolder = new JettyWebSocketServletHolder(this, request);
        }
        
        @Override
        public void onClose(int closeCode, String message) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "onClose({0}, {1})", new Object[]{closeCode, message});
            }
            this.webSocketConnection = null;
        }

        @Override
        public void onOpen(Connection connection) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "onOpen({0}))", connection);
            }
            this.webSocketConnection = connection;
        }

        @Override
        public void onMessage(String data) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "onMessage({0})", data);
            }
            try {
                //TODO may want use string directly instead of converting it to byte[]
                byte[] bdata = data.getBytes("utf-8");
                invokeService(bdata, 0, bdata.length);
            } catch (UnsupportedEncodingException e) {
                // will not happen
            }            
        }

        @Override
        public void onMessage(byte[] data, int offset, int length) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "onMessage({0}, {1}, {2})", new Object[]{data, offset, length});
            }
            final byte[] safedata = new byte[length];
            System.arraycopy(data, offset, safedata, 0, length);
            invokeService(safedata, 0, safedata.length);
        }
        
        private void invokeService(final byte[] data, final int offset, final int length) {
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
                        response = createServletResponse();
                        request = createServletRequest(data, offset, length);
                        String reqid = request.getHeader(requestIdKey);
                        if (reqid != null) {
                            response.setHeader(responseIdKey, reqid);
                        }
                        invoke(null, null, request, response);
                    } catch (InvalidPathException ex) { 
                        reportErrorStatus(response, 400);
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Failed to invoke service", e);
                        reportErrorStatus(response, 500);
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

        // may want to move this error reporting code to WebSocketServletHolder
        private void reportErrorStatus(HttpServletResponse response, int status) {
            if (response != null) {
                response.setStatus(status);
                try {
                    response.getWriter().write("\r\n");
                    response.getWriter().close();
                    response.flushBuffer();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        
        private WebSocketVirtualServletRequest createServletRequest(byte[] data, int offset, int length) 
            throws IOException {
            return new WebSocketVirtualServletRequest(webSocketHolder, new ByteArrayInputStream(data, offset, length));
        }

        private WebSocketVirtualServletResponse createServletResponse() throws IOException {
            return new WebSocketVirtualServletResponse(webSocketHolder);
        }
        
        /**
         * Writes to the underlining socket.
         * 
         * @param data
         * @param offset
         * @param length
         */
        void write(byte[] data, int offset, int length) throws IOException {
            LOG.log(Level.FINE, "write(byte[], offset, length)");
            webSocketConnection.sendMessage(data, offset, length);
        }
    }
    
    private static class JettyWebSocketServletHolder implements WebSocketServletHolder {
        private JettyWebSocket webSocket;
        private Map<String, Object> requestProperties;
        private Map<String, Object> requestAttributes;
        
        JettyWebSocketServletHolder(JettyWebSocket webSocket, HttpServletRequest request) {
            this.webSocket = webSocket;
            this.requestProperties = readProperties(request);
            this.requestAttributes = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
            // attributes that are needed for finding the operation in some cases
            Object v = request.getAttribute("org.apache.cxf.transport.endpoint.address");
            if (v != null) {
                requestAttributes.put("org.apache.cxf.transport.endpoint.address", v);
            }
        }

        @SuppressWarnings("unchecked")
        private <T> T getRequestProperty(String name, Class<T> cls) {
            return (T)requestProperties.get(name);
        }

        private Map<String, Object> readProperties(HttpServletRequest request) {
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("servletPath", request.getServletPath());
            properties.put("requestURI", request.getRequestURI());
            properties.put("requestURL", request.getRequestURL());
            properties.put("contextPath", request.getContextPath());
            properties.put("servletPath", request.getServletPath());
            properties.put("servletContext", request.getServletContext());
            properties.put("pathInfo", request.getPathInfo());
            properties.put("pathTranslated", request.getPathTranslated());
            properties.put("protocol", request.getProtocol());
            properties.put("scheme", request.getScheme());
            // some additional ones
            properties.put("localAddr", request.getLocalAddr());
            properties.put("localName", request.getLocalName());
            properties.put("localPort", request.getLocalPort());
            properties.put("locale", request.getLocale());
            properties.put("locales", request.getLocales());
            properties.put("remoteHost", request.getRemoteHost());
            properties.put("remotePort", request.getRemotePort());
            properties.put("remoteAddr", request.getRemoteAddr());
            properties.put("serverName", request.getServerName());
            properties.put("serverPort", request.getServerPort());
            properties.put("secure", request.isSecure());
            properties.put("authType", request.getAuthType());
            properties.put("dispatcherType", request.getDispatcherType());

            return properties;
        }

        @Override
        public String getAuthType() {
            return getRequestProperty("authType", String.class);
        }

        @Override
        public String getContextPath() {
            return getRequestProperty("contextPath", String.class);
        }

        @Override
        public String getLocalAddr() {
            return getRequestProperty("LocalAddr", String.class);
        }

        @Override
        public String getLocalName() {
            return getRequestProperty("localName", String.class);
        }

        @Override
        public int getLocalPort() {
            return getRequestProperty("localPort", int.class);
        }

        @Override
        public Locale getLocale() {
            return getRequestProperty("locale", Locale.class);
        }

        @Override
        public Enumeration<Locale> getLocales() {
            return CastUtils.cast(getRequestProperty("locales", Enumeration.class));
        }

        @Override
        public String getProtocol() {
            return getRequestProperty("protocol", String.class);
        }

        @Override
        public String getRemoteAddr() {
            return getRequestProperty("remoteAddr", String.class);
        }

        @Override
        public String getRemoteHost() {
            return getRequestProperty("remoteHost", String.class);
        }

        @Override
        public int getRemotePort() {
            return getRequestProperty("remotePort", int.class);
        }

        @Override
        public String getRequestURI() {
            return getRequestProperty("requestURI", String.class);
        }

        @Override
        public StringBuffer getRequestURL() {
            return getRequestProperty("requestURL", StringBuffer.class);
        }

        @Override
        public DispatcherType getDispatcherType() {
            return getRequestProperty("dispatcherType", DispatcherType.class);
        }

        @Override
        public boolean isSecure() {
            return getRequestProperty("secure", boolean.class);
        }

        @Override
        public String getPathInfo() {
            return getRequestProperty("pathInfo", String.class);
        }

        @Override
        public String getPathTranslated() {
            return getRequestProperty("pathTranslated", String.class);
        }

        @Override
        public String getScheme() {
            return getRequestProperty("scheme", String.class);
        }

        @Override
        public String getServerName() {
            return getRequestProperty("serverName", String.class);
        }

        @Override
        public String getServletPath() {
            return getRequestProperty("servletPath", String.class);
        }

        @Override
        public int getServerPort() {
            return getRequestProperty("serverPort", int.class);
        }

        @Override
        public ServletContext getServletContext() {
            return getRequestProperty("serverContext", ServletContext.class);
        }

        @Override
        public Principal getUserPrincipal() {
            return getRequestProperty("userPrincipal", Principal.class);
        }

        @Override
        public Object getAttribute(String name) {
            return requestAttributes.get(name);
        }

        @Override
        public void write(byte[] data, int offset, int length) throws IOException {
            webSocket.write(data, offset, length);
        }
    }

}
