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
package org.apache.cxf.rs.security.oauth2.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.client.ClientWebApplicationException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.form.Form;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenGrant;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.provider.OAuthJSONProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

/**
 * The utility class for simplifying working with OAuth servers
 */
public final class OAuthClientUtils {
    private OAuthClientUtils() {
        
    }
    
    /**
     * Builds a complete URI for redirecting to OAuth Authorization Service
     * @param authorizationServiceURI the service endpoint address
     * @param clientId client registration id
     * @param redirectUri the uri the authorization code will be posted to
     * @param state the client state, example the key or the encrypted token 
     *              representing the info about the current end user's request
     * @scope scope the optional scope; if not specified then the authorization
     *              service will allocate the default scope               
     * @return authorization service URI
     */
    public static URI getAuthorizationURI(String authorizationServiceURI, 
                                          String clientId,
                                          String redirectUri,
                                          String state,
                                          String scope) {
        UriBuilder ub = getAuthorizationURIBuilder(authorizationServiceURI, 
                                                   clientId,
                                                   scope);
        if (redirectUri != null) {
            ub.queryParam(OAuthConstants.REDIRECT_URI, redirectUri);
        }
        if (state != null) {
            ub.queryParam(OAuthConstants.STATE, state);
        }
        return ub.build();
    }
    
    /**
     * Creates the builder for building OAuth AuthorizationService URIs
     * @param authorizationServiceURI the service endpoint address 
     * @param clientId client registration id
     * @param scope the optional scope; if not specified then the authorization
     *              service will allocate the default scope
     * @return the builder
     */
    public static UriBuilder getAuthorizationURIBuilder(String authorizationServiceURI, 
                                                 String clientId,
                                                 String scope) {
        UriBuilder ub = UriBuilder.fromUri(authorizationServiceURI);
        if (clientId != null) {
            ub.queryParam(OAuthConstants.CLIENT_ID, clientId);
        }
        if (scope != null) {
            ub.queryParam(OAuthConstants.SCOPE, scope);
        }
        ub.queryParam(OAuthConstants.RESPONSE_TYPE, OAuthConstants.CODE_RESPONSE_TYPE);
        return ub;                                   
    }
    
    /**
     * Obtains the access token from OAuth AccessToken Service 
     * using the initialized web client 
     * @param accessTokenService the AccessToken client
     * @param consumer {@link Consumer} representing the registered client 
     * @param grant {@link AccessTokenGrant} grant
     * @return {@link ClientAccessToken} access token
     * @throws OAuthServiceException
     */
    public static ClientAccessToken getAccessToken(WebClient accessTokenService,
                                                   Consumer consumer,
                                                   AccessTokenGrant grant) throws OAuthServiceException {
        
        return getAccessToken(accessTokenService, consumer, grant, true);
    }
    
    /**
     * Obtains the access token from OAuth AccessToken Service 
     * @param accessTokenServiceUri the AccessToken endpoint address
     * @param consumer {@link Consumer} representing the registered client 
     * @param grant {@link AccessTokenGrant} grant
     * @param setAuthorizationHeader if set to true then HTTP Basic scheme
     *           will be used to pass client id and secret, otherwise they will
     *           be passed in the form payload
     * @return {@link ClientAccessToken} access token
     * @throws OAuthServiceException
     */
    public static ClientAccessToken getAccessToken(String accessTokenServiceUri,
                                                   Consumer consumer,
                                                   AccessTokenGrant grant,
                                                   boolean setAuthorizationHeader) 
        throws OAuthServiceException {
        OAuthJSONProvider provider = new OAuthJSONProvider();
        WebClient accessTokenService = 
            WebClient.create(accessTokenServiceUri, Collections.singletonList(provider));
        accessTokenService.accept("application/json");
        return getAccessToken(accessTokenService, consumer, grant, true);
    }
    
