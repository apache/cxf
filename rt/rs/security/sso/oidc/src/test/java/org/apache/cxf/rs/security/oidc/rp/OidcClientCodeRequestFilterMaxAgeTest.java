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

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oidc.common.IdToken;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for max_age enforcement in {@link OidcClientCodeRequestFilter}.
 *
 * OIDC Core §3.1.3.7 requires the RP to validate auth_time when max_age is used.
 * The max_age parameter sent to the OP must be a duration in seconds (§3.1.2.1).
 */
public class OidcClientCodeRequestFilterMaxAgeTest {

    private static final URI ABSOLUTE_PATH = URI.create("https://app.example.com/rp/callback");
    private static final String MAX_AGE_PARAMETER = "max_age";

    // -----------------------------------------------------------------------
    // toCodeRequestState – state encoding
    // -----------------------------------------------------------------------

    /**
     * The state entry must be a Unix timestamp in seconds, comparable to the
     * id_token auth_time claim. Storing milliseconds here (old bug) would produce a
     * value ~1000x larger than any real auth_time, making the validation unreachable.
     */
    @Test
    public void testStateStoresMinAuthTimeInSeconds() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        filter.setMaxAgeOffset(60L);

        long beforeSeconds = System.currentTimeMillis() / 1000;
        MultivaluedMap<String, String> state = invokeToCodeRequestState(filter, new MultivaluedHashMap<>());
        long afterSeconds = System.currentTimeMillis() / 1000;

        String stored = state.getFirst(MAX_AGE_PARAMETER);
        assertNotNull("State must contain max_age entry", stored);

