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

import java.io.IOException;
import java.security.Principal;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
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
import org.apache.cxf.sts.token.provider.jwt.DefaultJWTClaimsProvider;
import org.apache.cxf.sts.token.provider.jwt.JWTClaimsProvider;
import org.apache.cxf.sts.token.provider.jwt.JWTClaimsProviderParameters;
import org.apache.cxf.sts.token.provider.jwt.JWTTokenProvider;
import org.apache.cxf.sts.token.validator.jwt.DefaultJWTRoleParser;
import org.apache.cxf.sts.token.validator.jwt.JWTTokenValidator;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for validating JWTTokens.
 */
public class JWTTokenValidatorTest {
    private static TokenStore tokenStore;

    @BeforeClass
    public static void init() throws TokenStoreException {
        tokenStore = new DefaultInMemoryTokenStore();
    }

    @org.junit.Test
    public void testCreateAndValidateSignedJWT() throws Exception {
        // Create
        TokenProvider jwtTokenProvider = new JWTTokenProvider();
        ((JWTTokenProvider)jwtTokenProvider).setSignToken(true);

        TokenProviderParameters providerParameters = createProviderParameters();

        assertTrue(jwtTokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE));
        TokenProviderResponse providerResponse = jwtTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        String token = (String)providerResponse.getToken();
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);

        // Validate the token
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

        Principal principal = validatorResponse.getPrincipal();
        assertTrue(principal != null && principal.getName() != null);
    }

    @org.junit.Test
    public void testInvalidSignature() throws Exception {
        // Create
        TokenProvider jwtTokenProvider = new JWTTokenProvider();
        ((JWTTokenProvider)jwtTokenProvider).setSignToken(true);

        TokenProviderParameters providerParameters = createProviderParameters();
        Crypto crypto = CryptoFactory.getInstance(getEveCryptoProperties());
        CallbackHandler callbackHandler = new EveCallbackHandler();
        providerParameters.getStsProperties().setSignatureCrypto(crypto);
        providerParameters.getStsProperties().setCallbackHandler(callbackHandler);
        providerParameters.getStsProperties().setSignatureUsername("eve");

        assertTrue(jwtTokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE));
        TokenProviderResponse providerResponse = jwtTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        String token = (String)providerResponse.getToken();
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);

        // Validate the token
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
        assertTrue(validatorResponse.getToken().getState() == STATE.INVALID);
    }

    @org.junit.Test
    public void testUnsignedToken() throws Exception {
        // Create
        TokenProvider jwtTokenProvider = new JWTTokenProvider();
        ((JWTTokenProvider)jwtTokenProvider).setSignToken(false);

        TokenProviderParameters providerParameters = createProviderParameters();
        Crypto crypto = CryptoFactory.getInstance(getEveCryptoProperties());
        CallbackHandler callbackHandler = new EveCallbackHandler();
        providerParameters.getStsProperties().setSignatureCrypto(crypto);
        providerParameters.getStsProperties().setCallbackHandler(callbackHandler);
        providerParameters.getStsProperties().setSignatureUsername("eve");

        assertTrue(jwtTokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE));
        TokenProviderResponse providerResponse = jwtTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        String token = (String)providerResponse.getToken();
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 2);

        // Validate the token
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
        assertTrue(validatorResponse.getToken().getState() == STATE.INVALID);
    }

    @org.junit.Test
    public void testInvalidConditionJWT() throws Exception {
        // Create
        TokenProvider jwtTokenProvider = new JWTTokenProvider();
        ((JWTTokenProvider)jwtTokenProvider).setSignToken(true);

        DefaultJWTClaimsProvider jwtClaimsProvider = new DefaultJWTClaimsProvider();
        jwtClaimsProvider.setLifetime(1L);
        ((JWTTokenProvider)jwtTokenProvider).setJwtClaimsProvider(jwtClaimsProvider);

        TokenProviderParameters providerParameters = createProviderParameters();

        assertTrue(jwtTokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE));
        TokenProviderResponse providerResponse = jwtTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        String token = (String)providerResponse.getToken();
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);

        Thread.sleep(1500L);

        // Validate the token
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
        assertTrue(validatorResponse.getToken().getState() == STATE.INVALID);
    }

    @org.junit.Test
    public void testChangedSignature() throws Exception {
        // Create
        TokenProvider jwtTokenProvider = new JWTTokenProvider();
        ((JWTTokenProvider)jwtTokenProvider).setSignToken(true);

        DefaultJWTClaimsProvider jwtClaimsProvider = new DefaultJWTClaimsProvider();
        jwtClaimsProvider.setLifetime(1L);
        ((JWTTokenProvider)jwtTokenProvider).setJwtClaimsProvider(jwtClaimsProvider);

        TokenProviderParameters providerParameters = createProviderParameters();

        assertTrue(jwtTokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE));
        TokenProviderResponse providerResponse = jwtTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        String token = (String)providerResponse.getToken();
        // Change the signature
        token += "blah";
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);

        Thread.sleep(1500L);

        // Validate the token
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
        assertTrue(validatorResponse.getToken().getState() == STATE.INVALID);
    }

    @org.junit.Test
    public void testJWTWithRoles() throws Exception {
        // Create
        TokenProvider jwtTokenProvider = new JWTTokenProvider();
        ((JWTTokenProvider)jwtTokenProvider).setSignToken(true);

        JWTClaimsProvider claimsProvider = new RoleJWTClaimsProvider("manager");
        ((JWTTokenProvider)jwtTokenProvider).setJwtClaimsProvider(claimsProvider);

        TokenProviderParameters providerParameters = createProviderParameters();

        assertTrue(jwtTokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE));
        TokenProviderResponse providerResponse = jwtTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        String token = (String)providerResponse.getToken();
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);

        // Validate the token
        TokenValidator jwtTokenValidator = new JWTTokenValidator();
        // Set the role
        DefaultJWTRoleParser roleParser = new DefaultJWTRoleParser();
        roleParser.setRoleClaim("role");
        ((JWTTokenValidator)jwtTokenValidator).setRoleParser(roleParser);
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

        Principal principal = validatorResponse.getPrincipal();
        assertTrue(principal != null && principal.getName() != null);
        Set<Principal> roles = validatorResponse.getRoles();
        assertTrue(roles != null && !roles.isEmpty());
        assertEquals("manager", roles.iterator().next().getName());
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

    private Properties getEveCryptoProperties() {
        Properties properties = new Properties();
        properties.put(
            "org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin"
        );
        properties.put("org.apache.wss4j.crypto.merlin.keystore.password", "evespass");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "eve.jks");

        return properties;
    }

    public class EveCallbackHandler implements CallbackHandler {

        public void handle(Callback[] callbacks) throws IOException,
                UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof WSPasswordCallback) { // CXF
                    WSPasswordCallback pc = (WSPasswordCallback) callbacks[i];
                    if ("eve".equals(pc.getIdentifier())) {
                        pc.setPassword("evekpass");
                        break;
                    }
                }
            }
        }
    }

    private Element createTokenWrapper(String token) {
        Document doc = DOMUtils.getEmptyDocument();
        Element tokenWrapper = doc.createElementNS(null, "TokenWrapper");
        tokenWrapper.setTextContent(token);
        return tokenWrapper;
    }

    private static class RoleJWTClaimsProvider extends DefaultJWTClaimsProvider {

        private String role;

        RoleJWTClaimsProvider(String role) {
            this.role = role;
        }

        @Override
        public JwtClaims getJwtClaims(JWTClaimsProviderParameters jwtClaimsProviderParameters) {
            JwtClaims claims = super.getJwtClaims(jwtClaimsProviderParameters);
            claims.setProperty("role", role);
            return claims;
        }
    }
}
