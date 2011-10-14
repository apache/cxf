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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriBuilder;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.form.Form;

public final class OAuthClientSupport {
    private OAuthClientSupport() {
        
    }
    public static URI getAuthorizationServiceURI(String authorizationServiceURI, String token) {
        return UriBuilder.fromUri(authorizationServiceURI).
            queryParam("oauth_token", token).build();
                                           
    }
    
    public static Token getRequestToken(WebClient requestTokenService,
                             Consumer consumer,
                             URI callback,
                             Map<String, String> extraParams) {
        Map<String, String> parameters = new HashMap<String, String>();
        if (extraParams != null) {
            parameters.putAll(extraParams);
        }
        parameters.put(OAuth.OAUTH_CALLBACK, callback.toString());
        parameters.put(OAuth.OAUTH_SIGNATURE_METHOD, "HMAC-SHA1");
        parameters.put(OAuth.OAUTH_NONCE, UUID.randomUUID().toString());
        parameters.put(OAuth.OAUTH_TIMESTAMP, String.valueOf(System.currentTimeMillis() / 1000));
        parameters.put(OAuth.OAUTH_CONSUMER_KEY, consumer.getKey());
        
        OAuthConsumer oAuthConsumer = new OAuthConsumer(null, consumer.getKey(), consumer.getSecret(), 
                null);
        OAuthAccessor accessor = new OAuthAccessor(oAuthConsumer);
        return getToken(requestTokenService, accessor, parameters);
    }
    public static Token getAccessToken(WebClient accessTokenService,
                                       Consumer consumer,
                                       Token requestToken,
                                       String verifier) {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(OAuth.OAUTH_CONSUMER_KEY, consumer.getKey());
        parameters.put(OAuth.OAUTH_TOKEN, requestToken.getToken());
        parameters.put(OAuth.OAUTH_VERIFIER, verifier);
        parameters.put(OAuth.OAUTH_SIGNATURE_METHOD, "HMAC-SHA1");
        
        OAuthConsumer oAuthConsumer = new OAuthConsumer(null, consumer.getKey(), 
                consumer.getSecret(), null);
        OAuthAccessor accessor = new OAuthAccessor(oAuthConsumer);
        accessor.requestToken = requestToken.getToken();
        accessor.tokenSecret = requestToken.getSecret();
        return getToken(accessTokenService, accessor, parameters);
    }
    
    public static String createAuthorizationHeader(Consumer consumer,
                                            Token token, 
                                            String method, 
                                            String requestURI) {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(OAuth.OAUTH_CONSUMER_KEY, consumer.getKey());
        if (token != null) {
            parameters.put(OAuth.OAUTH_TOKEN, token.getToken());
        }
        parameters.put(OAuth.OAUTH_SIGNATURE_METHOD, "HMAC-SHA1");
        parameters.put(OAuth.OAUTH_NONCE, UUID.randomUUID().toString());
        parameters.put(OAuth.OAUTH_TIMESTAMP, String.valueOf(System.currentTimeMillis() / 1000));
        
        OAuthConsumer oAuthConsumer = 
            new OAuthConsumer(null, consumer.getKey(), consumer.getSecret(), null);
        OAuthAccessor accessor = new OAuthAccessor(oAuthConsumer);
        if (token != null) {
            accessor.accessToken = token.getToken();
            accessor.tokenSecret = token.getSecret();
        }
        return doGetAuthorizationHeader(accessor, method, requestURI, parameters);
    }
    
    private static String doGetAuthorizationHeader(OAuthAccessor accessor, 
            String method, String requestURI, Map<String, String> parameters) {
        try {
            OAuthMessage msg = accessor.newRequestMessage(method, requestURI, parameters.entrySet());
            return msg.getAuthorizationHeader(null);
        } catch (Exception ex) {
            throw new WebApplicationException(500);
        }
    }
    
    private static Token getToken(WebClient tokenService, OAuthAccessor accessor,
        Map<String, String> parameters) {
        try {
            String header = 
                doGetAuthorizationHeader(accessor, "POST", tokenService.getBaseURI().toString(),
                        parameters);
            tokenService.header("Authorization", header);
            Form form = tokenService.post(null, Form.class);
            return new Token(form.getData().getFirst("oauth_token"),
                    form.getData().getFirst("oauth_token_secret"));
        } catch (Exception ex) {
            throw new WebApplicationException(500);
        }
    }
    public static class Token {
        private String token;
        private String secret;
        
        public Token(String token, String secret) {
            this.token = token;
            this.secret = secret;
        }
        public String getToken() {
            return token;
        }
        
        public String getSecret() {
            return secret;
        }


    }
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
