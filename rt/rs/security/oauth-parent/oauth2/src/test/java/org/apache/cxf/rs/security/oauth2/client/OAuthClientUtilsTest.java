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

import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
}