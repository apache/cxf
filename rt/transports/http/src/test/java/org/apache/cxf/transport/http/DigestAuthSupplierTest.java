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
package org.apache.cxf.transport.http;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

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
        HTTPConduit conduit = control.createMock(HTTPConduit.class);
        AuthorizationPolicy authorizationPolicy = new AuthorizationPolicy();
        authorizationPolicy.setUserName("testUser");
        authorizationPolicy.setPassword("testPassword");
        
        EasyMock.expect(conduit.getAuthorization()).andReturn(authorizationPolicy).atLeastOnce();
        URL url = new URL("http://myserver");
        Message message = new MessageImpl();
        control.replay();
        String authToken = authSupplier
            .getAuthorizationForRealm(conduit, url, message, "myRealm", fullHeader);
        assertEquals("Digest response=\"28e616b6868f60aaf9b19bb5b172f076\", " 
                     + "cnonce=\"27db039b76362f3d55da10652baee38c\", " 
                     + "username=\"testUser\", nc=\"00000001\", " 
                     + "nonce=\"MTI0ODg3OTc5NzE2OTplZGUyYTg0Yzk2NTFkY2YyNjc1Y2JjZjU2MTUzZmQyYw==\", "
                     + "realm=\"MyCompany realm.\", qop=\"auth\", uri=\"\"", authToken);
        control.verify();
    }
}
