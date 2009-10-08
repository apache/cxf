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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.cxf.helpers.CastUtils;

/**
 * A Map backed by a limted capacity strong HashMap ("eden") containing
 * the youngest entries, overflowing into an unlimted WeakMapHash ("aged")
 * containing the older entries.
 * Used to avoid caching schemes being frustrated by over-agressive GC.
 * 
 * @param <K> key type
 * @param <V> value type
 */
public class TwoStageMap<K, V> implements Map<K, V> {

    private static final int DEFAULT_EDEN_CAPACITY = 50;

    private final int edenCapacity;
    private Map<K, V> eden;
    private List<K> edenKeys;
    private Map<K, V> aged;
    
    public TwoStageMap() {
        this(DEFAULT_EDEN_CAPACITY);
    }
    
    public TwoStageMap(int c) {
        edenCapacity = c;        
        eden = new HashMap<K, V>(edenCapacity);
        edenKeys = new ArrayList<K>(edenCapacity);
        aged = new WeakHashMap<K, V>();
    }
    
    public synchronized int size() {
        return eden.size() + aged.size();
    }

    public synchronized boolean isEmpty() {
        return eden.isEmpty() && aged.isEmpty();
    }

    public synchronized boolean containsKey(Object key) {
        return eden.containsKey(key) || aged.containsKey(key);
    }

    public synchronized boolean containsValue(Object value) {
        return eden.containsValue(value) || aged.containsValue(value);
    }

    public synchronized V get(Object key) {
        V edenValue = eden.get(key);
        return edenValue != null ? edenValue : aged.get(key);
    }

    public synchronized V put(K key, V value) {
        if (eden.size() >= edenCapacity) {
            K victimKey = edenKeys.remove(0);
            aged.put(victimKey, eden.remove(victimKey));            
        }
        edenKeys.add(key);
        return eden.put(key, value);
    }

    public synchronized V remove(Object key) {
        V victim = null;
        if (eden.containsKey(key)) {
            edenKeys.remove(key);
            victim = eden.remove(key);
        } else {
            victim = aged.remove(key);
        }
        return victim;
    }

    public synchronized void putAll(Map<? extends K, ? extends V> t) {
        Set<Entry<? extends K, ? extends V>> entries =
            CastUtils.cast(t.entrySet());
        for (Entry<? extends K, ? extends V> entry : entries) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public synchronized void clear() {
        eden.clear();
        edenKeys.clear();
        aged.clear();
    }

    public synchronized Set<K> keySet() {
        Set<K> keys = eden.keySet();
        keys.addAll(aged.keySet());
        return keys;
    }

    public synchronized Collection<V> values() {
        Collection<V> values = eden.values();
        values.addAll(aged.values());
        return values;
    }

    public synchronized Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> entries = eden.entrySet();
        entries.addAll(aged.entrySet());
        return entries;
    }
}
