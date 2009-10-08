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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * This implementation class is responsible for mediating
 * access to registered EndpointResolvers, which themselves map
 * between abstract and concrete endpoint references, and/or
 * facilitate renewal of stale references.
 * <p>
 * An underlying mechanism in the style of the OGSA WS-Naming
 * specification is assumed, where an EPR maybe be fully abstract,
 * or concrete but with sufficient information embedded to enable
 * its renewal if necessary.
 */
public class EndpointResolverRegistryImpl implements EndpointResolverRegistry {

    private Bus bus;
    private List<EndpointResolver> resolvers;
    
    /**
     * Initialize registry, and expose as Bus extension.
     */
    @PostConstruct
    public void init() {
        resolvers = new ArrayList<EndpointResolver>();
        if (bus != null) {
            bus.setExtension(this, EndpointResolverRegistry.class);
        }
    }
    
    /**
     * Register an endpoint resolver.
     *
     * @param resolver the EndpointResolver to add to the chain.
     */
    public synchronized void register(EndpointResolver resolver) {
        resolvers.add(resolver);
    }
    
    /**
     * Unregister an endpoint resolver.
     *
     * @param resolver the EndpointResolver to remove from the chain.
     */
    public synchronized void unregister(EndpointResolver resolver) {
        resolvers.remove(resolver);
    }

    /**
     * Walk the list of registered EndpointResolvers, so as to
     * retrieve a concrete EPR corresponding to the given abstract EPR,
     * returning a cached reference if already resolved.
     * <p>
     * This API is used by any actor that requires a concrete EPR (e.g.
     * a transport-level Conduit), and must be called each and every
     * time the EPR content is to be accessed (e.g. before each connection
     * establishment attempt). 
     *
     * @param logical the abstract EPR to resolve
     * @return the resolved concrete EPR if appropriate, null otherwise
     */
    public synchronized EndpointReferenceType resolve(EndpointReferenceType logical) {
        EndpointReferenceType physical = null;
        for (EndpointResolver resolver : resolvers) {
            physical = resolver.resolve(logical);
            if (physical != null) {
                break;
            }
        }
        return physical;
    }
    
    /**
     * Walk the list of registered EndpointResolvers, so as to force a fresh 
     * resolution of the given abstract EPR, discarding any previously cached 
     * reference.
     * <p>
     * This API may be used by say the transport-level Conduit when it
     * detects a non-transient error on the outgoing connection, or
     * by any other actor in the dispatch with the ability to infer
     * server-side unavailability.
     * 
     * @param logical the previously resolved abstract EPR
     * @param physical the concrete EPR to refresh
     * @return the renewed concrete EPR if appropriate, null otherwise
     */
    public EndpointReferenceType renew(EndpointReferenceType logical,
                                       EndpointReferenceType physical) {
        EndpointReferenceType fresh = null;
        for (EndpointResolver resolver : resolvers) {
            fresh = resolver.renew(logical, physical);
            if (fresh != null) {
                break;
            }
        }
        return fresh;
    }
    
    /**
     * Walk the list of registered EndpointResolvers, so as to mint a new 
     * abstract EPR for a given service name.
     * 
     * @param serviceName
     * @return the newly minted EPR if appropriate, null otherwise
     */
    public EndpointReferenceType mint(QName serviceName) {
        EndpointReferenceType logical = null;
        for (EndpointResolver resolver : resolvers) {
            logical = resolver.mint(serviceName);
            if (logical != null) {
                break;
            }
        }
        return logical;
    }
    
    /**
     * Walk the list of registered EndpointResolvers, so as to mint a new 
     * abstract EPR for a gievn physical EPR.
     * 
     * @param serviceName
     * @return the newly minted EPR if appropriate, null otherwise
     */
    public EndpointReferenceType mint(EndpointReferenceType physical) {
        EndpointReferenceType logical = null;
        for (EndpointResolver resolver : resolvers) {
            logical = resolver.mint(physical);
            if (logical != null) {
                break;
            }
        }
        return logical;        
    }
    
    /**
     * @return the encapsulated list of registered resolvers
     */
    protected List<EndpointResolver> getResolvers() {
        return resolvers;
    }
    
    /**
     * @param b Bus to encapsulate
     */
    public void setBus(Bus b) {
        bus = b;
    }
}
