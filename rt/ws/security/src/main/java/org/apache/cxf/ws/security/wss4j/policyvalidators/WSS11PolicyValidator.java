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
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSSecurityEngineResult;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.Wss11;

/**
 * Validate a WSS11 policy.
 */
public class WSS11PolicyValidator 
    extends AbstractTokenPolicyValidator implements TokenPolicyValidator {
    
    public boolean validatePolicy(
        AssertionInfoMap aim,
        Message message,
        Element soapBody,
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults
    ) {
        Collection<AssertionInfo> ais = 
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.WSS11);
        if (!ais.isEmpty()) {
            parsePolicies(ais, message, results);
            
            PolicyUtils.assertPolicy(aim, SPConstants.MUST_SUPPORT_REF_THUMBPRINT);
            PolicyUtils.assertPolicy(aim, SPConstants.MUST_SUPPORT_REF_ENCRYPTED_KEY);
            PolicyUtils.assertPolicy(aim, SPConstants.REQUIRE_SIGNATURE_CONFIRMATION);
            
            // WSS 1.0
            PolicyUtils.assertPolicy(aim, SPConstants.MUST_SUPPORT_REF_KEY_IDENTIFIER);
            PolicyUtils.assertPolicy(aim, SPConstants.MUST_SUPPORT_REF_ISSUER_SERIAL);
            PolicyUtils.assertPolicy(aim, SPConstants.MUST_SUPPORT_REF_EXTERNAL_URI);
            PolicyUtils.assertPolicy(aim, SPConstants.MUST_SUPPORT_REF_EMBEDDED_TOKEN);
        }
        
        return true;
    }
    
    private void parsePolicies(
        Collection<AssertionInfo> ais, 
        Message message,  
        List<WSSecurityEngineResult> results
    ) {
        List<WSSecurityEngineResult> scResults =
            WSSecurityUtil.fetchAllActionResults(results, WSConstants.SC);
        
        for (AssertionInfo ai : ais) {
            Wss11 wss11 = (Wss11)ai.getAssertion();
            ai.setAsserted(true);

            if (!MessageUtils.isRequestor(message)) {
                continue;
            }
            
            if ((wss11.isRequireSignatureConfirmation() && scResults.isEmpty())
                || (!wss11.isRequireSignatureConfirmation() && !scResults.isEmpty())) {
                ai.setNotAsserted(
                    "Signature Confirmation policy validation failed"
                );
                continue;
            }
        }
    }
    
}
