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

package org.apache.cxf.workqueue;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.management.JMException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.management.InstrumentationManager;

public class AutomaticWorkQueueImpl extends ThreadPoolExecutor implements AutomaticWorkQueue {

    static final int DEFAULT_MAX_QUEUE_SIZE = 256;
    private static final Logger LOG =
        LogUtils.getL7dLogger(AutomaticWorkQueueImpl.class);
    
    int maxQueueSize;
    
    WorkQueueManagerImpl manager;
    String name = "default";

    public AutomaticWorkQueueImpl() {
        this(DEFAULT_MAX_QUEUE_SIZE);
    }    
    public AutomaticWorkQueueImpl(String name) {
        this(DEFAULT_MAX_QUEUE_SIZE, name);
    }    
    public AutomaticWorkQueueImpl(int max) {
        this(max, "default");
    }
    public AutomaticWorkQueueImpl(int max, String name) {
        this(max,
             0,
             25,
             5,
             2 * 60 * 1000L,
             name);
    }
    public AutomaticWorkQueueImpl(int mqs, 
                                  int initialThreads, 
                                  int highWaterMark, 
                                  int lowWaterMark,
                                  long dequeueTimeout) {
        this(mqs, initialThreads, highWaterMark, lowWaterMark, dequeueTimeout, "default");
    }    
    public AutomaticWorkQueueImpl(int mqs, 
                                  int initialThreads, 
                                  int highWaterMark, 
                                  int lowWaterMark,
                                  long dequeueTimeout,
                                  String name) {
        
        super(-1 == lowWaterMark ? Integer.MAX_VALUE : lowWaterMark, 
            -1 == highWaterMark ? Integer.MAX_VALUE : highWaterMark,
                TimeUnit.MILLISECONDS.toMillis(dequeueTimeout), TimeUnit.MILLISECONDS, 
                mqs == -1 ? new LinkedBlockingQueue<Runnable>(DEFAULT_MAX_QUEUE_SIZE)
                    : new LinkedBlockingQueue<Runnable>(mqs),
            createThreadFactory(name));
        
        maxQueueSize = mqs == -1 ? DEFAULT_MAX_QUEUE_SIZE : mqs;
        
        
        lowWaterMark = -1 == lowWaterMark ? Integer.MAX_VALUE : lowWaterMark;
        highWaterMark = -1 == highWaterMark ? Integer.MAX_VALUE : highWaterMark;
                
        StringBuilder buf = new StringBuilder();
        buf.append("Constructing automatic work queue with:\n");
        buf.append("max queue size: " + maxQueueSize + "\n");
        buf.append("initialThreads: " + initialThreads + "\n");
        buf.append("lowWaterMark: " + lowWaterMark + "\n");
        buf.append("highWaterMark: " + highWaterMark + "\n");
        LOG.fine(buf.toString());

        if (initialThreads > highWaterMark) {
            initialThreads = highWaterMark;
        }

        // as we cannot prestart more core than corePoolSize initial threads, we temporarily
        // change the corePoolSize to the number of initial threads
        // this is important as otherwise these threads will be created only when the queue has filled up, 
        // potentially causing problems with starting up under heavy load
        if (initialThreads < Integer.MAX_VALUE && initialThreads > 0) {
            setCorePoolSize(initialThreads);
            int started = prestartAllCoreThreads();
            if (started < initialThreads) {
                LOG.log(Level.WARNING, "THREAD_START_FAILURE_MSG", new Object[] {started, initialThreads});
            }
            setCorePoolSize(lowWaterMark);
        }
    }
    private static ThreadFactory createThreadFactory(final String name) {
        ThreadGroup group;
        try { 
            //Try and find the highest level ThreadGroup that we're allowed to use.
            //That SHOULD allow the default classloader and thread locals and such 
            //to be the least likely to cause issues down the road.
            group = AccessController.doPrivileged(
                new PrivilegedAction<ThreadGroup>() { 
                    public ThreadGroup run() { 
                        ThreadGroup group = Thread.currentThread().getThreadGroup(); 
                        ThreadGroup parent = group;
                        try { 
                            while (parent != null) { 
                                group = parent;  
                                parent = parent.getParent(); 
                            } 
                        } catch (SecurityException se) {
                            //ignore - if we get here, the "group" is as high as 
                            //the security manager will allow us to go.   Use that one.
                        }
                        return new ThreadGroup(group, name + "-workqueue");
                    } 
                }
            );
        } catch (SecurityException e) { 
            group = new ThreadGroup(name + "-workqueue");
        }
        group.setDaemon(true);
        return new AWQThreadFactory(group, name);
    }
    static class AWQThreadFactory implements ThreadFactory {
        final AtomicInteger threadNumber = new AtomicInteger(1);
        ThreadGroup group;
        String name;
        ClassLoader loader;
        AWQThreadFactory(ThreadGroup gp, String nm) {
            group = gp;
            name = nm;
            //force the loader to be the loader of CXF, not the application loader
            loader = AutomaticWorkQueueImpl.class.getClassLoader();
        }
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, 
                                  r, 
                                  name + "-workqueue-" + threadNumber.getAndIncrement(),
                                  0);
            t.setContextClassLoader(loader);
            if (!t.isDaemon()) {
                t.setDaemon(true);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
        public void setName(String s) {
            name = s;
        }
    }
    
