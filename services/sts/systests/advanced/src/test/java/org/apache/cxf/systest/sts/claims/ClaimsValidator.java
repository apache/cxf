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
package org.apache.cxf.systest.sts.claims;

import java.util.List;

import org.w3c.dom.Element;

import org.apache.cxf.sts.claims.ClaimTypes;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.SamlAssertionValidator;
import org.opensaml.core.xml.XMLObject;

/**
 * This class validates a SAML Assertion and checks that it has an "AuthenticatedRole" attribute
 * corresponding to "admin-user". Note that it only throws an error if the role has the wrong
 * value, not if the role doesn't exist. This is because the WS-SecurityPolicy validation will
 * check to make sure that the correct defined Claims have been met in the token.
 */
public class ClaimsValidator extends SamlAssertionValidator {

    @Override
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        Credential validatedCredential = super.validate(credential, data);
        SamlAssertionWrapper assertion = validatedCredential.getSamlAssertion();

        boolean valid = false;
        if (assertion.getSaml1() != null) {
            valid = handleSAML1Assertion(assertion.getSaml1());
        } else if (assertion.getSaml2() != null) {
            valid = handleSAML2Assertion(assertion.getSaml2());
        }

        if (valid) {
            return validatedCredential;
        }

        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
    }

    private boolean handleSAML1Assertion(
        org.opensaml.saml.saml1.core.Assertion assertion
    ) throws WSSecurityException {
        List<org.opensaml.saml.saml1.core.AttributeStatement> attributeStatements =
            assertion.getAttributeStatements();
        if (attributeStatements == null || attributeStatements.isEmpty()) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
        }

        for (org.opensaml.saml.saml1.core.AttributeStatement statement : attributeStatements) {
            List<org.opensaml.saml.saml1.core.Attribute> attributes = statement.getAttributes();
            for (org.opensaml.saml.saml1.core.Attribute attribute : attributes) {

                if (!ClaimTypes.URI_BASE.toString().equals(attribute.getAttributeNamespace())) {
                    continue;
                }

                for (XMLObject attributeValue : attribute.getAttributeValues()) {
                    Element attributeValueElement = attributeValue.getDOM();
                    String text = attributeValueElement.getTextContent();
                    if (!"admin-user".equals(text)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean handleSAML2Assertion(
        org.opensaml.saml.saml2.core.Assertion assertion
    ) throws WSSecurityException {
        List<org.opensaml.saml.saml2.core.AttributeStatement> attributeStatements =
            assertion.getAttributeStatements();
        if (attributeStatements == null || attributeStatements.isEmpty()) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
        }

        for (org.opensaml.saml.saml2.core.AttributeStatement statement : attributeStatements) {
            List<org.opensaml.saml.saml2.core.Attribute> attributes = statement.getAttributes();
            for (org.opensaml.saml.saml2.core.Attribute attribute : attributes) {
                if (!attribute.getName().startsWith(ClaimTypes.URI_BASE.toString())) {
                    continue;
                }

                for (XMLObject attributeValue : attribute.getAttributeValues()) {
                    Element attributeValueElement = attributeValue.getDOM();
                    String text = attributeValueElement.getTextContent();
                    if (!"admin-user".equals(text)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

}
