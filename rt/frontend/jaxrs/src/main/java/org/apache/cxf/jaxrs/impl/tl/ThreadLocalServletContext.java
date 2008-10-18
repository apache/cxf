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
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class ThreadLocalServletContext extends AbstractThreadLocalProxy<ServletContext> 
    implements ServletContext {

    public Object getAttribute(String name) {
        return get().getAttribute(name);
    }

    public Enumeration getAttributeNames() {
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

    public Enumeration getInitParameterNames() {
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

    public Set getResourcePaths(String path) {
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
    public Enumeration getServletNames() {
        return get().getServletNames();
    }

    @SuppressWarnings("deprecation")
    public Enumeration getServlets() {
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

}