    @Resource(name = "org.apache.cxf.workqueue.WorkQueueManager")
    public void setManager(WorkQueueManagerImpl mgr) {
        manager = mgr;
    }
    public WorkQueueManager getManager() {
        return manager;
    }

    public void setName(String s) {
        name = s;
        ThreadFactory factory = this.getThreadFactory();
        if (factory instanceof AWQThreadFactory) {
            ((AWQThreadFactory)factory).setName(s);
        }
    }
    public String getName() {
        return name;
    }
    
    @PostConstruct
    public void register() {
        if (manager != null) {
            manager.addNamedWorkQueue(name, this);
            InstrumentationManager imanager = manager.getBus().getExtension(InstrumentationManager.class);
            if (null != imanager) {
                try {
                    imanager.register(new WorkQueueImplMBeanWrapper(this));
                } catch (JMException jmex) {
                    LOG.log(Level.WARNING , jmex.getMessage(), jmex);
                }
            }
        }
    }
    
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(super.toString());
        buf.append(" [queue size: ");
        buf.append(getSize());
        buf.append(", max size: ");
        buf.append(maxQueueSize);
        buf.append(", threads: ");
        buf.append(getPoolSize());
        buf.append(", active threads: ");
        buf.append(getActiveCount());
        buf.append(", low water mark: ");
        buf.append(getLowWaterMark());
        buf.append(", high water mark: ");
        buf.append(getHighWaterMark());
        buf.append("]");
        return buf.toString();
    }
    
    public void execute(final Runnable command) {
        //Grab the context classloader of this thread.   We'll make sure we use that 
        //on the thread the runnable actually runs on.
        
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Runnable r = new Runnable() {
            public void run() {
                ClassLoader orig = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(loader);
                    command.run();
                } finally {
                    Thread.currentThread().setContextClassLoader(orig);
                }
            }
        };
        super.execute(r);
    }
    
    // WorkQueue interface
    public void execute(Runnable work, long timeout) {
        try {
            execute(work);
        } catch (RejectedExecutionException ree) {
            try {
                getQueue().offer(work, timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ie) {
                throw new RejectedExecutionException(ie);
            }
        }    
    }

    public void schedule(final Runnable work, final long delay) {
        // temporary implementation, replace with shared long-lived scheduler
        // task
        execute(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    // ignore
                }
                work.run();
            }
        });
    }
    
    // AutomaticWorkQueue interface
    
    public void shutdown(boolean processRemainingWorkItems) {
        if (!processRemainingWorkItems) {
            getQueue().clear();
        }
        shutdown();    
    }

    /**
     * Gets the maximum size (capacity) of the backing queue.
     * @return the maximum size (capacity) of the backing queue.
     */
    public long getMaxSize() {
        return maxQueueSize;
    }

    /**
     * Gets the current size of the backing queue.
     * @return the current size of the backing queue.
     */
    public long getSize() {
        return getQueue().size();
    }


    public boolean isEmpty() {
        return getQueue().size() == 0;
    }

    boolean isFull() {
        return getQueue().remainingCapacity() == 0;
    }

    public int getHighWaterMark() {
        int hwm = getMaximumPoolSize();
        return hwm == Integer.MAX_VALUE ? -1 : hwm;
    }

    public int getLowWaterMark() {
        int lwm = getCorePoolSize();
        return lwm == Integer.MAX_VALUE ? -1 : lwm;
    }

    public void setHighWaterMark(int hwm) {
        setMaximumPoolSize(hwm < 0 ? Integer.MAX_VALUE : hwm);
    }

    public void setLowWaterMark(int lwm) {
        setCorePoolSize(lwm < 0 ? 0 : lwm);
    }
}
