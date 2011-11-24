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

import java.util.List;

import org.w3c.dom.Element;

import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.ws.security.WSSecurityEngineResult;

/**
 * Validate a WS-SecurityPolicy corresponding to a SupportingToken.
 */
public interface SupportingTokenPolicyValidator {
    
    /**
     * Set the list of UsernameToken results
     */
    void setUsernameTokenResults(List<WSSecurityEngineResult> utResultsList, boolean valUsernameToken);
    
    /**
     * Set the list of SAMLToken results
     */
    void setSAMLTokenResults(List<WSSecurityEngineResult> samlResultsList);
    
    /**
     * Set the Timestamp element
     */
    void setTimestampElement(Element timestampElement);
    
    /**
     * Validate a particular policy from the AssertionInfoMap argument. Return true if the policy is valid.
     */
    boolean validatePolicy(
        AssertionInfoMap aim, 
        Message message,
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults,
        List<WSSecurityEngineResult> encryptedResults
    );
}
