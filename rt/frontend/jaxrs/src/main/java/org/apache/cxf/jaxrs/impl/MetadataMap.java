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

package org.apache.cxf.jaxrs.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jakarta.ws.rs.core.MultivaluedMap;

public class MetadataMap<K, V> implements MultivaluedMap<K, V> {

    private boolean caseInsensitive;
    private boolean readOnly;
    private Map<K, List<V>> m;

    public MetadataMap() {
        this.m = new LinkedHashMap<>();
    }

    public MetadataMap(int size) {
        this.m = new LinkedHashMap<>(size);
    }

    public MetadataMap(Map<K, List<V>> store) {
        this(store, true);
    }

    public MetadataMap(Map<K, List<V>> store, boolean copy) {
        this(store, copy, false, false);
    }

    public MetadataMap(boolean readOnly, boolean caseInsensitive) {
        this(null, readOnly, caseInsensitive);
    }

    public MetadataMap(Map<K, List<V>> store, boolean readOnly, boolean caseInsensitive) {

        this(store, true, readOnly, caseInsensitive);

    }

    public MetadataMap(Map<K, List<V>> store, boolean copyStore,
                       boolean readOnly, boolean caseInsensitive) {

        if (copyStore) {
            this.m = new LinkedHashMap<>();
            if (store != null) {
                for (Map.Entry<K, List<V>> entry : store.entrySet()) {
                    List<V> values = new ArrayList<>(entry.getValue());
                    m.put(entry.getKey(), values);
                }
            }
        } else {
            this.m = store;
        }
        if (readOnly) {
            this.m = Collections.unmodifiableMap(m);
        }
        this.caseInsensitive = caseInsensitive;
        this.readOnly = readOnly;

    }

    public void add(K key, V value) {
        addValue(key, value, true);
    }

    private void addValue(K key, V value, boolean last) {
        List<V> data = getList(key);
        try {
            if (last) {
                data.add(value);
            } else {
                data.add(0, value);
            }
        } catch (UnsupportedOperationException ex) {
            // this may happen if an unmodifiable List was set via put or putAll
            if (!readOnly) {
                List<V> newList = new ArrayList<>(data);
                put(key, newList);
                addValue(key, value, last);
            } else {
                throw ex;
            }
        }
    }

    private List<V> getList(K key) {
        List<V> data = this.get(key);
        if (data == null) {
            data = new ArrayList<>();
            m.put(key, data);
        }
        return readOnly ? Collections.unmodifiableList(data) : data;
    }

    public V getFirst(K key) {
        List<V> data = this.get(key);
        return data == null || data.isEmpty() ? null : data.get(0);
    }

    public void putSingle(K key, V value) {
        List<V> data = new ArrayList<>();
        data.add(value);
        this.put(key, data);
    }

    public void clear() {
        m.clear();
    }

    public boolean containsKey(Object key) {
        if (!caseInsensitive) {
            return m.containsKey(key);
        }
        return getMatchingKey(key) != null;
    }

    public boolean containsValue(Object value) {
        return m.containsValue(value);
    }

    public Set<Entry<K, List<V>>> entrySet() {
        return m.entrySet();
    }

    public List<V> get(Object key) {
        if (!caseInsensitive || key == null) {
            return m.get(key);
        }
        K realKey = getMatchingKey(key);
        return realKey == null ? null : m.get(realKey);
    }

    private K getMatchingKey(Object key) {
        for (K entry : m.keySet()) {
            if (entry != null && entry.toString().equalsIgnoreCase(key.toString())
                || entry == null && key == null) {
                return entry;
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return m.isEmpty();
    }

    public Set<K> keySet() {
        if (!caseInsensitive) {
            return m.keySet();
        }
        Set<K> set = new TreeSet<>(new KeyComparator<K>());
        set.addAll(m.keySet());
        return set;
    }

    public List<V> put(K key, List<V> value) {
        K realKey = !caseInsensitive || key == null ? key : getMatchingKey(key);
        return m.put(realKey == null ? key : realKey, value);
    }

    public void putAll(Map<? extends K, ? extends List<V>> map) {
        if (!caseInsensitive) {
            m.putAll(map);
        } else {
            for (Map.Entry<? extends K, ? extends List<V>> entry : map.entrySet()) {
                this.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public List<V> remove(Object key) {
        if (caseInsensitive) {
            K realKey = getMatchingKey(key);
            return m.remove(realKey == null ? key : realKey);
        }
        return m.remove(key);
    }

    public int size() {
        return m.size();
    }

    public Collection<List<V>> values() {
        return m.values();
    }

    @Override
    public int hashCode() {
        return m.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return m.equals(o);
    }

    public String toString() {
        return m.toString();
    }

    private static final class KeyComparator<K> implements Comparator<K> {

        public int compare(K k1, K k2) {
            String s1 = k1.toString();
            String s2 = k2.toString();
            return s1.compareToIgnoreCase(s2);
        }

    }

    @SafeVarargs
    public final void addAll(K key, V... newValues) {
        this.addAllValues(key, Arrays.asList(newValues));
    }

    public void addAll(K key, List<V> newValues) {
        this.addAllValues(key, newValues);
    }

    private void addAllValues(K key, List<V> newValues) {
        if (newValues == null) {
            throw new NullPointerException("List is empty");
        }
        if (newValues.isEmpty()) {
            return;
        }
        getList(key).addAll(newValues);
    }

    public void addFirst(K key, V value) {
        addValue(key, value, false);
    }

    public boolean equalsIgnoreValueOrder(MultivaluedMap<K, V> map) {
        Set<K> mapKeys = map.keySet();
        if (mapKeys.size() != m.keySet().size()) {
            return false;
        }

        for (K key : mapKeys) {
            List<V> localValues = this.get(key);
            List<V> mapValues = map.get(key);
            if (localValues == null
                || localValues.size() != mapValues.size()
                || !localValues.containsAll(mapValues)) {
                return false;
            }
        }
        return true;
    }
}
