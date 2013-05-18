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

package org.apache.cxf.ws.eventing.backend.manager;

import java.util.UUID;

import org.apache.cxf.ws.eventing.backend.notification.SubscriptionEndStatus;

/**
 * The core functionality representing WS-Eventing backend logic. It holds an instance of a database and
 * acts as a layer for communicating with it. There are two interfaces which are used to communicate
 * with a SubscriptionManager:
 * - SubscriptionManagerInterfaceForManagers is used by the manager Web Service
 * - SubscriptionManagerInterfaceForEventSources is used by the event source Web Service
 */
public interface SubscriptionManager
        extends SubscriptionManagerInterfaceForManagers,
        SubscriptionManagerInterfaceForEventSources,
        SubscriptionManagerInterfaceForNotificators {

    void subscriptionEnd(UUID subscriptionId, String reason, SubscriptionEndStatus status);

}
