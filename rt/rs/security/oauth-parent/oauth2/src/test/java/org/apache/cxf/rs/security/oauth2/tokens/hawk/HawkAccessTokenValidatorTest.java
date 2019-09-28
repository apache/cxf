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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth2.client.HttpRequestProperties;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenValidation;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class HawkAccessTokenValidatorTest {

    private HawkAccessTokenValidator validator = new HawkAccessTokenValidator();
    private OAuthDataProvider dataProvider = EasyMock.createMock(OAuthDataProvider.class);
    private MessageContext messageContext = EasyMock.createMock(MessageContext.class);

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

        EasyMock.expect(dataProvider.getAccessToken(macAccessToken.getTokenKey())).andReturn(macAccessToken);
        EasyMock.expect(messageContext.getHttpServletRequest()).andReturn(httpRequest);
        EasyMock.expect(messageContext.getUriInfo()).andReturn(uriInfo);
        EasyMock.replay(dataProvider, messageContext, httpRequest, uriInfo);

        String authData = getClientAuthHeader(macAccessToken);
        AccessTokenValidation tokenValidation = validator
            .validateAccessToken(messageContext,
                                 OAuthConstants.HAWK_AUTHORIZATION_SCHEME,
                                 authData.split(" ")[1],
                                 null);
        assertNotNull(tokenValidation);
        EasyMock.verify(dataProvider, messageContext, httpRequest);
    }

    private static String getClientAuthHeader(HawkAccessToken macAccessToken) {
        String address = "http://localhost:8080/appContext/oauth2/testResource";
        HttpRequestProperties props = new HttpRequestProperties(URI.create(address), "GET");

        return new HawkAuthorizationScheme(props, macAccessToken)
            .toAuthorizationHeader(macAccessToken.getMacAlgorithm(),
                                   macAccessToken.getMacKey());
    }

    private static HttpServletRequest mockHttpRequest() {
        HttpServletRequest httpRequest = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(httpRequest.getMethod()).andReturn("GET");
        return httpRequest;
    }

    private static UriInfo mockUriInfo() {
        UriInfo ui = EasyMock.createMock(UriInfo.class);
        EasyMock.expect(ui.getRequestUri()).andReturn(
            URI.create("http://localhost:8080/appContext/oauth2/testResource"));
        return ui;
    }

}