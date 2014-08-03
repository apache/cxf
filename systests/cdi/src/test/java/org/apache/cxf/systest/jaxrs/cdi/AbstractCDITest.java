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
package org.apache.cxf.systest.jaxrs.cdi;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.Test;


public abstract class AbstractCDITest extends AbstractBusClientServerTestBase {
    @Test
    public void testInjectedVersionIsProperlyReturned() {
        Response r = createWebClient("/rest/api/bookstore/version", MediaType.TEXT_PLAIN).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        assertEquals("1.0", r.readEntity(String.class));
    }
    
    @Test
    public void testResponseHasBeenReceivedWhenAddingNewBook() {
        Response r = createWebClient("/rest/api/bookstore/books").post(
            new Form()
                .param("id", "1234")
                .param("name", "Book 1234"));
        assertEquals(Status.CREATED.getStatusCode(), r.getStatus());
    }
    
    @Test
    public void testResponseHasBeenReceivedWhenQueringAllBooks() {
        Response r = createWebClient("/rest/api/bookstore/books").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
    }
    
    @Test
    public void testAddAndQueryOneBook() {
        final String id = UUID.randomUUID().toString();
        
        Response r = createWebClient("/rest/api/bookstore/books").post(
            new Form()
                .param("id", id)
                .param("name", "Book 1234"));
        assertEquals(Status.CREATED.getStatusCode(), r.getStatus());
        
        r = createWebClient("/rest/api/bookstore/books").path(id).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        
        Book book = r.readEntity(Book.class);
        assertEquals(id, book.getId());
    }
    
    @Test
    public void testAddOneBookWithValidation() {
        final String id = UUID.randomUUID().toString();
        
        Response r = createWebClient("/rest/custom/bookstore/books").post(
            new Form()
                .param("id", id));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }
    
    
    protected WebClient createWebClient(final String url) {
        return createWebClient(url, MediaType.APPLICATION_JSON);
    }
    
    protected WebClient createWebClient(final String url, final String mediaType) {
        final List< ? > providers = Arrays.asList(new JacksonJsonProvider());
        
        final WebClient wc = WebClient
            .create("http://localhost:" + getPort() + url, providers)
            .accept(mediaType);
        
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        return wc;
    }
    
    protected abstract int getPort();
}
