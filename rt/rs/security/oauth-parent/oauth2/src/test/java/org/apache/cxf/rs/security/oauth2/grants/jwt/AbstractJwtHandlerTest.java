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
package org.apache.cxf.rs.security.oauth2.grants.jwt;

import java.util.Arrays;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

public class AbstractJwtHandlerTest {
    private static final String UNSIGNED_TEXT = "myUnsignedText";
    private static final byte[] SIGNATURE = "mySignature".getBytes();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private AbstractJwtHandler handler;
    @Mock
    private JwsSignatureVerifier signatureVerifier;
    @Mock
    private JwsHeaders headers;

    @Before
    public void setUp() {
        handler = new AbstractJwtHandler(Arrays.asList("someGrantType")) {
            @Override
            public ServerAccessToken createAccessToken(Client client, MultivaluedMap<String, String> params)
                throws OAuthServiceException {
                throw new UnsupportedOperationException("not implemented");
            }
        };
        handler.setJwsVerifier(signatureVerifier);
    }

    @Test
    public void testValidateSignatureWithValidSignature() {
        when(signatureVerifier.verify(headers, UNSIGNED_TEXT, SIGNATURE)).thenReturn(true);
        handler.validateSignature(headers, UNSIGNED_TEXT, SIGNATURE);
    }

    @Test
    public void testValidateSignatureWithInvalidSignature() {
        when(signatureVerifier.verify(headers, UNSIGNED_TEXT, SIGNATURE)).thenReturn(false);
        try {
            handler.validateSignature(headers, UNSIGNED_TEXT, SIGNATURE);
            fail("OAuthServiceException expected");
        } catch (OAuthServiceException expected) {
        }
    }
}
