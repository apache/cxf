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
package org.apache.cxf.transport.http.auth;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DigestAuthSupplierTest {

    /**
     * Tests that parseHeader correctly parses parameters that contain ==
     * 
     * @throws Exception
     */
    @Test
    public void testCXF2370() throws Exception {
        String origNonce = "MTI0ODg3OTc5NzE2OTplZGUyYTg0Yzk2NTFkY2YyNjc1Y2JjZjU2MTUzZmQyYw==";
        String fullHeader = "Digest realm=\"MyCompany realm.\", qop=\"auth\"," + "nonce=\"" + origNonce
                            + "\"";
        Map<String, String> map = new HttpAuthHeader(fullHeader).getParams();
        assertEquals(origNonce, map.get("nonce"));
        assertEquals("auth", map.get("qop"));
        assertEquals("MyCompany realm.", map.get("realm"));
    }

    @Test
    public void testEncode() throws MalformedURLException {
        String origNonce = "MTI0ODg3OTc5NzE2OTplZGUyYTg0Yzk2NTFkY2YyNjc1Y2JjZjU2MTUzZmQyYw==";
        String fullHeader = "Digest realm=\"MyCompany realm.\", qop=\"auth\"," + "nonce=\"" + origNonce
                            + "\"";
        
        /**
         * Initialize DigestAuthSupplier that always uses the same cnonce so we always
         * get the same response
         */
        DigestAuthSupplier authSupplier = new DigestAuthSupplier() {

            @Override
            public String createCnonce() throws UnsupportedEncodingException {
                return "27db039b76362f3d55da10652baee38c";
            }
            
        };
        IMocksControl control = EasyMock.createControl();
        AuthorizationPolicy authorizationPolicy = new AuthorizationPolicy();
        authorizationPolicy.setUserName("testUser");
        authorizationPolicy.setPassword("testPassword");
        URL url = new URL("http://myserver");
        Message message = new MessageImpl();
        control.replay();
        
        String authToken = authSupplier
            .getAuthorization(authorizationPolicy, url, message, fullHeader);
        HttpAuthHeader authHeader = new HttpAuthHeader(authToken);
        assertEquals("Digest", authHeader.getAuthType());
        Map<String, String> params = authHeader.getParams();
        Map<String, String> expectedParams = new HashMap<String, String>();
        expectedParams.put("response", "28e616b6868f60aaf9b19bb5b172f076");
        expectedParams.put("cnonce", "27db039b76362f3d55da10652baee38c");
        expectedParams.put("username", "testUser");
        expectedParams.put("nc", "00000001");
        expectedParams.put("nonce", "MTI0ODg3OTc5NzE2OTplZGUyYTg0Yzk2NTFkY2YyNjc1Y2JjZjU2MTUzZmQyYw==");
        expectedParams.put("realm", "MyCompany realm.");
        expectedParams.put("qop", "auth");
        expectedParams.put("uri", "");
        assertEquals(expectedParams, params);
        control.verify();
    }
}
