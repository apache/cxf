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
import java.util.List;

import org.apache.cxf.rs.security.oauth2.common.Client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OAuthUtilsTest {

    @Test
    public void testValidateScopesStrict() {
        List<String> requestScopes = OAuthUtils.parseScope("a c b");
        List<String> registeredScopes = OAuthUtils.parseScope("a b c d");
        assertTrue(OAuthUtils.validateScopes(requestScopes, registeredScopes, false));
    }
    @Test
    public void testValidateScopesStrictFail() {
        List<String> requestScopes = OAuthUtils.parseScope("a b c d");
        List<String> registeredScopes = OAuthUtils.parseScope("a b d");
        assertFalse(OAuthUtils.validateScopes(requestScopes, registeredScopes, false));
    }

    @Test
    public void testValidateScopesPartial() {
        List<String> requestScopes = OAuthUtils.parseScope("a b c-1");
        List<String> registeredScopes = OAuthUtils.parseScope("a b c");
        assertTrue(OAuthUtils.validateScopes(requestScopes, registeredScopes, true));
    }

    @Test
    public void testValidateScopesPartialFail() {
        List<String> requestScopes = OAuthUtils.parseScope("a b c");
        List<String> registeredScopes = OAuthUtils.parseScope("a b");
        assertFalse(OAuthUtils.validateScopes(requestScopes, registeredScopes, true));
    }

    @Test
    public void testGetRequestedScopesRegistered() {
        Client c = new Client();
        List<String> scopes = Collections.singletonList("a");
        c.setRegisteredScopes(scopes);
        assertEquals(scopes, OAuthUtils.getRequestedScopes(c, "", false, false));
    }

    @Test
    public void testParseScopeEmpty() {
        assertTrue(OAuthUtils.parseScope(null).isEmpty());
        assertTrue(OAuthUtils.parseScope("").isEmpty());
        assertTrue(OAuthUtils.parseScope(" ").isEmpty());
    }

}
