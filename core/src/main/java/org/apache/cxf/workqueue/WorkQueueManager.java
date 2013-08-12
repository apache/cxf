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
package org.apache.cxf.workqueue;

public interface WorkQueueManager {

    /**
     * Get the manager's default work queue.
     * @return AutomaticWorkQueue
     */
    AutomaticWorkQueue getAutomaticWorkQueue();

    /**
     * Get the named work queue.
     * @return AutomaticWorkQueue
     */
    AutomaticWorkQueue getNamedWorkQueue(String name);
    
    /**
     * Adds a named work queue
     * @param name
     * @param q
     */
    void addNamedWorkQueue(String name, AutomaticWorkQueue q);
    
    /**
     * Shuts down the manager's work queue. If
     * <code>processRemainingTasks</code> is true, waits for the work queue to
     * shutdown before returning.
     * @param processRemainingTasks - whether or not to wait for completion
     */
    void shutdown(boolean processRemainingTasks);
    
    /**
     * Only returns after workqueue has been shutdown.
     *
     */
    void run();
}
