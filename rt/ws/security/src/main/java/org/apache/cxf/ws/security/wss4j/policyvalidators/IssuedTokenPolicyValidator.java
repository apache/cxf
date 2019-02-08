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

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.saml.SAMLKeyInfo;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.token.BinarySecurity;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.IssuedToken;
import org.opensaml.saml.common.SAMLVersion;

/**
 * Validate a WSSecurityEngineResult corresponding to the processing of a SAML Assertion
 * or Kerberos token against an IssuedToken policy.
 */
public class IssuedTokenPolicyValidator extends AbstractSamlPolicyValidator {

    private ClaimsPolicyValidator claimsValidator = new DefaultClaimsPolicyValidator();

    /**
     * Return true if this SecurityPolicyValidator implementation is capable of validating a
     * policy defined by the AssertionInfo parameter
     */
    public boolean canValidatePolicy(AssertionInfo assertionInfo) {
        return assertionInfo.getAssertion() != null
            && (SP12Constants.ISSUED_TOKEN.equals(assertionInfo.getAssertion().getName())
                || SP11Constants.ISSUED_TOKEN.equals(assertionInfo.getAssertion().getName()));
    }

    /**
     * Validate policies.
     */
    public void validatePolicies(PolicyValidatorParameters parameters,
                                    Collection<AssertionInfo> ais) {
        List<WSSecurityEngineResult> samlResults = parameters.getSamlResults();
        if (samlResults != null) {
            for (WSSecurityEngineResult samlResult : samlResults) {
                SamlAssertionWrapper samlAssertion =
                    (SamlAssertionWrapper)samlResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
                if (validateSAMLToken(parameters, samlAssertion, ais)) {
                    // Store token on the security context
                    SecurityToken token = createSecurityToken(samlAssertion);
                    parameters.getMessage().getExchange().put(SecurityConstants.TOKEN, token);
                    return;
                }
            }
        }

        List<WSSecurityEngineResult> bstResults =
            parameters.getResults().getActionResults().get(WSConstants.BST);

        if (bstResults != null) {
            for (WSSecurityEngineResult bstResult : bstResults) {
                BinarySecurity binarySecurity =
                    (BinarySecurity)bstResult.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
                if (Boolean.TRUE.equals(bstResult.get(WSSecurityEngineResult.TAG_VALIDATED_TOKEN))
                    && validateBinarySecurityToken(parameters, binarySecurity, ais)) {
                    // Store token on the security context
                    SecurityToken token = createSecurityToken(binarySecurity);
                    parameters.getMessage().getExchange().put(SecurityConstants.TOKEN, token);
                    return;
                }
            }
        }
    }

    private boolean validateSAMLToken(PolicyValidatorParameters parameters,
                                      SamlAssertionWrapper samlAssertion,
                                      Collection<AssertionInfo> ais) {
        boolean asserted = true;
        for (AssertionInfo ai : ais) {
            IssuedToken issuedToken = (IssuedToken)ai.getAssertion();
            ai.setAsserted(true);
            assertToken(issuedToken, parameters.getAssertionInfoMap());

            if (!isTokenRequired(issuedToken, parameters.getMessage())) {
                continue;
            }

            if (samlAssertion == null) {
                asserted = false;
                ai.setNotAsserted(
                    "The received token does not match the token inclusion requirement"
                );
                continue;
            }

            Element template = issuedToken.getRequestSecurityTokenTemplate();
            if (template != null && !checkIssuedTokenTemplate(template, samlAssertion)) {
                asserted = false;
                ai.setNotAsserted("Error in validating the IssuedToken policy");
                continue;
            }

            Element claims = issuedToken.getClaims();
            if (claims != null) {
                String dialect = claims.getAttributeNS(null, "Dialect");
                if (claimsValidator.getDialect().equals(dialect)
                    && !claimsValidator.validatePolicy(claims, samlAssertion)) {
                    asserted = false;
                    ai.setNotAsserted("Error in validating the Claims policy");
                    continue;
                }
            }

            TLSSessionInfo tlsInfo = parameters.getMessage().get(TLSSessionInfo.class);
            Certificate[] tlsCerts = null;
            if (tlsInfo != null) {
                tlsCerts = tlsInfo.getPeerCertificates();
            }
            if (!checkHolderOfKey(samlAssertion, parameters.getSignedResults(), tlsCerts)) {
                asserted = false;
                ai.setNotAsserted("Assertion fails holder-of-key requirements");
                continue;
            }

        }

        return asserted;
    }

