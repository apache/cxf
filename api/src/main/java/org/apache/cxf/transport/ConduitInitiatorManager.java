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
 * The ConduitInitiatorManager provides an interface to register and retrieve
 * transport factories.
 */
public interface ConduitInitiatorManager {

    /**
     * Associates a name, often a URI, with a <code>ConduitInitiator</code>
     * when registering with the <code>Bus</code>'s <code>TransportRegistry</code>.
     * @param name A string containing the name used to identify the
     * <code>ConduitInitiator</code>
     * @param factory The <code>ConduitInitiator</code> to be registered.
     */
    void registerConduitInitiator(String name, ConduitInitiator factory);

    /**
     * Unregister a <code>ConduitInitiator</code>.
     * @param name A string containing the name of the
     * <code>ConduitInitiator</code>.
     */
    void deregisterConduitInitiator(String name);
    
    /**
     * Returns the <code>ConduitInitiator</code> registered with the specified name, 
     * loading the appropriate plugin if necessary.
     * 
     * @param name
     * @return the registered <code>ConduitInitiator</code>
     * @throws BusException
     */
    ConduitInitiator getConduitInitiator(String name) throws BusException;
    
    ConduitInitiator getConduitInitiatorForUri(String uri);
}
