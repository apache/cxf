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

import javax.xml.namespace.QName;

import org.apache.cxf.ws.addressing.EndpointReferenceType;


/**
 * Implementations of this interface are responsible for mapping
 * between abstract and concrete endpoint references, and/or
 * renewing stale references.
 * <p>
 * An underlying mechanism in the style of the OGSA WS-Naming
 * specification is assumed, where an EPR maybe be fully abstract,
 * or concrete but with sufficient information embedded to enable
 * its renewal if necessary.
 */
public interface EndpointResolver {
    /**
     * Retrieve a concrete EPR corresponding to the given abstract EPR,
     * returning a cached reference if already resolved.
     *
     * @param logical the abstract EPR to resolve
     * @return the resolved concrete EPR if appropriate, null otherwise
     */
    EndpointReferenceType resolve(EndpointReferenceType logical);

    /**
     * Force a fresh resolution of the given abstract EPR, discarding any
     * previously cached reference.
     *
     * @param logical the previously resolved abstract EPR
     * @param physical the concrete EPR to refresh
     * @return the renewed concrete EPR if appropriate, null otherwise
     */
    EndpointReferenceType renew(EndpointReferenceType logical,
                                EndpointReferenceType physical);
    
    /**
     * Mint a new abstract EPR for a given service name.
     * 
     * @param serviceName
     * @return the newly minted EPR if appropriate, null otherwise
     */
    EndpointReferenceType mint(QName serviceName);
    
    /**
     * Mint a new abstract EPR for a given concrete EPR
     * 
     * @param physical
     * @return the newly minted EPR if appropriate, null otherwise
     */
    EndpointReferenceType mint(EndpointReferenceType physical);
}
