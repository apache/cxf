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
import java.io.PipedInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import org.apache.cxf.transport.http.HttpClientHTTPConduit.HttpClientBodyPublisher;
import org.apache.cxf.transport.http.HttpClientHTTPConduit.HttpClientPipedOutputStream;
import org.apache.cxf.transport.http.HttpClientHTTPConduit.HttpClientWrappedOutputStream;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class HttpClientBodyPublisherTest {

    private static final int REQUEST_SIZE = 206;
    private static final int CHUNK_THRESHOLD_SIZE = 4096;

    private static final int AWAIT_TEST_TIMEOUT = 4;

    /**
     * The HTTP client subscribes and receives the complete request body.
     */
    @Test
    public void testSubscribeBeforePublisherClose() throws Exception {
        byte[] expected = body(REQUEST_SIZE);

        TestPubPipeWrap pubStream = streams(expected.length);

        pubStream.pipedOutputStream.write(expected);

        /*
         * We subscribe first
         */
        CountDownLatch countDownLatchBeforeSubscribe = new CountDownLatch(1);
        CountDownLatch countDownLatchAfterSubscribe = new CountDownLatch(1);
        // The HttpClient arrive before the close ... and subscribe...
        // Using PipedInputStream as pin I use another thread to subscribe
        CompletableFuture<byte[]> received = subscribeAsync(
                pubStream.publisher,
                countDownLatchBeforeSubscribe,
                countDownLatchAfterSubscribe
        );
        // Wait to be subscribed
        assertTrue(countDownLatchBeforeSubscribe.await(AWAIT_TEST_TIMEOUT, TimeUnit.SECONDS));
        assertTrue(countDownLatchAfterSubscribe.await(AWAIT_TEST_TIMEOUT, TimeUnit.SECONDS));

        /*
         * ...and then close
         */

        // The caller close the stream
        pubStream.wrappedOutputStream.close();
        // Close also the publisher
        pubStream.pipedOutputStream.close();

        // Assert the body is the same
        assertArrayEquals(expected,  received.get(AWAIT_TEST_TIMEOUT, TimeUnit.SECONDS));
        // ... and the connection marked completed
        assertTrue(pubStream.wrappedOutputStream.connectionComplete);
    }

    /**
     * The HTTP client subscribes twice .
     */
    @Test
    public void testSubscribeTwiceBeforePublisherClose() throws Exception {
        byte[] expected = body(REQUEST_SIZE);

        TestPubPipeWrap pubStream = streams(expected.length);

        pubStream.pipedOutputStream.write(expected);

        /*
         * We subscribe first ... 1°
         */
        CountDownLatch countDownLatchBeforeSubscribe1 = new CountDownLatch(1);
        CountDownLatch countDownLatchAfterSubscribe1 = new CountDownLatch(1);
        // The HttpClient arrive before the close ... and subscribe...
        // Using PipedInputStream as pin I use another thread to subscribe
        CompletableFuture<byte[]> received = subscribeAsync(
                pubStream.publisher,
                countDownLatchBeforeSubscribe1,
                countDownLatchAfterSubscribe1
        );
        // Wait to be subscribed 1°
        assertTrue(countDownLatchBeforeSubscribe1.await(AWAIT_TEST_TIMEOUT, TimeUnit.SECONDS));
        assertTrue(countDownLatchAfterSubscribe1.await(AWAIT_TEST_TIMEOUT, TimeUnit.SECONDS));

        /*
         * We subscribe first ... 2°
         */
        CountDownLatch countDownLatchBeforeSubscribe2 = new CountDownLatch(1);
        CountDownLatch countDownLatchAfterSubscribe2 = new CountDownLatch(1);
        // The HttpClient arrive before the close ... and subscribe...
        // Using PipedInputStream as pin I use another thread to subscribe
        CompletableFuture<byte[]> received2 = subscribeAsync(
                pubStream.publisher,
                countDownLatchBeforeSubscribe2,
                countDownLatchAfterSubscribe2
        );

        //We assure also the 2° thread is in position in publisher.subscribe(...)
        assertTrue(countDownLatchBeforeSubscribe2.await(AWAIT_TEST_TIMEOUT, TimeUnit.SECONDS));

        /*
         * ...and then close
         */

        // The caller close the stream
        pubStream.wrappedOutputStream.close();

        // Close also the publisher
        pubStream.pipedOutputStream.close();

        // Wait to be subscribed 2° ...the lock on the pin is Publisher is release by now
        assertTrue(countDownLatchAfterSubscribe2.await(AWAIT_TEST_TIMEOUT, TimeUnit.SECONDS));

        // Assert the body is the same
        assertArrayEquals(expected,  received.get(AWAIT_TEST_TIMEOUT, TimeUnit.SECONDS));
        assertArrayEquals(new byte[0],  received2.get(AWAIT_TEST_TIMEOUT, TimeUnit.SECONDS));
        // ... and the connection marked completed
        assertTrue(pubStream.wrappedOutputStream.connectionComplete);
    }

    /**
     * The publisher is closed before the HTTP client subscribes.
     * The complete request body must still be available.
     */
    @Test
    public void testSubscribeAfterPublisherClose() throws Exception {
        byte[] expected = body(REQUEST_SIZE);

        TestPubPipeWrap pubStream = streams(expected.length);

        pubStream.pipedOutputStream.write(expected);

        /*
         * We close before the publisher
         */

        // The caller close the stream
        pubStream.wrappedOutputStream.close();
        // Close also the publisher
        pubStream.pipedOutputStream.close();


        /*
         * ...and then subscribe
         */

        CountDownLatch countDownLatchBeforeSubscribe = new CountDownLatch(1);
        CountDownLatch countDownLatchAfterSubscribe = new CountDownLatch(1);
        // The HttpClient arrive before the close ... and subscribe...
        // Using PipedInputStream as pin I use another thread to subscribe
        CompletableFuture<byte[]> received = subscribeAsync(
                pubStream.publisher,
                countDownLatchBeforeSubscribe,
                countDownLatchAfterSubscribe
        );
        // Wait to be subscribed
        assertTrue(countDownLatchBeforeSubscribe.await(AWAIT_TEST_TIMEOUT, TimeUnit.SECONDS));
        assertTrue(countDownLatchAfterSubscribe.await(AWAIT_TEST_TIMEOUT, TimeUnit.SECONDS));

        // Assert the body is the same
        assertArrayEquals(expected,  received.get(AWAIT_TEST_TIMEOUT, TimeUnit.SECONDS));
        // ... and the connection not marked completed (the stream is null in the publisher)
        assertFalse(pubStream.wrappedOutputStream.connectionComplete);
    }

    /**
     * The content-length must remain available even after publisher.close()
     */
    @Test
    public void testContentLengthAfterClose() throws Exception {
        TestPubPipeWrap streams = streams(REQUEST_SIZE);

        assertEquals(REQUEST_SIZE, streams.publisher.contentLength());

        streams.publisher.close();

        assertEquals(REQUEST_SIZE, streams.publisher.contentLength());
    }

    private static TestPubPipeWrap streams(int contentLen) throws Exception {
        PipedInputStream pin = new PipedInputStream(CHUNK_THRESHOLD_SIZE);

        HttpClientWrappedOutputStream wrappedOutputStream =
                mock(HttpClientWrappedOutputStream.class);

        wrappedOutputStream.future = new CompletableFuture<>();
        wrappedOutputStream.contentLen = contentLen;

        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setConnectionTimeout(180);

        HttpClientBodyPublisher publisher =
                new HttpClientBodyPublisher(wrappedOutputStream, () -> pin);

        HttpClientPipedOutputStream pipedOutputStream =
                new HttpClientPipedOutputStream(wrappedOutputStream, pin, policy, publisher);

        wrappedOutputStream.pout = pipedOutputStream;

        return new TestPubPipeWrap(publisher, pipedOutputStream, wrappedOutputStream);
    }

    private static CompletableFuture<byte[]> subscribeAsync(
            HttpClientBodyPublisher publisher,
            CountDownLatch countDownLatchBeforeSubscribe,
            CountDownLatch countDownLatchAfterSubscribe
    ) {

        CompletableFuture<byte[]> received = new CompletableFuture<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                Flow.Subscriber<ByteBuffer> subscriber = new Flow.Subscriber<ByteBuffer>() {
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        countDownLatchAfterSubscribe.countDown();
                        subscription.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(ByteBuffer buffer) {
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        baos.writeBytes(bytes);
                    }

                    @Override
                    public void onError(Throwable error) {
                        received.completeExceptionally(error);
                        executor.shutdown();
                    }

                    @Override
                    public void onComplete() {
                        received.complete(baos.toByteArray());
                        executor.shutdown();
                    }
                };

                countDownLatchBeforeSubscribe.countDown();
                publisher.subscribe(subscriber);

            } catch (Throwable e) {
                received.completeExceptionally(e);
                executor.shutdown();
            }
        });

        return received;
    }

    private static byte[] body(int size) {
        byte[] request = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                .getBytes(StandardCharsets.UTF_8);
        byte[] body = Arrays.copyOf(request, size);
        Arrays.fill(body, Math.min(request.length, size), size, (byte)' ');
        return body;
    }

    private static class TestPubPipeWrap {
        final HttpClientBodyPublisher publisher;
        final HttpClientPipedOutputStream pipedOutputStream;
        final HttpClientWrappedOutputStream wrappedOutputStream;

        TestPubPipeWrap(
                HttpClientBodyPublisher publisher,
                HttpClientPipedOutputStream pipedOutputStream,
                HttpClientWrappedOutputStream wrappedOutputStream
        ) {
            this.publisher = publisher;
            this.pipedOutputStream = pipedOutputStream;
            this.wrappedOutputStream = wrappedOutputStream;
        }
    }
}
