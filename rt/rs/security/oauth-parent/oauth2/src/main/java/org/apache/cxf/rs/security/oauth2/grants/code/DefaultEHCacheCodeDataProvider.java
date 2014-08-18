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

import net.sf.ehcache.Ehcache;

import org.apache.cxf.Bus;
import org.apache.cxf.rs.security.oauth2.provider.DefaultEHCacheOAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

public class DefaultEHCacheCodeDataProvider extends DefaultEHCacheOAuthDataProvider 
    implements AuthorizationCodeDataProvider {
    public static final String CODE_GRANT_CACHE_KEY = "cxf.oauth2.client.cache";
    
    private long grantLifetime;
    private Ehcache codeGrantCache;
    
    protected DefaultEHCacheCodeDataProvider() {
        this(DEFAULT_CONFIG_URL, null);
    }
    
    protected DefaultEHCacheCodeDataProvider(String configFileURL, Bus bus) {
        this(configFileURL, bus, CODE_GRANT_CACHE_KEY,
             CLIENT_CACHE_KEY, ACCESS_TOKEN_CACHE_KEY, REFRESH_TOKEN_CACHE_KEY);
    }
    
    protected DefaultEHCacheCodeDataProvider(String configFileURL, 
                                               Bus bus,
                                               String clientCacheKey,
                                               String codeCacheKey,
                                               String accessTokenKey,
                                               String refreshTokenKey) {
        super(configFileURL, bus, clientCacheKey, accessTokenKey, refreshTokenKey);
        codeGrantCache = createCache(cacheManager, codeCacheKey);
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
        ServerAuthorizationCodeGrant grant = getCacheValue(codeGrantCache, 
                                                                  code, 
                                                                  ServerAuthorizationCodeGrant.class);
        if (grant != null) {
            codeGrantCache.remove(code);
        }
        return grant;
    }
    
    protected ServerAuthorizationCodeGrant doCreateCodeGrant(AuthorizationCodeRegistration reg)
        throws OAuthServiceException {
        ServerAuthorizationCodeGrant grant = 
            new ServerAuthorizationCodeGrant(reg.getClient(), getCode(reg), getGrantLifetime(), getIssuedAt());
        grant.setApprovedScopes(getApprovedScopes(reg));
        grant.setAudience(reg.getAudience());
        grant.setClientCodeVerifier(reg.getClientCodeVerifier());
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
        putCacheValue(codeGrantCache, grant.getCode(), grant, grant.getExpiresIn());
    }
}
