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

package org.apache.cxf.transport.http.netty.server.servlet;


import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.transport.http.netty.server.util.Utils;


public class NettyServletContext implements ServletContext {

    private static final Logger LOG =
            LogUtils.getL7dLogger(NettyServletContext.class);

    private Map<String, Object> attributes;

    private Map<String, String> initParameters;

    private String servletContextName;

    private final String contextPath;

    public NettyServletContext(String contextPath) {
        this.contextPath = contextPath;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes != null ? attributes.get(name) : null;
    }

    @Override
    public Enumeration getAttributeNames() {
        return Utils.enumerationFromKeys(attributes);
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public int getMajorVersion() {
        return 2;
    }

    @Override
    public int getMinorVersion() {
        return 4;
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        return NettyServletContext.class.getResource(path);
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        return NettyServletContext.class.getResourceAsStream(path);
    }

    @Override
    public String getServerInfo() {
        return "Netty Servlet";
    }

    public void addInitParameter(String name, String value) {
        if (this.initParameters == null) {
            this.initParameters = new HashMap<String, String>();
        }
        this.initParameters.put(name, value);
    }

    @Override
    public String getInitParameter(String name) {
        if (this.initParameters == null) {
            return null;
        }
        return this.initParameters.get(name);
    }

    @Override
    public Enumeration getInitParameterNames() {
        return Utils.enumerationFromKeys(this.initParameters);
    }

    @Override
    public void log(String msg) {
        LOG.info(msg);
    }

    @Override
    public void log(Exception exception, String msg) {
        LOG.log(Level.SEVERE, msg, exception);
    }

    @Override
    public void log(String message, Throwable throwable) {
        LOG.log(Level.SEVERE, message, throwable);
    }

    @Override
    public void removeAttribute(String name) {
        if (this.attributes != null) {
            this.attributes.remove(name);
        }
    }

    @Override
    public void setAttribute(String name, Object object) {
        if (this.attributes == null) {
            this.attributes = new HashMap<String, Object>();
        }
        this.attributes.put(name, object);
    }

    @Override
    public String getServletContextName() {
        return this.servletContextName;
    }

    void setServletContextName(String servletContextName) {
        this.servletContextName = servletContextName;
    }

    @Override
    public Servlet getServlet(String name) throws ServletException {
        throw new IllegalStateException(
                "Deprecated as of Java Servlet API 2.1, with no direct replacement!");
    }

    @Override
    public Enumeration getServletNames() {
        throw new IllegalStateException(
                "Method 'getServletNames' deprecated as of Java Servlet API 2.0, with no replacement.");
    }

    @Override
    public Enumeration getServlets() {
        throw new IllegalStateException(
                "Method 'getServlets' deprecated as of Java Servlet API 2.0, with no replacement.");
    }

    @Override
    public ServletContext getContext(String uripath) {
        return this;
    }

    @Override
    public String getMimeType(String file) {
        return Utils.getMimeType(file);

    }

    @Override
    public Set getResourcePaths(String path) {
        throw new IllegalStateException(
                "Method 'getResourcePaths' not yet implemented!");
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        throw new IllegalStateException(
                "Method 'getNamedDispatcher' not yet implemented!");
    }

    @Override
    public String getRealPath(String path) {
        throw new IllegalStateException(
                "Method 'getRealPath' not yet implemented!");
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        throw new IllegalStateException(
                "Method 'getRequestDispatcher' not yet implemented!");
    }
}
