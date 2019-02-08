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

import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.model.AbstractToken;
import org.apache.wss4j.policy.model.AbstractToken.DerivedKeys;
import org.apache.wss4j.policy.model.IssuedToken;
import org.apache.wss4j.policy.model.KerberosToken;
import org.apache.wss4j.policy.model.KeyValueToken;
import org.apache.wss4j.policy.model.SamlToken;
import org.apache.wss4j.policy.model.SecurityContextToken;
import org.apache.wss4j.policy.model.SpnegoContextToken;
import org.apache.wss4j.policy.model.SupportingTokens;
import org.apache.wss4j.policy.model.UsernameToken;
import org.apache.wss4j.policy.model.X509Token;

/**
 * Validate a SignedEndorsingSupportingToken policy.
 */
public class SignedEndorsingTokenPolicyValidator extends AbstractSupportingTokenPolicyValidator {

    /**
     * Return true if this SecurityPolicyValidator implementation is capable of validating a
     * policy defined by the AssertionInfo parameter
     */
    public boolean canValidatePolicy(AssertionInfo assertionInfo) {
        return assertionInfo.getAssertion() != null
            && (SP12Constants.SIGNED_ENDORSING_SUPPORTING_TOKENS.equals(assertionInfo.getAssertion().getName())
                || SP11Constants.SIGNED_ENDORSING_SUPPORTING_TOKENS.equals(assertionInfo.getAssertion().getName()));
    }

    /**
     * Validate policies.
     */
    public void validatePolicies(PolicyValidatorParameters parameters, Collection<AssertionInfo> ais) {
        for (AssertionInfo ai : ais) {
            SupportingTokens binding = (SupportingTokens)ai.getAssertion();
            ai.setAsserted(true);

            setSignedParts(binding.getSignedParts());
            setEncryptedParts(binding.getEncryptedParts());
            setSignedElements(binding.getSignedElements());
            setEncryptedElements(binding.getEncryptedElements());

            List<AbstractToken> tokens = binding.getTokens();
            for (AbstractToken token : tokens) {
                if (!isTokenRequired(token, parameters.getMessage())) {
                    assertDerivedKeys(token, parameters.getAssertionInfoMap());
                    assertSecurePartsIfTokenNotRequired(binding, parameters.getAssertionInfoMap());
                    continue;
                }

                DerivedKeys derivedKeys = token.getDerivedKeys();
                boolean derived = derivedKeys == DerivedKeys.RequireDerivedKeys;
                boolean processingFailed = false;
                if (token instanceof KerberosToken) {
                    if (!processKerberosTokens(parameters, derived)) {
                        processingFailed = true;
                    }
                } else if (token instanceof SamlToken) {
                    if (!processSAMLTokens(parameters, derived)) {
                        processingFailed = true;
                    }
                } else if (token instanceof X509Token) {
                    if (!processX509Tokens(parameters, derived)) {
                        processingFailed = true;
                    }
                } else if (token instanceof KeyValueToken) {
                    if (!processKeyValueTokens(parameters)) {
                        processingFailed = true;
                    }
                } else if (token instanceof UsernameToken) {
                    if (!processUsernameTokens(parameters, derived)) {
                        processingFailed = true;
                    }
                } else if (token instanceof SecurityContextToken
                    || token instanceof SpnegoContextToken) {
                    if (!processSCTokens(parameters, derived)) {
                        processingFailed = true;
                    }
                } else if (token instanceof IssuedToken) {
                    IssuedToken issuedToken = (IssuedToken)token;
                    if (isSamlTokenRequiredForIssuedToken(issuedToken) && !processSAMLTokens(parameters, derived)) {
                        processingFailed = true;
                    }
                } else {
                    processingFailed = true;
                }

                if (processingFailed) {
                    ai.setNotAsserted(
                        "The received token does not match the signed endorsing supporting token requirement"
                    );
                    continue;
                }

                if (derived && parameters.getResults().getActionResults().containsKey(WSConstants.DKT)) {
                    assertDerivedKeys(token, parameters.getAssertionInfoMap());
                }
            }
        }
    }

    protected boolean isSigned() {
        return true;
    }

    protected boolean isEncrypted() {
        return false;
    }

    protected boolean isEndorsing() {
        return true;
    }
}
