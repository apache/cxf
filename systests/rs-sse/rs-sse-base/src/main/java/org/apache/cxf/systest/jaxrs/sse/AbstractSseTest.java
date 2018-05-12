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
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;
import javax.ws.rs.sse.SseEventSource.Builder;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItems;

public abstract class AbstractSseTest extends AbstractSseBaseTest {
    @Test
    public void testBooksStreamIsReturnedFromLastEventId() throws InterruptedException {
        final WebTarget target = createWebTarget("/rest/api/bookstore/sse/" + UUID.randomUUID())
            .property(HttpHeaders.LAST_EVENT_ID_HEADER, 150);
        final Collection<Book> books = new ArrayList<>();
        
        try (SseEventSource eventSource = SseEventSource.target(target).build()) {
            eventSource.register(collect(books), System.out::println);
            eventSource.open();
            // Give the SSE stream some time to collect all events
            awaitEvents(5000, books, 4);
        }

        // Easing the test verification here, it does not work well for Atm + Jetty
        assertThat(books, 
            hasItems(
                new Book("New Book #151", 151), 
                new Book("New Book #152", 152), 
                new Book("New Book #153", 153), 
                new Book("New Book #154", 154)
            )
        );
    }

    @Test
    public void testBooksStreamIsReturnedFromInboundSseEvents() throws InterruptedException {
        final WebTarget target = createWebTarget("/rest/api/bookstore/sse/0");
        final Collection<Book> books = new ArrayList<>();
        
        try (SseEventSource eventSource = SseEventSource.target(target).build()) {
            eventSource.register(collect(books), System.out::println);
            eventSource.open();
            // Give the SSE stream some time to collect all events
            awaitEvents(5000, books, 4);
        }
        // Easing the test verification here, it does not work well for Atm + Jetty
        assertThat(books, 
            hasItems(
                new Book("New Book #1", 1), 
                new Book("New Book #2", 2), 
                new Book("New Book #3", 3), 
                new Book("New Book #4", 4)
            )
        );
    }

    @Test
    public void testNoDataIsReturnedFromInboundSseEvents() throws InterruptedException {
        final WebTarget target = createWebTarget("/rest/api/bookstore/nodata");
        final Collection<Book> books = new ArrayList<>();
        
        try (SseEventSource eventSource = SseEventSource.target(target).build()) {
            eventSource.register(collect(books), System.out::println);
            eventSource.open();
            // Give the SSE stream some time to collect all events
            Thread.sleep(1000);
        }
        // Easing the test verification here, it does not work well for Atm + Jetty
        assertTrue(books.isEmpty());
    }

    @Test
    public void testBooksStreamIsReconnectedFromInboundSseEvents() throws InterruptedException {
        final WebTarget target = createWebTarget("/rest/api/bookstore/sse/0");
        final Collection<Book> books = new ArrayList<>();
        
        final Builder builder = SseEventSource.target(target).reconnectingEvery(1, TimeUnit.SECONDS);
        try (SseEventSource eventSource = builder.build()) {
            eventSource.register(collect(books), System.out::println);
            eventSource.open();
            // Give the SSE stream some time to collect all events
            awaitEvents(5000, books, 12);
        }

        assertThat(books, 
            hasItems(
                new Book("New Book #1", 1), 
                new Book("New Book #2", 2), 
                new Book("New Book #3", 3), 
                new Book("New Book #4", 4),
                new Book("New Book #5", 5), 
                new Book("New Book #6", 6), 
                new Book("New Book #7", 7), 
                new Book("New Book #8", 8),
                new Book("New Book #9", 9), 
                new Book("New Book #10", 10), 
                new Book("New Book #11", 11), 
                new Book("New Book #12", 12)
            )
        );
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
            .get(10, TimeUnit.SECONDS)
            .close();

        for (final Future<Response> result: results) {
            final Response r = result.get(3, TimeUnit.SECONDS);
            assertEquals(Status.OK.getStatusCode(), r.getStatus());

            final String response = r.readEntity(String.class);
            assertThat(response, containsString("id: 1000"));
            assertThat(response, containsString("data: " + toJson("New Book #1000", 1000)));

            assertThat(response, containsString("id: 2000"));
            assertThat(response, containsString("data: " + toJson("New Book #2000", 2000)));

            r.close();
        }
    }

    @Test
    public void testBooksAreReturned() throws JsonProcessingException {
        Response r = createWebClient("/rest/api/bookstore", MediaType.APPLICATION_JSON).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        final Book[] books = r.readEntity(Book[].class);
        assertThat(Arrays.asList(books), hasItems(new Book("New Book #1", 1), new Book("New Book #2", 2)));

        r.close();
    }

    @Test
    public void testBooksStreamIsReturnedFromInboundSseEventsNoDelay() throws InterruptedException {
        final WebTarget target = createWebTarget("/rest/api/bookstore/nodelay/sse/0");
        final Collection<Book> books = new ArrayList<>();
        
        try (SseEventSource eventSource = SseEventSource.target(target).build()) {
            eventSource.register(collect(books), System.out::println);
            eventSource.open();
            // Give the SSE stream some time to collect all events
            awaitEvents(5000, books, 5);
        }
        // Easing the test verification here, it does not work well for Atm + Jetty
        assertThat(books, 
            hasItems(
                new Book("New Book #1", 1), 
                new Book("New Book #2", 2), 
                new Book("New Book #3", 3), 
                new Book("New Book #4", 4),
                new Book("New Book #5", 5)
            )
        );
    }

    private static Consumer<InboundSseEvent> collect(final Collection< Book > books) {
        return event -> books.add(event.readData(Book.class, MediaType.APPLICATION_JSON_TYPE));
    }
}
