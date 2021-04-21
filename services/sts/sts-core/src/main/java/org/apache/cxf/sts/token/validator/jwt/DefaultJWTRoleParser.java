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
package org.apache.cxf.sts.token.validator.jwt;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.cxf.common.security.SimpleGroup;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.sts.token.validator.DefaultSubjectRoleParser;

/**
 * A default implementation to extract roles from a JWT token
 */
public class DefaultJWTRoleParser extends DefaultSubjectRoleParser implements JWTRoleParser {

    private boolean useJaasSubject = true;
    private String roleClaim;

    /**
     * Return the set of User/Principal roles from the token.
     * @param principal the Principal associated with the token
     * @param subject the JAAS Subject associated with a successful validation of the token
     * @param token The JWTToken
     * @return the set of User/Principal roles from the token.
     */
    public Set<Principal> parseRolesFromToken(
        Principal principal, Subject subject, JwtToken token
    ) {
        if (subject != null && useJaasSubject) {
            return super.parseRolesFromSubject(principal, subject);
        }

        final Set<Principal> roles;
        if (roleClaim != null && token != null && token.getClaims().containsProperty(roleClaim)) {
            roles = new HashSet<>();
            String role = token.getClaims().getStringProperty(roleClaim).trim();
            for (String r : role.split(",")) {
                roles.add(new SimpleGroup(r));
            }
        } else {
            roles = Collections.emptySet();
        }

        return roles;
    }

    public boolean isUseJaasSubject() {
        return useJaasSubject;
    }

    /**
     * Whether to get roles from the JAAS Subject (if not null) returned from SAML Assertion
     * Validation or not. The default is true.
     * @param useJaasSubject whether to get roles from the JAAS Subject or not
     */
    public void setUseJaasSubject(boolean useJaasSubject) {
        this.useJaasSubject = useJaasSubject;
    }

    public String getRoleClaim() {
        return roleClaim;
    }

    public void setRoleClaim(String roleClaim) {
        this.roleClaim = roleClaim;
    }

}