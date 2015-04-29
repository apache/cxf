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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.oauth2.provider.DefaultEncryptingOAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.rs.security.oauth2.utils.crypto.ModelEncryptionSupport;
import org.apache.cxf.rt.security.crypto.KeyProperties;

public class DefaultEncryptingCodeDataProvider extends DefaultEncryptingOAuthDataProvider 
    implements AuthorizationCodeDataProvider {
    private long grantLifetime;
    private Set<String> grants = Collections.synchronizedSet(new HashSet<String>());
    public DefaultEncryptingCodeDataProvider(String algo, int keySize) {
        super(algo, keySize);
    }
    public DefaultEncryptingCodeDataProvider(KeyProperties props) {
        super(props);
    }
    public DefaultEncryptingCodeDataProvider(SecretKey key) {
        super(key);
    }
    @Override
    public ServerAuthorizationCodeGrant createCodeGrant(AuthorizationCodeRegistration reg)
        throws OAuthServiceException {
        ServerAuthorizationCodeGrant grant = doCreateCodeGrant(reg);
        saveAuthorizationGrant(grant);
        return grant;
    }

    @Override
    public ServerAuthorizationCodeGrant removeCodeGrant(String code) throws OAuthServiceException {
        grants.remove(code);
        return ModelEncryptionSupport.decryptCodeGrant(this, code, key);
    }
    
    protected ServerAuthorizationCodeGrant doCreateCodeGrant(AuthorizationCodeRegistration reg)
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
    
    protected void saveAuthorizationGrant(ServerAuthorizationCodeGrant grant) { 
        String encrypted = ModelEncryptionSupport.encryptCodeGrant(grant, key);
        grant.setCode(encrypted);
        grants.add(encrypted);
    }
}
