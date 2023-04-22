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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.core.Response;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.local.LocalConduit;
import org.apache.cxf.transport.local.LocalTransportFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
        sf.setProvider(new JacksonJsonProvider());
        List<Interceptor<? extends Message>> outInts = new ArrayList<>();
        outInts.add(new CustomOutInterceptor());
        sf.setOutInterceptors(outInts);

        List<Interceptor<? extends Message>> inInts = new ArrayList<>();
        inInts.add(new CustomInFaultyInterceptor());
        sf.setInInterceptors(inInts);

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
    public void testProxyPipedDispatchGet() throws Exception {
        BookStore localProxy =
            JAXRSClientFactory.create("local://books", BookStore.class);
        Book book = localProxy.getBook("123");
        assertEquals(123L, book.getId());
    }
    @Test
    public void testProxyPipedDispatchGetBookType() throws Exception {
        BookStore localProxy =
            JAXRSClientFactory.create("local://books",
                                      BookStore.class,
                                      Collections.singletonList(new JacksonJsonProvider()));
        BookType book = localProxy.getBookType();
        assertEquals(124L, book.getId());
    }

    @Test
    public void testProxyServerInFaultMapped() throws Exception {
        BookStore localProxy = JAXRSClientFactory.create("local://books", BookStore.class);
        Response r = localProxy.infault();
        assertEquals(401, r.getStatus());
    }

    @Test
    public void testProxyServerInFaultEscaped() throws Exception {
        BookStore localProxy = JAXRSClientFactory.create("local://books", BookStore.class);
        Response r = localProxy.infault2();
        assertEquals(500, r.getStatus());
    }

    @Test
    public void testProxyServerInFaultDirectDispatch() throws Exception {
        BookStore localProxy = JAXRSClientFactory.create("local://books", BookStore.class);
        WebClient.getConfig(localProxy).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, "true");
        WebClient.getConfig(localProxy).getInFaultInterceptors().add(new TestFaultInInterceptor());
        Response r = localProxy.infault2();
        assertEquals(500, r.getStatus());
    }

    @Test
    public void testProxyEmtpyResponse() throws Exception {
        BookStore localProxy = JAXRSClientFactory.create("local://books", BookStore.class);
        assertNull(localProxy.getEmptyBook());
        assertEquals(204, WebClient.client(localProxy).getResponse().getStatus());
    }

    @Test
    public void testProxyEmptyResponseDirectDispatch() throws Exception {
        BookStore localProxy = JAXRSClientFactory.create("local://books", BookStore.class);
        WebClient.getConfig(localProxy).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, "true");
        assertNull(localProxy.getEmptyBook());
        assertEquals(204, WebClient.client(localProxy).getResponse().getStatus());
    }

    @Test
    public void testProxyServerOutFault() throws Exception {
        BookStore localProxy = JAXRSClientFactory.create("local://books", BookStore.class);
        Response r = localProxy.outfault();
        assertEquals(403, r.getStatus());
    }

    @Test
    public void testProxyServerOutFaultDirectDispacth() throws Exception {
        BookStore localProxy = JAXRSClientFactory.create("local://books", BookStore.class);
        WebClient.getConfig(localProxy).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, "true");
        Response r = localProxy.outfault();
        assertEquals(403, r.getStatus());
    }

    @Test
    public void testSubresourceProxyDirectDispatchGet() throws Exception {
        BookStore localProxy =
            JAXRSClientFactory.create("local://books", BookStore.class);

        WebClient.getConfig(localProxy).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, "true");

        Book bookSubProxy = localProxy.getBookSubResource("123");
        Book book = bookSubProxy.retrieveState();
        assertEquals(123L, book.getId());
    }

    @Test
    public void testProxyDirectDispatchPostWithGzip() throws Exception {
        BookStore localProxy =
            JAXRSClientFactory.create("local://books", BookStore.class);

        WebClient.getConfig(localProxy).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);

        Response response = localProxy.addBook(new Book("New", 124L));
        assertEquals(200, response.getStatus());
        assertTrue(response.getMetadata().getFirst("Location") instanceof URI);
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
    public void testWebClientDirectDispatchBookId() throws Exception {
        WebClient localClient = WebClient.create("local://books");
        localClient.accept("text/plain");

        WebClient.getConfig(localClient).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        localClient.path("bookstore/books/check/uuid/a6f7357f-6e7e-40e5-9b4a-c455c23b10a2");
        boolean hasBook = localClient.get(boolean.class);
        assertThat(hasBook, equalTo(false));
    }
    
    @Test
    public void testWebClientPipedDispatchBookId() throws Exception {
        WebClient localClient = WebClient.create("local://books");
        localClient.accept("text/plain");

        localClient.path("bookstore/books/check/uuid/a6f7357f-6e7e-40e5-9b4a-c455c23b10a2");
        boolean hasBook = localClient.get(boolean.class);
        assertThat(hasBook, equalTo(false));
    }

    @Test
    public void testWebClientDirectDispatchBookType() throws Exception {
        WebClient localClient = WebClient.create("local://books",
                                                 Collections.singletonList(new JacksonJsonProvider()));

        WebClient.getConfig(localClient).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        localClient.path("bookstore/booktype");
        BookType book = localClient.get(BookType.class);
        assertEquals(124L, book.getId());
    }

    @Test
    public void testWebClientPipedDispatch() throws Exception {
        WebClient localClient = WebClient.create("local://books");
        localClient.accept("text/xml");
        localClient.path("bookstore/books");
        Book book = localClient.type("application/xml").post(new Book("New", 124L), Book.class);
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

    private static class TestFaultInInterceptor extends AbstractPhaseInterceptor<Message> {
        TestFaultInInterceptor() {
            super(Phase.PRE_STREAM);
        }

        public void handleMessage(Message message) throws Fault {
            message.getExchange().put(Message.RESPONSE_CODE, 500);
        }

    }
}
