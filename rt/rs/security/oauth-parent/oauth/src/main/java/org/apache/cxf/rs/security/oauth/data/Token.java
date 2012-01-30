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

    private String tokenString;
    private String tokenSecret;
    private long issuedAt = -1;
    private long lifetime = -1;
    private Client client;
    private List<OAuthPermission> scopes = Collections.emptyList();
    private UserSubject subject;
    private boolean preAuthorized;
    
    protected Token(Client client, String tokenKey,
                    String tokenSecret, long lifetime, long issuedAt) {
        this.client = client;
        this.tokenString = tokenKey;
        this.tokenSecret = tokenSecret;
        this.lifetime = lifetime;
        this.issuedAt = issuedAt;
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
    public UserSubject getSubject() {
        return subject;
    }

    public void setPreAuthorized(boolean preAuthorized) {
        this.preAuthorized = preAuthorized;
    }

    public boolean isPreAuthorized() {
        return preAuthorized;
    }

}
