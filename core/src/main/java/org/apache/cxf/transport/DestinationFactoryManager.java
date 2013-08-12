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

import org.apache.cxf.BusException;

/**
 * The DestinationFactoryManager provides an interface to register and retrieve
 * transport factories.
 */
public interface DestinationFactoryManager {

    /**
     * Associates a name, often a URI, with a <code>DestinationFactory</code>
     * when registering with the <code>Bus</code>'s <code>TransportRegistry</code>.
     * @param name A string containing the name used to identify the
     * <code>DestinationFactory</code>
     * @param factory The <code>DestinationFactory</code> to be registered.
     */
    void registerDestinationFactory(String name, DestinationFactory factory);

    /**
     * Unregister a <code>DestinationFactory</code>.
     * @param name A string containing the name of the
     * <code>DestinationFactory</code>.
     */
    void deregisterDestinationFactory(String name);
    
    /**
     * Returns the <code>DestinationFactory</code> registered with the specified name, 
     * loading the appropriate plugin if necessary.
     * 
     * @param name
     * @return the registered <code>DestinationFactory</code>
     * @throws BusException
     */
    DestinationFactory getDestinationFactory(String name) throws BusException;

    DestinationFactory getDestinationFactoryForUri(String uri);
}
