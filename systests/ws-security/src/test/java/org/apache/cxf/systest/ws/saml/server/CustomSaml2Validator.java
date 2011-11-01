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
package org.apache.cxf.systest.ws.saml.server;

import java.util.List;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.SamlAssertionValidator;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AttributeStatement;

/**
 * This class does some trivial validation of a received SAML Assertion. It checks that it is
 * a SAML 2 Assertion, and checks the issuer name and confirmation method, and that it has
 * an Attribute Statement. 
 */
public class CustomSaml2Validator extends SamlAssertionValidator {
    
    @Override
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        Credential validatedCredential = super.validate(credential, data);
        AssertionWrapper assertion = validatedCredential.getAssertion();
        
        if (!"sts".equals(assertion.getIssuerString())) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
        }
        
        String confirmationMethod = assertion.getConfirmationMethods().get(0);
        if (!(OpenSAMLUtil.isMethodSenderVouches(confirmationMethod)
            || OpenSAMLUtil.isMethodHolderOfKey(confirmationMethod))) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
        }
        
        Assertion saml2Assertion = assertion.getSaml2();
        if (saml2Assertion == null) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
        }
        
        List<AttributeStatement> attributeStatements = saml2Assertion.getAttributeStatements();
        if (attributeStatements == null || attributeStatements.isEmpty()) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
        }
        
        return validatedCredential;
    }

}
