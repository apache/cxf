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
package org.apache.cxf.rs.security.oauth2.tokens.refresh;

import java.util.LinkedList;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OrderColumn;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

/**
 * Simple Refresh Token implementation
 */
@Entity
public class RefreshToken extends ServerAccessToken {

    private static final long serialVersionUID = 2837120382251693874L;
    private List<String> accessTokens = new LinkedList<>();

    public RefreshToken(Client client,
                        long lifetime) {
        super(client,
                OAuthConstants.REFRESH_TOKEN_TYPE,
                OAuthUtils.generateRandomTokenKey(),
                lifetime,
                OAuthUtils.getIssuedAt());
    }

    public RefreshToken(Client client,
                        String tokenKey,
                        long lifetime,
                        long issuedAt) {
        super(client,
                OAuthConstants.REFRESH_TOKEN_TYPE,
                tokenKey,
                lifetime,
                issuedAt);
    }

    public RefreshToken(ServerAccessToken token,
                        String key,
                        List<String> accessTokens) {
        super(validateTokenType(token, OAuthConstants.REFRESH_TOKEN_TYPE), key);
        this.accessTokens = accessTokens;
    }

    public RefreshToken() {

    }

    @ElementCollection(fetch = FetchType.EAGER)
    @OrderColumn
    public List<String> getAccessTokens() {
        return accessTokens;
    }

    public void setAccessTokens(List<String> accessTokens) {
        this.accessTokens = accessTokens;
    }

    public void addAccessToken(String token) {
        getAccessTokens().add(token);
    }

    public boolean removeAccessToken(String token) {
        return getAccessTokens().remove(token);
    }
}
