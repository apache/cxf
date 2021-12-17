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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OrderColumn;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;


/**
 * Server Access Token representation
 */
@MappedSuperclass
public abstract class ServerAccessToken extends AccessToken {
    private static final long serialVersionUID = 638776204861456064L;

    private String grantType;
    private Client client;
    private List<OAuthPermission> scopes = new LinkedList<>();
    private UserSubject subject;
    private List<String> audiences = new LinkedList<>();
    private String clientCodeVerifier;
    private String nonce;
    private String responseType;
    private String grantCode;
    private Map<String, String> extraProperties = new LinkedHashMap<>();

    protected ServerAccessToken() {

    }

    protected ServerAccessToken(Client client,
                                String tokenType,
                                String tokenKey,
                                long expiresIn) {
        this(client, tokenType, tokenKey, expiresIn, OAuthUtils.getIssuedAt());
    }

    protected ServerAccessToken(Client client,
                                String tokenType,
                                String tokenKey,
                                long expiresIn,
                                long issuedAt) {
        super(tokenType, tokenKey, expiresIn, issuedAt);
        this.client = client;
    }

    protected ServerAccessToken(ServerAccessToken token, String key) {
        super(token.getTokenType(),
                key,
                token.getExpiresIn(),
                token.getIssuedAt(),
                token.getRefreshToken(),
                token.getParameters());
        this.client = token.getClient();
        this.grantType = token.getGrantType();
        this.scopes = token.getScopes();
        this.audiences = token.getAudiences();
        this.subject = token.getSubject();
        this.responseType = token.getResponseType();
        this.clientCodeVerifier = token.getClientCodeVerifier();
        this.nonce = token.getNonce();
        this.grantCode = token.getGrantCode();
    }

    protected static ServerAccessToken validateTokenType(ServerAccessToken token, String expectedType) {
        if (!token.getTokenType().equals(expectedType)) {
            throw new OAuthServiceException(OAuthConstants.SERVER_ERROR);
        }
        return token;
    }

    /**
     * Returns the Client associated with this token
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
     * Returns a list of opaque permissions/scopes
     * @return the scopes
     */
    @ManyToMany(fetch = FetchType.EAGER)
    public List<OAuthPermission> getScopes() {
        return scopes;
    }

    /**
     * Sets a list of opaque permissions/scopes
     * @param scopes the scopes
     */
    public void setScopes(List<OAuthPermission> scopes) {
        this.scopes = scopes;
    }

    /**
     * Returns a subject capturing the login name
     * the end user used to login to the resource server
     * when authorizing a given client request
     * @return UserSubject
     */
    @ManyToOne
    public UserSubject getSubject() {
        return subject;
    }

    /**
     * Sets a subject capturing the login name
     * the end user used to login to the resource server
     * when authorizing a given client request
     * @param subject
     */
    public void setSubject(UserSubject subject) {
        this.subject = subject;
    }

    /**
     * Returns the grant type which was used to obtain the access token
     * @return the grant type
     */
    public String getGrantType() {
        return grantType;
    }

    /**
     * Sets the grant type which was used to obtain the access token
     * @param grantType the grant type
     */
    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    /**
     * Get the response type
     * @return the response type, null if no redirection flow was used
     */
    public String getResponseType() {
        return responseType;
    }

    /**
     * Set the response type
     * @param responseType the response type
     */
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @OrderColumn
    public List<String> getAudiences() {
        return audiences;
    }

    public void setAudiences(List<String> audiences) {
        this.audiences = audiences;
    }

    public String getClientCodeVerifier() {
        return clientCodeVerifier;
    }

    public void setClientCodeVerifier(String clientCodeVerifier) {
        this.clientCodeVerifier = clientCodeVerifier;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "extraPropName")
    public Map<String, String> getExtraProperties() {
        return extraProperties;
    }

    public void setExtraProperties(Map<String, String> extraProperties) {
        this.extraProperties = extraProperties;
    }

    /**
     * Get the grant code
     * @return the grant code, null if no authorization code grant was used
     */
    public String getGrantCode() {
        return grantCode;
    }

    /**
     * Set the grant code which was used to request the token
     * @param grantCode the grant code
     */
    public void setGrantCode(String grantCode) {
        this.grantCode = grantCode;
    }
}
