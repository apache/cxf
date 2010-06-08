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

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.systest.jaxrs.BookNotFoundFault;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSSpringSecurityInterfaceTest extends AbstractSpringSecurityTest {
    public static final String PORT = BookServerSecuritySpringInterface.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerSecuritySpringInterface.class));
    }
    
    @Test
    public void testFailedAuthentication() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstorestorage/thosebooks/123"; 
        getBook(endpointAddress, "foo", "ba", 401);
    }
    
    @Test
    public void testGetBookUserAdmin() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstorestorage/thosebooks/123"; 
        getBook(endpointAddress, "foo", "bar", 200);
        getBook(endpointAddress, "bob", "bobspassword", 200);
    }
    
    @Test
    public void testGetBookUser() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstorestorage/thosebooks/123/123"; 
        getBook(endpointAddress, "foo", "bar", 200);
        getBook(endpointAddress, "bob", "bobspassword", 200);
        getBook(endpointAddress, "baddy", "baddyspassword", 403);
    }
    
    @Test
    public void testGetBookAdmin() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstorestorage/thosebooks"; 
        getBook(endpointAddress, "foo", "bar", 200); 
        getBook(endpointAddress, "bob", "bobspassword", 403);
    }
    
    @Test
    public void testGetBookSubresource() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstorestorage/subresource"; 
        getBook(endpointAddress, "foo", "bar", 200); 
        getBook(endpointAddress, "bob", "bobspassword", 403);
    }   
    
    @Test
    public void testWebClientAdmin() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstorestorage/thosebooks";
        doGetBookWebClient(address, "foo", "bar",  200);
    }
    
    @Test
    public void testProxyClientAdmin() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstorestorage";
        doGetBookProxyClient(address, "foo", "bar",  200);
    }
    
    @Test
    public void testWebClientUserUnauthorized() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstorestorage/thosebooks";
        doGetBookWebClient(address, "bob", "bobspassword", 403);
    }
    
    @Test
    public void testWebClientUserAuthorized() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstorestorage/thosebooks/123/123";
        doGetBookWebClient(address, "bob", "bobspassword", 200);
    }
    
    private void doGetBookWebClient(String address, String username, String password, int expectedStatus) {
        WebClient wc = WebClient.create(address, username, password, null);
        Response r = wc.get();
        assertEquals(expectedStatus, r.getStatus());
        WebClient wc2 = WebClient.fromClient(wc);
        r = wc2.get();
        assertEquals(expectedStatus, r.getStatus());    
    }
    
    private void doGetBookProxyClient(String address, String username, String password, int expectedStatus) 
        throws BookNotFoundFault {
        SecureBookInterface books = JAXRSClientFactory.create(address, SecureBookInterface.class, 
                                                       username, password, null);
        Book b = books.getThatBook();
        assertEquals(123, b.getId());
        Response r = WebClient.client(books).getResponse();
        assertEquals(expectedStatus, r.getStatus());
            
    }
    
    @Test
    public void testGetBookSubresourceAdmin() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstorestorage/securebook/self"; 
        getBook(endpointAddress, "foo", "bar", 200); 
        getBook(endpointAddress, "bob", "bobspassword", 403);
    }
}
