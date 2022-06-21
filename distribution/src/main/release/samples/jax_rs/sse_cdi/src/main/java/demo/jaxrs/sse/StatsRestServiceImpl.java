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
package demo.jaxrs.sse;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.OutboundSseEvent.Builder;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;

import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

@Path("/stats")
public class StatsRestServiceImpl {
    private static final Random RANDOM = new Random();

    private SseBroadcaster broadcaster;
    private Builder builder;


    @Context 
    public void setSse(Sse sse) {
        this.broadcaster = sse.newBroadcaster();
        this.builder = sse.newEventBuilder();
        
        Flowable
            .interval(500, TimeUnit.MILLISECONDS)
            .zipWith(
                Flowable.generate((Emitter<OutboundSseEvent.Builder> emitter) -> emitter.onNext(builder.name("stats"))),
                (id, bldr) -> createStatsEvent(bldr, id)
            )
            .subscribeOn(Schedulers.single())
            .subscribe(broadcaster::broadcast);
    }
    
    @GET
    @Path("sse")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void stats(@Context SseEventSink sink) {
        broadcaster.register(sink);
    }

    private static OutboundSseEvent createStatsEvent(final OutboundSseEvent.Builder builder, final long eventId) {
        return builder
            .id("" + eventId)
            .data(Stats.class, new Stats(new Date().getTime(), RANDOM.nextInt(100)))
            .mediaType(MediaType.APPLICATION_JSON_TYPE)
            .build();
    }
}
