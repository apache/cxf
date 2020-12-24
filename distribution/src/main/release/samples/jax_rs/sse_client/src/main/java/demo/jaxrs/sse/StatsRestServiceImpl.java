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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.OutboundSseEvent.Builder;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

@Path("/stats")
public class StatsRestServiceImpl {
    private static final Random RANDOM = new Random();
    private Sse sse;

    @Context 
    public void setSse(Sse sse) {
        this.sse = sse;
    }

    @GET
    @Path("sse")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void stats(@Context SseEventSink sink) {
        new Thread() {
            public void run() {
                try {
                    final Builder builder = sse.newEventBuilder();
                    sink.send(createStatsEvent(builder.name("stats"), 1));
                    Thread.sleep(500);
                    sink.send(createStatsEvent(builder.name("stats"), 2));
                    Thread.sleep(500);
                    sink.send(createStatsEvent(builder.name("stats"), 3));
                    Thread.sleep(500);
                    sink.send(createStatsEvent(builder.name("stats"), 4));
                    Thread.sleep(500);
                    sink.send(createStatsEvent(builder.name("stats"), 5));
                    Thread.sleep(500);
                    sink.send(createStatsEvent(builder.name("stats"), 6));
                    Thread.sleep(500);
                    sink.send(createStatsEvent(builder.name("stats"), 7));
                    Thread.sleep(500);
                    sink.send(createStatsEvent(builder.name("stats"), 8));
                    sink.close();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private static OutboundSseEvent createStatsEvent(final OutboundSseEvent.Builder builder, final int eventId) {
        return builder
            .id("" + eventId)
            .data(Stats.class, new Stats(new Date().getTime(), RANDOM.nextInt(100)))
            .mediaType(MediaType.APPLICATION_JSON_TYPE)
            .build();
    }
}
