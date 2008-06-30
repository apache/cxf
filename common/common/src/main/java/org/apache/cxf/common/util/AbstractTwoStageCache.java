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

import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;


/**
 * This class pools objects, for efficiency accross a lightweight
 * fixed-size primary cache and a variable-size secondary cache - the
 * latter uses soft references to allow the polled object be GCed if
 * necessary.
 * <p>
 * To use the cache, a subclass is defined which provides an implementation
 * of the abstract get() method - this may be conveniently achieved via
 * an anonymous subclass. The cache is then populated by calling the
 * populate_cache() method - the reason a two-stage process is used is
 * to avoid problems with the inner class create() method accessing outer
 * class data members from the inner class ctor (before its reference to
 * the outer class is initialized).
 * <p>
 *
 */
public abstract class AbstractTwoStageCache<E> {
    private Object mutex;
    private int preallocation;
    private int primaryCacheSize;
    private int secondaryCacheHighWaterMark;
    
    private Queue<E> primaryCache;
    private Queue<SoftReference<E>> secondaryCache;

    /**
     * Constructor.
     *
     * @param pCacheSize primary cache size
     * @param secondary_cache_max secondary cache high water mark
     * @param preallocation the number of object to preallocation when the
     * cache is created
     */
    public AbstractTwoStageCache(int pCacheSize, int highWaterMark, int prealloc) {
        this(pCacheSize, highWaterMark, prealloc, null);
    }


    /**
     * Constructor.
     *
     * @param pCacheSize primary cache size
     * @param secondary_cache_max secondary cache high water mark
     * @param preallocation the number of object to preallocation when the
     * cache is created
     * @param mutex object to use as a monitor
     */
    public AbstractTwoStageCache(int pCacheSize, int highWaterMark, int prealloc, Object mutexParam) {
        this.primaryCacheSize = Math.min(pCacheSize, highWaterMark);
        this.secondaryCacheHighWaterMark = highWaterMark - pCacheSize;
        this.preallocation = prealloc > highWaterMark ? highWaterMark : prealloc;
        this.mutex = mutexParam != null ? mutexParam : this;
    }

    public String toString() {
        return "AbstractTwoStageCache";
    }


    /**
     * Over-ride this method to create objects to populate the pool
     *
     * @return newly created object
     */
    protected abstract E create() throws Exception;


    /**
     * Populate the cache
     */
    public void populateCache() throws Exception {
        // create cache
        primaryCache = new LinkedList<E>();
        secondaryCache = new LinkedList<SoftReference<E>>();

        // preallocate objects into primary cache
        int primaryCachePreallocation = 
            (preallocation > primaryCacheSize) ? primaryCacheSize : preallocation;
        for (int i = 0; i < primaryCachePreallocation; i++) {
            primaryCache.offer(create());
        }
        
        // preallocate objects into secondary cache
        int secondaryCachePreallocation = preallocation - primaryCachePreallocation;
        for (int i = 0; i < secondaryCachePreallocation; i++) {
            secondaryCache.offer(new SoftReference<E>(create()));
        }
    }


    /**
     * Return a cached or newly created object
     *
     * @return an object
     */
    public E get() throws Exception {
        E ret = poll();

        if (ret == null) {
            ret = create();
        }

        return ret;
    }


    /**
     * Return a cached object if one is available
     *
     * @return an object
     */
    public E poll() {
        E ret = null;

        synchronized (mutex) {
            if (primaryCache != null) {
                ret = primaryCache.poll();
                if (ret == null) {
                    SoftReference<E> sr = secondaryCache.poll();
                    while (ret == null && sr != null) {
                        if (sr != null) {
                            ret = sr.get();
                        }
                        if (ret == null) {
                            sr = secondaryCache.poll();
                        }
                    }
                }
            }
        }

        return ret;
    }


    /**
     * Recycle an old Object.
     *
     * @param oldObject the object to recycle
     * @return true iff the object can be accomodated in the cache
     */
    public boolean recycle(E oldObject) {
        boolean cached = false;

        synchronized (mutex) {
            if (primaryCache != null) {
                if (primaryCache.size() < primaryCacheSize) {
                    cached = primaryCache.offer(oldObject);
                }

                if (!cached && (secondaryCache.size() >= secondaryCacheHighWaterMark)) {
                    // check for nulls in secondary cache and remove them to create room
                    Iterator<SoftReference<E>> it = secondaryCache.iterator();
                    while (it.hasNext()) {
                        SoftReference<E> sr = it.next();
                        if (sr.get() == null) {
                            it.remove();
                        }
                    }
                }

                if (!cached && (secondaryCache.size() < secondaryCacheHighWaterMark)) {
                    cached = secondaryCache.offer(new SoftReference<E>(oldObject));
                }
            }
        }

        return cached;
    }
}
