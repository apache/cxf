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

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.SamlToken;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDataRef;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;
import org.apache.ws.security.util.WSSecurityUtil;

import org.opensaml.common.SAMLVersion;

/**
 * Validate a WSSecurityEngineResult corresponding to the processing of a SAML Assertion
 * against the appropriate policy.
 */
public class SamlTokenPolicyValidator extends AbstractSamlPolicyValidator {
    
    private List<WSSecurityEngineResult> signedResults;
    private Element soapBody;
    private Message message;
    private List<WSSecurityEngineResult> samlResults;

    public SamlTokenPolicyValidator(
        Element soapBody,
        List<WSSecurityEngineResult> signedResults,
        Message message,
        List<WSSecurityEngineResult> results
    ) {
        this.soapBody = soapBody;
        this.signedResults = signedResults;
        this.message = message;
        samlResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.ST_SIGNED, samlResults);
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.ST_UNSIGNED, samlResults);
    }
    
    public boolean validatePolicy(
        AssertionInfoMap aim
    ) {
        Collection<AssertionInfo> samlAis = aim.get(SP12Constants.SAML_TOKEN);
        if (samlAis == null || samlAis.isEmpty()) {
            return true;
        }
        
        for (AssertionInfo ai : samlAis) {
            SamlToken samlToken = (SamlToken)ai.getAssertion();
            ai.setAsserted(true);

            boolean tokenRequired = isTokenRequired(samlToken, message);
            if (tokenRequired && samlResults.isEmpty()) {
                ai.setNotAsserted(
                    "The received token does not match the token inclusion requirement"
                );
                return false;
            }
            if (!tokenRequired) {
                continue;
            }

            for (WSSecurityEngineResult result : samlResults) {
                AssertionWrapper assertionWrapper = 
                    (AssertionWrapper)result.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
                
                if (!checkVersion(samlToken, assertionWrapper)) {
                    ai.setNotAsserted("Wrong SAML Version");
                    return false;
                }
                TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
                Certificate[] tlsCerts = null;
                if (tlsInfo != null) {
                    tlsCerts = tlsInfo.getPeerCertificates();
                }
                if (!checkHolderOfKey(assertionWrapper, signedResults, tlsCerts)) {
                    ai.setNotAsserted("Assertion fails holder-of-key requirements");
                    return false;
                }
                if (!checkSenderVouches(assertionWrapper, tlsCerts)) {
                    ai.setNotAsserted("Assertion fails sender-vouches requirements");
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
    
    /**
     * Check the sender-vouches requirements against the received assertion. The SAML
     * Assertion and the SOAP Body must be signed by the same signature.
     */
    private boolean checkSenderVouches(
        AssertionWrapper assertionWrapper,
        Certificate[] tlsCerts
    ) {
        //
        // If we have a 2-way TLS connection, then we don't have to check that the
        // assertion + SOAP body are signed
        //
        if (tlsCerts != null && tlsCerts.length > 0) {
            return true;
        }
        List<String> confirmationMethods = assertionWrapper.getConfirmationMethods();
        for (String confirmationMethod : confirmationMethods) {
            if (OpenSAMLUtil.isMethodSenderVouches(confirmationMethod)) {
                if (signedResults == null || signedResults.isEmpty()) {
                    return false;
                }
                if (!checkAssertionAndBodyAreSigned(assertionWrapper)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Return true if there is a signature which references the Assertion and the SOAP Body.
     * @param assertionWrapper the AssertionWrapper object
     * @return true if there is a signature which references the Assertion and the SOAP Body.
     */
    private boolean checkAssertionAndBodyAreSigned(AssertionWrapper assertionWrapper) {
        for (WSSecurityEngineResult signedResult : signedResults) {
            List<WSDataRef> sl =
                CastUtils.cast((List<?>)signedResult.get(
                    WSSecurityEngineResult.TAG_DATA_REF_URIS
                ));
            boolean assertionIsSigned = false;
            boolean bodyIsSigned = false;
            if (sl != null) {
                for (WSDataRef dataRef : sl) {
                    Element se = dataRef.getProtectedElement();
                    if (se == assertionWrapper.getElement()) {
                        assertionIsSigned = true;
                    }
                    if (se == soapBody) {
                        bodyIsSigned = true;
                    }
                    if (assertionIsSigned && bodyIsSigned) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
