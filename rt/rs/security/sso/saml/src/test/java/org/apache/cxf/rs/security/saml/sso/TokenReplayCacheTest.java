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
import java.util.UUID;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for the TokenReplayCache implementations
 */
public class TokenReplayCacheTest {

    @Test
    public void testEhCacheTokenReplayCache() throws Exception {

        TokenReplayCache<String> replayCache = new EHCacheTokenReplayCache();

        testTokenReplayCacheInstance(replayCache);

        replayCache.close();
    }

    @Test
    public void testEhCacheCloseCacheTwice() throws Exception {
        TokenReplayCache<String> replayCache = new EHCacheTokenReplayCache();
        replayCache.close();
        replayCache.close();
    }

    // No expiry specified so it falls back to the default
    @Test
    public void testEhCacheTokenReplayCacheNoExpirySpecified() throws Exception {
        TokenReplayCache<String> replayCache = new EHCacheTokenReplayCache();

        String id = UUID.randomUUID().toString();
        replayCache.putId(id);
        assertTrue(replayCache.contains(id));

        replayCache.close();
    }

    // The negative expiry is rejected and it falls back to the default
    @Test
    public void testEhCacheTokenReplayCacheNegativeExpiry() throws Exception {
        TokenReplayCache<String> replayCache = new EHCacheTokenReplayCache();

        String id = UUID.randomUUID().toString();
        replayCache.putId(id, Instant.now().minusSeconds(100L));
        assertTrue(replayCache.contains(id));

        replayCache.close();
    }

    // The huge expiry is rejected and it falls back to the default
    @Test
    public void testEhCacheTokenReplayCacheHugeExpiry() throws Exception {
        TokenReplayCache<String> replayCache = new EHCacheTokenReplayCache();

        String id = UUID.randomUUID().toString();
        replayCache.putId(id, Instant.now().plus(14, ChronoUnit.HOURS));
        assertTrue(replayCache.contains(id));

        replayCache.close();
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
}