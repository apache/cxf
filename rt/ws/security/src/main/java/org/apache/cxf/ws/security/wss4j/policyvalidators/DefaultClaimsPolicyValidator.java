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

package org.apache.cxf.ws.security.wss4j.policyvalidators;

import java.net.URI;
import java.util.List;

import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;

/**
 * Validate a WS-SecurityPolicy Claims policy for the
 * "http://schemas.xmlsoap.org/ws/2005/05/identity" namespace.
 */
public class DefaultClaimsPolicyValidator implements ClaimsPolicyValidator {

    private static final String DEFAULT_CLAIMS_NAMESPACE =
        "http://schemas.xmlsoap.org/ws/2005/05/identity";

    /**
     * Validate a particular Claims policy against a received SAML Assertion.
     * Return true if the policy is valid.
     */
    public boolean validatePolicy(
        Element claimsPolicy,
        SamlAssertionWrapper assertion
    ) {
        if (claimsPolicy == null) {
            return false;
        }

        String dialect = claimsPolicy.getAttributeNS(null, "Dialect");
        if (!DEFAULT_CLAIMS_NAMESPACE.equals(dialect)) {
            return false;
        }

        Element claimType = DOMUtils.getFirstElement(claimsPolicy);
        while (claimType != null) {
            if ("ClaimType".equals(claimType.getLocalName())) {
                String claimTypeUri = claimType.getAttributeNS(null, "Uri");
                String claimTypeOptional = claimType.getAttributeNS(null, "Optional");

                if (("".equals(claimTypeOptional) || !Boolean.parseBoolean(claimTypeOptional))
                    && !findClaimInAssertion(assertion, URI.create(claimTypeUri))) {
                    return false;
                }
            }

            claimType = DOMUtils.getNextElement(claimType);
        }

        return true;
    }

    /**
     * Return the dialect that this ClaimsPolicyValidator can parse
     */
    public String getDialect() {
        return DEFAULT_CLAIMS_NAMESPACE;
    }

    private boolean findClaimInAssertion(SamlAssertionWrapper assertion, URI claimURI) {
        if (assertion.getSaml1() != null) {
            return findClaimInAssertion(assertion.getSaml1(), claimURI);
        } else if (assertion.getSaml2() != null) {
            return findClaimInAssertion(assertion.getSaml2(), claimURI);
        }
        return false;
    }

    private boolean findClaimInAssertion(org.opensaml.saml.saml2.core.Assertion assertion, URI claimURI) {
        List<org.opensaml.saml.saml2.core.AttributeStatement> attributeStatements =
            assertion.getAttributeStatements();
        if (attributeStatements == null || attributeStatements.isEmpty()) {
            return false;
        }

        for (org.opensaml.saml.saml2.core.AttributeStatement statement : attributeStatements) {

            List<org.opensaml.saml.saml2.core.Attribute> attributes = statement.getAttributes();
            for (org.opensaml.saml.saml2.core.Attribute attribute : attributes) {

                if (attribute.getName().equals(claimURI.toString())
                    && attribute.getAttributeValues() != null && !attribute.getAttributeValues().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean findClaimInAssertion(org.opensaml.saml.saml1.core.Assertion assertion, URI claimURI) {
        List<org.opensaml.saml.saml1.core.AttributeStatement> attributeStatements =
            assertion.getAttributeStatements();
        if (attributeStatements == null || attributeStatements.isEmpty()) {
            return false;
        }

        for (org.opensaml.saml.saml1.core.AttributeStatement statement : attributeStatements) {

            List<org.opensaml.saml.saml1.core.Attribute> attributes = statement.getAttributes();
            for (org.opensaml.saml.saml1.core.Attribute attribute : attributes) {

                URI attributeNamespace = URI.create(attribute.getAttributeNamespace());
                String desiredRole = attributeNamespace.relativize(claimURI).toString();
                if (attribute.getAttributeName().equals(desiredRole)
                    && attribute.getAttributeValues() != null && !attribute.getAttributeValues().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
