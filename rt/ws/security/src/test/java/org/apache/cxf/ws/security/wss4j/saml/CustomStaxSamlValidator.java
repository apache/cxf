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

package org.apache.cxf.ws.security.wss4j.saml;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.stax.impl.securityToken.SamlSecurityTokenImpl;
import org.apache.wss4j.stax.securityToken.SamlSecurityToken;
import org.apache.wss4j.stax.validate.SamlTokenValidatorImpl;
import org.apache.wss4j.stax.validate.TokenContext;
import org.apache.xml.security.stax.securityToken.InboundSecurityToken;

/**
 * A trivial custom Validator for a SAML Assertion. It makes sure that the issuer is
 * "www.example.com", checks the version of the assertion, and checks the subject confirmation
 * method.
 */
public class CustomStaxSamlValidator extends SamlTokenValidatorImpl {

    private boolean requireSAML1Assertion = true;
    private boolean requireSenderVouches = true;

    public void setRequireSAML1Assertion(boolean requireSAML1Assertion) {
        this.requireSAML1Assertion = requireSAML1Assertion;
    }

    public void setRequireSenderVouches(boolean requireSenderVouches) {
        this.requireSenderVouches = requireSenderVouches;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends SamlSecurityToken & InboundSecurityToken> T validate(
        final SamlAssertionWrapper samlAssertionWrapper,
        final InboundSecurityToken subjectSecurityToken,
        final TokenContext tokenContext
    ) throws WSSecurityException {
        //jdk 1.6 compiler bug? http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6302954
        //type parameters of <T>T cannot be determined; no unique maximal instance exists for type variable T with
        // upper bounds org.apache.wss4j.stax.securityToken.SamlSecurityToken,
        // org.apache.wss4j.stax.securityToken.SamlSecurityToken,
        // org.apache.xml.security.stax.ext.securityToken.InboundSecurityToken
        //works fine on jdk 1.7
        final SamlSecurityToken token =
            super.</*fake @see above*/SamlSecurityTokenImpl>
                        validate(samlAssertionWrapper, subjectSecurityToken, tokenContext);

        //
        // Do some custom validation on the assertion
        //
        if (!"www.example.com".equals(samlAssertionWrapper.getIssuerString())) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
        }

        if (requireSAML1Assertion && samlAssertionWrapper.getSaml1() == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
        } else if (!requireSAML1Assertion && samlAssertionWrapper.getSaml2() == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
        }

        String confirmationMethod = samlAssertionWrapper.getConfirmationMethods().get(0);
        if (confirmationMethod == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
        }
        if (requireSenderVouches && !OpenSAMLUtil.isMethodSenderVouches(confirmationMethod)) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
        } else if (!requireSenderVouches
            && !OpenSAMLUtil.isMethodHolderOfKey(confirmationMethod)) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
        }

        return (T)token;
    }

}