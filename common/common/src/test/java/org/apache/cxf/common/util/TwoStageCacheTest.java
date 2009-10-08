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

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class TwoStageCacheTest extends Assert {


    @Test
    public void testToString() {
        TestTwoStageCache cache = new TestTwoStageCache(3, 5, 0);
        assertEquals("AbstractTwoStageCache", cache.toString());
    }
    
    /*
     * Test method for 'org.apache.cxf.common.util.AbstractTwoStageCache.get()'
     */
    @Test
    public void testGet() throws Throwable {
        TestTwoStageCache cache = new TestTwoStageCache(3, 5, 0);
        cache.populateCache();

        for (int x = 0; x < 10; x++) {
            assertNotNull(cache.get());
        }
        
        cache = new TestTwoStageCache(3, 5, 5);
        cache.populateCache();

        for (int x = 0; x < 10; x++) {
            assertNotNull(cache.get());
        }
    }

    /*
     * Test method for 'org.apache.cxf.common.util.AbstractTwoStageCache.poll()'
     */
    @Test
    public void testPoll() throws Throwable {
        TestTwoStageCache cache = new TestTwoStageCache(3, 5, 0);
        cache.populateCache();
        int count = 0;
        while (cache.poll() != null) {
            count++;
        }
        assertEquals(0, count);
        
        cache = new TestTwoStageCache(3, 5, 3);
        cache.populateCache();
        count = 0;
        while (cache.poll() != null) {
            count++;
        }
        assertEquals(3, count);

        cache = new TestTwoStageCache(3, 5, 5);
        cache.populateCache();
        count = 0;
        while (cache.poll() != null) {
            count++;
        }
        assertEquals(5, count);

        // try to prealloc more than high water mark...
        cache = new TestTwoStageCache(3, 5, 9);
        cache.populateCache();
        count = 0;
        while (cache.poll() != null) {
            count++;
        }
        assertEquals(5, count);    
        
        
    }

    /*
     * Test method for 'org.apache.cxf.common.util.AbstractTwoStageCache.recycle(E)'
     */
    @Test
    public void testRecycle() throws Throwable {
        TestTwoStageCache cache = new TestTwoStageCache(3, 8, 5, new Object());
        cache.populateCache();

        Object objs[] = new Object[10];
        
        for (int x = 0; x < 10; x++) {
            objs[x] = cache.get();
        }
        for (int x = 0; x < 10; x++) {
            cache.recycle(objs[x]);
        }
        int count = 0;
        while (cache.poll() != null) {
            count++;
        }
        assertEquals(8, count);    

        count = 0;
        for (int x = 0; x < 10; x++) {
            cache.recycle(objs[x]);
            objs[x] = null;
            System.gc();
        }
        objs = null;
        

        if (System.getProperty("java.vendor").contains("IBM")) {
            //The IBM VM will dump a core file and a heap dump 
            //at OOM which kind of pollutes the svn space
            return;
        }

        List<byte[]> list = new LinkedList<byte[]>();
        int allocCount = 0;
        try {
            while (allocCount++ < 1000) {
                System.gc();
                long memFree = Runtime.getRuntime().freeMemory();
                int memToAlloc = memFree > 512 * 1024 * 1024
                                    ? 512 * 1024 * 1024 : (int)memFree;
                list.add(new byte[memToAlloc]);
            }
            fail("cannot trigger OutOfMemoryError within a reasonable timeframe"); 
        } catch (OutOfMemoryError ex) {
            System.gc();
            list = null;
            System.gc();
        }
        cache.recycle(cache.create());
        cache.recycle(cache.create());
        cache.recycle(cache.create());
        
        System.gc();
        while (cache.poll() != null) {
            count++;
        }
        assertTrue("Did not get enough objects " + count, 3 <= count);    
    
    }

    
    static class TestTwoStageCache extends AbstractTwoStageCache<Object> {
        public TestTwoStageCache(int pCacheSize, int highWaterMark, int prealloc) {
            super(pCacheSize, highWaterMark, prealloc);
        }
        public TestTwoStageCache(int pCacheSize, int highWaterMark,
                                 int prealloc, Object mutex) {
            super(pCacheSize, highWaterMark, prealloc, mutex);
        }
        public Object create() {
            return new Object();
        }        
    }
}
