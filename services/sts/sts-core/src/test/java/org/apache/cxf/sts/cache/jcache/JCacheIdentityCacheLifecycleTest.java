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
package org.apache.cxf.sts.cache.jcache;

import java.util.Collections;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.ehcache.jsr107.EhcacheCachingProvider;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JCacheIdentityCacheLifecycleTest {

    // Both caches share the named cache of the default configuration: closing one leaves it to the other
    @Test
    public void testClosingOneCacheLeavesTheOtherUsable() {
        Bus bus = BusFactory.getDefaultBus();
        EhcacheCachingProvider provider = new EhcacheCachingProvider();
        JCacheIdentityCache first = new JCacheIdentityCache(provider, null, "first", bus, null);
        JCacheIdentityCache second = new JCacheIdentityCache(provider, null, "second", bus, null);
        try {
            Map<String, String> identities = Collections.singletonMap("realmB", "bob");
            first.add("alice", "realmA", identities);
            first.close();
            first.close();
            assertEquals(identities, second.get("alice", "realmA"));
        } finally {
            first.close();
            second.close();
            provider.close();
        }
    }
}
