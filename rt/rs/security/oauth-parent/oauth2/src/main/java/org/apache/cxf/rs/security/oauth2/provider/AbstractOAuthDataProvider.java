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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.tokens.bearer.BearerAccessToken;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

public abstract class AbstractOAuthDataProvider implements OAuthDataProvider, ClientRegistrationProvider {
    private long accessTokenLifetime = 3600L;
    private long refreshTokenLifetime; // refresh tokens are eternal by default
    private boolean recycleRefreshTokens = true;
    private Map<String, OAuthPermission> permissionMap = new HashMap<String, OAuthPermission>();
    private MessageContext messageContext;
    private List<String> defaultScopes;
    private List<String> requiredScopes;
    private List<String> invisibleToClientScopes;
    private boolean supportPreauthorizedTokens;
    private boolean useJwtFormatForAccessTokens;
    private OAuthJoseJwtProducer jwtAccessTokenHandler;
    
    protected AbstractOAuthDataProvider() {
    }
    
    @Override
    public ServerAccessToken createAccessToken(AccessTokenRegistration reg)
        throws OAuthServiceException {
        ServerAccessToken at = doCreateAccessToken(reg);
        saveAccessToken(at);
        if (isRefreshTokenSupported(reg.getApprovedScope())) {
            createNewRefreshToken(at);
        }
        return at;
    }
    
    protected ServerAccessToken doCreateAccessToken(AccessTokenRegistration atReg) {
        ServerAccessToken at = createNewAccessToken(atReg.getClient());
        at.setAudiences(atReg.getAudiences());
        at.setGrantType(atReg.getGrantType());
        List<String> theScopes = atReg.getApprovedScope();
        List<OAuthPermission> thePermissions = 
            convertScopeToPermissions(atReg.getClient(), theScopes);
        at.setScopes(thePermissions);
        at.setSubject(atReg.getSubject());
        at.setClientCodeVerifier(atReg.getClientCodeVerifier());
        at.setNonce(atReg.getNonce());
        at.setResponseType(atReg.getResponseType());
        at.setGrantCode(atReg.getGrantCode());
        at.getExtraProperties().putAll(atReg.getExtraProperties());
        
        if (isUseJwtFormatForAccessTokens()) {
            JwtClaims claims = createJwtAccessToken(at);
            String jose = processJwtAccessToken(claims);
            at.setTokenKey(jose);
        }
        
        return at;
    }
    
    protected JwtClaims createJwtAccessToken(ServerAccessToken at) {
        JwtClaims claims = new JwtClaims();
        claims.setTokenId(at.getTokenKey());
        claims.setAudience(at.getClient().getClientId());
        claims.setIssuedAt(at.getIssuedAt());
        if (at.getExpiresIn() > 0) {
            claims.setExpiryTime(at.getIssuedAt() + at.getExpiresIn());
        }
        if (at.getSubject() != null) {
            claims.setSubject(at.getSubject().getLogin());
        }
        if (at.getIssuer() != null) {
            claims.setIssuer(at.getIssuer());
        }
        if (!at.getScopes().isEmpty()) {
            claims.setClaim(OAuthConstants.SCOPE, 
                            OAuthUtils.convertPermissionsToScopeList(at.getScopes()));
        }
        // OAuth2 resource indicators (resource server audience)
        if (at.getAudiences().isEmpty()) {
            List<String> resourceAudiences = at.getAudiences();
            claims.setClaim("resource", 
                            resourceAudiences.size() == 1 ? resourceAudiences.get(0) : resourceAudiences);
        }
        
        //TODO: consider auto-setting all the remaining token properties as claims either optionally 
        // or if JWE encryption is enabled for the providers be able to choose if they
        // want to save JOSE token representations only - though the providers can always override
        // this method too and set the extra claims. If all ServerAccessToken properties are set as claims
        // then the providers will only have to save ServerAccessToken.getTokenKey() in 
        // saveAccessToken(ServerAccessToken) which will be a JOSE representation of a given ServerAccessToken
        // instance but will have to restore ServerAccessToken from it when the runtime requests ServerAccessToken
        // for the validation purposes. 
        
        return claims;
    }

    protected ServerAccessToken createNewAccessToken(Client client) {
        return new BearerAccessToken(client, accessTokenLifetime);
    }
    
