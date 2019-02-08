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
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.UriBuilder;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.signature.RSA_SHA1;
import org.apache.cxf.jaxrs.client.WebClient;
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
    public static URI getAuthorizationURI(String authorizationServiceURI, String requestToken) {
        return UriBuilder.fromUri(authorizationServiceURI).
            queryParam("oauth_token", requestToken).build();

    }

    /**
     * Returns a simple representation of the Request token
     * @param requestTokenService initialized RequestToken service client
     * @param consumer Consumer bean containing the consumer key and secret
     * @param callback the callback URI where the request token verifier will
     *        be returned
     * @param extraParams additional parameters such as state, scope, etc
     * @return the token
     */
    public static Token getRequestToken(WebClient requestTokenService,
                             Consumer consumer,
                             URI callback,
                             Map<String, String> extraParams) throws OAuthServiceException {
        return getRequestToken(requestTokenService, consumer, callback, extraParams, null);
    }

    public static Map<String, Object> prepareOAuthRsaProperties(PrivateKey pk) {
        Map<String, Object> props = new HashMap<>();
        props.put(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.RSA_SHA1);
        props.put(RSA_SHA1.PRIVATE_KEY, pk);
        return props;
    }

    public static Token getRequestToken(WebClient requestTokenService,
                                        Consumer consumer,
                                        URI callback,
                                        Map<String, String> extraParams,
                                        Map<String, Object> oauthConsumerProps) throws OAuthServiceException {
        Map<String, String> parameters = new HashMap<>();
        if (extraParams != null) {
            parameters.putAll(extraParams);
        }
        parameters.put(OAuth.OAUTH_CALLBACK, callback.toString());

        if (oauthConsumerProps == null || !oauthConsumerProps.containsKey(OAuth.OAUTH_SIGNATURE_METHOD)) {
            parameters.put(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.HMAC_SHA1);
        }
        parameters.put(OAuth.OAUTH_NONCE, UUID.randomUUID().toString());
        parameters.put(OAuth.OAUTH_TIMESTAMP, String.valueOf(System.currentTimeMillis() / 1000));
        parameters.put(OAuth.OAUTH_CONSUMER_KEY, consumer.getKey());

        OAuthAccessor accessor = createAccessor(consumer, oauthConsumerProps);
        return getToken(requestTokenService, accessor, parameters);
    }
    private static OAuthAccessor createAccessor(Consumer consumer, Map<String, Object> props) {
        OAuthConsumer oAuthConsumer = new OAuthConsumer(null, consumer.getKey(), consumer.getSecret(),
                                                        null);
        if (props != null) {
            for (Map.Entry<String, Object> entry : props.entrySet()) {
                oAuthConsumer.setProperty(entry.getKey(), entry.getValue());
            }
        }
        return new OAuthAccessor(oAuthConsumer);
    }

    /**
     * Returns a simple representation of the Access token
     * @param accessTokenService initialized AccessToken service client
     * @param consumer Consumer bean containing the consumer key and secret
     * @param verifier the verifier/authorization key
     * @return the token
     */
    public static Token getAccessToken(WebClient accessTokenService,
                                       Consumer consumer,
                                       Token requestToken,
                                       String verifier) throws OAuthServiceException {
        return getAccessToken(accessTokenService, consumer, requestToken, verifier, null);
    }

    public static Token getAccessToken(WebClient accessTokenService,
                                       Consumer consumer,
                                       Token requestToken,
                                       String verifier,
                                       Map<String, Object> oauthConsumerProps) throws OAuthServiceException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(OAuth.OAUTH_CONSUMER_KEY, consumer.getKey());
        parameters.put(OAuth.OAUTH_TOKEN, requestToken.getToken());
        parameters.put(OAuth.OAUTH_VERIFIER, verifier);
        if (oauthConsumerProps == null || !oauthConsumerProps.containsKey(OAuth.OAUTH_SIGNATURE_METHOD)) {
            parameters.put(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.HMAC_SHA1);
        }

        OAuthAccessor accessor = createAccessor(consumer, oauthConsumerProps);
        accessor.requestToken = requestToken.getToken();
        accessor.tokenSecret = requestToken.getSecret();
        return getToken(accessTokenService, accessor, parameters);
    }

    /**
     * Creates OAuth Authorization header
     * @param consumer Consumer bean containing the consumer key and secret
     * @param token Access token representation
     * @param method HTTP method
     * @param requestURI request URI
     * @return the header value
     */
    public static String createAuthorizationHeader(Consumer consumer,
                                            Token accessToken,
                                            String method,
                                            String requestURI) {
        return createAuthorizationHeader(consumer, accessToken, method, requestURI, null);
    }

    public static String createAuthorizationHeader(Consumer consumer,
                                                   Token accessToken,
                                                   String method,
                                                   String requestURI,
                                                   Map<String, Object> oauthConsumerProps) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(OAuth.OAUTH_CONSUMER_KEY, consumer.getKey());
        if (accessToken != null) {
            parameters.put(OAuth.OAUTH_TOKEN, accessToken.getToken());
        }
        if (oauthConsumerProps == null || !oauthConsumerProps.containsKey(OAuth.OAUTH_SIGNATURE_METHOD)) {
            parameters.put(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.HMAC_SHA1);
        }
        parameters.put(OAuth.OAUTH_NONCE, UUID.randomUUID().toString());
        parameters.put(OAuth.OAUTH_TIMESTAMP, String.valueOf(System.currentTimeMillis() / 1000));

        OAuthAccessor accessor = createAccessor(consumer, oauthConsumerProps);
        if (accessToken != null) {
            accessor.accessToken = accessToken.getToken();
            accessor.tokenSecret = accessToken.getSecret();
        }
        return doGetAuthorizationHeader(accessor, method, requestURI, parameters);
    }


    /**
     * Creates OAuth Authorization header containing consumer key and secret values only
     * @param consumer Consumer bean containing the consumer key and secret
     * @return the header value
     */
    public static String createAuthorizationHeader(Consumer consumer) {
        StringBuilder sb = new StringBuilder();
        sb.append("OAuth ").append("oauth_consumer_key=").append(consumer.getKey())
          .append("oauth_consumer_secret=").append(consumer.getSecret());
        return sb.toString();

    }

    private static String doGetAuthorizationHeader(OAuthAccessor accessor,
            String method, String requestURI, Map<String, String> parameters) {
        try {
            OAuthMessage msg = accessor.newRequestMessage(method, requestURI, parameters.entrySet());
            StringBuilder sb = new StringBuilder();
            sb.append(msg.getAuthorizationHeader(null));
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                if (!entry.getKey().startsWith("oauth_")) {
                    sb.append(", ");
                    sb.append(OAuth.percentEncode(entry.getKey())).append("=\"");
                    sb.append(OAuth.percentEncode(entry.getValue())).append('"');
                }
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new ProcessingException(ex);
        }
    }

    private static Token getToken(WebClient tokenService, OAuthAccessor accessor,
        Map<String, String> parameters) throws OAuthServiceException {
        String header = doGetAuthorizationHeader(accessor,
                                                 "POST",
                                                 tokenService.getBaseURI().toString(),
                                                 parameters);
        try {
            tokenService.replaceHeader("Authorization", header);
            Form form = tokenService.post(null, Form.class);
            return new Token(form.asMap().getFirst("oauth_token"),
                    form.asMap().getFirst("oauth_token_secret"));
        } catch (WebApplicationException ex) {
            throw new OAuthServiceException(ex);
        }
    }

    /**
     * Simple token representation
     */
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
