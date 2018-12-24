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

import org.junit.Assert;
import org.junit.Test;

public class ProxyClassLoaderCacheTest extends Assert {
    
    private ProxyClassLoaderCache cache;
    
    @Test
    public void testClassLoaderIdentical() throws Exception {
        cache = new ProxyClassLoaderCache();
        ClassLoader cl1 = cache.getProxyClassLoader(
            this.getClass().getClassLoader(), 
            new Class<?>[]{Closeable.class, Client.class,  HelloWorld.class});
        ClassLoader cl2 = cache.getProxyClassLoader(
            this.getClass().getClassLoader(), 
            new Class<?>[]{Closeable.class, Client.class,  HelloWorld.class});
        assertTrue(cl1 == cl2);
    }
    
    @Test
    public void testClassLoaderIdenticalWithMultipleThreads() throws Exception {
        cache = new ProxyClassLoaderCache();
        Set<ClassLoader> clSet = Collections.synchronizedSet(new HashSet<>());
        CountDownLatch countDownLatch = new CountDownLatch(50);
        for (int i = 0; i < 50; i++) {
            new Thread(new HelloWorker(clSet, countDownLatch)).start();
        }
        countDownLatch.await(); 
        assertTrue(clSet.size() == 1);
    }
            
    interface HelloWorld {
        void sayHello();
    }
    
    class HelloWorker implements Runnable {

        private Set<ClassLoader> classLoaderSet;
        
        private CountDownLatch doneSignal;
        HelloWorker(Set<ClassLoader> classLoaderSet,
                           CountDownLatch doneSignal) {
            this.classLoaderSet = classLoaderSet;
            this.doneSignal = doneSignal;
        }

        public void run() {
            

            try {
                this.classLoaderSet.add(cache.getProxyClassLoader(
                                        this.getClass().getClassLoader(), 
                                        new Class<?>[]{Closeable.class, 
                                        Client.class,  
                                        HelloWorld.class}));
                doneSignal.countDown();
           
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                
            }

        }

    }
}
