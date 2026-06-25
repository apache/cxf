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
package org.apache.cxf.rs.security.oauth2.filters;

import java.lang.reflect.Field;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenValidation;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;


public class JwtAccessTokenValidatorTest {

    private static final String SIGNING_KEY = "AyM1SysPpbyDfgZld3umj1qzKObwVMkoq2QjvA6P5f8";
    private static final String DIFFERENT_SIGNING_KEY = "hJtXIZ2uSN5kbQfbtTNWbg6X5U0ZSyxP6oJ6H3f3j1k";

    @Test
    public void testValidateAccessTokenSignedAndSignatureVerified() {
        JwtAccessTokenValidator validator = new JwtAccessTokenValidator();
        validator.setJwsVerifier(new HmacJwsSignatureVerifier(SIGNING_KEY, SignatureAlgorithm.HS256));
        validator.setValidateAudience(false);

        String jwt = createSignedToken(SIGNING_KEY, "signed-client", 3600);
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();

        AccessTokenValidation result = validator.validateAccessToken(
            mock(MessageContext.class), "Bearer", jwt, params);

        assertNotNull(result);
        assertTrue(result.isInitialValidationSuccessful());
        assertEquals("signed-client", result.getClientId());
    }

    @Test
    public void testValidateAccessTokenSignedButSignatureValidationFails() {
        JwtAccessTokenValidator validator = new JwtAccessTokenValidator();
        validator.setJwsVerifier(new HmacJwsSignatureVerifier(DIFFERENT_SIGNING_KEY, SignatureAlgorithm.HS256));
        validator.setValidateAudience(false);

        String jwt = createSignedToken(SIGNING_KEY, "signed-client", 3600);
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();

        OAuthServiceException ex = assertThrows(OAuthServiceException.class, () ->
            validator.validateAccessToken(mock(MessageContext.class), "Bearer", jwt, params));

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("Invalid Signature"));
    }

    @Test
    public void testValidateAccessTokenExpired() {
        JwtAccessTokenValidator validator = new JwtAccessTokenValidator();
        validator.setJwsVerifier(new HmacJwsSignatureVerifier(SIGNING_KEY, SignatureAlgorithm.HS256));
        validator.setValidateAudience(false);

        String jwt = createSignedToken(SIGNING_KEY, "signed-client", -3600); // Expired 1 hour ago
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();

        OAuthServiceException ex = assertThrows(OAuthServiceException.class, () ->
            validator.validateAccessToken(mock(MessageContext.class), "Bearer", jwt, params));

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("expired"));
    }

    @Test
    public void testValidateAccessTokenNotBefore() {
        JwtAccessTokenValidator validator = new JwtAccessTokenValidator();
        validator.setJwsVerifier(new HmacJwsSignatureVerifier(SIGNING_KEY, SignatureAlgorithm.HS256));
        validator.setValidateAudience(false);

        // Not valid before 1 hour from now
        String jwt = createSignedToken(SIGNING_KEY, "signed-client", 3600, 3600, null);
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();

        OAuthServiceException ex = assertThrows(OAuthServiceException.class, () ->
            validator.validateAccessToken(mock(MessageContext.class), "Bearer", jwt, params));

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("cannot be accepted"));
    }

    @After
    public void clearCurrentMessage() throws Exception {
        setThreadLocalMessage(null);
    }

    @Test
    public void testValidAudience() throws Exception {
        JwtAccessTokenValidator validator = new JwtAccessTokenValidator();
        validator.setJwsVerifier(new HmacJwsSignatureVerifier(SIGNING_KEY, SignatureAlgorithm.HS256));

        String jwt = createSignedToken(SIGNING_KEY, "signed-client", 3600, 0, "valid-audience");
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();

        Message message = new MessageImpl();
        message.put(JwtConstants.EXPECTED_CLAIM_AUDIENCE, "valid-audience");
        setThreadLocalMessage(message);

        AccessTokenValidation result = validator.validateAccessToken(
            mock(MessageContext.class), "Bearer", jwt, params);

        assertNotNull(result);
        assertTrue(result.isInitialValidationSuccessful());
        assertEquals("signed-client", result.getClientId());
    }

    @Test
    public void testInvalidAudience() throws Exception {
        JwtAccessTokenValidator validator = new JwtAccessTokenValidator();
        validator.setJwsVerifier(new HmacJwsSignatureVerifier(SIGNING_KEY, SignatureAlgorithm.HS256));

        String jwt = createSignedToken(SIGNING_KEY, "signed-client", 3600, 0, "invalid-audience");
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();

        Message message = new MessageImpl();
        message.put(JwtConstants.EXPECTED_CLAIM_AUDIENCE, "valid-audience");
        setThreadLocalMessage(message);

        OAuthServiceException ex = assertThrows(OAuthServiceException.class, () ->
            validator.validateAccessToken(mock(MessageContext.class), "Bearer", jwt, params));

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("Invalid audience restriction"));
    }

    private static String createSignedToken(String key, String clientId, long expiresInSeconds) {
        return createSignedToken(key, clientId, expiresInSeconds, 0, null);
    }

    private static String createSignedToken(String key, String clientId, long expiresInSeconds,
                                           long notBeforeOffsetSeconds, String audience) {
        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuedAt(now);
        claims.setExpiryTime(now + expiresInSeconds);
        if (clientId != null) {
            claims.setClaim("client_id", clientId);
        }
        claims.setIssuer("SomeIssuer");
        claims.setSubject("SomeSubject");
        if (notBeforeOffsetSeconds != 0) {
            claims.setNotBefore(now + notBeforeOffsetSeconds);
        }
        if (audience != null) {
            claims.setAudience(audience);
        }

        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(claims);
        return producer.signWith(new HmacJwsSignatureProvider(key, SignatureAlgorithm.HS256));
    }

    private static void setThreadLocalMessage(Message message) throws Exception {
        Field f = PhaseInterceptorChain.class.getDeclaredField("CURRENT_MESSAGE");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Message> tl = (ThreadLocal<Message>) f.get(null);
        if (message == null) {
            tl.remove();
        } else {
            tl.set(message);
        }
    }
}
