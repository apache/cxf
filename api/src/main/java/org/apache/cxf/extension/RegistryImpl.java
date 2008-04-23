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

package org.apache.cxf.extension;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 */
public class RegistryImpl<K, T> implements Registry<K, T> {
    
    protected final Map<K, T> entries;
    
    protected RegistryImpl() {
        this(null);
    }
    
    protected RegistryImpl(Map<K, T> e) {
        if (null == e) {
            e = new ConcurrentHashMap<K, T>();
        } else if (!(e instanceof ConcurrentHashMap)) {
            e = new ConcurrentHashMap<K, T>(e);
        }
        entries = e;
    }
    

    public void register(K k, T t) {
        entries.put(k, t);
    }

    public void unregister(K k) {
        entries.remove(k);
    }

    public T get(K k) {
        return  entries.get(k);
    }

    
    
    
}
