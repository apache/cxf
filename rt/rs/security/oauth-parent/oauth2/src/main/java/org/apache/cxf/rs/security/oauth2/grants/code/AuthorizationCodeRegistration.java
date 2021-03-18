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
package org.apache.cxf.rs.security.oauth2.grants.code;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;

/**
 * Captures the information associated with the code grant registration request.
 * @see ServerAuthorizationCodeGrant
 */
public class AuthorizationCodeRegistration {
    private Client client;
    private List<String> requestedScope = Collections.emptyList();
    private List<String> approvedScope = Collections.emptyList();
    private String redirectUri;
    private UserSubject subject;
    private String audience;
    private String nonce;
    private String responseType;
    private String clientCodeChallenge;
    private String clientCodeChallengeMethod;
    private boolean preauthorizedTokenAvailable;
    private Map<String, String> extraProperties = new LinkedHashMap<>();
    /**
     * Sets the {@link Client} reference
     * @param client the client
     */
    public void setClient(Client client) {
        this.client = client;
    }
    /**
     * Gets {@link Client} reference
     * @return the client
     */
    public Client getClient() {
        return client;
    }
    /**
     * Sets the redirect URI
     * @param redirectUri the redirect URI
     */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
    /**
     * Gets the redirect URI
     * @return the redirect URI
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * Sets the scopes request by the client
     * @param requestedScope the requested scopes
     */
    public void setRequestedScope(List<String> requestedScope) {
        this.requestedScope = requestedScope;
    }

    /**
     * Gets the scopes request by the client
     * @return the requested scopes
     */
    public List<String> getRequestedScope() {
        return requestedScope;
    }

    /**
     * Sets the scopes explicitly approved by the end user.
     * If this list is empty then the end user had no way to down-scope.
     * @param approvedScope the approved scopes
     */
    public void setApprovedScope(List<String> approvedScope) {
        this.approvedScope = approvedScope;
    }

    /**
     * Gets the scopes explicitly approved by the end user
     * @return the approved scopes
     */
    public List<String> getApprovedScope() {
        return approvedScope;
    }

    /**
     * Sets the user subject representing the end user
     * @param subject the subject
     */
    public void setSubject(UserSubject subject) {
        this.subject = subject;
    }

    /**
     * Gets the user subject representing the end user
     * @return the subject
     */
    public UserSubject getSubject() {
        return subject;
    }
    public String getAudience() {
        return audience;
    }
    public void setAudience(String audience) {
        this.audience = audience;
    }
    public String getClientCodeChallenge() {
        return clientCodeChallenge;
    }
    public void setClientCodeChallenge(String clientCodeChallenge) {
        this.clientCodeChallenge = clientCodeChallenge;
    }
    public String getNonce() {
        return nonce;
    }
    public void setNonce(String nonce) {
        this.nonce = nonce;
    }
    public boolean isPreauthorizedTokenAvailable() {
        return preauthorizedTokenAvailable;
    }
    public void setPreauthorizedTokenAvailable(boolean preauthorizedTokenAvailable) {
        this.preauthorizedTokenAvailable = preauthorizedTokenAvailable;
    }
    public Map<String, String> getExtraProperties() {
        return extraProperties;
    }
    public void setExtraProperties(Map<String, String> extraProperties) {
        this.extraProperties = extraProperties;
    }
    public String getResponseType() {
        return responseType;
    }
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getClientCodeChallengeMethod() {
        return clientCodeChallengeMethod;
    }

    public void setClientCodeChallengeMethod(String clientCodeChallengeMethod) {
        this.clientCodeChallengeMethod = clientCodeChallengeMethod;
    }
}
