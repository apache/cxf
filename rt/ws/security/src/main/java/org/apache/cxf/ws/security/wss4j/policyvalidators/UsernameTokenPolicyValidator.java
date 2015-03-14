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
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSSecurityEngineResult;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.wss4j.policy.SP13Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractSecurityAssertion;
import org.apache.wss4j.policy.model.SupportingTokens;
import org.apache.wss4j.policy.model.UsernameToken.PasswordType;

/**
 * Validate a UsernameToken policy.
 */
public class UsernameTokenPolicyValidator 
    extends AbstractTokenPolicyValidator implements TokenPolicyValidator {
    
    public boolean validatePolicy(
        AssertionInfoMap aim,
        Message message,
        Element soapBody,
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults
    ) {
        Collection<AssertionInfo> ais = 
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.USERNAME_TOKEN);
        if (!ais.isEmpty()) {
            parsePolicies(ais, message, results);
            
            PolicyUtils.assertPolicy(aim, SP13Constants.CREATED);
            PolicyUtils.assertPolicy(aim, SP13Constants.NONCE);
            PolicyUtils.assertPolicy(aim, SPConstants.NO_PASSWORD);
            PolicyUtils.assertPolicy(aim, SPConstants.HASH_PASSWORD);
            PolicyUtils.assertPolicy(aim, SPConstants.USERNAME_TOKEN10);
            PolicyUtils.assertPolicy(aim, SPConstants.USERNAME_TOKEN11);
        }
        
        return true;
    }
    
    private void parsePolicies(
        Collection<AssertionInfo> ais, 
        Message message,
        List<WSSecurityEngineResult> results
    ) {
        final List<Integer> actions = new ArrayList<Integer>(2);
        actions.add(WSConstants.UT);
        actions.add(WSConstants.UT_NOPASSWORD);
        List<WSSecurityEngineResult> utResults = 
            WSSecurityUtil.fetchAllActionResults(results, actions);
        
        for (AssertionInfo ai : ais) {
            org.apache.wss4j.policy.model.UsernameToken usernameTokenPolicy = 
                (org.apache.wss4j.policy.model.UsernameToken)ai.getAssertion();
            ai.setAsserted(true);
            if (!isTokenRequired(usernameTokenPolicy, message)) {
                continue;
            }

            if (utResults.isEmpty()) {
                ai.setNotAsserted(
                    "The received token does not match the token inclusion requirement"
                );
                continue;
            }

            if (!checkTokens(usernameTokenPolicy, ai, utResults)) {
                continue;
            }
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
