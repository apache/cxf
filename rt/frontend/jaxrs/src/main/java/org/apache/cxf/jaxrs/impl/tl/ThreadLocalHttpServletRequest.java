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

package org.apache.cxf.jaxrs.impl.tl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;


import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
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

public class ThreadLocalHttpServletRequest extends AbstractThreadLocalProxy<HttpServletRequest>
    implements HttpServletRequest {

    public String getAuthType() {
        return get().getAuthType();
    }

    public String getContextPath() {
        return get().getContextPath();
    }

    public Cookie[] getCookies() {
        return get().getCookies();
    }

    public long getDateHeader(String name) {
        return get().getDateHeader(name);
    }

    public String getHeader(String name) {
        return get().getHeader(name);
    }

    public Enumeration<String> getHeaderNames() {
        return get().getHeaderNames();
    }

    public Enumeration<String> getHeaders(String name) {
        return get().getHeaders(name);
    }

    public int getIntHeader(String name) {
        return get().getIntHeader(name);
    }

    public String getMethod() {
        return get().getMethod();
    }

    public String getPathInfo() {
        return get().getPathInfo();
    }

    public String getPathTranslated() {
        return get().getPathTranslated();
    }

    public String getQueryString() {
        return get().getQueryString();
    }

    public String getRemoteUser() {
        return get().getRemoteUser();
    }

    public String getRequestURI() {
        return get().getRequestURI();
    }

    public StringBuffer getRequestURL() {
        return get().getRequestURL();
    }

    public String getRequestedSessionId() {
        return get().getRequestedSessionId();
    }

    public String getServletPath() {
        return get().getServletPath();
    }

    public HttpSession getSession() {
        return get().getSession();
    }

    public HttpSession getSession(boolean create) {
        return get().getSession(create);
    }

    public Principal getUserPrincipal() {
        return get().getUserPrincipal();
    }

    public boolean isRequestedSessionIdFromCookie() {
        return get().isRequestedSessionIdFromCookie();
    }

    public boolean isRequestedSessionIdFromURL() {
        return get().isRequestedSessionIdFromURL();
    }

    public boolean isRequestedSessionIdValid() {
        return get().isRequestedSessionIdValid();
    }

    public boolean isUserInRole(String role) {
        return get().isUserInRole(role);
    }

    public Object getAttribute(String name) {
        return get().getAttribute(name);
    }

    public Enumeration<String> getAttributeNames() {
        return get().getAttributeNames();
    }

    public String getCharacterEncoding() {
        return get().getCharacterEncoding();
    }

    public int getContentLength() {
        return get().getContentLength();
    }

    public String getContentType() {
        return get().getContentType();
    }

    public ServletInputStream getInputStream() throws IOException {
        return get().getInputStream();
    }

    public String getLocalAddr() {
        return get().getLocalAddr();
    }

    public String getLocalName() {
        return get().getLocalName();
    }

    public int getLocalPort() {
        return get().getLocalPort();
    }

    public Locale getLocale() {
        return get().getLocale();
    }

    public Enumeration<Locale> getLocales() {
        return get().getLocales();
    }

    public String getParameter(String name) {
        return get().getParameter(name);
    }

    public Map<String, String[]> getParameterMap() {
        return get().getParameterMap();
    }

    public Enumeration<String> getParameterNames() {
        return get().getParameterNames();
    }

    public String[] getParameterValues(String name) {
        return get().getParameterValues(name);
    }

    public String getProtocol() {
        return get().getProtocol();
    }

    public BufferedReader getReader() throws IOException {
        return get().getReader();
    }

    public String getRemoteAddr() {
        return get().getRemoteAddr();
    }

    public String getRemoteHost() {
        return get().getRemoteHost();
    }

    public int getRemotePort() {
        return get().getRemotePort();
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        return get().getRequestDispatcher(path);
    }

    public String getScheme() {
        return get().getScheme();
    }

    public String getServerName() {
        return get().getServerName();
    }

    public int getServerPort() {
        return get().getServerPort();
    }

    public boolean isSecure() {
        return get().isSecure();
    }

    public void removeAttribute(String name) {
        get().removeAttribute(name);

    }

    public void setAttribute(String name, Object o) {
        get().setAttribute(name, o);

    }

    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        get().setCharacterEncoding(env);

    }

    public AsyncContext getAsyncContext() {
        return get().getAsyncContext();
    }

    public DispatcherType getDispatcherType() {
        return get().getDispatcherType();
    }

    public ServletContext getServletContext() {
        return get().getServletContext();
    }

    public boolean isAsyncStarted() {
        return get().isAsyncStarted();
    }

    public boolean isAsyncSupported() {
        return get().isAsyncSupported();
    }

    public AsyncContext startAsync() {
        return get().startAsync();
    }

    public AsyncContext startAsync(ServletRequest request, ServletResponse response) {
        return get().startAsync(request, response);
    }

    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return get().authenticate(response);
    }

    public Part getPart(String name) throws IOException, ServletException {
        return get().getPart(name);
    }

    public Collection<Part> getParts() throws IOException, ServletException {
        return get().getParts();
    }

    public void login(String username, String password) throws ServletException {
        get().login(username, password);
    }

    public void logout() throws ServletException {
        get().logout();
    }

    //Servlet 3.1 additions
    public long getContentLengthLong() {
        return get().getContentLengthLong();
    }
    public String changeSessionId() {
        return get().changeSessionId();
    }
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException,
        ServletException {
        return get().upgrade(handlerClass);
    }
    @Override
    public String getRequestId() {
        return get().getRequestId();
    }
    
    @Override
    public ServletConnection getServletConnection() {
        return get().getServletConnection();
    }
    
    @Override
    public String getProtocolRequestId() {
        return get().getProtocolRequestId();
    }
    
}
