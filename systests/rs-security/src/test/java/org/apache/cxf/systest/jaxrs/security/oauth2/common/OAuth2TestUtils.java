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
package org.apache.cxf.systest.jaxrs.security.oauth2.common;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.apache.cxf.jaxrs.provider.json.JsonMapObjectProvider;
import org.apache.cxf.rs.security.jose.jaxrs.JsonWebKeysProvider;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.oauth2.client.Consumer;
import org.apache.cxf.rs.security.oauth2.client.OAuthClientUtils;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeGrant;
import org.apache.cxf.rs.security.oauth2.provider.OAuthJSONProvider;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.builder.SAML1Constants;

import org.junit.Assert;

/**
 * Some test utils for the OAuth 2.0 tests
 */
public final class OAuth2TestUtils {

    private OAuth2TestUtils() {
        // complete
    }

    public static String getAuthorizationCode(WebClient client) {
        return getAuthorizationCode(client, null);
    }

    public static String getAuthorizationCode(WebClient client, String scope) {
        return getAuthorizationCode(client, scope, "consumer-id");
    }

    public static String getAuthorizationCode(WebClient client, String scope, String consumerId) {
        return getAuthorizationCode(client, scope, consumerId, null, null);
    }

    public static String getAuthorizationCode(WebClient client, String scope, String consumerId,
                                              String nonce, String state) {
        AuthorizationCodeParameters parameters = new AuthorizationCodeParameters();
        parameters.setConsumerId(consumerId);
        parameters.setScope(scope);
        parameters.setNonce(nonce);
        parameters.setState(state);
        parameters.setResponseType("code");
        parameters.setPath("authorize/");
        String location = getLocation(client, parameters);
        return getSubstring(location, "code");
    }

    public static String getLocation(WebClient client, AuthorizationCodeParameters parameters) {
        // Make initial authorization request
        client.type("application/json").accept("application/json");
        client.query("client_id", parameters.getConsumerId());
        client.query("redirect_uri", "http://www.blah.apache.org");
        client.query("response_type", parameters.getResponseType());
        if (parameters.getScope() != null) {
            client.query("scope", parameters.getScope());
        }
        if (parameters.getNonce() != null) {
            client.query("nonce", parameters.getNonce());
        }
        if (parameters.getState() != null) {
            client.query("state", parameters.getState());
        }
        if (parameters.getRequest() != null) {
            client.query("request", parameters.getRequest());
        }
        if (parameters.getCodeChallenge() != null) {
            client.query("code_challenge", parameters.getCodeChallenge());
        }
        if (parameters.getCodeChallengeMethod() != null) {
            client.query("code_challenge_method", parameters.getCodeChallengeMethod());
        }

        client.path(parameters.getPath());
        Response response = client.get();

        OAuthAuthorizationData authzData = response.readEntity(OAuthAuthorizationData.class);
        return getLocation(client, authzData, parameters.getState());
    }

    public static String getLocation(WebClient client, OAuthAuthorizationData authzData, String state) {

        // Now call "decision" to get the authorization code grant
        client.path("decision");
        client.type("application/x-www-form-urlencoded");

        Form form = new Form();
        form.param("session_authenticity_token", authzData.getAuthenticityToken());
        form.param("client_id", authzData.getClientId());
        form.param("redirect_uri", authzData.getRedirectUri());
        if (authzData.getNonce() != null) {
            form.param("nonce", authzData.getNonce());
        }
        if (authzData.getProposedScope() != null) {
            form.param("scope", authzData.getProposedScope());
        }
        if (authzData.getState() != null) {
            form.param("state", authzData.getState());
        }
        if (authzData.getClientCodeChallenge() != null) {
            form.param("code_challenge", authzData.getClientCodeChallenge());
        }
        if (authzData.getClientCodeChallengeMethod() != null) {
            form.param("code_challenge_method", authzData.getClientCodeChallengeMethod());
        }
        form.param("response_type", authzData.getResponseType());
        form.param("oauthDecision", "allow");

        Response response = client.post(form);
        String location = response.getHeaderString("Location");
        if (state != null) {
            Assert.assertTrue(location.contains("state=" + state));
        }

        return location;
    }