    @Override
    public ServerAccessToken refreshAccessToken(Client client, String refreshTokenKey,
                                                List<String> restrictedScopes) throws OAuthServiceException {
        RefreshToken currentRefreshToken = recycleRefreshTokens 
            ? revokeRefreshToken(refreshTokenKey) : getRefreshToken(refreshTokenKey);
        if (currentRefreshToken == null) { 
            throw new OAuthServiceException(OAuthConstants.ACCESS_DENIED);
        }
        if (OAuthUtils.isExpired(currentRefreshToken.getIssuedAt(), currentRefreshToken.getExpiresIn())) {
            if (!recycleRefreshTokens) {
                revokeRefreshToken(refreshTokenKey);
            }
            throw new OAuthServiceException(OAuthConstants.ACCESS_DENIED);
        }
        if (recycleRefreshTokens) {
            revokeAccessTokens(currentRefreshToken);
        }
        
        ServerAccessToken at = doRefreshAccessToken(client, currentRefreshToken, restrictedScopes);
        saveAccessToken(at);
        if (recycleRefreshTokens) {
            createNewRefreshToken(at);
        } else {
            updateRefreshToken(currentRefreshToken, at);
        }
        return at;
    }
    
    @Override
    public void revokeToken(Client client, String tokenKey, String tokenTypeHint) throws OAuthServiceException {
        ServerAccessToken accessToken = null;
        if (!OAuthConstants.REFRESH_TOKEN.equals(tokenTypeHint)) { 
            accessToken = revokeAccessToken(tokenKey);
        }
        if (accessToken != null) {
            handleLinkedRefreshToken(accessToken);
        } else if (!OAuthConstants.ACCESS_TOKEN.equals(tokenTypeHint)) {
            RefreshToken currentRefreshToken = revokeRefreshToken(tokenKey);
            revokeAccessTokens(currentRefreshToken);
        }
    }
    protected void handleLinkedRefreshToken(ServerAccessToken accessToken) {
        if (accessToken != null && accessToken.getRefreshToken() != null) {
            RefreshToken rt = getRefreshToken(accessToken.getRefreshToken());
            if (rt == null) {
                return;
            }
            
            unlinkRefreshAccessToken(rt, accessToken.getTokenKey());
            if (rt.getAccessTokens().isEmpty()) {
                revokeRefreshToken(rt.getTokenKey());
            } else {
                saveRefreshToken(rt);
            }
        }
        
    }

    protected void revokeAccessTokens(RefreshToken currentRefreshToken) {
        if (currentRefreshToken != null) {
            for (String accessTokenKey : currentRefreshToken.getAccessTokens()) {
                revokeAccessToken(accessTokenKey);
            }
        }
    }

    protected void unlinkRefreshAccessToken(RefreshToken rt, String tokenKey) {
        List<String> accessTokenKeys = rt.getAccessTokens();
        for (int i = 0; i < accessTokenKeys.size(); i++) {
            if (accessTokenKeys.get(i).equals(tokenKey)) {
                accessTokenKeys.remove(i);
                break;
            }
        }
    }

        
    
    @Override
    public List<OAuthPermission> convertScopeToPermissions(Client client, List<String> requestedScopes) {
        if (requiredScopes != null && !requestedScopes.containsAll(requiredScopes)) {
            throw new OAuthServiceException("Required scopes are missing");
        }
        if (requestedScopes.isEmpty()) {
            return Collections.emptyList();
        } else if (!permissionMap.isEmpty()) {
            List<OAuthPermission> list = new ArrayList<OAuthPermission>();
            for (String scope : requestedScopes) {
                OAuthPermission permission = permissionMap.get(scope);
                if (permission == null) {
                    throw new OAuthServiceException("Unexpected scope: " + scope);
                }
                list.add(permission);
            }
            return list;
        } else {
            throw new OAuthServiceException("Requested scopes can not be mapped");
        }
    }

    @Override
    public ServerAccessToken getPreauthorizedToken(Client client, 
                                                   List<String> requestedScopes,
                                                   UserSubject sub, 
                                                   String grantType) throws OAuthServiceException {
        if (!isSupportPreauthorizedTokens()) {
            return null;
        }

        ServerAccessToken token = null;
        for (ServerAccessToken at : getAccessTokens(client, sub)) {
            if (at.getClient().getClientId().equals(client.getClientId())
                && at.getGrantType().equals(grantType)
                && (sub == null || at.getSubject().getLogin().equals(sub.getLogin()))) {
                token = at;
                break;
            }
        }
        if (token != null 
            && OAuthUtils.isExpired(token.getIssuedAt(), token.getExpiresIn())) {
            revokeToken(client, token.getTokenKey(), OAuthConstants.ACCESS_TOKEN);
            token = null;
        }
        return token;
        
    }
    
