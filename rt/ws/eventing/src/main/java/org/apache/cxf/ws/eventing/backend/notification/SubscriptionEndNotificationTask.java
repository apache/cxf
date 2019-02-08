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


import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.eventing.LanguageSpecificStringType;
import org.apache.cxf.ws.eventing.SubscriptionEnd;
import org.apache.cxf.ws.eventing.backend.database.SubscriptionTicket;
import org.apache.cxf.ws.eventing.client.EndToEndpoint;
import org.apache.cxf.ws.eventing.shared.handlers.ReferenceParametersAddingHandler;

public class SubscriptionEndNotificationTask implements Runnable {

    private SubscriptionTicket target;
    private String reason;
    private SubscriptionEndStatus status;

    public SubscriptionEndNotificationTask(SubscriptionTicket ticket, String reason,
                                           SubscriptionEndStatus status) {
        this.target = ticket;
        this.reason = reason;
        this.status = status;
    }

    @Override
    public void run() {
        try {
            // needed SOAP handlers
            ReferenceParametersAddingHandler handler = new
                    ReferenceParametersAddingHandler(
                    target.getNotificationReferenceParams());
            JaxWsProxyFactoryBean service = new JaxWsProxyFactoryBean();
            service.getOutInterceptors().add(new LoggingOutInterceptor());
            service.setServiceClass(EndToEndpoint.class);
            service.setAddress(target.getEndToURL());
            service.getHandlers().add(handler);

            EndToEndpoint endpoint = (EndToEndpoint)service.create();
            SubscriptionEnd message = new SubscriptionEnd();
            message.setStatus(status.toString());
            if (reason != null) {
                LanguageSpecificStringType reasonElement = new LanguageSpecificStringType();
                reasonElement.setLang("en-US");
                reasonElement.setValue(reason);
                message.getReason().add(reasonElement);
            }
            endpoint.subscriptionEnd(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
