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
package org.apache.cxf.systest.ws.saml;

import java.util.List;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.SamlAssertionValidator;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Audience;
import org.opensaml.saml2.core.AudienceRestriction;
import org.opensaml.saml2.core.Conditions;

/**
 * This class checks that the Audiences received as part of AudienceRestrictions match a set 
 * list of endpoints.
 */
public class Saml2AudienceRestrictionValidator extends SamlAssertionValidator {
    
    private List<String> endpointAddresses;
    
    @Override
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        Credential validatedCredential = super.validate(credential, data);
        AssertionWrapper assertion = validatedCredential.getAssertion();
        
        Assertion saml2Assertion = assertion.getSaml2();
        if (saml2Assertion == null) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
        }
        
        return validatedCredential;
    }
    
    @Override
    public void checkConditions(AssertionWrapper samlAssertion) throws WSSecurityException {
        super.checkConditions(samlAssertion);
        
        if (endpointAddresses == null || endpointAddresses.isEmpty()) {
            return;
        }
        
        Conditions conditions = samlAssertion.getSaml2().getConditions();
        if (conditions != null && conditions.getAudienceRestrictions() != null) {
            boolean foundAddress = false;
            for (AudienceRestriction audienceRestriction : conditions.getAudienceRestrictions()) {
                List<Audience> audiences = audienceRestriction.getAudiences();
                if (audiences != null) {
                    for (Audience audience : audiences) {
                        String audienceURI = audience.getAudienceURI();
                        if (endpointAddresses.contains(audienceURI)) {
                            foundAddress = true;
                            break;
                        }
                    }
                }
            }
            
            if (!foundAddress) {
                throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
            }
        }
    }

    public List<String> getEndpointAddresses() {
        return endpointAddresses;
    }

    public void setEndpointAddresses(List<String> endpointAddresses) {
        this.endpointAddresses = endpointAddresses;
    }

}
