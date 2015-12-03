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

import javax.ws.rs.core.Form;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.client.OAuthClientUtils;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.common.UserInfo;

public class UserInfoClient extends AbstractTokenValidator {
    private boolean sendTokenAsFormParameter;
    private WebClient profileClient;
    private boolean getUserInfoFromJwt;
    public UserInfo getUserInfo(ClientAccessToken at, IdToken idToken, OAuthClientUtils.Consumer client) {
        if (!sendTokenAsFormParameter) {
            OAuthClientUtils.setAuthorizationHeader(profileClient, at);
            if (getUserInfoFromJwt) {
                String jwt = profileClient.get(String.class);
                return getUserInfoFromJwt(jwt, idToken, client);
            } else {
                UserInfo profile = profileClient.get(UserInfo.class);
                validateUserInfo(profile, idToken, client);
                return profile;
            }
        } else {
            Form form = new Form().param("access_token", at.getTokenKey());
            if (getUserInfoFromJwt) {
                String jwt = profileClient.form(form).readEntity(String.class);
                return getUserInfoFromJwt(jwt, idToken, client);
            } else {
                UserInfo profile = profileClient.form(form).readEntity(UserInfo.class);
                validateUserInfo(profile, idToken, client);
                return profile;
            }
        }
    }
    public UserInfo getUserInfoFromJwt(String profileJwtToken, 
                                       IdToken idToken,
                                       OAuthClientUtils.Consumer client) {
        JwtToken jwt = getUserInfoJwt(profileJwtToken, client);
        return getUserInfoFromJwt(jwt, idToken, client);
    }
    public UserInfo getUserInfoFromJwt(JwtToken jwt, IdToken idToken, OAuthClientUtils.Consumer client) {
        UserInfo profile = new UserInfo(jwt.getClaims().asMap());
        validateUserInfo(profile, idToken, client);
        return profile;
    }
    public JwtToken getUserInfoJwt(String profileJwtToken, OAuthClientUtils.Consumer client) {
        return getJwtToken(profileJwtToken);
    }
    public void validateUserInfo(UserInfo profile, IdToken idToken, OAuthClientUtils.Consumer client) {
        validateJwtClaims(profile, client.getKey(), false);
        // validate subject
        if (!idToken.getSubject().equals(profile.getSubject())) {
            throw new SecurityException("Invalid subject");
        }
    }
    public void setUserInfoServiceClient(WebClient client) {
        this.profileClient = client;
    }
    public void setSendTokenAsFormParameter(boolean sendTokenAsFormParameter) {
        this.sendTokenAsFormParameter = sendTokenAsFormParameter;
    }
    public void setGetUserInfoFromJwt(boolean getUserInfoFromJwt) {
        this.getUserInfoFromJwt = getUserInfoFromJwt;
    }
    
}
