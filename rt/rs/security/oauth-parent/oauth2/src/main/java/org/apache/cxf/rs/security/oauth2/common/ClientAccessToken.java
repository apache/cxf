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

import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;



/**
 * Represents the extended client view of {@link AccessToken}.
 * It may contain the actual scope value assigned to the access token,
 * the refresh token key, and other properties such as when this token
 * will expire, etc.
 */
public class ClientAccessToken extends AccessToken {

    private static final long serialVersionUID = 831870452726298523L;
    private String scope;

    public ClientAccessToken() {

    }

    public ClientAccessToken(String tokenType, String tokenKey) {
        super(tokenType, tokenKey);
    }

    /**
     * Sets the actual scope assigned to the access token.
     * For example, it can be down-scoped in which case the client
     * may need to adjust the way it works with the end user.
     * @param approvedScope the actual scope
     */
    public void setApprovedScope(String approvedScope) {
        this.scope = approvedScope;
    }

    /**
     * Gets the actual scope assigned to the access token.
     * @return the scope
     */
    public String getApprovedScope() {
        return scope;
    }

    @Override
    public String toString() {
        if (OAuthConstants.BEARER_AUTHORIZATION_SCHEME.equalsIgnoreCase(super.getTokenType())) {
            return OAuthConstants.BEARER_AUTHORIZATION_SCHEME + " " + super.getTokenKey();
        }
        return super.toString();
    }
}
