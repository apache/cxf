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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.sse.InboundSseEvent;
import jakarta.ws.rs.sse.SseEventSource;
import jakarta.ws.rs.sse.SseEventSource.Builder;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public abstract class AbstractSseTest extends AbstractSseBaseTest {
    @Before
    public void setUp() {
        assertThat(createWebTarget("/rest/api/bookstore/filtered/stats")
            .request()
            .put(null)
            .getStatus(), equalTo(204));

    }
    
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

    @SuppressWarnings("unchecked")
    @Test
    public void testBooksStreamIsReturnedFromInboundSseEventsWithPOST() throws InterruptedException, IOException {
        final WebTarget target = createWebTarget("/rest/api/bookstore/sse/0");
        final Collection<Book> books = new ArrayList<>();
        
        @SuppressWarnings("rawtypes")
        MessageBodyReader mbr = new JacksonJsonProvider();
        
        Response response = target.request(MediaType.SERVER_SENT_EVENTS)
            .post(Entity.entity(42, MediaType.TEXT_PLAIN));
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(response.readEntity(InputStream.class)))) {
            String s;
            Integer id = null;
            Book book = null;
            
            while ((s = br.readLine()) != null) {
                if (s.trim().isEmpty()) {
                    if (id == null && book == null) {
                        continue;
                    } else if (id != null && book != null) {
                        books.add(book);
                        id = null;
                        book = null;
                        continue;
                    }
                    fail("The event did not contain both an id " + id + " and a book " + book);
                }
                if (s.startsWith("event:")) {
                    assertEquals("Not a book event", "event: book", s.trim());
                    continue;
                }
                if (s.startsWith("id:")) {
                    assertNull("There was an existing id " + id, id);
                    id = Integer.parseInt(s.substring(3).trim());
                    continue;
                }
                if (s.startsWith("data:")) {
                    assertNull("There was an existing book " + book, book);
                    book = (Book) mbr.readFrom(Book.class, Book.class, null, MediaType.APPLICATION_JSON_TYPE, null, 
                            new ByteArrayInputStream(s.substring(5).trim().getBytes(StandardCharsets.UTF_8)));
                    continue;
                }
                fail("Unexpected String content returned by SSE POST " + s);
            }
        }
    
        // Easing the test verification here, it does not work well for Atm + Jetty
        assertThat(books,
                hasItems(
                        new Book("New Book #43", 43),
                        new Book("New Book #44", 44),
                        new Book("New Book #45", 45),
                        new Book("New Book #46", 46)
                        )
        );
    }

    @Test
    public void testBookTitlesStreamIsReturnedFromInboundSseEvents() throws InterruptedException {
        final WebTarget target = createWebTarget("/rest/api/bookstore/titles/sse");
        final Collection<String> titles = new ArrayList<>();

        try (SseEventSource eventSource = SseEventSource.target(target).build()) {
            eventSource.register(collectRaw(titles), System.out::println);
            eventSource.open();
            // Give the SSE stream some time to collect all events
            awaitEvents(5000, titles, 4);
        }
        // Easing the test verification here, it does not work well for Atm + Jetty
        assertThat(titles,
            hasItems(
                "New Book #1",
                "New Book #2",
                "New Book #3",
                "New Book #4"
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
    public void testBooksSseContainerResponseFilterIsCalled() throws InterruptedException {
        final WebTarget target = createWebTarget("/rest/api/bookstore/filtered/sse");
        final Collection<Book> books = new ArrayList<>();

        assertThat(createWebTarget("/rest/api/bookstore/filtered/stats")
            .request()
            .get(Integer.class), equalTo(0));
    
        try (SseEventSource eventSource = SseEventSource.target(target).build()) {
            eventSource.register(collect(books), System.out::println);
            eventSource.open();
            // Give the SSE stream some time to collect all events
            Thread.sleep(1000);
        }
        // Easing the test verification here, it does not work well for Atm + Jetty
        assertTrue(books.isEmpty());

        assertThat(createWebTarget("/rest/api/bookstore/filtered/stats")
            .request()
            .get(Integer.class), equalTo(1));
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
    public void testBooksContainerResponseFilterIsCalled() throws InterruptedException {
        Response r = createWebClient("/rest/api/bookstore", MediaType.APPLICATION_JSON).get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        assertThat(createWebTarget("/rest/api/bookstore/filtered/stats")
            .request()
            .get(Integer.class), equalTo(1));
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

    @Test
    public void testClientClosesEventSource() throws InterruptedException {
        final WebTarget target = createWebTarget("/rest/api/bookstore/client-closes-connection/sse/0");
        final Collection<Book> books = new ArrayList<>();

        try (SseEventSource eventSource = SseEventSource.target(target).build()) {
            eventSource.register(collect(books), System.out::println);
            eventSource.open();

            // wait for single event, close before server sends other 3
            awaitEvents(200, books, 1);

            // Only two out of 4 messages should be delivered, others should be discarded
            final Response r =
                createWebClient("/rest/api/bookstore/client-closes-connection/received", MediaType.APPLICATION_JSON)
                    .put(null);
            assertThat(r.getStatus(), equalTo(204));

            assertThat(eventSource.close(1, TimeUnit.SECONDS), equalTo(true));
        }

        // Easing the test verification here, it does not work well for Atm + Jetty
        assertThat(books,
            hasItems(
                new Book("New Book #1", 1)
            )
        );

        // Only two out of 4 messages should be delivered, others should be discarded
        final Response r =
            createWebClient("/rest/api/bookstore/client-closes-connection/closed", MediaType.APPLICATION_JSON)
                .put(null);
        assertThat(r.getStatus(), equalTo(204));

        // Give server some time to finish up the sink
        Thread.sleep(2000);

        // Only two out of 4 messages should be delivered, others should be discarded
        final BookBroadcasterStats stats =
            createWebClient("/rest/api/bookstore/client-closes-connection/stats", MediaType.APPLICATION_JSON)
                .get()
                .readEntity(BookBroadcasterStats.class);

        // Tomcat will feedback through onError callback, others through onComplete
        assertThat(stats.isErrored(), equalTo(supportsErrorPropagation()));
        // The sink should be in closed state
        assertThat(stats.isWasClosed(), equalTo(true));
        // The onClose callback should be called
        assertThat(stats.isClosed(), equalTo(true));

        // It is very hard to get the predictable match here, but at most
        // 2 events could get through before the client's connection drop off
        assertTrue(stats.getCompleted() == 2 || stats.getCompleted() == 1);
    }

    @Test
    public void testBooksSseContainerResponseAddedHeaders() throws InterruptedException {
        final WebTarget target = createWebTarget("/rest/api/bookstore/headers/sse");
        try (Response response = target.request(MediaType.SERVER_SENT_EVENTS).get()) {
            assertThat(response.getStatus(), equalTo(202));
            assertThat(response.getHeaderString("X-My-Header"), equalTo("headers"));
            assertThat(response.getHeaderString("X-My-ProtocolHeader"), equalTo("protocol-headers"));
        }
    }

    /**
     * Jetty / Undertow do not propagate errors from the runnable passed to
     * AsyncContext::start() up to the AsyncEventListener::onError(). Tomcat however
     * does it.
     * @return
     */
    protected boolean supportsErrorPropagation() {
        return false;
    }

    private static Consumer<InboundSseEvent> collect(final Collection<Book> books) {
        return event -> books.add(event.readData(Book.class, MediaType.APPLICATION_JSON_TYPE));
    }
    
    private static Consumer<InboundSseEvent> collectRaw(final Collection<String> titles) {
        return event -> titles.add(event.readData(String.class, MediaType.TEXT_PLAIN_TYPE));
    }
}
