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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ReflectionUtil;

@NoJSR250Annotations
public class AutomaticWorkQueueImpl implements AutomaticWorkQueue {
    public static final String PROPERTY_NAME = "name";
    static final int DEFAULT_MAX_QUEUE_SIZE = 256;
    private static final Logger LOG =
        LogUtils.getL7dLogger(AutomaticWorkQueueImpl.class);
    

    String name = "default";
    int maxQueueSize;
    int initialThreads;
    int lowWaterMark;
    int highWaterMark; 
    long dequeueTimeout;
    volatile int approxThreadCount;

    ThreadPoolExecutor executor;
    Method addWorkerMethod;
    Object addWorkerArgs[];
    
    AWQThreadFactory threadFactory;
    ReentrantLock mainLock;
    
    
    DelayQueue<DelayedTaskWrapper> delayQueue;
    WatchDog watchDog;
    
    boolean shared;
    int sharedCount;
    
    private List<PropertyChangeListener> changeListenerList;
    
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
        this.maxQueueSize = mqs == -1 ? DEFAULT_MAX_QUEUE_SIZE : mqs;
        this.initialThreads = initialThreads;
        this.highWaterMark = -1 == highWaterMark ? Integer.MAX_VALUE : highWaterMark;
        this.lowWaterMark = -1 == lowWaterMark ? Integer.MAX_VALUE : lowWaterMark;
        this.dequeueTimeout = dequeueTimeout;
        this.name = name;
        this.changeListenerList = new ArrayList<PropertyChangeListener>();
    }
    
    public void addChangeListener(PropertyChangeListener listener) {
        this.changeListenerList.add(listener);
    }
    
    public void removeChangeListener(PropertyChangeListener listener) {
        this.changeListenerList.remove(listener);
    }
    
    public void notifyChangeListeners(PropertyChangeEvent event) {
        for (PropertyChangeListener listener : changeListenerList) {
            listener.propertyChange(event);
        }
    }
    
    public void setShared(boolean shared) {
        this.shared = shared;
    }
    public boolean isShared() {
        return shared;
    }
    public void addSharedUser() {
        sharedCount++;
    }
    public void removeSharedUser() {
        sharedCount--;
    }
    public int getShareCount() {
        return sharedCount;
    }
    
    protected synchronized ThreadPoolExecutor getExecutor() {
        if (executor == null) {
            threadFactory = createThreadFactory(name);
            executor = new ThreadPoolExecutor(lowWaterMark, 
                                              highWaterMark,
                                              TimeUnit.MILLISECONDS.toMillis(dequeueTimeout), 
                                              TimeUnit.MILLISECONDS, 
                                              new LinkedBlockingQueue<Runnable>(maxQueueSize),
                                              threadFactory) {
                @Override
                protected void terminated() {
                    ThreadFactory f = executor.getThreadFactory();
                    if (f instanceof AWQThreadFactory) {
                        ((AWQThreadFactory)f).shutdown();
                    }
                    if (watchDog != null) {
                        watchDog.shutdown();
                    }
                }
            };
            
                    
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
            // this is important as otherwise these threads will be created only when 
            // the queue has filled up, 
            // potentially causing problems with starting up under heavy load
            if (initialThreads < Integer.MAX_VALUE && initialThreads > 0) {
                executor.setCorePoolSize(initialThreads);
                int started = executor.prestartAllCoreThreads();
                if (started < initialThreads) {
                    LOG.log(Level.WARNING, "THREAD_START_FAILURE_MSG",
                            new Object[] {started, initialThreads});
                }
                executor.setCorePoolSize(lowWaterMark);
            }
    
            ReentrantLock l = null;
            try {
                Field f = ThreadPoolExecutor.class.getDeclaredField("mainLock");
                ReflectionUtil.setAccessible(f);
                l = (ReentrantLock)f.get(executor);
            } catch (Throwable t) {
                l = new ReentrantLock();
            }
            mainLock = l;

            try {
                //java 5/6
                addWorkerMethod = ThreadPoolExecutor.class.getDeclaredMethod("addIfUnderMaximumPoolSize",
                                                                             Runnable.class);
                addWorkerArgs = new Object[] {null};
            } catch (Throwable t) {
                try {
                    //java 7
                    addWorkerMethod = ThreadPoolExecutor.class.getDeclaredMethod("addWorker",
                                                                                 Runnable.class, Boolean.TYPE);
                    addWorkerArgs = new Object[] {null, Boolean.FALSE};
                } catch (Throwable t2) {
                    //nothing we cando
                }
            }

        }
        return executor;
    }
    private AWQThreadFactory createThreadFactory(final String nm) {
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
                        return new ThreadGroup(group, nm + "-workqueue");
                    } 
                }
            );
        } catch (SecurityException e) { 
            group = new ThreadGroup(nm + "-workqueue");
        }
        return new AWQThreadFactory(group, nm);
    }
    
    static class DelayedTaskWrapper implements Delayed, Runnable {
        long trigger;
        Runnable work;
        
        DelayedTaskWrapper(Runnable work, long delay) {
            this.work = work;
            trigger = System.currentTimeMillis() + delay;
        }
        
        public long getDelay(TimeUnit unit) {
            long n = trigger - System.currentTimeMillis();
            return unit.convert(n, TimeUnit.MILLISECONDS);
        }

        public int compareTo(Delayed delayed) {
            long other = ((DelayedTaskWrapper)delayed).trigger;
            int returnValue;
            if (this.trigger < other) {
                returnValue = -1;
            } else if (this.trigger > other) {
                returnValue = 1;
            } else {
                returnValue = 0;
            }
            return returnValue;
        }

        public void run() {
            work.run();
        }
        
    }
    
    class WatchDog extends Thread {
        DelayQueue<DelayedTaskWrapper> delayQueue;
        AtomicBoolean shutdown = new AtomicBoolean(false);
        
        WatchDog(DelayQueue<DelayedTaskWrapper> queue) {
            delayQueue = queue;
        }
        
        public void shutdown() {
            shutdown.set(true);
            // to exit the waiting thread
            interrupt();
        }
        
        public void run() {
            DelayedTaskWrapper task;
            try {
                while (!shutdown.get()) {
                    task = delayQueue.take();
                    if (task != null) {
                        try {
                            execute(task);
                        } catch (Exception ex) {
                            LOG.warning("Executing the task from DelayQueue with exception: " + ex);
                        }
                    }
                }
            } catch (InterruptedException e) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.finer("The DelayQueue watchdog Task is stopping");
                }
            }

        }
        
    }
    class AWQThreadFactory implements ThreadFactory {
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
        
        public Thread newThread(final Runnable r) {
            if (group.isDestroyed()) {
                group = new ThreadGroup(group.getParent(), name + "-workqueue");
            }
            Runnable wrapped = new Runnable() {
                public void run() {
                    ++approxThreadCount;
                    try {
                        r.run();
                    } finally {
                        --approxThreadCount;
                    }
                }
            };
            final Thread t = new Thread(group, 
                                  wrapped, 
                                  name + "-workqueue-" + threadNumber.getAndIncrement(),
                                  0);
            AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                public Boolean run() {
                    t.setContextClassLoader(loader);
                    return true;
                }
            });
            t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
        public void setName(String s) {
            name = s;
        }
        public void shutdown() {
            if (!group.isDestroyed()) {
                try {
                    group.destroy();
                    group.setDaemon(true);
                } catch (Throwable t) {
                    //ignore
                }
            }            
        }
    }
    
    public void setName(String s) {
        name = s;
        if (threadFactory != null) {
            threadFactory.setName(s);
        }
    }
    public String getName() {
        return name;
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
                ClassLoaderHolder orig = ClassLoaderUtils.setThreadContextClassloader(loader);
                try {
                    command.run();
                } finally {
                    if (orig != null) {
                        orig.reset();
                    }
                }
            }
        };
        //The ThreadPoolExecutor in the JDK doesn't expand the number
        //of threads until the queue is full.   However, we would 
        //prefer the number of threads to expand immediately and 
        //only uses the queue if we've reached the maximum number 
        //of threads.
        ThreadPoolExecutor ex = getExecutor();
        ex.execute(r);
        if (addWorkerMethod != null 
            && !ex.getQueue().isEmpty() 
            && this.approxThreadCount < highWaterMark) {
            mainLock.lock();
            try {
                int ps = this.getPoolSize();
                int sz = executor.getQueue().size();
                int sz2 = this.getActiveCount();
                
                if ((sz + sz2) > ps) {
                    ReflectionUtil.setAccessible(addWorkerMethod).invoke(executor, addWorkerArgs);
                }
            } catch (Exception exc) {
                //ignore
            } finally {
                mainLock.unlock();
            }
        }
    }
    
    // WorkQueue interface
    public void execute(Runnable work, long timeout) {
        try {
            execute(work);
        } catch (RejectedExecutionException ree) {
            try {
                if (!getExecutor().getQueue().offer(work, timeout, TimeUnit.MILLISECONDS)) {
                    throw ree;
                }
            } catch (InterruptedException ie) {
                throw ree;
            }
        }    
    }

    public synchronized void schedule(final Runnable work, final long delay) {
        if (delayQueue == null) {
            delayQueue = new DelayQueue<DelayedTaskWrapper>();
            watchDog = new WatchDog(delayQueue);
            watchDog.setDaemon(true);
            watchDog.start();
        }
        delayQueue.put(new DelayedTaskWrapper(work, delay));
    }
    
    // AutomaticWorkQueue interface
    
    public void shutdown(boolean processRemainingWorkItems) {
        if (executor != null) {
            if (!processRemainingWorkItems) {
                executor.getQueue().clear();
            }
            executor.shutdown();
        }
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
        return executor == null ? 0 : executor.getQueue().size();
    }


    public boolean isEmpty() {
        return executor == null ? true : executor.getQueue().size() == 0;
    }

    boolean isFull() {
        return executor == null ? false : executor.getQueue().remainingCapacity() == 0;
    }

    public int getHighWaterMark() {
        int hwm = executor == null ? highWaterMark : executor.getMaximumPoolSize();
        return hwm == Integer.MAX_VALUE ? -1 : hwm;
    }

    public int getLowWaterMark() {
        int lwm = executor == null ? lowWaterMark : executor.getCorePoolSize();
        return lwm == Integer.MAX_VALUE ? -1 : lwm;
    }

    public void setHighWaterMark(int hwm) {
        highWaterMark = hwm < 0 ? Integer.MAX_VALUE : hwm;
        if (executor != null) {
            notifyChangeListeners(new PropertyChangeEvent(this, "highWaterMark", 
                                                          this.executor.getMaximumPoolSize(), hwm));
            executor.setMaximumPoolSize(highWaterMark);
        }
    }

    public void setLowWaterMark(int lwm) {
        lowWaterMark = lwm < 0 ? 0 : lwm;
        if (executor != null) {
            notifyChangeListeners(new PropertyChangeEvent(this, "lowWaterMark",
                                                          this.executor.getCorePoolSize(), lwm)); 
            executor.setCorePoolSize(lowWaterMark);
        }
    }

    public void setInitialSize(int initialSize) {
        notifyChangeListeners(new PropertyChangeEvent(this, "initialSize", this.initialThreads, initialSize));
        this.initialThreads = initialSize;
    }
    
    public void setQueueSize(int size) {
        notifyChangeListeners(new PropertyChangeEvent(this, "queueSize", this.maxQueueSize, size));
        this.maxQueueSize = size;
    }
    
    public void setDequeueTimeout(long l) {
        notifyChangeListeners(new PropertyChangeEvent(this, "dequeueTimeout", this.dequeueTimeout, l));
        this.dequeueTimeout = l;
    }
    
    public boolean isShutdown() {
        if (executor == null) {
            return false;
        }
        return executor.isShutdown();
    }
    public int getLargestPoolSize() {
        if (executor == null) {
            return 0;
        }
        return executor.getLargestPoolSize();
    }
    public int getPoolSize() {
        if (executor == null) {
            return 0;
        }
        return executor.getPoolSize();
    }
    public int getActiveCount() {
        if (executor == null) {
            return 0;
        }
        return executor.getActiveCount();
    }
    public void update(Dictionary config) {
        String s = (String)config.get("highWaterMark");
        if (s != null) {
            this.highWaterMark = Integer.parseInt(s);
        }
        s = (String)config.get("lowWaterMark");
        if (s != null) {
            this.lowWaterMark = Integer.parseInt(s);
        }
        s = (String)config.get("initialSize");
        if (s != null) {
            this.initialThreads = Integer.parseInt(s);
        }
        s = (String)config.get("dequeueTimeout");
        if (s != null) {
            this.dequeueTimeout = Long.parseLong(s);
        }
        s = (String)config.get("queueSize");
        if (s != null) {
            this.maxQueueSize = Integer.parseInt(s);
        } 
    }
    public Dictionary getProperties() {
        Dictionary<String, String> properties = new Hashtable<String, String>();
        NumberFormat nf = NumberFormat.getIntegerInstance();
        properties.put("name", nf.format(getName()));
        properties.put("highWaterMark", nf.format(getHighWaterMark()));
        properties.put("lowWaterMark", nf.format(getLowWaterMark()));
        properties.put("initialSize", nf.format(getLowWaterMark()));
        properties.put("dequeueTimeout", nf.format(getLowWaterMark()));
        properties.put("queueSize", nf.format(getLowWaterMark()));
        return properties;
    }
}
