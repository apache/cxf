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
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.systest.jaxrs.BookStore.BookInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple";
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
    public void testGetBookSpec() {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple";
        Client client = ClientBuilder.newClient();
        client.register((Object)ClientFilterClientAndConfigCheck.class);
        client.property("clientproperty", "somevalue");
        Book book = client.target(address).request("application/xml").get(Book.class);
        assertEquals(124L, book.getId());
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
    public void testGetBookWebTargetProvider() {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders";
        Client client = ClientBuilder.newClient();
        client.register(new BookInfoReader());
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
        assertNull(response.getHeaderString("DynamicResponse"));
        assertNull(response.getHeaderString("Custom"));
        assertEquals("serverWrite", response.getHeaderString("ServerWriterInterceptor"));
        assertEquals("serverWrite2", response.getHeaderString("ServerWriterInterceptor2"));
        assertEquals("serverWriteHttpResponse", 
                     response.getHeaderString("ServerWriterInterceptorHttpResponse"));
        assertEquals("text/plain;charset=us-ascii", response.getMediaType().toString());
    }
    
    @Test
    public void testPostMatchContainerFilterThrowsException() {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple?throwException";
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
    public void testPostCollectionGenericEntity() throws Exception {
        
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/collections3"; 
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xml").type("application/xml");
        
        GenericEntity<List<Book>> collectionEntity = createGenericEntity();
        final Holder<Book> holder = new Holder<Book>();
        InvocationCallback<Book> callback = createCallback(holder);        
            
        Future<Book> future = wc.post(collectionEntity, callback);
        Book book = future.get();
        assertEquals(200, wc.getResponse().getStatus());
        assertSame(book, holder.value);
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
        final Holder<Book> holder = new Holder<Book>();
        InvocationCallback<Book> callback = 
            new GenericInvocationCallback<Book>(holder) { };        
            
        Future<Book> future = wc.post(collectionEntity, callback);
        Book book = future.get();
        assertEquals(200, wc.getResponse().getStatus());
        assertSame(book, holder.value);
        assertNotSame(collectionEntity.getEntity().get(0), book);
        assertEquals(collectionEntity.getEntity().get(0).getName(), book.getName());
    }
    
    @Test
    public void testPostCollectionGenericEntityAsEntity() throws Exception {
        
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/collections3"; 
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xml");
        
        GenericEntity<List<Book>> collectionEntity = createGenericEntity();
        
        final Holder<Book> holder = new Holder<Book>();
        InvocationCallback<Book> callback = createCallback(holder);        
            
        Future<Book> future = wc.async().post(Entity.entity(collectionEntity, "application/xml"),
                                              callback);
        Book book = future.get();
        assertEquals(200, wc.getResponse().getStatus());
        assertSame(book, holder.value);
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
    public void testPostReplaceBookMistypedCT() throws Exception {
        
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/books2"; 
        WebClient wc = WebClient.create(endpointAddress,
                                        Collections.singletonList(new ReplaceBodyFilter()));
        wc.accept("text/mistypedxml").type("text/xml");
        Book book = wc.post(new Book("book", 555L), Book.class);
        assertEquals(561L, book.getId());
    }
    
    @Test
    public void testReplaceBookMistypedCTAndHttpVerb() throws Exception {
        
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/books2/mistyped"; 
        WebClient wc = WebClient.create(endpointAddress,
                                        Collections.singletonList(new ReplaceBodyFilter()));
        wc.accept("text/mistypedxml").type("text/xml").header("THEMETHOD", "PUT");
        Book book = wc.invoke("DELETE", new Book("book", 555L), Book.class);
        assertEquals(561L, book.getId());
    }
    @Test
    public void testReplaceBookMistypedCTAndHttpVerb2() throws Exception {
        
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/books2/mistyped"; 
        WebClient wc = WebClient.create(endpointAddress,
                                        Collections.singletonList(new ReplaceBodyFilter()));
        wc.accept("text/mistypedxml").header("THEMETHOD", "PUT");
        Book book = wc.invoke("GET", null, Book.class);
        assertEquals(561L, book.getId());
    }
    
    @Test
    public void testPostGetCollectionGenericEntityAndType() throws Exception {
        
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/collections"; 
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xml").type("application/xml");
        GenericEntity<List<Book>> collectionEntity = createGenericEntity();
        final Holder<List<Book>> holder = new Holder<List<Book>>();
        InvocationCallback<List<Book>> callback = new CustomInvocationCallback(holder);
            
        Future<List<Book>> future = wc.async().post(Entity.entity(collectionEntity, "application/xml"),
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
    
    private GenericEntity<List<Book>> createGenericEntity() {
        Book b1 = new Book("CXF in Action", 123L);
        Book b2 = new Book("CXF Rocks", 124L);
        List<Book> books = new ArrayList<Book>();
        books.add(b1);
        books.add(b2);
        return new GenericEntity<List<Book>>(books) {
            };
    }
    
    private InvocationCallback<Book> createCallback(final Holder<Book> holder) {
        return new InvocationCallback<Book>() {
            public void completed(Book response) {
                holder.value = response;
            }
            public void failed(Throwable error) {
                error.printStackTrace();
            }
        };
    }
    
    
    private static class CustomInvocationCallback implements InvocationCallback<List<Book>> {
        private Holder<List<Book>> holder;
        public CustomInvocationCallback(Holder<List<Book>> holder) {
            this.holder = holder;
        }
        
        @Override
        public void completed(List<Book> books) {
            holder.value = books;
            
        }

        @Override
        public void failed(Throwable arg0) {
            // TODO Auto-generated method stub
            
        }
        
    }
    private static class GenericInvocationCallback<T> implements InvocationCallback<T> {
        private Holder<T> holder;
        public GenericInvocationCallback(Holder<T> holder) {
            this.holder = holder;
        }
        
        @Override
        public void completed(T book) {
            holder.value = book;
            
        }

        @Override
        public void failed(Throwable arg0) {
            // TODO Auto-generated method stub
            
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
    
    private WebClient createWebClient(String address) {
        List<Object> providers = new ArrayList<Object>();
        providers.add(new ClientHeaderRequestFilter());
        providers.add(new ClientHeaderResponseFilter());
        return WebClient.create(address, providers);
    }
    
    private WebClient createWebClientPost(String address) {
        List<Object> providers = new ArrayList<Object>();
        providers.add(new ClientHeaderRequestFilter());
        providers.add(new ClientHeaderResponseFilter());
        providers.add(new ClientReaderInterceptor());
        providers.add(new ClientWriterInterceptor());
        return WebClient.create(address, providers);
    }
    
    private void doTestGetBookAsync(String address, boolean asyncInvoker) 
        throws InterruptedException, ExecutionException {
        
        WebClient wc = createWebClient(address);
        
        final Holder<Book> holder = new Holder<Book>();
        InvocationCallback<Book> callback = createCallback(holder);
        
        Future<Book> future = asyncInvoker ? wc.async().get(callback) : wc.get(callback);
        Book book = future.get();
        assertSame(book, holder.value);
        assertEquals(124L, book.getId());
        validateResponse(wc);   
    }
    
    private void doTestPostBookAsyncHandler(String address) 
        throws InterruptedException, ExecutionException {
        
        WebClient wc = createWebClientPost(address);
        
        final Holder<Book> holder = new Holder<Book>();
        final InvocationCallback<Book> callback = new InvocationCallback<Book>() {
            public void completed(Book response) {
                holder.value = response;
            }
            public void failed(Throwable error) {
            }
        };
        
        Future<Book> future = wc.post(new Book("async", 126L), callback);
        Book book = future.get();
        assertSame(book, holder.value);
        assertEquals(124L, book.getId());
        validatePostResponse(wc, true, false);   
    }
    
    private void doTestGetBookAsyncResponse(String address, boolean asyncInvoker) 
        throws InterruptedException, ExecutionException {
        
        WebClient wc = createWebClient(address);
        wc.accept(MediaType.APPLICATION_XML_TYPE);
        
        final Holder<Response> holder = new Holder<Response>();
        final InvocationCallback<Response> callback = new InvocationCallback<Response>() {
            public void completed(Response response) {
                holder.value = response;
            }
            public void failed(Throwable error) {
            }
        };
        
        Future<Response> future = asyncInvoker ? wc.async().get(callback) : wc.get(callback);
        Book book = future.get().readEntity(Book.class);
        assertEquals(124L, book.getId());
        validateResponse(wc);   
    }
    
    private void validateResponse(WebClient wc) {
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
    
    private void validatePostResponse(WebClient wc, boolean async, boolean bodyEmpty) {
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
        List<Object> providers = new ArrayList<Object>();
        providers.add(new ClientCacheRequestFilter());
        providers.add(new ClientHeaderResponseFilter());
        WebClient wc = WebClient.create(address, providers);
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
        List<JAXBElement<Book>> books = 
            new ArrayList<JAXBElement<Book>>();
        books.add(new JAXBElement<Book>(new QName("bookRootElement"), 
            Book.class, b1));
        books.add(new JAXBElement<Book>(new QName("bookRootElement"), 
            Book.class, b2));
        
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
    
    private static class ReplaceBodyFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext rc) throws IOException {
            String method = rc.getMethod();
            String expectedMethod = null; 
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
    
    private static class ClientCacheRequestFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext context) throws IOException {
            context.abortWith(Response.status(201).entity(context.getEntity()).type(MediaType.TEXT_XML_TYPE).build());
        }
    }
    
    private static class ClientHeaderRequestFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext context) throws IOException {
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

        @Override
        public void filter(ClientRequestContext reqContext, 
                           ClientResponseContext respContext) throws IOException {
            respContext.getHeaders().putSingle(HttpHeaders.LOCATION, "http://localhost/redirect");
            
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
    
    private static class BookInfoReader implements MessageBodyReader<BookInfo> {

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
}
