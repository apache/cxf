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

import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.KerberosToken;
import org.apache.ws.security.message.token.KerberosSecurity;

/**
 * Validate a WSSecurityEngineResult corresponding to the processing of a Kerberos Token
 * against the appropriate policy.
 */
public class KerberosTokenPolicyValidator extends AbstractTokenPolicyValidator {
    
    private Message message;

    public KerberosTokenPolicyValidator(
        Message message
    ) {
        this.message = message;
    }
    
    public boolean validatePolicy(
        AssertionInfoMap aim,
        KerberosSecurity kerberosToken
    ) {
        Collection<AssertionInfo> krbAis = aim.get(SP12Constants.KERBEROS_TOKEN);
        if (krbAis != null && !krbAis.isEmpty()) {
            for (AssertionInfo ai : krbAis) {
                KerberosToken kerberosTokenPolicy = (KerberosToken)ai.getAssertion();
                ai.setAsserted(true);
                
                if (!isTokenRequired(kerberosTokenPolicy, message)) {
                    continue;
                }
                
                if (!checkToken(kerberosTokenPolicy, kerberosToken)) {
                    ai.setNotAsserted("An incorrect Kerberos Token Type is detected");
                    continue;
                }
            }
        }
        return true;
    }
    
    private boolean checkToken(KerberosToken kerberosTokenPolicy, KerberosSecurity kerberosToken) {
        boolean isV5ApReq = kerberosTokenPolicy.isV5ApReqToken11();
        boolean isGssV5ApReq = kerberosTokenPolicy.isGssV5ApReqToken11();

        if (isV5ApReq && kerberosToken.isV5ApReq()) {
            return true;
        } else if (isGssV5ApReq && kerberosToken.isGssV5ApReq()) {
            return true;
        } else if (!(isV5ApReq || isGssV5ApReq)) {
            return true;
        }
        return false;
    }
}
