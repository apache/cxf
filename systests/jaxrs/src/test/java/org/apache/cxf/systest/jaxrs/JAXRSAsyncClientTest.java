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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Priority;
import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.ws.Holder;

import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
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
        assertTrue("server did not launch correctly",
                   launchServer(BookServerAsyncClient.class, true));
        createStaticBus();
    }

    @Before
    public void setUp() throws Exception {
        String property = System.getProperty("test.delay");
        if (property != null) {
            Thread.sleep(Long.valueOf(property));
        }
    }
    @Test
    public void testRetrieveBookCustomMethodAsyncSync() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/retrieve";
        WebClient wc = WebClient.create(address);
        // Setting this property is not needed given that
        // we have the async conduit loaded in the test module:
        // WebClient.getConfig(wc).getRequestContext().put("use.async.http.conduit", true);
        wc.type("application/xml").accept("application/xml");
        Book book = wc.invoke("RETRIEVE", new Book("Retrieve", 123L), Book.class);
        assertEquals("Retrieve", book.getName());
        wc.close();
    }

    @Test
    public void testPatchBook() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/patch";
        WebClient wc = WebClient.create(address);
        wc.type("application/xml");
        WebClient.getConfig(wc).getRequestContext().put("use.async.http.conduit", true);
        Book book = wc.invoke("PATCH", new Book("Patch", 123L), Book.class);
        assertEquals("Patch", book.getName());
        wc.close();
    }

    @Test
    public void testPatchBookTimeout() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/patch";
        WebClient wc = WebClient.create(address);
        wc.type("application/xml");
        ClientConfiguration clientConfig = WebClient.getConfig(wc);
        clientConfig.getRequestContext().put("use.async.http.conduit", true);
        HTTPClientPolicy clientPolicy = clientConfig.getHttpConduit().getClient();
        clientPolicy.setReceiveTimeout(500);
        clientPolicy.setConnectionTimeout(500);
        try {
            Book book = wc.invoke("PATCH", new Book("Timeout", 123L), Book.class);
            fail("should throw an exception due to timeout, instead got " + book);
        } catch (javax.ws.rs.ProcessingException e) {
            //expected!!!
        }
    }

    @Test
    public void testPatchBookInputStream() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/patch";
        WebClient wc = WebClient.create(address);
        wc.type("application/xml");
        WebClient.getConfig(wc).getRequestContext().put("use.async.http.conduit", true);
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
        WebClient wc = WebClient.create(address);
        wc.type("application/xml").accept("application/xml");
        WebClient.getConfig(wc).getRequestContext().put("use.async.http.conduit", true);
        Book book = wc.invoke("DELETE", new Book("Delete", 123L), Book.class);
        assertEquals("Delete", book.getName());
        wc.close();
    }

    @Test
    public void testRetrieveBookCustomMethodAsync() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/retrieve";
        WebClient wc = WebClient.create(address);
        wc.accept("application/xml");
        Future<Book> book = wc.async().method("RETRIEVE", Entity.xml(new Book("Retrieve", 123L)),
                                              Book.class);
        assertEquals("Retrieve", book.get().getName());
        wc.close();
    }

    @Test
    public void testGetBookAsyncResponse404() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/404";
        WebClient wc = createWebClient(address);
        Future<Response> future = wc.async().get(Response.class);
        assertEquals(404, future.get().getStatus());
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
    public void testNonExistent() throws Exception {
        String address = "http://168.168.168.168/bookstore";
        List<Object> providers = new ArrayList<>();
        providers.add(new TestResponseFilter());
        WebClient wc = WebClient.create(address, providers);
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
    public void testNonExistentJaxrs20WithGet() throws Exception {
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
    public void testNonExistentJaxrs20WithPost() throws Exception {
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
        WebClient wc = WebClient.create(address, providers);

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
        WebClient wc = WebClient.create(address, providers);

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
        ClientBuilder.newBuilder().register(new BookServerAsyncClient.BooleanReaderWriter())
            .build().target(address)
            .request().accept("text/boolean").async().get(callback).get();
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



        Book book = store.getBookByMatrixParams("12", "3");
        assertNull(book);
        Thread.sleep(3000);
        assertNotNull(bookHolder.value);
        assertEquals(123L, bookHolder.value.getId());

        store.checkBook(123L);
        Thread.sleep(3000);
        assertTrue(booleanHolder.value);
    }

    @SuppressWarnings({
     "unchecked", "rawtypes"
    })
    private static <T> InvocationCallback<T> createGenericInvocationCallback() {
        return new GenericInvocationCallback();
    }

    @Test
    public void testGetBookAsync404Callback() throws Exception {
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
    
    private WebClient createWebClient(String address) {
        return WebClient.create(address);
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

    private static class FaultyBookWriter implements MessageBodyWriter<Book> {

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
                            MultivaluedMap<String, Object> arg5, OutputStream arg6) throws IOException,
            WebApplicationException {
            throw new RuntimeException();

        }

    }
    @Consumes("application/xml")
    private static class FaultyBookReader implements MessageBodyReader<Book> {

        @Override
        public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return true;
        }

        @Override
        public Book readFrom(Class<Book> arg0, Type arg1, Annotation[] arg2, MediaType arg3,
                             MultivaluedMap<String, String> arg4, InputStream arg5) throws IOException,
            WebApplicationException {
            throw new RuntimeException();
        }


    }

    public static class TestResponseFilter implements ClientResponseFilter {

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
            throws IOException {

        }

    }
    private static class GenericInvocationCallback<T> implements InvocationCallback<T> {
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
}
