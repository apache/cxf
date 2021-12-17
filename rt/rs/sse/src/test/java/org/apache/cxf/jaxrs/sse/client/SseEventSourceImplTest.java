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
package org.apache.cxf.jaxrs.sse.client;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.sse.InboundSseEvent;
import jakarta.ws.rs.sse.SseEventSource;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class SseEventSourceImplTest {

    enum Type {
        NO_CONTENT, NO_SERVER, BUSY, UNAVAILABLE, RETRY_AFTER,
        EVENT, EVENT_JUST_DATA, EVENT_JUST_NAME, EVENT_MULTILINE_DATA, EVENT_NO_RETRY, EVENT_BAD_RETRY, EVENT_MIXED,
        EVENT_BAD_NEW_LINES, EVENT_NOT_AUTHORIZED, EVENT_LAST_EVENT_ID, EVENT_RETRY_LAST_EVENT_ID;
    }

    private static final String EVENT = "event: event\n"
        + "id: 1\n"
        + "data: test data\n"
        + "retry: 10000\n"
        + ": test comment\n"
        + "\n";

    private static final String EVENT_JUST_DATA = "\n"
        + "data: just test data\n"
        + "\n";

    private static final String EVENT_MULTILINE_DATA = "\n"
            + "data: just test data\n"
            + "data: in multiple lines\n"
            + "\n";

    private static final String EVENT_JUST_NAME = "\n"
        + "event: just name\n";

    private static final String EVENT_NO_RETRY = "event: event\n"
        + "id: 1\n"
        + "data: test data\n"
        + ": test comment\n"
        + "\n";

    private static final String EVENT_BAD_RETRY = "event: event\n"
        + "id: 1\n"
        + "data: test data\n"
        + "retry: blba\n"
        + ": test comment\n"
        + "\n";

    private static final String EVENT_MIXED = EVENT_JUST_DATA + EVENT;
    private static final String EVENT_BAD_NEW_LINES =  "\n\n\n\n\n\n";

    private static final String LOCAL_ADDRESS = "local://";

    private static final Map<Type, Server> SERVERS = new EnumMap<>(Type.class);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final List<InboundSseEvent> events = new ArrayList<>();
    private final List<Throwable> errors = new ArrayList<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @After
    public void tearDown() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(1L, TimeUnit.SECONDS);
    }

    @Test
    public void testNoReconnectWhenNoContentIsReturned() {
        try (SseEventSource eventSource = withNoReconnect(Type.NO_CONTENT)) {
            eventSource.open();
            assertThat(eventSource.isOpen(), equalTo(false));

            assertThat(events.size(), equalTo(0));
        }
    }
    
    @Test
    public void testNoReconnectWhenUnavailableIsReturned() {
        try (SseEventSource eventSource = withNoReconnect(Type.UNAVAILABLE)) {
            eventSource.open();
            assertThat(eventSource.isOpen(), equalTo(false));

            await()
                .during(Duration.ofMillis(3000L))
                .untilAsserted(() -> assertThat(eventSource.isOpen(), equalTo(false)));

            assertThat(events.size(), equalTo(0));
        }
    }
    
    @Test
    public void testNoReconnectWhenRetryAfterIsReturned() {
        try (SseEventSource eventSource = withNoReconnect(Type.RETRY_AFTER)) {
            eventSource.open();
            assertThat(eventSource.isOpen(), equalTo(false));

            await()
                .atMost(Duration.ofMillis(3000L))
                .untilAsserted(() -> assertThat(eventSource.isOpen(), equalTo(true)));

            assertThat(events.size(), equalTo(1));
        }
    }

    @Test
    public void testReuseSameEventSourceSeveralTimes() {
        try (SseEventSource eventSource = withNoReconnect(Type.NO_CONTENT)) {
            eventSource.open();
            assertThat(eventSource.isOpen(), equalTo(false));

            eventSource.open();
            assertThat(eventSource.isOpen(), equalTo(false));

            assertThat(events.size(), equalTo(0));
        }
    }

    @Test
    public void testReconnectWillBeScheduledOnError() throws InterruptedException {
        try (SseEventSource eventSource = withReconnect(Type.NO_SERVER)) {
            eventSource.open();
            assertThat(eventSource.isOpen(), equalTo(false));

            // Sleep a little bit for reconnect to reschedule
            Thread.sleep(150L);
            assertThat(errors.size(), equalTo(2));
        }
    }

    @Test
    public void testNoReconnectWillBeScheduledWhenClosed() throws InterruptedException {
        try (SseEventSource eventSource = withReconnect(Type.NO_SERVER)) {
            eventSource.open();
            assertThat(eventSource.isOpen(), equalTo(false));
            eventSource.close(1L, TimeUnit.SECONDS);

            // Sleep a little bit to make sure for reconnect to reschedule (after 100ms)
            Thread.sleep(150L);
            assertThat(errors.size(), equalTo(1));
        }
    }

    @Test
    public void testWhenTryToConnectTwiceSecondAttemtShouldFail() throws InterruptedException, ExecutionException {
        try (SseEventSource eventSource = withReconnect(Type.BUSY)) {
            eventSource.open();

            // The attempt to open the SSE connection in another thread at the same
            // time should fail
            final Future<?> future = executor.submit(() -> eventSource.open());
            exception.expectCause(instanceOf(IllegalStateException.class));
            assertThat(future.get(), equalTo(null));

            assertThat(eventSource.isOpen(), equalTo(true));
            assertThat(events.size(), equalTo(1));
        }
    }

    @Test
    public void testNoReconnectAndOneEventReceived() throws InterruptedException, IOException {
        try (SseEventSource eventSource = withNoReconnect(Type.EVENT)) {
            eventSource.open();

            assertThat(eventSource.isOpen(), equalTo(true));

            // Allow the event processor to pull for events (150ms)
            Thread.sleep(150L);
        }

        await()
            .atMost(Duration.ofMillis(500L))
            .untilAsserted(() -> assertThat(events.size(), equalTo(1)));

        assertThat(events.get(0).getId(), equalTo("1"));
        assertThat(events.get(0).getReconnectDelay(), equalTo(10000L));
        assertThat(events.get(0).getComment(), equalTo("test comment"));
        assertThat(events.get(0).readData(), equalTo("test data"));
    }

    @Test
    public void testNoReconnectAndJustDataEventIsReceived() throws InterruptedException, IOException {
        try (SseEventSource eventSource = withNoReconnect(Type.EVENT_JUST_DATA)) {
            eventSource.open();

            assertThat(eventSource.isOpen(), equalTo(true));

            // Allow the event processor to pull for events (150ms)
            Thread.sleep(150L);
        }

        await()
            .atMost(Duration.ofMillis(500L))
            .untilAsserted(() -> assertThat(events.size(), equalTo(1)));

        assertThat(events.get(0).getName(), nullValue());
        assertThat(events.get(0).readData(), equalTo("just test data"));
    }

    @Test
    public void testNoReconnectAndMultilineDataEventIsReceived() throws InterruptedException, IOException {
        try (SseEventSource eventSource = withNoReconnect(Type.EVENT_MULTILINE_DATA)) {
            eventSource.open();

            assertThat(eventSource.isOpen(), equalTo(true));

            // Allow the event processor to pull for events (150ms)
            Thread.sleep(150L);
        }

        await()
            .atMost(Duration.ofMillis(500L))
            .untilAsserted(() -> assertThat(events.size(), equalTo(1)));

        assertThat(events.get(0).getName(), nullValue());
        assertThat(events.get(0).readData(), equalTo("just test data\nin multiple lines"));
    }
    
    @Test
    public void testNoReconnectAndJustEventNameIsReceived() throws InterruptedException, IOException {
        final Map<String, Object> properties = Collections
            .singletonMap(SseEventSourceImpl.DISCARD_INCOMPLETE_EVENTS, false);
        
        try (SseEventSource eventSource = withNoReconnect(Type.EVENT_JUST_NAME, properties)) {
            eventSource.open();

            assertThat(eventSource.isOpen(), equalTo(true));

            // Allow the event processor to pull for events (150ms)
            Thread.sleep(150L);
        }

        await()
            .atMost(Duration.ofMillis(500L))
            .untilAsserted(() -> assertThat(events.size(), equalTo(1)));

        assertThat(events.get(0).getName(), equalTo("just name"));
    }

    @Test
    public void testNoReconnectAndIncompleteEventIsDiscarded() throws InterruptedException, IOException {
        try (SseEventSource eventSource = withNoReconnect(Type.EVENT_JUST_NAME)) {
            eventSource.open();

            assertThat(eventSource.isOpen(), equalTo(true));

            // Allow the event processor to pull for events (150ms)
            Thread.sleep(150L);
        }

        // incomplete event should be discarded
        await()
            .during(Duration.ofMillis(500L))
            .until(events::isEmpty);

        assertThat(events.size(), equalTo(0));
    }

    @Test
    public void testNoReconnectAndMixedEventsAreReceived() throws InterruptedException, IOException {
        try (SseEventSource eventSource = withNoReconnect(Type.EVENT_MIXED)) {
            eventSource.open();

            assertThat(eventSource.isOpen(), equalTo(true));

            // Allow the event processor to pull for events (150ms)
            Thread.sleep(150L);
        }

        await()
            .atMost(Duration.ofMillis(500L))
            .untilAsserted(() -> assertThat(events.size(), equalTo(2)));

        assertThat(events.get(0).getName(), nullValue());
        assertThat(events.get(0).readData(), equalTo("just test data"));
        assertThat(events.get(1).getId(), equalTo("1"));
        assertThat(events.get(1).getReconnectDelay(), equalTo(10000L));
        assertThat(events.get(1).getComment(), equalTo("test comment"));
        assertThat(events.get(1).readData(), equalTo("test data"));
    }

    @Test
    public void testNoReconnectAndNoEventsAreDetected() throws InterruptedException, IOException {
        try (SseEventSource eventSource = withNoReconnect(Type.EVENT_BAD_NEW_LINES)) {
            eventSource.open();

            assertThat(eventSource.isOpen(), equalTo(true));

            // Allow the event processor to pull for events (150ms)
            Thread.sleep(150L);
        }

        assertThat(events.size(), equalTo(0));
    }

    @Test
    public void testReconnectAndTwoEventsReceived() throws InterruptedException, IOException {
        try (SseEventSource eventSource = withReconnect(Type.EVENT_NO_RETRY)) {
            eventSource.open();

            assertThat(eventSource.isOpen(), equalTo(true));

            // Allow the event processor to pull for events (200ms)
            Thread.sleep(150L);
        }

        await()
            .atMost(Duration.ofMillis(500L))
            .untilAsserted(() -> assertThat(events.size(), equalTo(2)));

        assertThat(events.get(0).getId(), equalTo("1"));
        assertThat(events.get(0).getComment(), equalTo("test comment"));
        assertThat(events.get(0).readData(), equalTo("test data"));
        assertThat(events.get(1).getId(), equalTo("1"));
        assertThat(events.get(1).getComment(), equalTo("test comment"));
        assertThat(events.get(1).readData(), equalTo("test data"));
    }

    @Test
    public void testReconnectAndNotAuthorized() throws InterruptedException, IOException {
        try (SseEventSource eventSource = withReconnect(Type.EVENT_NOT_AUTHORIZED)) {
            eventSource.open();
            assertThat(eventSource.isOpen(), equalTo(false));
            assertThat(errors.size(), equalTo(1));

            // Allow the event processor to pull for events (150ms)
            Thread.sleep(150L);
        }
        
        await()
            .atMost(Duration.ofMillis(500L))
            .untilAsserted(() -> assertThat(errors.size(), equalTo(2)));

        assertThat(events.size(), equalTo(0));
    }

    @Test
    public void testNoReconnectAndNotAuthorized() throws InterruptedException, IOException {
        try (SseEventSource eventSource = withNoReconnect(Type.EVENT_NOT_AUTHORIZED)) {
            eventSource.open();
            assertThat(eventSource.isOpen(), equalTo(false));
            assertThat(errors.size(), equalTo(1));

            // Allow the event processor to pull for events (150ms)
            Thread.sleep(150L);
        }
        
        await()
            .atMost(Duration.ofMillis(500L))
            .untilAsserted(() -> assertThat(errors.size(), equalTo(1)));

        assertThat(events.size(), equalTo(0));
    }

    @Test
    public void testNoReconnectAndCloseTheStreamWhileEventIsBeingReceived() throws InterruptedException, IOException {
        try (SseEventSource eventSource = withNoReconnect(Type.BUSY)) {
            eventSource.open();

            assertThat(eventSource.isOpen(), equalTo(true));

            // Allow the event processor to pull for events (200ms)
            Thread.sleep(50L);
            assertThat(eventSource.close(100L, TimeUnit.MILLISECONDS), equalTo(true));
            assertThat(eventSource.isOpen(), equalTo(false));
        }
    }

    @Test
    public void testInvalidReconnectDelayInTheEvent() throws InterruptedException, IOException {
        try (SseEventSource eventSource = withNoReconnect(Type.EVENT_BAD_RETRY)) {
            eventSource.open();

            assertThat(eventSource.isOpen(), equalTo(true));

            // Allow the event processor to pull for events (150ms)
            Thread.sleep(150L);
        }

        await()
            .atMost(Duration.ofMillis(500L))
            .untilAsserted(() -> assertThat(events.size(), equalTo(1)));

        assertThat(events.get(0).getId(), equalTo("1"));
        assertThat(events.get(0).getReconnectDelay(), equalTo(-1L));
        assertThat(events.get(0).getComment(), equalTo("test comment"));
        assertThat(events.get(0).readData(), equalTo("test data"));
    }

    @Test
    public void testTryToCloseWhileConnecting() throws ExecutionException, InterruptedException {
        try (SseEventSource eventSource = withNoReconnect(Type.BUSY)) {
            final Future<?> future = executor.submit(() -> eventSource.open());

            // Wait a bit for open() to advance
            Thread.sleep(50L);
            eventSource.close();

            assertThat(future.get(), equalTo(null));
            assertThat(eventSource.isOpen(), equalTo(false));
        }
    }

    @Test
    public void testConnectWithLastEventId() throws InterruptedException, IOException {
        try (SseEventSource eventSource = withNoReconnect(Type.EVENT_LAST_EVENT_ID, "10")) {
            eventSource.open();
            assertThat(eventSource.isOpen(), equalTo(true));

            // Allow the event processor to pull for events (150ms)
            Thread.sleep(150L);
        }

        await()
            .atMost(Duration.ofMillis(500L))
            .untilAsserted(() -> assertThat(events.size(), equalTo(1)));
        
        assertThat(events.get(0).getId(), equalTo("10"));
        assertThat(events.get(0).getReconnectDelay(), equalTo(10000L));
        assertThat(events.get(0).getComment(), equalTo("test comment"));
        assertThat(events.get(0).readData(), equalTo("test data"));
    }
    
    @Test
    public void testReconnectWithLastEventId() throws InterruptedException, IOException {
        try (SseEventSource eventSource = withReconnect(Type.EVENT_RETRY_LAST_EVENT_ID, "10")) {
            eventSource.open();
            assertThat(eventSource.isOpen(), equalTo(false));
            assertThat(errors.size(), equalTo(1));

            // Allow the event processor to pull for events (150ms)
            Thread.sleep(150L);
        }

        await()
            .atMost(Duration.ofMillis(500L))
            .untilAsserted(() -> assertThat(events.size(), equalTo(1)));
        
        assertThat(events.get(0).getId(), equalTo("10"));
        assertThat(events.get(0).getReconnectDelay(), equalTo(10000L));
        assertThat(events.get(0).getComment(), equalTo("test comment"));
        assertThat(events.get(0).readData(), equalTo("test data"));
    }
    
    private SseEventSource withNoReconnect(Type type) {
        return withNoReconnect(type, null, Collections.emptyMap());
    }
    
    private SseEventSource withNoReconnect(Type type, Map<String, Object> properties) {
        return withNoReconnect(type, null, properties);
    }
    
    private SseEventSource withNoReconnect(Type type, String lastEventId) {
        return withNoReconnect(type, lastEventId, Collections.emptyMap());
    }
    
    private SseEventSource withNoReconnect(Type type, String lastEventId, Map<String, Object> properties) {
        SseEventSource eventSource = SseEventSource.target(target(type, lastEventId, properties)).build();
        eventSource.register(events::add, errors::add);
        return eventSource;
    }
    
    private SseEventSource withReconnect(Type type) {
        return withReconnect(type, null);
    }

    private SseEventSource withReconnect(Type type, String lastEventId) {
        SseEventSource eventSource = SseEventSource.target(target(type, lastEventId))
                .reconnectingEvery(100L, TimeUnit.MILLISECONDS)
                .build();
        eventSource.register(events::add, errors::add);
        return eventSource;
    }

    private static WebTarget target(Type type, String lastEventId) {
        return target(type, lastEventId, Collections.emptyMap());
    }
    
    private static WebTarget target(Type type, String lastEventId, Map<String, Object> properties) {
        final Client client = ClientBuilder.newClient();
        if (properties != null) {
            properties.forEach(client::property);
        }
        
        final WebTarget target = client.target(LOCAL_ADDRESS + type.name());
        if (lastEventId != null) {
            target.property(HttpHeaders.LAST_EVENT_ID_HEADER, lastEventId);
        }
        return target;
    }

    @BeforeClass
    public static void startServer() {
        startServer(Type.NO_CONTENT, null);
        // Type.NO_SERVER

        startBusyServer(Type.BUSY);
        startNotAuthorizedServer(Type.EVENT_NOT_AUTHORIZED);
        startUnavailableServer(Type.UNAVAILABLE);
        startUnavailableServer(Type.RETRY_AFTER);

        startServer(Type.EVENT, EVENT);
        startServer(Type.EVENT_JUST_DATA, EVENT_JUST_DATA);
        startServer(Type.EVENT_JUST_NAME, EVENT_JUST_NAME);
        startServer(Type.EVENT_MULTILINE_DATA, EVENT_MULTILINE_DATA);
        startServer(Type.EVENT_NO_RETRY, EVENT_NO_RETRY);
        startServer(Type.EVENT_BAD_RETRY, EVENT_BAD_RETRY);
        startServer(Type.EVENT_MIXED, EVENT_MIXED);
        startServer(Type.EVENT_BAD_NEW_LINES, EVENT_BAD_NEW_LINES);
        
        final Function<HttpHeaders, String> function = headers -> {
            final String lastEventId = headers.getHeaderString(HttpHeaders.LAST_EVENT_ID_HEADER);
            if (lastEventId != null) {
                return EVENT.replaceAll("id: 1", "id: " + lastEventId); 
            } else {
                return EVENT;
            }
        };
        
        startDynamicServer(Type.EVENT_RETRY_LAST_EVENT_ID, function);
        startDynamicServer(Type.EVENT_LAST_EVENT_ID, function);
    }

    private static void startNotAuthorizedServer(Type type) {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setAddress(LOCAL_ADDRESS + type.name());
        sf.setServiceBean(new ProtectedEventServer());
        SERVERS.put(type, sf.create());
    }

    private static void startBusyServer(Type type) {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setAddress(LOCAL_ADDRESS + type.name());
        sf.setServiceBean(new BusyEventServer());
        SERVERS.put(type, sf.create());
    }
    
    private static void startUnavailableServer(Type type) {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setAddress(LOCAL_ADDRESS + type.name());
        sf.setServiceBean(new StatusServer(503, type));
        SERVERS.put(type, sf.create());
    }

    
    private static void startServer(Type type, String payload) {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setAddress(LOCAL_ADDRESS + type.name());
        sf.setServiceBean(new EventServer(payload));
        SERVERS.put(type, sf.create());
    }
    
    private static void startDynamicServer(Type type, Function<HttpHeaders, String> function) {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setAddress(LOCAL_ADDRESS + type.name());
        sf.setServiceBean(new DynamicServer(function, type == Type.EVENT_RETRY_LAST_EVENT_ID));
        SERVERS.put(type, sf.create());
    }

    @AfterClass
    public static void stopServer() {
        for (Server server : SERVERS.values()) {
            server.stop();
            server.destroy();
        }
    }
    
    public static class StatusServer {
        private final int status;
        private final Type type;
        private volatile boolean triggered;

        public StatusServer(int status, Type type) {
            this.status = status;
            this.type = type;
        }

        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public Response event() {
            try {
                if (triggered) {
                    return Response.ok(EVENT).build();
                } else if (status == 503) {
                    if (type == Type.RETRY_AFTER) {
                        return Response
                           .status(status)
                           .header(HttpHeaders.RETRY_AFTER, "2")
                           .build();
                    } else {
                        return Response
                            .status(status)
                            .build();
                    }
                } else {
                    return Response.status(status).build();
                }
            } finally {
                triggered = true;
            }
        }
    }

    public static class EventServer {
        private final String payload;

        public EventServer(String event) {
            payload = event;
        }

        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public String event() {
            return payload;
        }
    }

    public static class BusyEventServer extends EventServer {
        public BusyEventServer() {
            super(EVENT);
        }
        @Override
        public String event() {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
            }
            return super.event();
        }
    }

    public static class ProtectedEventServer {
        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public Response event() {
            return Response.status(Status.UNAUTHORIZED).build();
        }
    }

    public static class DynamicServer {
        private final Function<HttpHeaders, String> function;
        private volatile boolean fail; 

        public DynamicServer(Function<HttpHeaders, String> function, boolean fail) {
            this.function = function;
            this.fail = fail;
        }

        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public String event(@Context HttpHeaders headers) {
            if (fail) {
                fail = false;
                throw new BadRequestException();
            } else {
                return function.apply(headers);
            }
        }
    }

}
