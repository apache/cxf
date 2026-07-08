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
package org.apache.cxf.rs.security.oauth2.grants.jwt;

import java.lang.reflect.Field;

import javax.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.OAuthDataProviderImpl;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.tokens.bearer.BearerAccessToken;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class JwtBearerGrantHandlerTest {

    private static final String SIGNING_KEY = "jwt-grant-test-signing-key";

    @Before
    public void setUp() throws Exception {
        setThreadLocalMessage(new MessageImpl());
    }

    @After
    public void tearDown() throws Exception {
        setThreadLocalMessage(null);
    }

    @Test
    public void testMismatchedClientAndSubjectRejected() {
        JwtBearerGrantHandler handler = new JwtBearerGrantHandler();
        handler.setDataProvider(new SubjectAwareDataProvider());
        handler.setJwsVerifier(new HmacJwsSignatureVerifier(SIGNING_KEY, SignatureAlgorithm.HS256));

        Client client = new Client("fuzz-client", "secret", true);
        String assertion = createSignedAssertion("trusted-issuer", "victim-user");

        MultivaluedMap<String, String> params = new MetadataMap<>();
        params.putSingle(Constants.CLIENT_GRANT_ASSERTION_PARAM, assertion);

        try {
            handler.createAccessToken(client, params);
            fail("OAuthServiceException expected");
        } catch (OAuthServiceException expected) {
            assertEquals("invalid_grant", expected.getMessage());
        }
    }

    @Test
    public void testMatchingClientAndSubjectAccepted() {
        JwtBearerGrantHandler handler = new JwtBearerGrantHandler();
        handler.setDataProvider(new SubjectAwareDataProvider());
        handler.setJwsVerifier(new HmacJwsSignatureVerifier(SIGNING_KEY, SignatureAlgorithm.HS256));

        Client client = new Client("fuzz-client", "secret", true);
        String assertion = createSignedAssertion("trusted-issuer", client.getClientId());

        MultivaluedMap<String, String> params = new MetadataMap<>();
        params.putSingle(Constants.CLIENT_GRANT_ASSERTION_PARAM, assertion);

        ServerAccessToken token = handler.createAccessToken(client, params);

        assertNotNull(token);
        assertNotNull(token.getSubject());
        assertEquals(client.getClientId(), token.getSubject().getLogin());
    }

    private static String createSignedAssertion(String issuer, String subject) {
        long now = System.currentTimeMillis() / 1000;
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(issuer);
        claims.setSubject(subject);
        claims.setIssuedAt(now);
        claims.setExpiryTime(now + 300);

        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(claims);
        return producer.signWith(new HmacJwsSignatureProvider(SIGNING_KEY, SignatureAlgorithm.HS256));
    }

    private static final class SubjectAwareDataProvider extends OAuthDataProviderImpl {
        @Override
        public ServerAccessToken createAccessToken(AccessTokenRegistration accessToken) {
            BearerAccessToken token = new BearerAccessToken(accessToken.getClient(), 3600);
            token.setSubject(accessToken.getSubject());
            token.setGrantType(accessToken.getGrantType());
            return token;
        }
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
