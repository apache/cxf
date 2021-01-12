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
package demo.jaxrs.server;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Random;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Response.ResponseBuilder;
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
    @Path("/static/{resource:.*}")
    public Response getResource(@Context UriInfo uriInfo, @PathParam("resource") String resourcePath) {
        if (resourcePath.contains("favicon")) {
            return Response.status(404).build();
        }

        try {
            final URL resourceURL = getClass().getResource("/web-ui/" + resourcePath);
            final ResponseBuilder rb = Response.ok(resourceURL.openStream());
            
            int ind = resourcePath.lastIndexOf('.');
            if (ind != -1 && ind < resourcePath.length()) {
                String ext = resourcePath.substring(ind + 1);
                if ("js".equalsIgnoreCase(ext)) {
                    rb.type("application/javascript");
                } else {
                    rb.type(MediaType.TEXT_HTML);
                }
            }
            
            return rb.build();
        } catch (IOException ex) {
            throw new NotFoundException(ex);
        }
    }
 
    @GET
    @Path("/sse")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void stats(@Context SseEventSink sink) {
        new Thread() {
            public void run() {
                try {
                    final Builder builder = sse.newEventBuilder();
                    sink.send(createStatsEvent(builder, 1));
                    Thread.sleep(1000);
                    sink.send(createStatsEvent(builder, 2));
                    Thread.sleep(1000);
                    sink.send(createStatsEvent(builder, 3));
                    Thread.sleep(1000);
                    sink.send(createStatsEvent(builder, 4));
                    Thread.sleep(1000);
                    sink.send(createStatsEvent(builder, 5));
                    Thread.sleep(1000);
                    sink.send(createStatsEvent(builder, 6));
                    Thread.sleep(1000);
                    sink.send(createStatsEvent(builder, 7));
                    Thread.sleep(1000);
                    sink.send(createStatsEvent(builder, 8));
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
            .mediaType(MediaType.APPLICATION_JSON_TYPE)
            .data(Stats.class, new Stats(new Date().getTime(), RANDOM.nextInt(100)))
            .build();
    }
}
