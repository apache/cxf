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
import org.apache.cxf.ws.security.policy.model.SecurityContextToken;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.util.WSSecurityUtil;

/**
 * Validate a WSSecurityEngineResult corresponding to the processing of a SecurityContextToken
 * against the appropriate policy.
 */
public class SecurityContextTokenPolicyValidator extends AbstractTokenPolicyValidator {
    
    private List<WSSecurityEngineResult> sctResults;
    private Message message;

    public SecurityContextTokenPolicyValidator(Message message, List<WSSecurityEngineResult> results) {
        this.message = message;
        sctResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.SCT, sctResults);
    }
    
    public boolean validatePolicy(AssertionInfoMap aim) {
        Collection<AssertionInfo> sctAis = aim.get(SP12Constants.SECURITY_CONTEXT_TOKEN);
        if (sctAis != null && !sctAis.isEmpty()) {
            for (AssertionInfo ai : sctAis) {
                SecurityContextToken sctPolicy = (SecurityContextToken)ai.getAssertion();
                ai.setAsserted(true);
                    
                boolean tokenRequired = isTokenRequired(sctPolicy, message);
                
                if (!tokenRequired) {
                    continue;
                }
                
                if (sctResults.isEmpty()) {
                    ai.setNotAsserted(
                        "The received token does not match the token inclusion requirement"
                    );
                    return false;
                }
            }
        }
        return true;
    }
    
}
