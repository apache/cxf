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
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseContext;
import javax.ws.rs.sse.SseEventOutput;

@Path("/api/bookstore")
public class BookStore {
    private final CountDownLatch latch = new CountDownLatch(2);
    private final AtomicReference<SseBroadcaster> broadcaster = 
        new AtomicReference<SseBroadcaster>();
    
    private static OutboundSseEvent createStatsEvent(final OutboundSseEvent.Builder builder, final int eventId) {
        return builder
            .id(Integer.toString(eventId))
            .data(Book.class, new Book("New Book #" + eventId, eventId))
            .mediaType(MediaType.APPLICATION_JSON_TYPE)
            .build();
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
    public SseEventOutput forBook(@Context SseContext sseContext, @PathParam("id") final String id, 
            @HeaderParam(HttpHeaders.LAST_EVENT_ID_HEADER) @DefaultValue("0") final String lastEventId) {
        final SseEventOutput output = sseContext.newOutput();
        
        new Thread() {
            public void run() {
                try {
                    final Integer id = Integer.valueOf(lastEventId);

                    output.write(createStatsEvent(sseContext.newEvent().name("book"), id + 1));
                    Thread.sleep(200);
                    output.write(createStatsEvent(sseContext.newEvent().name("book"), id + 2));
                    Thread.sleep(200);
                    output.write(createStatsEvent(sseContext.newEvent().name("book"), id + 3));
                    Thread.sleep(200);
                    output.write(createStatsEvent(sseContext.newEvent().name("book"), id + 4));
                    Thread.sleep(200);
                    output.close();
                } catch (final InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        return output;
    }

    @GET
    @Path("broadcast/sse")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public SseEventOutput broadcast(@Context SseContext sseContext) {
        final SseEventOutput output = sseContext.newOutput();
        
        if (broadcaster.get() == null) {
            broadcaster.compareAndSet(null, sseContext.newBroadcaster());
        }
        
        try {
            broadcaster.get().register(output);
            
            broadcaster.get().broadcast(createStatsEvent(sseContext.newEvent().name("book"), 1000));
            broadcaster.get().broadcast(createStatsEvent(sseContext.newEvent().name("book"), 2000));
            
            return output;
        } finally {
            latch.countDown();
        }
    }
    
    @POST
    @Path("broadcast/close")
    public void stop() throws InterruptedException {
        // Await a least 2 clients to be broadcasted over 
        latch.await(2, TimeUnit.SECONDS);
        
        if (broadcaster.get() != null) {
            broadcaster.get().close();
        }
    }
}
