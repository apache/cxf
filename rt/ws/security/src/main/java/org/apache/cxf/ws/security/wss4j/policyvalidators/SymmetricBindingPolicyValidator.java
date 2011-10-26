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

import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.SymmetricBinding;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;

/**
 * Validate a SymmetricBinding policy.
 */
public class SymmetricBindingPolicyValidator extends AbstractBindingPolicyValidator {
    
    private List<WSSecurityEngineResult> signedResults;
    private List<WSSecurityEngineResult> encryptedResults;
    private Message message;
    private boolean hasDerivedKeys;

    public SymmetricBindingPolicyValidator(
        Message message,
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults
    ) {
        this.message = message;
        this.results = results;
        this.signedResults = signedResults;
        
        // Store the encryption results and whether we have any derived key results
        encryptedResults = new ArrayList<WSSecurityEngineResult>();
        for (WSSecurityEngineResult result : results) {
            Integer actInt = (Integer)result.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.DKT) {
                hasDerivedKeys = true;
            } else if (actInt.intValue() == WSConstants.ENCR) {
                encryptedResults.add(result);
            }
        }
    }
    
    public boolean validatePolicy(
        AssertionInfoMap aim
    ) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.SYMMETRIC_BINDING);
        if (ais == null || ais.isEmpty()) {                       
            return true;
        }
        
        for (AssertionInfo ai : ais) {
            SymmetricBinding binding = (SymmetricBinding)ai.getAssertion();
            ai.setAsserted(true);

            // Check the protection order
            if (!checkProtectionOrder(binding, ai)) {
                return false;
            }
            
            // Check various properties of the binding
            if (!checkProperties(binding, ai, aim, signedResults, message)) {
                return false;
            }
            
            // Check various tokens of the binding
            if (!checkTokens(binding, ai, aim)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check various tokens of the binding
     */
    private boolean checkTokens(
        SymmetricBinding binding, 
        AssertionInfo ai,
        AssertionInfoMap aim
    ) {
        if (binding.getEncryptionToken() != null) {
            assertPolicy(aim, binding.getEncryptionToken());
            if (!checkDerivedKeys(
                binding.getEncryptionToken(), hasDerivedKeys, signedResults, encryptedResults
            )) {
                ai.setNotAsserted("Message fails the DerivedKeys requirement");
                return false;
            }
        }
        
        if (binding.getSignatureToken() != null) {
            assertPolicy(aim, binding.getSignatureToken());
            if (!checkDerivedKeys(
                binding.getSignatureToken(), hasDerivedKeys, signedResults, encryptedResults
            )) {
                ai.setNotAsserted("Message fails the DerivedKeys requirement");
                return false;
            }
        }
        
        if (binding.getProtectionToken() != null) {
            assertPolicy(aim, binding.getProtectionToken());
            if (!checkDerivedKeys(
                binding.getProtectionToken(), hasDerivedKeys, signedResults, encryptedResults
            )) {
                ai.setNotAsserted("Message fails the DerivedKeys requirement");
                return false;
            }
        }
        
        return true;
    }
    
}
