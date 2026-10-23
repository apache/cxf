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
package org.apache.cxf.ws.security.cache.jcache;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for the JCacheReplayCache
 */
public class JCacheReplayCacheTest {

    @Test
    public void testAddIfAbsent() throws Exception {
        JCacheReplayCache replayCache = new JCacheReplayCache("cxf.jcache.replay.test-" + UUID.randomUUID());
        try {
            String id = UUID.randomUUID().toString();
            assertTrue(replayCache.addIfAbsent(id, null));
            assertTrue(replayCache.contains(id));
            assertFalse(replayCache.addIfAbsent(id, null));

            // An identifier added with add() is also seen
            id = UUID.randomUUID().toString();
            replayCache.add(id);
            assertFalse(replayCache.addIfAbsent(id, null));

            // An empty identifier cannot be cached, so is never reported as added
            assertFalse(replayCache.addIfAbsent(null, null));
            assertFalse(replayCache.addIfAbsent("", null));
            assertFalse(replayCache.contains(""));
        } finally {
            replayCache.close();
        }
    }

    /**
     * Concurrent requests carrying the same identifier must not all be accepted: exactly one
     * addIfAbsent call per identifier may succeed.
     */
    @Test
    public void testConcurrentAddIfAbsent() throws Exception {
        JCacheReplayCache replayCache = new JCacheReplayCache("cxf.jcache.replay.test-" + UUID.randomUUID());

        final int threads = 8;
        final int identifiers = 500;
        List<String> ids = new ArrayList<>(identifiers);
        for (int i = 0; i < identifiers; i++) {
            ids.add(UUID.randomUUID().toString());
        }
        AtomicIntegerArray accepted = new AtomicIntegerArray(identifiers);
        CyclicBarrier barrier = new CyclicBarrier(threads);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>(threads);
            for (int t = 0; t < threads; t++) {
                futures.add(executor.submit(() -> {
                    barrier.await();
                    for (int i = 0; i < identifiers; i++) {
                        if (replayCache.addIfAbsent(ids.get(i), null)) {
                            accepted.incrementAndGet(i);
                        }
                    }
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdown();
            replayCache.close();
        }

        for (int i = 0; i < identifiers; i++) {
            assertEquals("Identifier " + i + " was accepted more than once", 1, accepted.get(i));
        }
    }
}
