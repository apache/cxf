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

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.ws.security.WSDataRef;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSSecurityEngineResult;

/**
 * Validate an EndorsingSupportingToken policy. 
 */
public class EndorsingTokenPolicyValidator {
    
    private List<WSSecurityEngineResult> signedResults;
    private Message message;

    public EndorsingTokenPolicyValidator(
        List<WSSecurityEngineResult> signedResults,
        Message message
    ) {
        this.signedResults = signedResults;
        this.message = message;
    }
    
    public boolean validatePolicy(
        AssertionInfoMap aim
    ) {
        Collection<AssertionInfo> endorsingAis = aim.get(SP12Constants.ENDORSING_SUPPORTING_TOKENS);
        if (endorsingAis != null && !endorsingAis.isEmpty()) {
            for (AssertionInfo ai : endorsingAis) {
                ai.setAsserted(true);
                
                TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
                boolean transport = false;
                if (tlsInfo != null) {
                    transport = true;
                }
                if (!checkEndorsed(transport)) {
                    ai.setNotAsserted("Message fails endorsing supporting tokens requirements");
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Check the endorsing supporting token policy. If we're using the Transport Binding then
     * check that the Timestamp is signed. Otherwise, check that the signature is signed.
     * @param transport
     * @return true if the endorsed supporting token policy is correct
     */
    private boolean checkEndorsed(boolean transport) {
        if (transport) {
            return checkTimestampIsSigned();
        }
        return checkSignatureIsSigned();
    }
    
    /**
     * Return true if the Timestamp is signed
     * @return true if the Timestamp is signed
     */
    private boolean checkTimestampIsSigned() {
        for (WSSecurityEngineResult signedResult : signedResults) {
            List<WSDataRef> sl =
                CastUtils.cast((List<?>)signedResult.get(
                    WSSecurityEngineResult.TAG_DATA_REF_URIS
                ));
            if (sl != null) {
                for (WSDataRef dataRef : sl) {
                    QName signedQName = dataRef.getName();
                    if (WSSecurityEngine.TIMESTAMP.equals(signedQName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Return true if the Signature is itself signed
     * @return true if the Signature is itself signed
     */
    private boolean checkSignatureIsSigned() {
        for (WSSecurityEngineResult signedResult : signedResults) {
            List<WSDataRef> sl =
                CastUtils.cast((List<?>)signedResult.get(
                    WSSecurityEngineResult.TAG_DATA_REF_URIS
                ));
            if (sl != null) {
                for (WSDataRef dataRef : sl) {
                    QName signedQName = dataRef.getName();
                    if (WSSecurityEngine.SIGNATURE.equals(signedQName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
