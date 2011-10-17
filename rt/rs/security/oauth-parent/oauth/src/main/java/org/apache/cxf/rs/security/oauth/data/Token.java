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
package org.apache.cxf.rs.security.oauth.data;

import java.util.Collections;
import java.util.List;

/**
 * Base Token representation
 */
public abstract class Token {

    protected String tokenString;
    protected String tokenSecret;
    protected long issuedAt = -1;
    protected long lifetime = -1;
    protected Client client;
    protected List<String> scopes = Collections.emptyList();
    protected List<String> uris = Collections.emptyList();
    
    protected Token(Client client, String tokenKey,
                    String tokenSecret, long lifetime) {
        this.client = client;
        this.tokenString = tokenKey;
        this.tokenSecret = tokenSecret;
        initTokenLifeTime(lifetime);
    }

    protected Token(Client client, String tokenKey,
                    String tokenSecret) {
        this(client, tokenKey, tokenSecret, -1);
    }

    private void initTokenLifeTime(Long lifetm) {
        this.lifetime = lifetm;
        issuedAt = System.currentTimeMillis() / 1000;
    }

    /**
     * Returns the Client associated with this token
     * @return the client
     */
    public Client getClient() {
        return client;
    }

    /**
     * Returns the token key
     * @return the key
     */
    public String getTokenKey() {
        return tokenString;
    }

    /**
     * Returns the token secret
     * @return the secret
     */
    public String getTokenSecret() {
        return tokenSecret;
    }

    /**
     * Returns the time (in seconds) when this token was issued at
     * @return the seconds
     */
    public long getIssuedAt() {
        return issuedAt;
    }

    /**
     * Returns the number of seconds this token can be valid after it was issued
     * @return the seconds
     */
    public long getLifetime() {
        return lifetime;
    }

    /**
     * Returns a list of opaque permissions/scopes
     * @return the scopes
     */
    public List<String> getScopes() {
        return scopes;
    }

    /**
     * Sets a list of opaque permissions/scopes
     * @param scopes the scopes
     */
    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }
    
    /**
     * Returns a list of relative URIs the consumer wishes to access
     * @return the uris
     */
    public List<String> getUris() {
        return uris;
    }

    /**
     * Sets a list of relative URIs the consumer wishes to access
     * @param uris the uris
     */
    public void setUris(List<String> uris) {
        this.uris = uris;
    }
    
}
