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

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.rs.security.oauth2.client.ClientCodeRequestFilter;
import org.apache.cxf.rs.security.oauth2.client.ClientTokenContext;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;

public class OidcClientCodeRequestFilter extends ClientCodeRequestFilter {

    private UserInfoClient userInfoClient;
    private boolean userInfoRequired = true; 
    @Override
    protected ClientTokenContext createTokenContext(ContainerRequestContext rc, ClientAccessToken at) {
        OidcClientTokenContextImpl ctx = new OidcClientTokenContextImpl();
        if (at != null) {
            ctx.setIdToken(userInfoClient.getIdToken(at, getConsumer().getKey()));
            if (userInfoRequired) {
                ctx.setUserInfo(userInfoClient.getUserInfo(at, ctx.getIdToken()));
            }
            rc.setSecurityContext(new OidcSecurityContext(ctx));
        }
        
        return ctx;
    }
    public void setUserInfoClient(UserInfoClient userInfoClient) {
        this.userInfoClient = userInfoClient;
    }
    public void setUserInfoRequired(boolean userInfoRequired) {
        this.userInfoRequired = userInfoRequired;
    }
    @Override
    protected void checkSecurityContextStart(ContainerRequestContext rc) {
        SecurityContext sc = rc.getSecurityContext();
        if (sc != null && !(sc instanceof OidcSecurityContext)) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
    }
}
