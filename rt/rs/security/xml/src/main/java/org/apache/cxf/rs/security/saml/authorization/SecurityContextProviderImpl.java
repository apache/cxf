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
import java.util.Set;

import org.w3c.dom.Element;

import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.saml.assertion.Subject;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.claims.SAMLClaim;
import org.apache.cxf.rt.security.saml.claims.SAMLSecurityContext;
import org.apache.cxf.rt.security.saml.utils.SAMLUtils;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.builder.SAML2Constants;

public class SecurityContextProviderImpl implements SecurityContextProvider {

    private static final String ROLE_QUALIFIER_PROPERTY = "org.apache.cxf.saml.claims.role.qualifier";
    private static final String ROLE_NAMEFORMAT_PROPERTY = "org.apache.cxf.saml.claims.role.nameformat";

    public SecurityContext getSecurityContext(Message message,
            SamlAssertionWrapper wrapper) {
        // First check to see if we are allowed to set up a security context
        // The SAML Assertion must be signed, or we must explicitly allow unsigned
        String allowUnsigned =
            (String)SecurityUtils.getSecurityPropertyValue(
                SecurityConstants.ENABLE_UNSIGNED_SAML_ASSERTION_PRINCIPAL, message
            );
        boolean allowUnsignedSamlPrincipals = Boolean.parseBoolean(allowUnsigned);
        if (!(wrapper.isSigned() || allowUnsignedSamlPrincipals)) {
            return null;
        }

        ClaimCollection claims = getClaims(wrapper);
        Subject subject = getSubject(message, wrapper, claims);
        SecurityContext securityContext = doGetSecurityContext(message, subject, claims);
        if (securityContext instanceof SAMLSecurityContext) {
            Element assertionElement = wrapper.getElement();
            ((SAMLSecurityContext)securityContext).setAssertionElement(assertionElement);
        }
        return securityContext;
    }

    protected ClaimCollection getClaims(SamlAssertionWrapper wrapper) {
        return SAMLUtils.getClaims(wrapper);
    }

    protected Subject getSubject(Message message, SamlAssertionWrapper wrapper, ClaimCollection claims) {
        return org.apache.cxf.rs.security.saml.SAMLUtils.getSubject(message, wrapper);
    }

    protected SecurityContext doGetSecurityContext(
        Message message, Subject subject, ClaimCollection claims
    ) {
        String defaultRoleName = (String)message.getContextualProperty(ROLE_QUALIFIER_PROPERTY);
        if (defaultRoleName == null) {
            defaultRoleName =
                (String)message.getContextualProperty(SecurityConstants.SAML_ROLE_ATTRIBUTENAME);
        }
        String defaultNameFormat = (String)message.getContextualProperty(ROLE_NAMEFORMAT_PROPERTY);

        String subjectPrincipalName = getSubjectPrincipalName(subject, claims);
        SubjectPrincipal subjectPrincipal =
            new SubjectPrincipal(subjectPrincipalName, subject);

        String roleName =
            defaultRoleName == null ? SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT : defaultRoleName;
        String nameFormat =
            defaultNameFormat == null ? SAML2Constants.ATTRNAME_FORMAT_UNSPECIFIED : defaultNameFormat;
        Set<Principal> roles =
            SAMLUtils.parseRolesFromClaims(claims, roleName, nameFormat);

        return new SAMLSecurityContext(subjectPrincipal, roles, claims);
    }

    //TODO: This can be overridden, but consider also introducing dedicated handlers
    protected String getSubjectPrincipalName(Subject subject, ClaimCollection claims) {
        // parse/decipher subject name, or check claims such as
        // givenName, email, firstName
        // and use it to authenticate with the external system if needed

        // Or if STS has been used to validate the SAML token on the server side then
        // whatever name the subject has provided can probably be used as a principal name
        // as IDP must've confirmed that this subject indeed got authenticated and such...
        return subject.getName();
    }
}
