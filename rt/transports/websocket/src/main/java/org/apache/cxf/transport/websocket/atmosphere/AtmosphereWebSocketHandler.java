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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.transport.websocket.InvalidPathException;
import org.apache.cxf.transport.websocket.WebSocketConstants;
import org.apache.cxf.transport.websocket.WebSocketDestinationService;
import org.apache.cxf.transport.websocket.WebSocketServletHolder;
import org.apache.cxf.transport.websocket.WebSocketVirtualServletRequest;
import org.apache.cxf.transport.websocket.WebSocketVirtualServletResponse;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.websocket.WebSocket;
import org.atmosphere.websocket.WebSocketProcessor.WebSocketException;
import org.atmosphere.websocket.WebSocketProtocol;

/**
 * @deprecated No longer used as the protocol handling is done by Atmosphere's protocol intercepter
 * such as org.apache.cxf.transport.websocket.atmosphere.DefaultProtocolInterceptor.
 */
public class AtmosphereWebSocketHandler implements WebSocketProtocol {
    private static final Logger LOG = LogUtils.getL7dLogger(AtmosphereWebSocketHandler.class);

    protected AtmosphereWebSocketServletDestination destination;

    //REVISIT make these keys configurable
    private String requestIdKey = WebSocketConstants.DEFAULT_REQUEST_ID_KEY;
    private String responseIdKey = WebSocketConstants.DEFAULT_RESPONSE_ID_KEY;
    
    public AtmosphereWebSocketServletDestination getDestination() {
        return destination;
    }

    public void setDestination(AtmosphereWebSocketServletDestination destination) {
        this.destination = destination;
    }

    /** {@inheritDoc}*/
    @Override
    public void configure(AtmosphereConfig config) {
        LOG.fine("configure(AtmosphereConfig)");

    }

    /** {@inheritDoc}*/
    @Override
    public List<AtmosphereRequest> onMessage(WebSocket webSocket, String data) {
        LOG.fine("onMessage(WebSocket, String)");
        //TODO may want to use string directly instead of converting it to byte[]
        byte[] bdata = null;
        try {
            bdata = data.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            // will not happen
        }
        return invokeService(webSocket, new ByteArrayInputStream(bdata, 0, bdata.length));
    }

    /** {@inheritDoc}*/
    @Override
    public List<AtmosphereRequest> onMessage(WebSocket webSocket, byte[] data, int offset, int length) {
        final byte[] safedata = new byte[length];
        System.arraycopy(data, offset, safedata, 0, length);
        return invokeService(webSocket, new ByteArrayInputStream(safedata, 0, safedata.length));
    }
    
    protected List<AtmosphereRequest> invokeService(final WebSocket webSocket,  final InputStream stream) {
        LOG.fine("invokeService(WebSocket, InputStream)");
        // invoke the service asynchronously as onMessage is synchronously blocked (in jetty)
        // make sure the byte array passed to this method is immutable, as the websocket framework
        // may corrupt the byte array after this method is returned (i.e., before the data is returned in
        // the executor's thread.
        executeServiceTask(new Runnable() {
            @Override
            public void run() {
                HttpServletRequest request = null;
                HttpServletResponse response = null;
                try {
                    WebSocketServletHolder webSocketHolder = new AtmosphereWebSocketServletHolder(webSocket);
                    response = createServletResponse(webSocketHolder);
                    request = createServletRequest(webSocketHolder, stream);
                    if (destination != null) {
                        String reqid = request.getHeader(requestIdKey);
                        if (reqid != null) {
                            response.setHeader(responseIdKey, reqid);
                        }
                        ((WebSocketDestinationService)destination).invokeInternal(null,
                            webSocket.resource().getRequest().getServletContext(),
                            request, response);
                    }
                } catch (InvalidPathException ex) {
                    reportErrorStatus(response, 400);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to invoke service", e);
                }
            }
        });
        return null;
    }

    private void executeServiceTask(Runnable r) {
        try {
            destination.getExecutor().execute(r);
        } catch (RejectedExecutionException e) {
            LOG.warning(
                "Executor queue is full, run the service invocation task in caller thread." 
                + "  Users can specify a larger executor queue to avoid this.");
            r.run();
        }
    }
    
