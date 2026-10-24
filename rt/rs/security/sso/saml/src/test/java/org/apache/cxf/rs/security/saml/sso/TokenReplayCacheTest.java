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

package org.apache.cxf.rs.security.saml.sso;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.apache.cxf.rs.security.saml.sso.jcache.JCacheTokenReplayCache;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for the TokenReplayCache implementations
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class TokenReplayCacheTest {
    private final Class<? extends TokenReplayCache<String>> tokenReplayCacheClass;
    
    public TokenReplayCacheTest(Class<? extends TokenReplayCache<String>> tokenReplayCacheClass) {
        this.tokenReplayCacheClass = tokenReplayCacheClass;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Class<? extends TokenReplayCache<String>>> data() {
        return Arrays.asList(EHCacheTokenReplayCache.class, JCacheTokenReplayCache.class);
    }

    @Test
    public void testTokenReplayCacheTokenReplayCache() throws Exception {

        TokenReplayCache<String> replayCache = createCache();

        testTokenReplayCacheInstance(replayCache);

        replayCache.close();
    }

    @Test
    public void testTokenReplayCacheCloseCacheTwice() throws Exception {
        TokenReplayCache<String> replayCache = createCache();
        replayCache.close();
        replayCache.close();
    }

    // No expiry specified so it falls back to the default
    @Test
    public void testTokenReplayCacheNoExpirySpecified() throws Exception {
        TokenReplayCache<String> replayCache = createCache();

        String id = UUID.randomUUID().toString();
        replayCache.putId(id);
        assertTrue(replayCache.contains(id));

        replayCache.close();
    }

    // The negative expiry is rejected and it falls back to the default
    @Test
    public void testTokenReplayCacheNegativeExpiry() throws Exception {
        TokenReplayCache<String> replayCache = createCache();

        String id = UUID.randomUUID().toString();
        replayCache.putId(id, Instant.now().minusSeconds(100L));
        assertTrue(replayCache.contains(id));

        replayCache.close();
    }

    // The huge expiry is rejected and it falls back to the default
    @Test
    public void testTokenReplayCacheHugeExpiry() throws Exception {
        TokenReplayCache<String> replayCache = createCache();

        String id = UUID.randomUUID().toString();
        replayCache.putId(id, Instant.now().plus(14, ChronoUnit.HOURS));
        assertTrue(replayCache.contains(id));

        replayCache.close();
    }

    @Test
    public void testTokenReplayCachePutIdIfAbsent() throws Exception {
        TokenReplayCache<String> replayCache = createCache();
        try {
            String id = UUID.randomUUID().toString();
            assertTrue(replayCache.putIdIfAbsent(id, null));
            assertTrue(replayCache.contains(id));
            assertFalse(replayCache.putIdIfAbsent(id, null));
            assertFalse(replayCache.putIdIfAbsent(id, Instant.now().plusSeconds(100L)));

            // An identifier added with putId() is also seen
            id = UUID.randomUUID().toString();
            replayCache.putId(id);
            assertFalse(replayCache.putIdIfAbsent(id, null));

            // An empty identifier cannot be cached, so is never reported as added
            assertFalse(replayCache.putIdIfAbsent(null, null));
            assertFalse(replayCache.putIdIfAbsent("", null));
            assertFalse(replayCache.contains(""));
        } finally {
            replayCache.close();
        }
    }

    // Concurrent requests carrying the same identifier must not all be accepted: exactly one
    // putIdIfAbsent call per identifier may succeed.
    @Test
    public void testTokenReplayCacheConcurrentPutIdIfAbsent() throws Exception {
        TokenReplayCache<String> replayCache = createCache();

        final int threads = 8;
        final int identifiers = 500;
        List<String> ids = new ArrayList<>(identifiers);
        for (int i = 0; i < identifiers; i++) {
            ids.add(UUID.randomUUID().toString());
        }
        AtomicIntegerArray accepted = new AtomicIntegerArray(identifiers);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        Instant expiry = Instant.now().plusSeconds(100L);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>(threads);
            for (int t = 0; t < threads; t++) {
                futures.add(executor.submit(() -> {
                    barrier.await();
                    for (int i = 0; i < identifiers; i++) {
                        if (replayCache.putIdIfAbsent(ids.get(i), expiry)) {
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

    private void testTokenReplayCacheInstance(TokenReplayCache<String> replayCache) throws InterruptedException {

        // Test default TTL caches OK
        String id = UUID.randomUUID().toString();
        replayCache.putId(id);
        assertTrue(replayCache.contains(id));

        // Test specifying TTL caches OK
        id = UUID.randomUUID().toString();
        replayCache.putId(id, Instant.now().plusSeconds(100L));
        assertTrue(replayCache.contains(id));

        // Test expiration
        id = UUID.randomUUID().toString();
        replayCache.putId(id, Instant.now().plusSeconds(1L));
        Thread.sleep(1250L);
        assertFalse(replayCache.contains(id));

    }

    private TokenReplayCache<String> createCache() throws Exception {
        return tokenReplayCacheClass.getDeclaredConstructor().newInstance();
    }
}
