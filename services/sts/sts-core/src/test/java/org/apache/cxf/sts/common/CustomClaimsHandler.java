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
package org.apache.cxf.sts.common;

import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimTypes;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.apache.cxf.sts.common.CustomClaimParser.CustomRequestClaim;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSInteger;
import org.opensaml.saml.saml2.core.AttributeValue;

/**
 * A custom ClaimsHandler implementation for use in the tests.
 */
public class CustomClaimsHandler implements ClaimsHandler {
    
    private static List<URI> knownURIs = new ArrayList<URI>();
    private static final URI ROLE_CLAIM = 
            URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role");
    
    static {
        knownURIs.add(ClaimTypes.FIRSTNAME);
        knownURIs.add(ClaimTypes.LASTNAME);
        knownURIs.add(ClaimTypes.EMAILADDRESS);
        knownURIs.add(ClaimTypes.STREETADDRESS);
        knownURIs.add(ClaimTypes.MOBILEPHONE);
        knownURIs.add(ROLE_CLAIM);
    }

    public List<URI> getSupportedClaimTypes() {
        return knownURIs;
    }    
    
    public ProcessedClaimCollection retrieveClaimValues(
            ClaimCollection claims, ClaimsParameters parameters) {
      
        if (claims != null && claims.size() > 0) {
            ProcessedClaimCollection claimCollection = new ProcessedClaimCollection();
            for (Claim requestClaim : claims) {
                ProcessedClaim claim = new ProcessedClaim();
                claim.setClaimType(requestClaim.getClaimType());
                if (ClaimTypes.FIRSTNAME.equals(requestClaim.getClaimType())) {
                    if (requestClaim instanceof CustomRequestClaim) {
                        CustomRequestClaim customClaim = (CustomRequestClaim) requestClaim;
                        String customName = customClaim.getValues().get(0) + "@" 
                            + customClaim.getScope();
                        claim.addValue(customName);
                    } else {
                        claim.addValue("alice");
                    }
                } else if (ClaimTypes.LASTNAME.equals(requestClaim.getClaimType())) {
                    claim.addValue("doe");
                } else if (ClaimTypes.EMAILADDRESS.equals(requestClaim.getClaimType())) {
                    claim.addValue("alice@cxf.apache.org");
                } else if (ClaimTypes.STREETADDRESS.equals(requestClaim.getClaimType())) {
                    claim.addValue("1234 1st Street");
                } else if (ClaimTypes.MOBILEPHONE.equals(requestClaim.getClaimType())) {
                    // Test custom (Integer) attribute value
                    XMLObjectBuilderFactory builderFactory = 
                        XMLObjectProviderRegistrySupport.getBuilderFactory();
                    
                    @SuppressWarnings("unchecked")
                    XMLObjectBuilder<XSInteger> xsIntegerBuilder = 
                        (XMLObjectBuilder<XSInteger>)builderFactory.getBuilder(XSInteger.TYPE_NAME);
                    XSInteger attributeValue = 
                        xsIntegerBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSInteger.TYPE_NAME);
                    attributeValue.setValue(185912592);
                    
                    claim.addValue(attributeValue);

                } else if (ROLE_CLAIM.equals(requestClaim.getClaimType())) {
                    if (requestClaim.getValues().size() > 0) {
                        for (Object requestedRole : requestClaim.getValues()) {
                            if (isUserInRole(parameters.getPrincipal(), requestedRole.toString())) {
                                claim.addValue(requestedRole);
                            }
                        }
                        if (claim.getValues().size() == 0) {
                            continue;
                        }
                    } else {
                        // If no specific role was requested return DUMMY role for user 
                        claim.addValue("DUMMY");
                    }
                }                
                claimCollection.add(claim);
            }
            return claimCollection;
        }
        
        return null;
    }

    private boolean isUserInRole(Principal principal, String requestedRole) {
        return true;
    }
        
}
