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
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;

import org.junit.Assert;
import org.junit.Test;

public class OAuthJSONProviderTest extends Assert {

    @Test
    public void testWriteClientAccessToken() throws Exception {
        ClientAccessToken token = new ClientAccessToken("bearer", "1234");
        token.setExpiresIn(12345);
        token.setRefreshToken("5678");
        token.setApprovedScope("read");
        token.setParameters(Collections.singletonMap("my_parameter", "abc"));
        
        OAuthJSONProvider provider = new OAuthJSONProvider();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        provider.writeTo(token, 
                         ClientAccessToken.class,
                         ClientAccessToken.class, 
                         new Annotation[]{}, 
                         MediaType.APPLICATION_JSON_TYPE, 
                         new MetadataMap<String, Object>(), 
                         bos);
        doReadClientAccessToken(bos.toString());
    }
    
    @Test
    public void testReadClientAccessToken() throws Exception {
        String response = 
            "{"
            + "\"access_token\":\"1234\","
            + "\"token_type\":\"bearer\","
            + "\"refresh_token\":\"5678\","
            + "\"expires_in\":12345,"
            + "\"scope\":\"read\","
            + "\"my_parameter\":\"abc\""
            + "}";
        doReadClientAccessToken(response);
    }
    
    @SuppressWarnings({
        "unchecked", "rawtypes"
    })
    public void doReadClientAccessToken(String response) throws Exception {
        OAuthJSONProvider provider = new OAuthJSONProvider();
        ClientAccessToken token = (ClientAccessToken)provider.readFrom((Class)ClientAccessToken.class, 
                          ClientAccessToken.class, 
                          new Annotation[]{}, 
                          MediaType.APPLICATION_JSON_TYPE, 
                          new MetadataMap<String, String>(), 
                          new ByteArrayInputStream(response.getBytes()));
        assertEquals("1234", token.getTokenKey());
        assertEquals("bearer", token.getTokenType());
        assertEquals("5678", token.getRefreshToken());
        assertEquals(12345, token.getExpiresIn());
        assertEquals("read", token.getApprovedScope());
        Map<String, String> extraParams = token.getParameters();
        assertEquals(1, extraParams.size());
        assertEquals("abc", extraParams.get("my_parameter"));
    }
    
    
    
}