    // may want to move this error reporting code to WebSocketServletHolder
    protected void reportErrorStatus(HttpServletResponse response, int status) {
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

    /** {@inheritDoc}*/
    @Override
    public void onOpen(WebSocket webSocket) {
        LOG.fine("onOpen(WebSocket)");
    }

    /** {@inheritDoc}*/
    @Override
    public void onClose(WebSocket webSocket) {
        LOG.fine("onClose(WebSocket)");
        
    }

    /** {@inheritDoc}*/
    @Override
    public void onError(WebSocket webSocket, WebSocketException t) {
        LOG.severe("onError(WebSocket, WebSocketException)");
    }

//    protected WebSocketVirtualServletRequest createServletRequest(WebSocketServletHolder webSocketHolder, 
//                                                                  byte[] data, int offset, int length) 
//        throws IOException {
//        return new WebSocketVirtualServletRequest(webSocketHolder, 
//                                                  new ByteArrayInputStream(data, offset, length));
//    }
    
    protected WebSocketVirtualServletRequest createServletRequest(WebSocketServletHolder webSocketHolder, 
                                                                  InputStream stream)
        throws IOException {
        return new WebSocketVirtualServletRequest(webSocketHolder, stream);
    }

    protected WebSocketVirtualServletResponse createServletResponse(WebSocketServletHolder webSocketHolder) 
        throws IOException {
        return new WebSocketVirtualServletResponse(webSocketHolder);
    }
    
    protected static class AtmosphereWebSocketServletHolder implements WebSocketServletHolder {
        private WebSocket webSocket;
        
        public AtmosphereWebSocketServletHolder(WebSocket webSocket) {
            this.webSocket = webSocket;
        }
        
        @Override
        public String getAuthType() {
            return webSocket.resource().getRequest().getAuthType();
        }

        @Override
        public String getContextPath() {
            return webSocket.resource().getRequest().getContextPath();
        }

        @Override
        public String getLocalAddr() {
            return webSocket.resource().getRequest().getLocalAddr();
        }

        @Override
        public String getLocalName() {
            return webSocket.resource().getRequest().getLocalName();
        }

        @Override
        public int getLocalPort() {
            return webSocket.resource().getRequest().getLocalPort();
        }

        @Override
        public Locale getLocale() {
            return webSocket.resource().getRequest().getLocale();
        }

        @Override
        public Enumeration<Locale> getLocales() {
            return webSocket.resource().getRequest().getLocales();
        }

        @Override
        public String getProtocol() {
            return webSocket.resource().getRequest().getProtocol();
        }

        @Override
        public String getRemoteAddr() {
            return webSocket.resource().getRequest().getRemoteAddr();
        }

        @Override
        public String getRemoteHost() {
            return webSocket.resource().getRequest().getRemoteHost();
        }

        @Override
        public int getRemotePort() {
            return webSocket.resource().getRequest().getRemotePort();
        }

        @Override
        public String getRequestURI() {
            return webSocket.resource().getRequest().getRequestURI();
        }

        @Override
        public StringBuffer getRequestURL() {
            return webSocket.resource().getRequest().getRequestURL();
        }

        @Override
        public DispatcherType getDispatcherType() {
            return webSocket.resource().getRequest().getDispatcherType();
        }

        @Override
        public boolean isSecure() {
            return webSocket.resource().getRequest().isSecure();
        }

        @Override
        public String getPathInfo() {
            return webSocket.resource().getRequest().getServletPath();
        }

        @Override
        public String getPathTranslated() {
            return webSocket.resource().getRequest().getPathTranslated();
        }

        @Override
        public String getScheme() {
            return webSocket.resource().getRequest().getScheme();
        }

        @Override
        public String getServerName() {
            return webSocket.resource().getRequest().getServerName();
        }

        @Override
        public String getServletPath() {
            return webSocket.resource().getRequest().getServletPath();
        }

        @Override
        public int getServerPort() {
            return webSocket.resource().getRequest().getServerPort();
        }

        @Override
        public ServletContext getServletContext() {
            return webSocket.resource().getRequest().getServletContext();
        }
        
        @Override
        public Principal getUserPrincipal() {
            return webSocket.resource().getRequest().getUserPrincipal();
        }

        @Override
        public Object getAttribute(String name) {
            return webSocket.resource().getRequest().getAttribute(name);
        }

        @Override
        public void write(byte[] data, int offset, int length) throws IOException {
            webSocket.write(data, offset, length);
        }
    }

}
