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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

public class MetadataMap<K, V> implements MultivaluedMap<K, V> {

    private boolean caseInsensitive;
    private Map<K, List<V>> m;
    
    public MetadataMap() {
        this.m = new LinkedHashMap<K, List<V>>();
    }
    
    public MetadataMap(Map<K, List<V>> store) {
        this(store, false, false);
    }
    
    public MetadataMap(boolean readOnly, boolean caseInsensitive) {
        this(null, readOnly, caseInsensitive);
    }
    
    public MetadataMap(Map<K, List<V>> store, boolean readOnly, boolean caseInsensitive) {
        
        this.caseInsensitive = caseInsensitive;
        
        this.m = new LinkedHashMap<K, List<V>>();
        if (store != null) {
            for (Map.Entry<K, List<V>> entry : store.entrySet()) {
                List<V> values = new ArrayList<V>(entry.getValue());
                m.put(entry.getKey(), readOnly 
                      ? Collections.unmodifiableList(values) : values);
            }
        }
        if (readOnly) {
            this.m = Collections.unmodifiableMap(m);
        }
        
    }
    
    public void add(K key, V value) {
        List<V> data = this.get(key);
        if (data == null) {
            data = new ArrayList<V>();    
            m.put(key, data);
        }
        data.add(value);
    }

    public V getFirst(K key) {
        List<V> data = this.get(key);
        return data == null ? null : data.get(0);
    }

    public void putSingle(K key, V value) {
        List<V> data = new ArrayList<V>();
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
        for (K theKey : m.keySet()) {
            if (theKey.toString().toLowerCase().equals(key.toString().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public boolean containsValue(Object value) {
        return m.containsValue(value);
    }

    public Set<Entry<K, List<V>>> entrySet() {
        return m.entrySet();
    }

    public List<V> get(Object key) {
        if (!caseInsensitive) {
            return m.get(key);
        }
        for (Map.Entry<K, List<V>> entry : m.entrySet()) {
            if (entry.getKey().toString().toLowerCase().equals(key.toString().toLowerCase())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return m.isEmpty();
    }

    public Set<K> keySet() {
        return m.keySet();
    }

    public List<V> put(K key, List<V> value) {
        return m.put(key, value);
    }

    public void putAll(Map<? extends K, ? extends List<V>> map) {
        m.putAll(map);
    }

    public List<V> remove(Object key) {
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
    
    
}
