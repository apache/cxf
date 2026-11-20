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
import java.util.function.Supplier;

import javax.cache.configuration.Configuration;

import org.apache.wss4j.common.cache.EHCacheExpiry;
import org.apache.wss4j.common.cache.EHCacheValue;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.expiry.ExpiryPolicy;
import org.ehcache.jsr107.Eh107Configuration;

/**
 * Evicts each replay identifier when its own expiry has passed, which a JCache expiry policy cannot do
 * as it does not see the value. Only loaded when the caching provider is Ehcache.
 */
final class EhcacheReplayExpiry implements ExpiryPolicy<String, EHCacheValue> {
    private final Clock clock;

    private EhcacheReplayExpiry(Clock clock) {
        this.clock = clock;
    }

    static Configuration<String, EHCacheValue> configuration(Clock clock) {
        // unbounded like a JCache MutableConfiguration, as evicting an unexpired identifier allows a replay
        return Eh107Configuration.fromEhcacheCacheConfiguration(
            CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, EHCacheValue.class,
                ResourcePoolsBuilder.heap(Long.MAX_VALUE))
                .withExpiry(new EhcacheReplayExpiry(clock))
                .build());
    }

    @Override
    public Duration getExpiryForCreation(String key, EHCacheValue value) {
        return remaining(value);
    }

    @Override
    public Duration getExpiryForAccess(String key, Supplier<? extends EHCacheValue> value) {
        return null;
    }

    // unlike EHCacheExpiry, a replaced identifier gets the expiry of its new value
    @Override
    public Duration getExpiryForUpdate(String key, Supplier<? extends EHCacheValue> oldValue,
                                       EHCacheValue newValue) {
        return remaining(newValue);
    }

    private Duration remaining(EHCacheValue value) {
        Instant expiry = value.getExpiry();
        if (expiry == null) {
            return Duration.ofSeconds(EHCacheExpiry.DEFAULT_TTL);
        }
        Duration remaining = Duration.between(clock.instant(), expiry);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
