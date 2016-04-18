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

import java.io.IOException;
import java.util.Date;
import java.util.Random;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseContext;
import javax.ws.rs.sse.SseEventOutput;

@Path("/stats")
public class StatsRestServiceImpl {
    private static final Random RANDOM = new Random();

    private static OutboundSseEvent createStatsEvent(final OutboundSseEvent.Builder builder, final int eventId) {
        return builder
            .id("" + eventId)
            .data(Stats.class, new Stats(new Date().getTime(), RANDOM.nextInt(100)))
            .mediaType(MediaType.APPLICATION_JSON_TYPE)
            .build();
    }
    
    @GET
    @Path("sse/{id}")
    @Produces("text/event-stream")
    public SseEventOutput stats(@Context SseContext sseContext, @PathParam("id") final String id) {
        final SseEventOutput output = sseContext.newOutput();
        
        new Thread() {
            public void run() {
                try {
                    output.write(createStatsEvent(sseContext.newEvent().name("stats"), 1));
                    Thread.sleep(1000);
                    output.write(createStatsEvent(sseContext.newEvent().name("stats"), 2));
                    Thread.sleep(1000);
                    output.write(createStatsEvent(sseContext.newEvent().name("stats"), 3));
                    Thread.sleep(1000);
                    output.write(createStatsEvent(sseContext.newEvent().name("stats"), 4));
                    Thread.sleep(1000);
                    output.write(createStatsEvent(sseContext.newEvent().name("stats"), 5));
                    Thread.sleep(1000);
                    output.write(createStatsEvent(sseContext.newEvent().name("stats"), 6));
                    Thread.sleep(1000);
                    output.write(createStatsEvent(sseContext.newEvent().name("stats"), 7));
                    Thread.sleep(1000);
                    output.write(createStatsEvent(sseContext.newEvent().name("stats"), 8));
                    output.close();
                } catch (final InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        return output;
    }
}
