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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicIntegerArray;

import javax.cache.Cache;
import javax.cache.Caching;

import org.apache.wss4j.common.cache.EHCacheExpiry;
import org.apache.wss4j.common.cache.EHCacheValue;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for the JCacheReplayCache
 */
public class JCacheReplayCacheTest {

    @Test
    public void testClosingOtherCacheAndRepeatedClose() throws Exception {
        String name = UUID.randomUUID().toString();
        JCacheReplayCache first = new JCacheReplayCache(name);
        JCacheReplayCache same = new JCacheReplayCache(name);
        JCacheReplayCache different = new JCacheReplayCache(UUID.randomUUID().toString());
        try {
            first.add("same");
            different.add("different");
            first.close();
            first.close();
            assertTrue(same.contains("same"));
            assertTrue(different.contains("different"));
        } finally {
            first.close();
            same.close();
            different.close();
        }
    }

    @Test
    public void testExplicitExpiryAndReplacement() throws Exception {
        TestClock clock = new TestClock();
        JCacheReplayCache cache = new JCacheReplayCache(UUID.randomUUID().toString(), 10000, clock);
        try {
            cache.add("short", clock.instant().plusSeconds(1));
            assertTrue(cache.addIfAbsent("atomic", clock.instant().plusSeconds(1)));
            assertTrue(cache.contains("short"));
            clock.advance(2);
            assertFalse(cache.contains("short"));
            assertTrue(cache.addIfAbsent("atomic", clock.instant().plusSeconds(30)));
            assertFalse(cache.addIfAbsent("atomic", null));
            clock.advance(31);
            assertFalse(cache.contains("atomic"));
        } finally {
            cache.close();
        }
    }

    @Test
    public void testLongExpiryAndDefaultBounds() throws Exception {
        TestClock clock = new TestClock();
        JCacheReplayCache cache = new JCacheReplayCache(UUID.randomUUID().toString(), 10000, clock);
        try {
            Instant longExpiry = clock.instant().plusSeconds(7200);
            cache.add("long", longExpiry);
            cache.add("default");
            cache.add("past", clock.instant().minusSeconds(1));
            cache.add("now", clock.instant());
            cache.add("too-long", clock.instant().plusSeconds(43201));
            assertEquals(longExpiry, cache.get("long").getExpiry());
            assertEquals(clock.instant().plusSeconds(3600), cache.get("past").getExpiry());
            assertEquals(cache.get("past").getExpiry(), cache.get("now").getExpiry());
            assertEquals(cache.get("past").getExpiry(), cache.get("too-long").getExpiry());
            clock.advance(3601);
            assertTrue(cache.contains("long"));
            assertFalse(cache.contains("default"));
            assertFalse(cache.contains("past"));
            clock.advance(3600);
            assertFalse(cache.contains("long"));
        } finally {
            cache.close();
        }
    }

    /**
     * Ehcache evicts an identifier at its own expiry, even if it is not looked up again, and a
     * replaced identifier gets the expiry of its new value.
     */
    @Test
    public void testEhcacheEvictsAtEntryExpiry() throws Exception {
        String name = UUID.randomUUID().toString();
        JCacheReplayCache cache = new JCacheReplayCache(name);
        try {
            Cache<String, EHCacheValue> store = Caching.getCachingProvider().getCacheManager()
                .getCache(name, String.class, EHCacheValue.class);
            cache.add("short", Instant.now().plusSeconds(1));
            cache.add("replaced", Instant.now().plusSeconds(1));
            cache.add("replaced", Instant.now().plusSeconds(60));
            Thread.sleep(1500);
            assertFalse(store.containsKey("short"));
            assertTrue(store.containsKey("replaced"));
        } finally {
            cache.close();
        }
    }

    private static final class TestClock extends Clock {
        private Instant now = Instant.now();

        void advance(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

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
        assertConcurrentAddIfAbsent(false);
    }

    @Test
    public void testConcurrentAddIfAbsentAfterExpiry() throws Exception {
        assertConcurrentAddIfAbsent(true);
    }

    private void assertConcurrentAddIfAbsent(boolean expired) throws Exception {
        TestClock clock = new TestClock();
        JCacheReplayCache replayCache = new JCacheReplayCache(
            "cxf.jcache.replay.test-" + UUID.randomUUID(), 10000, clock);

        final int threads = 8;
        final int identifiers = 500;
        List<String> ids = new ArrayList<>(identifiers);
        for (int i = 0; i < identifiers; i++) {
            ids.add(UUID.randomUUID().toString());
        }
        if (expired) {
            for (String id : ids) {
                replayCache.add(id, clock.instant().plusSeconds(1));
            }
            clock.advance(2);
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

    // An identifier whose expiry is the max TTL away is held until then
    @Test
    public void testMaxTTL() throws Exception {
        TestClock clock = new TestClock();
        JCacheReplayCache replayCache =
            new JCacheReplayCache("cxf.jcache.replay.test-" + UUID.randomUUID(), 10000, clock);
        try {
            Duration maxTTL = replayCache.getMaxTTL();
            assertEquals(Duration.ofSeconds(EHCacheExpiry.MAX_TTL), maxTTL);
            assertTrue(replayCache.addIfAbsent("id", clock.instant().plus(maxTTL)));
            clock.advance(maxTTL.getSeconds() - 1);
            assertFalse(replayCache.addIfAbsent("id", clock.instant().plusSeconds(60)));
            clock.advance(1);
            assertFalse(replayCache.contains("id"));
        } finally {
            replayCache.close();
        }
    }
}
