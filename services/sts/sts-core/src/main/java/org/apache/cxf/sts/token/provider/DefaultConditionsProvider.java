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

import java.text.ParseException;
import java.util.Date;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.request.Lifetime;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.ws.security.saml.ext.bean.ConditionsBean;
import org.apache.ws.security.util.XmlSchemaDateFormat;
import org.joda.time.DateTime;

/**
 * A default implementation of the ConditionsProvider interface.
 */
public class DefaultConditionsProvider implements ConditionsProvider {
    
    public static final long DEFAULT_MAX_LIFETIME = 60L * 60L;
    
    private static final Logger LOG = LogUtils.getL7dLogger(DefaultConditionsProvider.class);
    
    private long lifetime = 300L;
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
     * @param default lifetime in seconds
     */
    public void setLifetime(long lifetime) {
        this.lifetime = lifetime;
    }
    
    /**
     * Get the default lifetime in seconds for issued SAML token where requestor
     * doesn't specify a lifetime element
     * @return the lifetime in seconds
     */
    public long getLifetime() {
        return lifetime;
    }
    
    /**
     * Set the maximum lifetime in seconds for issued SAML tokens
     * @param maximum lifetime in seconds
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
    public ConditionsBean getConditions(TokenProviderParameters providerParameters) {
        ConditionsBean conditions = new ConditionsBean();
        if (lifetime > 0) {
            Lifetime tokenLifetime = providerParameters.getTokenRequirements().getLifetime();
            if (acceptClientLifetime && tokenLifetime != null
                && tokenLifetime.getCreated() != null && tokenLifetime.getExpires() != null) {
                try {
                    XmlSchemaDateFormat fmt = new XmlSchemaDateFormat();
                    Date creationTime = fmt.parse(tokenLifetime.getCreated());
                    Date expirationTime = fmt.parse(tokenLifetime.getExpires());
                    
                    // Check to see if the created time is in the future
                    Date validCreation = new Date();
                    long currentTime = validCreation.getTime();
                    if (futureTimeToLive > 0) {
                        validCreation.setTime(currentTime + futureTimeToLive * 1000);
                    }
                    if (creationTime != null && creationTime.after(validCreation)) {
                        LOG.fine("The Created Time is too far in the future");
                        throw new STSException(
                            "The Created Time is too far in the future", STSException.INVALID_TIME
                        );
                    }
                    
                    long requestedLifetime = expirationTime.getTime() - creationTime.getTime();
                    if (requestedLifetime > (getMaxLifetime() * 1000L)) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Requested lifetime [").append(requestedLifetime / 1000L);
                        sb.append(" sec] exceed configured maximum lifetime [").append(getMaxLifetime());
                        sb.append(" sec]");
                        LOG.warning(sb.toString());
                        if (isFailLifetimeExceedance()) {
                            throw new STSException("Requested lifetime exceeds maximum lifetime",
                                    STSException.INVALID_TIME);
                        } else {
                            expirationTime.setTime(creationTime.getTime() + (getMaxLifetime() * 1000L));
                        }
                    }
                    
                    DateTime creationDateTime = new DateTime(creationTime.getTime());
                    DateTime expirationDateTime = new DateTime(expirationTime.getTime());
                    
                    conditions.setNotAfter(expirationDateTime);
                    conditions.setNotBefore(creationDateTime);
                } catch (ParseException e) {
                    LOG.warning("Failed to parse life time element: " + e.getMessage());
                    conditions.setTokenPeriodMinutes((int)(lifetime / 60L));
                }
                
            } else {
                conditions.setTokenPeriodMinutes((int)(lifetime / 60L));
            }
        } else {
            conditions.setTokenPeriodMinutes(5);
        }
        conditions.setAudienceURI(providerParameters.getAppliesToAddress());
        
        return conditions;
    }

}
