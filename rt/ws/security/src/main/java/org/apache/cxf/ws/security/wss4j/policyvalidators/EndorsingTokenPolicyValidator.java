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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.KerberosToken;
import org.apache.cxf.ws.security.policy.model.SecurityContextToken;
import org.apache.cxf.ws.security.policy.model.SupportingToken;
import org.apache.cxf.ws.security.policy.model.Token;
import org.apache.cxf.ws.security.policy.model.X509Token;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDataRef;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.ws.security.message.token.KerberosSecurity;
import org.apache.ws.security.message.token.PKIPathSecurity;
import org.apache.ws.security.message.token.Timestamp;
import org.apache.ws.security.message.token.X509Security;
import org.apache.ws.security.util.WSSecurityUtil;

/**
 * Validate an EndorsingSupportingToken policy. 
 */
public class EndorsingTokenPolicyValidator extends AbstractTokenPolicyValidator {
    
    private List<WSSecurityEngineResult> results;
    private List<WSSecurityEngineResult> signedResults;
    private Message message;
    private Element timestamp;
    private boolean tls;
    
    public EndorsingTokenPolicyValidator(
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults,
        Message message
    ) {
        this.results = results;
        this.signedResults = signedResults;
        this.message = message;
        WSSecurityEngineResult result = WSSecurityUtil.fetchActionResult(results, WSConstants.TS);
        if (result != null) {
            Timestamp ts = (Timestamp)result.get(WSSecurityEngineResult.TAG_TIMESTAMP);
            timestamp = ts.getElement();
        }
        
        // See whether TLS is in use or not
        TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
        if (tlsInfo != null) {
            tls = true;
        }
    }
    
    public boolean validatePolicy(
        AssertionInfoMap aim
    ) {
        Collection<AssertionInfo> endorsingAis = aim.get(SP12Constants.ENDORSING_SUPPORTING_TOKENS);
        if (endorsingAis != null && !endorsingAis.isEmpty()) {
            for (AssertionInfo ai : endorsingAis) {
                SupportingToken binding = (SupportingToken)ai.getAssertion();
                if (SPConstants.SupportTokenType.SUPPORTING_TOKEN_ENDORSING != binding.getTokenType()) {
                    continue;
                }
                ai.setAsserted(true);
                
                List<Token> tokens = binding.getTokens();
                for (Token token : tokens) {
                    if (!isTokenRequired(token, message)) {
                        continue;
                    }
                    boolean derived = token.isDerivedKeys();
                    if (token instanceof KerberosToken && !processKerberosTokens(derived)) {
                        ai.setNotAsserted(
                             "The received token does not match the supporting token requirement"
                        );
                        return false;
                    } else if (token instanceof X509Token && !processX509Tokens(derived)) {
                        ai.setNotAsserted(
                            "The received token does not match the supporting token requirement"
                        );
                        return false;
                    } else if (token instanceof SecurityContextToken && !processSCTokens(derived)) {
                        ai.setNotAsserted(
                            "The received token does not match the supporting token requirement"
                        );
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    private boolean processKerberosTokens(boolean derived) {
        List<WSSecurityEngineResult> tokenResults = new ArrayList<WSSecurityEngineResult>();
        for (WSSecurityEngineResult wser : results) {
            Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.BST) {
                BinarySecurity binarySecurity = 
                    (BinarySecurity)wser.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
                if (binarySecurity instanceof KerberosSecurity) {
                    if (derived) {
                        byte[] secret = (byte[])wser.get(WSSecurityEngineResult.TAG_SECRET);
                        WSSecurityEngineResult dktResult = getMatchingDerivedKey(secret);
                        if (dktResult != null) {
                            tokenResults.add(dktResult);
                        }
                    }
                    tokenResults.add(wser);
                }
            }
        }
        
        if (tokenResults.isEmpty()) {
            return false;
        }
        
        return checkEndorsed(tokenResults, tls);
    }
    
    private boolean processX509Tokens(boolean derived) {
        List<WSSecurityEngineResult> tokenResults = new ArrayList<WSSecurityEngineResult>();
        for (WSSecurityEngineResult wser : results) {
            Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.BST) {
                BinarySecurity binarySecurity = 
                    (BinarySecurity)wser.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
                if (binarySecurity instanceof X509Security
                    || binarySecurity instanceof PKIPathSecurity) {
                    if (derived) {
                        WSSecurityEngineResult resultToStore = processX509DerivedTokenResult(wser);
                        if (resultToStore != null) {
                            tokenResults.add(resultToStore);
                        }
                    }
                    tokenResults.add(wser);
                }
            }
        }
        
        if (tokenResults.isEmpty()) {
            return false;
        }
        
        return checkEndorsed(tokenResults, tls);
    }
    
    private WSSecurityEngineResult processX509DerivedTokenResult(WSSecurityEngineResult result) {
        X509Certificate cert = 
            (X509Certificate)result.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
        WSSecurityEngineResult encrResult = getMatchingEncryptedKey(cert);
        if (encrResult != null) {
            byte[] secret = (byte[])encrResult.get(WSSecurityEngineResult.TAG_SECRET);
            WSSecurityEngineResult dktResult = getMatchingDerivedKey(secret);
            if (dktResult != null) {
                return dktResult;
            }
        }
        return null;
    }
    
    private boolean processSCTokens(boolean derived) {
        List<WSSecurityEngineResult> tokenResults = new ArrayList<WSSecurityEngineResult>();
        for (WSSecurityEngineResult wser : results) {
            Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.SCT) {
                if (derived) {
                    byte[] secret = (byte[])wser.get(WSSecurityEngineResult.TAG_SECRET);
                    WSSecurityEngineResult dktResult = getMatchingDerivedKey(secret);
                    if (dktResult != null) {
                        tokenResults.add(dktResult);
                    }
                }
                tokenResults.add(wser);
            }
        }
        
        if (tokenResults.isEmpty()) {
            return false;
        }
        
        return checkEndorsed(tokenResults, tls);
    }
    
    /**
     * Get a security result representing a Derived Key that has a secret key that
     * matches the parameter.
     */
    private WSSecurityEngineResult getMatchingDerivedKey(byte[] secret) {
        for (WSSecurityEngineResult wser : results) {
            Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.DKT) {
                byte[] dktSecret = (byte[])wser.get(WSSecurityEngineResult.TAG_SECRET);
                if (Arrays.equals(secret, dktSecret)) {
                    return wser;
                }
            }
        }
        return null;
    }
    
    /**
     * Get a security result representing an EncryptedKey that matches the parameter.
     */
    private WSSecurityEngineResult getMatchingEncryptedKey(X509Certificate cert) {
        for (WSSecurityEngineResult wser : results) {
            Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.ENCR) {
                X509Certificate encrCert = 
                    (X509Certificate)wser.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
                if (cert.equals(encrCert)) {
                    return wser;
                }
            }
        }
        return null;
    }
    
