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

package org.apache.cxf.rs.security.oauth2.grants.code;

import java.util.List;

import org.apache.cxf.rs.security.oauth2.provider.AbstractOAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;


/**
 * Abstract AuthorizationCodeDataProvider implementation
 */
public abstract class AbstractAuthorizationCodeDataProvider
    extends AbstractOAuthDataProvider implements AuthorizationCodeDataProvider {

    private long grantLifetime = 3600L;

    public ServerAuthorizationCodeGrant createCodeGrant(AuthorizationCodeRegistration reg)
        throws OAuthServiceException {
        ServerAuthorizationCodeGrant grant =
            new ServerAuthorizationCodeGrant(reg.getClient(), getCode(reg), getGrantLifetime(), getIssuedAt());
        grant.setApprovedScopes(getApprovedScopes(reg));
        grant.setAudience(reg.getAudience());
        grant.setClientCodeChallenge(reg.getClientCodeChallenge());
        grant.setSubject(reg.getSubject());
        grant.setRedirectUri(reg.getRedirectUri());
        return grant;
    }

    protected List<String> getApprovedScopes(AuthorizationCodeRegistration reg) {
        return reg.getApprovedScope();
    }

    protected String getCode(AuthorizationCodeRegistration reg) {
        return OAuthUtils.generateRandomTokenKey();
    }

    public long getGrantLifetime() {
        return grantLifetime;
    }

    public void setGrantLifetime(long lifetime) {
        this.grantLifetime = lifetime;
    }

    protected long getIssuedAt() {
        return OAuthUtils.getIssuedAt();
    }
}
