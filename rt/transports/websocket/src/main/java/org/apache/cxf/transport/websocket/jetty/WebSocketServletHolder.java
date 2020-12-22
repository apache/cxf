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

import java.io.IOException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContext;

/**
 *
 */
public interface WebSocketServletHolder {
    String getAuthType();
    String getContextPath();
    String getLocalAddr();
    String getLocalName();
    int getLocalPort();
    Locale getLocale();
    Enumeration<Locale> getLocales();
    String getProtocol();
    String getRemoteAddr();
    String getRemoteHost();
    int getRemotePort();
    String getRequestURI();
    StringBuffer getRequestURL();
    DispatcherType getDispatcherType();
    boolean isSecure();
    String getPathInfo();
    String getPathTranslated();
    String getScheme();
    String getServerName();
    String getServletPath();
    ServletContext getServletContext();
    int getServerPort();
    Principal getUserPrincipal();
    Object getAttribute(String name);
    void write(byte[] data, int offset, int length) throws IOException;
}
