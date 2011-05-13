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
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.IssuedToken;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.saml.SAMLKeyInfo;
import org.apache.ws.security.saml.ext.AssertionWrapper;

import org.opensaml.common.SAMLVersion;

/**
 * Validate a WSSecurityEngineResult corresponding to the processing of a SAML Assertion
 * against an IssuedToken policy.
 */
public class IssuedTokenPolicyValidator extends AbstractSamlPolicyValidator {
    
    private List<WSSecurityEngineResult> signedResults;
    private Message message;

    public IssuedTokenPolicyValidator(
        List<WSSecurityEngineResult> signedResults,
        Message message
    ) {
        this.signedResults = signedResults;
        this.message = message;
    }
    
    public boolean validatePolicy(
        AssertionInfoMap aim,
        WSSecurityEngineResult wser
    ) {
        Collection<AssertionInfo> issuedAis = aim.get(SP12Constants.ISSUED_TOKEN);
        if (issuedAis != null && !issuedAis.isEmpty()) {
            for (AssertionInfo ai : issuedAis) {
                AssertionWrapper assertionWrapper = 
                    (AssertionWrapper)wser.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
                IssuedToken issuedToken = (IssuedToken)ai.getAssertion();
                ai.setAsserted(true);
                
                boolean tokenRequired = isTokenRequired(issuedToken, message);
                if ((tokenRequired && assertionWrapper == null) 
                    || (!tokenRequired && assertionWrapper != null)) {
                    ai.setNotAsserted(
                        "The received token does not match the token inclusion requirement"
                    );
                    return false;
                }
                if (!tokenRequired) {
                    continue;
                }
                
                Element template = issuedToken.getRstTemplate();
                if (template != null && !checkIssuedTokenTemplate(template, assertionWrapper)) {
                    ai.setNotAsserted("Error in validating the IssuedToken policy");
                    return false;
                }
                
                TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
                Certificate[] tlsCerts = null;
                if (tlsInfo != null) {
                    tlsCerts = tlsInfo.getPeerCertificates();
                }
                if (!checkHolderOfKey(assertionWrapper, signedResults, tlsCerts)) {
                    ai.setNotAsserted("Assertion fails holder-of-key requirements");
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Check the issued token template against the received assertion
     */
    private boolean checkIssuedTokenTemplate(Element template, AssertionWrapper assertionWrapper) {
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
            }
            child = DOMUtils.getNextElement(child);
        }
        return true;
    }
   
}
