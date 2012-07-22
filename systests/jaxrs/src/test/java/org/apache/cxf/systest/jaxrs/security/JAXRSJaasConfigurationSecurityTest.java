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

package org.apache.cxf.systest.jaxrs.security;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSJaasConfigurationSecurityTest extends AbstractSpringSecurityTest {
    public static final int PORT = BookServerJaasSecurity.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerJaasSecurity.class, 
                                true));
    }
    
    @Test
    public void testJaasInterceptorAuthenticationFailure() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/service/jaasConfig/bookstorestorage/thosebooks/123"; 
        getBook(endpointAddress, "foo", "bar1", 401);
    }
    
    @Test
    public void testGetBookUserAdminJaasInterceptor() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/service/jaasConfig/bookstorestorage/thosebooks/123"; 
        getBook(endpointAddress, "foo", "bar", 403);
        getBook(endpointAddress, "bob", "bobspassword", 200);
    }
    
    @Test
    public void testJaasFilterAuthenticationFailure() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/service/jaasConfigFilter/bookstorestorage/thosebooks/123"; 
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("text/xml");
        wc.header(HttpHeaders.AUTHORIZATION, 
                  "Basic " + base64Encode("foo" + ":" + "bar1"));
        Response r = wc.get();
        assertEquals(401, r.getStatus());
        Object wwwAuthHeader = r.getMetadata().getFirst(HttpHeaders.WWW_AUTHENTICATE);
        assertNotNull(wwwAuthHeader);
        assertEquals("Basic", wwwAuthHeader.toString());
    }
    
    @Test
    public void testGetBookUserAdminJaasFilter() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/service/jaasConfigFilter/bookstorestorage/thosebooks/123"; 
        getBook(endpointAddress, "foo", "bar", 403);
        getBook(endpointAddress, "bob", "bobspassword", 200);
    }
}
