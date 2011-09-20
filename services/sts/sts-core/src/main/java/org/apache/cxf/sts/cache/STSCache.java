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

public interface STSCache {

    /**
     * Method used to retrieve cached objects 
     * @param key the key
     * @return the cached object
     */
    Object get(Object key);
    
    /**
     * Method used to store objects in the cache
     * @param key the key
     * @param value the value
     */
    void put(Object key, Object value);
    
    /**
     * Method used to store objects in the cache
     * @param key the key
     * @param value the value
     * @param timeToLiveSeconds timeToLive for the object
     */
    void put(Object key, Object value, Integer timeToLiveSeconds);
    
    /**
     * Method used to remove the cached object
     * @param key the key
     * @return result
     */
    boolean remove(Object key);
    
    /**
     * Method used to remove all the objects from the cache.
     */
    void removeAll();
    
    /**
     * Method used to get the current size of the cache
     * @return size
     */
    int size();
      
}
