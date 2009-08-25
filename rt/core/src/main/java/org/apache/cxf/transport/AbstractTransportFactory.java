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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.helpers.CastUtils;

/**
 * Helper methods for {@link DestinationFactory}s and {@link ConduitInitiator}s.
 */
public abstract class AbstractTransportFactory {
    protected Bus bus;
    private List<String> transportIds;
    
    public AbstractTransportFactory() {
    }
    public AbstractTransportFactory(List<String> ids, Bus b) {
        transportIds = ids;
        bus = b;
        register();
    }
    
    public Bus getBus() {
        return bus;
    }
    public void setBus(Bus b) {
        unregister();
        bus = b;
        register();
    }

    public final List<String> getTransportIds() {
        return transportIds;
    }

    public void setTransportIds(List<String> transportIds) {
        unregister();
        this.transportIds = transportIds;
        register();
    }

    public Set<String> getUriPrefixes() {
        return CastUtils.cast(Collections.EMPTY_SET);
    }
    
    public final void register() {
        if (null == bus) {
            return;
        }
        if (this instanceof DestinationFactory) {
            DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
            if (null != dfm && getTransportIds() != null) {
                for (String ns : getTransportIds()) {
                    dfm.registerDestinationFactory(ns, (DestinationFactory)this);
                }
            }
        }
        if (this instanceof ConduitInitiator) {
            ConduitInitiatorManager cim = bus.getExtension(ConduitInitiatorManager.class);
            if (cim != null && getTransportIds() != null) {
                for (String ns : getTransportIds()) {
                    cim.registerConduitInitiator(ns, (ConduitInitiator)this);
                }
            }
        }
    }
    public final void unregister() {
        if (null == bus) {
            return;
        }
        if (this instanceof DestinationFactory) {
            DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
            if (null != dfm && getTransportIds() != null) {
                for (String ns : getTransportIds()) {
                    try {
                        if (dfm.getDestinationFactory(ns) == this) {
                            dfm.deregisterDestinationFactory(ns);
                        }
                    } catch (BusException e) {
                        //ignore
                    }
                }
            }
        }
        if (this instanceof ConduitInitiator) {
            ConduitInitiatorManager cim = bus.getExtension(ConduitInitiatorManager.class);
            if (cim != null && getTransportIds() != null) {
                for (String ns : getTransportIds()) {
                    try {
                        if (cim.getConduitInitiator(ns) == this) {
                            cim.deregisterConduitInitiator(ns);
                        }
                    } catch (BusException e) {
                        //ignore
                    }
                }
            }
        }
        
    }
    
}
