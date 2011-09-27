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

import java.util.List;


public abstract class Token {

    protected String tokenString;
    protected String tokenSecret;
    protected long issuedAt = -1;
    protected long lifetime = -1;
    protected Client client;
    protected List<String> scopes;
    protected List<String> uris;
    private List<String> httpVerbs;
    
    protected Token(Client client, String tokenString,
                    String tokenSecret, long lifetime) {
        this.client = client;
        this.tokenString = tokenString;
        this.tokenSecret = tokenSecret;
        initTokenLifeTime(lifetime);
    }

    protected Token(Client client, String tokenString,
                    String tokenSecret) {
        this(client, tokenString, tokenSecret, -1);
    }

    private void initTokenLifeTime(Long lifetm) {
        this.lifetime = lifetm;
        issuedAt = System.currentTimeMillis() / 1000;
    }

    public Client getClient() {
        return client;
    }

    public String getTokenString() {
        return tokenString;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }

    public long getIssuedAt() {
        return issuedAt;
    }

    public long getLifetime() {
        return lifetime;
    }

    public List<String> getScopes() {
        return scopes == null || scopes.isEmpty() ? client.getScopes() : scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }
       

    public List<String> getUris() {
        return uris == null || uris.isEmpty() ? client.getUris() : uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }

    public void setHttpVerbs(List<String> httpVerbs) {
        this.httpVerbs = httpVerbs;
    }

    public List<String> getHttpVerbs() {
        return httpVerbs;
    }
    
    
}
