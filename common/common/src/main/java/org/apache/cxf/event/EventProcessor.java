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

package org.apache.cxf.event;

public interface EventProcessor {
    
    /**
     * Sends an event to the processor.
     * @param e the event
     */
    void sendEvent(Event e);
    
    /**
     * Registers an event listener with this event processor.
     * @param listener the event listener
     */
    void addEventListener(EventListener listener);
    
    /**
     * Registers an event listener with this event processor. The listener will
     * only be notified when the event passes through the specified filter.
     * @param listener the event listener
     * @param filter the event filter
     */
    void addEventListener(EventListener listener, EventFilter filter);
    

    /**
     * Unregisters an event listener.
     * @param listener the event listener
     */
    void removeEventListener(EventListener listener);
}
