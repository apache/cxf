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

package org.apache.cxf.rs.security.oauth.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.oauth.OAuth;
import net.oauth.OAuthProblemException;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth.data.AccessToken;
import org.apache.cxf.rs.security.oauth.data.Client;
import org.apache.cxf.rs.security.oauth.data.OAuthPermission;
import org.apache.cxf.rs.security.oauth.data.RequestToken;
import org.apache.cxf.rs.security.oauth.data.RequestTokenRegistration;
import org.apache.cxf.rs.security.oauth.data.Token;
import org.apache.cxf.rs.security.oauth.provider.DefaultOAuthValidator;
import org.apache.cxf.rs.security.oauth.provider.MD5TokenGenerator;
import org.apache.cxf.rs.security.oauth.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth.provider.OAuthServiceException;

public class MemoryOAuthDataProvider implements OAuthDataProvider {

    private static final ConcurrentHashMap<String, OAuthPermission> AVAILABLE_PERMISSIONS = 
        new ConcurrentHashMap<String, OAuthPermission>();

    static {
        AVAILABLE_PERMISSIONS
                .put("read_info", new OAuthPermission("read_info", "Read your personal information",
                        "ROLE_USER"));
        AVAILABLE_PERMISSIONS.put("modify_info",
                new OAuthPermission("modify_info", "Modify your personal information", "ROLE_ADMIN"));
    }

    protected ConcurrentHashMap<String, Client> clientAuthInfo = new ConcurrentHashMap<String, Client>();

    protected MetadataMap<String, String> userRegisteredClients = new MetadataMap<String, String>();

    protected MetadataMap<String, String> userAuthorizedClients = new MetadataMap<String, String>();

    protected ConcurrentHashMap<String, Token> oauthTokens = new ConcurrentHashMap<String, Token>();

    protected MD5TokenGenerator tokenGenerator = new MD5TokenGenerator();

    protected DefaultOAuthValidator validator = new DefaultOAuthValidator();

    public MemoryOAuthDataProvider() {
        Client client = new Client(OAuthTestUtils.CLIENT_ID, OAuthTestUtils.CLIENT_ID, 
            OAuthTestUtils.CLIENT_SECRET,
            OAuthTestUtils.CALLBACK, OAuthTestUtils.APPLICATION_NAME);
        clientAuthInfo.put(OAuthTestUtils.CLIENT_ID, client);
    }
    
    public List<OAuthPermission> getPermissionsInfo(List<String> requestPermissions) {
        List<OAuthPermission> permissions = new ArrayList<OAuthPermission>();
        for (String requestScope : requestPermissions) {
            OAuthPermission oAuthPermission = AVAILABLE_PERMISSIONS.get(requestScope);
            permissions.add(oAuthPermission);
        }
    
        return permissions;
    }
    
    public Client getClient(String consumerKey) {
        return clientAuthInfo.get(consumerKey);
    }

    public RequestToken createRequestToken(RequestTokenRegistration reg) throws OAuthServiceException {
        String token = generateToken();
        String tokenSecret = generateToken();

        RequestToken reqToken = new RequestToken(reg.getClient(), token, tokenSecret, 
                                                 reg.getLifetime());
        reqToken.setPermissions(reg.getPermissions());
        reqToken.setScopes(reg.getScopes());
        
        oauthTokens.put(token, reqToken);
        return reqToken;
    }

    public RequestToken getRequestToken(String tokenString) throws OAuthServiceException {

        Token token = oauthTokens.get(tokenString);
        if (token == null || (!RequestToken.class.isAssignableFrom(token.getClass()))) {
            throw new OAuthServiceException(new OAuthProblemException(OAuth.Problems.TOKEN_REJECTED));
        }
        RequestToken requestToken = (RequestToken) token;

        Client c = token.getClient();
        if (c == null) {
            throw new OAuthServiceException(new OAuthProblemException(OAuth.Problems.CONSUMER_KEY_UNKNOWN));
        }
        try {
            validator.validateToken(requestToken);
        } catch (OAuthProblemException ex) {
            throw new OAuthServiceException(ex);
        }
        return requestToken;
    }

    public String createRequestTokenVerifier(RequestToken requestToken) throws
            OAuthServiceException {
        requestToken.setOauthVerifier(generateToken());
        return requestToken.getOauthVerifier();
    }

    public AccessToken createAccessToken(RequestToken requestToken) throws
            OAuthServiceException {

        Client client = requestToken.getClient();
        requestToken = getRequestToken(requestToken.getTokenString());

        String accessTokenString = generateToken();
        String tokenSecretString = generateToken();

        AccessToken accessToken = new AccessToken(client, accessTokenString, tokenSecretString, 3600);

        accessToken.setPermissions(requestToken.getPermissions());
        accessToken.setScopes(requestToken.getScopes());

        synchronized (oauthTokens) {
            oauthTokens.remove(requestToken.getTokenString());
            oauthTokens.put(accessTokenString, accessToken);
            synchronized (userAuthorizedClients) {
                userAuthorizedClients.add(client.getConsumerKey(), client.getConsumerKey());
            }
        }

        return accessToken;
    }

    public AccessToken getAccessToken(String accessToken) throws OAuthServiceException
    {
        Token token = oauthTokens.get(accessToken);
        if (token == null || !AccessToken.class.isAssignableFrom(token.getClass())) {
            throw new OAuthServiceException(new OAuthProblemException(OAuth.Problems.TOKEN_REJECTED));
        }
        try {
            validator.validateToken(token);
        } catch (OAuthProblemException ex) {
            throw new OAuthServiceException(ex);
        }
        return (AccessToken) token;
    }

    

    public void removeTokens(String consumerKey) {
        if (!StringUtils.isEmpty(consumerKey)) {
            List<String> registeredApps = this.userAuthorizedClients.get(consumerKey);
            if (registeredApps != null) {
                registeredApps.remove(consumerKey);
            }
            for (Token token : oauthTokens.values()) {
                Client authNInfo = token.getClient();
                if (consumerKey.equals(authNInfo.getConsumerKey())) {
                    oauthTokens.remove(token.getTokenString());
                }
            }
        }
    }

    protected String generateToken() throws OAuthServiceException {
        String token;
        try {
            token = tokenGenerator.generateToken(UUID.randomUUID().toString().getBytes("UTF-8"));
        } catch (Exception e) {
            throw new OAuthServiceException("Unable to create token ", e.getCause());
        }
        return token;
    }

    public void setClientAuthInfo(Map<String, Client> clientAuthInfo) {
        this.clientAuthInfo.putAll(clientAuthInfo);
    }
}
