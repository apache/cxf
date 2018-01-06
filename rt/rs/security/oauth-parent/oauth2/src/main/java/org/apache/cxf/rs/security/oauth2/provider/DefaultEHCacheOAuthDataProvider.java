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
package org.apache.cxf.rs.security.oauth2.provider;

import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.rs.security.jose.jwt.JoseJwtConsumer;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;
import org.apache.cxf.rs.security.oauth2.utils.EHCacheUtil;
import org.apache.cxf.rs.security.oauth2.utils.JwtTokenUtils;

public class DefaultEHCacheOAuthDataProvider extends AbstractOAuthDataProvider {
    public static final String CLIENT_CACHE_KEY = "cxf.oauth2.client.cache";
    public static final String ACCESS_TOKEN_CACHE_KEY = "cxf.oauth2.accesstoken.cache";
    public static final String REFRESH_TOKEN_CACHE_KEY = "cxf.oauth2.refreshtoken.cache";
    public static final String DEFAULT_CONFIG_URL = "cxf-oauth2-ehcache.xml";

    protected CacheManager cacheManager;
    private Ehcache clientCache;
    private Ehcache accessTokenCache;
    private Ehcache refreshTokenCache;
    private boolean storeJwtTokenKeyOnly;
    private JoseJwtConsumer jwtTokenConsumer;

    public DefaultEHCacheOAuthDataProvider() {
        this(DEFAULT_CONFIG_URL, BusFactory.getThreadDefaultBus(true));
    }

    public DefaultEHCacheOAuthDataProvider(String configFileURL, Bus bus) {
        this(configFileURL, bus, CLIENT_CACHE_KEY, ACCESS_TOKEN_CACHE_KEY, REFRESH_TOKEN_CACHE_KEY);
    }

    public DefaultEHCacheOAuthDataProvider(String configFileURL,
                                               Bus bus,
                                               String clientCacheKey,
                                               String accessTokenCacheKey,
                                               String refreshTokenCacheKey) {
        createCaches(configFileURL, bus, clientCacheKey, accessTokenCacheKey, refreshTokenCacheKey);
    }

    @Override
    public Client doGetClient(String clientId) throws OAuthServiceException {
        return getCacheValue(clientCache, clientId, Client.class);
    }

    public void setClient(Client client) {
        putCacheValue(clientCache, client.getClientId(), client, 0);
    }

    @Override
    protected void doRemoveClient(Client c) {
        clientCache.remove(c.getClientId());
    }

    @Override
    public List<Client> getClients(UserSubject resourceOwner) {
        List<String> keys = CastUtils.cast(clientCache.getKeys());
        List<Client> clients = new ArrayList<>(keys.size());
        for (String key : keys) {
            Client c = doGetClient(key);
            if (isClientMatched(c, resourceOwner)) {
                clients.add(c);
            }
        }
        return clients;
    }

    @Override
    public List<ServerAccessToken> getAccessTokens(Client c, UserSubject sub) {
        List<String> keys = CastUtils.cast(accessTokenCache.getKeys());
        List<ServerAccessToken> tokens = new ArrayList<>(keys.size());
        for (String key : keys) {
            ServerAccessToken token = getAccessToken(key);
            if (isTokenMatched(token, c, sub)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    @Override
    public List<RefreshToken> getRefreshTokens(Client c, UserSubject sub) {
        List<String> keys = CastUtils.cast(refreshTokenCache.getKeys());
        List<RefreshToken> tokens = new ArrayList<>(keys.size());
        for (String key : keys) {
            RefreshToken token = getRefreshToken(key);
            if (isTokenMatched(token, c, sub)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    @Override
    public ServerAccessToken getAccessToken(String accessToken) throws OAuthServiceException {
        ServerAccessToken at = null;
        if (isUseJwtFormatForAccessTokens() && isStoreJwtTokenKeyOnly()) {
            String jose = getCacheValue(accessTokenCache, accessToken, String.class);
            if (jose != null) {
                JoseJwtConsumer theConsumer = jwtTokenConsumer == null ? new JoseJwtConsumer() : jwtTokenConsumer;
                at = JwtTokenUtils.createAccessTokenFromJwt(theConsumer, jose, this,
                                                                  super.getJwtAccessTokenClaimMap());
            }
        } else {
            at = getCacheValue(accessTokenCache, accessToken, ServerAccessToken.class);
        }
        return at;
    }

    @Override
    protected void doRevokeAccessToken(ServerAccessToken at) {
        accessTokenCache.remove(at.getTokenKey());
    }
    @Override
    protected RefreshToken getRefreshToken(String refreshTokenKey) {
        return getCacheValue(refreshTokenCache, refreshTokenKey, RefreshToken.class);
    }
    @Override
    protected void doRevokeRefreshToken(RefreshToken rt) {
        refreshTokenCache.remove(rt.getTokenKey());
    }

    protected void saveAccessToken(ServerAccessToken serverToken) {
        Object accessTokenObject = null;
        if (isUseJwtFormatForAccessTokens() && isStoreJwtTokenKeyOnly()) {
            accessTokenObject = serverToken.getTokenKey();
        } else {
            accessTokenObject = serverToken;
        }
        putCacheValue(accessTokenCache, serverToken.getTokenKey(), accessTokenObject, serverToken.getExpiresIn());
    }

    protected void saveRefreshToken(RefreshToken refreshToken) {
        putCacheValue(refreshTokenCache, refreshToken.getTokenKey(), refreshToken, refreshToken.getExpiresIn());
    }

    protected static <T> T getCacheValue(Ehcache cache, String key, Class<T> cls) {
        Element e = cache.get(key);
        if (e != null) {
            return cls.cast(e.getObjectValue());
        }
        return null;
    }
    protected static void putCacheValue(Ehcache cache, String key, Object value, long ttl) {
        Element element = new Element(key, value);
        int parsedTTL = (int)ttl;
        if (ttl != parsedTTL) {
            throw new OAuthServiceException("Requested time to live can not be supported");
        }
        element.setTimeToLive(parsedTTL);
        element.setTimeToIdle(parsedTTL);
        cache.put(element);
    }
    

    protected static Ehcache createCache(CacheManager cacheManager, String cacheKey) {
        CacheConfiguration clientCC = EHCacheUtil.getCacheConfiguration(cacheKey, cacheManager);
        return cacheManager.addCacheIfAbsent(new Cache(clientCC));
    }

    private void createCaches(String configFile, Bus bus,
                              String clientCacheKey, String accessTokenKey, String refreshTokenKey) {
        cacheManager = EHCacheUtil.createCacheManager(configFile, bus);

        clientCache = createCache(cacheManager, clientCacheKey);
        accessTokenCache = createCache(cacheManager, accessTokenKey);
        refreshTokenCache = createCache(cacheManager, refreshTokenKey);
    }

    @Override
    public void close() {
        cacheManager.shutdown();
    }

    public boolean isStoreJwtTokenKeyOnly() {
        return storeJwtTokenKeyOnly;
    }

    public void setStoreJwtTokenKeyOnly(boolean storeJwtTokenKeyOnly) {
        this.storeJwtTokenKeyOnly = storeJwtTokenKeyOnly;
    }

    public void setJwtTokenConsumer(JoseJwtConsumer jwtTokenConsumer) {
        this.jwtTokenConsumer = jwtTokenConsumer;
    }

}
