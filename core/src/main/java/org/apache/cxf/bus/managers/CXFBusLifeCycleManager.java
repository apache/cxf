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

package org.apache.cxf.bus.managers;

import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.annotation.Resource;
import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.configuration.ConfiguredBeanLocator;

@NoJSR250Annotations(unlessNull = "bus")
public class CXFBusLifeCycleManager implements BusLifeCycleManager {

    private final CopyOnWriteArrayList<BusLifeCycleListener> listeners;
    private Bus bus;
    private boolean initCalled;
    private boolean preShutdownCalled;
    private boolean postShutdownCalled;

    public CXFBusLifeCycleManager() {
        listeners = new CopyOnWriteArrayList<>();
    }
    public CXFBusLifeCycleManager(Bus b) {
        listeners = new CopyOnWriteArrayList<>();
        setBus(b);
    }

    @Resource
    public final void setBus(Bus b) {
        bus = b;
        if (null != bus) {
            bus.setExtension(this, BusLifeCycleManager.class);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.cxf.buslifecycle.BusLifeCycleManager#registerLifeCycleListener(
     * org.apache.cxf.buslifecycle.BusLifeCycleListener)
     */
    public final void registerLifeCycleListener(BusLifeCycleListener listener) {
        listeners.addIfAbsent(listener);
        if (initCalled) {
            listener.initComplete();
        }

    }

    /* (non-Javadoc)
     * @see org.apache.cxf.buslifecycle.BusLifeCycleManager#unregisterLifeCycleListener(
     * org.apache.cxf.buslifecycle.BusLifeCycleListener)
     */
    public void unregisterLifeCycleListener(BusLifeCycleListener listener) {
        listeners.remove(listener);
    }


    public void initComplete() {
        if (bus != null) {
            bus.getExtension(ConfiguredBeanLocator.class)
                .getBeansOfType(BusLifeCycleListener.class);
        }
        preShutdownCalled = false;
        postShutdownCalled = false;
        initCalled = true;
        for (BusLifeCycleListener listener : listeners) {
            listener.initComplete();
        }
    }

    public void preShutdown() {
        if (!preShutdownCalled) {
            preShutdownCalled = true;
            ListIterator<BusLifeCycleListener> li = listeners.listIterator(listeners.size());
            while (li.hasPrevious()) {
                li.previous().preShutdown();
            }
        }
    }

    public void postShutdown() {
        if (!preShutdownCalled) {
            preShutdown();
        }
        if (!postShutdownCalled) {
            postShutdownCalled = true;
            ListIterator<BusLifeCycleListener> li = listeners.listIterator(listeners.size());
            while (li.hasPrevious()) {
                li.previous().postShutdown();
            }

        }
    }

}
