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
package org.apache.cxf.rs.security.oidc.rp;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.client.OAuthClientUtils;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.common.UserInfo;

public class UserInfoClient extends IdTokenReader {
    private boolean encryptedOnly;
    private WebClient profileClient;
    public UserInfo getUserInfo(ClientAccessToken at, IdToken idToken) {
        return getUserInfo(at, idToken, false);
    }
    public UserInfo getUserInfo(ClientAccessToken at, IdToken idToken, boolean asJwt) {
        OAuthClientUtils.setAuthorizationHeader(profileClient, at);
        if (asJwt) {
            String jwt = profileClient.get(String.class);
            return getUserInfoFromJwt(jwt, idToken);
        } else {
            UserInfo profile = profileClient.get(UserInfo.class);
            validateUserInfo(profile, idToken);
            return profile;
        }
    }
    public UserInfo getUserInfoFromJwt(String profileJwtToken, IdToken idToken) {
        JwtToken jwt = getUserInfoJwt(profileJwtToken);
        return getUserInfoFromJwt(jwt, idToken);
    }
    public UserInfo getUserInfoFromJwt(JwtToken jwt, IdToken idToken) {
        UserInfo profile = new UserInfo(jwt.getClaims().asMap());
        validateUserInfo(profile, idToken);
        return profile;
    }
    public JwtToken getUserInfoJwt(String profileJwtToken) {
        return getJwtToken(profileJwtToken, encryptedOnly);
    }
    public void validateUserInfo(UserInfo profile, IdToken idToken) {
        validateJwtClaims(profile, idToken.getAudience(), false);
        // validate subject
        if (!idToken.getSubject().equals(profile.getSubject())) {
            throw new SecurityException("Invalid subject");
        }
    }
    public void setEncryptedOnly(boolean encryptedOnly) {
        this.encryptedOnly = encryptedOnly;
    }
    public void setUserInfoServiceClient(WebClient client) {
        this.profileClient = client;
    }
    
}
