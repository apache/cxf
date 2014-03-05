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

package org.apache.cxf.transport.http_jetty;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.eclipse.jetty.websocket.WebSocket;

class JettyWebSocket implements WebSocket.OnBinaryMessage, WebSocket.OnTextMessage {
    private static final Logger LOG = LogUtils.getL7dLogger(JettyWebSocket.class);
    private static final String CRLF = "\r\n";
    
    private JettyHTTPDestination jettyHTTPDestination;
    private ServletContext servletContext;
    private Connection webSocketConnection;
    private Map<String, Object> requestProperties;
    private String protocol;
    
    public JettyWebSocket(JettyHTTPDestination jettyHTTPDestination, ServletContext servletContext,
                          HttpServletRequest request, String protocol) {
        this.jettyHTTPDestination = jettyHTTPDestination;
        this.servletContext = servletContext;
        this.protocol = protocol;
        this.requestProperties = readProperties(request);
    }
    
    private Map<String, Object> readProperties(HttpServletRequest request) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("servletPath", request.getServletPath());
        properties.put("requestURI", request.getRequestURI());
        properties.put("requestURL", request.getRequestURL().toString());
        properties.put("contextPath", request.getContextPath());
        properties.put("servletPath", request.getServletPath());
        properties.put("pathInfo", request.getPathInfo());
        properties.put("protocol", protocol);
        // some additional ones
        properties.put("localAddr", request.getLocalAddr());
        properties.put("localName", request.getLocalName());
        properties.put("localPort", request.getLocalPort());
        properties.put("locale", request.getLocale());
        properties.put("locales", request.getLocales());
        properties.put("remoteHost", request.getRemoteHost());
        properties.put("remoteAddr", request.getRemoteAddr());
        properties.put("serverName", request.getServerName());
        properties.put("secure", request.isSecure());
        
