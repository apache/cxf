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
package org.apache.cxf.rs.security.oauth2.tokens.bearer;

import jakarta.persistence.Entity;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

/**
 * Simple Bearer Access Token implementations
 */
@Entity
public class BearerAccessToken extends ServerAccessToken {
    private static final long serialVersionUID = -3614732043728799245L;

    public BearerAccessToken(Client client,
                             long lifetime) {
        super(client,
              OAuthConstants.BEARER_TOKEN_TYPE,
              OAuthUtils.generateRandomTokenKey(),
              lifetime,
              OAuthUtils.getIssuedAt());
    }
    public BearerAccessToken(Client client,
                             String tokenKey,
                             long lifetime,
                             long issuedAt) {
        super(client, OAuthConstants.BEARER_TOKEN_TYPE, tokenKey, lifetime, issuedAt);
    }
    public BearerAccessToken(ServerAccessToken token) {
        this(token, OAuthUtils.generateRandomTokenKey());
    }
    public BearerAccessToken(ServerAccessToken token, String newKey) {
        super(validateTokenType(token, OAuthConstants.BEARER_TOKEN_TYPE), newKey);
    }
    public BearerAccessToken() {

    }
}
