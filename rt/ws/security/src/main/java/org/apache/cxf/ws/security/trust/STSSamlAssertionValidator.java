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

package org.apache.cxf.ws.security.trust;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.SamlAssertionValidator;

/**
 * This class validates a SAML Assertion by invoking the SamlAssertionValidator in WSS4J. It
 * overrides the signature verification, so that if the signature is not trusted, it just sets
 * a boolean. The STSTokenValidator can parse this tag and dispatch the Assertion to the STS
 * for validation.
 */
public class STSSamlAssertionValidator extends SamlAssertionValidator {
    private static final Logger LOG = LogUtils.getL7dLogger(STSSamlAssertionValidator.class);

    private boolean trustVerificationSucceeded;

    /**
     * Try to verify trust on the assertion. If it fails, then set a boolean and return.
     * @param assertion The signed Assertion
     * @param data The RequestData context
     * @return A Credential instance
     * @throws WSSecurityException
     */
    @Override
    protected Credential verifySignedAssertion(
        SamlAssertionWrapper assertion,
        RequestData data
    ) throws WSSecurityException {
        try {
            Credential credential = super.verifySignedAssertion(assertion, data);
            trustVerificationSucceeded = true;
            return credential;
        } catch (WSSecurityException ex) {
            LOG.log(Level.WARNING, "Local trust verification of SAML assertion failed: " + ex.getMessage(),
                    ex);
            trustVerificationSucceeded = false;
            return null;
        }
    }

    /**
     * Return if trust verification on the signature of the assertion succeeded.
     * @return if trust verification on the signature of the assertion succeeded
     */
    public boolean isTrustVerificationSucceeded() {
        return trustVerificationSucceeded;
    }

}