    public static ClientAccessToken getAccessTokenWithAuthorizationCode(WebClient client, String code) {
        return getAccessTokenWithAuthorizationCode(client, code, "consumer-id", null);
    }

    public static ClientAccessToken getAccessTokenWithAuthorizationCode(WebClient client,
                                                                        String code,
                                                                        String consumerId,
                                                                        String audience) {
        return getAccessTokenWithAuthorizationCode(client, code, consumerId, audience, null);
    }

    public static ClientAccessToken getAccessTokenWithAuthorizationCode(WebClient client,
                                                                        String code,
                                                                        String consumerId,
                                                                        String audience,
                                                                        String codeVerifier) {
        Map<String, String> extraParams = new HashMap<>(3);
        extraParams.put(OAuthConstants.REDIRECT_URI, "http://www.blah.apache.org");
        if (audience != null) {
            extraParams.put(OAuthConstants.CLIENT_AUDIENCE, audience);
        }
        if (codeVerifier != null) {
            extraParams.put(OAuthConstants.AUTHORIZATION_CODE_VERIFIER, codeVerifier);
        }
        return OAuthClientUtils.getAccessToken(
            client.path("token"),
            new Consumer(consumerId),
            new AuthorizationCodeGrant(code),
            extraParams,
            false);
    }

    public static List<Object> setupProviders() {
        JSONProvider<OAuthAuthorizationData> jsonP = new JSONProvider<>();
        jsonP.setNamespaceMap(Collections.singletonMap("http://org.apache.cxf.rs.security.oauth",
                                                       "ns2"));

        return Arrays.asList(jsonP, new OAuthJSONProvider(), new JsonWebKeysProvider(), new JsonMapObjectProvider());
    }

    public static String createToken(String audRestr) throws WSSecurityException {
        return createToken(audRestr, true, true);
    }

    public static String createToken(String audRestr, boolean saml2, boolean sign)
        throws WSSecurityException {
        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler(sign);
        samlCallbackHandler.setAudience(audRestr);
        if (!saml2) {
            samlCallbackHandler.setSaml2(false);
            samlCallbackHandler.setConfirmationMethod(SAML1Constants.CONF_BEARER);
        }

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(samlCallbackHandler, samlCallback);

        SamlAssertionWrapper samlAssertion = new SamlAssertionWrapper(samlCallback);
        if (samlCallback.isSignAssertion()) {
            samlAssertion.signAssertion(
                samlCallback.getIssuerKeyName(),
                samlCallback.getIssuerKeyPassword(),
                samlCallback.getIssuerCrypto(),
                samlCallback.isSendKeyValue(),
                samlCallback.getCanonicalizationAlgorithm(),
                samlCallback.getSignatureAlgorithm()
            );
        }

        return samlAssertion.assertionToString();
    }

    public static String createToken(String issuer, String subject, String audience,
                               boolean expiry, boolean sign) {
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject(subject);
        if (issuer != null) {
            claims.setIssuer(issuer);
        }
        claims.setIssuedAt(OAuthUtils.getIssuedAt());
        if (expiry) {
            claims.setExpiryTime(claims.getIssuedAt() + 60L);
        }
        if (audience != null) {
            claims.setAudiences(Collections.singletonList(audience));
        }

        if (sign) {
            // Sign the JWT Token
            Properties signingProperties = new Properties();
            signingProperties.put("rs.security.keystore.type", "jks");
            signingProperties.put("rs.security.keystore.password", "password");
            signingProperties.put("rs.security.keystore.alias", "alice");
            signingProperties.put("rs.security.keystore.file", "keys/alice.jks");
            signingProperties.put("rs.security.key.password", "password");
            signingProperties.put("rs.security.signature.algorithm", "RS256");

            JwsHeaders jwsHeaders = new JwsHeaders(signingProperties);
            JwsJwtCompactProducer jws = new JwsJwtCompactProducer(jwsHeaders, claims);

            JwsSignatureProvider sigProvider =
                JwsUtils.loadSignatureProvider(signingProperties, jwsHeaders);

            return jws.signWith(sigProvider);
        }

        JwsHeaders jwsHeaders = new JwsHeaders(SignatureAlgorithm.NONE);
        JwsJwtCompactProducer jws = new JwsJwtCompactProducer(jwsHeaders, claims);
        return jws.getSignedEncodedJws();
    }

