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
package org.apache.cxf.sts.token.validator;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.saml.claims.SAMLSecurityContext;
import org.apache.cxf.rt.security.saml.utils.SAMLUtils;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;

/**
 * A default implementation to extract roles from a SAML Assertion
 */
public class DefaultSAMLRoleParser extends DefaultSubjectRoleParser implements SAMLRoleParser {
    /**
     * This configuration tag specifies the default attribute name where the roles are present
     * The default is "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role".
     */
    public static final String SAML_ROLE_ATTRIBUTENAME_DEFAULT =
        "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

    private boolean useJaasSubject = true;
    private String roleAttributeName = SAML_ROLE_ATTRIBUTENAME_DEFAULT;

    /**
     * Return the set of User/Principal roles from the Assertion.
     * @param principal the Principal associated with the Assertion
     * @param subject the JAAS Subject associated with a successful validation of the Assertion
     * @param assertion The Assertion object
     * @return the set of User/Principal roles from the Assertion.
     */
    public Set<Principal> parseRolesFromAssertion(
        Principal principal, Subject subject, SamlAssertionWrapper assertion
    ) {
        if (subject != null && useJaasSubject) {
            return super.parseRolesFromSubject(principal, subject);
        }

        ClaimCollection claims = SAMLUtils.getClaims(assertion);
        Set<Principal> roles =
            SAMLUtils.parseRolesFromClaims(claims, roleAttributeName, null);

        SAMLSecurityContext context =
            new SAMLSecurityContext(principal, roles, claims);

        return context.getUserRoles();
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

    public String getRoleAttributeName() {
        return roleAttributeName;
    }

    /**
     * Set the attribute URI of the SAML AttributeStatement where the role information is stored.
     * The default is "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role".
     * @param roleAttributeName the Attribute URI where role information is stored
     */
    public void setRoleAttributeName(String roleAttributeName) {
        this.roleAttributeName = roleAttributeName;
    }

}