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

package org.apache.cxf.ws.security.cache;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.CacheManager;

/**
 * We need to reference count the EHCacheManager things
 */
public final class EHCacheManagerHolder {
    private static final ConcurrentHashMap<String, AtomicInteger> COUNTS 
        = new ConcurrentHashMap<String, AtomicInteger>(8, 0.75f, 2);
    
    private EHCacheManagerHolder() {
        //utility
    }
    
    public static CacheManager getCacheManager(URL configFileURL) {
        CacheManager cacheManager;
        if (configFileURL == null) {
            cacheManager = CacheManager.create();
        } else {
            cacheManager = CacheManager.create(configFileURL);
        }
        AtomicInteger a = COUNTS.get(cacheManager.getName());
        if (a == null) {
            COUNTS.putIfAbsent(cacheManager.getName(), new AtomicInteger());
            a = COUNTS.get(cacheManager.getName());
        }
        a.incrementAndGet();
        return cacheManager;
    }
    
    public static void releaseCacheManger(CacheManager cacheManager) {
        AtomicInteger a = COUNTS.get(cacheManager.getName());
        if (a.decrementAndGet() == 0) {
            cacheManager.shutdown();
        }
    }
    
}
