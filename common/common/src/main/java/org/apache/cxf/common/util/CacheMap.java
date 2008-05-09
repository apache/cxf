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
package org.apache.cxf.common.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;


/**
 * Implements a useful caching map. It weakly references the keys, 
 * but strongly references the data. It works much like the WeakHashMap,
 * in that when the keys are garbage collected, the data is removed from 
 * the map.
 * 
 * The main difference is that keys used for lookups don't have to be "=="
 * the same to maintain the data in the cache.  Basically, lookups in this 
 * map use a ".equals" compare, but the keys are then stored with a "==" 
 * compare so if the original key is garbage collected, the other keys that
 * may reference the data keep the data in the cache.
 *
 * <b>
 * Note that this implementation is not synchronized. Not even a little. 
 * 'Read-only' operations can trigger internal modifications. If you share this 
 * class between threads, you must protect every operation.
 * </b>
 */
public class CacheMap<K, V> implements Map<K, V> {
    Map<K, V> mainDataMap = new WeakHashMap<K, V>();
    Map<K, V> extraKeyMap = new WeakIdentityHashMap<K, V>();

    public void clear() {
        mainDataMap.clear();
        extraKeyMap.clear();
    }

    private void updateMainDataMap() {
        //if the singleton in the mainDataMap has been garbage collected, 
        //we'll copy another version of it from the extraKeyMap
        for (K o : extraKeyMap.keySet()) {
            if (!mainDataMap.containsKey(o)) {
                mainDataMap.put(o, extraKeyMap.get(o));
            }
        }
    }
    
    public boolean containsKey(Object key) {
        if (!mainDataMap.containsKey(key)) {
            updateMainDataMap();
            return mainDataMap.containsKey(key);
        }
        return true;
    }

    public boolean containsValue(Object value) {
        return mainDataMap.containsValue(value)
            || extraKeyMap.containsValue(value);
    }

    public Set<java.util.Map.Entry<K, V>> entrySet() {
        updateMainDataMap();
        return mainDataMap.entrySet();
    }

    @SuppressWarnings("unchecked")
    public V get(Object key) {
        V val = mainDataMap.get(key);
        if (val == null) {
            updateMainDataMap();
            val = mainDataMap.get(val);
        }
        if (val != null) {
            extraKeyMap.put((K)key, val);
        }
        return val;
    }

    public boolean isEmpty() {
        return mainDataMap.isEmpty() && extraKeyMap.isEmpty();
    }

    public Set<K> keySet() {
        updateMainDataMap();
        return mainDataMap.keySet();
    }

    public V put(K key, V value) {
        V v = mainDataMap.put(key, value);
        V v2 = extraKeyMap.put(key, value);
        return v == null ? v2 : v;
    }

    public void putAll(Map<? extends K, ? extends V> t) {
        for (Map.Entry<? extends K, ? extends V> ent : t.entrySet()) {
            put(ent.getKey(), ent.getValue());
        }
    }

    public V remove(Object key) {
        V v = mainDataMap.remove(key);
        V v2 = extraKeyMap.remove(key);
        return v == null ? v2 : v;
    }

    public int size() {
        updateMainDataMap();
        return mainDataMap.size();
    }

    public Collection<V> values() {
        updateMainDataMap();
        return mainDataMap.values();
    }
    
    public String toString() {
        updateMainDataMap();
        return mainDataMap.toString();
    }
}
