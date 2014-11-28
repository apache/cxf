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
import org.apache.cxf.rs.security.oidc.common.UserInfo;
import org.apache.cxf.rs.security.oidc.common.UserToken;

public class UserInfoValidator extends AbstractTokenValidator {
    private boolean encryptedOnly;
    
    public UserInfo getUserInfo(WebClient profileClient, UserToken idToken) {
        return getProfile(profileClient, idToken, false);
    }
    public UserInfo getProfile(WebClient profileClient, UserToken idToken, boolean asJwt) {
        if (asJwt) {
            String jwt = profileClient.get(String.class);
            return getUserInfoFromJwt(jwt, idToken);
        } else {
            UserInfo profile = profileClient.get(UserInfo.class);
            validateUserInfo(profile, idToken);
            return profile;
        }
    }
    public UserInfo getUserInfoFromJwt(String profileJwtToken, UserToken idToken) {
        JwtToken jwt = getUserInfoJwt(profileJwtToken, idToken);
        return getUserInfoFromJwt(jwt, idToken);
    }
    public UserInfo getUserInfoFromJwt(JwtToken jwt, UserToken idToken) {
        UserInfo profile = new UserInfo(jwt.getClaims().asMap());
        validateUserInfo(profile, idToken);
        return profile;
    }
    public JwtToken getUserInfoJwt(String profileJwtToken, UserToken idToken) {
        return getJwtToken(profileJwtToken, idToken.getAudience(), (String)idToken.getProperty("kid"), encryptedOnly);
    }
    public void validateUserInfo(UserInfo profile, UserToken idToken) {
        validateJwtClaims(profile, idToken.getAudience(), false);
        // validate subject
        if (!idToken.getSubject().equals(profile.getSubject())) {
            throw new SecurityException("Invalid subject");
        }
    }
    public void setEncryptedOnly(boolean encryptedOnly) {
        this.encryptedOnly = encryptedOnly;
    }
    
}
