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

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import javax.xml.namespace.QName;

import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;

import jakarta.annotation.Priority;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.client.ResponseProcessingException;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.ws.Holder;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.systest.jaxrs.BookStore.BookInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JAXRS20ClientServerBookTest extends AbstractBusClientServerTestBase {

    public static final String PORT = BookServer20.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServer20.class, true));
    }

    @Before
    public void setUp() throws Exception {
        String property = System.getProperty("test.delay");
        if (property != null) {
            Thread.sleep(Long.valueOf(property));
        }
    }

    @Test
    public void testEchoBookElement() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        JAXBElement<Book> element = store.echoBookElement(new JAXBElement<Book>(new QName("", "Book"),
                                     Book.class,
                                     new Book("CXF", 123L)));
        Book book = element.getValue();
        assertEquals(123L, book.getId());
        assertEquals("CXF", book.getName());

        Book book2 = store.echoBookElement(new Book("CXF3", 128L));
        assertEquals(130L, book2.getId());
        assertEquals("CXF3", book2.getName());
    }

    @Test
    public void testListOfLongAndDoubleQuery() throws Exception {
        WebTarget echoEndpointTarget = ClientBuilder
            .newClient()
            .target("http://localhost:" + PORT + "/bookstore/listoflonganddouble")
            .queryParam("value", 1, 0, 2, 3);

        Book book = echoEndpointTarget.request().accept("text/xml").get(Book.class);
        assertEquals(1023L, book.getId());
    }

    @Test
    public void testGetGenericBookManyClientsInParallel() throws InterruptedException {
        final ExecutorService pool = Executors.newFixedThreadPool(100);
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        final AtomicLong httpClientThreads = new AtomicLong(); 

        final Supplier<Long> captureHttpClientThreads = () ->
            Arrays
                .stream(threadMXBean.getAllThreadIds())
                .mapToObj(id -> threadMXBean.getThreadInfo(id))
                .filter(Objects::nonNull)
                .filter(t -> t.getThreadName().startsWith("HttpClient-"))
                .filter(t -> t.getThreadName().endsWith("-SelectorManager"))
                .count();

        // Capture the number of client threads at start
        final long expectedHttpClientThreads = captureHttpClientThreads.get();
        final Collection<WebClient> clients = new ArrayList<>();
        try {
            final String target = "http://localhost:" + PORT + "/bookstore/genericbooks/123";

            for (int i = 0; i < 1000; ++i) {
                final WebClient client = WebClient.create(target, true);
                clients.add(client);

                // We are not checking the future completion, but the fact we were able 
                // to execute this amount of requests without blowing live threads set 
                pool.submit(() -> {
                    Book book = client.sync().get(Book.class);
                    assertEquals(124L, book.getId());

                    // Capture all "HttpClient-" selector threads
                    httpClientThreads.accumulateAndGet(captureHttpClientThreads.get(),
                        (x1, x2) -> x1 > x2 ? x1 : x2);
                });

                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
            }
        } finally {
            pool.shutdown();
            assertThat(pool.awaitTermination(1, TimeUnit.MINUTES), is(true));
            assertThat(captureHttpClientThreads.get(), greaterThan(0L));
            assertThat(httpClientThreads.get(), greaterThan(0L));
            assertThat(httpClientThreads.get(), lessThan(150L)); /* a bit higher that pool size */
        }

        clients.forEach(WebClient::close);
    
        // Since JDK-21, HttpClient Implements AutoCloseable
        if (Runtime.version().feature() >= 21) { 
            assertThat(captureHttpClientThreads.get(), lessThanOrEqualTo(expectedHttpClientThreads));
        }
    }

    @Test
    public void testGetGenericBookSingleClientInParallel() throws InterruptedException {
        final ExecutorService pool = Executors.newFixedThreadPool(100);
        final String target = "http://localhost:" + PORT + "/bookstore/genericbooks/123";
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        final Supplier<Long> captureHttpClientThreads = () ->
            Arrays
                .stream(threadMXBean.getAllThreadIds())
                .mapToObj(id -> threadMXBean.getThreadInfo(id))
                .filter(Objects::nonNull)
                .filter(t -> t.getThreadName().startsWith("HttpClient-"))
                .filter(t -> t.getThreadName().endsWith("-SelectorManager"))
                .count();

        // Capture the number of client threads at start
        final long expectedHttpClientThreads = captureHttpClientThreads.get();
        final WebClient client = WebClient.create(target, true);
        try {
            final Collection<Future<?>> futures = new ArrayList<>(); 
            for (int i = 0; i < 100; ++i) {
                // We are not checking the future completion, but the fact we were able 
                // to execute this amount of requests without blowing live threads set 
                futures.add(
                    pool.submit(() -> {
                        Book b1 = client.sync().get(Book.class);
                        assertEquals(124L, b1.getId());
                    })
                );
            }

            // Find any completed future to make sure conduit was initialized
            while (true) {
                if (futures.stream().anyMatch(Future::isDone)) {
                    break;
                }
            }
        } finally {
            client.close();
        }

        pool.shutdown();
        // Since JDK-21, HttpClient Implements AutoCloseable
        if (pool.awaitTermination(2, TimeUnit.MINUTES) && Runtime.version().feature() >= 21) {
            assertThat(captureHttpClientThreads.get(), lessThanOrEqualTo(expectedHttpClientThreads));
        } else {
            pool.shutdownNow();
            // Since JDK-21, HttpClient Implements AutoCloseable
            if (pool.awaitTermination(2, TimeUnit.MINUTES) && Runtime.version().feature() >= 21) {
                assertThat(captureHttpClientThreads.get(), lessThanOrEqualTo(expectedHttpClientThreads));
            }
        }
    }

    @Test
    public void testGetGenericBook() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/genericbooks/123";
        doTestGetGenericBook(address, 124L, false);
    }

    @Test
    public void testGetGenericBook2() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/genericbooks2/123";
        doTestGetGenericBook(address, 123L, true);
    }

    private void doTestGetGenericBook(String address, long bookId, boolean checkAnnotations)
        throws Exception {
        WebClient wc = WebClient.create(address);
        wc.accept("application/xml");
        Book book = wc.get(Book.class);
        assertEquals(bookId, book.getId());
        MediaType mt = wc.getResponse().getMediaType();
        assertEquals("application/xml;charset=ISO-8859-1", mt.toString());
        if (checkAnnotations) {
            assertEquals("OK", wc.getResponse().getHeaderString("Annotations"));
        } else {
            assertNull(wc.getResponse().getHeaderString("Annotations"));
        }
    }

    @Test
    public void testGetBook() {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple?a=b";
        doTestGetBook(address, false);
    }

    @Test
    public void testGetBookSyncLink() {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple";
        WebClient wc = createWebClient(address);
        Book book = wc.sync().get(Book.class);
        assertEquals(124L, book.getId());
        validateResponse(wc);
    }

    @Test
    public void testGetBookSpecTemplate() {
        String address = "http://localhost:" + PORT + "/bookstore/{a}";
        Client client = ClientBuilder.newClient();
        client.register((Object)ClientFilterClientAndConfigCheck.class);
        client.register(new BTypeParamConverterProvider());
        client.property("clientproperty", "somevalue");
        WebTarget webTarget = client.target(address).path("{b}")
            .resolveTemplate("a", "bookheaders").resolveTemplate("b", "simple");
        Invocation.Builder builder = webTarget.request("application/xml").header("a", new BType());

        Response r = builder.get();
        Book book = r.readEntity(Book.class);
        assertEquals(124L, book.getId());
        assertEquals("b", r.getHeaderString("a"));
    }
    @Test
    public void testGetBookSpec() {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple";
        Client client = ClientBuilder.newClient();
        client.register((Object)ClientFilterClientAndConfigCheck.class);
        client.register(new BTypeParamConverterProvider());
        client.property("clientproperty", "somevalue");
        WebTarget webTarget = client.target(address);
        Invocation.Builder builder = webTarget.request("application/xml").header("a", new BType());

        Response r = builder.get();
        Book book = r.readEntity(Book.class);
        assertEquals(124L, book.getId());
        assertEquals("b", r.getHeaderString("a"));
    }

    @Test
    public void testGetBookSpecProvider() {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple";
        Client client = ClientBuilder.newClient();
        client.register(new BookInfoReader());
        WebTarget target = client.target(address);
        BookInfo book = target.request("application/xml").get(BookInfo.class);
        assertEquals(124L, book.getId());
        book = target.request("application/xml").get(BookInfo.class);
        assertEquals(124L, book.getId());
    }
    @Test
    public void testGetBookSpecProviderWithFeature() {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple";
        Client client = ClientBuilder.newClient();
        client.register(new ClientTestFeature());
        WebTarget target = client.target(address);
        BookInfo book = target.request("application/xml").get(BookInfo.class);
        assertEquals(124L, book.getId());
        book = target.request("application/xml").get(BookInfo.class);
        assertEquals(124L, book.getId());
    }

    @Test
    public void testGetBookWebTargetProvider() {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders";
        Client client = ClientBuilder.newClient();
        client.register(new BookInfoReader());
        BookInfo book = client.target(address).path("simple")
            .request("application/xml").get(BookInfo.class);
        assertEquals(124L, book.getId());

    }
    
    @Test
    public void testGetBookWebTargetInjectableProvider() {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders";
        Client client = ClientBuilder.newClient();
        client.register(new BookInfoInjectableReader());
        BookInfo book = client.target(address).path("simple")
            .request("application/xml").get(BookInfo.class);
        assertEquals(124L, book.getId());

    }

    @Test
    public void testGetBookSyncWithAsync() {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple";
        doTestGetBook(address, true);
    }

    @Test
    public void testGetBookAsync() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple";
        doTestGetBookAsync(address, false);
    }

    @Test
    public void testGetBookAsyncNoCallback() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple";
        WebClient wc = createWebClient(address);
        Future<Book> future = wc.async().get(Book.class);
        Book book = future.get();
        assertEquals(124L, book.getId());
        validateResponse(wc);
    }

    @Test
    public void testGetBookAsyncResponse() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple";
        doTestGetBookAsyncResponse(address, false);
    }

    @Test
    public void testGetBookAsyncInvoker() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple";
        doTestGetBookAsync(address, true);
    }

    @Test
    public void testPreMatchContainerFilterThrowsException() {
        String address = "http://localhost:" + PORT + "/throwException";
        WebClient wc = WebClient.create(address);
        Response response = wc.get();
        assertEquals(500, response.getStatus());
        assertEquals("Prematch filter error", response.readEntity(String.class));
        assertEquals("prematch", response.getHeaderString("FilterException"));
        assertEquals("OK", response.getHeaderString("Response"));
        assertEquals("OK2", response.getHeaderString("Response2"));
        assertNull(response.getHeaderString("IOException"));
        assertNull(response.getHeaderString("DynamicResponse"));
        assertNull(response.getHeaderString("Custom"));
        assertEquals("serverWrite", response.getHeaderString("ServerWriterInterceptor"));
        assertEquals("serverWrite2", response.getHeaderString("ServerWriterInterceptor2"));
        assertEquals("serverWriteHttpResponse",
                     response.getHeaderString("ServerWriterInterceptorHttpResponse"));
        assertEquals("text/plain;charset=us-ascii", response.getMediaType().toString());
    }

    @Test
    public void testPreMatchContainerFilterThrowsIOException() {
        String address = "http://localhost:" + PORT + "/throwExceptionIO";
        WebClient wc = WebClient.create(address);
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(1000000L);
        Response response = wc.get();
        assertEquals(500, response.getStatus());
        assertEquals("Prematch filter error", response.readEntity(String.class));
        assertEquals("prematch", response.getHeaderString("FilterException"));
        assertEquals("OK", response.getHeaderString("Response"));
        assertEquals("OK2", response.getHeaderString("Response2"));
        assertNull(response.getHeaderString("DynamicResponse"));
        assertNull(response.getHeaderString("Custom"));
        assertEquals("true", response.getHeaderString("IOException"));
        assertEquals("serverWrite", response.getHeaderString("ServerWriterInterceptor"));
        assertEquals("serverWrite2", response.getHeaderString("ServerWriterInterceptor2"));
        assertEquals("serverWriteHttpResponse",
                     response.getHeaderString("ServerWriterInterceptorHttpResponse"));
        assertEquals("text/plain;charset=us-ascii", response.getMediaType().toString());
    }

    @Test
    public void testPostMatchContainerFilterThrowsException() {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple?throwException=true";
        WebClient wc = WebClient.create(address);
        Response response = wc.get();
        assertEquals(500, response.getStatus());
        assertEquals("Postmatch filter error", response.readEntity(String.class));
        assertEquals("postmatch", response.getHeaderString("FilterException"));
        assertEquals("OK", response.getHeaderString("Response"));
        assertEquals("OK2", response.getHeaderString("Response2"));
        assertEquals("Dynamic", response.getHeaderString("DynamicResponse"));
        assertEquals("custom", response.getHeaderString("Custom"));
        assertEquals("serverWrite", response.getHeaderString("ServerWriterInterceptor"));
        assertEquals("text/plain;charset=us-ascii", response.getMediaType().toString());
    }

    @Test
    public void testGetBookWrongPath() {
        String address = "http://localhost:" + PORT + "/wrongpath";
        doTestGetBook(address, false);
    }
    @Test
    public void testGetBookWrongPathAsync() throws Exception {
        String address = "http://localhost:" + PORT + "/wrongpath";
        doTestGetBookAsync(address, false);
    }
    
    @Test
    public void testGetBookAbsolutePathAsync() throws Exception {
        String address = "http://localhost:" + PORT + "/absolutepath";
        doTestGetBookAsync(address, false);
    }

    @Test
    public void testPostCollectionGenericEntity() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/collections3";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xml").type("application/xml");

        GenericEntity<List<Book>> collectionEntity = createGenericEntity();
        BookInvocationCallback callback = new BookInvocationCallback();

        Future<Book> future = wc.post(collectionEntity, callback);
        Book book = future.get();
        assertEquals(200, wc.getResponse().getStatus());
        assertSame(book, callback.value());
        assertNotSame(collectionEntity.getEntity().get(0), book);
        assertEquals(collectionEntity.getEntity().get(0).getName(), book.getName());
    }
    @Test
    public void testPostCollectionGenericEntityGenericCallback() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/collections3";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xml").type("application/xml");

        GenericEntity<List<Book>> collectionEntity = createGenericEntity();
        GenericInvocationCallback<Book> callback = new GenericInvocationCallback<Book>(new Holder<>()) {
        };

        Future<Book> future = wc.post(collectionEntity, callback);
        Book book = future.get();
        assertEquals(200, wc.getResponse().getStatus());
        assertSame(book, callback.value());
        assertNotSame(collectionEntity.getEntity().get(0), book);
        assertEquals(collectionEntity.getEntity().get(0).getName(), book.getName());
    }

    @Test
    public void testPostCollectionGenericEntityAsEntity() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/collections3";
        WebClient wc = WebClient.create(endpointAddress)
            .accept("application/xml");

        GenericEntity<List<Book>> collectionEntity = createGenericEntity();

        BookInvocationCallback callback = new BookInvocationCallback();

        Future<Book> future = wc.async().post(Entity.entity(collectionEntity, "application/xml"),
                                              callback);
        Book book = future.get();
        assertEquals(200, wc.getResponse().getStatus());
        assertSame(book, callback.value());
        assertNotSame(collectionEntity.getEntity().get(0), book);
        assertEquals(collectionEntity.getEntity().get(0).getName(), book.getName());
    }

    @Test
    public void testPostReplaceBook() throws Exception {

        String endpointAddress = "http://localhost:" + PORT + "/bookstore/books2";
        WebClient wc = WebClient.create(endpointAddress,
                                        Collections.singletonList(new ReplaceBodyFilter()));
        wc.accept("text/xml").type("application/xml");
        Book book = wc.post(new Book("book", 555L), Book.class);
        assertEquals(561L, book.getId());
    }

    @Test
    public void testPostReplaceBookMistypedAT() throws Exception {

        String endpointAddress = "http://localhost:" + PORT + "/bookstore/books2";
        WebClient wc = WebClient.create(endpointAddress,
                                        Collections.singletonList(new ReplaceBodyFilter()));
        wc.accept("text/mistypedxml").type("text/xml");
        Book book = wc.post(new Book("book", 555L), Book.class);
        assertEquals(561L, book.getId());
    }

    @Test
    public void testReplaceBookMistypedATAndHttpVerb() throws Exception {

        String endpointAddress = "http://localhost:" + PORT + "/bookstore/books2/mistyped";
        WebClient wc = WebClient.create(endpointAddress,
                                        Collections.singletonList(new ReplaceBodyFilter()));
        wc.accept("text/mistypedxml").type("text/xml").header("THEMETHOD", "PUT");
        Book book = wc.invoke("DELETE", new Book("book", 555L), Book.class);
        assertEquals(561L, book.getId());
    }
    @Test
    public void testReplaceBookMistypedATAndHttpVerb2() throws Exception {

        String endpointAddress = "http://localhost:" + PORT + "/bookstore/books2/mistyped";
        WebClient wc = WebClient.create(endpointAddress,
                                        Collections.singletonList(new ReplaceBodyFilter()));
        wc.accept("text/mistypedxml").header("THEMETHOD", "PUT");
        Book book = wc.invoke("GET", null, Book.class);
        assertEquals(561L, book.getId());
    }

    @Test
    public void testPostGetCollectionGenericEntityAndTypeXml() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/collections";
        WebClient wc = WebClient.create(endpointAddress);
        doTestPostGetCollectionGenericEntityAndType(wc, MediaType.APPLICATION_XML_TYPE);
    }
    @Test
    public void testPostGetCollectionGenericEntityAndTypeJson() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/collections";
        WebClient wc = WebClient.create(endpointAddress,
                                        Collections.singletonList(new JacksonXmlBindJsonProvider()));
        doTestPostGetCollectionGenericEntityAndType(wc, MediaType.APPLICATION_JSON_TYPE);
    }

    private void doTestPostGetCollectionGenericEntityAndType(WebClient wc, MediaType mediaType) throws Exception {

        wc.accept(mediaType).type(mediaType);
        GenericEntity<List<Book>> collectionEntity = createGenericEntity();
        InvocationCallback<List<Book>> callback = new ListBookInvocationCallback();

        Future<List<Book>> future = wc.async().post(Entity.entity(collectionEntity, mediaType),
                                                    callback);

        List<Book> books2 = future.get();
        assertNotNull(books2);

        List<Book> books = collectionEntity.getEntity();
        assertNotSame(books, books2);
        assertEquals(2, books2.size());
        Book b11 = books.get(0);
        assertEquals(123L, b11.getId());
        assertEquals("CXF in Action", b11.getName());
        Book b22 = books.get(1);
        assertEquals(124L, b22.getId());
        assertEquals("CXF Rocks", b22.getName());
        assertEquals(200, wc.getResponse().getStatus());
    }

    @Test
    public void testPostGetCollectionGenericEntityAndType2() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/collections";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xml").type("application/xml");
        GenericEntity<List<Book>> collectionEntity = createGenericEntity();
        GenericType<List<Book>> genericResponseType = new GenericType<List<Book>>() {
        };

        Future<List<Book>> future = wc.async().post(Entity.entity(collectionEntity, "application/xml"),
                                                    genericResponseType);

        List<Book> books2 = future.get();
        assertNotNull(books2);

        List<Book> books = collectionEntity.getEntity();
        assertNotSame(books, books2);
        assertEquals(2, books2.size());
        Book b11 = books.get(0);
        assertEquals(123L, b11.getId());
        assertEquals("CXF in Action", b11.getName());
        Book b22 = books.get(1);
        assertEquals(124L, b22.getId());
        assertEquals("CXF Rocks", b22.getName());
        assertEquals(200, wc.getResponse().getStatus());
    }

    @Test
    public void testPostGetCollectionGenericEntityAndType3() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/collections";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xml").type("application/xml");
        GenericEntity<List<Book>> collectionEntity = createGenericEntity();
        GenericType<List<Book>> genericResponseType = new GenericType<List<Book>>() {
        };

        Future<Response> future = wc.async().post(Entity.entity(collectionEntity, "application/xml"));

        Response r = future.get();
        List<Book> books2 = r.readEntity(genericResponseType);
        assertNotNull(books2);

        List<Book> books = collectionEntity.getEntity();
        assertNotSame(books, books2);
        assertEquals(2, books2.size());
        Book b11 = books.get(0);
        assertEquals(123L, b11.getId());
        assertEquals("CXF in Action", b11.getName());
        Book b22 = books.get(1);
        assertEquals(124L, b22.getId());
        assertEquals("CXF Rocks", b22.getName());
        assertEquals(200, wc.getResponse().getStatus());
    }

    private static GenericEntity<List<Book>> createGenericEntity() {
        return new GenericEntity<List<Book>>(Arrays.asList(
            new Book("CXF in Action", 123L),
            new Book("CXF Rocks", 124L))) { };
    }

    private static class GenericInvocationCallback<T> implements InvocationCallback<T> {
        private Holder<T> holder;
        GenericInvocationCallback(Holder<T> holder) {
            this.holder = holder;
        }

        @Override
        public void completed(T book) {
            holder.value = book;
        }

        @Override
        public void failed(Throwable throwable) {
        }

        public T value() {
            return holder.value;
        }
    }

    private static class BookInvocationCallback extends GenericInvocationCallback<Book> {
        BookInvocationCallback() {
            super(new Holder<Book>());
        }
    }

    private static class ListBookInvocationCallback extends GenericInvocationCallback<List<Book>> {
        ListBookInvocationCallback() {
            super(new Holder<List<Book>>());
        }
    }

    private void doTestGetBook(String address, boolean useAsync) {
        WebClient wc = createWebClient(address);
        if (useAsync) {
            WebClient.getConfig(wc).getRequestContext().put("use.async.http.conduit", true);
        }
        Book book = wc.get(Book.class);
        assertEquals(124L, book.getId());
        validateResponse(wc);
    }

    private static WebClient createWebClient(String address) {
        return WebClient.create(address, Arrays.asList(
            new ClientHeaderRequestFilter(),
            new ClientHeaderResponseFilter()));
    }

    private static WebClient createWebClientPost(String address) {
        return WebClient.create(address, Arrays.asList(
            new ClientHeaderRequestFilter(),
            new ClientHeaderResponseFilter(),
            new ClientReaderInterceptor(),
            new ClientWriterInterceptor()));
    }

    private void doTestGetBookAsync(String address, boolean asyncInvoker)
        throws InterruptedException, ExecutionException {

        WebClient wc = createWebClient(address);

        final BookInvocationCallback callback = new BookInvocationCallback();

        Future<Book> future = asyncInvoker ? wc.async().get(callback) : wc.get(callback);
        Book book = future.get();
        assertSame(book, callback.value());
        assertEquals(124L, book.getId());
        validateResponse(wc);
    }

    private void doTestPostBookAsyncHandler(String address)
        throws InterruptedException, ExecutionException {

        WebClient wc = createWebClientPost(address);

        final BookInvocationCallback callback = new BookInvocationCallback();

        Future<Book> future = wc.post(new Book("async", 126L), callback);
        Book book = future.get();
        assertSame(book, callback.value());
        assertEquals(124L, book.getId());
        validatePostResponse(wc, true, false);
    }

    private void doTestGetBookAsyncResponse(String address, boolean asyncInvoker)
        throws InterruptedException, ExecutionException {

        WebClient wc = createWebClient(address);
        wc.accept(MediaType.APPLICATION_XML_TYPE);

        final InvocationCallback<Response> callback = new GenericInvocationCallback<>(new Holder<>());

        Future<Response> future = asyncInvoker ? wc.async().get(callback) : wc.get(callback);
        Book book = future.get().readEntity(Book.class);
        assertEquals(124L, book.getId());
        validateResponse(wc);
    }

    private static void validateResponse(WebClient wc) {
        Response response = wc.getResponse();
        assertEquals("OK", response.getHeaderString("Response"));
        assertEquals("OK2", response.getHeaderString("Response2"));
        assertEquals("Dynamic", response.getHeaderString("DynamicResponse"));
        assertEquals("Dynamic2", response.getHeaderString("DynamicResponse2"));
        assertEquals("custom", response.getHeaderString("Custom"));
        assertEquals("simple", response.getHeaderString("Simple"));
        assertEquals("serverWrite", response.getHeaderString("ServerWriterInterceptor"));
        assertEquals("application/xml;charset=us-ascii", response.getMediaType().toString());
        assertEquals("http://localhost/redirect", response.getHeaderString(HttpHeaders.LOCATION));
    }

    private static void validatePostResponse(WebClient wc, boolean async, boolean bodyEmpty) {
        validateResponse(wc);
        Response response = wc.getResponse();
        assertEquals(!async ? "serverRead" : "serverReadAsync",
            response.getHeaderString("ServerReaderInterceptor"));
        if (!bodyEmpty) {
            assertEquals("clientWrite", response.getHeaderString("ClientWriterInterceptor"));
        } else {
            assertEquals("true", response.getHeaderString("EmptyRequestStreamDetected"));
        }
        assertEquals("clientRead", response.getHeaderString("ClientReaderInterceptor"));
    }

    @Test
    public void testClientFiltersLocalResponse() {
        String address = "http://localhost:" + PORT + "/bookstores";
        WebClient wc = WebClient.create(address, Arrays.asList(
            new ClientCacheRequestFilter(),
            new ClientHeaderResponseFilter(true)));
        Book theBook = new Book("Echo", 123L);
        Response r = wc.post(theBook);
        assertEquals(201, r.getStatus());
        assertEquals("http://localhost/redirect", r.getHeaderString(HttpHeaders.LOCATION));
        Book responseBook = r.readEntity(Book.class);
        assertSame(theBook, responseBook);
    }

    @Test
    public void testClientFiltersLocalResponseLambdas() {
        String address = "http://localhost:" + PORT + "/bookstores";
        WebClient wc = WebClient.create(address, Arrays.asList(
            (ClientRequestFilter) ctx -> {
                ctx.abortWith(Response.status(201).entity(ctx.getEntity()).type(MediaType.TEXT_XML_TYPE).build());
            },
            (ClientResponseFilter) (reqContext, respContext) -> {
                MultivaluedMap<String, String> headers = respContext.getHeaders();
                headers.putSingle(HttpHeaders.LOCATION, "http://localhost/redirect");
            }));
        Book theBook = new Book("Echo", 123L);
        Response r = wc.post(theBook);
        assertEquals(201, r.getStatus());
        assertEquals("http://localhost/redirect", r.getHeaderString(HttpHeaders.LOCATION));
        Book responseBook = r.readEntity(Book.class);
        assertSame(theBook, responseBook);
    }

    @Test
    public void testPostBook() {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple";
        WebClient wc = createWebClientPost(address);
        Book book = wc.post(new Book("Book", 126L), Book.class);
        assertEquals(124L, book.getId());
        validatePostResponse(wc, false, false);
    }

    @Test
    public void testPostEmptyBook() {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple";
        WebClient wc = createWebClientPost(address);
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(1000000);
        Book book = wc.post(null, Book.class);
        assertEquals(124L, book.getId());
        validatePostResponse(wc, false, true);
    }

    @Test
    public void testPostBookNewMediaType() {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple";
        WebClient wc = createWebClientPost(address);
        wc.header("newmediatype", "application/v1+xml");
        Book book = wc.post(new Book("Book", 126L), Book.class);
        assertEquals(124L, book.getId());
        validatePostResponse(wc, false, false);
        assertEquals("application/v1+xml", wc.getResponse().getHeaderString("newmediatypeused"));
    }

    @Test
    public void testBookExistsServerStreamReplace() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/check2";
        WebClient wc = WebClient.create(address);
        wc.accept("text/plain").type("text/plain");
        assertTrue(wc.post("s", Boolean.class));
    }

    @Test
    public void testBookExistsServerAddressOverwrite() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/checkN";
        WebClient wc = WebClient.create(address);
        wc.accept("text/plain").type("text/plain");
        assertTrue(wc.post("s", Boolean.class));
    }
    @Test
    public void testBookExistsServerAddressOverwriteWithQuery() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/checkNQuery?a=b";
        WebClient wc = WebClient.create(address);
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000);
        wc.accept("text/plain").type("text/plain");
        assertTrue(wc.post("s", Boolean.class));
    }

    @Test
    public void testPostBookAsync() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple/async";
        WebClient wc = createWebClientPost(address);
        Future<Book> future = wc.async().post(Entity.xml(new Book("Book", 126L)), Book.class);
        assertEquals(124L, future.get().getId());
        validatePostResponse(wc, true, false);
    }

    @Test
    public void testPostBookAsyncHandler() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple/async";
        doTestPostBookAsyncHandler(address);
    }

    @Test
    public void testJAXBElementBookCollection() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/jaxbelementxmlrootcollections";
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(address);

        Book b1 = new Book("CXF in Action", 123L);
        Book b2 = new Book("CXF Rocks", 124L);
        List<JAXBElement<Book>> books = Arrays.asList(
            new JAXBElement<Book>(new QName("bookRootElement"), Book.class, b1),
            new JAXBElement<Book>(new QName("bookRootElement"), Book.class, b2));

        GenericEntity<List<JAXBElement<Book>>> collectionEntity =
            new GenericEntity<List<JAXBElement<Book>>>(books) { };
        GenericType<List<JAXBElement<Book>>> genericResponseType =
            new GenericType<List<JAXBElement<Book>>>() { };

        List<JAXBElement<Book>> books2 =
            target.request().accept("application/xml")
            .post(Entity.entity(collectionEntity, "application/xml"), genericResponseType);

        assertNotNull(books2);
        assertNotSame(books, books2);
        assertEquals(2, books2.size());
        Book b11 = books.get(0).getValue();
        assertEquals(123L, b11.getId());
        assertEquals("CXF in Action", b11.getName());
        Book b22 = books.get(1).getValue();
        assertEquals(124L, b22.getId());
        assertEquals("CXF Rocks", b22.getName());
    }

    @Test
    public void testUnknownHostException() throws InterruptedException {
        String address = "http://unknown-host/bookstore/bookheaders/simple/async";
        try {
            doTestPostBookAsyncHandler(address);
            fail("Should fail with UnknownHostException");
        } catch (ExecutionException e) {
            assertTrue("Should fail with UnknownHostException",
                    ExceptionUtils.getRootCause(e) instanceof UnknownHostException);
        }
    }

    @Test
    public void testGetSetEntityStream() {
        String address = "http://localhost:" + PORT + "/bookstore/entityecho";
        String entity = "BOOKSTORE";

        Client client = ClientBuilder.newClient();
        client.register(new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext context) throws IOException {
                context.setEntityStream(new ReplacingOutputStream(
                                 context.getEntityStream(), 'X', 'O'));
            }
        });

        WebTarget target = client.target(address);

        Response response = target.request().post(
                Entity.entity(entity.replace('O', 'X'), "text/plain"));
        assertEquals(entity, response.readEntity(String.class));
    }

    @Test
    public void testGetSetEntityStreamLambda() {
        String address = "http://localhost:" + PORT + "/bookstore/entityecho";
        String entity = "BOOKSTORE";

        Client client = ClientBuilder.newClient();
        client.register((ClientRequestFilter) context -> {
            context.setEntityStream(new ReplacingOutputStream(context.getEntityStream(), 'X', 'O'));
        });

        WebTarget target = client.target(address);

        Response response = target.request().post(
                Entity.entity(entity.replace('O', 'X'), "text/plain"));
        assertEquals(entity, response.readEntity(String.class));
    }

    @Test
    public void testClientResponseFilter() {
        final String address = "http://localhost:" + PORT + "/bookstore/books/wildcard";
        try (Response response = ClientBuilder.newClient()
             .register(AddHeaderClientResponseFilter.class)
             .target(address)
             .request("text/plain")
             .get()) {
            assertEquals(200, response.getStatus());
            assertEquals("true", response.getHeaderString("X-Done"));
        }
    }

    @Test
    public void testExceptionWhenMultipleClientResponseFilters() {
        final String address = "http://localhost:" + PORT + "/bookstore/books/wildcard";
        try (Response response = ClientBuilder.newClient()
             .register(AddHeaderClientResponseFilter.class)
             .register(FaultyClientResponseFilter.class)
             .target(address)
             .request("text/plain")
             .put(null)) {
            fail("Should not be invoked");
        } catch (ResponseProcessingException ex) {
            // Seems to be an issue here, CXF creates the response context only once
            // for all client response filters, the changes performed upstream the chain
            // are not visible to the downstream filters. 
            assertEquals(null, ex.getResponse().getHeaderString("X-Done"));
        } catch (Throwable ex) {
            fail("Should be handled by ResponseProcessingException block");
        }
    }

    @Test(expected = ResponseProcessingException.class)
    public void testExceptionInClientResponseFilter() {
        final String address = "http://localhost:" + PORT + "/bookstore/books/wildcard";
        try (Response response = ClientBuilder.newClient()
             .register(FaultyClientResponseFilter.class)
             .target(address)
             .request("text/plain")
             .get()) {
            fail("Should raise ResponseProcessingException");
        }
    }

    @Test(expected = ResponseProcessingException.class)
    public void testExceptionInClientResponseFilterWhenNotFound() {
        final String address = "http://localhost:" + PORT + "/bookstore/notFound";
        try (Response response = ClientBuilder.newClient()
             .register(FaultyClientResponseFilter.class)
             .target(address)
             .request("text/plain")
             .put(null)) {
            fail("Should not be invoked");
        }
    }

    @Test
    public void testNotFound() throws Exception {
        final String address = "http://localhost:" + PORT + "/bookstore/notFound";
        try (Response response = ClientBuilder.newClient()
             .target(address)
             .request("text/plain")
             .put(null)) {
            assertThat(response.getStatus(), equalTo(404));
        }
    }

    @Test
    @SuppressWarnings({"checkstyle:linelength"})
    public void testQueryParamSpecialCharactersEncoded() throws Exception {
        final String address = "http://localhost:" + PORT + "/bookstore/queryParamSpecialCharacters";

        try (Response response = ClientBuilder.newClient()
                .register(AddHeaderClientResponseFilter.class)
                .target(address)
                .queryParam(URLEncoder.encode("/?abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~%1A!$'()*+,;:@"), 
                    "apiKeyQueryParam1Value")
                .request(MediaType.TEXT_PLAIN)
                .get()) {
            assertEquals(200, response.getStatus());
            
            final String actual = response.readEntity(String.class);
            final String expected = "apiKeyQueryParam1Value";
            
            assertEquals(expected, actual);
        }
    }

    @Test
    public void testGetHeaders() throws Exception {
        final WebTarget target = ClientBuilder
            .newClient()
            .register(JacksonXmlBindJsonProvider.class)
            .target("http://localhost:" + PORT + "/bookstore/headers");

        @SuppressWarnings("unchecked")
        final Map<String, Object> headers = target
            .request().accept("application/json")
            .get(Map.class);

        assertThat(headers, hasKey("Accept"));
        assertThat(headers, not(hasKey("Content-Type")));
    }

    @Test
    public void testGetBookDateAnnotated() throws Exception {
        final String response = ClientBuilder
            .newClient()
            .target("http://localhost:" + PORT + "/bookstore/annotated/123")
            .request(MediaType.TEXT_PLAIN)
            .get()
            .readEntity(String.class);
            
        assertThat(response, equalTo("2020-01-01{jakarta.ws.rs.GET,jakarta.ws.rs.Path,jakarta.ws.rs.Produces,"
            + "jakarta.ws.rs.ext.Provider,jakarta.ws.rs.Consumes}"));
    }
    
    @Test
    public void testGetCookies() throws Exception {
        final WebTarget target = ClientBuilder
            .newClient()
            .property("org.apache.cxf.http.cookie.separator", ";")
            .target("http://localhost:" + PORT + "/bookstore/cookies");

        @SuppressWarnings("unchecked")
        final Response response = target
            .request().accept("application/json")
            .cookie(new Cookie("a", "1"))
            .cookie(new Cookie("b", "2"))
            .get();

        assertThat(response.getHeaderString(HttpHeaders.SET_COOKIE), equalTo("$Version=1;a=1; $Version=1;b=2"));
    }

    private static final class ReplaceBodyFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext rc) throws IOException {
            String method = rc.getMethod();
            final String expectedMethod;
            if (rc.getAcceptableMediaTypes().contains(MediaType.valueOf("text/mistypedxml"))
                && rc.getHeaders().getFirst("THEMETHOD") != null) {
                expectedMethod = MediaType.TEXT_XML_TYPE.equals(rc.getMediaType()) ? "DELETE" : "GET";
                rc.setUri(URI.create("http://localhost:" + PORT + "/bookstore/books2"));
                rc.setMethod(rc.getHeaders().getFirst("THEMETHOD").toString());
                if ("GET".equals(expectedMethod)) {
                    rc.getHeaders().putSingle("Content-Type", "text/xml");
                }
            } else {
                expectedMethod = "POST";
            }


            if (!expectedMethod.equals(method)) {
                throw new RuntimeException();
            }
            if ("GET".equals(expectedMethod)) {
                rc.setEntity(new Book("book", 560L));
            } else {
                rc.setEntity(new Book("book", ((Book)rc.getEntity()).getId() + 5));
            }
        }


    }

    private static final class ClientCacheRequestFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext context) throws IOException {
            context.abortWith(Response.status(201).entity(context.getEntity()).type(MediaType.TEXT_XML_TYPE).build());
        }
    }

    private static final class ClientHeaderRequestFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext context) throws IOException {
            String opName =
                (String)JAXRSUtils.getCurrentMessage().getExchange().get("org.apache.cxf.resource.operation.name");
            assertFalse(opName.endsWith("?a=b"));
            context.getHeaders().putSingle("Simple", "simple");
            if (context.hasEntity()) {
                context.getHeaders().putSingle("Content-Type", MediaType.APPLICATION_XML_TYPE);
            }
        }
    }

    public static class ClientFilterClientAndConfigCheck implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext context) throws IOException {
            String prop = context.getClient().getConfiguration().getProperty("clientproperty").toString();
            String prop2 = context.getConfiguration().getProperty("clientproperty").toString();
            if (!prop2.equals(prop) || !"somevalue".equals(prop2)) {
                throw new RuntimeException();
            }

        }
    }

    private static class ClientHeaderResponseFilter implements ClientResponseFilter {
        private boolean local;
        ClientHeaderResponseFilter() {

        }
        ClientHeaderResponseFilter(boolean local) {
            this.local = local;
        }
        @Override
        public void filter(ClientRequestContext reqContext,
                           ClientResponseContext respContext) throws IOException {
            MultivaluedMap<String, String> headers = respContext.getHeaders();
            if (!local) {
                assertEquals(1, headers.get("Date").size());
            }
            headers.putSingle(HttpHeaders.LOCATION, "http://localhost/redirect");

        }

    }


    public static class ClientReaderInterceptor implements ReaderInterceptor {

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException,
            WebApplicationException {
            if (context.getInputStream() != null) {
                context.getHeaders().add("ClientReaderInterceptor", "clientRead");
            }
            return context.proceed();
        }

    }

    public static class ClientWriterInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            context.getHeaders().add("ClientWriterInterceptor", "clientWrite");
            context.proceed();
        }

    }

    private static final class BookInfoInjectableReader implements MessageBodyReader<BookInfo> {
        @Context private ResourceContext resourceContext;

        @Override
        public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return true;
        }

        @Override
        public BookInfo readFrom(Class<BookInfo> arg0, Type arg1, Annotation[] anns, MediaType mt,
                                 MultivaluedMap<String, String> headers, InputStream is) throws IOException,
            WebApplicationException {
            
            if (resourceContext == null) {
                throw new WebApplicationException("The resourceContext should not be null");
            }
            
            Book book = new JAXBElementProvider<Book>().readFrom(Book.class, Book.class, anns, mt, headers, is);
            return new BookInfo(book);
        }

    }
    
    private static final class BookInfoReader implements MessageBodyReader<BookInfo> {
        @Override
        public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return true;
        }

        @Override
        public BookInfo readFrom(Class<BookInfo> arg0, Type arg1, Annotation[] anns, MediaType mt,
                                 MultivaluedMap<String, String> headers, InputStream is) throws IOException,
            WebApplicationException {
            Book book = new JAXBElementProvider<Book>().readFrom(Book.class, Book.class, anns, mt, headers, is);
            return new BookInfo(book);
        }

    }

    private static final class ClientTestFeature implements Feature {

        @Override
        public boolean configure(FeatureContext context) {
            context.register(new BookInfoReader());
            return true;
        }

    }

    static class BType {
        public String b() {
            return "b";
        }
    }

    static class BTypeParamConverterProvider implements ParamConverterProvider, ParamConverter<BType> {

        @SuppressWarnings("unchecked")
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> cls, Type t, Annotation[] anns) {
            return cls == BType.class ? (ParamConverter<T>)this : null;
        }

        @Override
        public BType fromString(String s) {
            return null;
        }

        @Override
        public String toString(BType bType) {
            return bType.b();
        }

    }
    
    
    @Priority(2)
    public static class AddHeaderClientResponseFilter implements ClientResponseFilter {
        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) 
                throws IOException {
            responseContext.getHeaders().add("X-Done", "true");
        }
    }

    @Priority(1)
    public static class FaultyClientResponseFilter implements ClientResponseFilter {
        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) 
                throws IOException {
            throw new IOException("Exception from client response filter");
        }
    }
}
