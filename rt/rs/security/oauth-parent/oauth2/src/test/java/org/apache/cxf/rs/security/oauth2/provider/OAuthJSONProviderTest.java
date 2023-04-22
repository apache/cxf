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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.TokenIntrospection;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OAuthJSONProviderTest {

    @Test
    public void testReadException() throws IOException {
        String response =
            "{"
            + "\"error\":\"invalid_client\","
            + "\"error_description\":\"Client authentication failed\""
            + "}";

        OAuthJSONProvider provider = new OAuthJSONProvider();
        Map<String, String> responseMap =
            provider.readJSONResponse(new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)));
        assertEquals(responseMap.size(), 2);
        assertEquals("invalid_client", responseMap.get("error"));
        assertEquals("Client authentication failed", responseMap.get("error_description"));
    }

    @Test
    public void testReadExceptionWithCommaInMessage() throws IOException {
        String response =
            "{"
            + "\"error\":\"invalid_client\","
            + "\"error_description\":\"Client authentication failed, due to xyz\""
            + "}";

        OAuthJSONProvider provider = new OAuthJSONProvider();
        Map<String, String> responseMap =
            provider.readJSONResponse(new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)));
        assertEquals(responseMap.size(), 2);
        assertEquals("invalid_client", responseMap.get("error"));
        assertEquals("Client authentication failed, due to xyz", responseMap.get("error_description"));
    }

    @Test
    public void testWriteBearerClientAccessToken() throws Exception {
        ClientAccessToken token = new ClientAccessToken(OAuthConstants.BEARER_TOKEN_TYPE, "1234");
        token.setExpiresIn(12345);
        token.setRefreshToken("5678");
        token.setApprovedScope("read");
        token.setParameters(Collections.singletonMap("my_parameter", "http://abc"));

        OAuthJSONProvider provider = new OAuthJSONProvider();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(token,
                         ClientAccessToken.class,
                         ClientAccessToken.class,
                         new Annotation[]{},
                         MediaType.APPLICATION_JSON_TYPE,
                         new MetadataMap<String, Object>(),
                         bos);
        doReadClientAccessToken(bos.toString(), OAuthConstants.BEARER_TOKEN_TYPE, token.getParameters());
    }

    @Test
    public void testReadBearerClientAccessToken() throws Exception {
        String response =
            "{"
            + "\"access_token\":\"1234\","
            + "\"token_type\":\"bearer\","
            + "\"refresh_token\":\"5678\","
            + "\"expires_in\":12345,"
            + "\"scope\":\"read\","
            + "\"my_parameter\":\"http://abc\""
            + "}";
        doReadClientAccessToken(response, OAuthConstants.BEARER_TOKEN_TYPE,
                                Collections.singletonMap("my_parameter", "http://abc"));
    }


    @Test
    @SuppressWarnings({
        "unchecked", "rawtypes"
    })
    public void testReadTokenIntrospection() throws Exception {
        String response =
            "{\"active\":true,\"client_id\":\"WjcK94pnec7CyA\",\"username\":\"alice\",\"token_type\":\"Bearer\""
            + ",\"scope\":\"a\",\"aud\":\"https://localhost:8082/service\","
                + "\"iat\":1453472181,\"exp\":1453475781}";
        OAuthJSONProvider provider = new OAuthJSONProvider();
        TokenIntrospection t = (TokenIntrospection)provider.readFrom((Class)TokenIntrospection.class,
                                                                     TokenIntrospection.class,
                          new Annotation[]{},
                          MediaType.APPLICATION_JSON_TYPE,
                          new MetadataMap<String, String>(),
                          new ByteArrayInputStream(response.getBytes()));
        assertTrue(t.isActive());
        assertEquals("WjcK94pnec7CyA", t.getClientId());
        assertEquals("alice", t.getUsername());
        assertEquals("a", t.getScope());
        assertEquals(1, t.getAud().size());
        assertEquals("https://localhost:8082/service", t.getAud().get(0));
        assertEquals(1453472181L, t.getIat().longValue());
        assertEquals(1453475781L, t.getExp().longValue());
    }
    @Test
    @SuppressWarnings({
        "unchecked", "rawtypes"
    })
    public void testReadTokenIntrospectionMultipleAuds() throws Exception {
        String response =
            "{\"active\":true,\"client_id\":\"WjcK94pnec7CyA\",\"username\":\"alice\",\"token_type\":\"Bearer\""
            + ",\"scope\":\"a\",\"aud\":[\"https://localhost:8082/service\",\"https://localhost:8083/service\"],"
                + "\"iat\":1453472181,\"exp\":1453475781}";
        OAuthJSONProvider provider = new OAuthJSONProvider();
        TokenIntrospection t = (TokenIntrospection)provider.readFrom((Class)TokenIntrospection.class,
                                                                     TokenIntrospection.class,
                          new Annotation[]{},
                          MediaType.APPLICATION_JSON_TYPE,
                          new MetadataMap<String, String>(),
                          new ByteArrayInputStream(response.getBytes()));
        assertTrue(t.isActive());
        assertEquals("WjcK94pnec7CyA", t.getClientId());
        assertEquals("alice", t.getUsername());
        assertEquals("a", t.getScope());
        assertEquals(2, t.getAud().size());
        assertEquals("https://localhost:8082/service", t.getAud().get(0));
        assertEquals("https://localhost:8083/service", t.getAud().get(1));
        assertEquals(1453472181L, t.getIat().longValue());
        assertEquals(1453475781L, t.getExp().longValue());
    }

    @Test
    @SuppressWarnings({
        "unchecked", "rawtypes"
    })
    public void testReadTokenIntrospectionSingleAudAsArray() throws Exception {
        String response =
            "{\"active\":false,\"client_id\":\"WjcK94pnec7CyA\",\"username\":\"alice\",\"token_type\":\"Bearer\""
            + ",\"scope\":\"a\",\"aud\":[\"https://localhost:8082/service\"],"
                + "\"iat\":1453472181,\"exp\":1453475781}";
        OAuthJSONProvider provider = new OAuthJSONProvider();
        TokenIntrospection t = (TokenIntrospection)provider.readFrom((Class)TokenIntrospection.class,
                                                                     TokenIntrospection.class,
                          new Annotation[]{},
                          MediaType.APPLICATION_JSON_TYPE,
                          new MetadataMap<String, String>(),
                          new ByteArrayInputStream(response.getBytes()));
        assertFalse(t.isActive());
        assertEquals("WjcK94pnec7CyA", t.getClientId());
        assertEquals("alice", t.getUsername());
        assertEquals("a", t.getScope());
        assertEquals(1, t.getAud().size());
        assertEquals("https://localhost:8082/service", t.getAud().get(0));
        assertEquals(1453472181L, t.getIat().longValue());
        assertEquals(1453475781L, t.getExp().longValue());
    }

    @SuppressWarnings({
        "unchecked", "rawtypes"
    })

    public ClientAccessToken doReadClientAccessToken(String response,
                                        String expectedTokenType,
                                        Map<String, String> expectedParams) throws Exception {
        OAuthJSONProvider provider = new OAuthJSONProvider();
        ClientAccessToken token = (ClientAccessToken)provider.readFrom((Class)ClientAccessToken.class,
                          ClientAccessToken.class,
                          new Annotation[]{},
                          MediaType.APPLICATION_JSON_TYPE,
                          new MetadataMap<String, String>(),
                          new ByteArrayInputStream(response.getBytes()));
        assertEquals("1234", token.getTokenKey());
        assertTrue(expectedTokenType.equalsIgnoreCase(token.getTokenType()));
        assertEquals("5678", token.getRefreshToken());
        assertEquals(12345, token.getExpiresIn());
        assertEquals("read", token.getApprovedScope());
        Map<String, String> extraParams = token.getParameters();
        if (expectedParams != null) {
            assertEquals(expectedParams, extraParams);
        }
        assertEquals("http://abc", extraParams.get("my_parameter"));

        return token;

    }

    @Test
    public void testWriteHawkClientAccessToken() throws Exception {
        ClientAccessToken token = new ClientAccessToken("hawk", "1234");
        token.setExpiresIn(12345);
        token.setRefreshToken("5678");
        token.setApprovedScope("read");
        Map<String, String> params = new LinkedHashMap<>();
        params.put(OAuthConstants.HAWK_TOKEN_KEY, "test_mac_secret");
        params.put(OAuthConstants.HAWK_TOKEN_ALGORITHM, OAuthConstants.HMAC_ALGO_SHA_1);
        params.put("my_parameter", "http://abc");

        token.setParameters(params);

        OAuthJSONProvider provider = new OAuthJSONProvider();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        provider.writeTo(token, ClientAccessToken.class, ClientAccessToken.class, new Annotation[] {},
                         MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), bos);
        doReadClientAccessToken(bos.toString(),
                                OAuthConstants.HAWK_TOKEN_TYPE,
                                params);

    }

    @Test
    public void testReadHawkClientAccessToken() throws Exception {
        String response = "{" + "\"access_token\":\"1234\"," + "\"token_type\":\"hawk\","
            + "\"refresh_token\":\"5678\"," + "\"expires_in\":12345," + "\"scope\":\"read\","
            + "\"secret\":\"adijq39jdlaska9asud\"," + "\"algorithm\":\"hmac-sha-256\","
            + "\"my_parameter\":\"http://abc\"" + "}";
        ClientAccessToken macToken = doReadClientAccessToken(response, "hawk", null);
        assertEquals("adijq39jdlaska9asud",
                     macToken.getParameters().get(OAuthConstants.HAWK_TOKEN_KEY));
        assertEquals("hmac-sha-256",
                     macToken.getParameters().get(OAuthConstants.HAWK_TOKEN_ALGORITHM));
    }

}