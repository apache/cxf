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

import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.ws.security.policy.SPConstants.IncludeTokenType;
import org.apache.cxf.ws.security.policy.model.Token;
import org.apache.ws.security.WSDerivedKeyTokenPrincipal;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.saml.SAMLKeyInfo;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;

/**
 * Some abstract functionality for validating SAML Assertions
 */
public abstract class AbstractSamlPolicyValidator extends AbstractTokenPolicyValidator {
    
    /**
     * Check to see if a token is required or not.
     * @param token the token
     * @param message The message
     * @return true if the token is required
     */
    protected boolean isTokenRequired(
        Token token,
        Message message
    ) {
        IncludeTokenType inclusion = token.getInclusion();
        if (inclusion == IncludeTokenType.INCLUDE_TOKEN_NEVER) {
            return false;
        } else if (inclusion == IncludeTokenType.INCLUDE_TOKEN_ALWAYS) {
            return true;
        } else {
            boolean initiator = MessageUtils.isRequestor(message);
            if (initiator && (inclusion == IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_INITIATOR)) {
                return true;
            } else if (!initiator && (inclusion == IncludeTokenType.INCLUDE_TOKEN_ONCE
                || inclusion == IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT)) {
                return true;
            }
            return false;
        }
    }
    
    /**
     * Check the holder-of-key requirements against the received assertion. The subject
     * credential of the SAML Assertion must have been used to sign some portion of
     * the message, thus showing proof-of-possession of the private/secret key. Alternatively,
     * the subject credential of the SAML Assertion must match a client certificate credential
     * when 2-way TLS is used.
     * @param assertionWrapper the SAML Assertion wrapper object
     * @param signedResults a list of all of the signed results
     */
    public boolean checkHolderOfKey(
        AssertionWrapper assertionWrapper,
        List<WSSecurityEngineResult> signedResults,
        Certificate[] tlsCerts
    ) {
        List<String> confirmationMethods = assertionWrapper.getConfirmationMethods();
        for (String confirmationMethod : confirmationMethods) {
            if (OpenSAMLUtil.isMethodHolderOfKey(confirmationMethod)) {
                if (tlsCerts == null && (signedResults == null || signedResults.isEmpty())) {
                    return false;
                }
                SAMLKeyInfo subjectKeyInfo = assertionWrapper.getSubjectKeyInfo();
                if (!compareCredentials(subjectKeyInfo, signedResults, tlsCerts)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Compare the credentials of the assertion to the credentials used in 2-way TLS or those
     * used to verify signatures.
     * Return true on a match
     * @param subjectKeyInfo the SAMLKeyInfo object
     * @param signedResults a list of all of the signed results
     * @return true if the credentials of the assertion were used to verify a signature
     */
    private boolean compareCredentials(
        SAMLKeyInfo subjectKeyInfo,
        List<WSSecurityEngineResult> signedResults,
        Certificate[] tlsCerts
    ) {
        X509Certificate[] subjectCerts = subjectKeyInfo.getCerts();
        PublicKey subjectPublicKey = subjectKeyInfo.getPublicKey();
        byte[] subjectSecretKey = subjectKeyInfo.getSecret();
        
        //
        // Try to match the TLS certs first
        //
        if (tlsCerts != null && tlsCerts.length > 0 && subjectCerts != null 
            && subjectCerts.length > 0 && tlsCerts[0].equals(subjectCerts[0])) {
            return true;
        } else if (tlsCerts != null && tlsCerts.length > 0 && subjectPublicKey != null
            && tlsCerts[0].getPublicKey().equals(subjectPublicKey)) {
            return true;
        }
        
        //
        // Now try the message-level signatures
        //
        for (WSSecurityEngineResult signedResult : signedResults) {
            X509Certificate[] certs =
                (X509Certificate[])signedResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATES);
            PublicKey publicKey =
                (PublicKey)signedResult.get(WSSecurityEngineResult.TAG_PUBLIC_KEY);
            byte[] secretKey =
                (byte[])signedResult.get(WSSecurityEngineResult.TAG_SECRET);
            if (certs != null && certs.length > 0 && subjectCerts != null
                && subjectCerts.length > 0 && certs[0].equals(subjectCerts[0])) {
                return true;
            }
            if (publicKey != null && publicKey.equals(subjectPublicKey)) {
                return true;
            }
            if (checkSecretKey(secretKey, subjectSecretKey, signedResult)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean checkSecretKey(
        byte[] secretKey,
        byte[] subjectSecretKey,
        WSSecurityEngineResult signedResult
    ) {
        if (secretKey != null && subjectSecretKey != null) {
            if (Arrays.equals(secretKey, subjectSecretKey)) {
                return true;
            } else {
                Principal principal =
                    (Principal)signedResult.get(WSSecurityEngineResult.TAG_PRINCIPAL);
                if (principal instanceof WSDerivedKeyTokenPrincipal) {
                    secretKey = ((WSDerivedKeyTokenPrincipal)principal).getSecret();
                    if (Arrays.equals(secretKey, subjectSecretKey)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
