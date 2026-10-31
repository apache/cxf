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
package org.apache.cxf.rs.security.oauth2.provider;

import java.util.Arrays;
import java.util.Collections;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JCacheJWTOAuthDataProviderTest extends AbstractOAuthDataProviderTest {

    @Before
    public void setUp() throws Exception {
        JCacheOAuthDataProvider provider = new JCacheOAuthDataProvider();
        provider.setUseJwtFormatForAccessTokens(true);
        initializeProvider(provider);
        setProvider(provider);
    }

    @Test
    public void testRefreshedJwtAccessTokenKeepsCertificateBinding() {
        MessageContext mc = mock(MessageContext.class);
        when(mc.get(JoseConstants.HEADER_X509_THUMBPRINT_SHA256)).thenReturn("thumbprint");
        getProvider().setMessageContext(mc);

        Client c = addClient("101", "bob");

        AccessTokenRegistration atr = new AccessTokenRegistration();
        atr.setClient(c);
        atr.setApprovedScope(Arrays.asList("a", "refreshToken"));
        atr.setSubject(c.getResourceOwnerSubject());

        ServerAccessToken at = getProvider().createAccessToken(atr);
        ServerAccessToken at2 = getProvider().refreshAccessToken(c, at.getRefreshToken(), Collections.emptyList());

        JwtToken jwt = new JwsJwtCompactConsumer(at2.getTokenKey()).getJwtToken();
        assertEquals(Collections.singletonMap(JoseConstants.HEADER_X509_THUMBPRINT_SHA256, "thumbprint"),
                     jwt.getClaim(JwtConstants.CLAIM_CONFIRMATION));
    }

}
