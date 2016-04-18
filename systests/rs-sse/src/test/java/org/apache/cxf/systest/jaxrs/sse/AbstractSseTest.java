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
package org.apache.cxf.systest.jaxrs.sse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItems;

public abstract class AbstractSseTest extends AbstractBusClientServerTestBase {
    private final ObjectMapper mapper = new ObjectMapper();
    
    @Test
    public void testBooksStreamIsReturned() throws JsonProcessingException {
        Response r = createWebClient("/rest/api/bookstore/sse/100").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        
        final String response = r.readEntity(String.class);
        assertThat(response, containsString("id: 1"));
        assertThat(response, containsString("data: " + toJson("New Book #1", 1)));
        
        assertThat(response, containsString("id: 2"));
        assertThat(response, containsString("data: " + toJson("New Book #2", 2)));
        
        assertThat(response, containsString("id: 3"));
        assertThat(response, containsString("data: " + toJson("New Book #3", 3)));
        
        assertThat(response, containsString("id: 4"));
        assertThat(response, containsString("data: " + toJson("New Book #4", 4)));
    }
    
    @Test
    public void testBooksStreamIsReturnedFromLastEventId() throws JsonProcessingException {
        Response r = createWebClient("/rest/api/bookstore/sse/100")
            .header(HttpHeaders.LAST_EVENT_ID_HEADER, 150)
            .get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        
        final String response = r.readEntity(String.class);
        assertThat(response, containsString("id: 151"));
        assertThat(response, containsString("data: " + toJson("New Book #151", 151)));
        
        assertThat(response, containsString("id: 152"));
        assertThat(response, containsString("data: " + toJson("New Book #152", 152)));
        
        assertThat(response, containsString("id: 152"));
        assertThat(response, containsString("data: " + toJson("New Book #153", 153)));
        
        assertThat(response, containsString("id: 152"));
        assertThat(response, containsString("data: " + toJson("New Book #154", 154)));
    }
    
    @Test
    public void testBooksAreReturned() throws JsonProcessingException {
        Response r = createWebClient("/rest/api/bookstore", MediaType.APPLICATION_JSON).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        
        final Book[] books = r.readEntity(Book[].class);
        assertThat(Arrays.asList(books), hasItems(new Book("New Book #1", 1), new Book("New Book #2", 2)));
    }
    
    @Test
    public void testBooksStreamIsBroadcasted() throws Exception {
        final Collection<Future<Response>> results = new ArrayList<>();
        
        for (int i = 0; i < 2; ++i) {
            results.add(
                createWebClient("/rest/api/bookstore/broadcast/sse").async().get()
            );
        }

        createWebClient("/rest/api/bookstore/broadcast/close")
            .async()
            .post(null)
            .get(4, TimeUnit.SECONDS)
            .close();

        for (final Future<Response> result: results) {
            final Response r = result.get(1, TimeUnit.SECONDS);
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
    
            final String response = r.readEntity(String.class);
            assertThat(response, containsString("id: 1000"));
            assertThat(response, containsString("data: " + toJson("New Book #1000", 1000)));
            
            assertThat(response, containsString("id: 2000"));
            assertThat(response, containsString("data: " + toJson("New Book #2000", 2000)));
            
            r.close();
        }
    }

    private String toJson(final String name, final Integer id) throws JsonProcessingException {
        return mapper.writeValueAsString(new Book(name, id));
    }
    
    protected WebClient createWebClient(final String url, final String media) {
        final List< ? > providers = Arrays.asList(new JacksonJsonProvider());
        
        final WebClient wc = WebClient
            .create("http://localhost:" + getPort() + url, providers)
            .accept(media);
        
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(5000L);
        return wc;
    }
    
    protected WebClient createWebClient(final String url) {
        return createWebClient(url, MediaType.SERVER_SENT_EVENTS);
    }
    
    protected abstract int getPort();
}
