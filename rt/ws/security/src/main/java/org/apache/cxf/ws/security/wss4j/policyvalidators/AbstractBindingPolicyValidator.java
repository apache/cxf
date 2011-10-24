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

import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.neethi.Assertion;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDataRef;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.message.token.Timestamp;
import org.apache.ws.security.util.WSSecurityUtil;

/**
 * Some abstract functionality for validating a security binding.
 */
public abstract class AbstractBindingPolicyValidator {
    
    /**
     * Validate a Timestamp
     * @param includeTimestamp whether a Timestamp must be included or not
     * @param transportBinding whether the Transport binding is in use or not
     * @param results the results list
     * @param signedResults the signed results list
     * @param message the Message object
     * @return whether the Timestamp policy is valid or not
     */
    protected boolean validateTimestamp(
        boolean includeTimestamp,
        boolean transportBinding,
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults,
        Message message
    ) {
        List<WSSecurityEngineResult> timestampResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.TS, timestampResults);
        
        // Check whether we received a timestamp and compare it to the policy
        if (includeTimestamp && timestampResults.isEmpty()) {
            return false;
        } else if (!includeTimestamp && !timestampResults.isEmpty()) {
            return false;
        } else if (!includeTimestamp) {
            return true;
        }
        
        // At this point we received a (required) Timestamp. Now check that it is integrity protected.
        if (transportBinding) {
            return true;
        } else if (!signedResults.isEmpty()) {
            Timestamp timestamp = 
                (Timestamp)timestampResults.get(0).get(WSSecurityEngineResult.TAG_TIMESTAMP);
            for (WSSecurityEngineResult signedResult : signedResults) {
                List<WSDataRef> dataRefs = 
                    CastUtils.cast((List<?>)signedResult.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
                for (WSDataRef dataRef : dataRefs) {
                    if (timestamp == dataRef.getProtectedElement()) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Validate the entire header and body signature property.
     */
    protected boolean validateEntireHeaderAndBodySignatures(
        List<WSSecurityEngineResult> signedResults
    ) {
        if (signedResults.isEmpty()) {
            return false;
        }
        for (WSSecurityEngineResult signedResult : signedResults) {
            List<WSDataRef> dataRefs = 
                    CastUtils.cast((List<?>)signedResult.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
            for (WSDataRef dataRef : dataRefs) {
                String xpath = dataRef.getXpath();
                String[] nodes = xpath.split("/");
                // envelope/Body || envelope/Header/header
                if (nodes.length == 2 || nodes.length == 3) {
                    return true;
                // envelope/Header/wsse:Security/header
                } else if (nodes.length == 4 && nodes[2].contains("Security")) {
                    return true;
                }
            }
        }
        return true;
    }
    
    /**
     * Validate the layout assertion. It just checks the LaxTsFirst and LaxTsLast properties
     */
    protected boolean validateLayout(
        List<WSSecurityEngineResult> results,
        boolean laxTimestampFirst,
        boolean laxTimestampLast
    ) {
        if (laxTimestampFirst) {
            if (results.isEmpty()) {
                return false;
            }
            Integer firstAction = (Integer)results.get(0).get(WSSecurityEngineResult.TAG_ACTION);
            if (firstAction.intValue() != WSConstants.TS) {
                return false;
            }
        } else if (laxTimestampLast) {
            if (results.isEmpty()) {
                return false;
            }
            Integer lastAction = 
                (Integer)results.get(results.size() - 1).get(WSSecurityEngineResult.TAG_ACTION);
            if (lastAction.intValue() != WSConstants.TS) {
                return false;
            }
        }
        return true;
        
    }
    
    protected void assertPolicy(AssertionInfoMap aim, Assertion token) {
        Collection<AssertionInfo> ais = aim.get(token.getName());
        if (ais != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == token) {
                    ai.setAsserted(true);
                }
            }    
        }
    }
    
    protected boolean assertPolicy(AssertionInfoMap aim, QName q) {
        Collection<AssertionInfo> ais = aim.get(q);
        if (ais != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
            }    
            return true;
        }
        return false;
    }
    
    protected void notAssertPolicy(AssertionInfoMap aim, QName q, String msg) {
        Collection<AssertionInfo> ais = aim.get(q);
        if (ais != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                ai.setNotAsserted(msg);
            }    
        }
    }
}
