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

import java.util.List;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.eventing.DeliveryType;
import org.apache.cxf.ws.eventing.ExpirationType;
import org.apache.cxf.ws.eventing.FilterType;
import org.apache.cxf.ws.eventing.FormatType;
import org.apache.cxf.ws.eventing.backend.database.SubscriptionTicket;

public interface SubscriptionManagerInterfaceForEventSources {

    SubscriptionTicketGrantingResponse subscribe(DeliveryType delivery, EndpointReferenceType endTo,
                                                 ExpirationType expires, FilterType filter,
                                                 FormatType format);

    /**
     * READ ONLY. Returns an unmodifiable list of the subscriptions in database.
     */
    List<SubscriptionTicket> getTickets();

}
