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

import org.junit.BeforeClass;

public class HazelCastCacheTest extends org.junit.Assert {
  
    private static STSCache cache;
    
    @BeforeClass
    public static void init() throws Exception {
        cache = new HazelCastCache("default");
    }
    
    // tests STSCache apis for storing in the cache.
    @org.junit.Test
    public void testCacheStore() {
        String key = "key";
        String value = "value";
        cache.put(key, value);
        assertEquals(value, cache.get(key));
        cache.remove(key);
        assertNull(cache.get(key));
    }
    
    // tests STSCache apis for removing from the cache.
    @org.junit.Test
    public void testCacheRemove() {
        cache.put("test1", "test1");
        cache.put("test2", "test2");
        cache.put("test3", "test3");
        assertTrue(cache.size() == 3);
        assertTrue(cache.remove("test3"));
        assertFalse(cache.remove("test3"));
        assertNull(cache.get("test3"));
        cache.removeAll();
        assertTrue(cache.size() == 0);
    }
}
