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
package org.apache.cxf.sts.token.validator;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.cache.DefaultInMemoryTokenStore;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.provider.jwt.JWTTokenProvider;
import org.apache.cxf.sts.token.realm.JWTRealmCodec;
import org.apache.cxf.sts.token.realm.RealmProperties;
import org.apache.cxf.sts.token.validator.jwt.JWTTokenValidator;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for validating JWTTokens in different realms
 */
public class JWTTokenValidatorRealmTest {
    private static TokenStore tokenStore;

    @BeforeClass
    public static void init() throws TokenStoreException {
        tokenStore = new DefaultInMemoryTokenStore();
    }

    @org.junit.Test
    public void testRealmA() throws Exception {
        // Create
        TokenProvider jwtTokenProvider = new JWTTokenProvider();
        ((JWTTokenProvider)jwtTokenProvider).setSignToken(true);
        ((JWTTokenProvider)jwtTokenProvider).setRealmMap(getRealms());

        TokenProviderParameters providerParameters = createProviderParameters();
        providerParameters.setRealm("A");

        assertTrue(jwtTokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE));
        TokenProviderResponse providerResponse = jwtTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        String token = (String)providerResponse.getToken();
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);

        // Validate the token - no realm is returned
        TokenValidator jwtTokenValidator = new JWTTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of a JWT Token
        ReceivedToken validateTarget = new ReceivedToken(createTokenWrapper(token));
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(jwtTokenValidator.canHandleToken(validateTarget));

        TokenValidatorResponse validatorResponse =
            jwtTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);
        assertNull(validatorResponse.getTokenRealm());

        // Now set the JWTRealmCodec implementation on the Validator
        ((JWTTokenValidator)jwtTokenValidator).setRealmCodec(new IssuerJWTRealmCodec());

        validatorResponse = jwtTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);
        assertEquals("A", validatorResponse.getTokenRealm());

        Principal principal = validatorResponse.getPrincipal();
        assertTrue(principal != null && principal.getName() != null);
    }

    @org.junit.Test
    public void testRealmB() throws Exception {
        // Create
        TokenProvider jwtTokenProvider = new JWTTokenProvider();
        ((JWTTokenProvider)jwtTokenProvider).setSignToken(true);
        ((JWTTokenProvider)jwtTokenProvider).setRealmMap(getRealms());

        TokenProviderParameters providerParameters = createProviderParameters();
        providerParameters.setRealm("B");

        assertTrue(jwtTokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE));
        TokenProviderResponse providerResponse = jwtTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        String token = (String)providerResponse.getToken();
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);

        // Validate the token - no realm is returned
        TokenValidator jwtTokenValidator = new JWTTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of a JWT Token
        ReceivedToken validateTarget = new ReceivedToken(createTokenWrapper(token));
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(jwtTokenValidator.canHandleToken(validateTarget));

        TokenValidatorResponse validatorResponse =
            jwtTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);
        assertNull(validatorResponse.getTokenRealm());

        // Now set the JWTRealmCodec implementation on the Validator
        ((JWTTokenValidator)jwtTokenValidator).setRealmCodec(new IssuerJWTRealmCodec());

        validatorResponse = jwtTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);
        assertEquals("B", validatorResponse.getTokenRealm());

        Principal principal = validatorResponse.getPrincipal();
        assertTrue(principal != null && principal.getName() != null);
    }

    private Map<String, RealmProperties> getRealms() {
        // Create Realms
        Map<String, RealmProperties> realms = new HashMap<>();
        RealmProperties realm = new RealmProperties();
        realm.setIssuer("A-Issuer");
        realms.put("A", realm);
        realm = new RealmProperties();
        realm.setIssuer("B-Issuer");
        realms.put("B", realm);
        return realms;
    }

    private TokenProviderParameters createProviderParameters() throws WSSecurityException {
        TokenProviderParameters parameters = new TokenProviderParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(JWTTokenProvider.JWT_TOKEN_TYPE);
        parameters.setTokenRequirements(tokenRequirements);

        KeyRequirements keyRequirements = new KeyRequirements();
        parameters.setKeyRequirements(keyRequirements);

        parameters.setTokenStore(tokenStore);

        parameters.setPrincipal(new CustomTokenPrincipal("alice"));
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        parameters.setMessageContext(msgCtx);

        parameters.setAppliesToAddress("http://dummy-service.com/dummy");

        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        parameters.setStsProperties(stsProperties);

        parameters.setEncryptionProperties(new EncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());

        return parameters;
    }

    private TokenValidatorParameters createValidatorParameters() throws WSSecurityException {
        TokenValidatorParameters parameters = new TokenValidatorParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(STSConstants.STATUS);
        parameters.setTokenRequirements(tokenRequirements);

        KeyRequirements keyRequirements = new KeyRequirements();
        parameters.setKeyRequirements(keyRequirements);

        parameters.setPrincipal(new CustomTokenPrincipal("alice"));
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        parameters.setMessageContext(msgCtx);

        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        parameters.setStsProperties(stsProperties);
        parameters.setTokenStore(tokenStore);

        return parameters;
    }

    private Properties getEncryptionProperties() {
        Properties properties = new Properties();
        properties.put(
            "org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin"
        );
        properties.put("org.apache.wss4j.crypto.merlin.keystore.password", "stsspass");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "keys/stsstore.jks");

        return properties;
    }

    private Element createTokenWrapper(String token) {
        Document doc = DOMUtils.getEmptyDocument();
        Element tokenWrapper = doc.createElementNS(null, "TokenWrapper");
        tokenWrapper.setTextContent(token);
        return tokenWrapper;
    }

    /**
     * This class returns a realm associated with a JWTToken depending on the issuer.
     */
    private static final class IssuerJWTRealmCodec implements JWTRealmCodec {

        public String getRealmFromToken(JwtToken token) {
            if ("A-Issuer".equals(token.getClaim(JwtConstants.CLAIM_ISSUER))) {
                return "A";
            } else if ("B-Issuer".equals(token.getClaim(JwtConstants.CLAIM_ISSUER))) {
                return "B";
            }
            return null;
        }

    }
}
