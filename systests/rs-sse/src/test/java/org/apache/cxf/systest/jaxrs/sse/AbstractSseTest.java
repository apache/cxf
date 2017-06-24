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
import java.util.Collection;
import java.util.function.Consumer;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItems;

public abstract class AbstractSseTest extends AbstractSseBaseTest {
    @Test
    public void testBooksStreamIsReturnedFromLastEventId() throws InterruptedException {
        final WebTarget target = createWebTarget("/rest/api/bookstore/sse/0")
            .property(HttpHeaders.LAST_EVENT_ID_HEADER, 150);
        final Collection<Book> books = new ArrayList<>();
        
        try (final SseEventSource eventSource = SseEventSource.target(target).build()) {
            eventSource.register(collect(books), System.out::println);
            eventSource.open();
            // Give the SSE stream some time to collect all events
            awaitEvents(3000, books, 4);
        }

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
        
        try (final SseEventSource eventSource = SseEventSource.target(target).build()) {
            eventSource.register(collect(books), System.out::println);
            eventSource.open();
            // Give the SSE stream some time to collect all events
            awaitEvents(3000, books, 4);
        }

        assertThat(books, 
            hasItems(
                new Book("New Book #1", 1), 
                new Book("New Book #2", 2), 
                new Book("New Book #3", 3), 
                new Book("New Book #4", 4)
            )
        );
    }

    private static Consumer<InboundSseEvent> collect(final Collection< Book > books) {
        return event -> books.add(event.readData(Book.class, MediaType.APPLICATION_JSON_TYPE));
    }
}
