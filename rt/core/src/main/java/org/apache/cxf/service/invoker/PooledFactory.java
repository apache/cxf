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

package org.apache.cxf.service.invoker;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.cxf.message.Exchange;

/**
 * Factory the maintains a pool of instances that are used.
 * 
 * Can optionally create more instances than the size of the queue
 */
public class PooledFactory implements Factory {
    BlockingQueue<Object> pool;
    Factory factory;
    int count;
    int max;
    boolean createMore;

    /**
     * Pool of instances of the svcClass
     * @param svcClass the class to create
     * @param max the absolute maximum number to create and pool
     */
    public PooledFactory(final Class svcClass, int max) {
        this(new PerRequestFactory(svcClass), max, false);
    }
    /**
     * Pool of instances contructed from the given factory
     * @param factory
     * @param max the absolute maximum number to create and pool
     */
    public PooledFactory(final Factory factory, int max) {
        this(factory, max, false);
    }

    /**
     * Pool of instances contructed from the given factory
     * @param factory
     * @param max the absolute maximum number to create and pool
     * @param createMore If the pool is empty, but max objects have already 
     * been constructed, should more be constructed on a per-request basis (and 
     * then discarded when done) or should requests block until instances are 
     * released back into the pool. 
     */
    public PooledFactory(final Factory factory, int max, boolean createMore) {
        this.factory = factory;
        if (max < 1) {
            max = 16;
        }
        pool = new ArrayBlockingQueue<Object>(max, true);
        this.max = max;
        this.count = 0;
        this.createMore = createMore;
    }
    
    /**
     * Pool constructed from the give Collection of objects. 
     * @param objs The collection of objects to pre-polulate the pool
     */
    public PooledFactory(Collection<Object> objs) {
        pool = new ArrayBlockingQueue<Object>(objs.size(), true);
        pool.addAll(objs);
    }

    /** {@inheritDoc}*/
    public Object create(Exchange ex) throws Throwable {
        if (factory == null 
            || ((count >= max) && !createMore)) {
            return pool.take();
        }
        Object o = pool.poll();
        if (o == null) {
            return createObject(ex);
        }
        return o;
    }
    protected synchronized Object createObject(Exchange e) throws Throwable {
        //recheck the count/max stuff now that we're in a sync block
        if (factory == null 
            || ((count >= max) && !createMore)) {
            return pool.take();
        }
        
        count++;
        return factory.create(e);        
    }

    /** {@inheritDoc}*/
    public void release(Exchange ex, Object o) {
        pool.offer(o);
    }

}
