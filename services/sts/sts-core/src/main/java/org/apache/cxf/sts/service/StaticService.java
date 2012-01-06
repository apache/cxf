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

package org.apache.cxf.sts.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.cxf.common.logging.LogUtils;

/**
 * This class represents a (static) service. It can be spring-loaded with a set of Endpoint
 * Strings, which are compiled into a collection of (reg-ex) Patterns.
 */
public class StaticService implements ServiceMBean {
    private static final Logger LOG = LogUtils.getL7dLogger(StaticService.class);
    
    private String tokenType;
    private String keyType;
    private EncryptionProperties encryptionProperties;
    
    /**
     * a collection of compiled regular expression patterns
     */
    private final Collection<Pattern> endpointPatterns = new ArrayList<Pattern>();
    
    /**
     * Return true if the supplied address corresponds to a known address for this service
     */
    public boolean isAddressInEndpoints(String address) {
        String addressToMatch = address;
        if (addressToMatch == null) {
            addressToMatch = "";
        }
        for (Pattern endpointPattern : endpointPatterns) {
            final Matcher matcher = endpointPattern.matcher(addressToMatch);
            if (matcher.matches()) {
                LOG.fine("Address " + address + " matches with pattern " + endpointPattern);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get the default Token Type to be issued for this Service
     */
    public String getTokenType() {
        return tokenType;
    }
    
    /**
     * Set the default Token Type to be issued for this Service
     */
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
        LOG.fine("Setting Token Type: " + tokenType);
    }
    
    /**
     * Get the default Key Type to be issued for this Service
     */
    public String getKeyType() {
        return keyType;
    }
    
    /**
     * Set the default Key Type to be issued for this Service
     */
    public void setKeyType(String keyType) {
        this.keyType = keyType;
        LOG.fine("Setting Key Type: " + keyType);
    }
    
    /**
     * Set the list of endpoint addresses that correspond to this service
     */
    public void setEndpoints(List<String> endpoints) {
        if (endpoints != null) {
            for (String endpoint : endpoints) {
                try {
                    endpointPatterns.add(Pattern.compile(endpoint.trim()));
                } catch (PatternSyntaxException ex) {
                    LOG.severe(ex.getMessage());
                    throw ex;
                }
            }
        }
    }
    
    /**
     * Get the EncryptionProperties to be used to encrypt tokens issued for this service
     */
    public EncryptionProperties getEncryptionProperties() {
        return encryptionProperties;
    }
    
    /**
     * Set the EncryptionProperties to be used to encrypt tokens issued for this service
     */
    public void setEncryptionProperties(EncryptionProperties encryptionProperties) {
        this.encryptionProperties = encryptionProperties;
        LOG.fine("Setting encryption properties");
    }
    
}
