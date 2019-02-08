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
package org.apache.cxf.rt.security.saml.claims;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Element;

import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.claims.ClaimsSecurityContext;

public class SAMLSecurityContext implements ClaimsSecurityContext {

    private final Principal principal;
    private Set<Principal> roles;
    private Element assertionElement;
    private String issuer;
    private ClaimCollection claims;

    public SAMLSecurityContext(Principal principal) {
        this(principal, null);
    }

    public SAMLSecurityContext(
        Principal principal,
        Set<Principal> roles
    ) {
        this(principal, roles, null);
    }

    public SAMLSecurityContext(
        Principal principal,
        Set<Principal> roles,
        ClaimCollection claims
    ) {
        this.principal = principal;
        this.roles = roles;
        this.claims = claims;
    }

    public ClaimCollection getClaims() {
        return claims;
    }

    public Principal getUserPrincipal() {
        return principal;
    }

    public boolean isUserInRole(String role) {
        if (roles == null) {
            return false;
        }
        for (Principal principalRole : roles) {
            if (principalRole != principal && principalRole.getName().equals(role)) {
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
        if (roles == null) {
            return Collections.emptySet();
        }
        Set<Principal> retRoles = new HashSet<>(roles);
        if (principal != null && retRoles.contains(principal)) {
            retRoles.remove(principal);
        }
        return retRoles;
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
