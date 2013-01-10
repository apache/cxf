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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import javax.xml.ws.Holder;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRS20ClientServerBookTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServer20.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServer20.class, true));
    }
    
    @Test
    public void testGetBook() {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple";
        doTestGetBook(address);
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
    public void testGetBookWrongPath() {
        String address = "http://localhost:" + PORT + "/wrongpath";
        doTestGetBook(address);
    }
    @Test
    public void testGetBookWrongPathAsync() throws Exception {
        String address = "http://localhost:" + PORT + "/wrongpath";
        doTestGetBookAsync(address, false);
    }
    
    private void doTestGetBook(String address) {
        WebClient wc = createWebClient(address);
        Book book = wc.get(Book.class);
        assertEquals(124L, book.getId());
        validateResponse(wc);
    }
    
    private WebClient createWebClient(String address) {
        List<Object> providers = new ArrayList<Object>();
        providers.add(new ClientHeaderRequestFilter());
        providers.add(new ClientHeaderResponseFilter());
        WebClient wc = WebClient.create(address, providers);
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(1000000L);
        return wc;
    }
    
    private WebClient createWebClientPost(String address) {
        List<Object> providers = new ArrayList<Object>();
        providers.add(new ClientHeaderRequestFilter());
        providers.add(new ClientHeaderResponseFilter());
        providers.add(new ClientReaderInterceptor());
        providers.add(new ClientWriterInterceptor());
        WebClient wc = WebClient.create(address, providers);
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(1000000L);
        return wc;
    }
    
    private void doTestGetBookAsync(String address, boolean asyncInvoker) 
        throws InterruptedException, ExecutionException {
        
        WebClient wc = createWebClient(address);
        
        final Holder<Book> holder = new Holder<Book>();
        final InvocationCallback<Book> callback = new InvocationCallback<Book>() {
            public void completed(Book response) {
                holder.value = response;
            }
            public void failed(Throwable error) {
            }
        };
        
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
        validatePostResponse(wc);   
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
        assertEquals("custom", response.getHeaderString("Custom"));
        assertEquals("simple", response.getHeaderString("Simple"));
        assertEquals("serverWrite", response.getHeaderString("ServerWriterInterceptor"));
        assertEquals("http://localhost/redirect", response.getHeaderString(HttpHeaders.LOCATION));
    }
    
    private void validatePostResponse(WebClient wc) {
        validateResponse(wc);
        Response response = wc.getResponse();
        assertEquals("serverRead", response.getHeaderString("ServerReaderInterceptor"));
        assertEquals("clientWrite", response.getHeaderString("ClientWriterInterceptor"));
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
        validatePostResponse(wc);
    }
    
    @Test
    public void testPostBookAsync() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple";
        WebClient wc = createWebClientPost(address);
        Future<Book> future = wc.async().post(Entity.xml(new Book("Book", 126L)), Book.class);
        assertEquals(124L, future.get().getId());
        validatePostResponse(wc);
    }
    
    @Test
    public void testPostBookAsyncHandler() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple";
        doTestPostBookAsyncHandler(address);
    }
    
    private static class ClientCacheRequestFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext context) throws IOException {
            context.abortWith(Response.status(201).entity(context.getEntity()).build());
        }
    }
    
    private static class ClientHeaderRequestFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext context) throws IOException {
            context.getHeaders().putSingle("Simple", "simple");
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
            context.getHeaders().add("ClientReaderInterceptor", "clientRead");
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
}
