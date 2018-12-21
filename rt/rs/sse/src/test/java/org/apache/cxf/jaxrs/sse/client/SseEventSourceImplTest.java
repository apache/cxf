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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.ClientProviderFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.client.spec.ClientImpl.WebTargetImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SseEventSourceImplTest {
    private static final String EVENT = "event: event\n"
        + "id: 1\n"
        + "data: test data\n"
        + "retry: 10000\n"
        + ": test comment\n"
        + "\n";

    private static final String EVENT_JUST_DATA = "\n"
        + "data: just test data\n"
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

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ClientProviderFactory clientProviderFactory = ClientProviderFactory.createInstance(null);

    /**
     * Subclass the WebClient to augment the visibility of getConfiguration() method.
     */
    private static class TestWebClient extends WebClient {
        TestWebClient(URI baseURI) {
            super(baseURI);
        }

        @Override
        public ClientConfiguration getConfiguration() {
            return super.getConfiguration();
        }
    }

    @Mock
    private TestWebClient client;
    @Mock
    private ClientConfiguration clientConfiguration;
    @Mock
    private WebTargetImpl target;
    @Mock
    private Configuration configuration;
    @Mock
    private Invocation.Builder builder;
    @Mock
    private Endpoint endpoint;
    @Mock
    private Response response;

    @Before
    public void setUp() {
        when(target.getConfiguration()).thenReturn(configuration);
        when(target.getWebClient()).thenReturn(client);
        when(target.request(MediaType.SERVER_SENT_EVENTS)).thenReturn(builder);
        when(builder.header(any(String.class), any(Object.class))).thenReturn(builder);
        when(builder.get()).thenReturn(response);
        when(client.getConfiguration()).thenReturn(clientConfiguration);
        when(clientConfiguration.getEndpoint()).thenReturn(endpoint);
        when(endpoint.get("org.apache.cxf.jaxrs.client.ClientProviderFactory")).thenReturn(clientProviderFactory);
    }

    @After
    public void tearDown() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void testNoReconnectWhenNoContentIsReturned() {
        // Verify that 204 response code won't force reconnect
        when(response.getStatus()).thenReturn(204);

        try (SseEventSource eventSource = withNoReconnect()) {
            eventSource.open();
            assertThat(eventSource.isOpen(), equalTo(false));
            verify(response, times(1)).getStatus();
        }
    }

    @Test
    public void testReuseSameEventSourceSeveralTimes() {
        // Verify that 204 response code won't force reconnect
        when(response.getStatus()).thenReturn(204);

        try (SseEventSource eventSource = withNoReconnect()) {
            eventSource.open();
            assertThat(eventSource.isOpen(), equalTo(false));
            verify(response, times(1)).getStatus();

            eventSource.open();
            assertThat(eventSource.isOpen(), equalTo(false));
            verify(response, times(2)).getStatus();
        }
    }

    @Test
    public void testReconnectWillBeScheduledOnError() throws InterruptedException {
        when(builder.get()).thenThrow(new RuntimeException("Connection refused"));

        try (SseEventSource eventSource = withReconnect()) {
            eventSource.open();
            assertThat(eventSource.isOpen(), equalTo(false));

            // Sleep a little bit for reconnect to reschedule
            Thread.sleep(150);
            verify(builder, atLeast(2)).get();
        }
    }

    @Test
    public void testNoReconnectWillBeScheduledWhenClosed() throws InterruptedException {
        when(builder.get()).thenThrow(new RuntimeException("Connection refused"));

        try (SseEventSource eventSource = withReconnect()) {
            eventSource.open();
            assertThat(eventSource.isOpen(), equalTo(false));
            eventSource.close(1, TimeUnit.SECONDS);

            // Sleep a little bit to make sure for reconnect to reschedule (after 100ms)
            Thread.sleep(150);
            verify(builder, times(1)).get();
        }
    }

    @Test
    public void testWhenTryToConnectTwiceSecondAttemtShouldFail() throws InterruptedException, ExecutionException {
        when(builder.get()).then(invocation -> {
            Thread.sleep(100);
            return response;
        });

        try (SseEventSource eventSource = withReconnect()) {
            eventSource.open();

            // The attempt to open the SSE connection in another thread at the same
            // time should fail
            final Future<?> future = executor.submit(() -> eventSource.open());
            exception.expectCause(instanceOf(IllegalStateException.class));
            assertThat(future.get(), equalTo(null));

            assertThat(eventSource.isOpen(), equalTo(true));
            verify(builder, times(1)).get();
        }
    }

    @Test
    public void testNoReconnectAndOneEventReceived() throws InterruptedException, IOException {
        try (InputStream is = new ByteArrayInputStream(EVENT.getBytes(StandardCharsets.UTF_8))) {
            when(response.getStatus()).thenReturn(200);
            when(response.readEntity(InputStream.class)).thenReturn(is);

            final List<InboundSseEvent> events = new ArrayList<>();
            try (SseEventSource eventSource = withNoReconnect()) {
                eventSource.register(events::add);
                eventSource.open();

                assertThat(eventSource.isOpen(), equalTo(true));
                verify(response, times(1)).getStatus();

                // Allow the event processor to pull for events (150ms)
                Thread.sleep(150);
            }

            assertThat(events.size(), equalTo(1));
            assertThat(events.get(0).getId(), equalTo("1"));
            assertThat(events.get(0).getReconnectDelay(), equalTo(10000L));
            assertThat(events.get(0).getComment(), equalTo("test comment"));
            assertThat(events.get(0).readData(), equalTo("test data"));
        }
    }

    @Test
    public void testNoReconnectAndJustDataEventIsReceived() throws InterruptedException, IOException {
        try (InputStream is = new ByteArrayInputStream(EVENT_JUST_DATA.getBytes(StandardCharsets.UTF_8))) {
            when(response.getStatus()).thenReturn(200);
            when(response.readEntity(InputStream.class)).thenReturn(is);

            final List<InboundSseEvent> events = new ArrayList<>();
            try (SseEventSource eventSource = withNoReconnect()) {
                eventSource.register(events::add);
                eventSource.open();

                assertThat(eventSource.isOpen(), equalTo(true));
                verify(response, times(1)).getStatus();

                // Allow the event processor to pull for events (150ms)
                Thread.sleep(150);
            }

            assertThat(events.size(), equalTo(1));
            assertThat(events.get(0).getName(), nullValue());
            assertThat(events.get(0).readData(), equalTo("just test data"));
        }
    }

    @Test
    public void testNoReconnectAndJustEventNameIsReceived() throws InterruptedException, IOException {
        try (InputStream is = new ByteArrayInputStream(EVENT_JUST_NAME.getBytes(StandardCharsets.UTF_8))) {
            when(response.getStatus()).thenReturn(200);
            when(response.readEntity(InputStream.class)).thenReturn(is);

            final List<InboundSseEvent> events = new ArrayList<>();
            try (SseEventSource eventSource = withNoReconnect()) {
                eventSource.register(events::add);
                eventSource.open();

                assertThat(eventSource.isOpen(), equalTo(true));
                verify(response, times(1)).getStatus();

                // Allow the event processor to pull for events (150ms)
                Thread.sleep(150);
            }

            assertThat(events.size(), equalTo(1));
            assertThat(events.get(0).getName(), equalTo("just name"));
        }
    }

    @Test
    public void testNoReconnectAndMixedEventsAreReceived() throws InterruptedException, IOException {
        try (InputStream is = new ByteArrayInputStream(EVENT_MIXED.getBytes(StandardCharsets.UTF_8))) {
            when(response.getStatus()).thenReturn(200);
            when(response.readEntity(InputStream.class)).thenReturn(is);

            final List<InboundSseEvent> events = new ArrayList<>();
            try (SseEventSource eventSource = withNoReconnect()) {
                eventSource.register(events::add);
                eventSource.open();

                assertThat(eventSource.isOpen(), equalTo(true));
                verify(response, times(1)).getStatus();

                // Allow the event processor to pull for events (150ms)
                Thread.sleep(150);
            }

            assertThat(events.size(), equalTo(2));
            assertThat(events.get(0).getName(), nullValue());
            assertThat(events.get(0).readData(), equalTo("just test data"));
            assertThat(events.get(1).getId(), equalTo("1"));
            assertThat(events.get(1).getReconnectDelay(), equalTo(10000L));
            assertThat(events.get(1).getComment(), equalTo("test comment"));
            assertThat(events.get(1).readData(), equalTo("test data"));
        }
    }

    @Test
    public void testNoReconnectAndNoEventsAreDetected() throws InterruptedException, IOException {
        try (InputStream is = new ByteArrayInputStream(EVENT_BAD_NEW_LINES.getBytes(StandardCharsets.UTF_8))) {
            when(response.getStatus()).thenReturn(200);
            when(response.readEntity(InputStream.class)).thenReturn(is);

            final List<InboundSseEvent> events = new ArrayList<>();
            try (SseEventSource eventSource = withNoReconnect()) {
                eventSource.register(events::add);
                eventSource.open();

                assertThat(eventSource.isOpen(), equalTo(true));
                verify(response, times(1)).getStatus();

                // Allow the event processor to pull for events (150ms)
                Thread.sleep(150);
            }

            assertThat(events.size(), equalTo(0));
        }
    }

    @Test
    public void testReconnectAndTwoEventsReceived() throws InterruptedException, IOException {
        final Collection<InputStream> closeables = new ArrayList<>();

        try {
            when(response.getStatus()).thenReturn(200);
            when(response.readEntity(InputStream.class)).then(Invocation -> {
                final InputStream is = new ByteArrayInputStream(EVENT_NO_RETRY.getBytes(StandardCharsets.UTF_8));
                closeables.add(is);
                return is;
            });

            final List<InboundSseEvent> events = new ArrayList<>();
            try (SseEventSource eventSource = withReconnect()) {
                eventSource.register(events::add);
                eventSource.open();

                assertThat(eventSource.isOpen(), equalTo(true));
                //verify(response, times(1)).getStatus();

                // Allow the event processor to pull for events (200ms)
                Thread.sleep(150);
            }

            assertThat(events.size(), equalTo(2));
            assertThat(events.get(0).getId(), equalTo("1"));
            assertThat(events.get(0).getComment(), equalTo("test comment"));
            assertThat(events.get(0).readData(), equalTo("test data"));
            assertThat(events.get(1).getId(), equalTo("1"));
            assertThat(events.get(1).getComment(), equalTo("test comment"));
            assertThat(events.get(1).readData(), equalTo("test data"));
        } finally {
            for (final InputStream is: closeables) {
                is.close();
            }
        }
    }

    @Test
    public void testNoReconnectAndCloseTheStreamWhileEventIsBeingReceived() throws InterruptedException, IOException {
        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(InputStream.class)).then(invocation -> {
            Thread.sleep(200);
            return null;
        });

        final List<InboundSseEvent> events = new ArrayList<>();
        try (SseEventSource eventSource = withNoReconnect()) {
            eventSource.register(events::add);
            eventSource.open();

            assertThat(eventSource.isOpen(), equalTo(true));
            verify(response, times(1)).getStatus();

            // Allow the event processor to pull for events (200ms)
            Thread.sleep(50);
            assertThat(eventSource.close(100, TimeUnit.MILLISECONDS), equalTo(true));
            assertThat(eventSource.isOpen(), equalTo(false));
        }
    }

    @Test
    public void testInvalidReconnectDelayInTheEvent() throws InterruptedException, IOException {
        try (InputStream is = new ByteArrayInputStream(EVENT_BAD_RETRY.getBytes(StandardCharsets.UTF_8))) {
            when(response.getStatus()).thenReturn(200);
            when(response.readEntity(InputStream.class)).thenReturn(is);

            final List<InboundSseEvent> events = new ArrayList<>();
            try (SseEventSource eventSource = withNoReconnect()) {
                eventSource.register(events::add);
                eventSource.open();

                assertThat(eventSource.isOpen(), equalTo(true));
                verify(response, times(1)).getStatus();

                // Allow the event processor to pull for events (150ms)
                Thread.sleep(150);
            }

            assertThat(events.size(), equalTo(1));
            assertThat(events.get(0).getId(), equalTo("1"));
            assertThat(events.get(0).getReconnectDelay(), equalTo(-1L));
            assertThat(events.get(0).getComment(), equalTo("test comment"));
            assertThat(events.get(0).readData(), equalTo("test data"));
        }
    }

    @Test
    public void testTryToCloseWhileConnecting() throws ExecutionException, InterruptedException {
        when(response.getStatus()).thenReturn(200);
        when(builder.get()).then(invocation -> {
            Thread.sleep(200);
            return response;
        });

        try (SseEventSource eventSource = withNoReconnect()) {
            final Future<?> future = executor.submit(() -> eventSource.open());

            // Wait a bit for open() to advance
            Thread.sleep(100);
            eventSource.close();

            assertThat(future.get(), equalTo(null));
            assertThat(eventSource.isOpen(), equalTo(false));
        }
    }

    private SseEventSource withNoReconnect() {
        return SseEventSource.target(target).build();
    }

    private SseEventSource withReconnect() {
        return SseEventSource.target(target).reconnectingEvery(100, TimeUnit.MILLISECONDS).build();
    }
}
