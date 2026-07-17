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
package org.apache.cxf.rs.security.oidc.idp;

import java.util.Arrays;
import java.util.Collections;

import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.common.UserInfo;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UserInfoServiceTest {

    @Test
    public void testCreateFromIdTokenFiltersClaimsForOpenIdScope() {
        UserInfoService service = new UserInfoService();
        service.setAdditionalClaims(Collections.singletonList("email_verified"));

        IdToken idToken = new IdToken();
        idToken.setSubject("alice");
        idToken.setName("Alice Example");
        idToken.setEmail("alice@example.com");
        idToken.setEmailVerified(true);
        idToken.setGivenName("Alice");
        idToken.setFamilyName("Example");

        UserInfo userInfo = service.createFromIdToken(idToken, Collections.singletonList(OidcUtils.OPENID_SCOPE));

        assertEquals("alice", userInfo.getSubject());
        assertNull(userInfo.getName());
        assertNull(userInfo.getEmail());
        assertNull(userInfo.getGivenName());
        assertNull(userInfo.getFamilyName());
        assertNull(userInfo.getEmailVerified());
        assertNull(userInfo.getPhoneNumber());
    }

    @Test
    public void testCreateFromIdTokenReturnsProfileAndEmailClaimsForGrantedScopes() {
        UserInfoService service = new UserInfoService();
        service.setAdditionalClaims(Collections.singletonList("email_verified"));

        IdToken idToken = new IdToken();
        idToken.setSubject("alice");
        idToken.setName("Alice Example");
        idToken.setEmail("alice@example.com");
        idToken.setEmailVerified(true);
        idToken.setGivenName("Alice");
        idToken.setFamilyName("Example");

        UserInfo userInfo = service.createFromIdToken(idToken,
            Arrays.asList(OidcUtils.OPENID_SCOPE, OidcUtils.PROFILE_SCOPE, OidcUtils.EMAIL_SCOPE));

        assertEquals("alice", userInfo.getSubject());
        assertEquals("Alice Example", userInfo.getName());
        assertEquals("alice@example.com", userInfo.getEmail());
        assertEquals("Alice", userInfo.getGivenName());
        assertEquals("Example", userInfo.getFamilyName());
        assertEquals(Boolean.TRUE, userInfo.getEmailVerified());
    }
}