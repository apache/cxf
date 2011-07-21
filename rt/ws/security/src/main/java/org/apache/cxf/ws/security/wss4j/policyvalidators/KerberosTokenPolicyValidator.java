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
import org.apache.cxf.ws.security.policy.model.KerberosToken;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.ws.security.message.token.KerberosSecurity;
import org.apache.ws.security.util.WSSecurityUtil;

/**
 * Validate a WSSecurityEngineResult corresponding to the processing of a Kerberos Token
 * against the appropriate policy.
 */
public class KerberosTokenPolicyValidator extends AbstractTokenPolicyValidator {
    
    private List<WSSecurityEngineResult> bstResults;
    private Message message;

    public KerberosTokenPolicyValidator(
        Message message,
        List<WSSecurityEngineResult> results
    ) {
        this.message = message;
        bstResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.BST, bstResults);
    }
    
    public boolean validatePolicy(
        AssertionInfoMap aim
    ) {
        Collection<AssertionInfo> krbAis = aim.get(SP12Constants.KERBEROS_TOKEN);
        if (krbAis != null && !krbAis.isEmpty()) {
            for (AssertionInfo ai : krbAis) {
                KerberosToken kerberosTokenPolicy = (KerberosToken)ai.getAssertion();
                ai.setAsserted(true);
                
                if (!isTokenRequired(kerberosTokenPolicy, message)) {
                    continue;
                }
                
                if (bstResults.isEmpty()) {
                    ai.setNotAsserted(
                        "The received token does not match the token inclusion requirement"
                    );
                    return false;
                }
                
                if (!checkToken(kerberosTokenPolicy)) {
                    ai.setNotAsserted("An incorrect Kerberos Token Type is detected");
                    return false;
                }
            }
        }
        return true;
    }
    
    private boolean checkToken(KerberosToken kerberosTokenPolicy) {
        if (!bstResults.isEmpty()) {
            boolean isV5ApReq = kerberosTokenPolicy.isV5ApReqToken11();
            boolean isGssV5ApReq = kerberosTokenPolicy.isGssV5ApReqToken11();
            
            for (WSSecurityEngineResult result : bstResults) {
                BinarySecurity binarySecurityToken = 
                    (BinarySecurity)result.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
                if (binarySecurityToken instanceof KerberosSecurity) {
                    if (isV5ApReq && ((KerberosSecurity)binarySecurityToken).isV5ApReq()) {
                        return true;
                    } else if (isGssV5ApReq 
                        && ((KerberosSecurity)binarySecurityToken).isGssV5ApReq()) {
                        return true;
                    } else if (!isV5ApReq && !isGssV5ApReq) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
