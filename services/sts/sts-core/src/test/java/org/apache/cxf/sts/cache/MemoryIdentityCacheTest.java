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
package org.apache.cxf.sts.cache;

import java.io.Closeable;
import java.io.IOException;

import org.apache.cxf.sts.IdentityMapper;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MemoryIdentityCacheTest {

    // tests TokenStore apis for storing in the cache.
    @org.junit.Test
    public void testOneMapping() throws Exception {
        AbstractIdentityCache cache = getIdentityCache(new CacheIdentityMapper());

        cache.mapPrincipal("REALM_A", new CustomTokenPrincipal("user_aaa"), "REALM_B");
        assertNotNull(cache.get("user_aaa", "REALM_A"));
        assertNotNull(cache.get("user_bbb", "REALM_B"));

        if (cache instanceof Closeable) {
            ((Closeable)cache).close();
        }
    }

    @org.junit.Test
    public void testTwoDistinctMappings() throws IOException {
        AbstractIdentityCache cache = getIdentityCache(new CacheIdentityMapper());

        cache.mapPrincipal("REALM_A", new CustomTokenPrincipal("user_aaa"), "REALM_B");
        cache.mapPrincipal("REALM_C", new CustomTokenPrincipal("user_ccc"), "REALM_D");
        assertNotNull(cache.get("user_aaa", "REALM_A"));
        assertNotNull(cache.get("user_bbb", "REALM_B"));
        assertNotNull(cache.get("user_ccc", "REALM_C"));
        assertNotNull(cache.get("user_ddd", "REALM_D"));

        if (cache instanceof Closeable) {
            ((Closeable)cache).close();
        }
    }

    @org.junit.Test
    public void testTwoDistinctAndOneRelatedMapping() throws IOException {
        AbstractIdentityCache cache = getIdentityCache(new CacheIdentityMapper());

        cache.mapPrincipal("REALM_A", new CustomTokenPrincipal("user_aaa"), "REALM_B");
        cache.mapPrincipal("REALM_C", new CustomTokenPrincipal("user_ccc"), "REALM_D");
        cache.mapPrincipal("REALM_A", new CustomTokenPrincipal("user_aaa"), "REALM_D");
        //now, mapping from A -> D and B -> D are cached as well
        assertNotNull(cache.get("user_aaa", "REALM_A"));
        assertNotNull(cache.get("user_bbb", "REALM_B"));
        assertNotNull(cache.get("user_ccc", "REALM_C"));
        assertNotNull(cache.get("user_ddd", "REALM_D"));
        assertEquals(4, cache.get("user_aaa", "REALM_A").size());
        assertEquals(4, cache.get("user_bbb", "REALM_B").size());
        assertEquals(4, cache.get("user_ccc", "REALM_C").size());
        assertEquals(4, cache.get("user_ddd", "REALM_D").size());

        if (cache instanceof Closeable) {
            ((Closeable)cache).close();
        }
    }

    @org.junit.Test
    public void testTwoDistinctAndTwoRelatedMapping() throws IOException {
        AbstractIdentityCache cache = getIdentityCache(new CacheIdentityMapper());

        cache.mapPrincipal("REALM_A", new CustomTokenPrincipal("user_aaa"), "REALM_B");
        cache.mapPrincipal("REALM_D", new CustomTokenPrincipal("user_ddd"), "REALM_E");
        //No Mapping occured between A,B and D,E (C not involved at all)
        assertEquals(2, cache.get("user_aaa", "REALM_A").size());
        assertEquals(2, cache.get("user_bbb", "REALM_B").size());
        assertEquals(2, cache.get("user_ddd", "REALM_D").size());
        assertEquals(2, cache.get("user_eee", "REALM_E").size());

        cache.mapPrincipal("REALM_B", new CustomTokenPrincipal("user_bbb"), "REALM_C");
        assertNotNull(cache.get("user_aaa", "REALM_A"));
        assertNotNull(cache.get("user_bbb", "REALM_B"));
        assertNotNull(cache.get("user_ccc", "REALM_C"));
        assertNotNull(cache.get("user_ddd", "REALM_D"));
        assertNotNull(cache.get("user_eee", "REALM_E"));
        assertEquals(3, cache.get("user_aaa", "REALM_A").size());
        assertEquals(3, cache.get("user_bbb", "REALM_B").size());
        assertEquals(3, cache.get("user_ccc", "REALM_C").size());
        //No mapping occurred between A,B,C and D,E -> distinct
        assertEquals(2, cache.get("user_ddd", "REALM_D").size());
        assertEquals(2, cache.get("user_eee", "REALM_E").size());

        cache.mapPrincipal("REALM_C", new CustomTokenPrincipal("user_ccc"), "REALM_E");
        //All mappings are known now
        assertNotNull(cache.get("user_aaa", "REALM_A"));
        assertNotNull(cache.get("user_bbb", "REALM_B"));
        assertNotNull(cache.get("user_ccc", "REALM_C"));
        assertNotNull(cache.get("user_ddd", "REALM_D"));
        assertNotNull(cache.get("user_eee", "REALM_E"));
        assertEquals(5, cache.get("user_aaa", "REALM_A").size());
        assertEquals(5, cache.get("user_bbb", "REALM_B").size());
        assertEquals(5, cache.get("user_ccc", "REALM_C").size());
        assertEquals(5, cache.get("user_ddd", "REALM_D").size());
        assertEquals(5, cache.get("user_eee", "REALM_E").size());

        if (cache instanceof Closeable) {
            ((Closeable)cache).close();
        }
    }

    protected AbstractIdentityCache getIdentityCache(IdentityMapper mapper) {
        return new MemoryIdentityCache(mapper);
    }
}
