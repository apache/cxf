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

package org.apache.cxf.jaxrs.impl;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.message.Message;


public class SecurityContextImpl implements SecurityContext {
    private Message m;

    public SecurityContextImpl(Message m) {
        this.m = m;
    }

    public String getAuthenticationScheme() {
        if (m.get(AuthorizationPolicy.class) != null) {
            return SecurityContext.BASIC_AUTH;
        }
        @SuppressWarnings("unchecked")
        Map<String, List<String>> headers =
            (Map<String, List<String>>)m.get(Message.PROTOCOL_HEADERS);
        if (headers != null) {
            List<String> values = headers.get(HttpHeaders.AUTHORIZATION);
            if (values != null && values.size() == 1) {
                String value = values.get(0);
                if (value != null) {
                    int index = value.trim().indexOf(' ');
                    if (index != -1) {
                        return value.substring(0, index);
                    }
                }
            }
        }
        return null;
    }

    public Principal getUserPrincipal() {
        org.apache.cxf.security.SecurityContext sc = getInternalSecurityContext();
        return sc == null ? null : sc.getUserPrincipal();
    }


    public boolean isSecure() {
        String value = HttpUtils.getEndpointAddress(m);
        return value.startsWith("https://");
    }

    public boolean isUserInRole(String role) {
        org.apache.cxf.security.SecurityContext sc = getInternalSecurityContext();
        return sc != null && sc.isUserInRole(role);
    }

    private org.apache.cxf.security.SecurityContext getInternalSecurityContext() {
        org.apache.cxf.security.SecurityContext sc = m.getContent(org.apache.cxf.security.SecurityContext.class);
        if (sc == null) {
            sc = m.get(org.apache.cxf.security.SecurityContext.class);
        }
        return sc;
    }
}
