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
package org.apache.cxf.sts.token.provider.jwt;

import java.security.Principal;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Logger;

import javax.security.auth.x500.X500Principal;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.claims.ClaimsUtils;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.ws.security.sts.provider.STSException;

/**
 * A default implementation to create a JWTClaims object. The Subject name is the name
 * of the current principal. 
 */
public class DefaultJWTClaimsProvider implements JWTClaimsProvider {
    
    private static final Logger LOG = LogUtils.getL7dLogger(DefaultJWTClaimsProvider.class);
    private boolean useX500CN;
                                                            
    /**
     * Get a JwtClaims object.
     */
    public JwtClaims getJwtClaims(JWTClaimsProviderParameters jwtClaimsProviderParameters) {
        
        JwtClaims claims = new JwtClaims();
        claims.setSubject(getSubjectName(jwtClaimsProviderParameters));
        claims.setTokenId(UUID.randomUUID().toString());
        
        // Set the Issuer
        String issuer = jwtClaimsProviderParameters.getIssuer();
        if (issuer == null) {
            STSPropertiesMBean stsProperties = jwtClaimsProviderParameters.getProviderParameters().getStsProperties();
            claims.setIssuer(stsProperties.getIssuer());
        } else {
            claims.setIssuer(issuer);
        }
        
        handleWSTrustClaims(jwtClaimsProviderParameters, claims);
        
        Date currentDate = new Date();
        claims.setIssuedAt(currentDate.getTime() / 1000L);
        long currentTime = currentDate.getTime() + 300L * 1000L;
        currentDate.setTime(currentTime);
        claims.setExpiryTime(currentDate.getTime() / 1000L);
        
        return claims;
    }
    
    protected String getSubjectName(JWTClaimsProviderParameters jwtClaimsProviderParameters) {
        Principal principal = getPrincipal(jwtClaimsProviderParameters);
        if (principal == null) {
            LOG.fine("Error in getting principal");
            throw new STSException("Error in getting principal", STSException.REQUEST_FAILED);
        }
        
        String subjectName = principal.getName();
        if (principal instanceof X500Principal) {
            // Just use the "cn" instead of the entire DN
            try {
                String principalName = principal.getName();
                int index = principalName.indexOf('=');
                principalName = principalName.substring(index + 1, principalName.indexOf(',', index));
                subjectName = principalName;
            } catch (Throwable ex) {
                subjectName = principal.getName();
                //Ignore, not X500 compliant thus use the whole string as the value
            }
        }
        
        return subjectName;
    }
        
    /**
     * Get the Principal (which is used as the Subject). By default, we check the following (in order):
     *  - A valid OnBehalfOf principal
     *  - A valid ActAs principal
     *  - A valid principal associated with a token received as ValidateTarget
     *  - The principal associated with the request. We don't need to check to see if it is "valid" here, as it
     *    is not parsed by the STS (but rather the WS-Security layer).
     */
    protected Principal getPrincipal(JWTClaimsProviderParameters jwtClaimsProviderParameters) {
        TokenProviderParameters providerParameters = jwtClaimsProviderParameters.getProviderParameters();

        Principal principal = null;
        //TokenValidator in IssueOperation has validated the ReceivedToken
        //if validation was successful, the principal was set in ReceivedToken 
        if (providerParameters.getTokenRequirements().getOnBehalfOf() != null) {
            ReceivedToken receivedToken = providerParameters.getTokenRequirements().getOnBehalfOf();
            if (receivedToken.getState().equals(STATE.VALID)) {
                principal = receivedToken.getPrincipal();
            }
        } else if (providerParameters.getTokenRequirements().getActAs() != null) {
            ReceivedToken receivedToken = providerParameters.getTokenRequirements().getActAs();
            if (receivedToken.getState().equals(STATE.VALID)) {
                principal = receivedToken.getPrincipal();
            }
        } else if (providerParameters.getTokenRequirements().getValidateTarget() != null) {
            ReceivedToken receivedToken = providerParameters.getTokenRequirements().getValidateTarget();
            if (receivedToken.getState().equals(STATE.VALID)) {
                principal = receivedToken.getPrincipal();
            }
        } else {
            principal = providerParameters.getPrincipal();
        }

        return principal;
    }
    
    protected void handleWSTrustClaims(JWTClaimsProviderParameters jwtClaimsProviderParameters, JwtClaims claims) {
        TokenProviderParameters providerParameters = jwtClaimsProviderParameters.getProviderParameters();
        
        // Handle Claims
        ProcessedClaimCollection retrievedClaims = ClaimsUtils.processClaims(providerParameters);
        if (retrievedClaims != null) {
            Iterator<ProcessedClaim> claimIterator = retrievedClaims.iterator();
            while (claimIterator.hasNext()) {
                ProcessedClaim claim = claimIterator.next();
                if (claim.getClaimType() != null && claim.getValues() != null && !claim.getValues().isEmpty()) {
                    Object claimValues = claim.getValues();
                    if (claim.getValues().size() == 1) {
                        claimValues = claim.getValues().get(0);
                    }
                    claims.setProperty(claim.getClaimType().toString(), claimValues);
                }
            }
        }
    }
    
    public boolean isUseX500CN() {
        return useX500CN;
    }

    public void setUseX500CN(boolean useX500CN) {
        this.useX500CN = useX500CN;
    }
}
