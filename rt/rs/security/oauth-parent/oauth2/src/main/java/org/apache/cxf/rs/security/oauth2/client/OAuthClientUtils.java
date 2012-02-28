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
import org.apache.cxf.rs.security.oauth2.common.AccessTokenType;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.provider.OAuthJSONProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

/**
 * The utility class for simplifying making OAuth request and access token
 * requests as well as for creating Authorization OAuth headers
 */
public final class OAuthClientUtils {
    private OAuthClientUtils() {
        
    }
    
    /**
     * Returns URI of the authorization service with the query parameter containing 
     * the request token key 
     * @param authorizationServiceURI the service URI
     * @param requestToken the request token key
     * @return
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
    
    public static ClientAccessToken getAccessToken(WebClient accessTokenService,
                                                   Consumer consumer,
                                                   AccessTokenGrant grant) throws OAuthServiceException {
        
        return getAccessToken(accessTokenService, consumer, grant, true);
    }
    
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
    
    public static ClientAccessToken getAccessToken(WebClient accessTokenService,
                                                   Consumer consumer,
                                                   AccessTokenGrant grant,
                                                   boolean setAuthorizationHeader) 
        throws OAuthServiceException {
        
        Form form = new Form(grant.toMap());
        
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
        Response response = accessTokenService.form(form);
        Map<String, String> map = null;
        try {
            map = new OAuthJSONProvider().readJSONResponse((InputStream)response.getEntity());
        } catch (IOException ex) {
            throw new ClientWebApplicationException(ex);
        }
        if (200 == response.getStatus()) {
            if (map.containsKey(OAuthConstants.ACCESS_TOKEN)
                && map.containsKey(OAuthConstants.ACCESS_TOKEN_TYPE)) {
                String type = map.get(OAuthConstants.ACCESS_TOKEN_TYPE);
                
                ClientAccessToken token = new ClientAccessToken(
                                              AccessTokenType.fromString(type),
                                              map.get(OAuthConstants.ACCESS_TOKEN));
                return token;
            } else {
                throw new OAuthServiceException(OAuthConstants.SERVER_ERROR);
            }
        } else if (400 == response.getStatus() && map.containsValue(OAuthConstants.ERROR_KEY)) {
            OAuthError error = new OAuthError(map.get(OAuthConstants.ERROR_KEY),
                                              map.get(OAuthConstants.ERROR_DESCRIPTION_KEY));
            throw new OAuthServiceException(error);
        } 
        throw new OAuthServiceException(OAuthConstants.SERVER_ERROR);
    }
    
    /**
     * Creates OAuth Authorization header
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
        AccessTokenType type = token.getTokenType();
        if (type == AccessTokenType.BEARER) {
            sb.append("Bearer");
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
