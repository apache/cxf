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

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.systest.jaxrs.BookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSHttpsBookTest extends AbstractBusClientServerTestBase {

    private static final String CLIENT_CONFIG_FILE =
        "org/apache/cxf/systest/jaxrs/security/jaxrs-https.xml";
        
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookHttpsServer.class));
    }
    
    @Test
    public void testGetBook123Client() throws Exception {
        
        SpringBusFactory bf = new SpringBusFactory();
        Bus bus = bf.createBus(CLIENT_CONFIG_FILE);
        BusFactory.setDefaultBus(bus);
        
        BookStore bs = JAXRSClientFactory.create("https://localhost:9095", BookStore.class);
        // just to verify the interface call goes through CGLIB proxy too
        assertEquals("https://localhost:9095", WebClient.client(bs).getBaseURI().toString());
        Book b = bs.getBook("123");
        assertEquals(b.getId(), 123);
    }
    
    @Test
    public void testGetBook123WebClient() throws Exception {
        
        SpringBusFactory bf = new SpringBusFactory();
        Bus bus = bf.createBus(CLIENT_CONFIG_FILE);
        BusFactory.setDefaultBus(bus);
        
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("https://localhost:9095");
        WebClient client = bean.createWebClient();
        assertEquals("https://localhost:9095", client.getBaseURI().toString());
        
        client.path("/bookstore/books/123").accept(MediaType.APPLICATION_XML_TYPE);
        Book b = client.get(Book.class);
        assertEquals(123, b.getId());
    }
    
}
