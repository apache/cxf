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

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

public final class StatsClient {
    private StatsClient() {
    }
    
    public static void main(String[] args) throws InterruptedException {
        final WebTarget target = ClientBuilder
            .newClient()
            .register(JacksonJsonProvider.class)
            .target("http://localhost:8686/rest/stats/sse");
        
        try (SseEventSource eventSource = SseEventSource.target(target).build()) {
            eventSource.register(StatsClient::print, System.out::println);
            eventSource.open();
            // Give the SSE stream 5 seconds to collect all events
            Thread.sleep(5000);
        }
    }

    private static void print(InboundSseEvent event) {
        final Stats stats = event.readData(Stats.class, MediaType.APPLICATION_JSON_TYPE);
        System.out.println(stats.getTimestamp() + ": " + stats.getLoad() + "%");
    }
}
