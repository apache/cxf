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
import java.util.Arrays;
import java.util.Collections;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;

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
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthContext;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

public class OAuthRequestFilterTest {

    private static final String SIGNING_KEY = "AyM1SysPpbyDfgZld3umj1qzKObwVMkoq2QjvA6P5f8";

    @After
    public void clearCurrentMessage() throws Exception {
        setThreadLocalMessage(null);
    }

    @Test
    public void testValidateAudiencesMatchesSubPathInNonExactMode() throws Exception {
        OAuthRequestFilter filter = new OAuthRequestFilter();
        filter.setAudienceIsEndpointAddress(true);
        filter.setCompleteAudienceMatch(false);

        Message message = new MessageImpl();
        message.put(Message.REQUEST_URL, "/api/read/item");
        setThreadLocalMessage(message);

        String result = filter.validateAudiences(Collections.singletonList("/api/read"));
        assertEquals("/api/read", result);
    }

    @Test
    public void testValidateAudiencesRejectsSiblingPrefixInNonExactMode() throws Exception {
        OAuthRequestFilter filter = new OAuthRequestFilter();
        filter.setAudienceIsEndpointAddress(true);
        filter.setCompleteAudienceMatch(false);

        Message message = new MessageImpl();
        message.put(Message.REQUEST_URL, "/api/readadmin");
        setThreadLocalMessage(message);

        assertThrows(NotAuthorizedException.class,
            () -> filter.validateAudiences(Collections.singletonList("/api/read")));
    }

    @Test
    public void testValidateAudiencesRequiresExactMatchWhenConfigured() throws Exception {
        OAuthRequestFilter filter = new OAuthRequestFilter();
        filter.setAudienceIsEndpointAddress(true);
        filter.setCompleteAudienceMatch(true);

        Message message = new MessageImpl();
        message.put(Message.REQUEST_URL, "/api/read/item");
        setThreadLocalMessage(message);

        assertThrows(NotAuthorizedException.class,
            () -> filter.validateAudiences(Collections.singletonList("/api/read")));
    }

    @Test
    public void testValidateAudiencesSkipsEndpointCheckWhenDisabled() {
        OAuthRequestFilter filter = new OAuthRequestFilter();
        filter.setAudienceIsEndpointAddress(false);

        String result = filter.validateAudiences(Collections.singletonList("/api/read"));
        assertNull(result);
    }

    @Test
    public void testValidateAudiencesMatchesQueryBoundaryInNonExactMode() throws Exception {
        OAuthRequestFilter filter = new OAuthRequestFilter();
        filter.setAudienceIsEndpointAddress(true);
        filter.setCompleteAudienceMatch(false);

        Message message = new MessageImpl();
        message.put(Message.REQUEST_URL, "/api/read?include=details");
        setThreadLocalMessage(message);

        String result = filter.validateAudiences(Collections.singletonList("/api/read"));
        assertEquals("/api/read", result);
    }

    @Test
    public void testValidateAudiencesRequiresEndpointCheckEnabledForAudienceMatch() throws Exception {
        OAuthRequestFilter filter = new OAuthRequestFilter();
        filter.setCompleteAudienceMatch(false);

        Message message = new MessageImpl();
        message.put(Message.REQUEST_URL, "/api/read/item");
        setThreadLocalMessage(message);

        // Enabled by default on 4.1.x, unlike main
        filter.setAudienceIsEndpointAddress(false);
        assertNull(filter.validateAudiences(Collections.singletonList("/api/read")));

        filter.setAudienceIsEndpointAddress(true);
        assertEquals("/api/read", filter.validateAudiences(Collections.singletonList("/api/read")));
    }

    @Test
    public void testValidateAudiencesRejectsMissingAudiencesWhenAudienceConfigured() {
        OAuthRequestFilter filter = new OAuthRequestFilter();
        filter.setAudience("https://rs.example/api");

        assertThrows(NotAuthorizedException.class, () -> filter.validateAudiences(null));
        assertThrows(NotAuthorizedException.class, () -> filter.validateAudiences(Collections.emptyList()));
        assertEquals("https://rs.example/api",
            filter.validateAudiences(Collections.singletonList("https://rs.example/api")));
    }

