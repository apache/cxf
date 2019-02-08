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

import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.Wss11;

/**
 * Validate a WSS11 policy.
 */
public class WSS11PolicyValidator extends AbstractSecurityPolicyValidator {

    /**
     * Return true if this SecurityPolicyValidator implementation is capable of validating a
     * policy defined by the AssertionInfo parameter
     */
    public boolean canValidatePolicy(AssertionInfo assertionInfo) {
        return assertionInfo.getAssertion() != null
            && (SP12Constants.WSS11.equals(assertionInfo.getAssertion().getName())
                || SP11Constants.WSS11.equals(assertionInfo.getAssertion().getName()));
    }

    /**
     * Validate policies.
     */
    public void validatePolicies(PolicyValidatorParameters parameters, Collection<AssertionInfo> ais) {
        List<WSSecurityEngineResult> scResults =
            parameters.getResults().getActionResults().get(WSConstants.SC);

        for (AssertionInfo ai : ais) {
            Wss11 wss11 = (Wss11)ai.getAssertion();
            ai.setAsserted(true);
            assertToken(wss11, parameters.getAssertionInfoMap());

            if (!MessageUtils.isRequestor(parameters.getMessage())) {
                continue;
            }

            if ((wss11.isRequireSignatureConfirmation() && (scResults == null || scResults.isEmpty()))
                || (!wss11.isRequireSignatureConfirmation() && !(scResults == null || scResults.isEmpty()))) {
                ai.setNotAsserted(
                    "Signature Confirmation policy validation failed"
                );
                continue;
            }
        }
    }

    private void assertToken(Wss11 token, AssertionInfoMap aim) {
        String namespace = token.getName().getNamespaceURI();

        if (token.isMustSupportRefEmbeddedToken()) {
            PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.MUST_SUPPORT_REF_EMBEDDED_TOKEN));
        }
        if (token.isMustSupportRefEncryptedKey()) {
            PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.MUST_SUPPORT_REF_ENCRYPTED_KEY));
        }
        if (token.isMustSupportRefExternalURI()) {
            PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.MUST_SUPPORT_REF_EXTERNAL_URI));
        }
        if (token.isMustSupportRefIssuerSerial()) {
            PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.MUST_SUPPORT_REF_ISSUER_SERIAL));
        }
        if (token.isMustSupportRefKeyIdentifier()) {
            PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.MUST_SUPPORT_REF_KEY_IDENTIFIER));
        }
        if (token.isMustSupportRefThumbprint()) {
            PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.MUST_SUPPORT_REF_THUMBPRINT));
        }
        if (token.isRequireSignatureConfirmation()) {
            PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.REQUIRE_SIGNATURE_CONFIRMATION));
        }

    }


}
