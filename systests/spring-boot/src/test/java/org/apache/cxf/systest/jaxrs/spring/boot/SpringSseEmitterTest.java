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

package org.apache.cxf.systest.jaxrs.spring.boot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;
import org.apache.cxf.systest.jaxrs.resources.Book;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SpringSseEmitterTest.LibraryController.class)
public class SpringSseEmitterTest {
    private static final int CNT = 5;
    @LocalServerPort
    private int port;
    
    @RestController
    @EnableAutoConfiguration
    static class LibraryController {
        @GetMapping("/sse")
        public SseEmitter streamSseMvc() {
            final SseEmitter emitter = new SseEmitter();
            CompletableFuture.runAsync(() -> {
                try {
                    for (int eventId = 1; eventId <= CNT; ++eventId) {
                        SseEventBuilder event = SseEmitter.event()
                            .id(Integer.toString(eventId))
                            .data(new Book("New Book #" + eventId, "Author #" + eventId), MediaType.APPLICATION_JSON)
                            .name("book");
                        emitter.send(event);
                        Thread.sleep(100L);
                    }
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            });
            return emitter;
        }
    }
    
    @Test
    public void testSseEvents() throws InterruptedException {
        final WebTarget target = createWebTarget();
        final Collection<Book> books = new ArrayList<>(CNT);
        final AtomicReference<Throwable> throwable = new AtomicReference<>();

        try (SseEventSource eventSource = SseEventSource.target(target).build()) {
            eventSource.register(event -> {
                books.add(event.readData(Book.class, jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE));
                if (books.size() == CNT) {
                    synchronized (books) {
                        books.notify();
                    }
                }
            }, e -> throwable.set(e));
            eventSource.open();
            // Give the SSE stream some time to collect all events
            synchronized (books) {
                books.wait(5000L);
            }
        }

        assertThat(throwable.get(), nullValue());
        assertThat(books,
            hasItems(
                new Book("New Book #1", "Author #1"),
                new Book("New Book #2", "Author #2"),
                new Book("New Book #3", "Author #3"),
                new Book("New Book #4", "Author #4"),
                new Book("New Book #5", "Author #5")
            )
        );
    }
    
    private WebTarget createWebTarget() {
        return ClientBuilder
            .newClient()
            .property("http.receive.timeout", 8000)
            .register(JacksonJsonProvider.class)
            .target("http://localhost:" + port + "/sse");
    }

}
