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

import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.message.Message;

public class SecurityContextImpl implements SecurityContext {

    private Message m;
    
    public SecurityContextImpl(Message m) {
        this.m = m;
    }

    // TODO
    public String getAuthenticationScheme() {
        if (m.get(AuthorizationPolicy.class) != null) {
            return SecurityContext.BASIC_AUTH;
        }
        return "Unknown scheme";
    }

    public Principal getUserPrincipal() {
        org.apache.cxf.security.SecurityContext sc = 
            (org.apache.cxf.security.SecurityContext)m.get(org.apache.cxf.security.SecurityContext.class);
        return sc == null ? null : sc.getUserPrincipal();
    }

    
    public boolean isSecure() {
        String value = HttpUtils.getEndpointAddress(m);
        return value.startsWith("https://");
    }

    public boolean isUserInRole(String role) {
        org.apache.cxf.security.SecurityContext sc = 
            (org.apache.cxf.security.SecurityContext)m.get(org.apache.cxf.security.SecurityContext.class);
        return sc == null ? false : sc.isUserInRole(role);
    }

}
