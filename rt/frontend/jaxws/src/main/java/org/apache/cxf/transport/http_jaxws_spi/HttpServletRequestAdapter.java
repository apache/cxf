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
package org.apache.cxf.transport.http_jaxws_spi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import jakarta.xml.ws.spi.http.HttpContext;
import jakarta.xml.ws.spi.http.HttpExchange;

/**
 * This class provides a HttpServletRequest instance using information
 * coming from the HttpExchange and HttpContext instances provided
 * by the underlying container.
 * Note: many methods' implementation still TODO.
 *
 */
class HttpServletRequestAdapter implements HttpServletRequest {

    private HttpExchange exchange;
    private HttpContext context;
    private String characterEncoding;
    private ServletInputStreamAdapter servletInputStreamAdapter;
    private BufferedReader reader;

    HttpServletRequestAdapter(HttpExchange exchange) {
        this.exchange = exchange;
        this.context = exchange.getHttpContext();
    }

    public AsyncContext getAsyncContext() {
        throw new UnsupportedOperationException();
    }

    public Object getAttribute(String name) {
        return exchange.getAttribute(name);
    }

    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(exchange.getAttributeNames());
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public int getContentLength() {
        String s = getHeader("Content-Length");
        if (s != null) {
            return Integer.parseInt(s);
        }
        return -1;
    }

    public String getContentType() {
        return this.getHeader("Content-Type");
    }

    public DispatcherType getDispatcherType() {
        throw new UnsupportedOperationException();
    }

    public ServletInputStream getInputStream() throws IOException {
        if (servletInputStreamAdapter == null) {
            servletInputStreamAdapter = new ServletInputStreamAdapter(exchange.getRequestBody());
        }
        return servletInputStreamAdapter;
    }

    public String getLocalAddr() {
        InetSocketAddress isa = exchange.getLocalAddress();
        if (isa != null) {
            InetAddress ia = isa.getAddress();
            if (ia != null) {
                return ia.getHostAddress();
            }
        }
        return null;
    }

    public Locale getLocale() {
        return null;
    }

    public Enumeration<Locale> getLocales() {
        throw new UnsupportedOperationException();
    }

    public String getLocalName() {
        InetSocketAddress isa = exchange.getLocalAddress();
        if (isa != null) {
            InetAddress ia = isa.getAddress();
            if (ia != null) {
                return ia.getHostName();
            }
        }
        return null;
    }

    public int getLocalPort() {
        InetSocketAddress isa = exchange.getLocalAddress();
        return isa != null ? isa.getPort() : 0;
    }

    public String getParameter(String name) {
        throw new UnsupportedOperationException();
    }

    public Map<String, String[]> getParameterMap() {
        throw new UnsupportedOperationException();
    }

    public Enumeration<String> getParameterNames() {
        throw new UnsupportedOperationException();
    }

    public String[] getParameterValues(String name) {
        throw new UnsupportedOperationException();
    }

    public String getProtocol() {
        return exchange.getProtocol();
    }

    public BufferedReader getReader() throws IOException {
        if (reader == null) {
            Reader isr = characterEncoding == null
                ? new InputStreamReader(exchange.getRequestBody())
                : new InputStreamReader(exchange.getRequestBody(), characterEncoding);
            reader = new BufferedReader(isr);
        }
        return reader;
    }

    @Deprecated
    public String getRealPath(String path) {
        throw new UnsupportedOperationException();
    }

    public String getRemoteAddr() {
        InetSocketAddress isa = exchange.getRemoteAddress();
        if (isa != null) {
            InetAddress ia = isa.getAddress();
            if (ia != null) {
                return ia.getHostAddress();
            }
        }
        return null;
    }

    public String getRemoteHost() {
        InetSocketAddress isa = exchange.getRemoteAddress();
        if (isa != null) {
            InetAddress ia = isa.getAddress();
            if (ia != null) {
                return ia.getHostName();
            }
        }
        return null;
    }

    public int getRemotePort() {
        InetSocketAddress isa = exchange.getRemoteAddress();
        return isa != null ? isa.getPort() : 0;
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        throw new UnsupportedOperationException();
    }

