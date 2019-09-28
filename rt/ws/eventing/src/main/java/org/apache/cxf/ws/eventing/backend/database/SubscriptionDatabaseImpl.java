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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.ws.eventing.shared.faults.UnknownSubscription;

/**
 * Default implementation of a Subscription Database. Basically it is simply a wrapper
 * around a List<SubscriptionTicket> with some convenience methods. The underlying List
 * is a thread-safe CopyOnWriteArrayList.
 */
public class SubscriptionDatabaseImpl implements SubscriptionDatabase {

    private static final Logger LOG = LogUtils.getLogger(SubscriptionDatabaseImpl.class);
    private final List<SubscriptionTicket> ticketList;

    public SubscriptionDatabaseImpl() {
        LOG.info("Instantiating SubscriptionDatabaseImpl");
        ticketList = new CopyOnWriteArrayList<>();
    }


    @Override
    public void addTicket(SubscriptionTicket ticket) {
        ticketList.add(ticket);
        LOG.info("SubscriptionDatabaseImpl accepted ticket for subscription: " + ticket.getUuid());
    }

    @Override
    public List<SubscriptionTicket> getTickets() {
        return ticketList;
    }

    /**
     * Searches the database for a ticket with the specified UUID
     *
     * @param id the UUID which will be searched for
     * @return the ticket, or null of no ticket with this UUID exists
     */
    @Override
    public SubscriptionTicket findById(UUID id) {
        for (SubscriptionTicket ticket : ticketList) {
            if (ticket.getUuid().equals(id)) {
                return ticket;
            }
        }
        return null;
    }

    /**
     * Removes a ticket by UUID from the database.
     *
     * @param id the UUID of the ticket to remove
     * @throws UnknownSubscription if unknown UUID is supplied
     */
    @Override
    public synchronized void removeTicketByUUID(UUID id) {
        boolean removed = false;
        for (SubscriptionTicket ticket : ticketList) {
            if (ticket.getUuid().equals(id)) {
                ticketList.remove(ticket);
                removed = true;
                break;
            }
        }
        if (!removed) {
            throw new UnknownSubscription();
        }
    }

}
