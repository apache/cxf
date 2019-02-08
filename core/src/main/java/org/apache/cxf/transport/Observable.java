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

/**
 * Allows Observers to register for notification on incoming messages.
 */
public interface Observable {
    /**
     * Register a message observer for incoming messages.
     *
     * @param observer the observer to notify on receipt of incoming
     */
    void setMessageObserver(MessageObserver observer);


    /**
     * Retrieves the message observer for incoming messages
     * @return the MessageObserver for incoming messages
     */
    MessageObserver getMessageObserver();
}
