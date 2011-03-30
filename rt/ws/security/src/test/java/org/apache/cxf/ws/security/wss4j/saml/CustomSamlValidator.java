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

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.SamlAssertionValidator;

/**
 * A trivial custom Validator for a SAML Assertion. It makes sure that the issuer is 
 * "www.example.com", checks the version of the assertion, and checks the subject confirmation
 * method.
 */
public class CustomSamlValidator extends SamlAssertionValidator {
    
    private boolean requireSAML1Assertion = true;
    private boolean requireSenderVouches = true;
    
    public void setRequireSAML1Assertion(boolean requireSAML1Assertion) {
        this.requireSAML1Assertion = requireSAML1Assertion;
    }
    
    public void setRequireSenderVouches(boolean requireSenderVouches) {
        this.requireSenderVouches = requireSenderVouches;
    }
    
    @Override
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        Credential returnedCredential = super.validate(credential, data);
        
        //
        // Do some custom validation on the assertion
        //
        AssertionWrapper assertion = credential.getAssertion();
        if (!"www.example.com".equals(assertion.getIssuerString())) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
        }
        
        if (requireSAML1Assertion && assertion.getSaml1() == null) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
        } else if (!requireSAML1Assertion && assertion.getSaml2() == null) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
        }

        String confirmationMethod = assertion.getConfirmationMethods().get(0);
        if (confirmationMethod == null) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
        }
        if (requireSenderVouches && !OpenSAMLUtil.isMethodSenderVouches(confirmationMethod)) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
        } else if (!requireSenderVouches 
            && !OpenSAMLUtil.isMethodHolderOfKey(confirmationMethod)) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
        }
        
        return returnedCredential;
    }
    
}