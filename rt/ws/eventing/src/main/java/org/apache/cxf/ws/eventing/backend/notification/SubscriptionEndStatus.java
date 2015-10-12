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

package org.apache.cxf.ws.eventing.backend.notification;

import org.apache.cxf.ws.eventing.shared.EventingConstants;

public enum SubscriptionEndStatus {

    DELIVERY_FAILURE(EventingConstants.SUBSCRIPTION_END_DELIVERY_FAILURE),
    SOURCE_SHUTTING_DOWN(EventingConstants.SUBSCRIPTION_END_SHUTTING_DOWN),
    SOURCE_CANCELLING(EventingConstants.SUBSCRIPTION_END_SOURCE_CANCELLING);

    private String namespace;

    SubscriptionEndStatus(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String toString() {
        return namespace;
    }
}
