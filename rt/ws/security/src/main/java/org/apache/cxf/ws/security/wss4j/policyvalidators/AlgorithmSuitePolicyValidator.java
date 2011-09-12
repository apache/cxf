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
import java.util.List;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.security.policy.model.AlgorithmSuite;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDataRef;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.util.WSSecurityUtil;

/**
 * Validate a WSSecurityEngineResult corresponding to the processing of a Signature, EncryptedKey,
 * EncryptedData or DerivedKey structure against an AlgorithmSuite policy.
 */
public class AlgorithmSuitePolicyValidator extends AbstractTokenPolicyValidator {
    
    private List<WSSecurityEngineResult> algorithmResults;

    public AlgorithmSuitePolicyValidator(
        List<WSSecurityEngineResult> results
    ) {
        algorithmResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.SIGN, algorithmResults);
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.ENCR, algorithmResults);
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.DKT, algorithmResults);
    }
    
    public boolean validatePolicy(
        AssertionInfo aiBinding, AlgorithmSuite algorithmPolicy
    ) {
        for (WSSecurityEngineResult result : algorithmResults) {
            Integer actInt = (Integer)result.get(WSSecurityEngineResult.TAG_ACTION);
            if (WSConstants.SIGN == actInt 
                && !checkSignatureAlgorithms(result, algorithmPolicy, aiBinding)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check the Signature Algorithms
     */
    private boolean checkSignatureAlgorithms(
        WSSecurityEngineResult result, 
        AlgorithmSuite algorithmPolicy,
        AssertionInfo ai
    ) {
        String signatureMethod = 
            (String)result.get(WSSecurityEngineResult.TAG_SIGNATURE_METHOD);
        if (!algorithmPolicy.getAsymmetricSignature().equals(signatureMethod)
            && !algorithmPolicy.getSymmetricSignature().equals(signatureMethod)) {
            ai.setNotAsserted(
                "The signature method does not match the requirement"
            );
            return false;
        }
        String c14nMethod = 
            (String)result.get(WSSecurityEngineResult.TAG_CANONICALIZATION_METHOD);
        if (!algorithmPolicy.getInclusiveC14n().equals(c14nMethod)) {
            ai.setNotAsserted(
                "The c14n method does not match the requirement"
            );
            return false;
        }

        List<WSDataRef> dataRefs = 
            CastUtils.cast((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
        for (WSDataRef dataRef : dataRefs) {
            String digestMethod = dataRef.getDigestAlgorithm();
            if (!algorithmPolicy.getDigest().equals(digestMethod)) {
                ai.setNotAsserted(
                    "The digest method does not match the requirement"
                );
                return false;
            }
        }
        
        return true;
    }
    
}
