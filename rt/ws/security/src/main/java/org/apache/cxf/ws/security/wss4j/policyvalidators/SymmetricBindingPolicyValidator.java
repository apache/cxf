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

import java.util.Collection;
import java.util.List;

import org.w3c.dom.Element;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSSecurityEngineResult;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.SymmetricBinding;

/**
 * Validate a SymmetricBinding policy.
 */
public class SymmetricBindingPolicyValidator extends AbstractBindingPolicyValidator {
    
    public boolean validatePolicy(
        AssertionInfoMap aim,
        Message message,
        Element soapBody,
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults,
        List<WSSecurityEngineResult> encryptedResults
    ) {
        Collection<AssertionInfo> ais = 
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SYMMETRIC_BINDING);
        if (!ais.isEmpty()) {                       
            parsePolicies(aim, ais, message, soapBody, results, signedResults, encryptedResults);
        }
        
        return true;
    }
    
    private void parsePolicies(
        AssertionInfoMap aim,
        Collection<AssertionInfo> ais, 
        Message message,
        Element soapBody,
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults,
        List<WSSecurityEngineResult> encryptedResults
    ) {
        boolean hasDerivedKeys = false;
        for (WSSecurityEngineResult result : results) {
            Integer actInt = (Integer)result.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.DKT) {
                hasDerivedKeys = true;
                break;
            }
        }
        
        for (AssertionInfo ai : ais) {
            SymmetricBinding binding = (SymmetricBinding)ai.getAssertion();
            ai.setAsserted(true);

            // Check the protection order
            if (!checkProtectionOrder(binding, aim, ai, results)) {
                continue;
            }
            
            // Check various properties of the binding
            if (!checkProperties(binding, ai, aim, results, signedResults, message)) {
                continue;
            }
            
            // Check various tokens of the binding
            if (!checkTokens(binding, ai, aim, hasDerivedKeys, signedResults, encryptedResults)) {
                continue;
            }
        }
    }
    
    /**
     * Check various tokens of the binding
     */
    private boolean checkTokens(
        SymmetricBinding binding, 
        AssertionInfo ai,
        AssertionInfoMap aim,
        boolean hasDerivedKeys,
        List<WSSecurityEngineResult> signedResults,
        List<WSSecurityEngineResult> encryptedResults
    ) {
        if (binding.getEncryptionToken() != null) {
            PolicyUtils.assertPolicy(aim, binding.getEncryptionToken().getName());
            if (!checkDerivedKeys(
                binding.getEncryptionToken(), hasDerivedKeys, signedResults, encryptedResults
            )) {
                ai.setNotAsserted("Message fails the DerivedKeys requirement");
                return false;
            }
            PolicyUtils.assertPolicy(aim, SPConstants.REQUIRE_DERIVED_KEYS);
            PolicyUtils.assertPolicy(aim, SPConstants.REQUIRE_IMPLIED_DERIVED_KEYS);
            PolicyUtils.assertPolicy(aim, SPConstants.REQUIRE_EXPLICIT_DERIVED_KEYS);
        }
        
        if (binding.getSignatureToken() != null) {
            PolicyUtils.assertPolicy(aim, binding.getSignatureToken().getName());
            if (!checkDerivedKeys(
                binding.getSignatureToken(), hasDerivedKeys, signedResults, encryptedResults
            )) {
                ai.setNotAsserted("Message fails the DerivedKeys requirement");
                return false;
            }
            PolicyUtils.assertPolicy(aim, SPConstants.REQUIRE_DERIVED_KEYS);
            PolicyUtils.assertPolicy(aim, SPConstants.REQUIRE_IMPLIED_DERIVED_KEYS);
            PolicyUtils.assertPolicy(aim, SPConstants.REQUIRE_EXPLICIT_DERIVED_KEYS);
        }
        
        if (binding.getProtectionToken() != null) {
            PolicyUtils.assertPolicy(aim, binding.getProtectionToken().getName());
            if (!checkDerivedKeys(
                binding.getProtectionToken(), hasDerivedKeys, signedResults, encryptedResults
            )) {
                ai.setNotAsserted("Message fails the DerivedKeys requirement");
                return false;
            }
            PolicyUtils.assertPolicy(aim, SPConstants.REQUIRE_DERIVED_KEYS);
            PolicyUtils.assertPolicy(aim, SPConstants.REQUIRE_IMPLIED_DERIVED_KEYS);
            PolicyUtils.assertPolicy(aim, SPConstants.REQUIRE_EXPLICIT_DERIVED_KEYS);
        }
        
        return true;
    }
    
}