    protected boolean isRefreshTokenSupported(List<String> theScopes) {
        return theScopes.contains(OAuthConstants.REFRESH_TOKEN_SCOPE);
    }

    protected String getCurrentRequestedGrantType() {
        return (String)messageContext.get(OAuthConstants.GRANT_TYPE);
    }
    protected String getCurrentClientSecret() {
        return (String)messageContext.get(OAuthConstants.CLIENT_SECRET);
    }
    protected RefreshToken updateRefreshToken(RefreshToken rt, ServerAccessToken at) {
        linkAccessTokenToRefreshToken(rt, at);
        saveRefreshToken(rt);
        linkRefreshTokenToAccessToken(rt, at);
        return rt;
    }
    protected RefreshToken createNewRefreshToken(ServerAccessToken at) {
        RefreshToken rt = doCreateNewRefreshToken(at);
        return updateRefreshToken(rt, at);
    }
    protected RefreshToken doCreateNewRefreshToken(ServerAccessToken at) {
        RefreshToken rt = new RefreshToken(at.getClient(), refreshTokenLifetime);
        if (at.getAudiences() != null) {
            List<String> audiences = new LinkedList<String>();
            audiences.addAll(at.getAudiences());
            rt.setAudiences(audiences);
        }
        rt.setGrantType(at.getGrantType());
        if (at.getScopes() != null) {
            List<OAuthPermission> scopes = new LinkedList<OAuthPermission>();
            scopes.addAll(at.getScopes());
            rt.setScopes(scopes);
        }
        rt.setSubject(at.getSubject());
        rt.setClientCodeVerifier(at.getClientCodeVerifier());
        return rt;
    }
    
    protected void linkAccessTokenToRefreshToken(RefreshToken rt, ServerAccessToken at) {
        rt.getAccessTokens().add(at.getTokenKey());
    }
    protected void linkRefreshTokenToAccessToken(RefreshToken rt, ServerAccessToken at) {
        at.setRefreshToken(rt.getTokenKey());
    }

    protected ServerAccessToken doRefreshAccessToken(Client client, 
                                                     RefreshToken oldRefreshToken, 
                                                     List<String> restrictedScopes) {
        ServerAccessToken at = createNewAccessToken(client);
        at.setAudiences(oldRefreshToken.getAudiences());
        at.setGrantType(oldRefreshToken.getGrantType());
        at.setSubject(oldRefreshToken.getSubject());
        if (restrictedScopes.isEmpty()) {
            at.setScopes(oldRefreshToken.getScopes());
        } else {
            List<OAuthPermission> theNewScopes = convertScopeToPermissions(client, restrictedScopes);
            if (oldRefreshToken.getScopes().containsAll(theNewScopes)) {
                at.setScopes(theNewScopes);
            } else {
                throw new OAuthServiceException("Invalid scopes");
            }
        }
        return at;
    }
    
    public void setAccessTokenLifetime(long accessTokenLifetime) {
        this.accessTokenLifetime = accessTokenLifetime;
    }

    public void setRefreshTokenLifetime(long refreshTokenLifetime) {
        this.refreshTokenLifetime = refreshTokenLifetime;
    }
    
    public void setRecycleRefreshTokens(boolean recycleRefreshTokens) {
        this.recycleRefreshTokens = recycleRefreshTokens;
    }
    
    public void init() {
        for (OAuthPermission perm : permissionMap.values()) {
            if (defaultScopes != null && defaultScopes.contains(perm.getPermission())) {
                perm.setDefaultPermission(true);
            }
            if (invisibleToClientScopes != null && invisibleToClientScopes.contains(perm.getPermission())) {
                perm.setInvisibleToClient(true);
            }
        }
    }
    
    public void close() {
    }
    
    public Map<String, OAuthPermission> getPermissionMap() {
        return permissionMap;
    }

    public void setPermissionMap(Map<String, OAuthPermission> permissionMap) {
        this.permissionMap = permissionMap;
    }
    
    public void setSupportedScopes(Map<String, String> scopes) {
        for (Map.Entry<String, String> entry : scopes.entrySet()) {
            OAuthPermission permission = new OAuthPermission(entry.getKey(), entry.getValue());
            permissionMap.put(entry.getKey(), permission);
        }
    }

