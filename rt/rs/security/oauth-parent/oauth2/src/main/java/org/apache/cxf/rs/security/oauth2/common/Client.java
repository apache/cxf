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

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents a registered third-party Client application
 */
public class Client implements Serializable {
    
    private static final long serialVersionUID = -5550840247125850922L;
    
    private String clientId;
    // TODO: Consider introducing ClientCredentials instead
    // so that a secret, public key, etc can be kept
    private String clientSecret;
    
    private String applicationName;
    private String applicationDescription;
    private String applicationWebUri;
    private String applicationLogoUri;
    private List<String> redirectUris = new LinkedList<String>();
    
    private boolean isConfidential;
    private List<String> allowedGrantTypes = new LinkedList<String>();
    private List<String> registeredScopes = new LinkedList<String>();
    private List<String> registeredAudiences = new LinkedList<String>();
    
    private Map<String, String> properties = new HashMap<String, String>();
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
     * Gets the client registration id
     * @return the consumer key
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Gets the client secret
     * @return the secret
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
     * @return the application URI
     */
    public String getApplicationWebUri() {
        return applicationWebUri;
    }

    /**
     * Sets the public URI of the third-party application.
     * @param applicationWebUri the application URI
     */
    public void setApplicationWebUri(String applicationWebUri) {
        this.applicationWebUri = applicationWebUri;
    }

    /**
     * Sets the description of the third-party application.
     * @param applicationDescription the description
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
     * Sets the URI pointing to a logo image of the client application
     * @param logoPath the logo URI
     */
    public void setApplicationLogoUri(String logoPath) {
        this.applicationLogoUri = logoPath;
    }

    /**
     * Get the URI pointing to a logo image of the client application
     * @return the logo URI
     */
    public String getApplicationLogoUri() {
        return applicationLogoUri;
    }

    /**
     * Sets the confidentiality status of this client application.
     * This can be used to restrict which OAuth2 flows this client
     * can participate in.
     * 
     * @param isConf true if the client is confidential
     */
    public void setConfidential(boolean isConf) {
        this.isConfidential = isConf;
    }

    /**
     * Gets the confidentiality status of this client application.
     * @return the confidentiality status
     */
    public boolean isConfidential() {
        return isConfidential;
    }

    /**
     * Sets a list of URIs the AuthorizationService
     * may return the authorization code to.
     * @param redirectUris the redirect uris
     */
    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    /**
     * Gets a list of URIs the AuthorizationService
     * may return the authorization code to
     * @return the redirect uris
     */
    public List<String> getRedirectUris() {
        return redirectUris;
    }

    /**
     * Sets the list of access token grant types this client
     * can use to obtain the access tokens.
     * @param allowedGrantTypes the list of grant types
     */
    public void setAllowedGrantTypes(List<String> allowedGrantTypes) {
        this.allowedGrantTypes = allowedGrantTypes;
    }

    /**
     * Gets the list of access token grant types this client
     * can use to obtain the access tokens.
     * @return the list of grant types
     */
    public List<String> getAllowedGrantTypes() {
        return allowedGrantTypes;
    }

    /**
     * Sets the {@link UserSubject} representing this Client 
     * authentication, may be setup during the registration. 
     *
     * @param subject the user subject
     */
    public void setSubject(UserSubject subject) {
        this.subject = subject;
    }

    /**
     * Gets the {@link UserSubject} representing this Client 
     * authentication
     * @return the user subject
     */
    public UserSubject getSubject() {
        return subject;
    }
    
    /**
     * Get the list of additional client properties
     * @return the list of properties
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Set the list of additional client properties
     * @param properties the properties
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * Get the list of registered scopes
     * @return scopes
     */
    public List<String> getRegisteredScopes() {
        return registeredScopes;
    }

    /**
     * Set the list of registered scopes. 
     * Registering the scopes will allow the clients not to include the scopes
     * and delegate to the runtime to enforce that the current request scopes are
     * a subset of the pre-registered scopes.
     * 
     * Client Registration service is expected to reject unknown scopes. 
     * @param registeredScopes the scopes
     */
    public void setRegisteredScopes(List<String> registeredScopes) {
        this.registeredScopes = registeredScopes;
    }

    public List<String> getRegisteredAudiences() {
        return registeredAudiences;
    }

    public void setRegisteredAudiences(List<String> registeredAudiences) {
        this.registeredAudiences = registeredAudiences;
    }
}