    @Test
    public void testConfiguredAudienceIsUsedByJwtAccessTokenValidator() throws Exception {
        Message message = new MessageImpl();
        message.put(Message.REQUEST_URL, "https://rs.example/orders");
        setThreadLocalMessage(message);

        String jwt = createSignedToken("api://orders");
        OAuthRequestFilter filter = createJwtFilter(jwt);
        filter.setAudience("api://orders");

        filter.validateRequest(message);

        assertEquals("api://orders", message.get(JwtConstants.EXPECTED_CLAIM_AUDIENCE));
        assertEquals("api://orders", message.getContent(OAuthContext.class).getTokenAudience());
    }

    @Test
    public void testConfiguredAudienceRejectsJwtForOtherAudience() throws Exception {
        Message message = new MessageImpl();
        message.put(Message.REQUEST_URL, "https://rs.example/orders");
        setThreadLocalMessage(message);

        String jwt = createSignedToken("api://other");
        OAuthRequestFilter filter = createJwtFilter(jwt);
        filter.setAudience("api://orders");

        assertThrows(NotAuthorizedException.class, () -> filter.validateRequest(message));
    }

    @Test
    public void testConfiguredAudienceDoesNotOverrideExpectedAudience() throws Exception {
        Message message = new MessageImpl();
        message.put(Message.REQUEST_URL, "https://rs.example/orders");
        message.put(JwtConstants.EXPECTED_CLAIM_AUDIENCE, "api://preset");
        setThreadLocalMessage(message);

        String jwt = createSignedToken("api://orders");
        OAuthRequestFilter filter = createJwtFilter(jwt);
        filter.setAudience("api://orders");

        assertThrows(NotAuthorizedException.class, () -> filter.validateRequest(message));
        assertEquals("api://preset", message.get(JwtConstants.EXPECTED_CLAIM_AUDIENCE));
    }

    @Test
    public void testRequiredScopesRejectDuplicatedScopeInsteadOfMissingOne() throws Exception {
        Message message = new MessageImpl();
        message.put(Message.REQUEST_URL, "https://rs.example/orders");
        setThreadLocalMessage(message);

        String jwt = createSignedToken("api://orders", "read read");
        OAuthRequestFilter filter = createJwtFilter(jwt);
        filter.setAudience("api://orders");
        filter.setRequiredScopes(Arrays.asList("read", "admin"));

        assertThrows(ForbiddenException.class, () -> filter.validateRequest(message));
    }

    @Test
    public void testRequiredScopesAcceptTokenWithAllRequiredScopes() throws Exception {
        Message message = new MessageImpl();
        message.put(Message.REQUEST_URL, "https://rs.example/orders");
        setThreadLocalMessage(message);

        String jwt = createSignedToken("api://orders", "admin read read");
        OAuthRequestFilter filter = createJwtFilter(jwt);
        filter.setAudience("api://orders");
        filter.setRequiredScopes(Arrays.asList("read", "admin"));

        filter.validateRequest(message);

        assertEquals(3, message.getContent(OAuthContext.class).getPermissions().size());
    }

    private static OAuthRequestFilter createJwtFilter(String jwt) {
        JwtAccessTokenValidator validator = new JwtAccessTokenValidator();
        validator.setJwsVerifier(new HmacJwsSignatureVerifier(SIGNING_KEY, SignatureAlgorithm.HS256));

        OAuthRequestFilter filter = new OAuthRequestFilter() {
            @Override
            protected String[] getAuthorizationParts(Message m) {
                return new String[] {"Bearer", jwt};
            }
        };
        filter.setTokenValidator(validator);
        filter.setMessageContext(mock(MessageContext.class));
        return filter;
    }

    private static String createSignedToken(String audience) {
        return createSignedToken(audience, null);
    }

    private static String createSignedToken(String audience, String scope) {
        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuedAt(now);
        claims.setExpiryTime(now + 3600);
        claims.setIssuer("https://as.example");
        claims.setClaim("client_id", "client");
        claims.setAudience(audience);
        if (scope != null) {
            claims.setClaim(OAuthConstants.SCOPE, scope);
        }

        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(new JwtToken(claims));
        return producer.signWith(new HmacJwsSignatureProvider(SIGNING_KEY, SignatureAlgorithm.HS256));
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
