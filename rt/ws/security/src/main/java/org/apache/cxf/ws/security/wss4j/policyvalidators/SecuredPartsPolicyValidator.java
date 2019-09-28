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

import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageScope;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractSecuredParts;
import org.apache.wss4j.policy.model.Attachments;
import org.apache.wss4j.policy.model.Header;

/**
 * Validate either a SignedParts or EncryptedParts policy
 */
public class SecuredPartsPolicyValidator implements SecurityPolicyValidator {

    private CoverageType coverageType = CoverageType.ENCRYPTED;

    /**
     * Return true if this SecurityPolicyValidator implementation is capable of validating a
     * policy defined by the AssertionInfo parameter
     */
    public boolean canValidatePolicy(AssertionInfo assertionInfo) {
        if (coverageType == CoverageType.SIGNED) {
            return assertionInfo.getAssertion() != null
                && (SP12Constants.SIGNED_PARTS.equals(assertionInfo.getAssertion().getName())
                    || SP11Constants.SIGNED_PARTS.equals(assertionInfo.getAssertion().getName()));
        }
        return assertionInfo.getAssertion() != null
            && (SP12Constants.ENCRYPTED_PARTS.equals(assertionInfo.getAssertion().getName())
                || SP11Constants.ENCRYPTED_PARTS.equals(assertionInfo.getAssertion().getName()));
    }

    /**
     * Validate policies.
     */
    public void validatePolicies(PolicyValidatorParameters parameters, Collection<AssertionInfo> ais) {
        //
        // SIGNED_PARTS and ENCRYPTED_PARTS only apply to non-Transport bindings
        //
        if (isTransportBinding(parameters.getAssertionInfoMap(), parameters.getMessage())) {
            return;
        }

        Message msg = parameters.getMessage();
        Element soapBody = parameters.getSoapBody();
        Element header = parameters.getSoapHeader();
        soapBody = (Element)DOMUtils.getDomElement(soapBody);
        header = (Element)DOMUtils.getDomElement(header);
        Collection<WSDataRef> dataRefs = parameters.getEncrypted();
        if (coverageType == CoverageType.SIGNED) {
            dataRefs = parameters.getSigned();
        }

        for (AssertionInfo ai : ais) {
            if (ai.isAsserted()) {
                // Secured Parts could already have been asserted by one of the other validators, if
                // they are a child of a SupportingToken
                continue;
            }
            AbstractSecuredParts p = (AbstractSecuredParts)ai.getAssertion();
            ai.setAsserted(true);

            if (p.isBody()) {
                try {
                    if (coverageType == CoverageType.SIGNED) {
                        CryptoCoverageUtil.checkBodyCoverage(
                            soapBody, dataRefs, CoverageType.SIGNED, CoverageScope.ELEMENT
                        );
                    } else {
                        CryptoCoverageUtil.checkBodyCoverage(
                            soapBody, dataRefs, CoverageType.ENCRYPTED, CoverageScope.CONTENT
                        );
                    }
                } catch (WSSecurityException e) {
                    ai.setNotAsserted("Soap Body is not " + coverageType);
                    continue;
                }
            }

            for (Header h : p.getHeaders()) {
                if (header == null) {
                    ai.setNotAsserted(h.getNamespace() + ":" + h.getName() + " not + " + coverageType);
                } else {
                    try {
                        CryptoCoverageUtil.checkHeaderCoverage(header, dataRefs,
                                h.getNamespace(), h.getName(), coverageType,
                                CoverageScope.ELEMENT);
                    } catch (WSSecurityException e) {
                        ai.setNotAsserted(h.getNamespace() + ":" + h.getName() + " not + " + coverageType);
                    }
                }
            }

            Attachments attachments = p.getAttachments();
            if (attachments != null) {
                try {
                    CoverageScope scope = CoverageScope.ELEMENT;
                    if (attachments.isContentSignatureTransform()) {
                        scope = CoverageScope.CONTENT;
                    }
                    CryptoCoverageUtil.checkAttachmentsCoverage(msg.getAttachments(), dataRefs,
                                                                coverageType, scope);
                } catch (WSSecurityException e) {
                    ai.setNotAsserted("An attachment was not signed/encrypted");
                }
            }

        }
    }

    private boolean isTransportBinding(AssertionInfoMap aim, Message message) {
        AssertionInfo symAis = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.SYMMETRIC_BINDING);
        if (symAis != null) {
            return false;
        }

        AssertionInfo asymAis = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.ASYMMETRIC_BINDING);
        if (asymAis != null) {
            return false;
        }

        AssertionInfo transAis = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.TRANSPORT_BINDING);
        if (transAis != null) {
            return true;
        }

        // No bindings, check if we are using TLS
        TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
        if (tlsInfo != null) {
            // We don't need to check these policies for TLS
            PolicyUtils.assertPolicy(aim, SP12Constants.ENCRYPTED_PARTS);
            PolicyUtils.assertPolicy(aim, SP11Constants.ENCRYPTED_PARTS);
            PolicyUtils.assertPolicy(aim, SP12Constants.SIGNED_PARTS);
            PolicyUtils.assertPolicy(aim, SP11Constants.SIGNED_PARTS);
            return true;
        }

        return false;
    }

    public CoverageType getCoverageType() {
        return coverageType;
    }

    public void setCoverageType(CoverageType coverageType) {
        this.coverageType = coverageType;
    }
}
