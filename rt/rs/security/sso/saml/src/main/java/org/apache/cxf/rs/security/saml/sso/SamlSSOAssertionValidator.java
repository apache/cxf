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
package org.apache.cxf.rs.security.saml.sso;

import java.util.List;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.dom.validate.SamlAssertionValidator;

/**
 * An extension of the WSS4J SamlAssertionValidator. We can weaken the subject confirmation method requirements a bit
 * for SAML SSO. A Bearer Assertion does not have to be signed by default if the outer Response is signed.
 */
public class SamlSSOAssertionValidator extends SamlAssertionValidator {

    private static final Logger LOG = LogUtils.getL7dLogger(SamlSSOAssertionValidator.class);

    private final boolean signedResponse;

    public SamlSSOAssertionValidator(boolean signedResponse) {
        this.signedResponse = signedResponse;
    }

    /**
     * Check the Subject Confirmation method requirements
     */
    protected void verifySubjectConfirmationMethod(
        SamlAssertionWrapper samlAssertion
    ) throws WSSecurityException {

        List<String> methods = samlAssertion.getConfirmationMethods();
        if (methods == null || methods.isEmpty()) {
            if (super.getRequiredSubjectConfirmationMethod() != null) {
                LOG.warning("A required subject confirmation method was not present");
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                          "invalidSAMLsecurity");
            } else if (super.isRequireStandardSubjectConfirmationMethod()) {
                LOG.warning("A standard subject confirmation method was not present");
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                          "invalidSAMLsecurity");
            }
        }

        boolean signed = samlAssertion.isSigned();
        boolean requiredMethodFound = false;
        boolean standardMethodFound = false;
        for (String method : methods) {
            if (OpenSAMLUtil.isMethodHolderOfKey(method)) {
                if (samlAssertion.getSubjectKeyInfo() == null) {
                    LOG.warning("There is no Subject KeyInfo to match the holder-of-key subject conf method");
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noKeyInSAMLToken");
                }

                // The assertion must have been signed for HOK
                if (!signed) {
                    LOG.warning("A holder-of-key assertion must be signed");
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
                }
                standardMethodFound = true;
            }

            if (method != null) {
                if (method.equals(super.getRequiredSubjectConfirmationMethod())) {
                    requiredMethodFound = true;
                }
                if (SAML2Constants.CONF_BEARER.equals(method)
                    || SAML1Constants.CONF_BEARER.equals(method)) {
                    standardMethodFound = true;
                    if (super.isRequireBearerSignature() && !signed && !signedResponse) {
                        LOG.warning("A Bearer Assertion was not signed");
                        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                                      "invalidSAMLsecurity");
                    }
                } else if (SAML2Constants.CONF_SENDER_VOUCHES.equals(method)
                    || SAML1Constants.CONF_SENDER_VOUCHES.equals(method)) {
                    standardMethodFound = true;
                }
            }
        }

        if (!requiredMethodFound && super.getRequiredSubjectConfirmationMethod() != null) {
            LOG.warning("A required subject confirmation method was not present");
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                          "invalidSAMLsecurity");
        }

        if (!standardMethodFound && super.isRequireStandardSubjectConfirmationMethod()) {
            LOG.warning("A standard subject confirmation method was not present");
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                      "invalidSAMLsecurity");
        }
    }
}
