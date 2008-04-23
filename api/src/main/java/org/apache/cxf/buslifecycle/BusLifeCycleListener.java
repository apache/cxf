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
 * The listener interface for receiving notification of <code>Bus</code>
 * lifecycle events.
 *
 * A class that implements this interface will have its methods called
 * when the associated lifecycle events occur.  An implementing class
 * must register itself with the Bus through the
 * <code>BusLifeCycleManager</code> interface.
 */
public interface BusLifeCycleListener {

    /**
     * Invoked when the <code>Bus</code> has been initialized.
     *
     */
    void initComplete();
    
    /**
     * Invoked before the <code>Bus</code> is shutdown.
     *
     */
    void preShutdown();

    /**
     * Invoked after the <code>Bus</code> is shutdown.
     *
     */
    void postShutdown();
}
