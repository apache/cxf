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

package org.apache.cxf.systest.jaxrs;

import javax.ws.rs.core.Response;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.local.LocalConduit;
import org.apache.cxf.transport.local.LocalTransportFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JAXRSLocalTransportTest extends AbstractBusClientServerTestBase {
 
    private Server localServer;
    
    @Before
    public void setUp() {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(BookStore.class, BookStoreSpring.class);
        sf.setResourceProvider(BookStore.class,
                               new SingletonResourceProvider(new BookStore(), true));
        sf.setResourceProvider(BookStoreSpring.class,
                               new SingletonResourceProvider(new BookStoreSpring(), true));
        sf.setTransportId(LocalTransportFactory.TRANSPORT_ID);
        sf.setAddress("local://books");
        localServer = sf.create();
    }
    
    @After
    public void tearDown() {
        if (localServer != null) {
            localServer.stop();
        }
    }
    
    @Test
    public void testProxyDirectDispatchGet() throws Exception {
        BookStore localProxy = 
            JAXRSClientFactory.create("local://books", BookStore.class);
        
        WebClient.getConfig(localProxy).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        
        Book book = localProxy.getBook("123");
        assertEquals(123L, book.getId());
    }
    
    @Test
    public void testProxyDirectDispatchPostWithGzip() throws Exception {
        BookStore localProxy = 
            JAXRSClientFactory.create("local://books", BookStore.class);
        
        WebClient.getConfig(localProxy).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        
        Response response = localProxy.addBook(new Book("New", 124L));
        assertEquals(200, response.getStatus());
    }
    
    @Test
    public void testProxyDirectDispatchPost() throws Exception {
        BookStoreSpring localProxy = 
            JAXRSClientFactory.create("local://books", BookStoreSpring.class);
        
        WebClient.getConfig(localProxy).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        
        Book response = localProxy.convertBook(new Book2("New", 124L));
        assertEquals(124L, response.getId());
    }
    
    @Test
    public void testProxyPipedDispatchPost() throws Exception {
        BookStoreSpring localProxy = 
            JAXRSClientFactory.create("local://books", BookStoreSpring.class);
        
        Book response = localProxy.convertBook(new Book2("New", 124L));
        assertEquals(124L, response.getId());
    }
    
    @Test
    public void testWebClientDirectDispatch() throws Exception {
        WebClient localClient = WebClient.create("local://books");
        
        WebClient.getConfig(localClient).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        localClient.path("bookstore/books/123");
        Book book = localClient.get(Book.class);
        assertEquals(123L, book.getId());
    }
    
    @Test
    public void testWebClientPipedDispatch() throws Exception {
        WebClient localClient = WebClient.create("local://books");
        localClient.path("bookstore/books");
        Book book = localClient.post(new Book("New", 124L), Book.class);
        assertEquals(124L, book.getId());
    }
    
    @Test
    public void testProxyWithQuery() throws Exception {
        BookStore localProxy = 
            JAXRSClientFactory.create("local://books", BookStore.class);
        
        WebClient.getConfig(localProxy).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        
        Book book = localProxy.getBookByURLQuery(new String[] {"1", "2", "3"});
        assertEquals(123L, book.getId());
    }
}