    public static String getSubstring(String parentString, String substringName) {
        if (!parentString.contains(substringName)) {
            return null;
        }
        String foundString =
            parentString.substring(parentString.indexOf(substringName + "=") + (substringName + "=").length());
        int ampersandIndex = foundString.indexOf('&');
        if (ampersandIndex < 1) {
            ampersandIndex = foundString.length();
        }
        return foundString.substring(0, ampersandIndex);
    }

    public static HTTPConduitConfigurer clientHTTPConduitConfigurer() throws IOException, GeneralSecurityException {
        final TLSClientParameters tlsCP = new TLSClientParameters();
        tlsCP.setDisableCNCheck(true);

        try (InputStream is = OAuth2TestUtils.class.getResourceAsStream("/keys/Morpit.jks")) {
            final KeyStore keyStore = CryptoUtils.loadKeyStore(is, "password".toCharArray(), null);
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, "password".toCharArray());
            tlsCP.setKeyManagers(kmf.getKeyManagers());
        }

        try (InputStream is = OAuth2TestUtils.class.getResourceAsStream("/keys/Truststore.jks")) {
            final KeyStore keyStore = CryptoUtils.loadKeyStore(is, "password".toCharArray(), null);
            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            tlsCP.setTrustManagers(tmf.getTrustManagers());
        }

        return new HTTPConduitConfigurer() {
            public void configure(String name, String address, HTTPConduit c) {
                c.setTlsClientParameters(tlsCP);
                // 5 mins for long debug session
//                org.apache.cxf.transports.http.configuration.HTTPClientPolicy httpClientPolicy =
//                    new org.apache.cxf.transports.http.configuration.HTTPClientPolicy();
//                httpClientPolicy.setConnectionTimeout(300000L);
//                httpClientPolicy.setReceiveTimeout(300000L);
//                c.setClient(httpClientPolicy);
            }
        };
    }

    public static class AuthorizationCodeParameters {
        private String scope;
        private String consumerId;
        private String nonce;
        private String state;
        private String responseType;
        private String path;
        private String request;
        private String codeChallenge;
        private String codeChallengeMethod;

        public String getScope() {
            return scope;
        }
        public void setScope(String scope) {
            this.scope = scope;
        }
        public String getConsumerId() {
            return consumerId;
        }
        public void setConsumerId(String consumerId) {
            this.consumerId = consumerId;
        }
        public String getNonce() {
            return nonce;
        }
        public void setNonce(String nonce) {
            this.nonce = nonce;
        }
        public String getState() {
            return state;
        }
        public void setState(String state) {
            this.state = state;
        }
        public String getResponseType() {
            return responseType;
        }
        public void setResponseType(String responseType) {
            this.responseType = responseType;
        }
        public String getPath() {
            return path;
        }
        public void setPath(String path) {
            this.path = path;
        }
        public String getRequest() {
            return request;
        }
        public void setRequest(String request) {
            this.request = request;
        }
        public String getCodeChallenge() {
            return codeChallenge;
        }
        public void setCodeChallenge(String codeChallenge) {
            this.codeChallenge = codeChallenge;
        }
        public String getCodeChallengeMethod() {
            return codeChallengeMethod;
        }
        public void setCodeChallengeMethod(String codeChallengeMethod) {
            this.codeChallengeMethod = codeChallengeMethod;
        }
    }
}