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

import javax.xml.namespace.QName;

/**
 * A registry for maintaining a collection of contract resolvers.
 */
public interface ServiceContractResolverRegistry {

    /**
     * Resolves a service's QName to a URI representing the location of a 
     * WSDL contract. The registry method is called by the bus and should use 
     * the <code>getContractLocation</code> methods of the registered contract 
     * resolvers to do the actual resolution.
     *
     * @param qname the service qname to resolve into a URI
     * @return URI representing the WSDL contract's location
     */
    URI getContractLocation(QName qname);

    /**
     * Registers a contract resolver.
     *
     * @param resolver the contract resolver being registered
     */
    void register(ServiceContractResolver resolver);

    /**
     * Removes a contract resolver from the registry.
     *
     * @param resolver the contract resolver being removed
     */
    void unregister(ServiceContractResolver resolver);

    /**
     * Determines if a contract resolver is already registered with a
     * registry.
     *
     * @param resolver the contract resolver for which to search
     * @return <code>true</code> if the contract resolver is already registered
     */
    boolean isRegistered(ServiceContractResolver resolver);

}