        return properties;
    }

    @Override
    public void onClose(int closeCode, String message) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "onClose({0}, {1})", new Object[]{closeCode, message});
        }
    }

    @Override
    public void onOpen(Connection connection) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "onOpen({0}))", connection);
        }
        this.webSocketConnection = connection;
    }

    @Override
    public void onMessage(String data) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "onMessage({0})", data);
        }
        try {
            byte[] bdata = data.getBytes("utf-8");
            jettyHTTPDestination.invoke(null, servletContext, 
                                        createServletRequest(bdata, 0, bdata.length),
                                        createServletResponse());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to invoke service", e);
        }            
    }

    @Override
    public void onMessage(byte[] data, int offset, int length) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "onMessage({0}, {1}, {2})", new Object[]{data, offset, length});
        }
        try {
            jettyHTTPDestination.invoke(null, servletContext, 
                                        createServletRequest(data, offset, length),
                                        createServletResponse());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to invoke service", e);
        }
    }
    
    private <T> T getRequestProperty(String name, Class<T> cls) {
        return getValue(requestProperties, name, cls);
    }
    
    private WebSocketVirtualServletRequest createServletRequest(byte[] data, int offset, int length) 
        throws IOException {
        return new WebSocketVirtualServletRequest(servletContext, this, new ByteArrayInputStream(data, offset, length));
    }

    private WebSocketVirtualServletResponse createServletResponse() throws IOException {
        return new WebSocketVirtualServletResponse(this);
    }
    
    /**
     * Writes to the underlining socket.
     * 
     * @param data
     * @param offset
     * @param length
     */
    void write(byte[] data, int offset, int length) throws IOException {
        LOG.log(Level.INFO, "write(byte[], offset, length)");
        webSocketConnection.sendMessage(data, offset, length);
    }

    private static byte[] buildResponse(Map<String, String> headers, byte[] data, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        String v = headers.get("$sc");
        sb.append(v == null ? "200" : v).append(CRLF);
        v = headers.get("Content-Type");
        if (v != null) {
            sb.append("Content-Type: ").append(v).append(CRLF);
        }
        sb.append(CRLF);
        
        byte[] hb = sb.toString().getBytes();
        byte[] longdata = new byte[hb.length + length];
        System.arraycopy(hb, 0, longdata, 0, hb.length);
        if (data != null && length > 0) {
            System.arraycopy(data, offset, longdata, hb.length, length);
        }
        return longdata;
    }

    private static byte[] buildResponse(byte[] data, int offset, int length) {
        byte[] longdata = new byte[length + 2];
        longdata[0] = 0x0d;
        longdata[1] = 0x0a;
        System.arraycopy(data, offset, longdata, 2, length);
        return longdata;
    }
    
    ServletOutputStream getServletOutputStream(final Map<String, String> headers) {
        LOG.log(Level.INFO, "getServletOutputStream()");
        return new ServletOutputStream() {

            @Override
            public void write(int b) throws IOException {
                byte[] data = new byte[1];
                data[0] = (byte)b;
                write(data, 0, 1);
            }

            @Override
            public void write(byte[] data, int offset, int length) throws IOException {
                if (headers.get("$flushed") == null) {
                    data = buildResponse(headers, data, offset, length);
                    headers.put("$flushed", "true");
                } else {
                    data = buildResponse(data, offset, length);
                }
                webSocketConnection.sendMessage(data, 0, data.length);
            }
        };
    }
    
    OutputStream getOutputStream(final Map<String, String> headers) {
        LOG.log(Level.INFO, "getServletOutputStream()");
        return new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                byte[] data = new byte[1];
                data[0] = (byte)b;
                write(data, 0, 1);
            }
            
            @Override
            public void write(byte[] data, int offset, int length) throws IOException {
                if (headers.get("$flushed") == null) {
                    data = buildResponse(headers, data, offset, length);
                    headers.put("$flushed", "true");
                } else {
                    data = buildResponse(data, offset, length);
                }
                webSocketConnection.sendMessage(data, 0, data.length);
            }
        };
        
    }
   
    // 
    static class WebSocketVirtualServletRequest implements HttpServletRequest {
        private ServletContext context;
        private JettyWebSocket websocket;
        private InputStream in;
        private Map<String, String> requestHeaders;
        
        public WebSocketVirtualServletRequest(ServletContext context, JettyWebSocket websocket, InputStream in) 
            throws IOException {
            this.context = context;
            this.websocket = websocket;
            this.in = in;

            requestHeaders = readHeaders(in);
            String path = requestHeaders.get("$path");
            String origin = websocket.getRequestProperty("requestURI", String.class);
            if (path.startsWith(origin)) {
                //REVISIT for now, log it here and reject the request later.  
                LOG.log(Level.WARNING, "invalid path: {0} not within {1}", new Object[]{path, origin});
            }
        }

        @Override
        public AsyncContext getAsyncContext() {
            return null;
        }

        @Override
        public Object getAttribute(String name) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "getAttribute({0})", name);
            }
            return null;
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            LOG.log(Level.INFO, "getAttributeNames()");
            return null;
        }

        @Override
        public String getCharacterEncoding() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getCharacterEncoding()");
            return null;
        }

        @Override
        public int getContentLength() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getContentLength()");
            return 0;
        }

        @Override
        public String getContentType() {
            LOG.log(Level.INFO, "getContentType()");
            return requestHeaders.get("Content-Type");
        }

        @Override
        public DispatcherType getDispatcherType() {
            LOG.log(Level.INFO, "getDispatcherType()");
            return null;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new ServletInputStream() {
                @Override
                public int read() throws IOException {
                    return in.read();
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    return in.read(b, off, len);
                }
            };
        }

        @Override
        public String getLocalAddr() {
            LOG.log(Level.INFO, "getLocalAddr()");
            return websocket.getRequestProperty("localAddr", String.class);
        }

        @Override
        public String getLocalName() {
            LOG.log(Level.INFO, "getLocalName()");
            return websocket.getRequestProperty("localName", String.class);
        }

        @Override
        public int getLocalPort() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getLocalPort()");
            return 0;
        }

        @Override
        public Locale getLocale() {
            LOG.log(Level.INFO, "getLocale()");
            return websocket.getRequestProperty("locale", Locale.class);
        }

        @Override
        public Enumeration<Locale> getLocales() {
            LOG.log(Level.INFO, "getLocales()");
            return CastUtils.cast(websocket.getRequestProperty("locales", Enumeration.class));
        }

        @Override
        public String getParameter(String name) {
            // TODO Auto-generated method stub
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "getParameter({0})", name);
            }
            return null;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getParameterMap()");
            return null;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getParameterNames()");
            return null;
        }

        @Override
        public String[] getParameterValues(String name) {
            // TODO Auto-generated method stub
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "getParameterValues({0})", name);
            }
            return null;
        }

        @Override
        public String getProtocol() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getProtocol");
            return null;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            LOG.log(Level.INFO, "getReader");
            return new BufferedReader(new InputStreamReader(in, "utf-8"));
        }

        @Override
        public String getRealPath(String path) {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getRealPath");
            return null;
        }

        @Override
        public String getRemoteAddr() {
            LOG.log(Level.INFO, "getRemoteAddr");
            return websocket.getRequestProperty("remoteAddr", String.class);
        }

        @Override
        public String getRemoteHost() {
            LOG.log(Level.INFO, "getRemoteHost");
            return websocket.getRequestProperty("remoteHost", String.class);
        }

        @Override
        public int getRemotePort() {
            LOG.log(Level.INFO, "getRemotePort");
            return websocket.getRequestProperty("remotePort", int.class);
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String path) {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getRequestDispatcher");
            return null;
        }

        @Override
        public String getScheme() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getScheme");
            return null;
        }

        @Override
        public String getServerName() {
            return websocket.getRequestProperty("serverName", String.class);
        }

        @Override
        public int getServerPort() {
            LOG.log(Level.INFO, "getServerPort");
            return websocket.getRequestProperty("serverPoart", int.class);
        }

        @Override
        public ServletContext getServletContext() {
            LOG.log(Level.INFO, "getServletContext");
            return context;
        }

        @Override
        public boolean isAsyncStarted() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "isAsyncStarted");
            return false;
        }

        @Override
        public boolean isAsyncSupported() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "isAsyncSupported");
            return false;
        }

        @Override
        public boolean isSecure() {
            LOG.log(Level.INFO, "isSecure");
            return websocket.getRequestProperty("secure", boolean.class);
        }

        @Override
        public void removeAttribute(String name) {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "removeAttribute");
        }

        @Override
        public void setAttribute(String name, Object o) {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "setAttribute");
        }

        @Override
        public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
            LOG.log(Level.INFO, "setCharacterEncoding");
            // ignore as we stick to utf-8.
        }

        @Override
        public AsyncContext startAsync() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "startAsync");
            return null;
        }

        @Override
        public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "startAsync");
            return null;
        }

        @Override
        public boolean authenticate(HttpServletResponse servletResponse) throws IOException, ServletException {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "authenticate");
            return false;
        }

        @Override
        public String getAuthType() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getAuthType");
            return null;
        }

        @Override
        public String getContextPath() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getContextPath");
            return null;
        }

        @Override
        public Cookie[] getCookies() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getCookies");
            return null;
        }

        @Override
        public long getDateHeader(String name) {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getDateHeader");
            return 0;
        }

        @Override
        public String getHeader(String name) {
            LOG.log(Level.INFO, "getHeader");
            return requestHeaders.get(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            LOG.log(Level.INFO, "getHeaderNames");
            return Collections.enumeration(requestHeaders.keySet());
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            LOG.log(Level.INFO, "getHeaders");
            // our protocol assumes no multiple headers
            return Collections.enumeration(Arrays.asList(requestHeaders.get(name)));
        }

        @Override
        public int getIntHeader(String name) {
            LOG.log(Level.INFO, "getIntHeader");
            String v = requestHeaders.get(name);
            return v == null ? -1 : Integer.parseInt(v);
        }

        @Override
        public String getMethod() {
            LOG.log(Level.INFO, "getMethod");
            return requestHeaders.get("$method");
        }

        @Override
        public Part getPart(String name) throws IOException, ServletException {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getPart");
            return null;
        }

        @Override
        public Collection<Part> getParts() throws IOException, ServletException {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getParts");
            return null;
        }

        @Override
        public String getPathInfo() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getPathInfo");
            return null;
        }

        @Override
        public String getPathTranslated() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getPathTranslated");
            return null;
        }

        @Override
        public String getQueryString() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getQueryString");
            return null;
        }

        @Override
        public String getRemoteUser() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getRemoteUser");
            return null;
        }

        @Override
        public String getRequestURI() {
            LOG.log(Level.INFO, "getRequestURI");
            return requestHeaders.get("$path");
        }

        @Override
        public StringBuffer getRequestURL() {
            LOG.log(Level.INFO, "getRequestURL");
            String url = websocket.getRequestProperty("requestURL", String.class);
            String origin = websocket.getRequestProperty("requestURI", String.class);
            StringBuffer sb = new StringBuffer();
            String uri = getRequestURI();
            //REVISIT the way to reject the requeist uri that does not match the original request
            if (!uri.startsWith(origin)) {
                sb.append(url).append("invalid").append(uri);
            } else {
                sb.append(url).append(uri.substring(origin.length()));
            }
            
            return sb;
        }

        @Override
        public String getRequestedSessionId() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getRequestedSessionId");
            return null;
        }

        @Override
        public String getServletPath() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getServletPath");
            return null;
        }

        @Override
        public HttpSession getSession() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getSession");
            return null;
        }

        @Override
        public HttpSession getSession(boolean create) {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getSession");
            return null;
        }

        @Override
        public Principal getUserPrincipal() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getUserPrincipal");
            return null;
        }

        @Override
        public boolean isRequestedSessionIdFromCookie() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "isRequestedSessionIdFromCookie");
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromURL() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "isRequestedSessionIdFromURL");
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromUrl() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "isRequestedSessionIdFromUrl");
            return false;
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "isRequestedSessionIdValid");
            return false;
        }

        @Override
        public boolean isUserInRole(String role) {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "isUserInRole");
            return false;
        }

        @Override
        public void login(String username, String password) throws ServletException {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "login");
            
        }

        @Override
        public void logout() throws ServletException {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "logout");
            
        }
    }

    //TODO need to make the header setting to be written to the body (as symmetric to the request behavior)
    static class WebSocketVirtualServletResponse implements HttpServletResponse {
        private JettyWebSocket websocket;
        private Map<String, String> responseHeaders;
        private boolean flushed;

        public WebSocketVirtualServletResponse(JettyWebSocket websocket) {
            this.websocket = websocket;
            this.responseHeaders = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        }

        @Override
        public void flushBuffer() throws IOException {
            LOG.log(Level.INFO, "flushBuffer()");
            if (!flushed) {
                //REVISIT this mechanism to determine if the headers have been flushed
                if (responseHeaders.get("$flushed") == null) {
                    byte[] data = buildResponse(responseHeaders, null, 0, 0);
                    websocket.write(data, 0, data.length);
                    responseHeaders.put("$flushed", "true");
                }
                flushed = true;
            }
        }

        @Override
        public int getBufferSize() {
            LOG.log(Level.INFO, "getBufferSize()");
            return 0;
        }

        @Override
        public String getCharacterEncoding() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getCharacterEncoding()");
            return null;
        }

        @Override
        public String getContentType() {
            LOG.log(Level.INFO, "getContentType()");
            return responseHeaders.get("Content-Type");
        }

        @Override
        public Locale getLocale() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getLocale");
            return null;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            LOG.log(Level.INFO, "getOutputStream()");
            return websocket.getServletOutputStream(responseHeaders);
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            LOG.log(Level.INFO, "getWriter()");
            return new PrintWriter(websocket.getOutputStream(responseHeaders));
        }

        @Override
        public boolean isCommitted() {
            return false;
        }

        @Override
        public void reset() {
        }

        @Override
        public void resetBuffer() {
            LOG.log(Level.INFO, "resetBuffer()");
        }

        @Override
        public void setBufferSize(int size) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "setBufferSize({0})", size);
            }
        }

        @Override
        public void setCharacterEncoding(String charset) {
            // TODO 
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "setCharacterEncoding({0})", charset);
            }
        }

        @Override
        public void setContentLength(int len) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "setContentLength({0})", len);
            }
            responseHeaders.put("Content-Length", Integer.toString(len));
        }

        @Override
        public void setContentType(String type) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "setContentType({0})", type);
            }
            responseHeaders.put("Content-Type", type);
        }

        @Override
        public void setLocale(Locale loc) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "setLocale({0})", loc);
            }
        }

        @Override
        public void addCookie(Cookie cookie) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "addCookie({0})", cookie);
            }
        }

        @Override
        public void addDateHeader(String name, long date) {
            // TODO
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "addDateHeader({0}, {1})", new Object[]{name, date});
            }
        }

        @Override
        public void addHeader(String name, String value) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "addHeader({0}, {1})", new Object[]{name, value});
            }
            responseHeaders.put(name, value);
        }

        @Override
        public void addIntHeader(String name, int value) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "addIntHeader({0}, {1})", new Object[]{name, value});
            }
            responseHeaders.put(name, Integer.toString(value));
        }

        @Override
        public boolean containsHeader(String name) {
            // TODO Auto-generated method stub
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "containsHeader({0})", name);
            }
            return false;
        }

        @Override
        public String encodeRedirectURL(String url) {
            // TODO Auto-generated method stub
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "encodeRedirectURL({0})", url);
            }
            return null;
        }

        @Override
        public String encodeRedirectUrl(String url) {
            // TODO Auto-generated method stub
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "encodeRedirectUrl({0})", url);
            }
            return null;
        }

        @Override
        public String encodeURL(String url) {
            // TODO Auto-generated method stub
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "encodeURL({0})", url);
            }
            return null;
        }

        @Override
        public String encodeUrl(String url) {
            // TODO Auto-generated method stub
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "encodeUrl({0})", url);
            }
            return null;
        }

        @Override
        public String getHeader(String name) {
            // TODO Auto-generated method stub
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "getHeader({0})", name);
            }
            return null;
        }

        @Override
        public Collection<String> getHeaderNames() {
            // TODO Auto-generated method stub
            LOG.log(Level.INFO, "getHeaderNames()");
            return null;
        }

        @Override
        public Collection<String> getHeaders(String name) {
            // TODO Auto-generated method stub
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "getHeaders({0})", name);
            }
            return null;
        }

        @Override
        public int getStatus() {
            LOG.log(Level.INFO, "getStatus()");
            String v = responseHeaders.get("$sc");
            return v == null ? 200 : Integer.parseInt(v);
        }

        @Override
        public void sendError(int sc) throws IOException {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "sendError{0}", sc);
            }
            responseHeaders.put("$sc", Integer.toString(sc));
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "sendError({0}, {1})", new Object[]{sc, msg});
            }
            responseHeaders.put("$sc", Integer.toString(sc));
            responseHeaders.put("$sm", msg);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            // TODO Auto-generated method stub
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "sendRedirect({0})", location);
            }
        }

        @Override
        public void setDateHeader(String name, long date) {
            // ignore
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "setDateHeader({0}, {1})", new Object[]{name, date});
            }
        }

        @Override
        public void setHeader(String name, String value) {
            // TODO Auto-generated method stub
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "setHeader({0}, {1})", new Object[]{name, value});
            }
        }

        @Override
        public void setIntHeader(String name, int value) {
            // TODO Auto-generated method stub
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "setIntHeader({0}, {1})", new Object[]{name, value});
            }
        }

        @Override
        public void setStatus(int sc) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "setStatus({0})", sc);
            }
            responseHeaders.put("$sc", Integer.toString(sc));
        }

        @Override
        public void setStatus(int sc, String sm) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "setStatus({0}, {1})", new Object[]{sc, sm});
            }
            responseHeaders.put("$sc", Integer.toString(sc));
            responseHeaders.put("$sm", sm);
        }
    }

    /*
     * We accept only a restricted syntax as we have the syntax in our control.
     * Do not allow multiline or line-wrapped headers.
     * Do not allow charset other than utf-8. (although i would have preferred iso-8859-1 ;-)
     */
    private static Map<String, String> readHeaders(InputStream in) throws IOException {
        Map<String, String> headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        // read the request line
        String line = readLine(in);
        int del = line.indexOf(' ');
        if (del < 0) {
            throw new IOException("invalid request: " + line);
        }
        headers.put("$method", line.substring(0, del).trim());
        headers.put("$path", line.substring(del + 1).trim());
        
        // read headers
        while ((line = readLine(in)) != null) {
            if (line.length() > 0) {
                del = line.indexOf(':');
                if (del < 0) {
                    headers.put(line.trim(), "");
                } else {
                    headers.put(line.substring(0, del).trim(), line.substring(del + 1).trim());
                }
            }
        }

        return headers;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getValue(Map<String, Object> properties, String name, Class<T> cls) {
        return (T)properties.get(name);
    }
    

    ///// this is copied from AttachmentDeserializer with a minor change. think about putting this method to IOUtils
    private static String readLine(InputStream in) throws IOException {
        StringBuilder buffer = new StringBuilder(128);

        int c;

        while ((c = in.read()) != -1) {
            // a linefeed is a terminator, always.
            if (c == '\n') {
                break;
            } else if (c == '\r') {
                //just ignore the CR.  The next character SHOULD be an NL.  If not, we're
                //just going to discard this
                continue;
            } else {
                // just add to the buffer
                buffer.append((char)c);
            }
        }

        // no characters found...this was either an eof or a null line.
        if (buffer.length() == 0) {
            return null;
        }

        return buffer.toString();
    }
    ///// END
}
