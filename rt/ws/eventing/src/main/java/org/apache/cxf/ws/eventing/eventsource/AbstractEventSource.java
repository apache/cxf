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

package org.apache.cxf.ws.eventing.eventsource;

import java.util.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.xml.ws.WebServiceContext;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.ws.eventing.Subscribe;
import org.apache.cxf.ws.eventing.SubscribeResponse;
import org.apache.cxf.ws.eventing.backend.manager.SubscriptionManagerInterfaceForEventSources;
import org.apache.cxf.ws.eventing.backend.manager.SubscriptionTicketGrantingResponse;
import org.apache.cxf.ws.eventing.shared.utils.DurationAndDateUtil;

/**
 * Default implementation of Event Source web service.
 */
public abstract class AbstractEventSource implements EventSourceEndpoint {

    protected static final Logger LOG = LogUtils.getLogger(AbstractEventSource.class);

    @Resource
    protected WebServiceContext context;

    public AbstractEventSource() {
    }

    @Override
    public SubscribeResponse subscribeOp(Subscribe body) {
        SubscriptionTicketGrantingResponse databaseResponse = getSubscriptionManagerBackend()
                .subscribe(body.getDelivery(), body.getEndTo(), body.getExpires(), body.getFilter(),
                        body.getFormat());
        boolean shouldConvertToDuration;
        if (body.getExpires() != null) {
            shouldConvertToDuration = DurationAndDateUtil.isDuration(body.getExpires().getValue());
        } else {
            shouldConvertToDuration = true;
        }
        return generateResponseMessageFor(databaseResponse, shouldConvertToDuration);
    }

    protected abstract SubscriptionManagerInterfaceForEventSources getSubscriptionManagerBackend();

    protected SubscribeResponse generateResponseMessageFor(SubscriptionTicketGrantingResponse dbResponse,
                                                           boolean shouldConvertToDuration) {
        SubscribeResponse ret = new SubscribeResponse();
        // SubscriptionManager part
        ret.setSubscriptionManager(dbResponse.getSubscriptionManagerReference());
        // Expires part
        if (shouldConvertToDuration) {
            ret.setGrantedExpires(
                    DurationAndDateUtil.toExpirationTypeContainingDuration(dbResponse.getExpires()));
        } else {
            ret.setGrantedExpires(
                    DurationAndDateUtil.toExpirationTypeContainingGregorianCalendar(dbResponse.getExpires()));
        }
        return ret;
    }


}
