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
package org.apache.cxf.interceptor.security;

import java.security.Principal;
import java.util.Set;

import org.w3c.dom.Element;

import org.apache.cxf.security.LoginSecurityContext;

public class SAMLSecurityContext implements LoginSecurityContext {
    
    private final Principal principal;
    private Set<Principal> roles;
    private Element assertionElement;
    private String issuer;
    
    public SAMLSecurityContext(Principal principal) {
        this.principal = principal;
    }
    
    public SAMLSecurityContext(
        Principal principal, 
        Set<Principal> roles
    ) {
        this.principal = principal;
        this.roles = roles;
    }
    
    public Principal getUserPrincipal() {
        return principal;
    }

    public boolean isUserInRole(String role) {
        if (roles == null) {
            return false;
        }
        for (Principal principalRole : roles) {
            if (principalRole.getName().equals(role)) {
                return true;
            }
        }
        return false;
    }
    
    public javax.security.auth.Subject getSubject() {
        return null;
    }

    public void setUserRoles(Set<Principal> userRoles) {
        this.roles = userRoles;
    }
    
    public Set<Principal> getUserRoles() {
        return roles;
    }
    
    public void setAssertionElement(Element assertionElement) {
        this.assertionElement = assertionElement;
    }
    
    public Element getAssertionElement() {
        return assertionElement;
    }
    
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    
    public String getIssuer() {
        return issuer;
    }
}
