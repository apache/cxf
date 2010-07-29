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
package org.apache.cxf.transport.http;

import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

public class HttpServletRequestSnapshot extends HttpServletRequestWrapper {
    private String authType;
    private String characterEncoding;
    private int contentLength;
    private String contentType;
    private String contextPath;
    private Cookie[] cookies;
    private String localAddr;
    private Locale local;
    @SuppressWarnings("unchecked")
    private Enumeration locals;
    private String localName;
    private int localPort = -1;
    private String method;
    private String pathInfo;
    private String pathTranslated;
    private String protocol;
    private String queryString;
    private String remoteAddr;
    private String remoteHost;
    private int remotePort = -1;
    private String remoteUser;
    private String requestURI;
    private StringBuffer requestURL;
    private String schema;
    private String serverName;
    private int serverPort = -1;
    private String servletPath;
    private HttpSession session;
    private Principal principal;
    private Enumeration<String> requestHeaderNames;
    private Map<String, Enumeration<String>> headersMap = 
        new java.util.concurrent.ConcurrentHashMap<String, Enumeration<String>>();
    private String requestedSessionId;

    @SuppressWarnings("unchecked")
    public HttpServletRequestSnapshot(HttpServletRequest request) {
        super(request);
        authType = request.getAuthType();
        characterEncoding = request.getCharacterEncoding();
        contentLength = request.getContentLength();
        contentType = request.getContentType();
        contextPath = request.getContextPath();
        cookies = request.getCookies();
        requestHeaderNames = request.getHeaderNames();
        Enumeration<String> tmp = (Enumeration<String>)request.getHeaderNames();
        while (tmp.hasMoreElements()) {
            String key = tmp.nextElement();
            headersMap.put(key, request.getHeaders(key));
        }
        localAddr = request.getLocalAddr();
        local = request.getLocale();
        localName = request.getLocalName();
        localPort = request.getLocalPort();
        method = request.getMethod();
        pathInfo = request.getPathInfo();
        pathTranslated = request.getPathTranslated();
        protocol = request.getProtocol();
        queryString = request.getQueryString();
        remoteAddr = request.getRemoteAddr();
        remoteHost = request.getRemoteHost();
        remotePort = request.getRemotePort();
        remoteUser = request.getRemoteUser();
        requestURI = request.getRequestURI();
        requestURL = request.getRequestURL();
        requestedSessionId = request.getRequestedSessionId();
        schema = request.getScheme();
        serverName = request.getServerName();
        serverPort = request.getServerPort();
        servletPath = request.getServletPath();
        session = request.getSession();
        principal = request.getUserPrincipal();
    }

    @Override
    public String getAuthType() {
        return this.authType;
    }

    @Override
    public String getContextPath() {
        return this.contextPath;
    }

    @Override
    public Cookie[] getCookies() {
        return this.cookies;
    }

    @Override
    public String getHeader(String name) {
        if (headersMap.get(name) != null && headersMap.get(name).hasMoreElements()) {
            return headersMap.get(name).nextElement();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration getHeaderNames() {
        return this.requestHeaderNames;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration getHeaders(String name) {
        return headersMap.get(name);
    }

    @Override
    public String getMethod() {
        return this.method;
    }

    @Override
    public String getPathInfo() {
        return this.pathInfo;
    }

    @Override
    public String getPathTranslated() {
        return this.pathTranslated;
    }

    @Override
    public String getQueryString() {
        return this.queryString;
    }

    @Override
    public String getRemoteUser() {
        return this.remoteUser;
    }

    @Override
    public String getRequestURI() {
        return this.requestURI;
    }

    @Override
    public StringBuffer getRequestURL() {
        return this.requestURL;
    }

    @Override
    public String getRequestedSessionId() {
        return this.requestedSessionId;
    }

    @Override
    public String getServletPath() {
        return this.servletPath;
    }

    @Override
    public HttpSession getSession() {
        return this.session;
    }

    @Override
    public Principal getUserPrincipal() {
        return this.principal;
    }

    @Override
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    @Override
    public int getContentLength() {
        return this.contentLength;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public String getLocalAddr() {
        return this.localAddr;
    }

    @Override
    public String getLocalName() {
        return this.localName;
    }

    @Override
    public int getLocalPort() {
        return this.localPort;
    }

    @Override
    public Locale getLocale() {
        return this.local;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration getLocales() {
        return this.locals;
    }

    @Override
    public String getProtocol() {
        return this.protocol;
    }

    @Override
    public String getRemoteAddr() {
        return this.remoteAddr;
    }

    @Override
    public String getRemoteHost() {
        return this.remoteHost;
    }

    @Override
    public int getRemotePort() {
        return this.remotePort;
    }

    @Override
    public String getScheme() {
        return this.schema;
    }

    @Override
    public String getServerName() {
        return this.serverName;
    }

    @Override
    public int getServerPort() {
        return this.serverPort;
    }
}
