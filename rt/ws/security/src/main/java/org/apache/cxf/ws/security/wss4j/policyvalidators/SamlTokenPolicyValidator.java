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
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.saml.DOMSAMLUtil;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.SamlToken;
import org.apache.wss4j.policy.model.SamlToken.SamlTokenType;
import org.opensaml.saml.common.SAMLVersion;

/**
 * Validate a SamlToken policy.
 */
public class SamlTokenPolicyValidator extends AbstractSamlPolicyValidator {

    /**
     * Return true if this SecurityPolicyValidator implementation is capable of validating a
     * policy defined by the AssertionInfo parameter
     */
    public boolean canValidatePolicy(AssertionInfo assertionInfo) {
        return assertionInfo.getAssertion() != null
            && (SP12Constants.SAML_TOKEN.equals(assertionInfo.getAssertion().getName())
                || SP11Constants.SAML_TOKEN.equals(assertionInfo.getAssertion().getName()));
    }

    /**
     * Validate policies.
     */
    public void validatePolicies(PolicyValidatorParameters parameters, Collection<AssertionInfo> ais) {
        for (AssertionInfo ai : ais) {
            SamlToken samlToken = (SamlToken)ai.getAssertion();
            ai.setAsserted(true);
            assertToken(samlToken, parameters.getAssertionInfoMap());

            if (!isTokenRequired(samlToken, parameters.getMessage())) {
                PolicyUtils.assertPolicy(
                    parameters.getAssertionInfoMap(),
                    new QName(samlToken.getVersion().getNamespace(), samlToken.getSamlTokenType().name())
                );
                continue;
            }

            if (parameters.getSamlResults().isEmpty()) {
                ai.setNotAsserted(
                    "The received token does not match the token inclusion requirement"
                );
                continue;
            }

            String valSAMLSubjectConf =
                (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION,
                                                               parameters.getMessage());
            boolean validateSAMLSubjectConf = true;
            if (valSAMLSubjectConf != null) {
                validateSAMLSubjectConf = Boolean.parseBoolean(valSAMLSubjectConf);
            }

            // All of the received SAML Assertions must conform to the policy
            for (WSSecurityEngineResult result : parameters.getSamlResults()) {
                SamlAssertionWrapper assertionWrapper =
                    (SamlAssertionWrapper)result.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);

                if (!checkVersion(parameters.getAssertionInfoMap(), samlToken, assertionWrapper)) {
                    ai.setNotAsserted("Wrong SAML Version");
                    continue;
                }

                if (validateSAMLSubjectConf) {
                    TLSSessionInfo tlsInfo = parameters.getMessage().get(TLSSessionInfo.class);
                    Certificate[] tlsCerts = null;
                    if (tlsInfo != null) {
                        tlsCerts = tlsInfo.getPeerCertificates();
                    }
                    if (!checkHolderOfKey(assertionWrapper, parameters.getSignedResults(), tlsCerts)) {
                        ai.setNotAsserted("Assertion fails holder-of-key requirements");
                        continue;
                    }
                    if (parameters.getSoapBody() == null
                        || !DOMSAMLUtil.checkSenderVouches(assertionWrapper, tlsCerts, parameters.getSoapBody(),
                                                        parameters.getSignedResults())) {
                        ai.setNotAsserted("Assertion fails sender-vouches requirements");
                        continue;
                    }
                }
                /*
                    if (!checkIssuerName(samlToken, assertionWrapper)) {
                        ai.setNotAsserted("Wrong IssuerName");
                    }
                 */
            }
        }
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
    private boolean checkVersion(
        AssertionInfoMap aim,
        SamlToken samlToken,
        SamlAssertionWrapper assertionWrapper
    ) {
        SamlTokenType samlTokenType = samlToken.getSamlTokenType();
        if ((samlTokenType == SamlTokenType.WssSamlV11Token10
            || samlTokenType == SamlTokenType.WssSamlV11Token11)
            && assertionWrapper.getSamlVersion() != SAMLVersion.VERSION_11) {
            return false;
        } else if (samlTokenType == SamlTokenType.WssSamlV20Token11
            && assertionWrapper.getSamlVersion() != SAMLVersion.VERSION_20) {
            return false;
        }

        if (samlTokenType != null) {
            PolicyUtils.assertPolicy(aim, new QName(samlToken.getVersion().getNamespace(), samlTokenType.name()));
        }
        return true;
    }

    private void assertToken(SamlToken token, AssertionInfoMap aim) {
        String namespace = token.getName().getNamespaceURI();

        if (token.isRequireKeyIdentifierReference()) {
            PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.REQUIRE_KEY_IDENTIFIER_REFERENCE));
        }
    }

}
