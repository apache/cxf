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
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.security.auth.x500.X500Principal;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.claims.ClaimsUtils;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.apache.cxf.sts.request.Lifetime;
import org.apache.cxf.sts.request.Participants;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderUtils;
import org.apache.cxf.ws.security.sts.provider.STSException;

/**
 * A default implementation to create a JWTClaims object. The Subject name is the name
 * of the current principal.
 */
public class DefaultJWTClaimsProvider implements JWTClaimsProvider {

    public static final long DEFAULT_MAX_LIFETIME = 60L * 60L * 12L;

    private static final Logger LOG = LogUtils.getL7dLogger(DefaultJWTClaimsProvider.class);
    private boolean useX500CN;

    private long lifetime = 60L * 30L;
    private long maxLifetime = DEFAULT_MAX_LIFETIME;
    private boolean failLifetimeExceedance = true;
    private boolean acceptClientLifetime;
    private long futureTimeToLive = 60L;
    private Map<String, String> claimTypeMap;

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

        handleConditions(jwtClaimsProviderParameters, claims);

        handleAudienceRestriction(jwtClaimsProviderParameters, claims);

        handleActAs(jwtClaimsProviderParameters, claims);

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
                    claims.setProperty(translateClaim(claim.getClaimType()), claimValues);
                }
            }
        }
    }

    protected void handleConditions(JWTClaimsProviderParameters jwtClaimsProviderParameters, JwtClaims claims) {
        TokenProviderParameters providerParameters = jwtClaimsProviderParameters.getProviderParameters();

        Instant currentDate = Instant.now();
        long currentTime = currentDate.getEpochSecond();

        // Set the defaults first
        claims.setIssuedAt(currentTime);
        claims.setNotBefore(currentTime);
        claims.setExpiryTime(currentTime + lifetime);

        Lifetime tokenLifetime = providerParameters.getTokenRequirements().getLifetime();
        if (lifetime > 0 && acceptClientLifetime && tokenLifetime != null
            && tokenLifetime.getCreated() != null && tokenLifetime.getExpires() != null) {
            final Instant creationTime;
            Instant expirationTime;
            try {
                creationTime = ZonedDateTime.parse(tokenLifetime.getCreated()).toInstant();
                expirationTime = ZonedDateTime.parse(tokenLifetime.getExpires()).toInstant();
            } catch (DateTimeParseException ex) {
                LOG.fine("Error in parsing Timestamp Created or Expiration Strings");
                throw new STSException(
                                       "Error in parsing Timestamp Created or Expiration Strings",
                                       STSException.INVALID_TIME
                    );
            }

            // Check to see if the created time is in the future
            Instant validCreation = Instant.now();
            if (futureTimeToLive > 0) {
                validCreation = validCreation.plusSeconds(futureTimeToLive);
            }
            if (creationTime.isAfter(validCreation)) {
                LOG.fine("The Created Time is too far in the future");
                throw new STSException("The Created Time is too far in the future", STSException.INVALID_TIME);
            }

            long requestedLifetime = Duration.between(creationTime, expirationTime).getSeconds();
            if (requestedLifetime > getMaxLifetime()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Requested lifetime [").append(requestedLifetime);
                sb.append(" sec] exceed configured maximum lifetime [").append(getMaxLifetime());
                sb.append(" sec]");
                LOG.warning(sb.toString());
                if (isFailLifetimeExceedance()) {
                    throw new STSException("Requested lifetime exceeds maximum lifetime",
                                           STSException.INVALID_TIME);
                }
                expirationTime = creationTime.plusSeconds(getMaxLifetime());
            }

            long creationTimeInSeconds = creationTime.getEpochSecond();
            claims.setIssuedAt(creationTimeInSeconds);
            claims.setNotBefore(creationTimeInSeconds);
            claims.setExpiryTime(expirationTime.getEpochSecond());
        }
    }

    /**
     * Set the audience restriction claim. The Audiences are from an AppliesTo address, and the wst:Participants
     * (if either exist).
     */
    protected void handleAudienceRestriction(
        JWTClaimsProviderParameters jwtClaimsProviderParameters, JwtClaims claims
    ) {
        TokenProviderParameters providerParameters = jwtClaimsProviderParameters.getProviderParameters();

        List<String> audiences = new ArrayList<>();
        String appliesToAddress = providerParameters.getAppliesToAddress();
        if (appliesToAddress != null) {
            audiences.add(appliesToAddress);
        }

        Participants participants = providerParameters.getTokenRequirements().getParticipants();
        if (participants != null) {
            String address = TokenProviderUtils.extractAddressFromParticipantsEPR(participants.getPrimaryParticipant());
            if (address != null) {
                audiences.add(address);
            }

            if (participants.getParticipants() != null) {
                for (Object participant : participants.getParticipants()) {
                    if (participant != null) {
                        address = TokenProviderUtils.extractAddressFromParticipantsEPR(participant);
                        if (address != null) {
                            audiences.add(address);
                        }
                    }
                }
            }
        }
        if (!audiences.isEmpty()) {
            claims.setAudiences(audiences);
        }

    }

    protected void handleActAs(
        JWTClaimsProviderParameters jwtClaimsProviderParameters, JwtClaims claims
    ) {
        TokenProviderParameters providerParameters = jwtClaimsProviderParameters.getProviderParameters();

        if (providerParameters.getTokenRequirements().getActAs() != null) {
            ReceivedToken receivedToken = providerParameters.getTokenRequirements().getActAs();
            if (receivedToken.getState().equals(STATE.VALID)) {
                claims.setClaim("ActAs", receivedToken.getPrincipal().getName());
            }
        }
    }

    private String translateClaim(String claimType) {
        if (claimTypeMap == null || !claimTypeMap.containsKey(claimType)) {
            return claimType;
        }
        return claimTypeMap.get(claimType);
    }

    public boolean isUseX500CN() {
        return useX500CN;
    }

    public void setUseX500CN(boolean useX500CN) {
        this.useX500CN = useX500CN;
    }

    /**
     * Get how long (in seconds) a client-supplied Created Element is allowed to be in the future.
     * The default is 60 seconds to avoid common problems relating to clock skew.
     */
    public long getFutureTimeToLive() {
        return futureTimeToLive;
    }

    /**
     * Set how long (in seconds) a client-supplied Created Element is allowed to be in the future.
     * The default is 60 seconds to avoid common problems relating to clock skew.
     */
    public void setFutureTimeToLive(long futureTimeToLive) {
        this.futureTimeToLive = futureTimeToLive;
    }

    /**
     * Set the default lifetime in seconds for issued JWT tokens
     * @param lifetime default lifetime in seconds
     */
    public void setLifetime(long lifetime) {
        this.lifetime = lifetime;
    }

    /**
     * Get the default lifetime in seconds for issued JWT token where requestor
     * doesn't specify a lifetime element
     * @return the lifetime in seconds
     */
    public long getLifetime() {
        return lifetime;
    }

    /**
     * Set the maximum lifetime in seconds for issued JWT tokens
     * @param maxLifetime maximum lifetime in seconds
     */
    public void setMaxLifetime(long maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    /**
     * Get the maximum lifetime in seconds for issued JWT token
     * if requestor specifies lifetime element
     * @return the maximum lifetime in seconds
     */
    public long getMaxLifetime() {
        return maxLifetime;
    }

    /**
     * Is client lifetime element accepted
     * Default: false
     */
    public boolean isAcceptClientLifetime() {
        return this.acceptClientLifetime;
    }

    /**
     * Set whether client lifetime is accepted
     */
    public void setAcceptClientLifetime(boolean acceptClientLifetime) {
        this.acceptClientLifetime = acceptClientLifetime;
    }

    /**
     * If requested lifetime exceeds shall it fail (default)
     * or overwrite with maximum lifetime
     */
    public boolean isFailLifetimeExceedance() {
        return this.failLifetimeExceedance;
    }

    /**
     * If requested lifetime exceeds shall it fail (default)
     * or overwrite with maximum lifetime
     */
    public void setFailLifetimeExceedance(boolean failLifetimeExceedance) {
        this.failLifetimeExceedance = failLifetimeExceedance;
    }

    public Map<String, String> getClaimTypeMap() {
        return claimTypeMap;
    }

    /**
     * Specify a way to map ClaimType URIs to custom ClaimTypes
     * @param claimTypeMap
     */
    public void setClaimTypeMap(Map<String, String> claimTypeMap) {
        this.claimTypeMap = claimTypeMap;
    }

}
