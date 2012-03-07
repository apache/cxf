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
package org.apache.cxf.rs.security.oauth2.common;

import java.util.Collections;
import java.util.List;

/**
 * Represents a registered third-party Client application
 */
public class Client {
    
    private String clientId;
    // TODO: Consider introducing ClientCredentials instead
    // so that a secret, public key, etc can be kept
    private String clientSecret;
    
    private String applicationName;
    private String applicationDescription;
    private String applicationWebUri;
    private String applicationLogoUri;
    private List<String> redirectUris = Collections.emptyList();
    
    private boolean isConfidential;
    private List<String> allowedGrantTypes = Collections.emptyList();
    
    private UserSubject subject;
        
    public Client(String clientId, String clientSecret, boolean isConfidential) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.isConfidential = isConfidential;
    }

    public Client(String clientId, 
                  String clientSecret,
                  boolean isConfidential,
                  String applicationName,
                  String applicationWebUri) {
        this(clientId, clientSecret, isConfidential);
        this.applicationName = applicationName;
        this.applicationWebUri = applicationWebUri;
        
    }
    
    /**
     * Gets the consumer registration id
     * @return the consumer key
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Gets the secret key
     * @return the secret key
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Gets the name of the third-party application
     * this client represents
     * @return the application name
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Sets the name of the third-party application
     * this client represents
     * @param applicationName the name
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * Gets the public URI of the third-party application.
     * For example, this property can be used to validate 
     * request token callbacks
     * @return the application URI
     */
    public String getApplicationWebUri() {
        return applicationWebUri;
    }

    /**
     * Sets the public URI of the third-party application.
     */
    public void setApplicationWebUri(String applicationWebUri) {
        this.applicationWebUri = applicationWebUri;
    }

    /**
     * Sets the description of the third-party application.
     */
    public void setApplicationDescription(String applicationDescription) {
        this.applicationDescription = applicationDescription;
    }

    /**
     * Gets the description of the third-party application.
     * @return the application description
     */
    public String getApplicationDescription() {
        return applicationDescription;
    }
    
    /**
     * Sets the uri pointing to a client logo image.
     * At the moment it must be a relative URI
     * @param logoPath
     */
    public void setApplicationLogoUri(String logoPath) {
        this.applicationLogoUri = logoPath;
    }

    public String getApplicationLogoUri() {
        return applicationLogoUri;
    }

    public void setConfidential(boolean isConf) {
        this.isConfidential = isConf;
    }

    public boolean isConfidential() {
        return isConfidential;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setAllowedGrantTypes(List<String> allowedGrantTypes) {
        this.allowedGrantTypes = allowedGrantTypes;
    }

    public List<String> getAllowedGrantTypes() {
        return allowedGrantTypes;
    }

    public void setSubject(UserSubject subject) {
        this.subject = subject;
    }

    public UserSubject getSubject() {
        return subject;
    }
}
