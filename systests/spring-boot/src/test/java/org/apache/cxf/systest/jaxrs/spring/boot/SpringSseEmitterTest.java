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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.systest.jaxrs.resources.Book;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SpringSseEmitterTest.LibraryController.class)
public class SpringSseEmitterTest {
    @LocalServerPort
    private int port;
    
    @RestController
    @EnableAutoConfiguration
    static class LibraryController {
        @GetMapping("/sse")
        public SseEmitter streamSseMvc() {
            final SseEmitter emitter = new SseEmitter();
            final ExecutorService sseMvcExecutor = Executors.newSingleThreadExecutor();
            
            sseMvcExecutor.execute(() -> {
                try {
                    for (int eventId = 1; eventId <= 5; ++eventId) {
                        SseEventBuilder event = SseEmitter.event()
                            .id(Integer.toString(eventId))
                            .data(new Book("New Book #" + eventId, "Author #" + eventId), MediaType.APPLICATION_JSON)
                            .name("book");
                        emitter.send(event);
                        Thread.sleep(100);
                    }
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
        final Collection<Book> books = new ArrayList<>();

        try (SseEventSource eventSource = SseEventSource.target(target).build()) {
            eventSource.register(collect(books), System.out::println);
            eventSource.open();
            // Give the SSE stream some time to collect all events
            awaitEvents(5000, books, 5);
        }

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

    private static Consumer<InboundSseEvent> collect(final Collection< Book > books) {
        return event -> books.add(event.readData(Book.class, javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE));
    }
    
    private void awaitEvents(long timeout, final Collection<?> events, int size) throws InterruptedException {
        final long sleep = timeout / 10;
        
        for (int i = 0; i < timeout; i += sleep) {
            if (events.size() == size) {
                break;
            } else {
                Thread.sleep(sleep);
            }
        }
    }
}
