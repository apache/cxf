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

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.OutboundSseEvent.Builder;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/api/bookstore")
public class BookStore extends BookStoreClientCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BookStore.class);

    private final CountDownLatch latch = new CountDownLatch(2);
    private Sse sse;
    private SseBroadcaster broadcaster;

    @Context 
    public void setSse(Sse sse) {
        this.sse = sse;
        this.broadcaster = sse.newBroadcaster();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Book> books() {
        return Arrays.asList(
                new Book("New Book #1", 1),
                new Book("New Book #2", 2)
            );
    }

    @GET
    @Path("sse/{id}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void forBook(@Context SseEventSink sink, @PathParam("id") final String id,
            @HeaderParam(HttpHeaders.LAST_EVENT_ID_HEADER) @DefaultValue("0") final String lastEventId) {

        new Thread() {
            public void run() {
                try {
                    final Integer id = Integer.valueOf(lastEventId);
                    final OutboundSseEvent.Builder builder = sse.newEventBuilder();

                    sink.send(createEvent(builder.name("book"), id + 1));
                    Thread.sleep(200);
                    sink.send(createEvent(builder.name("book"), id + 2));
                    Thread.sleep(200);
                    sink.send(createEvent(builder.name("book"), id + 3));
                    Thread.sleep(200);
                    sink.send(createEvent(builder.name("book"), id + 4));
                    Thread.sleep(200);
                    sink.close();
                } catch (final InterruptedException ex) {
                    LOG.error("Communication error", ex);
                }
            }
        }.start();
    }
    
    @POST
    @Path("sse/{id}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Consumes(MediaType.TEXT_PLAIN)
    public void forBookPOST(@Context SseEventSink sink, @PathParam("id") final String id,
            final String lastEventId) {
        new Thread() {
            public void run() {
                try {
                    final Integer id = Integer.valueOf(lastEventId);
                    final OutboundSseEvent.Builder builder = sse.newEventBuilder();

                    sink.send(createEvent(builder.name("book"), id + 1));
                    Thread.sleep(200);
                    sink.send(createEvent(builder.name("book"), id + 2));
                    Thread.sleep(200);
                    sink.send(createEvent(builder.name("book"), id + 3));
                    Thread.sleep(200);
                    sink.send(createEvent(builder.name("book"), id + 4));
                    Thread.sleep(200);
                    sink.close();
                } catch (final InterruptedException ex) {
                    LOG.error("Communication error", ex);
                }
            }
        }.start();
    }
    
    @GET
    @Path("nodelay/sse/{id}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void forBookNoDelay(@Context SseEventSink sink, @PathParam("id") final String id) {
        final Builder builder = sse.newEventBuilder();
        
        CompletableFuture
            .runAsync(() -> {
                sink.send(createEvent(builder.name("book"), 1));
                sink.send(createEvent(builder.name("book"), 2));
                sink.send(createEvent(builder.name("book"), 3));
                sink.send(createEvent(builder.name("book"), 4));
                sink.send(createEvent(builder.name("book"), 5));
            })
            .whenComplete((r, ex) -> sink.close());
    }

    @GET
    @Path("/titles/sse")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void forBookTitlesOnly(@Context SseEventSink sink) {
        final Builder builder = sse.newEventBuilder();
        
        CompletableFuture
            .runAsync(() -> {
                sink.send(createRawEvent(builder.name("book"), 1));
                sink.send(createRawEvent(builder.name("book"), 2));
                sink.send(createRawEvent(builder.name("book"), 3));
                sink.send(createRawEvent(builder.name("book"), 4));
                sink.send(createRawEvent(builder.name("book"), 5));
            })
            .whenComplete((r, ex) -> sink.close());
    }

    @GET
    @Path("nodata")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void nodata(@Context SseEventSink sink) {
        sink.close();
    }

    @GET
    @Path("broadcast/sse")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void broadcast(@Context SseEventSink sink) {
        try {
            broadcaster.register(sink);
        } finally {
            latch.countDown();
        }
    }

    @POST
    @Path("broadcast/close")
    public void stop() {
        try {
            // Await a least 2 clients to be broadcasted over
            if (!latch.await(10, TimeUnit.SECONDS)) {
                LOG.warn("Not enough clients have been connected, closing broadcaster anyway");
            }

            final Builder builder = sse.newEventBuilder();
            broadcaster.broadcast(createEvent(builder.name("book"), 1000))
                .thenAcceptBoth(broadcaster.broadcast(createEvent(builder.name("book"), 2000)), (a, b) -> { })
                .whenComplete((r, ex) -> { 
                    if (broadcaster != null) {
                        broadcaster.close();
                    }
                });
        } catch (final InterruptedException ex) {
            LOG.error("Wait has been interrupted", ex);
        }
    }

    @GET
    @Path("/filtered/sse")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void filtered(@Context SseEventSink sink) {
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(200);
                    sink.close();
                } catch (final InterruptedException ex) {
                    LOG.error("Communication error", ex);
                }
            }
        }.start();
    }

    @GET
    @Path("/headers/sse")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void headers(@Context SseEventSink sink) {
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(200);
                    sink.close();
                } catch (final InterruptedException ex) {
                    LOG.error("Communication error", ex);
                }
            }
        }.start();
    }

    @GET
    @Path("/filtered/stats")
    @Produces(MediaType.TEXT_PLAIN)
    public int filteredStats() {
        return BookStoreResponseFilter.getInvocations();
    }
    
    @PUT
    @Path("/filtered/stats")
    public void clearStats() {
        BookStoreResponseFilter.reset();
    }

    @Override
    protected Sse getSse() {
        return sse;
    }
}
