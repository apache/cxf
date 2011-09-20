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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

public class DefaultInMemoryCache implements STSCache {

    private Cache cache;
    private CacheManager cacheManager = CacheManager.create();
    
    public DefaultInMemoryCache() {
        cache = new Cache("STS", 5000, false, false, 3600, 3600);
        cacheManager.addCache(cache);
    }
    
    public Object get(Object key) {
        Element element = cache.get(key);
        if (element != null) {
            return element.getObjectValue();
        } else {
            return element;
        }
    }

    public void put(Object key, Object value) {
        cache.put(new Element(key, value));
    }

    public boolean remove(Object key) {
        return cache.remove(key);
    }

    public void removeAll() {
        cache.removeAll();
    }

    public int size() {
        return cache.getSize();
    }

    public void put(Object key, Object value, Integer timeToLiveSeconds) {
        cache.put(new Element(key, value, false, timeToLiveSeconds, timeToLiveSeconds));
    }

}
