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

import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.Ehcache;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.DefaultEHCacheOAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;

public class DefaultEHCacheCodeDataProvider extends DefaultEHCacheOAuthDataProvider 
    implements AuthorizationCodeDataProvider {
    public static final String CODE_GRANT_CACHE_KEY = "cxf.oauth2.codegrant.cache";
    
    private long codeLifetime = 10 * 60;
    private Ehcache codeGrantCache;
    
    protected DefaultEHCacheCodeDataProvider() {
        this(DEFAULT_CONFIG_URL, BusFactory.getThreadDefaultBus(true));
    }
    
    protected DefaultEHCacheCodeDataProvider(String configFileURL, Bus bus) {
        this(configFileURL, bus, CLIENT_CACHE_KEY, CODE_GRANT_CACHE_KEY,
             ACCESS_TOKEN_CACHE_KEY, REFRESH_TOKEN_CACHE_KEY);
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
    protected Client doRemoveClient(Client c) {
        removeClientCodeGrants(c);
        return super.doRemoveClient(c);
    }
    
    protected void removeClientCodeGrants(Client c) {
        for (ServerAuthorizationCodeGrant grant : getCodeGrants(c, null)) {
            removeCodeGrant(grant.getCode());
        }
    }
    
    @Override
    public ServerAuthorizationCodeGrant createCodeGrant(AuthorizationCodeRegistration reg)
        throws OAuthServiceException {
        ServerAuthorizationCodeGrant grant = doCreateCodeGrant(reg);
        saveCodeGrant(grant);
        return grant;
    }
    
    protected ServerAuthorizationCodeGrant doCreateCodeGrant(AuthorizationCodeRegistration reg)
        throws OAuthServiceException {
        return AbstractCodeDataProvider.initCodeGrant(reg, codeLifetime);
    }

    public List<ServerAuthorizationCodeGrant> getCodeGrants(Client c, UserSubject sub) {
        List<String> keys = CastUtils.cast(codeGrantCache.getKeys());
        List<ServerAuthorizationCodeGrant> grants = 
            new ArrayList<ServerAuthorizationCodeGrant>(keys.size());
        for (String key : keys) {
            ServerAuthorizationCodeGrant grant = getCodeGrant(key);
            if (c == null || grant.getClient().getClientId().equals(c.getClientId())) {
                UserSubject grantSub = grant.getSubject();
                if (sub == null || grantSub != null && grantSub.getLogin().equals(sub.getLogin())) {
                    grants.add(grant);
                }
            }
        }
        return grants;
    }
    
    @Override
    public ServerAuthorizationCodeGrant removeCodeGrant(String code) throws OAuthServiceException {
        ServerAuthorizationCodeGrant grant = getCodeGrant(code);
        if (grant != null) {
            codeGrantCache.remove(code);
        }
        return grant;
    }
    
    public ServerAuthorizationCodeGrant getCodeGrant(String code) throws OAuthServiceException {
        return getCacheValue(codeGrantCache, 
                             code, 
                             ServerAuthorizationCodeGrant.class);
    }
        
    protected void saveCodeGrant(ServerAuthorizationCodeGrant grant) { 
        putCacheValue(codeGrantCache, grant.getCode(), grant, grant.getExpiresIn());
    }

    public void setCodeLifetime(long codeLifetime) {
        this.codeLifetime = codeLifetime;
    }
}
