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
package org.apache.cxf.rs.security.oauth2.grants.refresh;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenGrant;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

public class RefreshTokenGrant implements AccessTokenGrant {
    private static final long serialVersionUID = -4855594852737940210L;
    private String refreshToken;
    private String scope;

    public RefreshTokenGrant(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public RefreshTokenGrant(String refreshToken, String scope) {
        this.refreshToken = refreshToken;
        this.scope = scope;
    }

    public String getType() {
        return OAuthConstants.REFRESH_TOKEN_GRANT;
    }

    public MultivaluedMap<String, String> toMap() {
        MultivaluedMap<String, String> map = new MetadataMap<>();
        map.putSingle(OAuthConstants.GRANT_TYPE, OAuthConstants.REFRESH_TOKEN_GRANT);
        map.putSingle(OAuthConstants.REFRESH_TOKEN, refreshToken);
        if (scope != null) {
            map.putSingle(OAuthConstants.SCOPE, scope);
        }
        return map;
    }

}
