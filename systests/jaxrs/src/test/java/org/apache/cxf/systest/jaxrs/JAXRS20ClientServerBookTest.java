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

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

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
        doTestBook(address);
    }
    
    @Test
    public void testGetBookWrongPath() {
        String address = "http://localhost:" + PORT + "/wrongpath";
        doTestBook(address);
    }
    
    private void doTestBook(String address) {
        List<Object> providers = new ArrayList<Object>();
        providers.add(new ClientHeaderRequestFilter());
        providers.add(new ClientHeaderResponseFilter());
        WebClient wc = WebClient.create(address, providers);
        Book book = wc.get(Book.class);
        assertEquals(124L, book.getId());
        Response response = wc.getResponse();
        assertEquals("OK", response.getHeaderString("Response"));
        assertEquals("custom", response.getHeaderString("Custom"));
        assertEquals("simple", response.getHeaderString("Simple"));
        assertEquals("http://localhost/redirect", response.getHeaderString(HttpHeaders.LOCATION));
    }
    
    @Test
    public void testClientFiltersLocalResponse() {
        String address = "http://localhost:" + PORT + "/bookstores";
        List<Object> providers = new ArrayList<Object>();
        providers.add(new ClientCacheRequestFilter());
        providers.add(new ClientHeaderResponseFilter());
        WebClient wc = WebClient.create(address, providers);
        Response r = wc.get();
        assertEquals(201, r.getStatus());
        assertEquals("http://localhost/redirect", r.getHeaderString(HttpHeaders.LOCATION));
    }
    
    private static class ClientCacheRequestFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext context) throws IOException {
            context.abortWith(Response.status(201).build());
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
}
