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

import org.apache.cxf.rs.security.oauth2.provider.AbstractOAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;

public abstract class AbstractCodeDataProvider extends AbstractOAuthDataProvider 
    implements AuthorizationCodeDataProvider {
    private long codeLifetime = 3600L;
    
    protected AbstractCodeDataProvider() {
    }
        
    @Override
    public ServerAuthorizationCodeGrant createCodeGrant(AuthorizationCodeRegistration reg) 
        throws OAuthServiceException {
        ServerAuthorizationCodeGrant grant = initCodeGrant(reg, codeLifetime);
        saveCodeGrant(grant);
        return grant;
    }
    
    public void setCodeLifetime(long codeLifetime) {
        this.codeLifetime = codeLifetime;
    }
    
    protected abstract void saveCodeGrant(ServerAuthorizationCodeGrant grant);
    
    public static ServerAuthorizationCodeGrant initCodeGrant(AuthorizationCodeRegistration reg, long lifetime) {
        ServerAuthorizationCodeGrant grant = new ServerAuthorizationCodeGrant(reg.getClient(), lifetime);
        grant.setRedirectUri(reg.getRedirectUri());
        grant.setSubject(reg.getSubject());
        grant.setRequestedScopes(reg.getRequestedScope());
        grant.setApprovedScopes(reg.getApprovedScope());
        grant.setAudience(reg.getAudience());
        grant.setClientCodeChallenge(reg.getClientCodeChallenge());
        return grant;
    }
    
}
