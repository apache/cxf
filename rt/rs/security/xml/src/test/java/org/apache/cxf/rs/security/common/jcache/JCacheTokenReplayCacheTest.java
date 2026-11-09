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
package org.apache.cxf.rs.security.common.jcache;

import java.util.concurrent.TimeUnit;

import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.cache.expiry.ExpiryPolicy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Some unit tests for how JCacheTokenReplayCache works out its max TTL from a JCache expiry policy
 */
public class JCacheTokenReplayCacheTest {

    @Test
    public void testCreatedExpiryPolicy() {
        assertEquals(java.time.Duration.ofHours(12), JCacheTokenReplayCache.getMaxTTL(
            CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.HOURS, 12)).create()));
    }

    // An access only extends how long an entry is held, so the creation duration is the max TTL
    @Test
    public void testAccessedExpiryPolicy() {
        assertEquals(java.time.Duration.ofMinutes(5), JCacheTokenReplayCache.getMaxTTL(
            AccessedExpiryPolicy.factoryOf(new Duration(TimeUnit.MINUTES, 5)).create()));
    }

    // A cache that does not expire entries has no max TTL
    @Test
    public void testEternalExpiryPolicy() {
        assertNull(JCacheTokenReplayCache.getMaxTTL(EternalExpiryPolicy.factoryOf().create()));
    }

    // An entry can be held for less than its creation duration if an access or an update shortens it
    @Test
    public void testShortenedOnAccessOrUpdate() {
        assertEquals(java.time.Duration.ofMinutes(10), JCacheTokenReplayCache.getMaxTTL(
            new TestExpiryPolicy(new Duration(TimeUnit.HOURS, 1), null, new Duration(TimeUnit.MINUTES, 10))));
        assertEquals(java.time.Duration.ZERO, JCacheTokenReplayCache.getMaxTTL(
            new TestExpiryPolicy(new Duration(TimeUnit.HOURS, 1), Duration.ZERO, null)));
    }

    private static final class TestExpiryPolicy implements ExpiryPolicy {
        private final Duration creation;
        private final Duration access;
        private final Duration update;

        TestExpiryPolicy(Duration creation, Duration access, Duration update) {
            this.creation = creation;
            this.access = access;
            this.update = update;
        }

        @Override
        public Duration getExpiryForCreation() {
            return creation;
        }

        @Override
        public Duration getExpiryForAccess() {
            return access;
        }

        @Override
        public Duration getExpiryForUpdate() {
            return update;
        }
    }
}
