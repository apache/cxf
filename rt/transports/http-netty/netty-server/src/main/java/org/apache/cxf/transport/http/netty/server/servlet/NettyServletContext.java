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
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletRegistration.Dynamic;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;
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

    @SuppressWarnings("rawtypes")
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
            this.initParameters = new HashMap<>();
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

    @SuppressWarnings("rawtypes")
    @Override
    public Enumeration getInitParameterNames() {
        return Utils.enumerationFromKeys(this.initParameters);
    }

    @Override
    public void log(String msg) {
        LOG.info(msg);
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
            this.attributes = new HashMap<>();
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
    public ServletContext getContext(String uripath) {
        return this;
    }

    @Override
    public String getMimeType(String file) {
        return Utils.getMimeType(file);

    }

    @SuppressWarnings("rawtypes")
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

    @Override
    public int getEffectiveMajorVersion() {
        throw new IllegalStateException("Method 'getEffectiveMajorVersion' not yet implemented!");

    }

    @Override
    public int getEffectiveMinorVersion() {
        throw new IllegalStateException("Method 'getEffectiveMinorVersion' not yet implemented!");

    }

    @Override
    public boolean setInitParameter(String name, String value) {
        throw new IllegalStateException("Method 'setInitParameter' not yet implemented!");
    }

    @Override
    public Dynamic addServlet(String servletName, String className) {
        throw new IllegalStateException("Method 'addServlet' not yet implemented!");
    }

    @Override
    public Dynamic addServlet(String servletName, Servlet servlet) {
        throw new IllegalStateException("Method 'addServlet' not yet implemented!");
    }

    @Override
    public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        throw new IllegalStateException("Method 'addServlet' not yet implemented!");
    }

    @Override
    public Dynamic addJspFile(String servletName, String jspFile) {
        throw new IllegalStateException("Method 'addJspFile' not yet implemented!");
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        throw new IllegalStateException("Method 'createServlet' not yet implemented!");
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        throw new IllegalStateException("Method 'getServletRegistration' not yet implemented!");
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        throw new IllegalStateException("Method 'getServletRegistrations' not yet implemented!");
    }

    @Override
    public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
        throw new IllegalStateException("Method 'addFilter' not yet implemented!");
    }

    @Override
    public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        throw new IllegalStateException("Method 'addFilter' not yet implemented!");
    }

    @Override
    public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, 
            Class<? extends Filter> filterClass) {
        throw new IllegalStateException("Method 'addFilter' not yet implemented!");
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        throw new IllegalStateException("Method 'createFilter' not yet implemented!");
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        throw new IllegalStateException("Method 'getFilterRegistration' not yet implemented!");
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        throw new IllegalStateException("Method 'getFilterRegistrations' not yet implemented!");
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        throw new IllegalStateException("Method 'getSessionCookieConfig' not yet implemented!");
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        throw new IllegalStateException("Method 'setSessionTrackingModes' not yet implemented!");
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        throw new IllegalStateException("Method 'getDefaultSessionTrackingModes' not yet implemented!");
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        throw new IllegalStateException("Method 'getEffectiveSessionTrackingModes' not yet implemented!");
    }

    @Override
    public void addListener(String className) {
        throw new IllegalStateException("Method 'addListener' not yet implemented!");
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        throw new IllegalStateException("Method 'addListener' not yet implemented!");
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        throw new IllegalStateException("Method 'addListener' not yet implemented!");
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        throw new IllegalStateException("Method 'createListener' not yet implemented!");
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        throw new IllegalStateException("Method 'getJspConfigDescriptor' not yet implemented!");
    }

    @Override
    public ClassLoader getClassLoader() {
        throw new IllegalStateException("Method 'getClassLoader' not yet implemented!");
    }

    @Override
    public void declareRoles(String... roleNames) {
        throw new IllegalStateException("Method 'declareRoles' not yet implemented!");
    }

    @Override
    public String getVirtualServerName() {
        throw new IllegalStateException("Method 'getVirtualServerName' not yet implemented!");
    }

    @Override
    public int getSessionTimeout() {
        throw new IllegalStateException("Method 'getSessionTimeout' not yet implemented!");
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {
        throw new IllegalStateException("Method 'setSessionTimeout' not yet implemented!");
    }

    @Override
    public String getRequestCharacterEncoding() {
        throw new IllegalStateException("Method 'getRequestCharacterEncoding' not yet implemented!");
    }

    @Override
    public void setRequestCharacterEncoding(String encoding) {
        throw new IllegalStateException("Method 'setRequestCharacterEncoding' not yet implemented!");
    }

    @Override
    public String getResponseCharacterEncoding() {
        throw new IllegalStateException("Method 'getResponseCharacterEncoding' not yet implemented!");

    }

    @Override
    public void setResponseCharacterEncoding(String encoding) {
        throw new IllegalStateException("Method 'setResponseCharacterEncoding' not yet implemented!");
    }
}
