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

package org.apache.cxf.transport.websocket;

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
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.apache.cxf.common.logging.LogUtils;

/**
 * 
 */
public class WebSocketVirtualServletRequest implements HttpServletRequest {
    private static final Logger LOG = LogUtils.getL7dLogger(WebSocketVirtualServletRequest.class);

    private WebSocketServletHolder webSocketHolder;
    private InputStream in;
    private Map<String, String> requestHeaders;
    private Map<String, Object> attributes;
    
    public WebSocketVirtualServletRequest(WebSocketServletHolder websocket, InputStream in) 
        throws IOException {
        this.webSocketHolder = websocket;
        this.in = in;

        this.requestHeaders = WebSocketUtils.readHeaders(in);
        String path = requestHeaders.get(WebSocketUtils.URI_KEY);
        String origin = websocket.getRequestURI();
        if (!path.startsWith(origin)) {
            LOG.log(Level.WARNING, "invalid path: {0} not within {1}", new Object[]{path, origin});
            throw new InvalidPathException();
        }
        this.attributes = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
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
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "getAttribute({0})", name);
        }
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        LOG.log(Level.INFO, "getAttributeNames()");
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public String getCharacterEncoding() {
        // TODO Auto-generated method stub
        LOG.log(Level.INFO, "getCharacterEncoding()");
        return null;
    }

    @Override
    public int getContentLength() {
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
        };
    }

    @Override
    public String getLocalAddr() {
        LOG.log(Level.INFO, "getLocalAddr()");
        return webSocketHolder.getLocalAddr();
    }

    @Override
    public String getLocalName() {
        LOG.log(Level.INFO, "getLocalName()");
        return webSocketHolder.getLocalName();
    }

    @Override
    public int getLocalPort() {
        LOG.log(Level.INFO, "getLocalPort()");
        return webSocketHolder.getLocalPort();
    }

    @Override
    public Locale getLocale() {
        LOG.log(Level.INFO, "getLocale()");
        return webSocketHolder.getLocale();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        LOG.log(Level.INFO, "getLocales()");
        return webSocketHolder.getLocales();
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
        LOG.log(Level.INFO, "getProtocol");
        return webSocketHolder.getProtocol();
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
        return webSocketHolder.getRemoteAddr();
    }

    @Override
    public String getRemoteHost() {
        LOG.log(Level.INFO, "getRemoteHost");
        return webSocketHolder.getRemoteHost();
    }

    @Override
    public int getRemotePort() {
        LOG.log(Level.INFO, "getRemotePort");
        return webSocketHolder.getRemotePort();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        // TODO Auto-generated method stub
        LOG.log(Level.INFO, "getRequestDispatcher");
        return null;
    }

    @Override
    public String getScheme() {
        LOG.log(Level.INFO, "getScheme");
        return webSocketHolder.getScheme();
    }

    @Override
    public String getServerName() {
        return webSocketHolder.getServerName();
    }

    @Override
    public int getServerPort() {
        LOG.log(Level.INFO, "getServerPort");
        return webSocketHolder.getServerPort();
    }

    @Override
    public ServletContext getServletContext() {
        LOG.log(Level.INFO, "getServletContext");
        return webSocketHolder.getServletContext();
    }

    @Override
    public boolean isAsyncStarted() {
        LOG.log(Level.INFO, "isAsyncStarted");
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        LOG.log(Level.INFO, "isAsyncSupported");
        return false;
    }

    @Override
    public boolean isSecure() {
        LOG.log(Level.INFO, "isSecure");
        return webSocketHolder.isSecure();
    }

    @Override
    public void removeAttribute(String name) {
        LOG.log(Level.INFO, "removeAttribute");
        attributes.remove(name);
    }

    @Override
    public void setAttribute(String name, Object o) {
        LOG.log(Level.INFO, "setAttribute");
        attributes.put(name,  o);
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        LOG.log(Level.INFO, "setCharacterEncoding");
        // ignore as we stick to utf-8.
    }

    @Override
    public AsyncContext startAsync() {
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
        LOG.log(Level.INFO, "getAuthType");
        return webSocketHolder.getAuthType();
    }

    @Override
    public String getContextPath() {
        LOG.log(Level.INFO, "getContextPath");
        return webSocketHolder.getContextPath();
    }

    @Override
    public Cookie[] getCookies() {
        LOG.log(Level.INFO, "getCookies");
        return null;
    }

    @Override
    public long getDateHeader(String name) {
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
        return requestHeaders.get(WebSocketUtils.METHOD_KEY);
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        LOG.log(Level.INFO, "getPart");
        return null;
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        LOG.log(Level.INFO, "getParts");
        return null;
    }

    @Override
    public String getPathInfo() {
        LOG.log(Level.INFO, "getPathInfo");
        String uri = requestHeaders.get(WebSocketUtils.URI_KEY);
        String servletpath = webSocketHolder.getServletPath();
        //TODO remove the query string part
        //REVISIT may cache this value in requstHeaders?
        return uri.substring(servletpath.length());
    }

    @Override
    public String getPathTranslated() {
        LOG.log(Level.INFO, "getPathTranslated");
        String path = getPathInfo();
        String opathtrans = webSocketHolder.getPathTranslated();
        String opathinfo = webSocketHolder.getPathInfo();
        int pos = opathtrans.indexOf(opathinfo);
        //REVISIT may cache this value in requstHeaders?
        return new StringBuilder().append(opathtrans.substring(0, pos)).append(path).toString();
    }

    @Override
    public String getQueryString() {
        LOG.log(Level.INFO, "getQueryString");
        return null;
    }

    @Override
    public String getRemoteUser() {
        LOG.log(Level.INFO, "getRemoteUser");
        return null;
    }

    @Override
    public String getRequestURI() {
        LOG.log(Level.INFO, "getRequestURI");
        return requestHeaders.get(WebSocketUtils.URI_KEY);
    }

    @Override
    public StringBuffer getRequestURL() {
        LOG.log(Level.INFO, "getRequestURL");
        StringBuffer sb = webSocketHolder.getRequestURL();
        String ouri = webSocketHolder.getRequestURI();
        String uri = getRequestURI();
        sb.append(uri.substring(ouri.length()));
        return sb;
    }

    @Override
    public String getRequestedSessionId() {
        LOG.log(Level.INFO, "getRequestedSessionId");
        return null;
    }

    @Override
    public String getServletPath() {
        LOG.log(Level.INFO, "getServletPath");
        return webSocketHolder.getServletPath();
    }

    @Override
    public HttpSession getSession() {
        LOG.log(Level.INFO, "getSession");
        return null;
    }

    @Override
    public HttpSession getSession(boolean create) {
        LOG.log(Level.INFO, "getSession");
        return null;
    }

    @Override
    public Principal getUserPrincipal() {
        LOG.log(Level.INFO, "getUserPrincipal");
        return webSocketHolder.getUserPrincipal();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        LOG.log(Level.INFO, "isRequestedSessionIdFromCookie");
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        LOG.log(Level.INFO, "isRequestedSessionIdFromURL");
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        LOG.log(Level.INFO, "isRequestedSessionIdFromUrl");
        return false;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        LOG.log(Level.INFO, "isRequestedSessionIdValid");
        return false;
    }

    @Override
    public boolean isUserInRole(String role) {
        LOG.log(Level.INFO, "isUserInRole");
        return false;
    }

    @Override
    public void login(String username, String password) throws ServletException {
        LOG.log(Level.INFO, "login");
        
    }

    @Override
    public void logout() throws ServletException {
        LOG.log(Level.INFO, "logout");
    }
}
