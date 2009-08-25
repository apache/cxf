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

package org.apache.cxf.endpoint;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;

/**
 * A simple contract resolver registry. It maintains a list of contract resolvers in an
 * <code>ArrayList</code>.
 */
@NoJSR250Annotations(unlessNull = "bus")
public class ServiceContractResolverRegistryImpl implements ServiceContractResolverRegistry {

    private Bus bus;
    private List<ServiceContractResolver> resolvers 
        = new CopyOnWriteArrayList<ServiceContractResolver>();

    public ServiceContractResolverRegistryImpl() {
        
    }
    public ServiceContractResolverRegistryImpl(Bus b) {
        setBus(b);
    }
    

    /**
     * Sets the bus with which the registry is associated.
     *
     * @param bus
     */
    public final void setBus(Bus b) {
        this.bus = b;
        if (bus != null) {
            bus.setExtension(this, ServiceContractResolverRegistry.class);
        }
    }

    /**
     * Calls each of the registered <code>ServiceContractResolver</code> instances
     * to resolve the location of the service's contract. It returns the location 
     * from the first resolver that matches the QName to a location.
     *
     * @param qname QName to be resolved into a contract location
     * @return URI representing the location of the contract
    */
    public URI getContractLocation(QName qname) {
        for (ServiceContractResolver resolver : resolvers) {
            URI contractLocation = resolver.getContractLocation(qname);
            if (null != contractLocation) {
                return contractLocation;
            }
        }
        return null;
    }

    /**
     * Tests if a resolver is alreadey registered with this registry.
     *
     * @param resolver the contract resolver for which to searche
     * @return <code>true</code> if the resolver is registered
     */
    public boolean isRegistered(ServiceContractResolver resolver) {
        return resolvers.contains(resolver);
    }

    /**
     * Registers a contract resolver with this registry.
     *
     * @param resolver the contract resolver to register
     */
    public synchronized void register(ServiceContractResolver resolver) {
        resolvers.add(resolver);        
    }

    /**
     * Removes a contract resolver from this registry.
     *
     * @param resolver the contract resolver to remove
     */
    public synchronized void unregister(ServiceContractResolver resolver) {
        resolvers.remove(resolver);        
    }

    
    protected List<ServiceContractResolver> getResolvers() {
        return resolvers;
    }

}
