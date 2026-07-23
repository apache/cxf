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
package org.apache.cxf.rs.security.oidc.rp;

import java.lang.reflect.Method;
import java.net.URI;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for nonce enforcement in {@link OidcClientCodeRequestFilter}.
 *
 * OIDC Core §3.2.2.1 (Implicit Flow) and §3.3.2.1 (Hybrid Flow) require the
 * {@code nonce} parameter whenever an ID Token may be returned directly from
 * the Authorization Endpoint (i.e., the response_type contains {@code id_token}).
 */
public class OidcClientCodeRequestFilterNonceTest {

    private static final URI ABSOLUTE_PATH = URI.create("https://app.example.com/rp/callback");

    // -----------------------------------------------------------------------
    // validateIdToken – direct tests via reflection
    // -----------------------------------------------------------------------

    /**
     * Code Flow (default): nonce is optional. A token with no nonce claim must be
     * accepted even when the state carries no nonce.
     */
    @Test
    public void testCodeFlowAcceptsTokenWithoutNonce() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        // no responseType set → code flow

        IdToken token = new IdToken();
        // no nonce in token

        MultivaluedMap<String, String> state = new MetadataMap<>();
        // no nonce in state

        invokeValidateIdToken(filter, token, state); // must not throw
    }

    /**
     * Code Flow: if the RP sent a nonce it MUST be echoed back by the IdP.
     */
    @Test
    public void testCodeFlowRejectsNonceMismatch() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();

        IdToken token = new IdToken();
        token.setNonce("wrong-nonce");

        MultivaluedMap<String, String> state = new MetadataMap<>();
        state.putSingle(IdToken.NONCE_CLAIM, "correct-nonce");

        try {
            invokeValidateIdToken(filter, token, state);
            fail("Expected OAuthServiceException for nonce mismatch");
        } catch (OAuthServiceException ex) {
            assertEquals(OAuthConstants.INVALID_REQUEST, ex.getMessage());
        }
    }

    /**
     * Code Flow: token nonce absent when state carries one must be rejected.
     */
    @Test
    public void testCodeFlowRejectsAbsentTokenNonceWhenStateHasOne() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();

        IdToken token = new IdToken();
        // no nonce in token

        MultivaluedMap<String, String> state = new MetadataMap<>();
        state.putSingle(IdToken.NONCE_CLAIM, "expected-nonce");

        try {
            invokeValidateIdToken(filter, token, state);
            fail("Expected OAuthServiceException: token nonce absent");
        } catch (OAuthServiceException ex) {
            assertEquals(OAuthConstants.INVALID_REQUEST, ex.getMessage());
        }
    }

    /**
     * Implicit Flow (response_type=id_token): nonce is REQUIRED.
     * A null state (no ClientCodeStateManager configured) must be rejected.
     */
    @Test
    public void testImplicitFlowRejectsNullState() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        filter.setResponseType(OidcUtils.ID_TOKEN_RESPONSE_TYPE);

        IdToken token = new IdToken();

        try {
            invokeValidateIdToken(filter, token, null);
            fail("Expected OAuthServiceException: nonce required for implicit flow");
        } catch (OAuthServiceException ex) {
            assertEquals(OAuthConstants.INVALID_REQUEST, ex.getMessage());
        }
    }

    /**
     * Implicit Flow: nonce is REQUIRED; state without one must be rejected.
     */
    @Test
    public void testImplicitFlowRejectsStateWithoutNonce() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        filter.setResponseType(OidcUtils.ID_TOKEN_RESPONSE_TYPE);

        IdToken token = new IdToken();
        MultivaluedMap<String, String> state = new MetadataMap<>();
        // no nonce in state

        try {
            invokeValidateIdToken(filter, token, state);
            fail("Expected OAuthServiceException: nonce required for implicit flow");
        } catch (OAuthServiceException ex) {
            assertEquals(OAuthConstants.INVALID_REQUEST, ex.getMessage());
        }
    }

    /**
     * Implicit Flow: valid nonce round-trip must be accepted.
     */
    @Test
    public void testImplicitFlowAcceptsMatchingNonce() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        filter.setResponseType(OidcUtils.ID_TOKEN_RESPONSE_TYPE);

        IdToken token = new IdToken();
        token.setNonce("session-nonce-abc");

        MultivaluedMap<String, String> state = new MetadataMap<>();
        state.putSingle(IdToken.NONCE_CLAIM, "session-nonce-abc");

        invokeValidateIdToken(filter, token, state); // must not throw
    }

    /**
     * Implicit Flow: nonce mismatch must be rejected.
     */
    @Test
    public void testImplicitFlowRejectsNonceMismatch() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        filter.setResponseType(OidcUtils.ID_TOKEN_RESPONSE_TYPE);

        IdToken token = new IdToken();
        token.setNonce("attacker-nonce");

        MultivaluedMap<String, String> state = new MetadataMap<>();
        state.putSingle(IdToken.NONCE_CLAIM, "session-nonce-abc");

        try {
            invokeValidateIdToken(filter, token, state);
            fail("Expected OAuthServiceException for nonce mismatch");
        } catch (OAuthServiceException ex) {
            assertEquals(OAuthConstants.INVALID_REQUEST, ex.getMessage());
        }
    }

    /**
     * Hybrid Flow (response_type=code id_token): nonce is REQUIRED.
     */
    @Test
    public void testHybridFlowRejectsStateWithoutNonce() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        filter.setResponseType(OidcUtils.CODE_ID_TOKEN_RESPONSE_TYPE);

        IdToken token = new IdToken();
        MultivaluedMap<String, String> state = new MetadataMap<>();

        try {
            invokeValidateIdToken(filter, token, state);
            fail("Expected OAuthServiceException: nonce required for hybrid flow");
        } catch (OAuthServiceException ex) {
            assertEquals(OAuthConstants.INVALID_REQUEST, ex.getMessage());
        }
    }

    /**
     * Hybrid Flow (response_type=id_token token): nonce is REQUIRED.
     */
    @Test
    public void testImplicitWithAccessTokenFlowRejectsStateWithoutNonce() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        filter.setResponseType(OidcUtils.ID_TOKEN_AT_RESPONSE_TYPE);

        IdToken token = new IdToken();
        MultivaluedMap<String, String> state = new MetadataMap<>();

        try {
            invokeValidateIdToken(filter, token, state);
            fail("Expected OAuthServiceException: nonce required for id_token token flow");
        } catch (OAuthServiceException ex) {
            assertEquals(OAuthConstants.INVALID_REQUEST, ex.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // toCodeRequestState – nonce auto-generation
    // -----------------------------------------------------------------------

    /**
     * For Implicit Flow, {@code toCodeRequestState} must auto-generate a nonce when the
     * caller has not supplied one.
     */
    @Test
    public void testImplicitFlowAutoGeneratesNonce() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        filter.setResponseType(OidcUtils.ID_TOKEN_RESPONSE_TYPE);

        MultivaluedMap<String, String> state = invokeToCodeRequestState(filter, new MultivaluedHashMap<>());

        String nonce = state.getFirst(IdToken.NONCE_CLAIM);
        assertNotNull("A nonce must be auto-generated for implicit flow", nonce);
    }

    /**
     * For Implicit Flow, a caller-supplied nonce must be preserved (not replaced).
     */
    @Test
    public void testImplicitFlowPreservesCallerNonce() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        filter.setResponseType(OidcUtils.ID_TOKEN_RESPONSE_TYPE);

        MultivaluedHashMap<String, String> query = new MultivaluedHashMap<>();
        query.putSingle(IdToken.NONCE_CLAIM, "my-app-nonce");

        MultivaluedMap<String, String> state = invokeToCodeRequestState(filter, query);

        assertEquals("Caller-supplied nonce must not be overwritten",
            "my-app-nonce", state.getFirst(IdToken.NONCE_CLAIM));
    }

    /**
     * For Code Flow (default), no nonce should be auto-generated.
     */
    @Test
    public void testCodeFlowDoesNotAutoGenerateNonce() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        // no responseType set → code flow

        MultivaluedMap<String, String> state = invokeToCodeRequestState(filter, new MultivaluedHashMap<>());

        assertNull("Code flow must not auto-generate a nonce", state.getFirst(IdToken.NONCE_CLAIM));
    }

    /**
     * Hybrid flow detected from context: an id_token in the callback requestParams means
     * nonce is required even when responseType has NOT been explicitly configured.
     */
    @Test
    public void testHybridFlowContextDetectionRequiresNonce() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        // No responseType configured — the filter is code-flow by default.

        IdToken token = new IdToken();
        MultivaluedMap<String, String> state = new MetadataMap<>();
        // No nonce in state.

        // Simulate a hybrid callback: id_token was returned in the authorization response.
        MultivaluedMap<String, String> requestParams = new MetadataMap<>();
        requestParams.putSingle(OidcUtils.ID_TOKEN, "some.jwt.value");

        try {
            invokeValidateIdToken(filter, token, state, requestParams);
            fail("Expected OAuthServiceException: nonce required for hybrid flow (context-detected)");
        } catch (OAuthServiceException ex) {
            assertEquals(OAuthConstants.INVALID_REQUEST, ex.getMessage());
        }
    }

    /**
     * Hybrid flow detected from context: valid nonce round-trip must be accepted.
     */
    @Test
    public void testHybridFlowContextDetectionAcceptsMatchingNonce() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        // No responseType configured.

        IdToken token = new IdToken();
        token.setNonce("session-nonce");

        MultivaluedMap<String, String> state = new MetadataMap<>();
        state.putSingle(IdToken.NONCE_CLAIM, "session-nonce");

        MultivaluedMap<String, String> requestParams = new MetadataMap<>();
        requestParams.putSingle(OidcUtils.ID_TOKEN, "some.jwt.value");

        invokeValidateIdToken(filter, token, state, requestParams); // must not throw
    }

    /**
     * Pure code flow (no id_token in callback) must not require a nonce even when
     * context detection is active.
     */
    @Test
    public void testCodeFlowContextDetectionDoesNotRequireNonce() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        // No responseType configured.

        IdToken token = new IdToken();
        MultivaluedMap<String, String> state = new MetadataMap<>();
        // No id_token in the callback — plain code flow.
        MultivaluedMap<String, String> requestParams = new MetadataMap<>();
        requestParams.putSingle("code", "auth-code");

        invokeValidateIdToken(filter, token, state, requestParams); // must not throw
    }

    /** Convenience overload: no id_token in callback (non-hybrid). */
    private static void invokeValidateIdToken(OidcClientCodeRequestFilter filter,
                                              IdToken idToken,
                                              MultivaluedMap<String, String> state) {
        invokeValidateIdToken(filter, idToken, state, new MetadataMap<>());
    }

    private static void invokeValidateIdToken(OidcClientCodeRequestFilter filter,
                                              IdToken idToken,
                                              MultivaluedMap<String, String> state,
                                              MultivaluedMap<String, String> requestParams) {
        try {
            Method method = OidcClientCodeRequestFilter.class.getDeclaredMethod(
                "validateIdToken", IdToken.class, MultivaluedMap.class, boolean.class);
            method.setAccessible(true);
            boolean hybridFlowDetected = requestParams != null
                && requestParams.containsKey(OidcUtils.ID_TOKEN);
            method.invoke(filter, idToken, state, hybridFlowDetected);
        } catch (java.lang.reflect.InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof OAuthServiceException) {
                throw (OAuthServiceException) cause;
            }
            throw new IllegalStateException(cause);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static MultivaluedMap<String, String> invokeToCodeRequestState(
            OidcClientCodeRequestFilter filter, MultivaluedMap<String, String> queryParams) {
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getQueryParameters(anyBoolean())).thenReturn(queryParams);
        when(uriInfo.getAbsolutePath()).thenReturn(ABSOLUTE_PATH);

        ContainerRequestContext rc = mock(ContainerRequestContext.class);
        when(rc.getUriInfo()).thenReturn(uriInfo);
        when(rc.getMediaType()).thenReturn(null);

        try {
            Method method = OidcClientCodeRequestFilter.class.getDeclaredMethod(
                "toCodeRequestState", ContainerRequestContext.class, UriInfo.class);
            method.setAccessible(true);
            return (MultivaluedMap<String, String>) method.invoke(filter, rc, uriInfo);
        } catch (java.lang.reflect.InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException(cause);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