    public String getScheme() {
        return exchange.getScheme();
    }

    public String getServerName() {
        return null;
    }

    public int getServerPort() {
        return 0;
    }

    public ServletContext getServletContext() {
        return null;
    }

    public boolean isAsyncStarted() {
        throw new UnsupportedOperationException();
    }

    public boolean isAsyncSupported() {
        throw new UnsupportedOperationException();
    }

    public boolean isSecure() {
        throw new UnsupportedOperationException();
    }

    public void removeAttribute(String name) {
        throw new UnsupportedOperationException();
    }

    public void setAttribute(String name, Object o) {
        throw new UnsupportedOperationException();
    }

    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        this.characterEncoding = env;
    }

    public AsyncContext startAsync() {
        throw new UnsupportedOperationException();
    }

    public AsyncContext startAsync(ServletRequest request, ServletResponse response) {
        throw new UnsupportedOperationException();
    }

    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    public String getAuthType() {
        return null;
    }

    public String getContextPath() {
        return exchange.getContextPath();
    }

    public Cookie[] getCookies() {
        return null;
    }

    public long getDateHeader(String name) {
        String s = this.getHeader(name);
        return s != null ? Long.parseLong(s) : 0;
    }

    public String getHeader(String name) {
        return exchange.getRequestHeader(name);
    }

    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(exchange.getRequestHeaders().keySet());
    }

    public Enumeration<String> getHeaders(String name) {
        List<String> list = exchange.getRequestHeaders().get(name);
        return list != null ? Collections.enumeration(list) : null;
    }

    public int getIntHeader(String name) {
        String s = this.getHeader(name);
        return s != null ? Integer.parseInt(s) : 0;
    }

    public String getMethod() {
        return exchange.getRequestMethod();
    }

    public Part getPart(String name) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    public Collection<Part> getParts() throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    public String getPathInfo() {
        return exchange.getPathInfo();
    }

    public String getPathTranslated() {
        return null;
    }

    public String getQueryString() {
        return exchange.getQueryString();
    }

    public String getRemoteUser() {
        return null;
    }

    public String getRequestedSessionId() {
        return null;
    }

    public String getRequestURI() {
        return exchange.getRequestURI();
    }

    public StringBuffer getRequestURL() {
        StringBuffer sb = new StringBuffer();
        sb.append(exchange.getScheme());
        sb.append("://");
        String host = this.getHeader("Host");
        if (host != null) {
            sb.append(host);
        } else {
            InetSocketAddress la = exchange.getLocalAddress();
            if (la != null) {
                sb.append(la.getHostName());
                if (la.getPort() > 0) {
                    sb.append(':');
                    sb.append(la.getPort());
                }
            } else {
                sb.append("localhost");
            }
        }
        sb.append(exchange.getContextPath());
        sb.append(context.getPath());
        return sb;
    }

    public String getServletPath() {
        return null;
    }

    public HttpSession getSession() {
        return null;
    }

    public HttpSession getSession(boolean create) {
        return null;
    }

    public Principal getUserPrincipal() {
        return exchange.getUserPrincipal();
    }

    public boolean isRequestedSessionIdFromCookie() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public boolean isRequestedSessionIdFromUrl() {
        throw new UnsupportedOperationException();
    }

    public boolean isRequestedSessionIdFromURL() {
        throw new UnsupportedOperationException();
    }

    public boolean isRequestedSessionIdValid() {
        return false;
    }

    public boolean isUserInRole(String role) {
        return exchange.isUserInRole(role);
    }

    public void login(String username, String password) throws ServletException {
        throw new UnsupportedOperationException();
    }

    public void logout() throws ServletException {
        throw new UnsupportedOperationException();
    }

    private static class ServletInputStreamAdapter extends ServletInputStream {

        private InputStream delegate;

        ServletInputStreamAdapter(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
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
    }

    @Override
    public long getContentLengthLong() {
        String s = getHeader("Content-Length");
        if (s != null) {
            return Long.parseLong(s);
        }
        return -1;
    }

    @Override
    public String changeSessionId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> cls) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRequestId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProtocolRequestId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletConnection getServletConnection() {
        throw new UnsupportedOperationException();
    }
}
