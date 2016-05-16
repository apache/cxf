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
package org.apache.cxf.systest.jaxrs.security.oauth;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.oauth.OAuth;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth.client.OAuthClientUtils;
import org.apache.cxf.rs.security.oauth.client.OAuthClientUtils.Token;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

public class TemporaryCredentialServiceTest extends AbstractBusClientServerTestBase {

    public static final String TEMPORARY_CREDENTIALS_URL = "/a/oauth/initiate";
    public static final String HOST = "http://localhost:";

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(OAuthServer.class, true));
    }
    
    
    @Test
    public void testGetTemporaryCredentialsURIQuery() throws Exception {
        Map<String, String> parameters = new HashMap<String, String>();
        
        parameters.put(OAuth.OAUTH_SIGNATURE_METHOD, "HMAC-SHA1");
        parameters.put(OAuth.OAUTH_NONCE, UUID.randomUUID().toString());
        parameters.put(OAuth.OAUTH_TIMESTAMP, String.valueOf(System.currentTimeMillis() / 1000));
        
        String uri = HOST + OAuthServer.PORT + TEMPORARY_CREDENTIALS_URL;
        WebClient wc = WebClient.create(uri);
        
        Token t = OAuthClientUtils.getRequestToken(wc, 
            new OAuthClientUtils.Consumer(OAuthTestUtils.CLIENT_ID, OAuthTestUtils.CLIENT_SECRET), 
                                         URI.create(OAuthTestUtils.CALLBACK), 
                                         parameters);
        assertNotNull(t);
        assertNotNull(t.getToken());
        assertNotNull(t.getSecret());
        
    }

}
