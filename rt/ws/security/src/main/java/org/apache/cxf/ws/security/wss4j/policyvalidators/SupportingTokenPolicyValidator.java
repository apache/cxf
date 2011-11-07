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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Element;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.KerberosToken;
import org.apache.cxf.ws.security.policy.model.SamlToken;
import org.apache.cxf.ws.security.policy.model.SupportingToken;
import org.apache.cxf.ws.security.policy.model.Token;
import org.apache.cxf.ws.security.policy.model.UsernameToken;
import org.apache.cxf.ws.security.policy.model.X509Token;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDataRef;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.ws.security.message.token.KerberosSecurity;
import org.apache.ws.security.message.token.PKIPathSecurity;
import org.apache.ws.security.message.token.X509Security;
import org.apache.ws.security.util.WSSecurityUtil;

/**
 * Validate various SupportingToken policies.
 */
public class SupportingTokenPolicyValidator extends AbstractTokenPolicyValidator {
    
    private Message message;
    private List<WSSecurityEngineResult> results;
    private List<WSSecurityEngineResult> signedResults;
    private List<WSSecurityEngineResult> encryptedResults;
    private boolean tls;
    private boolean validateUsernameToken = true;

    public SupportingTokenPolicyValidator(
        Message message,
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults
    ) {
        this.message = message;
        this.results = results;
        this.signedResults = signedResults;
        
        // Store the encryption results
        encryptedResults = new ArrayList<WSSecurityEngineResult>();
        for (WSSecurityEngineResult result : results) {
            Integer actInt = (Integer)result.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.ENCR) {
                encryptedResults.add(result);
            }
        }
        
        // See whether TLS is in use or not
        TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
        if (tlsInfo != null) {
            tls = true;
        }
    }
    
    public void setValidateUsernameToken(boolean validateUsernameToken) {
        this.validateUsernameToken = validateUsernameToken;
    }
    
    public boolean validatePolicy(
        AssertionInfoMap aim
    ) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.SIGNED_SUPPORTING_TOKENS);
        if (ais == null || ais.isEmpty()) {                       
            return true;
        }
        
        for (AssertionInfo ai : ais) {
            SupportingToken binding = (SupportingToken)ai.getAssertion();
            if (SPConstants.SupportTokenType.SUPPORTING_TOKEN_SIGNED != binding.getTokenType()) {
                continue;
            }
            ai.setAsserted(true);
            
            List<Token> tokens = binding.getTokens();
            for (Token token : tokens) {
                if (!isTokenRequired(token, message)) {
                    continue;
                }
                if (token instanceof UsernameToken && !processUsernameTokens()) {
                    ai.setNotAsserted(
                        "The received token does not match the supporting token requirement"
                    );
                    return false;
                } else if (token instanceof SamlToken && !processSAMLTokens()) {
                    ai.setNotAsserted(
                         "The received token does not match the supporting token requirement"
                    );
                    return false;
                } else if (token instanceof KerberosToken && !processKerberosTokens()) {
                    ai.setNotAsserted(
                         "The received token does not match the supporting token requirement"
                    );
                    return false;
                } else if (token instanceof X509Token && !processX509Tokens()) {
                    ai.setNotAsserted(
                        "The received token does not match the supporting token requirement"
                    );
                    return false;
                }
            }

        }
        
        return true;
    }
    
    private boolean processUsernameTokens() {
        if (!validateUsernameToken) {
            return true;
        }
        List<WSSecurityEngineResult> tokenResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.UT, tokenResults);
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.UT_NOPASSWORD, tokenResults);
        
        if (tokenResults.isEmpty()) {
            return false;
        }
        
        return areTokensSigned(tokenResults);
    }
    
    private boolean processSAMLTokens() {
        List<WSSecurityEngineResult> tokenResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.ST_SIGNED, tokenResults);
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.ST_UNSIGNED, tokenResults);
        
        if (tokenResults.isEmpty()) {
            return false;
        }
        
        return areTokensSigned(tokenResults);
    }
    
    private boolean processKerberosTokens() {
        List<WSSecurityEngineResult> tokenResults = new ArrayList<WSSecurityEngineResult>();
        for (WSSecurityEngineResult wser : results) {
            Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.BST) {
                BinarySecurity binarySecurity = 
                    (BinarySecurity)wser.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
                if (binarySecurity instanceof KerberosSecurity) {
                    tokenResults.add(wser);
                }
            }
        }
        
        if (tokenResults.isEmpty()) {
            return false;
        }
        
        return areTokensSigned(tokenResults);
    }
    
    private boolean processX509Tokens() {
        List<WSSecurityEngineResult> tokenResults = new ArrayList<WSSecurityEngineResult>();
        for (WSSecurityEngineResult wser : results) {
            Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.BST) {
                BinarySecurity binarySecurity = 
                    (BinarySecurity)wser.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
                if (binarySecurity instanceof X509Security
                    || binarySecurity instanceof PKIPathSecurity) {
                    tokenResults.add(wser);
                }
            }
        }
        
        if (tokenResults.isEmpty()) {
            return false;
        }
        
        return areTokensSigned(tokenResults);
    }
    
    /**
     * Return true if a list of tokens were signed, false otherwise.
     */
    private boolean areTokensSigned(List<WSSecurityEngineResult> tokens) {
        if (tls) {
            return true;
        }
        for (WSSecurityEngineResult wser : tokens) {
            Element tokenElement = (Element)wser.get(WSSecurityEngineResult.TAG_TOKEN_ELEMENT);
            if (!isTokenSigned(tokenElement)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Return true if a token was signed, false otherwise.
     */
    private boolean isTokenSigned(Element token) {
        if (tls) {
            return true;
        }
        for (WSSecurityEngineResult signedResult : signedResults) {
            List<WSDataRef> dataRefs = 
                CastUtils.cast((List<?>)signedResult.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
            for (WSDataRef dataRef : dataRefs) {
                if (token == dataRef.getProtectedElement()) {
                    return true;
                }
            }
        }
        return false;
    }
    
}
