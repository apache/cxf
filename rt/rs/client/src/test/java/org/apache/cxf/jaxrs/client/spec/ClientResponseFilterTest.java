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
package org.apache.cxf.jaxrs.client.spec;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Priority;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.client.ResponseProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.transport.local.LocalTransportFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ClientResponseFilterTest {
    private static final String ADDRESS = "local://client";
    private Server server;

    @Path("/")
    public static class TestEndpoint {
        @GET
        public String get() {
            return "hello rabbit";
        }
        
        @PUT
        public Response update() {
            return Response.status(Status.NOT_FOUND).build();
        }
        
        @DELETE
        public void delete() {
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

    @Before
    public void setUp() throws Exception {
        final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(TestEndpoint.class);
        sf.setResourceProvider(TestEndpoint.class, new SingletonResourceProvider(new TestEndpoint(), false));
        sf.setTransportId(LocalTransportFactory.TRANSPORT_ID);
        sf.setAddress(ADDRESS);
        server = sf.create();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
        server.destroy();
    }

    @Test(expected = ResponseProcessingException.class)
    public void testExceptionInClientResponseFilter() {
        try (Response response = ClientBuilder.newClient()
             .register(FaultyClientResponseFilter.class)
             .target(ADDRESS)
             .request()
             .get()) {
            fail("Should raise ResponseProcessingException");
        }
    }
    
    @Test(expected = ResponseProcessingException.class)
    public void testExceptionInClientResponseFilterWhenNotFound() {
        try (Response response = ClientBuilder.newClient()
             .register(FaultyClientResponseFilter.class)
             .target(ADDRESS)
             .request()
             .put(null)) {
            fail("Should not be invoked");
        }
    }
    
    @Test
    public void testClientResponseFilterWhenNotFound() {
        try (Response response = ClientBuilder.newClient()
             .register(AddHeaderClientResponseFilter.class)
             .target(ADDRESS)
             .request()
             .put(null)) {
            assertEquals(404, response.getStatus());
            assertEquals("true", response.getHeaderString("X-Done"));
        }
    }
    
    @Test
    public void testClientResponseFilter() {
        try (Response response = ClientBuilder.newClient()
             .register(AddHeaderClientResponseFilter.class)
             .target(ADDRESS)
             .request()
             .get()) {
            assertEquals(200, response.getStatus());
            assertEquals("true", response.getHeaderString("X-Done"));
        }
    }
    
    @Test
    public void testExceptionWhenMultipleClientResponseFilters() {
        try (Response response = ClientBuilder.newClient()
             .register(AddHeaderClientResponseFilter.class)
             .register(FaultyClientResponseFilter.class)
             .target(ADDRESS)
             .request()
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
    
    @Test
    public void testAsyncClientResponseFilter() throws Exception {
        try (Response response = ClientBuilder.newClient()
             .register(AddHeaderClientResponseFilter.class)
             .target(ADDRESS)
             .request()
             .async()
             .get()
             .get(10, TimeUnit.SECONDS)) {
            assertEquals(200, response.getStatus());
            assertEquals("true", response.getHeaderString("X-Done"));
        }
    }
    
    @Test
    public void testExceptionWhenMultipleAsyncClientResponseFilters() {
        try (Response response = ClientBuilder.newClient()
             .register(AddHeaderClientResponseFilter.class)
             .register(FaultyClientResponseFilter.class)
             .target(ADDRESS)
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
    public void testExceptionInAsyncClientResponseFilter() throws Exception {
        try (Response response = ClientBuilder.newClient()
             .register(FaultyClientResponseFilter.class)
             .target(ADDRESS)
             .request()
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
    public void testExceptionInAsyncClientResponseFilterWhenNotFound() throws Exception {
        try (Response response = ClientBuilder.newClient()
             .register(FaultyClientResponseFilter.class)
             .target(ADDRESS)
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
    public void testAsyncClientResponseFilterWhenNotFound() throws Exception {
        try (Response response = ClientBuilder.newClient()
             .register(AddHeaderClientResponseFilter.class)
             .target(ADDRESS)
             .request()
             .async()
             .put(null)
             .get(10, TimeUnit.SECONDS)) {
            assertEquals(404, response.getStatus());
            assertEquals("true", response.getHeaderString("X-Done"));
        }
    }
}