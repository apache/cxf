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

public interface AutomaticWorkQueue extends WorkQueue {
    /**
     * Initiates an orderly shutdown. 
     * If <code>processRemainingWorkItems</code>
     * is true, waits for all active items to finish execution before returning, otherwise returns 
     * immediately after removing all non active items from the queue.
     * 
     * @param processRemainingWorkItems
     */
    void shutdown(boolean processRemainingWorkItems);
    
    /**
     * Returns true if this object has been shut down.
     * @return true if this object has been shut down.
     */
    boolean isShutdown();
}
