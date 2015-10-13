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
package org.apache.cxf.rs.security.jose.jaxrs;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.cxf.common.security.SimpleGroup;
import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.security.LoginSecurityContext;

public class JwtTokenSecurityContext implements LoginSecurityContext {
    private final JwtToken token;
    private final Principal principal;
    private final Set<Principal> roles;
    
    public JwtTokenSecurityContext(JwtToken jwt, String roleClaim) {
        principal = new SimplePrincipal(jwt.getClaims().getSubject());
        this.token = jwt;
        if (roleClaim != null && jwt.getClaims().containsProperty(roleClaim)) {
            roles = new HashSet<>();
            String role = jwt.getClaims().getStringProperty(roleClaim).trim();
            for (String r : role.split(",")) {
                roles.add(new SimpleGroup(r));
            }
        } else {
            roles = Collections.emptySet();
        }
    }
    
    public JwtToken getToken() {
        return token;
    }

    @Override
    public Subject getSubject() {
        return null;
    }

    @Override
    public Set<Principal> getUserRoles() {
        return Collections.unmodifiableSet(roles);
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public boolean isUserInRole(String role) {
        for (Principal principalRole : roles) {
            if (principalRole != principal && principalRole.getName().equals(role)) {
                return true;
            }
        }
        return false;
    }

}
