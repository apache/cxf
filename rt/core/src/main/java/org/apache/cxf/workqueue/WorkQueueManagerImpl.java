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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.management.JMException;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.management.InstrumentationManager;

@NoJSR250Annotations(unlessNull = "bus")
public class WorkQueueManagerImpl implements WorkQueueManager {

    private static final Logger LOG =
        LogUtils.getL7dLogger(WorkQueueManagerImpl.class);

    Map<String, AutomaticWorkQueue> namedQueues 
        = new ConcurrentHashMap<String, AutomaticWorkQueue>();
    boolean inShutdown;
    Bus bus;  
    
    public WorkQueueManagerImpl() {
        
    }
    public WorkQueueManagerImpl(Bus b) {
        setBus(b);
    }
    
    public Bus getBus() {
        return bus;
    }
    
    @Resource
    public final void setBus(Bus bus) {        
        this.bus = bus;
        if (null != bus) {
            bus.setExtension(this, WorkQueueManager.class);
            InstrumentationManager manager = bus.getExtension(InstrumentationManager.class);
            if (null != manager) {
                try {
                    manager.register(new WorkQueueManagerImplMBeanWrapper(this));
                } catch (JMException jmex) {
                    LOG.log(Level.WARNING , jmex.getMessage(), jmex);
                }
            }
        }
    }

    public synchronized AutomaticWorkQueue getAutomaticWorkQueue() {
        AutomaticWorkQueue defaultQueue = getNamedWorkQueue("default");
        if (defaultQueue == null) {
            defaultQueue = createAutomaticWorkQueue();
        }
        return defaultQueue;
    }

    public synchronized void shutdown(boolean processRemainingTasks) {
        inShutdown = true;
        for (AutomaticWorkQueue q : namedQueues.values()) {
            q.shutdown(processRemainingTasks);
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
            for (AutomaticWorkQueue q : namedQueues.values()) {
                while (!q.isShutdown()) {
                    try {            
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        // ignore
                    }
                }
            }
        }
        for (java.util.logging.Handler h : LOG.getHandlers())  {
            h.flush();
        }
        
    }

    public AutomaticWorkQueue getNamedWorkQueue(String name) {
        return namedQueues.get(name);
    }
    public void addNamedWorkQueue(String name, AutomaticWorkQueue q) {
        namedQueues.put(name, q);
    }
    
    private AutomaticWorkQueue createAutomaticWorkQueue() {        
        AutomaticWorkQueueImpl impl = new AutomaticWorkQueueImpl("default");
        impl.setManager(this);
        impl.register();
        return impl;       
    }

}
