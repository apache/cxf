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

import javax.xml.namespace.QName;

import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.model.AbstractToken;
import org.apache.wss4j.policy.model.AbstractTokenWrapper;
import org.apache.wss4j.policy.model.AsymmetricBinding;
import org.apache.wss4j.policy.model.X509Token;

/**
 * Validate an AsymmetricBinding policy.
 */
public class AsymmetricBindingPolicyValidator extends AbstractBindingPolicyValidator {

    /**
     * Return true if this SecurityPolicyValidator implementation is capable of validating a
     * policy defined by the AssertionInfo parameter
     */
    public boolean canValidatePolicy(AssertionInfo assertionInfo) {
        return assertionInfo.getAssertion() != null
            && (SP12Constants.ASYMMETRIC_BINDING.equals(assertionInfo.getAssertion().getName())
                || SP11Constants.ASYMMETRIC_BINDING.equals(assertionInfo.getAssertion().getName()));
    }

    /**
     * Validate policies.
     */
    public void validatePolicies(PolicyValidatorParameters parameters, Collection<AssertionInfo> ais) {
        boolean hasDerivedKeys =
            parameters.getResults().getActionResults().containsKey(WSConstants.DKT);

        for (AssertionInfo ai : ais) {
            AsymmetricBinding binding = (AsymmetricBinding)ai.getAssertion();
            ai.setAsserted(true);

            // Check the protection order
            if (!checkProtectionOrder(binding, parameters.getAssertionInfoMap(), ai,
                                      parameters.getResults().getResults())) {
                continue;
            }

            // Check various properties of the binding
            if (!checkProperties(binding, ai, parameters.getAssertionInfoMap(), parameters.getResults(),
                                 parameters.getSignedResults(), parameters.getMessage())) {
                continue;
            }

            // Check various tokens of the binding
            if (!checkTokens(binding, ai, parameters.getAssertionInfoMap(), hasDerivedKeys,
                             parameters.getSignedResults(), parameters.getEncryptedResults())) {
                continue;
            }
        }
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
            result &= checkInitiatorTokens(binding.getInitiatorToken(), ai, aim, hasDerivedKeys,
                                        signedResults, encryptedResults);
        }
        if (binding.getInitiatorSignatureToken() != null) {
            result &= checkInitiatorTokens(binding.getInitiatorSignatureToken(), ai, aim,
                                        hasDerivedKeys, signedResults, encryptedResults);
        }
        if (binding.getInitiatorEncryptionToken() != null) {
            result &= checkInitiatorTokens(binding.getInitiatorEncryptionToken(), ai, aim,
                                        hasDerivedKeys, signedResults, encryptedResults);
        }
        if (binding.getRecipientToken() != null) {
            result &= checkRecipientTokens(binding.getRecipientToken(), ai, aim, hasDerivedKeys,
                                        signedResults, encryptedResults);
        }
        if (binding.getRecipientSignatureToken() != null) {
            result &= checkRecipientTokens(binding.getRecipientSignatureToken(), ai, aim,
                                        hasDerivedKeys, signedResults, encryptedResults);
        }
        if (binding.getRecipientEncryptionToken() != null) {
            result &= checkRecipientTokens(binding.getRecipientEncryptionToken(), ai, aim,
                                        hasDerivedKeys, signedResults, encryptedResults);
        }

        return result;
    }

    private boolean checkInitiatorTokens(
        AbstractTokenWrapper wrapper,
        AssertionInfo ai,
        AssertionInfoMap aim,
        boolean hasDerivedKeys,
        List<WSSecurityEngineResult> signedResults,
        List<WSSecurityEngineResult> encryptedResults) {

        AbstractToken token = wrapper.getToken();
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
                unassertPolicy(aim, wrapper.getName(), error);
                ai.setNotAsserted(error);
                return false;
            }
        }
        PolicyUtils.assertPolicy(aim, wrapper.getName());
        if (!checkDerivedKeys(wrapper, hasDerivedKeys, signedResults, encryptedResults)) {
            ai.setNotAsserted("Message fails the DerivedKeys requirement");
            return false;
        }
        assertDerivedKeys(wrapper.getToken(), aim);

        return true;
    }

    private void unassertPolicy(AssertionInfoMap aim, QName q, String msg) {
        Collection<AssertionInfo> ais = aim.get(q);
        if (ais != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                ai.setNotAsserted(msg);
            }
        }
    }

    private boolean checkRecipientTokens(
        AbstractTokenWrapper wrapper,
        AssertionInfo ai,
        AssertionInfoMap aim,
        boolean hasDerivedKeys,
        List<WSSecurityEngineResult> signedResults,
        List<WSSecurityEngineResult> encryptedResults) {

        PolicyUtils.assertPolicy(aim, wrapper.getName());
        if (!checkDerivedKeys(wrapper, hasDerivedKeys, signedResults, encryptedResults)) {
            ai.setNotAsserted("Message fails the DerivedKeys requirement");
            return false;
        }
        assertDerivedKeys(wrapper.getToken(), aim);

        return true;
    }

}
