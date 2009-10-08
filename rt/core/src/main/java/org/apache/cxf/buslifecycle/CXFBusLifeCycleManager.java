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

package org.apache.cxf.buslifecycle;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.cxf.Bus;

public class CXFBusLifeCycleManager implements BusLifeCycleManager {

    private final List<BusLifeCycleListener> listeners;
    private Bus bus;
    private boolean preShutdownCalled;
    private boolean postShutdownCalled;
    
    public CXFBusLifeCycleManager() {
        listeners = new CopyOnWriteArrayList<BusLifeCycleListener>();
    }
    
    @Resource
    public void setBus(Bus b) {
        bus = b;
    }
    
    @PostConstruct
    public void register() {
        if (null != bus) {
            bus.setExtension(this, BusLifeCycleManager.class);
        }
    }
    
    /* (non-Javadoc)
     * @see org.apache.cxf.buslifecycle.BusLifeCycleManager#registerLifeCycleListener(
     * org.apache.cxf.buslifecycle.BusLifeCycleListener)
     */
    public void registerLifeCycleListener(BusLifeCycleListener listener) {
        listeners.add(listener);
        
    }

    /* (non-Javadoc)
     * @see org.apache.cxf.buslifecycle.BusLifeCycleManager#unregisterLifeCycleListener(
     * org.apache.cxf.buslifecycle.BusLifeCycleListener)
     */
    public void unregisterLifeCycleListener(BusLifeCycleListener listener) {
        listeners.remove(listener);      
    }
    
    public void initComplete() {
        preShutdownCalled = false;
        postShutdownCalled = false;
        for (BusLifeCycleListener listener : listeners) {
            listener.initComplete();
        }
    }
    
    public void preShutdown() {
        // TODO inverse order of registration?
        preShutdownCalled = true;
        for (BusLifeCycleListener listener : listeners) {
            listener.preShutdown();
        }
    }
    
    public void postShutdown() {
        if (!preShutdownCalled) {
            preShutdown();
        }
        if (!postShutdownCalled) {
            postShutdownCalled = true;
            // TODO inverse order of registration?
            for (BusLifeCycleListener listener : listeners) {
                listener.postShutdown();
            }
        }
    }
        
}
