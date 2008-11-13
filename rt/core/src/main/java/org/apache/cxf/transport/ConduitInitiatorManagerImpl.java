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

package org.apache.cxf.transport;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.bus.extension.DeferredMap;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.configuration.spring.MapProvider;

public final class ConduitInitiatorManagerImpl implements ConduitInitiatorManager {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(ConduitInitiatorManager.class);

    Map<String, ConduitInitiator> conduitInitiators;
    
    private Bus bus;
    public ConduitInitiatorManagerImpl() {
        conduitInitiators = new ConcurrentHashMap<String, ConduitInitiator>();
    }
    

    public ConduitInitiatorManagerImpl(MapProvider<String, ConduitInitiator> conduitInitiators) {
        this.conduitInitiators = conduitInitiators.createMap();
    }

    public ConduitInitiatorManagerImpl(Map<String, ConduitInitiator> conduitInitiators) {
        this.conduitInitiators = conduitInitiators;
    }
    
    /**
     * Spring is slow to resolve constructors. This accessor allows
     * for initialization via a property.
     * @param mapProvider
     */
    public void setMapProvider(MapProvider<String, ConduitInitiator> mapProvider) {
        this.conduitInitiators = mapProvider.createMap();
    }
    
    @Resource
    public void setBus(Bus b) {
        bus = b;
    }
    
    @PostConstruct
    public void register() {
        if (null != bus) {
            bus.setExtension(this, ConduitInitiatorManager.class);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.bus.ConduitInitiatorManager#registerConduitInitiator(java.lang.String,
     *      org.apache.cxf.transports.ConduitInitiator)
     */
    public void registerConduitInitiator(String namespace, ConduitInitiator factory) {
        conduitInitiators.put(namespace, factory);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.bus.ConduitInitiatorManager#deregisterConduitInitiator(java.lang.String)
     */
    public void deregisterConduitInitiator(String namespace) {
        conduitInitiators.remove(namespace);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.bus.ConduitInitiatorManager#ConduitInitiator(java.lang.String)
     */
    /**
     * Returns the conduit initiator for the given namespace, constructing it
     * (and storing in the cache for future reference) if necessary, using its
     * list of factory classname to namespace mappings.
     * 
     * @param namespace the namespace.
     */
    public ConduitInitiator getConduitInitiator(String namespace) throws BusException {
        ConduitInitiator factory = conduitInitiators.get(namespace);
        if (null == factory) {
            throw new BusException(new Message("NO_CONDUIT_INITIATOR", BUNDLE, namespace));
        }
        return factory;
    }

    @PreDestroy
    public void shutdown() {
        // nothing to do
    }

    public ConduitInitiator getConduitInitiatorForUri(String uri) {
        for (ConduitInitiator ci : conduitInitiators.values()) {
            for (String prefix : ci.getUriPrefixes()) {
                if (uri.startsWith(prefix)) {
                    return ci;
                }
            }
        }
        //looks like we'll need to undefer everything so we can try again.
        if (conduitInitiators instanceof DeferredMap) {
            ((DeferredMap)conduitInitiators).undefer();
            for (ConduitInitiator df : conduitInitiators.values()) {
                for (String prefix : df.getUriPrefixes()) {
                    if (uri.startsWith(prefix)) {
                        return df;
                    }
                }
            }
        }
        return null;
    }
    
}
