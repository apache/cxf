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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.interceptor.security.SAMLSecurityContext;
import org.apache.cxf.rs.security.saml.assertion.Claim;
import org.apache.cxf.rs.security.saml.assertion.Claims;
import org.apache.cxf.rs.security.saml.assertion.Subject;

public class JAXRSSAMLSecurityContext extends SAMLSecurityContext {
    
    private Claims claims;
    
    public JAXRSSAMLSecurityContext(Subject subject, List<Claim> claims) {
        this(new SubjectPrincipal(subject.getName(), subject), new Claims(claims));
    }
    
    public JAXRSSAMLSecurityContext(SubjectPrincipal p, Claims claims) {
        this(p, claims, Claim.DEFAULT_ROLE_NAME, Claim.DEFAULT_NAME_FORMAT);
    }
    
    public JAXRSSAMLSecurityContext(SubjectPrincipal p, 
                               Claims cs,
                               String roleClaimNameQualifier,
                               String roleClaimNameFormat) {
        super(p);
        
        Claim rolesClaim = null;
        for (Claim c : cs.getClaims()) {
            if (c.getName().equals(roleClaimNameQualifier)
                && c.getNameFormat().equals(roleClaimNameFormat)) {
                rolesClaim = c;
                break;
            }
        }
        this.claims = cs;

        Set<Principal> userRoles;
        if (rolesClaim != null) {
            userRoles = new HashSet<Principal>();
            for (String role : rolesClaim.getValues()) {
                userRoles.add(new SimplePrincipal(role));
            }
        } else {
            userRoles = null;
        }
        
        setUserRoles(userRoles);
    }
    
    public Claims getClaims() {
        return claims;
    }

}
