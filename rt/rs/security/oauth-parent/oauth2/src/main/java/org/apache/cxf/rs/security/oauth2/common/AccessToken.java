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
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

/**
 * Base Access Token representation
 */
@MappedSuperclass
public abstract class AccessToken implements Serializable {

    private static final long serialVersionUID = -5750544301887053480L;

    private String tokenKey;
    private String tokenType;
    private String refreshToken;
    private long expiresIn = -1;
    private long issuedAt = -1;
    private long notBefore = -1;
    private String issuer;
    private String encodedToken;


    private Map<String, String> parameters = new LinkedHashMap<>();

    protected AccessToken() {

    }

    protected AccessToken(String tokenType, String tokenKey) {
        this.tokenType = tokenType;
        this.tokenKey = tokenKey;
    }

    protected AccessToken(String tokenType, String tokenKey,
                          long expiresIn, long issuedAt) {
        this(tokenType, tokenKey);
        this.expiresIn = expiresIn;
        this.issuedAt = issuedAt;
    }

    protected AccessToken(String tokenType, String tokenKey,
                          long expiresIn, long issuedAt,
                          String refreshToken,
                          Map<String, String> parameters) {
        this(tokenType, tokenKey, expiresIn, issuedAt);
        this.refreshToken = refreshToken;
        this.parameters = parameters;
    }

    /**
     * Returns the token type such as bearer, mac, etc
     * @return the type
     */
    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String type) {
        this.tokenType = type;
    }

    /**
     * Returns the token key
     * @return the key
     */
    @Id
    public String getTokenKey() {
        return tokenKey;
    }

    public void setTokenKey(String key) {
        this.tokenKey = key;
    }

    /**
     * Sets the refresh token key the client can use to obtain a new
     * access token
     * @param refreshToken the refresh token
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * Gets the refresh token key the client can use to obtain a new
     * access token
     * @return the refresh token
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Gets token parameters
     * @return
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "propName")
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * The token lifetime
     * @return the lifetime, -1 means no 'expires_in' parameter was returned
     */
    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public long getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(long issuedAt) {
        this.issuedAt = issuedAt;
    }

    /**
     * Sets additional token parameters
     * @param parameters the token parameters
     */
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    @Transient
    public String getEncodedToken() {
        return encodedToken;
    }

    public void setEncodedToken(String encodedToken) {
        this.encodedToken = encodedToken;
    }

    /**
     * @return the Not Before" timestamp, -1 means no 'nbf' parameter was returned
     */
    public long getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(long notBefore) {
        this.notBefore = notBefore;
    }
}
