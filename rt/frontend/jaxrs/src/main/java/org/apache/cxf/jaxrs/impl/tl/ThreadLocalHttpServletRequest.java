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
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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

    public Enumeration getHeaderNames() {
        return get().getHeaderNames();
    }

    public Enumeration getHeaders(String name) {
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

    @SuppressWarnings("deprecation")
    public boolean isRequestedSessionIdFromUrl() {
        return get().isRequestedSessionIdFromUrl();
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

    public Enumeration getAttributeNames() {
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

    public Enumeration getLocales() {
        return get().getLocales();
    }

    public String getParameter(String name) {
        return get().getParameter(name);
    }

    public Map getParameterMap() {
        return get().getParameterMap();
    }

    public Enumeration getParameterNames() {
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

    @SuppressWarnings("deprecation")
    public String getRealPath(String path) {
        return get().getRealPath(path);
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

}
