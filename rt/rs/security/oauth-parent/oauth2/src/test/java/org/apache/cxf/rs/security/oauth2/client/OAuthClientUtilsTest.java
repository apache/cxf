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
package org.apache.cxf.rs.security.oauth2.client;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.refresh.RefreshTokenGrant;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

import org.junit.Test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OAuthClientUtilsTest {

    @Test
    public void getAccessToken() {
        WebClient accessTokenService = mock(WebClient.class);
        String tokenKey = "tokenKey";
        String response = "{\"" + OAuthConstants.ACCESS_TOKEN + "\":\"" + tokenKey + "\"}";
        expect(accessTokenService.form(anyObject(Form.class))).andReturn(
                Response.ok(new ByteArrayInputStream(response.getBytes()), MediaType.APPLICATION_JSON).build());
        replay(accessTokenService);

        ClientAccessToken cat = OAuthClientUtils.getAccessToken(accessTokenService, null, new RefreshTokenGrant(""),
                null, "defaultTokenType", false);
        assertEquals(tokenKey, cat.getTokenKey());

        verify(accessTokenService);
    }

    @Test
    public void getAccessTokenInternalServerError() {
        WebClient accessTokenService = mock(WebClient.class);
        expect(accessTokenService.form(anyObject(Form.class)))
                .andReturn(Response.serverError().type(MediaType.TEXT_PLAIN)
                        .entity(new ByteArrayInputStream("Unrecoverable error in the server.".getBytes())).build());
        replay(accessTokenService);

        try {
            OAuthClientUtils.getAccessToken(accessTokenService, null, new RefreshTokenGrant(""), null, null, false);
            fail();
        } catch (OAuthServiceException e) {
            assertEquals(OAuthConstants.SERVER_ERROR, e.getMessage());
        } finally {
            verify(accessTokenService);
        }
    }

    @Test
    public void fromMapToClientToken() {
        final Map<String, String> map = new HashMap<>();
        final String accessToken = "SlAV32hkKG";
        map.put(OAuthConstants.ACCESS_TOKEN, accessToken);
        final String tokenType = "Bearer";
        map.put(OAuthConstants.ACCESS_TOKEN_TYPE, tokenType);
        final String refreshToken = "8xLOxBtZp8";
        map.put(OAuthConstants.REFRESH_TOKEN, refreshToken);
        final String expiresIn = "3600";
        map.put(OAuthConstants.ACCESS_TOKEN_EXPIRES_IN, expiresIn);

        final ClientAccessToken token = OAuthClientUtils.fromMapToClientToken(map);
        assertEquals(accessToken, token.getTokenKey());
        assertEquals(tokenType, token.getTokenType());
        assertEquals(refreshToken, token.getRefreshToken());
        assertEquals(Long.parseLong(expiresIn), token.getExpiresIn());
    }

    @Test
    public void getAuthorizationURI() {
        String authorizationServiceURI = "https://authorization";
        String clientId = "clientId";
        String redirectUri = "https://redirect";
        String state = "unique";
        String scope = OAuthConstants.REFRESH_TOKEN_SCOPE;

        URI uri = OAuthClientUtils.getAuthorizationURI(authorizationServiceURI, clientId, redirectUri, state, scope);

        assertTrue(uri.toString().startsWith(authorizationServiceURI));

        Map<String, String> query = Arrays.asList(uri.getQuery().split("&")).stream().map(s -> s.split("="))
                .collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1] : ""));
        assertEquals(clientId, query.get(OAuthConstants.CLIENT_ID));
        assertEquals(redirectUri, query.get(OAuthConstants.REDIRECT_URI));
        assertEquals(state, query.get(OAuthConstants.STATE));
        assertEquals(OAuthConstants.CODE_RESPONSE_TYPE, query.get(OAuthConstants.RESPONSE_TYPE));
    }

}
