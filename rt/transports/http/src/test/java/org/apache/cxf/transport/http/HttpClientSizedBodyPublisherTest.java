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

package org.apache.cxf.transport.http;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.cxf.transport.http.HttpClientHTTPConduit.CloseableByteArrayOutputStream;
import org.apache.cxf.transport.http.HttpClientHTTPConduit.HttpClientSizedBodyPublisher;
import org.apache.cxf.transport.http.HttpClientHTTPConduit.HttpClientWrappedOutputStream;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * CXF-9135: the caller of an asynchronous request closes the stream without waiting for the HttpClient,
 * which reads the content length and subscribes to the body on its own thread. Each test drives one
 * ordering of the two threads deterministically.
 */
public class HttpClientSizedBodyPublisherTest {
    // The size of the request in the reporter's reproducer: "Too few bytes returned by the publisher (0/206)"
    private static final int REQUEST_SIZE = 206;

    /**
     * The ordering of the reporter's reproducer: the HttpClient asks for the content length while the
     * request is being written and subscribes only after the caller closed the stream.
     */
    @Test
    public void testContentLengthBeforeCloseAndSubscriptionAfterClose() throws Exception {
        byte[] expected = body(REQUEST_SIZE);
        CloseableByteArrayOutputStream body = new CloseableByteArrayOutputStream();
        HttpClientWrappedOutputStream stream = stream(body);
        HttpClientSizedBodyPublisher publisher = publisher(stream, body);

        ExecutorService httpClient = Executors.newSingleThreadExecutor();
        try {
            CompletableFuture<Thread> httpClientThread = new CompletableFuture<>();
            Future<Long> contentLength = httpClient.submit(() -> {
                httpClientThread.complete(Thread.currentThread());
                return publisher.contentLength();
            });
            awaitWaiting(httpClientThread.get(5, TimeUnit.SECONDS));
            assertFalse(contentLength.isDone());

            body.write(expected);
            closeLikeTheCaller(stream, body, publisher);

            assertEquals(REQUEST_SIZE, contentLength.get(5, TimeUnit.SECONDS).longValue());
            assertArrayEquals(expected, readBody(publisher));
        } finally {
            httpClient.shutdownNow();
        }
    }

    /** The caller closes the stream before the HttpClient reads the content length and the body. */
    @Test
    public void testContentLengthAndSubscriptionAfterClose() throws Exception {
        byte[] expected = body(REQUEST_SIZE);
        CloseableByteArrayOutputStream body = new CloseableByteArrayOutputStream();
        HttpClientWrappedOutputStream stream = stream(body);
        HttpClientSizedBodyPublisher publisher = publisher(stream, body);
        body.write(expected);
        closeLikeTheCaller(stream, body, publisher);

        assertEquals(REQUEST_SIZE, publisher.contentLength());
        assertArrayEquals(expected, readBody(publisher));
    }

    /** The HttpClient may subscribe again, for instance to retry the request. */
    @Test
    public void testRepeatedSubscriptionsAfterClose() throws Exception {
        byte[] expected = body(128 * 1024);
        CloseableByteArrayOutputStream body = new CloseableByteArrayOutputStream();
        HttpClientWrappedOutputStream stream = stream(body);
        HttpClientSizedBodyPublisher publisher = publisher(stream, body);
        body.write(expected);
        closeLikeTheCaller(stream, body, publisher);

        assertEquals(expected.length, publisher.contentLength());
        assertArrayEquals(expected, readBody(publisher));
        assertArrayEquals(expected, readBody(publisher));
    }

    @Test
    public void testEmptyBodyAfterClose() throws Exception {
        CloseableByteArrayOutputStream body = new CloseableByteArrayOutputStream();
        HttpClientWrappedOutputStream stream = stream(body);
        HttpClientSizedBodyPublisher publisher = publisher(stream, body);
        closeLikeTheCaller(stream, body, publisher);

        assertEquals(0, publisher.contentLength());
        assertArrayEquals(new byte[0], readBody(publisher));
    }

    // The state of HttpClientWrappedOutputStream for a request of unknown length without chunking
    private static HttpClientWrappedOutputStream stream(CloseableByteArrayOutputStream body) {
        HttpClientWrappedOutputStream stream = mock(HttpClientWrappedOutputStream.class);
        stream.pout = body;
        stream.contentLen = -1;
        return stream;
    }

    private static HttpClientSizedBodyPublisher publisher(HttpClientWrappedOutputStream stream,
            CloseableByteArrayOutputStream body) {
        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setConnectionTimeout(30000);
        return new HttpClientSizedBodyPublisher(stream, policy, body);
    }

    // As HttpClientWrappedOutputStream.close() does: close the buffer and drop it, then close the publisher
    private static void closeLikeTheCaller(HttpClientWrappedOutputStream stream,
            CloseableByteArrayOutputStream body, HttpClientSizedBodyPublisher publisher) throws Exception {
        body.close();
        stream.pout = null;
        publisher.close();
    }

    private static byte[] body(int size) {
        byte[] request = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            .getBytes(StandardCharsets.UTF_8);
        byte[] body = Arrays.copyOf(request, size);
        Arrays.fill(body, Math.min(request.length, size), size, (byte)' ');
        return body;
    }

    // Wait until the HttpClient thread waits in contentLength() for the request to be written
    private static void awaitWaiting(Thread thread) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (thread.getState() != Thread.State.TIMED_WAITING
            || Arrays.stream(thread.getStackTrace()).noneMatch(e -> "contentLength".equals(e.getMethodName()))) {
            assertTrue("contentLength() did not wait for the request", System.nanoTime() < deadline);
            Thread.sleep(10);
        }
    }

    private static byte[] readBody(HttpClientSizedBodyPublisher publisher) throws Exception {
        CompletableFuture<byte[]> completed = new CompletableFuture<>();
        ByteArrayOutputStream received = new ByteArrayOutputStream();
        publisher.subscribe(new Flow.Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer buffer) {
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                received.writeBytes(bytes);
            }

            @Override
            public void onError(Throwable error) {
                completed.completeExceptionally(error);
            }

            @Override
            public void onComplete() {
                completed.complete(received.toByteArray());
            }
        });
        return completed.get(5, TimeUnit.SECONDS);
    }
}
