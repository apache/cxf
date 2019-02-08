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

import org.apache.cxf.Bus;

/**
 * The listener interface for receiving notification of when <code>Bus</code>
 * objects are created.
 *
 * It's a simplified form of BusLifeCycleListener that takes the
 * target Bus as a parameter which is appropriate if the listener needs
 * to be a singleton that works with multiple Bus objects.
 *
 * A common pattern would be to register a full BusLifeCycleListener on the
 * target Bus to receive full Bus events
 */
public interface BusCreationListener {

    /**
     * Invoked to create a BusLifeCycleListener.
     */
    void busCreated(Bus b);

}
