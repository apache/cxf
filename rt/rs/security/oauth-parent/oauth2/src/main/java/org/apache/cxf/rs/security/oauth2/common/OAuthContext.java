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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Captures the information about the current client request
 * which custom filters may use to further protect the endpoints
 */
public class OAuthContext {

    private UserSubject resourceOwnerSubject;
    private UserSubject clientSubject;
    private List<OAuthPermission> tokenPermissions;
    private String tokenGrantType;
    private String clientId;
    private boolean isClientConfidential;
    private String tokenKey;
    private String tokenAudience;
    private String tokenIssuer;
    private String[] tokenRequestParts;
    private Map<String, String> tokenExtraProperties = new LinkedHashMap<>();

    public OAuthContext(UserSubject resourceOwnerSubject,
                        UserSubject clientSubject,
                        List<OAuthPermission> perms,
                        String tokenGrantType) {
        this.resourceOwnerSubject = resourceOwnerSubject;
        this.clientSubject = clientSubject;
        this.tokenPermissions = perms;
        this.tokenGrantType = tokenGrantType;
    }

    /**
     * Gets the {@link UserSubject} representing the resource owner
     * @return the subject
     */
    public UserSubject getSubject() {
        return resourceOwnerSubject;
    }

    /**
     * Gets the {@link UserSubject} representing the client
     * @return the subject
     */
    public UserSubject getClientSubject() {
        return clientSubject;
    }

    /**
     * Gets the list of the permissions assigned to the current access token
     * @return the permissions
     */
    public List<OAuthPermission> getPermissions() {
        return Collections.unmodifiableList(tokenPermissions);
    }

    /**
     * Returns the grant type which was used to obtain the access token
     * the client is using now during the current request
     * @return the grant type
     */
    public String getTokenGrantType() {
        return tokenGrantType;
    }

    /**
      * Returns the client which obtained the access token
      * @return the client id
    */
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets the client which obtained the access token
     * @param clientId
    */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Returns the access token the client is using now during the current request
     * @return the token
     */
    public String getTokenKey() {
        return tokenKey;
    }

    /**
     * Sets the access token the client is using now during the current request
     * @param tokenKey
     */
    public void setTokenKey(String tokenKey) {
        this.tokenKey = tokenKey;
    }

    public String getTokenAudience() {
        return tokenAudience;
    }

    public void setTokenAudience(String audience) {
        this.tokenAudience = audience;
    }

    public String[] getTokenRequestParts() {
        return tokenRequestParts;
    }

    public void setTokenRequestParts(String[] tokenRequestParts) {
        this.tokenRequestParts = tokenRequestParts;
    }
    public boolean isClientConfidential() {
        return isClientConfidential;
    }
    public void setClientConfidential(boolean isConfidential) {
        this.isClientConfidential = isConfidential;
    }

    public String getTokenIssuer() {
        return tokenIssuer;
    }

    public void setTokenIssuer(String tokenIssuer) {
        this.tokenIssuer = tokenIssuer;
    }

    public Map<String, String> getTokenExtraProperties() {
        return tokenExtraProperties;
    }

    public void setTokenExtraProperties(Map<String, String> tokenExtraProperties) {
        this.tokenExtraProperties = tokenExtraProperties;
    }
}
