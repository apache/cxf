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
    private String clientSecret;
    private String clientIpAddress;
    
    private String applicationName;
    private String applicationDescription;
    private String applicationWebUri;
    private String applicationLogoUri;
    private List<String> applicationCertificates = new LinkedList<String>();
    private List<String> redirectUris = new LinkedList<String>();
    
    private boolean isConfidential;
    private List<String> allowedGrantTypes = new LinkedList<String>();
    private List<String> registeredScopes = new LinkedList<String>();
    private List<String> registeredAudiences = new LinkedList<String>();
    
    private Map<String, String> properties = new HashMap<String, String>();
    private UserSubject subject;
    private UserSubject resourceOwnerSubject;
        
    public Client() {
        
    }
    
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
     * Get the client registration id
     * @return the consumer key
     */
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String id) {
        clientId = id;
    }
    
    /**
     * Get the client secret
     * @return the consumer key
     */
    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String id) {
        clientSecret = id;
    }
        
    /**
     * Get the name of the third-party application
     * this client represents
     * @return the application name
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Set the name of the third-party application
     * this client represents
     * @param applicationName the name
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * Get the public URI of the third-party application.
     * @return the application URI
     */
    public String getApplicationWebUri() {
        return applicationWebUri;
    }

    /**
     * Set the public URI of the third-party application.
     * @param applicationWebUri the application URI
     */
    public void setApplicationWebUri(String applicationWebUri) {
        this.applicationWebUri = applicationWebUri;
    }

    /**
     * Set the description of the third-party application.
     * @param applicationDescription the description
     */
    public void setApplicationDescription(String applicationDescription) {
        this.applicationDescription = applicationDescription;
    }

    /**
     * Get the description of the third-party application.
     * @return the application description
     */
    public String getApplicationDescription() {
        return applicationDescription;
    }
    
    /**
     * Set the URI pointing to a logo image of the client application
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
     * Set the confidentiality status of this client application.
     * This can be used to restrict which OAuth2 flows this client
     * can participate in.
     * 
     * @param isConf true if the client is confidential
     */
    public void setConfidential(boolean isConf) {
        this.isConfidential = isConf;
    }

    /**
     * Get the confidentiality status of this client application.
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
     * Get a list of URIs the AuthorizationService
     * may return the authorization code to
     * @return the redirect uris
     */
    public List<String> getRedirectUris() {
        return redirectUris;
    }

    /**
     * Set the list of access token grant types this client
     * can use to obtain the access tokens.
     * @param allowedGrantTypes the list of grant types
     */
    public void setAllowedGrantTypes(List<String> allowedGrantTypes) {
        this.allowedGrantTypes = allowedGrantTypes;
    }

    /**
     * Get the list of access token grant types this client
     * can use to obtain the access tokens.
     * @return the list of grant types
     */
    public List<String> getAllowedGrantTypes() {
        return allowedGrantTypes;
    }

    /**
     * Set the {@link UserSubject} representing this Client 
     * authentication. This property may be set during the registration
     * in cases where a 3rd party client needs to authenticate first before
     * registering as OAuth2 client. This property may also wrap a clientId
     * in cases where a client credentials flow is used   
     *
     * @param subject the user subject
     */
    public void setSubject(UserSubject subject) {
        this.subject = subject;
    }

    /**
     * Get the {@link UserSubject} representing this Client 
     * authentication
     * @return the user subject
     */
    public UserSubject getSubject() {
        return subject;
    }

    /**
     * Set the {@link UserSubject} representing the resource owner 
     * who has registered this client. This property may be set in cases where
     * each account (resource) owner registers account specific Clients
     *
     * @param subject the resource owner user subject
     */

    public void setResourceOwnerSubject(UserSubject resourceOwnerSubject) {
        this.resourceOwnerSubject = resourceOwnerSubject;
    }


    /**
     * Get the {@link UserSubject} representing the resource owner 
     * who has registered this client
     * @return the resource owner user subject
     */
    public UserSubject getResourceOwnerSubject() {
        return resourceOwnerSubject;
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

    /**
     * Set the list of registered audiences
     * @param registeredAudiences audiences
     */
    public void setRegisteredAudiences(List<String> registeredAudiences) {
        this.registeredAudiences = registeredAudiences;
    }

    public List<String> getApplicationCertificates() {
        return applicationCertificates;
    }

    /*
     * Set the Base64 encoded Application Public X509 Certificate
     * It can be used in combination with the clientSecret property to support 
     * Basic or other password-aware authentication on top of 2-way TLS.
     */
    public void setApplicationCertificates(List<String> applicationCertificates) {
        this.applicationCertificates = applicationCertificates;
    }

    public String getClientIpAddress() {
        return clientIpAddress;
    }

    public void setClientIpAddress(String clientIpAddress) {
        this.clientIpAddress = clientIpAddress;
    }
}
