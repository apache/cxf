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
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientLifeCycleListener;
import org.apache.cxf.endpoint.ClientLifeCycleManager;
import org.apache.cxf.extension.BusExtension;

@NoJSR250Annotations
public class ClientLifeCycleManagerImpl implements ClientLifeCycleManager, BusExtension {

    private CopyOnWriteArrayList<ClientLifeCycleListener> listeners
        = new CopyOnWriteArrayList<>();

    public ClientLifeCycleManagerImpl() {

    }

    public ClientLifeCycleManagerImpl(Bus b) {
        Collection<? extends ClientLifeCycleListener> l = b.getExtension(ConfiguredBeanLocator.class)
                .getBeansOfType(ClientLifeCycleListener.class);
        if (l != null) {
            listeners.addAll(l);
        }
    }

    public Class<?> getRegistrationType() {
        return ClientLifeCycleManager.class;
    }

    public void registerListener(ClientLifeCycleListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void clientCreated(Client client) {
        for (ClientLifeCycleListener listener : listeners) {
            listener.clientCreated(client);
        }
    }

    public void clientDestroyed(Client client) {
        ListIterator<ClientLifeCycleListener> li = listeners.listIterator(listeners.size());
        while (li.hasPrevious()) {
            li.previous().clientDestroyed(client);
        }
    }

    public void unRegisterListener(ClientLifeCycleListener listener) {
        listeners.remove(listener);
    }

}