        long minAuthTime = Long.parseLong(stored);
        // The stored value must be in the range [before - 60, after - 60]:
        // a milliseconds value (~1.7e12) would fail this range entirely.
        assertTrue("Stored min-auth-time must be in seconds, not milliseconds",
            minAuthTime >= beforeSeconds - 60 && minAuthTime <= afterSeconds - 60);
    }

    /**
     * When maxAgeOffset is not set, no max_age entry should appear in the state.
     */
    @Test
    public void testStateHasNoMaxAgeWhenNotConfigured() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();

        MultivaluedMap<String, String> state = invokeToCodeRequestState(filter, new MultivaluedHashMap<>());

        assertTrue("State must not contain max_age when maxAgeOffset is not set",
            state.getFirst(MAX_AGE_PARAMETER) == null);
    }

    // -----------------------------------------------------------------------
    // validateIdToken – max_age enforcement
    // -----------------------------------------------------------------------

    /**
     * A token whose auth_time is within the max_age window must be accepted.
     */
    @Test
    public void testAcceptsFreshAuthTime() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        filter.setMaxAgeOffset(300L); // 5 minutes

        long nowSeconds = System.currentTimeMillis() / 1000;
        long recentAuthTime = nowSeconds - 60; // authenticated 1 minute ago

        IdToken token = new IdToken();
        token.setAuthenticationTime(recentAuthTime);

        // State contains the minimum acceptable auth_time.
        MultivaluedMap<String, String> state = new MetadataMap<>();
        state.putSingle(MAX_AGE_PARAMETER, Long.toString(nowSeconds - 300));

        invokeValidateIdToken(filter, token, state); // must not throw
    }

    /**
     * A token whose auth_time is older than the max_age window must be rejected.
     * This was the primary regression: the old ms-vs-s bug made rejection unreachable.
     */
    @Test
    public void testRejectsStaleAuthTime() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        filter.setMaxAgeOffset(60L); // 1 minute

        long nowSeconds = System.currentTimeMillis() / 1000;
        long staleAuthTime = nowSeconds - 86400; // authenticated 24 hours ago

        IdToken token = new IdToken();
        token.setAuthenticationTime(staleAuthTime);

        MultivaluedMap<String, String> state = new MetadataMap<>();
        state.putSingle(MAX_AGE_PARAMETER, Long.toString(nowSeconds - 60));

        try {
            invokeValidateIdToken(filter, token, state);
            fail("Expected OAuthServiceException: auth_time is older than max_age allows");
        } catch (OAuthServiceException ex) {
            assertEquals(OAuthConstants.INVALID_REQUEST, ex.getMessage());
        }
    }

    /**
     * An auth_time exactly equal to minAuthTime (boundary) must be accepted.
     */
    @Test
    public void testAcceptsAuthTimeAtBoundary() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        filter.setMaxAgeOffset(60L);

        long nowSeconds = System.currentTimeMillis() / 1000;
        long minAuthTime = nowSeconds - 60;

        IdToken token = new IdToken();
        token.setAuthenticationTime(minAuthTime); // exactly at the boundary

        MultivaluedMap<String, String> state = new MetadataMap<>();
        state.putSingle(MAX_AGE_PARAMETER, Long.toString(minAuthTime));

        invokeValidateIdToken(filter, token, state); // must not throw
    }

    /**
     * A missing auth_time claim must be rejected when max_age was requested.
     * OIDC Core §3.1.3.7 requires auth_time to be present in this case.
     * Previously this caused a NullPointerException (HTTP 500).
     */
    @Test
    public void testRejectsNullAuthTime() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        filter.setMaxAgeOffset(60L);

        IdToken token = new IdToken();
        // auth_time deliberately absent

        MultivaluedMap<String, String> state = new MetadataMap<>();
        long nowSeconds = System.currentTimeMillis() / 1000;
        state.putSingle(MAX_AGE_PARAMETER, Long.toString(nowSeconds - 60));

        try {
            invokeValidateIdToken(filter, token, state);
            fail("Expected OAuthServiceException: auth_time absent when max_age was requested");
        } catch (OAuthServiceException ex) {
            assertEquals(OAuthConstants.INVALID_REQUEST, ex.getMessage());
        }
    }

    /**
     * When maxAgeOffset is not configured, a token without auth_time must be accepted
     * — the null-guard must not fire in the non-max_age path.
     */
    @Test
    public void testNoMaxAgeOffsetSkipsAuthTimeCheck() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        // maxAgeOffset not set

        IdToken token = new IdToken();
        // auth_time absent

        invokeValidateIdToken(filter, token, new MetadataMap<>()); // must not throw
    }

    // -----------------------------------------------------------------------
    // setAdditionalCodeRequestParams – max_age request parameter to OP
    // -----------------------------------------------------------------------

    /**
     * The max_age query parameter forwarded to the Authorization Endpoint must be the
     * duration in seconds, not a millisecond timestamp. OIDC Core §3.1.2.1 defines
     * max_age as "Maximum Authentication Age" (elapsed time in seconds).
     */
    @Test
    public void testMaxAgeRequestParamIsDuration() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();
        filter.setMaxAgeOffset(120L);

        UriBuilder ub = UriBuilder.fromUri("https://idp.example.com/authorize");
        invokeSetAdditionalCodeRequestParams(filter, ub, null, null);

        URI built = ub.build();
        String query = built.getQuery();
        assertNotNull("URI must have a query string", query);
        assertTrue("max_age must appear in query", query.contains("max_age="));

        // Extract the max_age value and verify it is the duration, not a large timestamp.
        String maxAgeValue = extractQueryParam(query, MAX_AGE_PARAMETER);
        assertNotNull("max_age must be present", maxAgeValue);

        long sent = Long.parseLong(maxAgeValue);
        // A milliseconds timestamp would be ~1.7e12; the duration is 120.
        assertEquals("max_age must be the configured duration in seconds, not a timestamp", 120L, sent);
    }

    /**
     * When maxAgeOffset is not set, no max_age parameter must be added to the request.
     */
    @Test
    public void testNoMaxAgeParamWhenNotConfigured() {
        OidcClientCodeRequestFilter filter = new OidcClientCodeRequestFilter();

        UriBuilder ub = UriBuilder.fromUri("https://idp.example.com/authorize");
        invokeSetAdditionalCodeRequestParams(filter, ub, null, null);

        String query = ub.build().getQuery();
        assertTrue("max_age must not be added when maxAgeOffset is not configured",
            query == null || !query.contains("max_age="));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static void invokeValidateIdToken(OidcClientCodeRequestFilter filter,
                                              IdToken idToken,
                                              MultivaluedMap<String, String> state) {
        try {
            Method method = OidcClientCodeRequestFilter.class.getDeclaredMethod(
                "validateIdToken", IdToken.class, MultivaluedMap.class, boolean.class);
            method.setAccessible(true);
            method.invoke(filter, idToken, state, false);
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

    private static void invokeSetAdditionalCodeRequestParams(OidcClientCodeRequestFilter filter,
                                                             UriBuilder ub,
                                                             MultivaluedMap<String, String> redirectState,
                                                             MultivaluedMap<String, String> codeRequestState) {
        try {
            Method method = OidcClientCodeRequestFilter.class.getDeclaredMethod(
                "setAdditionalCodeRequestParams",
                UriBuilder.class, MultivaluedMap.class, MultivaluedMap.class);
            method.setAccessible(true);
            method.invoke(filter, ub, redirectState, codeRequestState);
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

    private static String extractQueryParam(String query, String name) {
        for (String param : query.split("&")) {
            if (param.startsWith(name + "=")) {
                return param.substring(name.length() + 1);
            }
        }
        return null;
    }
}
