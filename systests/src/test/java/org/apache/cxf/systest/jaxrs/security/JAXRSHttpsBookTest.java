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

import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.systest.jaxrs.BookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSHttpsBookTest extends AbstractBusClientServerTestBase {

    private static final String CLIENT_CONFIG_FILE =
        "org/apache/cxf/systest/jaxrs/security/jaxrs-https.xml";
    private static final String CLIENT_CONFIG_FILE2 =
        "org/apache/cxf/systest/jaxrs/security/jaxrs-https-url.xml";
        
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookHttpsServer.class));
    }
    
    @Test
    public void testGetBook123Proxy() throws Exception {
        
        BookStore bs = JAXRSClientFactory.create("https://localhost:9095", BookStore.class, 
                                                 CLIENT_CONFIG_FILE);
        // just to verify the interface call goes through CGLIB proxy too
        assertEquals("https://localhost:9095", WebClient.client(bs).getBaseURI().toString());
        Book b = bs.getSecureBook("123");
        assertEquals(b.getId(), 123);
        b = bs.getSecureBook("123");
        assertEquals(b.getId(), 123);
    }
    
    @Test
    public void testGetBook123ProxyWithURLConduitId() throws Exception {
        
        BookStore bs = JAXRSClientFactory.create("https://localhost:9095", BookStore.class, 
                                                 CLIENT_CONFIG_FILE2);
        // just to verify the interface call goes through CGLIB proxy too
        assertEquals("https://localhost:9095", WebClient.client(bs).getBaseURI().toString());
        Book b = bs.getSecureBook("123");
        assertEquals(b.getId(), 123);
        b = bs.getSecureBook("123");
        assertEquals(b.getId(), 123);
    }
    
    
    @Test
    public void testGetBook123ProxyToWebClient() throws Exception {
        
        BookStore bs = JAXRSClientFactory.create("https://localhost:9095", BookStore.class, 
                                                 CLIENT_CONFIG_FILE);
        Book b = bs.getSecureBook("123");
        assertEquals(b.getId(), 123);
        WebClient wc = WebClient.fromClient(WebClient.client(bs));
        wc.path("/bookstore/securebooks/123").accept(MediaType.APPLICATION_XML_TYPE);
        Book b2 = wc.get(Book.class);
        assertEquals(123, b2.getId());
    }
    
    
    @Test
    public void testGetBook123WebClientToProxy() throws Exception {
        
        WebClient wc = WebClient.create("https://localhost:9095", CLIENT_CONFIG_FILE);
        wc.path("/bookstore/securebooks/123").accept(MediaType.APPLICATION_XML_TYPE);
        Book b = wc.get(Book.class);
        assertEquals(123, b.getId());
        
        wc.back(true);
        
        BookStore bs = JAXRSClientFactory.fromClient(wc, BookStore.class);
        Book b2 = bs.getSecureBook("123");
        assertEquals(b2.getId(), 123);
        
    }
    
    
    @Test
    public void testGetBook123WebClient() throws Exception {
        
        WebClient client = WebClient.create("https://localhost:9095", CLIENT_CONFIG_FILE);
        assertEquals("https://localhost:9095", client.getBaseURI().toString());
        
        client.path("/bookstore/securebooks/123").accept(MediaType.APPLICATION_XML_TYPE);
        Book b = client.get(Book.class);
        assertEquals(123, b.getId());
        
    }
    
    @Test
    public void testGetBook123WebClientWithURLConduitId() throws Exception {
        
        WebClient client = WebClient.create("https://localhost:9095", CLIENT_CONFIG_FILE2);
        assertEquals("https://localhost:9095", client.getBaseURI().toString());
        
        client.path("/bookstore/securebooks/123").accept(MediaType.APPLICATION_XML_TYPE);
        Book b = client.get(Book.class);
        assertEquals(123, b.getId());
        
    }
    
}
