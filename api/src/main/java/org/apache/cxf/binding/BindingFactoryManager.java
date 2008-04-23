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

package org.apache.cxf.binding;

import org.apache.cxf.BusException;

/**
 * The manager interface represents a repository for accessing 
 * <code>BindingFactory</code>s.
 *
 * Provides methods necessary for registering, deregistering or retrieving of
 * BindingFactorys.
 */
public interface BindingFactoryManager {

    /**
     * Registers a BindingFactory using the provided name.
     *
     * @param name The name of the BindingFactory.
     * @param binding The instance of the class that implements the
     * BindingFactory interface.
     */
    void registerBindingFactory(String name, BindingFactory binding);
    
    /**
     * Deregisters the BindingFactory with the provided name.
     *
     * @param name The name of the BindingFactory.
     */
    void unregisterBindingFactory(String name);

    /**
     * Retrieves the BindingFactory registered with the given name.
     *
     * @param name The name of the BindingFactory.
     * @return BindingFactory The registered BindingFactory.
     * @throws BusException If there is an error retrieving the BindingFactory.
     */
    BindingFactory getBindingFactory(String name) throws BusException;
}
