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
package org.apache.cxf.rs.security.oauth2.grants.code;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.provider.JCacheOAuthDataProvider;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class JCacheLifecycleTest {
    @Test
    public void testSharedCodeAndOAuthProvidersSurviveClose() {
        JCacheCodeDataProvider first = new JCacheCodeDataProvider();
        JCacheCodeDataProvider same = new JCacheCodeDataProvider();
        JCacheOAuthDataProvider oauth = new JCacheOAuthDataProvider();
        try {
            Client client = new Client("shared-client", "secret", true);
            first.setClient(client);
            AuthorizationCodeRegistration registration = new AuthorizationCodeRegistration();
            registration.setClient(client);
            ServerAuthorizationCodeGrant grant = first.createCodeGrant(registration);
            first.close();
            first.close();
            assertNotNull(same.getClient("shared-client"));
            assertNotNull(same.getCodeGrant(grant.getCode()));
            assertNotNull(oauth.getClient("shared-client"));
            same.close();
            assertNotNull(oauth.getClient("shared-client"));
        } finally {
            first.close();
            same.close();
            oauth.close();
        }
    }
}