    /**
     * Obtains the access token from OAuth AccessToken Service 
     * using the initialized web client 
     * @param accessTokenService the AccessToken client
     * @param consumer {@link Consumer} representing the registered client.
     * @param grant {@link AccessTokenGrant} grant
     * @param setAuthorizationHeader if set to true then HTTP Basic scheme
     *           will be used to pass client id and secret, otherwise they will
     *           be passed in the form payload  
     * @return {@link ClientAccessToken} access token
     * @throws OAuthServiceException
     */
    public static ClientAccessToken getAccessToken(WebClient accessTokenService,
                                                   Consumer consumer,
                                                   AccessTokenGrant grant,
                                                   boolean setAuthorizationHeader) 
        throws OAuthServiceException {
        
        Form form = new Form(grant.toMap());
    
        if (consumer != null) {
            if (setAuthorizationHeader) {
                StringBuilder sb = new StringBuilder();
                sb.append("Basic ");
                try {
                    String data = consumer.getKey() + ":" + consumer.getSecret();
                    sb.append(Base64Utility.encode(data.getBytes("UTF-8")));
                } catch (Exception ex) {
                    throw new ClientWebApplicationException(ex);
                }
                accessTokenService.header("Authorization", sb.toString());
            } else {
                form.set(OAuthConstants.CLIENT_ID, consumer.getKey());
                form.set(OAuthConstants.CLIENT_SECRET, consumer.getSecret());
            }
        } else {
            // in this case the AccessToken service is expected to find a mapping between
            // the authenticated credentials and the client registration id
        }
        Response response = accessTokenService.form(form);
        Map<String, String> map = null;
        try {
            map = new OAuthJSONProvider().readJSONResponse((InputStream)response.getEntity());
        } catch (IOException ex) {
            throw new ClientWebApplicationException(ex);
        }
        if (200 == response.getStatus()) {
            ClientAccessToken token = fromMapToClientToken(map);
            if (token == null) {
                throw new OAuthServiceException(OAuthConstants.SERVER_ERROR);
            } else {
                return token;
            }
        } else if (400 == response.getStatus() && map.containsKey(OAuthConstants.ERROR_KEY)) {
            OAuthError error = new OAuthError(map.get(OAuthConstants.ERROR_KEY),
                                              map.get(OAuthConstants.ERROR_DESCRIPTION_KEY));
            error.setErrorUri(map.get(OAuthConstants.ERROR_URI_KEY));
            throw new OAuthServiceException(error);
        } 
        throw new OAuthServiceException(OAuthConstants.SERVER_ERROR);
    }
    
    public static ClientAccessToken fromMapToClientToken(Map<String, String> map) {
        if (map.containsKey(OAuthConstants.ACCESS_TOKEN)
            && map.containsKey(OAuthConstants.ACCESS_TOKEN_TYPE)) {
            ClientAccessToken token = new ClientAccessToken(
                                          map.remove(OAuthConstants.ACCESS_TOKEN_TYPE),
                                          map.remove(OAuthConstants.ACCESS_TOKEN));
            
            String refreshToken = map.remove(OAuthConstants.REFRESH_TOKEN);
            if (refreshToken != null) {
                token.setRefreshToken(refreshToken);
            }
            String expiresInStr = map.remove(OAuthConstants.ACCESS_TOKEN_EXPIRES_IN);
            if (expiresInStr != null) {
                token.setExpiresIn(Long.valueOf(expiresInStr));
            }
            String scope = map.remove(OAuthConstants.SCOPE);
            if (scope != null) {
                token.setApprovedScope(scope);
            }
            
            token.setParameters(map);
            return token;
        } else {
            return null;
        }
    }
    
    /**
     * Creates OAuth Authorization header for accessing the end user's resources
     * @param consumer represents the registered client
     * @param accessToken the access token  
     * @return the header value
     */
    public static String createAuthorizationHeader(Consumer consumer,
                                                   ClientAccessToken accessToken)
        throws OAuthServiceException {
        StringBuilder sb = new StringBuilder();
        appendTokenData(sb, accessToken);  
        return sb.toString();
    }
    

    private static void appendTokenData(StringBuilder sb, ClientAccessToken token) 
        throws OAuthServiceException {
        // this should all be handled by token specific serializers
        if (OAuthConstants.BEARER_TOKEN_TYPE.equals(token.getTokenType())) {
            sb.append(OAuthConstants.BEARER_AUTHORIZATION_SCHEME);
            sb.append(" ");
            sb.append(token.getTokenKey());
        } else {
            // deal with MAC and other tokens
            throw new OAuthServiceException("Unsupported token type");
        }
        
    }
    
    /**
     * Simple consumer representation
     */
    public static class Consumer {
        
        private String key;
        private String secret;
        
        public Consumer(String key, String secret) {
            this.key = key;
            this.secret = secret;
        }
        public String getKey() {
            return key;
        }
    
        public String getSecret() {
            return secret;
        }
        
        
    }
}
