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

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSSecurityEngineResult;
import org.apache.wss4j.dom.message.token.BinarySecurity;
import org.apache.wss4j.dom.message.token.X509Security;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.X509Token;
import org.apache.wss4j.policy.model.X509Token.TokenType;

/**
 * Validate an X509 Token policy.
 */
public class X509TokenPolicyValidator extends AbstractTokenPolicyValidator implements TokenPolicyValidator {
    
    private static final Logger LOG = LogUtils.getL7dLogger(X509TokenPolicyValidator.class);
    
    private static final String X509_V3_VALUETYPE = WSConstants.X509TOKEN_NS + "#X509v3";
    private static final String PKI_VALUETYPE = WSConstants.X509TOKEN_NS + "#X509PKIPathv1";
    
    public boolean validatePolicy(
        AssertionInfoMap aim,
        Message message,
        Element soapBody,
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults
    ) {
        Collection<AssertionInfo> ais = getAllAssertionsByLocalname(aim, SPConstants.X509_TOKEN);
        if (!ais.isEmpty()) {
            parsePolicies(ais, message, results);
            
            assertPolicy(aim, SPConstants.WSS_X509_PKI_PATH_V1_TOKEN10);
            assertPolicy(aim, SPConstants.WSS_X509_PKI_PATH_V1_TOKEN11);
            assertPolicy(aim, SPConstants.WSS_X509_V1_TOKEN10);
            assertPolicy(aim, SPConstants.WSS_X509_V1_TOKEN11);
            assertPolicy(aim, SPConstants.WSS_X509_V3_TOKEN10);
            assertPolicy(aim, SPConstants.WSS_X509_V3_TOKEN11);
        }
        
        return true;
    }
    
    private void parsePolicies(
        Collection<AssertionInfo> ais, 
        Message message,
        List<WSSecurityEngineResult> results
    ) {
        List<WSSecurityEngineResult> bstResults = 
            WSSecurityUtil.fetchAllActionResults(results, WSConstants.BST);
        
        for (AssertionInfo ai : ais) {
            X509Token x509TokenPolicy = (X509Token)ai.getAssertion();
            ai.setAsserted(true);

            if (!isTokenRequired(x509TokenPolicy, message)) {
                continue;
            }

            if (bstResults.isEmpty()) {
                ai.setNotAsserted(
                    "The received token does not match the token inclusion requirement"
                );
                continue;
            }

            if (!checkTokenType(x509TokenPolicy.getTokenType(), bstResults)) {
                ai.setNotAsserted("An incorrect X.509 Token Type is detected");
                continue;
            }
        }
    }
    
    /**
     * Check that at least one received token matches the token type.
     */
    private boolean checkTokenType(
        TokenType tokenType,
        List<WSSecurityEngineResult> bstResults
    ) {
        if (bstResults.isEmpty()) {
            return false;
        }

        String requiredType = X509_V3_VALUETYPE;
        boolean v3certRequired = false;
        if (tokenType == TokenType.WssX509PkiPathV1Token10
            || tokenType == TokenType.WssX509PkiPathV1Token11) {
            requiredType = PKI_VALUETYPE;
        } else if (tokenType == TokenType.WssX509V3Token10 
            || tokenType == TokenType.WssX509V3Token11) {
            v3certRequired = true;
        }

        for (WSSecurityEngineResult result : bstResults) {
            BinarySecurity binarySecurityToken = 
                (BinarySecurity)result.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
            if (binarySecurityToken != null) {
                String type = binarySecurityToken.getValueType();
                if (requiredType.equals(type)) {
                    if (v3certRequired && binarySecurityToken instanceof X509Security) {
                        try {
                            X509Certificate cert = 
                                 ((X509Security)binarySecurityToken).getX509Certificate(null);
                            if (cert != null && cert.getVersion() == 3) {
                                return true;
                            }
                        } catch (WSSecurityException e) {
                            LOG.log(Level.FINE, e.getMessage());
                        }
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
