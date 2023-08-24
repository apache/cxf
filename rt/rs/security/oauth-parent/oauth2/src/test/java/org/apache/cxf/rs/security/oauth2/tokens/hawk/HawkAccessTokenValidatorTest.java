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
package org.apache.cxf.rs.security.oauth2.tokens.hawk;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth2.client.HttpRequestProperties;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenValidation;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HawkAccessTokenValidatorTest {

    private HawkAccessTokenValidator validator = new HawkAccessTokenValidator();
    private OAuthDataProvider dataProvider = mock(OAuthDataProvider.class);
    private MessageContext messageContext = mock(MessageContext.class);

    @Before
    public void setUp() {
        validator.setDataProvider(dataProvider);
    }

    @Test
    public void testValidateAccessToken() throws Exception {
        HawkAccessToken macAccessToken = new HawkAccessToken(new Client("testClientId", "testClientSecret",
                                                                          true),
                                                                          HmacAlgorithm.HmacSHA256, -1);
        HttpServletRequest httpRequest = mockHttpRequest();
        UriInfo uriInfo = mockUriInfo();

        when(dataProvider.getAccessToken(macAccessToken.getTokenKey())).thenReturn(macAccessToken);
        when(messageContext.getHttpServletRequest()).thenReturn(httpRequest);
        when(messageContext.getUriInfo()).thenReturn(uriInfo);

        String authData = getClientAuthHeader(macAccessToken);
        AccessTokenValidation tokenValidation = validator
            .validateAccessToken(messageContext,
                                 OAuthConstants.HAWK_AUTHORIZATION_SCHEME,
                                 authData.split(" ")[1],
                                 null);
        assertNotNull(tokenValidation);
    }

    private static String getClientAuthHeader(HawkAccessToken macAccessToken) {
        String address = "http://localhost:8080/appContext/oauth2/testResource";
        HttpRequestProperties props = new HttpRequestProperties(URI.create(address), "GET");

        return new HawkAuthorizationScheme(props, macAccessToken)
            .toAuthorizationHeader(macAccessToken.getMacAlgorithm(),
                                   macAccessToken.getMacKey());
    }

    private static HttpServletRequest mockHttpRequest() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getMethod()).thenReturn("GET");
        return httpRequest;
    }

    private static UriInfo mockUriInfo() {
        UriInfo ui = mock(UriInfo.class);
        when(ui.getRequestUri()).thenReturn(
            URI.create("http://localhost:8080/appContext/oauth2/testResource"));
        return ui;
    }

}