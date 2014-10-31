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

package org.apache.cxf.systest.jaxrs.security.jwt;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.rs.security.jose.jaxrs.JwsJsonClientResponseFilter;
import org.apache.cxf.rs.security.jose.jaxrs.JwsJsonWriterInterceptor;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSJwsJsonTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerJwsJson.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerJwsJson.class, true));
    }
    
    @Test
    public void testJwsJsonPlainTextHmac() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjsonhmac";
        BookStore bs = createBookStore(address, "org/apache/cxf/systest/jaxrs/security/secret.jwk.properties");
        String text = bs.echoText("book");
        assertEquals("book", text);
    }
    @Test
    public void testJweJsonBookBeanHmac() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjsonhmac";
        BookStore bs = createBookStore(address, "org/apache/cxf/systest/jaxrs/security/secret.jwk.properties");
        Book book = bs.echoBook(new Book("book", 123L));
        assertEquals("book", book.getName());
        assertEquals(123L, book.getId());
    }
    private BookStore createBookStore(String address, String properties) throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSJwsJsonTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        bean.setServiceClass(BookStore.class);
        bean.setAddress(address);
        List<Object> providers = new LinkedList<Object>();
        JwsJsonWriterInterceptor writer = new JwsJsonWriterInterceptor();
        writer.setUseJwsJsonOutputStream(true);
        providers.add(writer);
        providers.add(new JwsJsonClientResponseFilter());
        bean.setProviders(providers);
        bean.getProperties(true).put("rs.security.signature.list.properties", properties);
        return bean.create(BookStore.class);
    }
    
}
