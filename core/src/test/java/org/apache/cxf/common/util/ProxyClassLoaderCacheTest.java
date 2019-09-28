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



import java.io.Closeable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.cxf.endpoint.Client;

import org.junit.Test;

import static org.junit.Assert.assertSame;

public class ProxyClassLoaderCacheTest {

    private final ProxyClassLoaderCache cache = new ProxyClassLoaderCache();

    @Test
    public void testClassLoaderIdentical() throws Exception {
        ClassLoader cl1 = cache.getProxyClassLoader(
            this.getClass().getClassLoader(), 
            new Class<?>[]{Closeable.class, Client.class,  HelloWorld.class});
        ClassLoader cl2 = cache.getProxyClassLoader(
            this.getClass().getClassLoader(), 
            new Class<?>[]{Closeable.class, Client.class,  HelloWorld.class});
        assertSame(cl1, cl2);
    }

    @Test
    public void testClassLoaderIdenticalWithMultipleThreads() throws Exception {
        final Set<ClassLoader> clSet = Collections.synchronizedSet(new HashSet<>());
        final CountDownLatch countDownLatch = new CountDownLatch(50);
        for (int i = 0; i < 50; i++) {
            new Thread(() -> {
                try {
                    clSet.add(cache.getProxyClassLoader(
                                            this.getClass().getClassLoader(), 
                                            new Class<?>[]{Closeable.class, 
                                            Client.class,  
                                            HelloWorld.class}));
                } finally {
                    countDownLatch.countDown();
                }
            }).start();
        }
        countDownLatch.await(); 
        assertSame(1, clSet.size());
    }

    interface HelloWorld {
    }

}
