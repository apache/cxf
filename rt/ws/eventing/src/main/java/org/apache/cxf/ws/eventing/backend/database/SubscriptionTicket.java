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

package org.apache.cxf.ws.eventing.backend.database;

import java.util.GregorianCalendar;
import java.util.UUID;

import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.eventing.DeliveryType;
import org.apache.cxf.ws.eventing.FilterType;
import org.apache.cxf.ws.eventing.shared.faults.FilteringRequestedUnavailable;
import org.apache.cxf.ws.eventing.shared.utils.FilteringUtil;



/**
 * This class represents a valid subscription granted to a requesting client. Instances of such tickets
 * are stored in a SubscriptionDatabase.
 */
public class SubscriptionTicket {

    private EndpointReferenceType endTo;
    private DeliveryType delivery;
    private XMLGregorianCalendar expires;
    private FilterType filter;
    private UUID uuid;
    private boolean wrappedDelivery;

    /**
     * If set to true, this ticket does not expire and the 'expires' field is ignored.
     */
    private boolean nonExpiring;

    public SubscriptionTicket() {
    }

    public XMLGregorianCalendar getExpires() {
        return expires;
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

    public boolean isExpired() {
        if (nonExpiring) {
            return false;
        }
        return expires.toGregorianCalendar().before(new GregorianCalendar());
    }

    public void setExpires(XMLGregorianCalendar expires) {
        this.expires = expires;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Convenience method to extract the subscribed target URL.
     *
     * @return
     */
    public String getTargetURL() {
        @SuppressWarnings("unchecked")
        JAXBElement<EndpointReferenceType> el
            = (JAXBElement<EndpointReferenceType>)this.getDelivery().getContent().get(0);
        return el.getValue().getAddress().getValue().trim();
    }

    /**
     * Convenience method to extract the subscribed target URL.
     *
     * @return
     */
    public String getEndToURL() {
        try {
            return this.getEndTo().getAddress().getValue();
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public String getFilterString() {
        try {
            return (String)this.getFilter().getContent().get(0);
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public boolean isWrappedDelivery() {
        return wrappedDelivery;
    }

    public void setWrappedDelivery(boolean wrappedDelivery) {
        this.wrappedDelivery = wrappedDelivery;
    }

    public ReferenceParametersType getNotificationReferenceParams() {
        @SuppressWarnings("unchecked")
        JAXBElement<EndpointReferenceType> el
            = (JAXBElement<EndpointReferenceType>)this.getDelivery().getContent().get(0);
        return el.getValue().getReferenceParameters();
    }

    public boolean isNonExpiring() {
        return nonExpiring;
    }

    public void setNonExpiring(boolean nonExpiring) {
        this.nonExpiring = nonExpiring;
    }
}
