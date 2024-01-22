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

package org.apache.cxf.systest.jaxrs.security.jose.jwejws;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import jakarta.ws.rs.BadRequestException;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.rs.security.jose.jaxrs.JwsDetachedSignatureProvider;
import org.apache.cxf.rs.security.jose.jaxrs.multipart.JwsMultipartClientRequestFilter;
import org.apache.cxf.rs.security.jose.jaxrs.multipart.JwsMultipartClientResponseFilter;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.systest.jaxrs.security.jose.BookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRSJwsMultipartTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerJwsMultipart.PORT;
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerJwsMultipart.class, true));
    }

    @Test
    public void testJwsJwkBookHMacMultipart() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjwkhmac";
        BookStore bs = createJwsBookStoreHMac(address, false, false);
        Book book = bs.echoBookMultipart(new Book("book", 123L));
        assertEquals("book", book.getName());
        assertEquals(123L, book.getId());
    }

    @Test
    public void testJwsJwkBookHMacMultipartBuffered() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjwkhmac";
        BookStore bs = createJwsBookStoreHMac(address, true, false);
        Book book = bs.echoBookMultipart(new Book("book", 123L));
        assertEquals("book", book.getName());
        assertEquals(123L, book.getId());
    }

    @Test
    public void testJwsJwkBookHMacMultipartJwsJson() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjwkhmacJwsJson";
        BookStore bs = createJwsBookStoreHMac(address, false, true);
        Book book = bs.echoBookMultipart(new Book("book", 123L));
        assertEquals("book", book.getName());
        assertEquals(123L, book.getId());
    }

    @Test
    public void testJwsJwkBookRSAMultipart() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjwkrsa";
        BookStore bs = createJwsBookStoreRSA(address);
        Book book = bs.echoBookMultipart(new Book("book", 123L));
        assertEquals("book", book.getName());
        assertEquals(123L, book.getId());
    }

    @Test
    public void testJwsJwkBooksHMacMultipart() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjwkhmac";
        BookStore bs = createJwsBookStoreHMac(address, false, false);
        List<Book> books = new LinkedList<>();
        books.add(new Book("book", 123L));
        books.add(new Book("book2", 124L));
        List<Book> returnBooks = bs.echoBooksMultipart(books);
        assertEquals("book", returnBooks.get(0).getName());
        assertEquals(123L, returnBooks.get(0).getId());
        assertEquals("book2", returnBooks.get(1).getName());
        assertEquals(124L, returnBooks.get(1).getId());
    }

    @Test(expected = BadRequestException.class)
    public void testJwsJwkBooksHMacMultipartUnsigned() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjwkhmac";
        BookStore bs = JAXRSClientFactory.create(address, BookStore.class,
                            JAXRSJwsMultipartTest.class.getResource("client.xml").toString());
        bs.echoBookMultipart(new Book("book", 123L));
    }
    @Test(expected = BadRequestException.class)
    public void testJwsJwkBookHMacMultipartModified() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjwkhmacModified";
        BookStore bs = createJwsBookStoreHMac(address, false, false);
        bs.echoBookMultipartModified(new Book("book", 123L));
    }
    @Test(expected = BadRequestException.class)
    public void testJwsJwkBookHMacMultipartModifiedBufferPayload() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjwkhmacModifiedBufferPayload";
        BookStore bs = createJwsBookStoreHMac(address, true, false);
        bs.echoBookMultipartModified(new Book("book", 123L));
    }

    private BookStore createJwsBookStoreHMac(String address,
                                             boolean bufferPayload,
                                             boolean useJwsJsonSignatureFormat) throws Exception {
        JAXRSClientFactoryBean bean = createJAXRSClientFactoryBean(address, bufferPayload,
                                                                   useJwsJsonSignatureFormat);
        bean.getProperties(true).put("rs.security.signature.properties",
            "org/apache/cxf/systest/jaxrs/security/secret.jwk.properties");
        return bean.create(BookStore.class);
    }
    private BookStore createJwsBookStoreRSA(String address) throws Exception {
        JAXRSClientFactoryBean bean = createJAXRSClientFactoryBean(address, false, false);
        bean.getProperties(true).put("rs.security.signature.properties",
            "org/apache/cxf/systest/jaxrs/security/alice.jwk.properties");
        return bean.create(BookStore.class);
    }
    private JAXRSClientFactoryBean createJAXRSClientFactoryBean(String address,
                                                                boolean bufferPayload,
                                                                boolean useJwsJsonSignatureFormat) throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSJwsMultipartTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        bean.setServiceClass(BookStore.class);
        bean.setAddress(address);
        List<Object> providers = new LinkedList<>();
        JwsMultipartClientRequestFilter outFilter = new JwsMultipartClientRequestFilter();
        outFilter.setUseJwsJsonSignatureFormat(useJwsJsonSignatureFormat);
        providers.add(outFilter);
        JwsMultipartClientResponseFilter inFilter = new JwsMultipartClientResponseFilter();
        inFilter.setBufferPayload(bufferPayload);
        providers.add(inFilter);
        providers.add(new JwsDetachedSignatureProvider());
        bean.setProviders(providers);
        return bean;
    }
}
