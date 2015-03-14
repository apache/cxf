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

import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.wss4j.dom.message.token.KerberosSecurity;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.KerberosToken;
import org.apache.wss4j.policy.model.KerberosToken.ApReqTokenType;

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
        Collection<AssertionInfo> krbAis = 
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.KERBEROS_TOKEN);
        if (!krbAis.isEmpty()) {
            parsePolicies(aim, krbAis, kerberosToken);
            
            PolicyUtils.assertPolicy(aim, SPConstants.REQUIRE_KEY_IDENTIFIER_REFERENCE);
        }
        
        return true;
    }
    
    private void parsePolicies(
        AssertionInfoMap aim, 
        Collection<AssertionInfo> ais, 
        KerberosSecurity kerberosToken
    ) {
        for (AssertionInfo ai : ais) {
            KerberosToken kerberosTokenPolicy = (KerberosToken)ai.getAssertion();
            ai.setAsserted(true);
            
            if (!isTokenRequired(kerberosTokenPolicy, message)) {
                PolicyUtils.assertPolicy(
                    aim, 
                    new QName(kerberosTokenPolicy.getVersion().getNamespace(), 
                              "WssKerberosV5ApReqToken11")
                );
                PolicyUtils.assertPolicy(
                    aim, 
                    new QName(kerberosTokenPolicy.getVersion().getNamespace(), 
                              "WssGssKerberosV5ApReqToken11")
                );
                continue;
            }
            
            if (!checkToken(aim, kerberosTokenPolicy, kerberosToken)) {
                ai.setNotAsserted("An incorrect Kerberos Token Type is detected");
                continue;
            }
        }
    }
    
    private boolean checkToken(
        AssertionInfoMap aim,
        KerberosToken kerberosTokenPolicy, 
        KerberosSecurity kerberosToken
    ) {
        ApReqTokenType apReqTokenType = kerberosTokenPolicy.getApReqTokenType();

        if (apReqTokenType == ApReqTokenType.WssKerberosV5ApReqToken11 
            && kerberosToken.isV5ApReq()) {
            PolicyUtils.assertPolicy(
                aim, 
                new QName(kerberosTokenPolicy.getVersion().getNamespace(), "WssKerberosV5ApReqToken11")
            );
            return true;
        } else if (apReqTokenType == ApReqTokenType.WssGssKerberosV5ApReqToken11 
            && kerberosToken.isGssV5ApReq()) {
            PolicyUtils.assertPolicy(
                aim, 
                new QName(kerberosTokenPolicy.getVersion().getNamespace(), "WssGssKerberosV5ApReqToken11")
            );
            return true;
        }
        
        return false;
    }
}
