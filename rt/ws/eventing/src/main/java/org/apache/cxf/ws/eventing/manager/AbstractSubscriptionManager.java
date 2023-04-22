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

package org.apache.cxf.ws.eventing.manager;

import java.util.UUID;
import java.util.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.xml.ws.WebServiceContext;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.ws.eventing.ExpirationType;
import org.apache.cxf.ws.eventing.GetStatus;
import org.apache.cxf.ws.eventing.GetStatusResponse;
import org.apache.cxf.ws.eventing.Renew;
import org.apache.cxf.ws.eventing.RenewResponse;
import org.apache.cxf.ws.eventing.Unsubscribe;
import org.apache.cxf.ws.eventing.UnsubscribeResponse;
import org.apache.cxf.ws.eventing.backend.database.SubscriptionTicket;
import org.apache.cxf.ws.eventing.backend.manager.SubscriptionManagerInterfaceForManagers;
import org.apache.cxf.ws.eventing.shared.faults.UnknownSubscription;
import org.apache.cxf.ws.eventing.shared.utils.DurationAndDateUtil;

public abstract class AbstractSubscriptionManager implements SubscriptionManagerEndpoint {

    protected static final Logger LOG = LogUtils.getLogger(AbstractSubscriptionManager.class);

    @Resource
    protected WebServiceContext context;

    public AbstractSubscriptionManager() {
    }


    @Override
    public RenewResponse renewOp(Renew body) {
        RenewResponse response = new RenewResponse();
        String uuid = retrieveSubscriptionUUID();
        LOG.info("received Renew message for UUID=" + uuid);
        ExpirationType expiration = getSubscriptionManagerBackend()
                .renew(UUID.fromString(uuid), body.getExpires());
        response.setGrantedExpires(expiration);
        LOG.info("Extended subscription for UUID=" + uuid + " to " + expiration.getValue());
        return response;
    }

    @Override
    public GetStatusResponse getStatusOp(GetStatus body) {
        String uuid = retrieveSubscriptionUUID();
        LOG.info("received GetStatus message for UUID=" + uuid);
        SubscriptionTicket ticket = obtainTicketFromDatabaseOrThrowFault(uuid);
        GetStatusResponse response = new GetStatusResponse();
        response.setGrantedExpires(
                DurationAndDateUtil.toExpirationTypeContainingGregorianCalendar(ticket.getExpires()));
        return response;
    }

    @Override
    public UnsubscribeResponse unsubscribeOp(Unsubscribe body) {
        String uuid = retrieveSubscriptionUUID();
        LOG.info("received Unsubscribe message for UUID=" + uuid);
        getSubscriptionManagerBackend().unsubscribeTicket(UUID.fromString(uuid));
        LOG.info("successfully removed subscription with UUID " + uuid);
        return new UnsubscribeResponse();
    }

    protected abstract SubscriptionManagerInterfaceForManagers getSubscriptionManagerBackend();

    /**
     * Retrieves the subscription's uuid as it was specified in SOAP header.
     * Messages sent to SubscriptionManager by clients always need to specify the uuid.
     *
     * @return the uuid of the subscription specified in this message's headers. Note:
     *         obtaining this doesn't yet make sure that this subscription actually exists.
     */
    protected String retrieveSubscriptionUUID() {
        Object uuid = ((WrappedMessageContext)context.getMessageContext()).getWrappedMessage()
                .getContextualProperty("uuid");
        if (uuid == null) {
            throw new UnknownSubscription();
        }
        if (uuid.getClass() != String.class) {
            throw new Error("Subscription ID should be a String but is " + uuid.getClass().getName());
        }
        return (String)uuid;
    }

    /**
     * searches the subscription database for a subscription by the given UUID
     *
     * @param uuid
     * @return the SubscriptionTicket, or throws UnknownSubscription fault if no such subscription exists
     */
    protected SubscriptionTicket obtainTicketFromDatabaseOrThrowFault(String uuid) {
        SubscriptionTicket ticket = getSubscriptionManagerBackend().findTicket(UUID.fromString(uuid));
        if (ticket == null) {
            LOG.severe("Unknown ticket UUID: " + uuid);
            throw new UnknownSubscription();
        }
        return ticket;
    }

}
