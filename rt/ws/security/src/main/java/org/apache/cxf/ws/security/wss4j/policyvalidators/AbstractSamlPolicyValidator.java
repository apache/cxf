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
import java.util.List;

import org.apache.wss4j.common.saml.SAMLKeyInfo;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.saml.DOMSAMLUtil;

/**
 * Some abstract functionality for validating SAML Assertions
 */
public abstract class AbstractSamlPolicyValidator extends AbstractSecurityPolicyValidator {

    /**
     * Check the holder-of-key requirements against the received assertion. The subject
     * credential of the SAML Assertion must have been used to sign some portion of
     * the message, thus showing proof-of-possession of the private/secret key. Alternatively,
     * the subject credential of the SAML Assertion must match a client certificate credential
     * when 2-way TLS is used.
     * @param assertionWrapper the SAML Assertion wrapper object
     * @param signedResults a list of all of the signed results
     */
    public boolean checkHolderOfKey(
        SamlAssertionWrapper assertionWrapper,
        List<WSSecurityEngineResult> signedResults,
        Certificate[] tlsCerts
    ) {
        return DOMSAMLUtil.checkHolderOfKey(assertionWrapper, signedResults, tlsCerts);
    }

    /**
     * Compare the credentials of the assertion to the credentials used in 2-way TLS or those
     * used to verify signatures.
     * Return true on a match
     * @param subjectKeyInfo the SAMLKeyInfo object
     * @param signedResults a list of all of the signed results
     * @return true if the credentials of the assertion were used to verify a signature
     */
    protected boolean compareCredentials(
        SAMLKeyInfo subjectKeyInfo,
        List<WSSecurityEngineResult> signedResults,
        Certificate[] tlsCerts
    ) {
        return DOMSAMLUtil.compareCredentials(subjectKeyInfo, signedResults, tlsCerts);
    }

}
