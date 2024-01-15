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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ReadListener;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.transport.websocket.InvalidPathException;
import org.apache.cxf.transport.websocket.WebSocketUtils;
import org.eclipse.jetty.websocket.api.Session;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *
 */
public class WebSocketVirtualServletRequest implements HttpServletRequest {
    private static final Logger LOG = LogUtils.getL7dLogger(WebSocketVirtualServletRequest.class);

    private WebSocketServletHolder webSocketHolder;
    private InputStream in;
    private Map<String, String> requestHeaders;
    private Map<String, Object> attributes;

    public WebSocketVirtualServletRequest(WebSocketServletHolder websocket, InputStream in, Session session)
        throws IOException {
        this.webSocketHolder = websocket;
        this.in = in;

        Map<String, List<String>> ugHeaders = session.getUpgradeRequest().getHeaders();
        this.requestHeaders = WebSocketUtils.readHeaders(in);
        for (Map.Entry<String, List<String>> ent : ugHeaders.entrySet()) {
            if (!requestHeaders.containsKey(ent.getKey())) {
                requestHeaders.put(ent.getKey(), ent.getValue().get(0));
            }
        }
        String path = requestHeaders.get(WebSocketUtils.URI_KEY);
        String origin = websocket.getRequestURI();
        if (!path.startsWith(origin)) {
            LOG.log(Level.WARNING, "invalid path: {0} not within {1}", new Object[]{path, origin});
            throw new InvalidPathException();
        }
        this.attributes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Object v = websocket.getAttribute("org.apache.cxf.transport.endpoint.address");
        if (v != null) {
            attributes.put("org.apache.cxf.transport.endpoint.address", v);
        }
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "getAttribute({0}) -> {1}", new Object[] {name, attributes.get(name)});
        }
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        LOG.log(Level.FINE, "getAttributeNames()");
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public String getCharacterEncoding() {
        LOG.log(Level.FINE, "getCharacterEncoding()");
        return null;
    }

    @Override
    public int getContentLength() {
        LOG.log(Level.FINE, "getContentLength()");
        return -1;
    }

    @Override
    public String getContentType() {
        LOG.log(Level.FINE, "getContentType()");
        return requestHeaders.get("Content-Type");
    }

    @Override
    public DispatcherType getDispatcherType() {
        LOG.log(Level.FINE, "getDispatcherType()");
        return webSocketHolder.getDispatcherType();
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

            @Override
            public boolean isFinished() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isReady() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setReadListener(ReadListener arg0) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public String getLocalAddr() {
        LOG.log(Level.FINE, "getLocalAddr()");
        return webSocketHolder.getLocalAddr();
    }

    @Override
    public String getLocalName() {
        LOG.log(Level.FINE, "getLocalName()");
        return webSocketHolder.getLocalName();
    }

    @Override
    public int getLocalPort() {
        LOG.log(Level.FINE, "getLocalPort()");
        return webSocketHolder.getLocalPort();
    }

    @Override
    public Locale getLocale() {
        LOG.log(Level.FINE, "getLocale()");
        return webSocketHolder.getLocale();
    }

    public Enumeration<Locale> getLocales() {
        LOG.log(Level.FINE, "getLocales()");
        return webSocketHolder.getLocales();
    }

    @Override
    public String getParameter(String name) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "getParameter({0})", name);
        }
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        LOG.log(Level.FINE, "getParameterMap()");
        return null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        LOG.log(Level.FINE, "getParameterNames()");
        return null;
    }

    @Override
    public String[] getParameterValues(String name) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "getParameterValues({0})", name);
        }
        return null;
    }

    @Override
    public String getProtocol() {
        LOG.log(Level.FINE, "getProtocol");
        return webSocketHolder.getProtocol();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        LOG.log(Level.FINE, "getReader");
        return new BufferedReader(new InputStreamReader(in, UTF_8));
    }

    @Override
    public String getRemoteAddr() {
        LOG.log(Level.FINE, "getRemoteAddr");
        return webSocketHolder.getRemoteAddr();
    }

    @Override
    public String getRemoteHost() {
        LOG.log(Level.FINE, "getRemoteHost");
        return webSocketHolder.getRemoteHost();
    }

    @Override
    public int getRemotePort() {
        LOG.log(Level.FINE, "getRemotePort");
        return webSocketHolder.getRemotePort();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        LOG.log(Level.FINE, "getRequestDispatcher");
        return null;
    }

    @Override
    public String getScheme() {
        LOG.log(Level.FINE, "getScheme");
        return webSocketHolder.getScheme();
    }

    @Override
    public String getServerName() {
        return webSocketHolder.getServerName();
    }

    @Override
    public int getServerPort() {
        LOG.log(Level.FINE, "getServerPort");
        return webSocketHolder.getServerPort();
    }

    @Override
    public ServletContext getServletContext() {
        LOG.log(Level.FINE, "getServletContext");
        return webSocketHolder.getServletContext();
    }

    public boolean isAsyncStarted() {
        LOG.log(Level.FINE, "isAsyncStarted");
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        LOG.log(Level.FINE, "isAsyncSupported");
        return false;
    }

    public boolean isSecure() {
        LOG.log(Level.FINE, "isSecure");
        return webSocketHolder.isSecure();
    }

    @Override
    public void removeAttribute(String name) {
        LOG.log(Level.FINE, "removeAttribute");
        attributes.remove(name);
    }

    @Override
    public void setAttribute(String name, Object o) {
        LOG.log(Level.FINE, "setAttribute");
        attributes.put(name,  o);
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        LOG.log(Level.FINE, "setCharacterEncoding");
        // ignore as we stick to utf-8.
    }

    @Override
    public AsyncContext startAsync() {
        LOG.log(Level.FINE, "startAsync");
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
        LOG.log(Level.FINE, "startAsync");
        return null;
    }

    @Override
    public boolean authenticate(HttpServletResponse servletResponse) throws IOException, ServletException {
        LOG.log(Level.FINE, "authenticate");
        return false;
    }

    @Override
    public String getAuthType() {
        LOG.log(Level.FINE, "getAuthType");
        return webSocketHolder.getAuthType();
    }

    @Override
    public String getContextPath() {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "getContextPath -> " + webSocketHolder.getContextPath());
        }
        return webSocketHolder.getContextPath();
    }

    @Override
    public Cookie[] getCookies() {
        LOG.log(Level.FINE, "getCookies");
        return null;
    }

    @Override
    public long getDateHeader(String name) {
        LOG.log(Level.FINE, "getDateHeader");
        return 0;
    }

    @Override
    public String getHeader(String name) {
        LOG.log(Level.FINE, "getHeader");
        return requestHeaders.get(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        LOG.log(Level.FINE, "getHeaderNames");
        return Collections.enumeration(requestHeaders.keySet());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        LOG.log(Level.FINE, "getHeaders");
        // our protocol assumes no multiple headers
        return Collections.enumeration(Arrays.asList(requestHeaders.get(name)));
    }

    @Override
    public int getIntHeader(String name) {
        LOG.log(Level.FINE, "getIntHeader");
        String v = requestHeaders.get(name);
        return v == null ? -1 : Integer.parseInt(v);
    }

    @Override
    public String getMethod() {
        LOG.log(Level.FINE, "getMethod");
        return requestHeaders.get(WebSocketUtils.METHOD_KEY);
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        LOG.log(Level.FINE, "getPart");
        return null;
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        LOG.log(Level.FINE, "getParts");
        return null;
    }

    @Override
    public String getPathInfo() {
        String uri = requestHeaders.get(WebSocketUtils.URI_KEY);
        String servletpath = webSocketHolder.getServletPath();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "getPathInfo " + servletpath + " " + uri);
        }
        //TODO remove the query string part
        //REVISIT may cache this value in requstHeaders?
        return uri.substring(servletpath.length());
    }

    @Override
    public String getPathTranslated() {
        String path = getPathInfo();
        String opathtrans = webSocketHolder.getPathTranslated();
        // some container may choose not to return this value
        if (opathtrans == null) {
            return null;
        }
        String opathinfo = webSocketHolder.getPathInfo();
        LOG.log(Level.FINE, "getPathTranslated " + path + " " + opathinfo);
        int pos = opathtrans.indexOf(opathinfo);
        //REVISIT may cache this value in requstHeaders?
        return new StringBuilder().append(opathtrans.substring(0, pos)).append(path).toString();
    }

    @Override
    public String getQueryString() {
        LOG.log(Level.FINE, "getQueryString");
        return null;
    }

    @Override
    public String getRemoteUser() {
        LOG.log(Level.FINE, "getRemoteUser");
        return null;
    }

    @Override
    public String getRequestURI() {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "getRequestURI " + requestHeaders.get(WebSocketUtils.URI_KEY));
        }
        return requestHeaders.get(WebSocketUtils.URI_KEY);
    }

    @Override
    public StringBuffer getRequestURL() {
        StringBuffer sb = webSocketHolder.getRequestURL();
        String ouri = webSocketHolder.getRequestURI();
        String uri = getRequestURI();
        sb.append(uri.substring(ouri.length()));
        LOG.log(Level.FINE, "getRequestURL " + uri);
        return sb;
    }

    @Override
    public String getRequestedSessionId() {
        LOG.log(Level.FINE, "getRequestedSessionId");
        return null;
    }

    @Override
    public String getServletPath() {
        LOG.log(Level.FINE, "getServletPath " + webSocketHolder.getServletPath());
        return webSocketHolder.getServletPath();
    }

    @Override
    public HttpSession getSession() {
        LOG.log(Level.FINE, "getSession");
        return null;
    }

    @Override
    public HttpSession getSession(boolean create) {
        LOG.log(Level.FINE, "getSession");
        return null;
    }

    @Override
    public Principal getUserPrincipal() {
        LOG.log(Level.FINE, "getUserPrincipal");
        return webSocketHolder.getUserPrincipal();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        LOG.log(Level.FINE, "isRequestedSessionIdFromCookie");
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        LOG.log(Level.FINE, "isRequestedSessionIdFromURL");
        return false;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        LOG.log(Level.FINE, "isRequestedSessionIdValid");
        return false;
    }

    @Override
    public boolean isUserInRole(String role) {
        LOG.log(Level.FINE, "isUserInRole");
        return false;
    }

    @Override
    public void login(String username, String password) throws ServletException {
        LOG.log(Level.FINE, "login");

    }

    @Override
    public void logout() throws ServletException {
        LOG.log(Level.FINE, "logout");
    }

    @Override
    public long getContentLengthLong() {
        return -1;
    }

    @Override
    public String changeSessionId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }


    public String getRealPath(String path) {
        return path;
    }

    
    public boolean isRequestedSessionIdFromUrl() {
        LOG.log(Level.FINE, "isRequestedSessionIdFromUrl");
        return false;
    }

    public String getRequestId() {
        return null;
    }

    @Override
    public String getProtocolRequestId() {
        return null;
    }

    @Override
    public ServletConnection getServletConnection() {
        return null;
    }
}