    private boolean validateBinarySecurityToken(PolicyValidatorParameters parameters,
                                                BinarySecurity binarySecurity,
                                                Collection<AssertionInfo> ais) {
        boolean asserted = true;
        for (AssertionInfo ai : ais) {
            IssuedToken issuedToken = (IssuedToken)ai.getAssertion();
            ai.setAsserted(true);
            asserted = true;
            assertToken(issuedToken, parameters.getAssertionInfoMap());

            if (!isTokenRequired(issuedToken, parameters.getMessage())) {
                continue;
            }
            if (binarySecurity == null) {
                asserted = false;
                ai.setNotAsserted(
                    "The received token does not match the token inclusion requirement"
                );
                continue;
            }

            Element template = issuedToken.getRequestSecurityTokenTemplate();
            if (template != null && !checkIssuedTokenTemplate(template, binarySecurity)) {
                asserted = false;
                ai.setNotAsserted("Error in validating the IssuedToken policy");
                continue;
            }
        }

        return asserted;
    }

    private void assertToken(IssuedToken token, AssertionInfoMap aim) {
        String namespace = token.getName().getNamespaceURI();

        if (token.isRequireExternalReference()) {
            PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.REQUIRE_EXTERNAL_REFERENCE));
        }
        if (token.isRequireInternalReference()) {
            PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.REQUIRE_INTERNAL_REFERENCE));
        }
    }

    /**
     * Check the issued token template against the received assertion
     */
    private boolean checkIssuedTokenTemplate(Element template, SamlAssertionWrapper assertionWrapper) {
        Element child = DOMUtils.getFirstElement(template);
        while (child != null) {
            if ("TokenType".equals(child.getLocalName())) {
                String content = child.getTextContent();
                if (WSS4JConstants.WSS_SAML_TOKEN_TYPE.equals(content)
                    && assertionWrapper.getSamlVersion() != SAMLVersion.VERSION_11) {
                    return false;
                } else if (WSS4JConstants.WSS_SAML2_TOKEN_TYPE.equals(content)
                    && assertionWrapper.getSamlVersion() != SAMLVersion.VERSION_20) {
                    return false;
                }
            } else if ("KeyType".equals(child.getLocalName())) {
                String content = child.getTextContent();
                if (content.endsWith("SymmetricKey")) {
                    SAMLKeyInfo subjectKeyInfo = assertionWrapper.getSubjectKeyInfo();
                    if (subjectKeyInfo == null || subjectKeyInfo.getSecret() == null) {
                        return false;
                    }
                } else if (content.endsWith("PublicKey")) {
                    SAMLKeyInfo subjectKeyInfo = assertionWrapper.getSubjectKeyInfo();
                    if (subjectKeyInfo == null || (subjectKeyInfo.getPublicKey() == null
                        && subjectKeyInfo.getCerts() == null)) {
                        return false;
                    }
                }
            } else if ("Claims".equals(child.getLocalName())) {
                String dialect = child.getAttributeNS(null, "Dialect");
                if (claimsValidator.getDialect().equals(dialect)
                    && !claimsValidator.validatePolicy(child, assertionWrapper)) {
                    return false;
                }
            }
            child = DOMUtils.getNextElement(child);
        }
        return true;
    }

    /**
     * Check the issued token template against the received BinarySecurityToken
     */
    private boolean checkIssuedTokenTemplate(Element template, BinarySecurity binarySecurityToken) {
        Element child = DOMUtils.getFirstElement(template);
        while (child != null) {
            if ("TokenType".equals(child.getLocalName())) {
                String content = child.getTextContent();
                String valueType = binarySecurityToken.getValueType();
                if (!content.equals(valueType)) {
                    return false;
                }
            }
            child = DOMUtils.getNextElement(child);
        }
        return true;
    }

    private SecurityToken createSecurityToken(
        SamlAssertionWrapper assertionWrapper
    ) {
        SecurityToken token = new SecurityToken(assertionWrapper.getId());

        SAMLKeyInfo subjectKeyInfo = assertionWrapper.getSubjectKeyInfo();
        if (subjectKeyInfo != null) {
            token.setSecret(subjectKeyInfo.getSecret());
            X509Certificate[] certs = subjectKeyInfo.getCerts();
            if (certs != null && certs.length > 0) {
                token.setX509Certificate(certs[0], null);
            }
            if (subjectKeyInfo.getPublicKey() != null) {
                token.setKey(subjectKeyInfo.getPublicKey());
            }
        }
        if (assertionWrapper.getSaml1() != null) {
            token.setTokenType(WSS4JConstants.WSS_SAML_TOKEN_TYPE);
        } else if (assertionWrapper.getSaml2() != null) {
            token.setTokenType(WSS4JConstants.WSS_SAML2_TOKEN_TYPE);
        }
        token.setToken(assertionWrapper.getElement());

        return token;
    }

    private SecurityToken createSecurityToken(BinarySecurity binarySecurityToken) {
        SecurityToken token = new SecurityToken(binarySecurityToken.getID());
        token.setToken(binarySecurityToken.getElement());
        token.setSecret(binarySecurityToken.getToken());
        token.setTokenType(binarySecurityToken.getValueType());

        return token;
    }

}
