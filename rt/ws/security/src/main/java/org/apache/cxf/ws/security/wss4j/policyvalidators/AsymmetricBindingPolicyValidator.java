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

import org.w3c.dom.Element;

import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.AsymmetricBinding;
import org.apache.cxf.ws.security.policy.model.Token;
import org.apache.cxf.ws.security.policy.model.TokenWrapper;
import org.apache.cxf.ws.security.policy.model.X509Token;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;

/**
 * Validate an AsymmetricBinding policy.
 */
public class AsymmetricBindingPolicyValidator extends AbstractBindingPolicyValidator {
    
    public boolean validatePolicy(
        AssertionInfoMap aim,
        Message message,
        Element soapBody,
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults,
        List<WSSecurityEngineResult> encryptedResults
    ) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.ASYMMETRIC_BINDING);
        if (ais == null || ais.isEmpty()) {                       
            return true;
        }
        
        boolean hasDerivedKeys = false;
        for (WSSecurityEngineResult result : results) {
            Integer actInt = (Integer)result.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.DKT) {
                hasDerivedKeys = true;
                break;
            }
        }
        
        for (AssertionInfo ai : ais) {
            AsymmetricBinding binding = (AsymmetricBinding)ai.getAssertion();
            ai.setAsserted(true);

            // Check the protection order
            if (!checkProtectionOrder(binding, ai, results)) {
                return false;
            }
            
            // Check various properties of the binding
            if (!checkProperties(binding, ai, aim, results, signedResults, message)) {
                return false;
            }
            
            // Check various tokens of the binding
            if (!checkTokens(binding, ai, aim, hasDerivedKeys, signedResults, encryptedResults)) {
                return false;
            }
        }
        
        return true;
    }
    
    
    /**
     * Check various tokens of the binding
     */
    private boolean checkTokens(
        AsymmetricBinding binding, 
        AssertionInfo ai,
        AssertionInfoMap aim,
        boolean hasDerivedKeys,
        List<WSSecurityEngineResult> signedResults,
        List<WSSecurityEngineResult> encryptedResults
    ) {
        boolean result = true;
        if (binding.getInitiatorToken() != null) {
            result &= checkInitiatorTokens(binding.getInitiatorToken(), binding, ai, aim, hasDerivedKeys,
                                        signedResults, encryptedResults);            
        }
        if (binding.getInitiatorSignatureToken() != null) {
            result &= checkInitiatorTokens(binding.getInitiatorSignatureToken(), binding, ai, aim,
                                        hasDerivedKeys, signedResults, encryptedResults);
        }
        if (binding.getInitiatorEncryptionToken() != null) {
            result &= checkInitiatorTokens(binding.getInitiatorEncryptionToken(), binding, ai, aim,
                                        hasDerivedKeys, signedResults, encryptedResults);
        }
        if (binding.getRecipientToken() != null) {
            result &= checkRecipientTokens(binding.getRecipientToken(), binding, ai, aim, hasDerivedKeys,
                                        signedResults, encryptedResults);
        }
        if (binding.getRecipientSignatureToken() != null) {
            result &= checkRecipientTokens(binding.getRecipientSignatureToken(), binding, ai, aim,
                                        hasDerivedKeys, signedResults, encryptedResults);
        }
        if (binding.getRecipientEncryptionToken() != null) {
            result &= checkRecipientTokens(binding.getRecipientEncryptionToken(), binding, ai, aim,
                                        hasDerivedKeys, signedResults, encryptedResults);
        }
        
        return result;
    }
    
    private boolean checkInitiatorTokens(
        TokenWrapper wrapper, 
        AsymmetricBinding binding, 
        AssertionInfo ai,
        AssertionInfoMap aim, 
        boolean hasDerivedKeys,
        List<WSSecurityEngineResult> signedResults,
        List<WSSecurityEngineResult> encryptedResults) {

        Token token = wrapper.getToken();
        if (token instanceof X509Token) {
            boolean foundCert = false;
            for (WSSecurityEngineResult result : signedResults) {
                X509Certificate cert = (X509Certificate)result
                    .get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
                if (cert != null) {
                    foundCert = true;
                    break;
                }
            }
            if (!foundCert && !signedResults.isEmpty()) {
                String error = "An X.509 certificate was not used for the " + wrapper.getName();
                notAssertPolicy(aim, wrapper.getName(), error);
                ai.setNotAsserted(error);
                return false;
            }
        }
        assertPolicy(aim, wrapper);
        if (!checkDerivedKeys(wrapper, hasDerivedKeys, signedResults, encryptedResults)) {
            ai.setNotAsserted("Message fails the DerivedKeys requirement");
            return false;
        }

        return true;
    }

    private boolean checkRecipientTokens(
        TokenWrapper wrapper, 
        AsymmetricBinding binding, 
        AssertionInfo ai,
        AssertionInfoMap aim, 
        boolean hasDerivedKeys,
        List<WSSecurityEngineResult> signedResults,
        List<WSSecurityEngineResult> encryptedResults) {

        assertPolicy(aim, wrapper);
        if (!checkDerivedKeys(wrapper, hasDerivedKeys, signedResults, encryptedResults)) {
            ai.setNotAsserted("Message fails the DerivedKeys requirement");
            return false;
        }

        return true;
    }
    
}
