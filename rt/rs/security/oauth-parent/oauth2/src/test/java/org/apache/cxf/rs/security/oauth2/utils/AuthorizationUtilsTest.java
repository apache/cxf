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
package org.apache.cxf.rs.security.oauth2.utils;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class AuthorizationUtilsTest {

    @Test
    public void testThrowAuthorizationFailureSingleChallenge() {
        try {
            AuthorizationUtils.throwAuthorizationFailure(Collections.singleton("Basic"));
            fail("WebApplicationException expected");
        } catch (WebApplicationException ex) {
            Response r = ex.getResponse();
            assertEquals(401, r.getStatus());
            Object value = r.getMetadata().getFirst(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(value);
            assertEquals("Basic", value.toString());
        }
    }

    @Test
    public void testThrowAuthorizationFailureManyChallenges() {
        Set<String> challenges = new LinkedHashSet<>(Arrays.asList("Basic", "Bearer", "*"));
        try {
            AuthorizationUtils.throwAuthorizationFailure(challenges);
            fail("WebApplicationException expected");
        } catch (WebApplicationException ex) {
            Response r = ex.getResponse();
            assertEquals(401, r.getStatus());
            String value = r.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(value);
            assertEquals("Basic,Bearer", value);
        }
    }

    @Test
    public void testThrowAuthorizationFailureNoChallenge() {
        try {
            AuthorizationUtils.throwAuthorizationFailure(Collections.emptySet());
            fail("WebApplicationException expected");
        } catch (WebApplicationException ex) {
            Response r = ex.getResponse();
            assertEquals(401, r.getStatus());
            Object value = r.getMetadata().getFirst(HttpHeaders.WWW_AUTHENTICATE);
            assertNull(value);
        }
    }

    @Test
    public void testThrowAuthorizationFailureWithCause() {
        try {
            AuthorizationUtils.throwAuthorizationFailure(Collections.singleton("Basic"),
                                                         null, new RuntimeException("expired token"));
            fail("WebApplicationException expected");
        } catch (WebApplicationException ex) {
            Response r = ex.getResponse();
            assertEquals("expired token", r.getEntity());
            assertEquals(401, r.getStatus());
            Object value = r.getMetadata().getFirst(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(value);
            assertEquals("Basic", value.toString());
        }
    }

    @Test
    public void testThrowAuthorizationFailureWithRealm() {
        try {
            AuthorizationUtils.throwAuthorizationFailure(Collections.singleton("Basic"), "oauth");
            fail("WebApplicationException expected");
        } catch (WebApplicationException ex) {
            Response r = ex.getResponse();
            assertEquals(401, r.getStatus());
            String value = r.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(value);
            assertEquals("Basic realm=\"oauth\"", value);
        }
    }

    @Test
    public void testThrowAuthorizationFailureWithRealmInvalidCharacters() {
        String realm = "oauth\r\n\r\n\r\nX-Injected: true";
        try {
            AuthorizationUtils.throwAuthorizationFailure(Collections.singleton("Basic"), realm);
            fail("WebApplicationException expected");
        } catch (WebApplicationException ex) {
            Response r = ex.getResponse();
            assertEquals(401, r.getStatus());
            String value = r.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(value);
            assertEquals("Basic realm=" + "\"" + AuthorizationUtils.stripControlCharacters(realm) + "\"", value);
        }
    }

    @Test
    public void getAuthorizationParts() {
        String type = "type";
        String credentials = "credentials";

        Message m = new MessageImpl();
        m.put(Message.PROTOCOL_HEADERS, Collections.singletonMap(HttpHeaders.AUTHORIZATION,
                Collections.singletonList(type + ' ' + credentials)));
        MessageContext mc = new MessageContextImpl(m);

        String[] expected = new String[] {type, credentials};
        assertArrayEquals(expected,
                AuthorizationUtils.getAuthorizationParts(mc, null));
        assertArrayEquals(expected,
                AuthorizationUtils.getAuthorizationParts(mc, Collections.emptySet()));
        assertArrayEquals(expected,
                AuthorizationUtils.getAuthorizationParts(mc, Collections.singleton(type)));
        assertArrayEquals(expected,
                AuthorizationUtils.getAuthorizationParts(mc, Collections.singleton("*")));
        assertArrayEquals(expected,
                AuthorizationUtils.getAuthorizationParts(mc, new HashSet<>(Arrays.asList("another", type))));
    }

    @Test
    public void testGetAuthorizationPartsWithMultipleHeadersFails() {
        Message m = new MessageImpl();
        m.put(Message.PROTOCOL_HEADERS, Collections.singletonMap(HttpHeaders.AUTHORIZATION,
                Arrays.asList("Basic YWxpY2U6c2VjcmV0", "Bearer token")));
        MessageContext mc = new MessageContextImpl(m);

        try {
            AuthorizationUtils.getAuthorizationParts(mc);
            fail("WebApplicationException expected");
        } catch (WebApplicationException ex) {
            assertEquals(401, ex.getResponse().getStatus());
        }
    }

    @Test
    public void testGetBasicAuthParts() {
        String basicAuthData = Base64.getEncoder().encodeToString("alice:secret".getBytes());

        String[] parts = AuthorizationUtils.getBasicAuthParts(basicAuthData);
        assertArrayEquals(new String[] {"alice", "secret"}, parts);
    }

    @Test
    public void testGetBasicAuthPartsInvalidBase64() {
        try {
            AuthorizationUtils.getBasicAuthParts("not-base64");
            fail("WebApplicationException expected");
        } catch (WebApplicationException ex) {
            assertEquals(401, ex.getResponse().getStatus());
        }
    }

    @Test
    public void testGetBasicAuthPartsMissingSeparator() {
        String basicAuthData = Base64.getEncoder().encodeToString("alice".getBytes());

        try {
            AuthorizationUtils.getBasicAuthParts(basicAuthData);
            fail("WebApplicationException expected");
        } catch (WebApplicationException ex) {
            assertEquals(401, ex.getResponse().getStatus());
        }
    }

    @Test
    public void testGetBasicAuthUserInfo() {
        String basicAuthData = Base64.getEncoder().encodeToString("alice:secret".getBytes());

        Message m = new MessageImpl();
        m.put(Message.PROTOCOL_HEADERS, Collections.singletonMap(HttpHeaders.AUTHORIZATION,
                Collections.singletonList("Basic " + basicAuthData)));
        MessageContext mc = new MessageContextImpl(m);

        String[] userInfo = AuthorizationUtils.getBasicAuthUserInfo(mc);
        assertArrayEquals(new String[] {"alice", "secret"}, userInfo);
    }

}