    public MessageContext getMessageContext() {
        return messageContext;
    }

    public void setMessageContext(MessageContext messageContext) {
        this.messageContext = messageContext;
    }
    
    protected void removeClientTokens(Client c) {
        for (RefreshToken rt : getRefreshTokens(c, null)) {
            revokeRefreshToken(rt.getTokenKey());
        }
        for (ServerAccessToken at : getAccessTokens(c, null)) {
            revokeAccessToken(at.getTokenKey());
        }
    }
    
    @Override
    public Client removeClient(String clientId) {
        Client c = getClient(clientId);
        removeClientTokens(c);
        doRemoveClient(c);
        return c;
    }
    
    protected ServerAccessToken revokeAccessToken(String accessTokenKey) {
        ServerAccessToken at = getAccessToken(accessTokenKey);
        if (at != null) {
            doRevokeAccessToken(at);
        }
        return at;
    }
    protected RefreshToken revokeRefreshToken(String refreshTokenKey) { 
        RefreshToken refreshToken = getRefreshToken(refreshTokenKey);
        if (refreshToken != null) {
            doRevokeRefreshToken(refreshToken);
        }
        return refreshToken;
    }
    
    
    protected abstract void saveAccessToken(ServerAccessToken serverToken);
    protected abstract void saveRefreshToken(RefreshToken refreshToken);
    protected abstract void doRevokeAccessToken(ServerAccessToken accessToken);
    protected abstract void doRevokeRefreshToken(RefreshToken  refreshToken);
    protected abstract RefreshToken getRefreshToken(String refreshTokenKey);
    protected abstract void doRemoveClient(Client c);

    public List<String> getDefaultScopes() {
        return defaultScopes;
    }

    public void setDefaultScopes(List<String> defaultScopes) {
        this.defaultScopes = defaultScopes;
    }

    public List<String> getRequiredScopes() {
        return requiredScopes;
    }

    public void setRequiredScopes(List<String> requiredScopes) {
        this.requiredScopes = requiredScopes;
    }

    public List<String> getInvisibleToClientScopes() {
        return invisibleToClientScopes;
    }

    public void setInvisibleToClientScopes(List<String> invisibleToClientScopes) {
        this.invisibleToClientScopes = invisibleToClientScopes;
    }

    public boolean isSupportPreauthorizedTokens() {
        return supportPreauthorizedTokens;
    }

    public void setSupportPreauthorizedTokens(boolean supportPreauthorizedTokens) {
        this.supportPreauthorizedTokens = supportPreauthorizedTokens;
    }
    protected static boolean isClientMatched(Client c, UserSubject resourceOwner) {
        return resourceOwner == null 
            || c.getResourceOwnerSubject() != null 
                && c.getResourceOwnerSubject().getLogin().equals(resourceOwner.getLogin());
    }
    protected static boolean isTokenMatched(ServerAccessToken token, Client c, UserSubject sub) {
        if (c == null || token.getClient().getClientId().equals(c.getClientId())) {
            UserSubject tokenSub = token.getSubject();
            if (sub == null || tokenSub != null && tokenSub.getLogin().equals(sub.getLogin())) {
                return true;
            }
        }
        return false;
    }
    public void setClients(List<Client> clients) {    
        for (Client c : clients) {
            setClient(c);
        }
    }

    public boolean isUseJwtFormatForAccessTokens() {
        return useJwtFormatForAccessTokens;
    }

    public void setUseJwtFormatForAccessTokens(boolean useJwtFormatForAccessTokens) {
        this.useJwtFormatForAccessTokens = useJwtFormatForAccessTokens;
    }

    public OAuthJoseJwtProducer getJwtAccessTokenHandler() {
        return jwtAccessTokenHandler;
    }

    public void setJwtAccessTokenHandler(OAuthJoseJwtProducer jwtAccessTokenHandler) {
        this.jwtAccessTokenHandler = jwtAccessTokenHandler;
    }
    
    protected String processJwtAccessToken(JwtClaims jwtCliams) {
        // It will JWS-sign (default) and/or JWE-encrypt
        OAuthJoseJwtProducer processor = 
            getJwtAccessTokenHandler() == null ? new OAuthJoseJwtProducer() : getJwtAccessTokenHandler(); 
        return processor.processJwt(new JwtToken(jwtCliams));
    }
}
