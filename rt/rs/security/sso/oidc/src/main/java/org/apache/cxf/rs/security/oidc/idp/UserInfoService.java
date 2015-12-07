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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthContext;
import org.apache.cxf.rs.security.oauth2.provider.AbstractOAuthServerJoseJwtProducer;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.utils.OAuthContextUtils;
import org.apache.cxf.rs.security.oidc.common.UserInfo;

@Path("/userinfo")
public class UserInfoService extends AbstractOAuthServerJoseJwtProducer {
    private UserInfoProvider userInfoProvider;
    private OAuthDataProvider oauthDataProvider;
    
    @Context
    private MessageContext mc;
    @GET
    @Produces({"application/json", "application/jwt" })
    public Response getUserInfo() {
        OAuthContext oauth = OAuthContextUtils.getContext(mc);
        UserInfo userInfo = 
            userInfoProvider.getUserInfo(oauth.getClientId(), oauth.getSubject(), oauth.getPermissions());
        Object responseEntity = userInfo;
        if (super.isJwsRequired() || super.isJweRequired()) {
            responseEntity = super.processJwt(new JwtToken(userInfo),
                                              oauthDataProvider.getClient(oauth.getClientId()));
        }
        return Response.ok(responseEntity).build();
        
    }
    
    public void setUserInfoProvider(UserInfoProvider userInfoProvider) {
        this.userInfoProvider = userInfoProvider;
    }

    public void setOauthDataProvider(OAuthDataProvider oauthDataProvider) {
        this.oauthDataProvider = oauthDataProvider;
    }
}
