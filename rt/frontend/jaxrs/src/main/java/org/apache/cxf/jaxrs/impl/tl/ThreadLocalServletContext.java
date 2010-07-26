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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

public class ThreadLocalServletContext extends AbstractThreadLocalProxy<ServletContext> 
    implements ServletContext {

    public Object getAttribute(String name) {
        return get().getAttribute(name);
    }

    public Enumeration<String> getAttributeNames() {
        return get().getAttributeNames();
    }

    public ServletContext getContext(String uripath) {
        return get().getContext(uripath);
    }

    public String getContextPath() {
        return get().getContextPath();
    }

    public String getInitParameter(String name) {
        return get().getInitParameter(name);
    }

    public Enumeration<String> getInitParameterNames() {
        return get().getInitParameterNames();
    }

    public int getMajorVersion() {
        return get().getMajorVersion();
    }

    public String getMimeType(String file) {
        return get().getMimeType(file);
    }

    public int getMinorVersion() {
        return get().getMinorVersion();
    }

    public RequestDispatcher getNamedDispatcher(String name) {
        return get().getNamedDispatcher(name);
    }

    public String getRealPath(String path) {
        return get().getRealPath(path);
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        return get().getRequestDispatcher(path);
    }

    public URL getResource(String path) throws MalformedURLException {
        return get().getResource(path);
    }

    public InputStream getResourceAsStream(String path) {
        return get().getResourceAsStream(path);
    }

    public Set<String> getResourcePaths(String path) {
        return get().getResourcePaths(path);
    }

    public String getServerInfo() {
        return get().getServerInfo();
    }

    @SuppressWarnings("deprecation")
    public Servlet getServlet(String name) throws ServletException {
        return get().getServlet(name);
    }

    public String getServletContextName() {
        return get().getServletContextName();
    }

    @SuppressWarnings("deprecation")
    public Enumeration<String> getServletNames() {
        return get().getServletNames();
    }

    @SuppressWarnings("deprecation")
    public Enumeration<Servlet> getServlets() {
        return get().getServlets();
    }

    public void log(String msg) {
        get().log(msg);        
    }

    @SuppressWarnings("deprecation")
    public void log(Exception exception, String msg) {
        get().log(exception, msg);
    }

    public void log(String message, Throwable throwable) {
        get().log(message, throwable);
    }

    public void removeAttribute(String name) {
        get().removeAttribute(name);
    }

    public void setAttribute(String name, Object object) {
        get().setAttribute(name, object);
        
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return get().setInitParameter(name, value);
    }

    @Override
    public Dynamic addServlet(String servletName, String className) throws IllegalArgumentException,
        IllegalStateException {
        return get().addServlet(servletName, className);
    }

    @Override
    public Dynamic addServlet(String servletName, Servlet servlet) throws IllegalArgumentException,
        IllegalStateException {
        return get().addServlet(servletName, servlet);
    }

    @Override
    public Dynamic addServlet(String servletName, Class<? extends Servlet> clazz)
        throws IllegalArgumentException, IllegalStateException {
        return get().addServlet(servletName, clazz);
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        return get().createServlet(clazz);
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return get().getServletRegistration(servletName);
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return get().getServletRegistrations();
    }

    @Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className)
        throws IllegalArgumentException, IllegalStateException {
        return get().addFilter(filterName, className);
    }

    @Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter)
        throws IllegalArgumentException, IllegalStateException {
        return get().addFilter(filterName, filter);
    }

    @Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName,
                                                              Class<? extends Filter> filterClass)
        throws IllegalArgumentException, IllegalStateException {
        return get().addFilter(filterName, filterClass);
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        return get().createFilter(clazz);
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return get().getFilterRegistration(filterName);
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return get().getFilterRegistrations();
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        get().addListener(listenerClass);
    }

    @Override
    public void addListener(String className) {
        get().addListener(className);
        
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        get().addListener(t);
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        return get().createListener(clazz);
    }

    @Override
    public void declareRoles(String... roleNames) {
        get().declareRoles(roleNames);
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return get().getSessionCookieConfig();
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        get().setSessionTrackingModes(sessionTrackingModes);
        
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return get().getDefaultSessionTrackingModes();
    }

    @Override
    public int getEffectiveMajorVersion() throws UnsupportedOperationException {
        return get().getEffectiveMajorVersion();
    }

    @Override
    public int getEffectiveMinorVersion() throws UnsupportedOperationException {
        return get().getEffectiveMinorVersion();
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return get().getEffectiveSessionTrackingModes();
    }

    @Override
    public ClassLoader getClassLoader() {
        return get().getClassLoader();
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return get().getJspConfigDescriptor();
    }

}
