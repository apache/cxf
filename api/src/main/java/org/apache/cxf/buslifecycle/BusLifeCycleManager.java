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

package org.apache.cxf.buslifecycle;

/**
 * The manager interface for registering <code>BusLifeCycleListener</code>s.
 *
 * A class that implements the BusLifeCycleListener interface can be
 * registered or unregistered to receive notification of <code>Bus</code>
 * lifecycle events.
 */
public interface BusLifeCycleManager extends BusLifeCycleListener {

    /**
     * Register a listener to receive <code>Bus</code> lifecycle notification.
     *
     * @param listener The <code>BusLifeCycleListener</code> that will
     * receive the events.
     */
    void registerLifeCycleListener(BusLifeCycleListener listener);

    /**
     * Unregister a listener so that it will no longer receive <code>Bus</code>
     * lifecycle events.
     *
     * @param listener The <code>BusLifeCycleListener</code> to unregister.
     */
    void unregisterLifeCycleListener(BusLifeCycleListener listener);
}
