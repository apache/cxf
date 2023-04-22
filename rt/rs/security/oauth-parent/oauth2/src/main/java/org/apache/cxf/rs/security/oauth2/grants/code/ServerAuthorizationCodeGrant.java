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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderColumn;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;


/**
 * The Authorization Code Grant representation visible to the server
 */
@Entity
public class ServerAuthorizationCodeGrant extends AuthorizationCodeGrant {
    private static final long serialVersionUID = -5004608901535459036L;

    private long issuedAt;
    private long expiresIn;
    private Client client;
    private List<String> approvedScopes = new LinkedList<>();
    private List<String> requestedScopes = new LinkedList<>();
    private UserSubject subject;
    private String audience;
    private String responseType;
    private String clientCodeChallenge;
    private String clientCodeChallengeMethod;
    private String nonce;
    private boolean preauthorizedTokenAvailable;
    private Map<String, String> extraProperties = new LinkedHashMap<>();

    public ServerAuthorizationCodeGrant() {

    }

    public ServerAuthorizationCodeGrant(Client client,
                                        long lifetime) {
        this(client, OAuthUtils.generateRandomTokenKey(), lifetime,
                OAuthUtils.getIssuedAt());
    }

    public ServerAuthorizationCodeGrant(Client client,
                                        String code,
                                        long expiresIn,
                                        long issuedAt) {
        super(code);
        this.client = client;
        this.expiresIn = expiresIn;
        this.issuedAt = issuedAt;
    }

    /**
     * Returns the time (in seconds) this grant was issued at
     * @return the seconds
     */
    public long getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(long issuedAt) {
        this.issuedAt = issuedAt;
    }

    /**
     * Returns the number of seconds this grant can be valid after it was issued
     * @return the seconds this grant will be valid for
     */
    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * Returns the reference to {@link Client}
     * @return the client
     */
    @ManyToOne
    public Client getClient() {
        return client;
    }

    public void setClient(Client c) {
        this.client = c;
    }

    /**
     * Gets the scopes explicitly approved by the end user
     * @return the approved scopes
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @OrderColumn
    public List<String> getApprovedScopes() {
        return approvedScopes;
    }

    /**
     * Sets the scopes explicitly approved by the end user.
     * If this list is empty then the end user had no way to down-scope.
     * @param scopes the approved scopes
     */

    public void setApprovedScopes(List<String> scopes) {
        this.approvedScopes = scopes;
    }

    /**
     * Gets the user subject representing the end user
     * @return the subject
     */
    @ManyToOne
    public UserSubject getSubject() {
        return subject;
    }

    /**
     * Sets the user subject representing the end user
     * @param subject the subject
     */
    public void setSubject(UserSubject subject) {
        this.subject = subject;
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

    @ElementCollection(fetch = FetchType.EAGER)
    @OrderColumn
    public List<String> getRequestedScopes() {
        return requestedScopes;
    }

    public void setRequestedScopes(List<String> requestedScopes) {
        this.requestedScopes = requestedScopes;
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

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "extraPropName")
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
