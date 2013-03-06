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

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Element;

import org.apache.cxf.message.Message;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.wss4j.SAMLUtils;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSSecurityEngineResult;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.wss4j.policy.model.SamlToken;
import org.apache.wss4j.policy.model.SamlToken.SamlTokenType;
import org.opensaml.common.SAMLVersion;

/**
 * Validate a SamlToken policy.
 */
public class SamlTokenPolicyValidator extends AbstractSamlPolicyValidator implements TokenPolicyValidator {
    
    private Element body;
    private List<WSSecurityEngineResult> signed;
    
    public boolean validatePolicy(
        AssertionInfoMap aim,
        Message message,
        Element soapBody,
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults
    ) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.SAML_TOKEN);
        if (ais == null || ais.isEmpty()) {
            return true;
        }
        
        body = soapBody;
        signed = signedResults;
        
        List<WSSecurityEngineResult> samlResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.ST_SIGNED, samlResults);
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.ST_UNSIGNED, samlResults);
        
        for (AssertionInfo ai : ais) {
            SamlToken samlToken = (SamlToken)ai.getAssertion();
            ai.setAsserted(true);

            if (!isTokenRequired(samlToken, message)) {
                continue;
            }

            if (samlResults.isEmpty()) {
                ai.setNotAsserted(
                    "The received token does not match the token inclusion requirement"
                );
                continue;
            }
            
            // All of the received SAML Assertions must conform to the policy
            for (WSSecurityEngineResult result : samlResults) {
                SamlAssertionWrapper assertionWrapper = 
                    (SamlAssertionWrapper)result.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
                
                if (!checkVersion(samlToken, assertionWrapper)) {
                    ai.setNotAsserted("Wrong SAML Version");
                    continue;
                }
                TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
                Certificate[] tlsCerts = null;
                if (tlsInfo != null) {
                    tlsCerts = tlsInfo.getPeerCertificates();
                }
                if (!checkHolderOfKey(assertionWrapper, signedResults, tlsCerts)) {
                    ai.setNotAsserted("Assertion fails holder-of-key requirements");
                    continue;
                }
                if (!SAMLUtils.checkSenderVouches(assertionWrapper, tlsCerts, body, signed)) {
                    ai.setNotAsserted("Assertion fails sender-vouches requirements");
                    continue;
                }
                /*
                    if (!checkIssuerName(samlToken, assertionWrapper)) {
                        ai.setNotAsserted("Wrong IssuerName");
                    }
                 */
            }
        }
        
        return true;
    }
    
    /**
     * Check the IssuerName policy against the received assertion
    private boolean checkIssuerName(SamlToken samlToken, AssertionWrapper assertionWrapper) {
        String issuerName = samlToken.getIssuerName();
        if (issuerName != null && !"".equals(issuerName)) {
            String assertionIssuer = assertionWrapper.getIssuerString();
            if (!issuerName.equals(assertionIssuer)) {
                return false;
            }
        }
        return true;
    }
    */
    
    /**
     * Check the policy version against the received assertion
     */
    private boolean checkVersion(SamlToken samlToken, SamlAssertionWrapper assertionWrapper) {
        SamlTokenType samlTokenType = samlToken.getSamlTokenType();
        if ((samlTokenType == SamlTokenType.WssSamlV11Token10
            || samlTokenType == SamlTokenType.WssSamlV11Token11)
            && assertionWrapper.getSamlVersion() != SAMLVersion.VERSION_11) {
            return false;
        } else if (samlTokenType == SamlTokenType.WssSamlV20Token11
            && assertionWrapper.getSamlVersion() != SAMLVersion.VERSION_20) {
            return false;
        }
        return true;
    }
    
}
