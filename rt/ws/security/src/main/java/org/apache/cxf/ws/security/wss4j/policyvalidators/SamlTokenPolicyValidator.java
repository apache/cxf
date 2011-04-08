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

import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.SamlToken;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.saml.ext.AssertionWrapper;

import org.opensaml.common.SAMLVersion;

/**
 * Validate a WSSecurityEngineResult corresponding to the processing of a SAML Assertion
 * against the appropriate policy.
 */
public class SamlTokenPolicyValidator {
    
    public boolean validatePolicy(
        AssertionInfoMap aim,
        WSSecurityEngineResult wser
    ) {
        Collection<AssertionInfo> samlAis = aim.get(SP12Constants.SAML_TOKEN);
        if (samlAis != null && !samlAis.isEmpty()) {
            for (AssertionInfo ai : samlAis) {
                AssertionWrapper assertionWrapper = 
                    (AssertionWrapper)wser.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
                SamlToken samlToken = (SamlToken)ai.getAssertion();
                ai.setAsserted(true);

                if (!checkVersion(samlToken, assertionWrapper)) {
                    ai.setNotAsserted("Wrong SAML Version");
                    return false;
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
    private boolean checkVersion(SamlToken samlToken, AssertionWrapper assertionWrapper) {
        if ((samlToken.isUseSamlVersion11Profile10()
            || samlToken.isUseSamlVersion11Profile11())
            && assertionWrapper.getSamlVersion() != SAMLVersion.VERSION_11) {
            return false;
        } else if (samlToken.isUseSamlVersion20Profile11()
            && assertionWrapper.getSamlVersion() != SAMLVersion.VERSION_20) {
            return false;
        }
        return true;
    }
   
}
