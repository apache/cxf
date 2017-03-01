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
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.model.SecurityContextToken;

/**
 * Validate a SecurityContextToken policy.
 */
public class SecurityContextTokenPolicyValidator extends AbstractSecurityPolicyValidator {

    /**
     * Return true if this SecurityPolicyValidator implementation is capable of validating a
     * policy defined by the AssertionInfo parameter
     */
    public boolean canValidatePolicy(AssertionInfo assertionInfo) {
        return assertionInfo.getAssertion() != null
            && (SP12Constants.SECURITY_CONTEXT_TOKEN.equals(assertionInfo.getAssertion().getName())
                || SP11Constants.SECURITY_CONTEXT_TOKEN.equals(assertionInfo.getAssertion().getName()));
    }

    /**
     * Validate policies.
     */
    public void validatePolicies(PolicyValidatorParameters parameters, Collection<AssertionInfo> ais) {
        List<WSSecurityEngineResult> sctResults =
            parameters.getResults().getActionResults().get(WSConstants.SCT);

        for (AssertionInfo ai : ais) {
            SecurityContextToken sctPolicy = (SecurityContextToken)ai.getAssertion();
            ai.setAsserted(true);
            assertToken(sctPolicy, parameters.getAssertionInfoMap());

            if (!isTokenRequired(sctPolicy, parameters.getMessage())) {
                continue;
            }

            if (sctResults == null || sctResults.isEmpty()) {
                ai.setNotAsserted(
                    "The received token does not match the token inclusion requirement"
                );
                continue;
            }
        }
    }

    private void assertToken(SecurityContextToken token, AssertionInfoMap aim) {
        if (token.isRequireExternalUriReference()) {
            PolicyUtils.assertPolicy(aim, SP12Constants.REQUIRE_EXTERNAL_URI_REFERENCE);
        }
        if (token.isSc10SecurityContextToken()) {
            PolicyUtils.assertPolicy(aim, SP11Constants.SC10_SECURITY_CONTEXT_TOKEN);
        }
        if (token.isSc13SecurityContextToken()) {
            PolicyUtils.assertPolicy(aim, SP12Constants.SC13_SECURITY_CONTEXT_TOKEN);
        }
    }
}
