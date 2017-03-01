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

import javax.xml.namespace.QName;

import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SP13Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractSecurityAssertion;
import org.apache.wss4j.policy.model.SupportingTokens;
import org.apache.wss4j.policy.model.UsernameToken.PasswordType;
import org.apache.wss4j.policy.model.UsernameToken.UsernameTokenType;

/**
 * Validate a UsernameToken policy.
 */
public class UsernameTokenPolicyValidator extends AbstractSecurityPolicyValidator {

    /**
     * Return true if this SecurityPolicyValidator implementation is capable of validating a
     * policy defined by the AssertionInfo parameter
     */
    public boolean canValidatePolicy(AssertionInfo assertionInfo) {
        return assertionInfo.getAssertion() != null
            && (SP12Constants.USERNAME_TOKEN.equals(assertionInfo.getAssertion().getName())
                || SP11Constants.USERNAME_TOKEN.equals(assertionInfo.getAssertion().getName()));
    }

    /**
     * Validate policies. W
     */
    public void validatePolicies(PolicyValidatorParameters parameters, Collection<AssertionInfo> ais) {
        for (AssertionInfo ai : ais) {
            org.apache.wss4j.policy.model.UsernameToken usernameTokenPolicy =
                (org.apache.wss4j.policy.model.UsernameToken)ai.getAssertion();
            ai.setAsserted(true);
            assertToken(usernameTokenPolicy, parameters.getAssertionInfoMap());

            if (!isTokenRequired(usernameTokenPolicy, parameters.getMessage())) {
                continue;
            }

            if (parameters.getUsernameTokenResults().isEmpty()) {
                ai.setNotAsserted(
                    "The received token does not match the token inclusion requirement"
                );
                continue;
            }

            if (!checkTokens(usernameTokenPolicy, ai, parameters.getUsernameTokenResults())) {
                continue;
            }
        }
    }

    private void assertToken(org.apache.wss4j.policy.model.UsernameToken token, AssertionInfoMap aim) {
        String namespace = token.getName().getNamespaceURI();

        if (token.isCreated()) {
            PolicyUtils.assertPolicy(aim, SP13Constants.CREATED);
        }
        if (token.isNonce()) {
            PolicyUtils.assertPolicy(aim, SP13Constants.NONCE);
        }

        PasswordType passwordType = token.getPasswordType();
        if (passwordType != null) {
            PolicyUtils.assertPolicy(aim, new QName(namespace, passwordType.name()));
        }

        UsernameTokenType usernameTokenType = token.getUsernameTokenType();
        if (usernameTokenType != null) {
            PolicyUtils.assertPolicy(aim, new QName(namespace, usernameTokenType.name()));
        }
    }

    /**
     * All UsernameTokens must conform to the policy
     */
    public boolean checkTokens(
        org.apache.wss4j.policy.model.UsernameToken usernameTokenPolicy,
        AssertionInfo ai,
        List<WSSecurityEngineResult> utResults
    ) {
        for (WSSecurityEngineResult result : utResults) {
            UsernameToken usernameToken =
                (UsernameToken)result.get(WSSecurityEngineResult.TAG_USERNAME_TOKEN);
            PasswordType passwordType = usernameTokenPolicy.getPasswordType();
            boolean isHashPassword = passwordType == PasswordType.HashPassword;
            boolean isNoPassword = passwordType == PasswordType.NoPassword;
            if (isHashPassword != usernameToken.isHashed()) {
                ai.setNotAsserted("Password hashing policy not enforced");
                return false;
            }

            if (isNoPassword && (usernameToken.getPassword() != null)) {
                ai.setNotAsserted("Username Token NoPassword policy not enforced");
                return false;
            } else if (!isNoPassword && (usernameToken.getPassword() == null)
                && isNonEndorsingSupportingToken(usernameTokenPolicy)) {
                ai.setNotAsserted("Username Token No Password supplied");
                return false;
            }

            if (usernameTokenPolicy.isCreated()
                && (usernameToken.getCreated() == null || usernameToken.isHashed())) {
                ai.setNotAsserted("Username Token Created policy not enforced");
                return false;
            }

            if (usernameTokenPolicy.isNonce()
                && (usernameToken.getNonce() == null || usernameToken.isHashed())) {
                ai.setNotAsserted("Username Token Nonce policy not enforced");
                return false;
            }
        }
        return true;
    }

    /**
     * Return true if this UsernameToken policy is a (non-endorsing)SupportingToken. If this is
     * true then the corresponding UsernameToken must have a password element.
     */
    private boolean isNonEndorsingSupportingToken(
        org.apache.wss4j.policy.model.UsernameToken usernameTokenPolicy
    ) {
        AbstractSecurityAssertion parentAssertion = usernameTokenPolicy.getParentAssertion();
        if (parentAssertion instanceof SupportingTokens) {
            SupportingTokens supportingToken = (SupportingTokens)parentAssertion;
            String localname = supportingToken.getName().getLocalPart();
            if (localname.equals(SPConstants.SUPPORTING_TOKENS)
                || localname.equals(SPConstants.SIGNED_SUPPORTING_TOKENS)
                || localname.equals(SPConstants.ENCRYPTED_SUPPORTING_TOKENS)
                || localname.equals(SPConstants.SIGNED_ENCRYPTED_SUPPORTING_TOKENS)) {
                return true;
            }
        }
        return false;
    }

}
