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

package org.apache.cxf.message;

import java.util.Map;

public interface StringMap extends Map<String, Object> {
    
    /**
     * Convenience method for storing/retrieving typed objects from the map.
     * equivalent to:  (T)get(key.getName());
     * @param key the key
     * @return the value
     */
    <T> T get(Class<T> key);
    
    /**
     * Convenience method for storing/retrieving typed objects from the map.
     * equivalent to:  put(key.getName(), value);
     * @param key the key
     * @param value the value
     */
    <T> void put(Class<T> key, T value);
}
