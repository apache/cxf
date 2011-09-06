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
package org.apache.cxf.rs.security.saml.authorization;

import java.security.Principal;
import java.util.List;

import org.apache.cxf.rs.security.saml.assertion.Claim;
import org.apache.cxf.rs.security.saml.assertion.Claims;
import org.apache.cxf.rs.security.saml.assertion.Subject;
import org.apache.cxf.security.SecurityContext;

public class SAMLSecurityContext implements SecurityContext {
    
    private SubjectPrincipal p;
    private Claims claims; 
    private Claim rolesClaim;
    
    public SAMLSecurityContext(Subject subject, List<Claim> claims) {
        this(new SubjectPrincipal(subject), new Claims(claims));
    }
    
    public SAMLSecurityContext(SubjectPrincipal p, Claims claims) {
        this(p, claims, Claim.DEFAULT_ROLE_NAME, Claim.DEFAULT_NAME_FORMAT);
    }
    
    public SAMLSecurityContext(SubjectPrincipal p, 
                               Claims cs,
                               String roleClaimNameQualifier,
                               String roleClaimNameFormat) {
        this.p = p;
        for (Claim c : cs.getClaims()) {
            if (c.getName().equals(roleClaimNameQualifier)
                && c.getNameFormat().equals(roleClaimNameFormat)) {
                rolesClaim = c;
                break;
            }
        }
        this.claims = cs;
        
    }
    
    public Principal getUserPrincipal() {
        return p;
    }

    public boolean isUserInRole(String role) {
        if (rolesClaim == null) {
            return false;
        }
        for (String r : rolesClaim.getValues()) {
            if (r.equals(role)) {
                return true;
            }
        }
        return false;
    }
    
    public Claims getClaims() {
        return claims;
    }
}
