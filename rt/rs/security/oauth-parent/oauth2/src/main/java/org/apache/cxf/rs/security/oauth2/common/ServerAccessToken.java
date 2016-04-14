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

import javax.persistence.ElementCollection;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;

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
    private List<OAuthPermission> scopes = new LinkedList<OAuthPermission>();
    private UserSubject subject;
    private List<String> audiences = new LinkedList<String>();
    private String clientCodeVerifier;
    private String nonce;
    private String responseType;
    private String grantCode;
    private Map<String, String> extraProperties = new LinkedHashMap<String, String>();
    
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

    /**
     * Returns the Client associated with this token
     * @return the client
     */
    @OneToOne
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
    @ManyToMany
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
     * Sets a subject capturing the login name 
     * the end user used to login to the resource server
     * when authorizing a given client request
     * @param subject
     */
    public void setSubject(UserSubject subject) {
        this.subject = subject;
    }

    /**
     * Returns a subject capturing the login name 
     * the end user used to login to the resource server
     * when authorizing a given client request
     * @return UserSubject
     */
    @OneToOne
    public UserSubject getSubject() {
        return subject;
    }

    /**
     * Sets the grant type which was used to obtain the access token
     * @param grantType the grant type
     */
    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    /**
     * Returns the grant type which was used to obtain the access token
     * @return the grant type
     */
    public String getGrantType() {
        return grantType;
    }
    
    /**
     * Set the response type
     * @param responseType the response type
     */
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    /**
     * Get the response type
     * @return the response type, null if no redirection flow was used
     */
    public String getResponseType() {
        return responseType;
    }
    
    @ElementCollection
    public List<String> getAudiences() {
        return audiences;
    }

    public void setAudiences(List<String> audiences) {
        this.audiences = audiences;
    }
    
    protected static ServerAccessToken validateTokenType(ServerAccessToken token, String expectedType) {
        if (!token.getTokenType().equals(expectedType)) {
            throw new OAuthServiceException(OAuthConstants.SERVER_ERROR);
        }
        return token;
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

    @ElementCollection
    @MapKeyColumn(name = "extraPropName")
    public Map<String, String> getExtraProperties() {
        return extraProperties;
    }

    public void setExtraProperties(Map<String, String> extraProperties) {
        this.extraProperties = extraProperties;
    }
    /**
     * Set the grant code which was used to request the token
     * @param grantCode the grant code
     */
    public void setGrantCode(String grantCode) {
        this.grantCode = grantCode;
    }

    /**
     * Get the grant code
     * @return the grant code, null if no authorization code grant was used
     */
    public String getGrantCode() {
        return grantCode;
    }
}
