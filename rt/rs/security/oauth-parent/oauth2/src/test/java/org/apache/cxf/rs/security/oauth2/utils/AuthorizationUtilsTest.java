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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;

public class AuthorizationUtilsTest extends Assert {
    
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
        Set<String> challenges = new LinkedHashSet<String>();
        challenges.add("Basic");
        challenges.add("Bearer");
        try {
            AuthorizationUtils.throwAuthorizationFailure(challenges);
            fail("WebApplicationException expected");
        } catch (WebApplicationException ex) {
            Response r = ex.getResponse();
            assertEquals(401, r.getStatus());
            Object value = r.getMetadata().getFirst(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(value);
            assertEquals("Basic,Bearer", value.toString());
        }
    }

    @Test
    public void testThrowAuthorizationFailureNoChallenge() {
        try {
            AuthorizationUtils.throwAuthorizationFailure(Collections.<String>emptySet());
            fail("WebApplicationException expected");
        } catch (WebApplicationException ex) {
            Response r = ex.getResponse();
            assertEquals(401, r.getStatus());
            Object value = r.getMetadata().getFirst(HttpHeaders.WWW_AUTHENTICATE);
            assertNull(value);
        }
    }
}
