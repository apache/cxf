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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.management.JMException;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.management.InstrumentationManager;

public class WorkQueueManagerImpl implements WorkQueueManager {

    private static final Logger LOG =
        LogUtils.getL7dLogger(WorkQueueManagerImpl.class);

    ThreadingModel threadingModel = ThreadingModel.MULTI_THREADED;
    AutomaticWorkQueue autoQueue;
    boolean inShutdown;
    Bus bus;  
    
    public Bus getBus() {
        return bus;
    }
    
    @Resource
    public void setBus(Bus bus) {        
        this.bus = bus;
    }
    
    @PostConstruct
    public void register() {
        if (null != bus) {
            bus.setExtension(this, WorkQueueManager.class);
        }
    }

    public synchronized AutomaticWorkQueue getAutomaticWorkQueue() {
        if (autoQueue == null) {
            autoQueue = createAutomaticWorkQueue();
            InstrumentationManager manager = bus.getExtension(InstrumentationManager.class);
            if (null != manager) {
                try {
                    manager.register(new WorkQueueManagerImplMBeanWrapper(this));
                } catch (JMException jmex) {
                    LOG.log(Level.WARNING , jmex.getMessage(), jmex);
                }
            }
        }
        
        return autoQueue;
    }

    public ThreadingModel getThreadingModel() {
        return threadingModel;
    }

    public void setThreadingModel(ThreadingModel model) {
        threadingModel = model;
    }

    public synchronized void shutdown(boolean processRemainingTasks) {
        inShutdown = true;
        if (autoQueue != null) {
            autoQueue.shutdown(processRemainingTasks);
        }

        synchronized (this) {
            notifyAll();
        }
    }

    public void run() {
        synchronized (this) {
            while (!inShutdown) {
                try {            
                    wait();
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
            while (autoQueue != null && !autoQueue.isShutdown()) {
                try {            
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
        }
        for (java.util.logging.Handler h : LOG.getHandlers())  {
            h.flush();
        }
        
    }

    private AutomaticWorkQueue createAutomaticWorkQueue() {        
      
        // Configuration configuration = bus.getConfiguration();

        // configuration.getInteger("threadpool:initial_threads");
        int initialThreads = 1;

        // int lwm = configuration.getInteger("threadpool:low_water_mark");
        int lwm = 5;

        // int hwm = configuration.getInteger("threadpool:high_water_mark");
        int hwm = 25;

        // configuration.getInteger("threadpool:max_queue_size");
        int maxQueueSize = 10 * hwm;

        // configuration.getInteger("threadpool:dequeue_timeout");
        long dequeueTimeout = 2 * 60 * 1000L;

        return new AutomaticWorkQueueImpl(maxQueueSize, initialThreads, hwm, lwm, dequeueTimeout);
               
    }
    
}
