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

import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.client.Consumer;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class IdTokenReaderTest {

    @Test
    public void testCodeHashIsOptionalByDefaultForTokenEndpointIdToken() {
        IdTokenReader idTokenReader = new StubIdTokenReader(new JwtToken(new JwtClaims()));
        idTokenReader.setRequireAccessTokenHash(false);

        ClientAccessToken accessToken = new ClientAccessToken("Bearer", "access-token");
        accessToken.getParameters().put(OidcUtils.ID_TOKEN, "id-token");

        assertNotNull(idTokenReader.getIdJwtToken(accessToken, "auth-code", new Consumer("client-id")));
    }

    @Test(expected = OAuthServiceException.class)
    public void testCodeHashIsRequiredByDefaultForHybridTokenEndpointIdToken() {
        IdTokenReader idTokenReader = new StubIdTokenReader(new JwtToken(new JwtClaims()));
        idTokenReader.setRequireAccessTokenHash(false);

        ClientAccessToken accessToken = new ClientAccessToken("Bearer", "access-token");
        accessToken.getParameters().put(OidcUtils.ID_TOKEN, "id-token");
        accessToken.getParameters().put(OAuthConstants.RESPONSE_TYPE, OidcUtils.CODE_ID_TOKEN_RESPONSE_TYPE);

        idTokenReader.getIdJwtToken(accessToken, "auth-code", new Consumer("client-id"));
    }

    private static final class StubIdTokenReader extends IdTokenReader {
        private final JwtToken jwt;

        private StubIdTokenReader(JwtToken jwt) {
            this.jwt = jwt;
        }

        @Override
        public JwtToken getIdJwtToken(String idJwtToken, Consumer client) {
            return jwt;
        }
    }
}
