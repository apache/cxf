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

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.eventing.DeliveryType;
import org.apache.cxf.ws.eventing.FilterType;
import org.apache.cxf.ws.eventing.shared.faults.FilteringRequestedUnavailable;
import org.apache.cxf.ws.eventing.shared.utils.FilteringUtil;



/**
 * This is the response send from SubscriptionManager backend logic to the EventSource webservice.
 * It contains the necessary information for the Event Source to construct a JAX-WS response
 * for a client who sent a subscription request.
 */
public class SubscriptionTicketGrantingResponse {

    private EndpointReferenceType endTo;

    private DeliveryType delivery;
    private XMLGregorianCalendar expires;
    private FilterType filter;
    private UUID uuid;
    private EndpointReferenceType subscriptionManagerReference;

    public SubscriptionTicketGrantingResponse() {
    }

    public EndpointReferenceType getEndTo() {
        return endTo;
    }

    public void setEndTo(EndpointReferenceType endTo) {
        this.endTo = endTo;
    }

    public DeliveryType getDelivery() {
        return delivery;
    }

    public void setDelivery(DeliveryType delivery) {
        this.delivery = delivery;
    }

    public FilterType getFilter() {
        return filter;
    }

    public void setFilter(FilterType filter) {
        if (!FilteringUtil.isFilteringDialectSupported(filter.getDialect())) {
            throw new FilteringRequestedUnavailable();
        }
        this.filter = filter;
    }

    public void setUUID(UUID uuidToSet) {
        this.uuid = uuidToSet;
    }

    public UUID getUuid() {
        return uuid;
    }

    public EndpointReferenceType getSubscriptionManagerReference() {
        return subscriptionManagerReference;
    }

    public void setSubscriptionManagerReference(EndpointReferenceType subscriptionManagerReference) {
        this.subscriptionManagerReference = subscriptionManagerReference;
    }

    public XMLGregorianCalendar getExpires() {
        return expires;
    }

    public void setExpires(XMLGregorianCalendar expires) {
        this.expires = expires;
    }
}
