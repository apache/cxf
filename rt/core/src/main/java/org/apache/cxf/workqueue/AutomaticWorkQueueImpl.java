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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
    public AutomaticWorkQueueImpl(int max) {
        this(max,
             0,
             25,
             5,
             2 * 60 * 1000L);
    }
    
    public AutomaticWorkQueueImpl(int mqs, 
                                  int initialThreads, 
                                  int highWaterMark, 
                                  int lowWaterMark,
                                  long dequeueTimeout) {
        
        super(-1 == lowWaterMark ? Integer.MAX_VALUE : lowWaterMark, 
            -1 == highWaterMark ? Integer.MAX_VALUE : highWaterMark,
                TimeUnit.MILLISECONDS.toMillis(dequeueTimeout), TimeUnit.MILLISECONDS, 
                mqs == -1 ? new LinkedBlockingQueue<Runnable>(DEFAULT_MAX_QUEUE_SIZE)
                    : new LinkedBlockingQueue<Runnable>(mqs));
        
        maxQueueSize = mqs == -1 ? DEFAULT_MAX_QUEUE_SIZE : mqs;
        
        
        lowWaterMark = -1 == lowWaterMark ? Integer.MAX_VALUE : lowWaterMark;
        highWaterMark = -1 == highWaterMark ? Integer.MAX_VALUE : highWaterMark;
                
        StringBuffer buf = new StringBuffer();
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
    @Resource(name = "org.apache.cxf.workqueue.WorkQueueManager")
    public void setManager(WorkQueueManagerImpl mgr) {
        manager = mgr;
    }
    public WorkQueueManager getManager() {
        return manager;
    }

    public void setName(String s) {
        name = s;
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
        StringBuffer buf = new StringBuffer();
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
