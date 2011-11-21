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
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.Wss11;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.util.WSSecurityUtil;

/**
 * Validate a WSS11 policy.
 */
public class WSS11PolicyValidator {
    
    private List<WSSecurityEngineResult> scResults;
    private Message message;

    public WSS11PolicyValidator(
        Message message,
        List<WSSecurityEngineResult> results
    ) {
        this.message = message;
        scResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.SC, scResults);
    }
    
    public boolean validatePolicy(
        AssertionInfoMap aim
    ) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.WSS11);
        if (ais == null || ais.isEmpty()) {
            return true;
        }
        
        for (AssertionInfo ai : ais) {
            Wss11 wss11 = (Wss11)ai.getAssertion();
            ai.setAsserted(true);

            if (!MessageUtils.isRequestor(message)) {
                continue;
            }
            
            if (wss11.isRequireSignatureConfirmation() && scResults.isEmpty()) {
                ai.setNotAsserted(
                    "Signature Confirmation policy validation failed"
                );
                return false;
            }
        }
        return true;
    }
    
}
