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
import java.util.Map;

/**
 * Base Access Token representation
 */
public abstract class AccessToken {

    private String tokenKey;
    private String tokenType;
    private String refreshToken;
    private long expiresIn = -1;
    private long issuedAt = -1;
    
    
    private Map<String, String> parameters = new LinkedHashMap<String, String>();
    
    protected AccessToken(String tokenType, String tokenKey) {
        this.tokenType = tokenType;
        this.tokenKey = tokenKey;
    }
    
    protected AccessToken(String tokenType, String tokenKey,
                          long expiresIn, long issuedAt) {
        this.tokenType = tokenType;
        this.tokenKey = tokenKey;
        this.expiresIn = expiresIn;
        this.issuedAt = issuedAt;
    }

    /**
     * Returns the token type such as bearer, mac, etc
     * @return the type
     */
    public String getTokenType() {
        return tokenType;
    }
    
    /**
     * Returns the token key
     * @return the key
     */
    public String getTokenKey() {
        return tokenKey;
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

    // Can be set at the server or at the moment 
    // the token is deserialized on the client
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
}
