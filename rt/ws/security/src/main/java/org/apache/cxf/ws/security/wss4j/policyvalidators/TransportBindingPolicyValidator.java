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

import javax.xml.namespace.QName;

import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.TransportBinding;

/**
 * Validate a TransportBinding policy.
 */
public class TransportBindingPolicyValidator extends AbstractBindingPolicyValidator {

    /**
     * Return true if this SecurityPolicyValidator implementation is capable of validating a
     * policy defined by the AssertionInfo parameter
     */
    public boolean canValidatePolicy(AssertionInfo assertionInfo) {
        return assertionInfo.getAssertion() != null
            && (SP12Constants.TRANSPORT_BINDING.equals(assertionInfo.getAssertion().getName())
                || SP11Constants.TRANSPORT_BINDING.equals(assertionInfo.getAssertion().getName()));
    }

    /**
     * Validate policies.
     */
    public void validatePolicies(PolicyValidatorParameters parameters, Collection<AssertionInfo> ais) {
        for (AssertionInfo ai : ais) {
            TransportBinding binding = (TransportBinding)ai.getAssertion();
            ai.setAsserted(true);

            // Check that TLS is in use if we are not the requestor
            boolean initiator = MessageUtils.isRequestor(parameters.getMessage());
            TLSSessionInfo tlsInfo = parameters.getMessage().get(TLSSessionInfo.class);
            if (!initiator && tlsInfo == null) {
                ai.setNotAsserted("TLS is not enabled");
                continue;
            }

            // HttpsToken is validated by the HttpsTokenInterceptorProvider
            if (binding.getTransportToken() != null) {
                PolicyUtils.assertPolicy(parameters.getAssertionInfoMap(), binding.getTransportToken().getName());
            }

            // Check the IncludeTimestamp
            if (!validateTimestamp(binding.isIncludeTimestamp(), true, parameters.getResults(),
                                   parameters.getSignedResults(), parameters.getMessage())) {
                String error = "Received Timestamp does not match the requirements";
                ai.setNotAsserted(error);
                continue;
            }
            PolicyUtils.assertPolicy(parameters.getAssertionInfoMap(),
                                     new QName(binding.getName().getNamespaceURI(), SPConstants.INCLUDE_TIMESTAMP));
        }

        // We don't need to check these policies for the Transport binding
        if (!ais.isEmpty()) {
            PolicyUtils.assertPolicy(parameters.getAssertionInfoMap(), SP12Constants.ENCRYPTED_PARTS);
            PolicyUtils.assertPolicy(parameters.getAssertionInfoMap(), SP11Constants.ENCRYPTED_PARTS);
            PolicyUtils.assertPolicy(parameters.getAssertionInfoMap(), SP12Constants.SIGNED_PARTS);
            PolicyUtils.assertPolicy(parameters.getAssertionInfoMap(), SP11Constants.SIGNED_PARTS);
        }
    }

}
