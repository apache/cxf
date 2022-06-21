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
package org.apache.cxf.rs.security.oidc.idp;

import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthContext;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServerJoseJwtProducer;
import org.apache.cxf.rs.security.oauth2.utils.OAuthContextUtils;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.common.UserInfo;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;

@Path("/userinfo")
public class UserInfoService extends OAuthServerJoseJwtProducer {
    private UserInfoProvider userInfoProvider;
    private OAuthDataProvider oauthDataProvider;
    private List<String> additionalClaims = Collections.emptyList();
    private boolean convertClearUserInfoToString;
    @Context
    private MessageContext mc;
    @GET
    @Produces({"application/json", "application/jwt" })
    public Response getUserInfo() {
        OAuthContext oauth = OAuthContextUtils.getContext(mc);

        // Check the access token has the "openid" scope
        if (!oauth.getPermissions().stream()
            .map(OAuthPermission::getPermission)
            .anyMatch(OidcUtils.OPENID_SCOPE::equals)) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        UserInfo userInfo = null;
        if (userInfoProvider != null) {
            userInfo = userInfoProvider.getUserInfo(oauth.getClientId(), oauth.getSubject(),
                OAuthUtils.convertPermissionsToScopeList(oauth.getPermissions()));
        } else if (oauth.getSubject() instanceof OidcUserSubject) {
            OidcUserSubject oidcUserSubject = (OidcUserSubject)oauth.getSubject();
            userInfo = oidcUserSubject.getUserInfo();
            if (userInfo == null) {
                userInfo = createFromIdToken(oidcUserSubject.getIdToken());
            }
        }
        if (userInfo == null) {
            // Consider customizing the error code in case of UserInfo being not available
            return Response.serverError().build();
        }

        final Object responseEntity;
        // UserInfo may be returned in a clear form as JSON
        if (super.isJwsRequired() || super.isJweRequired()) {
            Client client = null;
            if (oauthDataProvider != null) {
                client = oauthDataProvider.getClient(oauth.getClientId());
            }
            responseEntity = super.processJwt(new JwtToken(userInfo), client);
        } else {
            responseEntity = convertUserInfoToResponseEntity(userInfo);
        }
        return Response.ok(responseEntity).build();

    }

    protected Object convertUserInfoToResponseEntity(UserInfo userInfo) {
        // By default a JAX-RS MessageBodyWriter is expected to serialize UserInfo.
        return convertClearUserInfoToString ? JwtUtils.claimsToJson(userInfo) : userInfo;
    }

    protected UserInfo createFromIdToken(IdToken idToken) {
        UserInfo userInfo = new UserInfo();
        userInfo.setSubject(idToken.getSubject());

        if (super.isJwsRequired()) {
            userInfo.setIssuer(idToken.getIssuer());
            userInfo.setAudience(idToken.getAudience());
        }
        if (idToken.getPreferredUserName() != null) {
            userInfo.setPreferredUserName(idToken.getPreferredUserName());
        }
        if (idToken.getName() != null) {
            userInfo.setName(idToken.getName());
        }
        if (idToken.getGivenName() != null) {
            userInfo.setGivenName(idToken.getGivenName());
        }
        if (idToken.getFamilyName() != null) {
            userInfo.setFamilyName(idToken.getFamilyName());
        }
        if (idToken.getEmail() != null) {
            userInfo.setEmail(idToken.getEmail());
        }
        if (idToken.getNickName() != null) {
            userInfo.setNickName(idToken.getNickName());
        }

        if (additionalClaims != null && !additionalClaims.isEmpty()) {
            for (String additionalClaim : additionalClaims) {
                if (idToken.containsProperty(additionalClaim)) {
                    userInfo.setClaim(additionalClaim, idToken.getClaim(additionalClaim));
                }
            }
        }

        //etc
        return userInfo;
    }

    public void setUserInfoProvider(UserInfoProvider userInfoProvider) {
        this.userInfoProvider = userInfoProvider;
    }

    public void setOauthDataProvider(OAuthDataProvider oauthDataProvider) {
        this.oauthDataProvider = oauthDataProvider;
    }

    /**
     * Set additional claims to return (if they exist in the IdToken).
     */
    public void setAdditionalClaims(List<String> additionalClaims) {
        this.additionalClaims = additionalClaims;
    }

    public void setConvertClearUserInfoToString(boolean convertClearUserInfoToString) {
        this.convertClearUserInfoToString = convertClearUserInfoToString;
    }
}
