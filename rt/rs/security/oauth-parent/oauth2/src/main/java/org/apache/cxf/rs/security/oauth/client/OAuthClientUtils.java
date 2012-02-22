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
package org.apache.cxf.rs.security.oauth.client;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.client.ClientWebApplicationException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.form.Form;
import org.apache.cxf.rs.security.oauth.common.AccessTokenGrant;
import org.apache.cxf.rs.security.oauth.common.AccessTokenType;
import org.apache.cxf.rs.security.oauth.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth.provider.OAuthServiceException;

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
            ub.queryParam("redirect_uri", redirectUri);
        }
        if (state != null) {
            ub.queryParam("state", state);
        }
        return ub.build();
    }
    
    public static UriBuilder getAuthorizationURIBuilder(String authorizationServiceURI, 
                                                 String clientId,
                                                 String scope) {
        UriBuilder ub = UriBuilder.fromUri(authorizationServiceURI);
        if (clientId != null) {
            ub.queryParam("client_id", clientId);
        }
        if (scope != null) {
            ub.queryParam("scope", scope);
        }
        return ub;                                   
    }
    
    public static ClientAccessToken getAccessToken(WebClient accessTokenService,
                                                   Consumer consumer,
                                                   AccessTokenGrant grant) throws OAuthServiceException {
        
        return getAccessToken(accessTokenService, consumer, grant, true);
    }
    
    public static ClientAccessToken getAccessToken(WebClient accessTokenService,
                                                   Consumer consumer,
                                                   AccessTokenGrant grant,
                                                   boolean setAuthorizationHeader) 
        throws OAuthServiceException {
        
        StringBuilder sb = new StringBuilder();
        sb.append("Basic ");
        try {
            String data = consumer.getKey() + ":" + consumer.getSecret();
            sb.append(Base64Utility.encode(data.getBytes("UTF-8")));
        } catch (Exception ex) {
            throw new ClientWebApplicationException(ex);
        }
        accessTokenService.header("Authorization", sb.toString());
        
        Form form = new Form(grant.toMap());
        accessTokenService.accept("application/json");
        return accessTokenService.post(form, ClientAccessToken.class);
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