    /**
     * Check the endorsing supporting token policy. If we're using the Transport Binding then
     * check that the Timestamp is signed. Otherwise, check that the signature is signed.
     * @param transport
     * @return true if the endorsed supporting token policy is correct
     */
    private boolean checkEndorsed(List<WSSecurityEngineResult> tokenResults, boolean transport) {
        if (transport) {
            return checkTimestampIsSigned(tokenResults);
        }
        return checkSignatureIsSigned(tokenResults);
    }
    
    /**
     * Return true if the Timestamp is signed by one of the token results
     * @param tokenResults A list of WSSecurityEngineResults corresponding to tokens
     * @return true if the Timestamp is signed
     */
    private boolean checkTimestampIsSigned(List<WSSecurityEngineResult> tokenResults) {
        for (WSSecurityEngineResult signedResult : signedResults) {
            List<WSDataRef> sl =
                CastUtils.cast((List<?>)signedResult.get(
                    WSSecurityEngineResult.TAG_DATA_REF_URIS
                ));
            if (sl != null) {
                for (WSDataRef dataRef : sl) {
                    if (timestamp == dataRef.getProtectedElement()
                        && checkSignature(signedResult, tokenResults)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Return true if the Signature is itself signed by one of the token results
     * @param tokenResults A list of WSSecurityEngineResults corresponding to tokens
     * @return true if the Signature is itself signed
     */
    private boolean checkSignatureIsSigned(List<WSSecurityEngineResult> tokenResults) {
        for (WSSecurityEngineResult signedResult : signedResults) {
            List<WSDataRef> sl =
                CastUtils.cast((List<?>)signedResult.get(
                    WSSecurityEngineResult.TAG_DATA_REF_URIS
                ));
            if (sl != null && sl.size() == 1) {
                for (WSDataRef dataRef : sl) {
                    QName signedQName = dataRef.getName();
                    if (WSSecurityEngine.SIGNATURE.equals(signedQName)
                        && checkSignature(signedResult, tokenResults)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Check that a WSSecurityEngineResult corresponding to a signature uses the same 
     * signing credential as one of the tokens.
     * @param signatureResult a WSSecurityEngineResult corresponding to a signature
     * @param tokenResult A list of WSSecurityEngineResults corresponding to tokens
     * @return 
     */
    private boolean checkSignature(
        WSSecurityEngineResult signatureResult,
        List<WSSecurityEngineResult> tokenResult
    ) {
        // See what was used to sign this result
        X509Certificate cert = 
            (X509Certificate)signatureResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
        byte[] secret = (byte[])signatureResult.get(WSSecurityEngineResult.TAG_SECRET);
        
        // Now see if the same credential exists in the tokenResult list
        for (WSSecurityEngineResult token : tokenResult) {
            BinarySecurity binarySecurity = 
                (BinarySecurity)token.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
            if (binarySecurity instanceof X509Security
                || binarySecurity instanceof PKIPathSecurity) {
                X509Certificate foundCert = 
                    (X509Certificate)token.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
                if (foundCert.equals(cert)) {
                    return true;
                }
            } else {
                byte[] foundSecret = (byte[])token.get(WSSecurityEngineResult.TAG_SECRET);
                if (foundSecret != null && Arrays.equals(foundSecret, secret)) {
                    return true;
                }
                byte[] derivedKey = 
                    (byte[])token.get(WSSecurityEngineResult.TAG_ENCRYPTED_EPHEMERAL_KEY);
                if (derivedKey != null && Arrays.equals(derivedKey, secret)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
