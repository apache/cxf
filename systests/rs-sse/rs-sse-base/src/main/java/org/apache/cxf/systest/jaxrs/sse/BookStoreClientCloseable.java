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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.Phaser;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
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

abstract class BookStoreClientCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BookStore.class);
    
    private final BookBroadcasterStats stats = new BookBroadcasterStats(); 
    private final Phaser phaser = new Phaser(2);
    
    protected abstract Sse getSse();

    @GET
    @Path("client-closes-connection/sse/{id}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void clientCloseConnection(@Context SseEventSink sink, @PathParam("id") final String idIgnore,
        @HeaderParam(HttpHeaders.LAST_EVENT_ID_HEADER) @DefaultValue("0") final String lastEventId) {

        stats.reset();
        new Thread(() -> {
            try {
                final Integer id = Integer.valueOf(lastEventId);
                final Builder builder = getSse().newEventBuilder();

                SseBroadcaster localBroadcaster = getSse().newBroadcaster();
                localBroadcaster.onError((sseEventSink, throwable) -> stats.errored());
                localBroadcaster.onClose(sseEventSink -> stats.closed());
                localBroadcaster.register(sink);

                localBroadcaster.broadcast(createEvent(builder.name("book"), id + 1))
                    .whenComplete((r, ex) -> stats.inc());
                
                // Await client to confirm the it got the event (PUT /client-closes-connection/received)
                phaser.arriveAndAwaitAdvance();
                
                Thread.sleep(500);
                localBroadcaster.broadcast(createEvent(builder.name("book"), id + 2))
                    .whenComplete((r, ex) -> { 
                        // we expect exception here
                        if (ex == null && !sink.isClosed()) {
                            stats.inc();
                        }   
                    });

                // Await client to confirm the connection is closed (PUT /client-closes-connection/closed)
                phaser.arriveAndAwaitAdvance();
                
                // This event should complete exceptionally since SseEventSource should be 
                // closed already.
                Thread.sleep(500);
                localBroadcaster.broadcast(createEvent(builder.name("book"), id + 3))
                    .whenComplete((r, ex) -> { 
                        // we expect exception here
                        if (ex == null && !sink.isClosed()) {
                            stats.inc();
                        }   
                    });
                
                // This event should complete immediately since the sink has been removed
                // from the broadcaster (closed).
                Thread.sleep(500);
                localBroadcaster.broadcast(createEvent(builder.name("book"), id + 4))
                    .whenComplete((r, ex) -> {
                        // we expect the sink to be closed at this point
                        if (ex != null || !sink.isClosed()) {
                            stats.inc();
                        }   
                    });

                stats.setWasClosed(sink.isClosed());
                phaser.arriveAndDeregister();
                
                sink.close();
            } catch (final InterruptedException ex) {
                LOG.error("Communication error", ex);
            } catch (final IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        ).start();
    }
    
    @PUT
    @Path("client-closes-connection/received")
    @Produces(MediaType.APPLICATION_JSON)
    public void received() {
        phaser.arriveAndAwaitAdvance();
    }
    
    @PUT
    @Path("client-closes-connection/closed")
    @Produces(MediaType.APPLICATION_JSON)
    public void closed() {
        phaser.arriveAndDeregister();
    }
    
    @GET
    @Path("client-closes-connection/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public BookBroadcasterStats stats() {
        return stats;
    }
    
    protected static OutboundSseEvent createEvent(final OutboundSseEvent.Builder builder, final int eventId) {
        return builder
            .id(Integer.toString(eventId))
            .data(Book.class, new Book("New Book #" + eventId, eventId))
            .mediaType(MediaType.APPLICATION_JSON_TYPE)
            .build();
    }
    
    protected static OutboundSseEvent createRawEvent(final OutboundSseEvent.Builder builder, final int eventId) {
        return builder
            .id(Integer.toString(eventId))
            .data("New Book #" + eventId)
            .mediaType(MediaType.TEXT_PLAIN_TYPE)
            .build();
    }
}
