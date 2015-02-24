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
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Element;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.wss4j.common.saml.SAMLKeyInfo;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSSecurityEngineResult;
import org.apache.wss4j.dom.message.token.BinarySecurity;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.IssuedToken;
import org.opensaml.saml.common.SAMLVersion;

/**
 * Validate a WSSecurityEngineResult corresponding to the processing of a SAML Assertion
 * or Kerberos token against an IssuedToken policy.
 */
public class IssuedTokenPolicyValidator extends AbstractSamlPolicyValidator {
    
    private List<WSSecurityEngineResult> signedResults;
    private Message message;
    private ClaimsPolicyValidator claimsValidator = new DefaultClaimsPolicyValidator();

    public IssuedTokenPolicyValidator(
        List<WSSecurityEngineResult> signedResults,
        Message message
    ) {
        this.signedResults = signedResults;
        this.message = message;
    }
    
    public boolean validatePolicy(
        Collection<AssertionInfo> ais,
        SamlAssertionWrapper assertionWrapper
    ) {
        if (ais == null || ais.isEmpty()) {
            return true;
        }
        
        for (AssertionInfo ai : ais) {
            IssuedToken issuedToken = (IssuedToken)ai.getAssertion();
            ai.setAsserted(true);

            if (!isTokenRequired(issuedToken, message)) {
                continue;
            }
            
            if (assertionWrapper == null) {
                ai.setNotAsserted(
                    "The received token does not match the token inclusion requirement"
                );
                continue;
            }

            Element template = issuedToken.getRequestSecurityTokenTemplate();
            if (template != null && !checkIssuedTokenTemplate(template, assertionWrapper)) {
                ai.setNotAsserted("Error in validating the IssuedToken policy");
                continue;
            }
            
            Element claims = issuedToken.getClaims();
            if (claims != null) {
                String dialect = claims.getAttributeNS(null, "Dialect");
                if (claimsValidator.getDialect().equals(dialect)
                    && !claimsValidator.validatePolicy(claims, assertionWrapper)) {
                    ai.setNotAsserted("Error in validating the Claims policy");
                    continue;
                }
            }

            TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
            Certificate[] tlsCerts = null;
            if (tlsInfo != null) {
                tlsCerts = tlsInfo.getPeerCertificates();
            }
            if (!checkHolderOfKey(assertionWrapper, signedResults, tlsCerts)) {
                ai.setNotAsserted("Assertion fails holder-of-key requirements");
                continue;
            }
        }
        
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        assertPolicy(aim, SPConstants.REQUIRE_INTERNAL_REFERENCE);
        assertPolicy(aim, SPConstants.REQUIRE_EXTERNAL_REFERENCE);
        
        return true;
    }
    
    public boolean validatePolicy(
        Collection<AssertionInfo> ais,
        BinarySecurity binarySecurityToken
    ) {
        if (ais == null || ais.isEmpty()) {
            return true;
        }
        
        for (AssertionInfo ai : ais) {
            IssuedToken issuedToken = (IssuedToken)ai.getAssertion();
            ai.setAsserted(true);

            if (!isTokenRequired(issuedToken, message)) {
                continue;
            }
            if (binarySecurityToken == null) {
                ai.setNotAsserted(
                    "The received token does not match the token inclusion requirement"
                );
                return false;
            }

            Element template = issuedToken.getRequestSecurityTokenTemplate();
            if (template != null && !checkIssuedTokenTemplate(template, binarySecurityToken)) {
                ai.setNotAsserted("Error in validating the IssuedToken policy");
                return false;
            }
        }
        
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        assertPolicy(aim, SPConstants.REQUIRE_INTERNAL_REFERENCE);
        assertPolicy(aim, SPConstants.REQUIRE_EXTERNAL_REFERENCE);
        
        return true;
    }
    
    /**
     * Check the issued token template against the received assertion
     */
    private boolean checkIssuedTokenTemplate(Element template, SamlAssertionWrapper assertionWrapper) {
        Element child = DOMUtils.getFirstElement(template);
        while (child != null) {
            if ("TokenType".equals(child.getLocalName())) {
                String content = child.getTextContent();
                if (WSConstants.WSS_SAML_TOKEN_TYPE.equals(content) 
                    && assertionWrapper.getSamlVersion() != SAMLVersion.VERSION_11) {
                    return false;
                } else if (WSConstants.WSS_SAML2_TOKEN_TYPE.equals(content) 
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
    
   
}
