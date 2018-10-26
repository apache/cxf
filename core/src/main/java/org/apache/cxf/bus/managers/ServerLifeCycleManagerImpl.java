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

import java.util.Collection;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerLifeCycleListener;
import org.apache.cxf.endpoint.ServerLifeCycleManager;
import org.apache.cxf.extension.BusExtension;

@NoJSR250Annotations
public class ServerLifeCycleManagerImpl implements ServerLifeCycleManager, BusExtension {

    private CopyOnWriteArrayList<ServerLifeCycleListener> listeners =
            new CopyOnWriteArrayList<>();

    public ServerLifeCycleManagerImpl() {

    }
    public ServerLifeCycleManagerImpl(Bus b) {
        Collection<? extends ServerLifeCycleListener> l = b.getExtension(ConfiguredBeanLocator.class)
                .getBeansOfType(ServerLifeCycleListener.class);
        if (l != null) {
            listeners.addAll(l);
        }
    }
    public Class<?> getRegistrationType() {
        return ServerLifeCycleManager.class;
    }


    public synchronized void registerListener(ServerLifeCycleListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void startServer(Server server) {
        for (ServerLifeCycleListener listener : listeners) {
            listener.startServer(server);
        }
    }

    public void stopServer(Server server) {
        ListIterator<ServerLifeCycleListener> li = listeners.listIterator(listeners.size());
        while (li.hasPrevious()) {
            li.previous().stopServer(server);
        }
    }

    public synchronized void unRegisterListener(ServerLifeCycleListener listener) {
        listeners.remove(listener);
    }
}
