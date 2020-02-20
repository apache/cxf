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
package org.apache.cxf.sts.token.provider;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.request.Lifetime;
import org.apache.cxf.sts.request.Participants;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.wss4j.common.saml.bean.AudienceRestrictionBean;
import org.apache.wss4j.common.saml.bean.ConditionsBean;

/**
 * A default implementation of the ConditionsProvider interface.
 */
public class DefaultConditionsProvider implements ConditionsProvider {

    public static final long DEFAULT_MAX_LIFETIME = 60L * 60L * 12L;

    private static final Logger LOG = LogUtils.getL7dLogger(DefaultConditionsProvider.class);

    private long lifetime = 60L * 30L;
    private long maxLifetime = DEFAULT_MAX_LIFETIME;
    private boolean failLifetimeExceedance = true;
    private boolean acceptClientLifetime;
    private long futureTimeToLive = 60L;

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
     * Set the default lifetime in seconds for issued SAML tokens
     * @param lifetime default lifetime in seconds
     */
    public void setLifetime(long lifetime) {
        this.lifetime = lifetime;
    }

    /**
     * Get the default lifetime in seconds for issued SAML token where requestor
     * doesn't specify a lifetime element
     * @return the lifetime in seconds
     */
    @Override
    public long getLifetime() {
        return lifetime;
    }

    /**
     * Set the maximum lifetime in seconds for issued SAML tokens
     * @param maxLifetime maximum lifetime in seconds
     */
    public void setMaxLifetime(long maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    /**
     * Get the maximum lifetime in seconds for issued SAML token
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


    /**
     * Get a ConditionsBean object.
     */
    @Override
    public ConditionsBean getConditions(TokenProviderParameters providerParameters) {
        ConditionsBean conditions = new ConditionsBean();

        Lifetime tokenLifetime = providerParameters.getTokenRequirements().getLifetime();
        if (lifetime > 0) {
            if (acceptClientLifetime && tokenLifetime != null
                    && (tokenLifetime.getCreated() != null || tokenLifetime.getExpires() != null)) {
                Instant creationTime = parsedInstantOrDefault(tokenLifetime.getCreated(), Instant.now());
                Instant expirationTime = parsedInstantOrDefault(tokenLifetime.getExpires(),
                        creationTime.plusSeconds(lifetime));

                // Check to see if the created time is in the future
                Instant validCreation = Instant.now();
                if (futureTimeToLive > 0) {
                    validCreation = validCreation.plusSeconds(futureTimeToLive);
                }
                if (creationTime.isAfter(validCreation)) {
                    LOG.fine("The Created Time is too far in the future");
                    throw new STSException(
                        "The Created Time is too far in the future", STSException.INVALID_TIME
                    );
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

                conditions.setNotAfter(expirationTime);
                conditions.setNotBefore(creationTime);

            } else {
                conditions.setTokenPeriodSeconds(lifetime);
            }
        } else {
            conditions.setTokenPeriodMinutes(5);
        }

        List<AudienceRestrictionBean> audienceRestrictions = createAudienceRestrictions(providerParameters);
        if (audienceRestrictions != null && !audienceRestrictions.isEmpty()) {
            conditions.setAudienceRestrictions(audienceRestrictions);
        }

        return conditions;
    }

    private Instant parsedInstantOrDefault(String dateTime, Instant defaultInstant) {
        if (dateTime == null || dateTime.isEmpty()) {
            return defaultInstant;
        }
        try {
            return ZonedDateTime.parse(dateTime).toInstant();
        } catch (DateTimeParseException ex) {
            LOG.fine("Error in parsing Timestamp Created or Expiration Strings");
            throw new STSException(
                "Error in parsing Timestamp Created or Expiration Strings",
                STSException.INVALID_TIME
            );
        }
    }

    /**
     * Create a list of AudienceRestrictions to be added to the Conditions Element of the
     * issued Assertion. The default behaviour is to add a single Audience URI per
     * AudienceRestriction Element. The Audience URIs are from an AppliesTo address, and
     * the wst:Participants (if either exist).
     */
    protected List<AudienceRestrictionBean> createAudienceRestrictions(
        TokenProviderParameters providerParameters
    ) {
        List<AudienceRestrictionBean> audienceRestrictions = new ArrayList<>();
        String appliesToAddress = providerParameters.getAppliesToAddress();
        if (appliesToAddress != null) {
            AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
            audienceRestriction.setAudienceURIs(Collections.singletonList(appliesToAddress));
            audienceRestrictions.add(audienceRestriction);
        }

        Participants participants = providerParameters.getTokenRequirements().getParticipants();
        if (participants != null) {
            String address =
                extractAddressFromParticipantsEPR(participants.getPrimaryParticipant());
            if (address != null) {
                AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
                audienceRestriction.setAudienceURIs(Collections.singletonList(address));
                audienceRestrictions.add(audienceRestriction);
            }

            if (participants.getParticipants() != null) {
                for (Object participant : participants.getParticipants()) {
                    if (participant != null) {
                        address = extractAddressFromParticipantsEPR(participant);
                        if (address != null) {
                            AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
                            audienceRestriction.setAudienceURIs(Collections.singletonList(address));
                            audienceRestrictions.add(audienceRestriction);
                        }
                    }
                }
            }
        }

        return audienceRestrictions;
    }

    /**
     * Extract an address from a Participants EPR DOM element
     */
    protected String extractAddressFromParticipantsEPR(Object participants) {
        return TokenProviderUtils.extractAddressFromParticipantsEPR(participants);
    }

}
