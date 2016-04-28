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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.apache.cxf.rs.security.jose.jaxrs.JsonWebKeysProvider;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.rs.security.oauth2.provider.OAuthJSONProvider;
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

        client.path(parameters.getPath());
        Response response = client.get();

        OAuthAuthorizationData authzData = response.readEntity(OAuthAuthorizationData.class);

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
        form.param("response_type", authzData.getResponseType());
        form.param("oauthDecision", "allow");

        response = client.post(form);
        String location = response.getHeaderString("Location");
        if (parameters.getState() != null) {
            Assert.assertTrue(location.contains("state=" + parameters.getState()));
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
        client.type("application/x-www-form-urlencoded").accept("application/json");
        client.path("token");

        Form form = new Form();
        form.param("grant_type", "authorization_code");
        form.param("code", code);
        form.param("client_id", consumerId);
        if (audience != null) {
            form.param("audience", audience);
        }
        Response response = client.post(form);

        return response.readEntity(ClientAccessToken.class);
    }
    
    public static List<Object> setupProviders() {
        List<Object> providers = new ArrayList<Object>();
        JSONProvider<OAuthAuthorizationData> jsonP = new JSONProvider<OAuthAuthorizationData>();
        jsonP.setNamespaceMap(Collections.singletonMap("http://org.apache.cxf.rs.security.oauth",
                                                       "ns2"));
        providers.add(jsonP);
        providers.add(new OAuthJSONProvider());
        providers.add(new JsonWebKeysProvider());
        
        return providers;
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
        claims.setIssuedAt(new Date().getTime() / 1000L);
        if (expiry) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, 60);
            claims.setExpiryTime(cal.getTimeInMillis() / 1000L);
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
            signingProperties.put("rs.security.keystore.file", 
                                  "org/apache/cxf/systest/jaxrs/security/certs/alice.jks");
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
        String foundString = 
            parentString.substring(parentString.indexOf(substringName + "=") + (substringName + "=").length());
        int ampersandIndex = foundString.indexOf('&');
        if (ampersandIndex < 1) {
            ampersandIndex = foundString.length();
        }
        return foundString.substring(0, ampersandIndex);
    }
    
    public static class AuthorizationCodeParameters {
        private String scope;
        private String consumerId;
        private String nonce;
        private String state;
        private String responseType;
        private String path; 
        private String request;
        
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
    }
}