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

package org.apache.cxf.systest.hc.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.client.ResponseProcessingException;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.xml.ws.Holder;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JAXRSAsyncClientTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerAsyncClient.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(BookServerAsyncClient.class, true));
        createStaticBus();
    }

    @Test
    public void testRetrieveBookCustomMethodAsyncSync() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/retrieve";
        WebClient wc = createWebClient(address).type("application/xml").accept("application/xml");
        Book book = wc.invoke("RETRIEVE", new Book("Retrieve", 123L), Book.class);
        assertEquals("Retrieve", book.getName());
        wc.close();
    }

    @Test
    public void testPatchBook() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/patch";
        WebClient wc = createWebClient(address).type("application/xml");
        Book book = wc.invoke("PATCH", new Book("Patch", 123L), Book.class);
        assertEquals("Patch", book.getName());
        wc.close();
    }

    @Test
    public void testPatchBookTimeout() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/patch";
        WebClient wc = WebClient.create(address).type("application/xml");
        ClientConfiguration clientConfig = WebClient.getConfig(wc);
        clientConfig.getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, true);
        HTTPClientPolicy clientPolicy = clientConfig.getHttpConduit().getClient();
        clientPolicy.setReceiveTimeout(500);
        clientPolicy.setConnectionTimeout(500);
        try {
            Book book = wc.invoke("PATCH", new Book("Timeout", 123L), Book.class);
            fail("should throw an exception due to timeout, instead got " + book);
        } catch (jakarta.ws.rs.ProcessingException e) {
            //expected!!!
        }
    }

    @Test
    public void testPatchBookInputStream() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/patch";
        WebClient wc = createWebClient(address).type("application/xml");
        Book book = wc.invoke("PATCH",
                              new ByteArrayInputStream(
                                  "<Book><name>Patch</name><id>123</id></Book>".getBytes()),
                              Book.class);
        assertEquals("Patch", book.getName());
        wc.close();
    }

    @Test
    public void testDeleteWithBody() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/deletebody";
        WebClient wc = createWebClient(address).type("application/xml").accept("application/xml");
        Book book = wc.invoke("DELETE", new Book("Delete", 123L), Book.class);
        assertEquals("Delete", book.getName());
        wc.close();
    }

    @Test
    public void testGetWithBody() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/getbody";
        WebClient wc = createWebClient(address).type("application/xml").accept("application/xml");
        try (Response response = wc.invoke("GET", new Book("Get", 123L))) {
            assertEquals(400, response.getStatus());
        }
        wc.close();
    }

    @Test
    public void testRetrieveBookCustomMethodAsync() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/retrieve";
        WebClient wc = createWebClient(address).accept("application/xml");
        Future<Book> book = wc.async().method("RETRIEVE", Entity.xml(new Book("Retrieve", 123L)),
                                              Book.class);
        assertEquals("Retrieve", book.get().getName());
        wc.close();
    }

    @Test
    public void testGetBookAsyncResponseNotFound() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/404";
        WebClient wc = createWebClient(address);
        Future<Response> future = wc.async().get(Response.class);
        assertEquals(404, future.get().getStatus());
        wc.close();
    }

    @Test
    public void testGetBookAsyncUpdate() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/updatebook/124";
        WebClient wc = createWebClient(address);
        Future<Response> future = wc.async().put(Entity.xml(new Book("My CXF Book", 124)));
        final Response response = future.get();
        assertEquals(200, response.getStatus());
        final Book book = response.readEntity(Book.class);
        assertThat(book.getId(), equalTo(124L));
        assertThat(book.getName(), equalTo("Updated My CXF Book"));
        wc.close();
    }

    @Test
    public void testGetBookAsync404() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/404";
        WebClient wc = createWebClient(address);
        Future<Book> future = wc.async().get(Book.class);
        try {
            future.get();
            fail("Exception expected");
        } catch (ExecutionException ex) {
            assertTrue(ex.getCause() instanceof NotFoundException);
        }
        wc.close();
    }

    @Test
    public void testNonExistentHostnameAsync() throws Exception {
        String address = "http://168.168.168.168/bookstore";
        List<Object> providers = new ArrayList<>();
        providers.add(new TestResponseFilter());
        WebClient wc = createWebClient(address, providers);
        Future<Book> future = wc.async().get(Book.class);
        try {
            future.get();
            fail("Exception expected");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            assertTrue(cause instanceof ProcessingException);
            assertTrue(ex.getCause().getCause() instanceof IOException);
        } finally {
            wc.close();
        }
    }
    
    @Test
    public void testNonExistentHostnameGet() throws Exception {
        String address = "http://168.168.168.168/bookstore";
        Client c = ClientBuilder.newClient();
        c.register(new TestResponseFilter());
        WebTarget t1 = c.target(address);
        Future<Response> future = t1.request().async().get();
        try {
            future.get();
            fail("Exception expected");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            assertTrue(cause instanceof ProcessingException);
            assertTrue(ex.getCause().getCause() instanceof IOException);
        } finally {
            c.close();
        }
    }

    @Test
    public void testNonExistentHostnamePost() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://168.168.168.168/");
        Invocation.Builder builder = target.request();
        Entity<String> entity = Entity.entity("entity", MediaType.WILDCARD_TYPE);
        Invocation invocation = builder.buildPost(entity);
        Future<String> future = invocation.submit(
            new GenericType<String>() {
            }
        );

        try {
            future.get();
        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            assertTrue(cause instanceof ProcessingException);
        }
    }

    @Test
    public void testPostBookProcessingException() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/";
        List<Object> providers = new ArrayList<>();
        providers.add(new FaultyBookWriter());
        WebClient wc = createWebClient(address, providers);

        Future<Book> future = wc.async().post(Entity.xml(new Book()), Book.class);
        try {
            future.get();
            fail("Exception expected");
        } catch (ExecutionException ex) {
            assertTrue(ex.getCause() instanceof ProcessingException);
        }
        wc.close();
    }

    @Test
    public void testGetBookResponseProcessingException() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/123";
        List<Object> providers = new ArrayList<>();
        providers.add(new FaultyBookReader());
        WebClient wc = createWebClient(address, providers);

        Future<Book> future = wc.async().get(Book.class);
        try {
            future.get();
            fail("Exception expected");
        } catch (ExecutionException ex) {
            assertTrue(ex.getCause() instanceof ResponseProcessingException);
        }
        wc.close();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testGenericInvocationCallback() throws Exception {
        InvocationCallback<?> callback = createGenericInvocationCallback();
        String address = "http://localhost:" + PORT + "/bookstore/books/check/123";
        Client client = ClientBuilder
            .newBuilder()
            .register(new BookServerAsyncClient.BooleanReaderWriter())
            .build();

        client
            .target(address)
            .request()
            .accept("text/boolean")
            .async()
            .get(callback)
            .get();

        assertTrue(((GenericInvocationCallback)callback).getResult().readEntity(Boolean.class));
    }

    @Test
    public void testAsyncProxyPrimitiveResponse() throws Exception {
        String address = "http://localhost:" + PORT;
        final Holder<Boolean> holder = new Holder<>();
        final InvocationCallback<Boolean> callback = new InvocationCallback<Boolean>() {
            public void completed(Boolean response) {
                holder.value = response;
            }
            public void failed(Throwable error) {
            }
        };

        BookStore store = JAXRSClientFactory.create(address, BookStore.class);
        WebClient.getConfig(store).getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, true);
        WebClient.getConfig(store).getRequestContext().put(InvocationCallback.class.getName(), callback);
        
        store.checkBook(123L);
        Thread.sleep(3000);
        assertTrue(holder.value);
    }
    
    @Test
    public void testAsyncProxyBookResponse() throws Exception {
        String address = "http://localhost:" + PORT;
        final Holder<Book> holder = new Holder<>();
        final InvocationCallback<Book> callback = new InvocationCallback<Book>() {
            public void completed(Book response) {
                holder.value = response;
            }
            public void failed(Throwable error) {
            }
        };

        BookStore store = JAXRSClientFactory.create(address, BookStore.class);
        WebClient.getConfig(store).getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, true);
        WebClient.getConfig(store).getRequestContext().put(InvocationCallback.class.getName(), callback);
        
        Book book = store.getBookByMatrixParams("12", "3");
        assertNull(book);
        Thread.sleep(3000);
        assertNotNull(holder.value);
        assertEquals(123L, holder.value.getId());
    }

    @Test
    public void testAsyncProxyMultipleCallbacks() throws Exception {
        String address = "http://localhost:" + PORT;
        final Holder<Book> bookHolder = new Holder<>();
        final InvocationCallback<Book> bookCallback = new InvocationCallback<Book>() {
            public void completed(Book response) {
                bookHolder.value = response;
            }
            public void failed(Throwable error) {
            }
        };
        final Holder<Boolean> booleanHolder = new Holder<>();
        final InvocationCallback<Boolean> booleanCallback = new InvocationCallback<Boolean>() {
            public void completed(Boolean response) {
                booleanHolder.value = response;
            }
            public void failed(Throwable error) {
            }
        };
        List<InvocationCallback<?>> callbacks = new ArrayList<>();
        callbacks.add(bookCallback);
        callbacks.add(booleanCallback);

        BookStore store = JAXRSClientFactory.create(address, BookStore.class);
        WebClient.getConfig(store).getRequestContext().put(InvocationCallback.class.getName(), callbacks);
        WebClient.getConfig(store).getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, true);

        Book book = store.getBookByMatrixParams("12", "3");
        assertNull(book);
        Thread.sleep(3000);
        assertNotNull(bookHolder.value);
        assertEquals(123L, bookHolder.value.getId());

        store.checkBook(123L);
        Thread.sleep(3000);
        assertTrue(booleanHolder.value);
    }

    @Test
    public void testGetBookAsyncNotFoundCallback() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/404";
        WebClient wc = createWebClient(address);
        final Holder<Object> holder = new Holder<>();
        InvocationCallback<Object> callback = createCallback(holder);
        try {
            wc.async().get(callback).get();
            fail("Exception expected");
        } catch (ExecutionException ex) {
            assertTrue(ex.getCause() instanceof NotFoundException);
            assertTrue(ex.getCause() == holder.value);
        }
        wc.close();
    }

    @Test
    public void testClientResponseFilter() throws Exception {
        final String address = "http://localhost:" + PORT + "/bookstore/books/wildcard";
        try (Response response = ClientBuilder.newClient()
             .register(AddHeaderClientResponseFilter.class)
             .target(address)
             .request("text/plain")
             .async()
             .get()
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
             .request()
             .async()
             .put(null)
             .get(10, TimeUnit.SECONDS)) {
            fail("Should not be invoked");
        } catch (ExecutionException ex) {
            assertThat(ex.getCause(), is(instanceOf(ResponseProcessingException.class)));
        } catch (Throwable ex) {
            fail("Should be handled by ResponseProcessingException block");
        }
    }

    @Test
    public void testExceptionInClientResponseFilter() throws Exception {
        final String address = "http://localhost:" + PORT + "/bookstore/books/wildcard";
        try (Response response = ClientBuilder.newClient()
             .register(FaultyClientResponseFilter.class)
             .target(address)
             .request("text/plain")
             .async()
             .get()
             .get(10, TimeUnit.SECONDS)) {
            fail("Should raise ResponseProcessingException");
        } catch (ExecutionException ex) {
            assertThat(ex.getCause(), is(instanceOf(ResponseProcessingException.class)));
        } catch (Throwable ex) {
            fail("Should be handled by ResponseProcessingException block");
        }
    }

    @Test
    public void testExceptionInClientResponseFilterWhenNotFound() throws Exception {
        final String address = "http://localhost:" + PORT + "/bookstore/notFound";
        try (Response response = ClientBuilder.newClient()
             .register(FaultyClientResponseFilter.class)
             .target(address)
             .request("text/plain")
             .async()
             .put(null)
             .get(10, TimeUnit.SECONDS)) {
            fail("Should not be invoked");
        } catch (ExecutionException ex) {
            assertThat(ex.getCause(), is(instanceOf(ResponseProcessingException.class)));
        } catch (Throwable ex) {
            fail("Should be handled by ResponseProcessingException block");
        }
    }
    
    @Test
    public void testNotFound() throws Exception {
        final String address = "http://localhost:" + PORT + "/bookstore/notFound";
        try (Response response = ClientBuilder.newClient()
             .target(address)
             .request("text/plain")
             .async()
             .put(null)
             .get(10, TimeUnit.SECONDS)) {
            assertThat(response.getStatus(), equalTo(404));
        }
    }

    @Test
    public void testStatusAngHeadersFromStream() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/statusFromStream";
        WebClient wc = createWebClient(address).accept("text/xml");
        Response r = wc.async().get().get();
        assertEquals(503, r.getStatus());
        assertEquals("text/custom+plain", r.getMediaType().toString());
        assertEquals("CustomValue", r.getHeaderString("CustomHeader"));
        assertEquals("Response is not available", r.readEntity(String.class));
    }
    
    @Test
    public void testBookAsStream() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/streamingresponse";
        WebClient wc = createWebClient(address).accept("text/xml");
        Response r = wc.async().get().get();
        assertEquals(200, r.getStatus());
        final Book book = r.readEntity(Book.class);
        assertThat(book.getId(), equalTo(124L));
        assertThat(book.getName(), equalTo("stream"));
    }

    @Test
    public void testSetCookieWebClient() throws Exception {
        final String address = "http://localhost:" + PORT + "/bookstore/setcookies";
        WebClient client = createWebClient(address);
        Response r = client.type("*/*").async().get().get();
        assertEquals(200, r.getStatus());
        List<Object> cookies = r.getMetadata().get("Set-Cookie");
        assertNotNull(cookies);
        assertEquals(1, cookies.size());
    }
    
   
    @Test
    public void testBookNoContent() throws Exception {
        final String address = "http://localhost:" + PORT + "/bookstore/no-content";
        WebClient client = createWebClient(address);
        Response r = client.type("*/*").async().post(null).get();
        assertEquals(204, r.getStatus());
        assertThat(r.readEntity(String.class), equalTo(""));
    }

    @Test
    public void testBookOneway() throws Exception {
        final String address = "http://localhost:" + PORT + "/bookstore/oneway";
        WebClient client = createWebClient(address, new TestResponseFilter());
        Response r = client.type("*/*").async().post(null).get();
        assertEquals(202, r.getStatus());
        assertThat(r.getEntity(), is(nullValue()));
        assertThat(r.getHeaderString("X-Filter"), equalTo("true"));
    }

    private WebClient createWebClient(String address, Object ... providers) {
        final WebClient wc = WebClient.create(address, Arrays.asList(providers));
        WebClient.getConfig(wc).getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, true);
        return wc;
    }

    private InvocationCallback<Object> createCallback(final Holder<Object> holder) {
        return new InvocationCallback<Object>() {
            public void completed(Object response) {
                holder.value = response;
            }
            public void failed(Throwable error) {
                holder.value = error;
            }
        };
    }

    @Produces("application/xml")
    private static final class FaultyBookWriter implements MessageBodyWriter<Book> {
        @Override
        public long getSize(Book arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
            return 0;
        }

        @Override
        public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return true;
        }

        @Override
        public void writeTo(Book arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4,
                MultivaluedMap<String, Object> arg5, OutputStream arg6) throws IOException, WebApplicationException {
            throw new RuntimeException();
        }
    }

    @Consumes("application/xml")
    private static final class FaultyBookReader implements MessageBodyReader<Book> {
        @Override
        public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return true;
        }

        @Override
        public Book readFrom(Class<Book> arg0, Type arg1, Annotation[] arg2, MediaType arg3,
                MultivaluedMap<String, String> arg4, InputStream arg5) throws IOException, WebApplicationException {
            throw new RuntimeException();
        }
    }

    public static class TestResponseFilter implements ClientResponseFilter {
        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
            throws IOException {
            responseContext.getHeaders().add("X-Filter", "true");
        }
    }

    private static final class GenericInvocationCallback<T> implements InvocationCallback<T> {
        private Object result;

        @Override
        public void completed(final Object o) {
            result = o;
        }

        @Override
        public void failed(final Throwable throwable) {
            // complete
        }

        public Response getResult() {
            return (Response)result;
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
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T> InvocationCallback<T> createGenericInvocationCallback() {
        return new GenericInvocationCallback();
    }
}
