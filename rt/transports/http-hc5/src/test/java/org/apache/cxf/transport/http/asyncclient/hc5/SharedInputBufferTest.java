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

package org.apache.cxf.transport.http.asyncclient.hc5;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;

public class SharedInputBufferTest {

    @Test
    public void testReaderWokenUpWhenDataBlockFillsBuffer() throws Exception {
        final CountDownLatch readerWaiting = new CountDownLatch(1);
        final SharedInputBuffer inbuf = new SharedInputBuffer(1024) {
            @Override
            protected void waitForData(int waitPos) throws IOException {
                readerWaiting.countDown();
                super.waitForData(waitPos);
            }
        };
        final byte[] data = new byte[4096];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte)i;
        }

        ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        try {
            Future<byte[]> reader = executor.submit(() -> {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] b = new byte[256];
                int n;
                while ((n = inbuf.read(b, 0, b.length)) != -1) {
                    out.write(b, 0, n);
                }
                return out.toByteArray();
            });
            executor.submit(() -> {
                // The reader holds the lock until it waits on it, so the data is consumed while
                // the reader is waiting
                readerWaiting.await();
                // A block larger than the reader's array plus the buffer size fills the buffer,
                // and the I/O thread waits until the reader has drained it
                inbuf.consumeContent(ByteBuffer.wrap(data), false);
                inbuf.consumeContent(ByteBuffer.allocate(0), true);
                return null;
            });

            assertArrayEquals(data, reader.get(10, TimeUnit.SECONDS));
        } finally {
            inbuf.shutdown();
            executor.shutdownNow();
        }
    }
    
    @Test
    public void testCloseWhenDataBlockFillsBuffer() throws Exception {
        final CountDownLatch consumerWaiting = new CountDownLatch(1);
        final SharedInputBuffer inbuf = new SharedInputBuffer(1024);
        final byte[] data = new byte[4096];

        final Thread t = new Thread(() -> {
            try {
                // No reader, the consumer should be blocked waiting for input
                inbuf.consumeContent(ByteBuffer.wrap(data), false);
                consumerWaiting.countDown();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });

        try {
            t.start();

            // Await until the consumer thread enters the WAITING state (waiting for condition) 
            Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(t.getState(), equalTo(Thread.State.WAITING))
            );

            inbuf.close();
            assertThat(consumerWaiting.await(10, TimeUnit.SECONDS), equalTo(true));
        } finally {
            inbuf.shutdown();
        }
    }
}
