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
package org.apache.cxf.wsn.services;

import jakarta.jws.WebService;
import org.apache.cxf.wsn.jms.JmsSubscription;
import org.apache.cxf.wsn.util.WSNHelper;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.b_2.SubscribeCreationFailedFaultType;
import org.oasis_open.docs.wsn.bw_2.InvalidFilterFault;
import org.oasis_open.docs.wsn.bw_2.InvalidMessageContentExpressionFault;
import org.oasis_open.docs.wsn.bw_2.InvalidProducerPropertiesExpressionFault;
import org.oasis_open.docs.wsn.bw_2.InvalidTopicExpressionFault;
import org.oasis_open.docs.wsn.bw_2.NotificationConsumer;
import org.oasis_open.docs.wsn.bw_2.SubscribeCreationFailedFault;
import org.oasis_open.docs.wsn.bw_2.TopicExpressionDialectUnknownFault;
import org.oasis_open.docs.wsn.bw_2.TopicNotSupportedFault;
import org.oasis_open.docs.wsn.bw_2.UnacceptableInitialTerminationTimeFault;
import org.oasis_open.docs.wsn.bw_2.UnrecognizedPolicyRequestFault;
import org.oasis_open.docs.wsn.bw_2.UnsupportedPolicyRequestFault;

@WebService(endpointInterface = "org.oasis_open.docs.wsn.bw_2.PausableSubscriptionManager",
            targetNamespace = "http://cxf.apache.org/wsn/jaxws",
            serviceName = "PausableSubscriptionManagerService",
            portName = "PausableSubscriptionManagerPort"
)
public class JaxwsSubscription extends JmsSubscription {

    private NotificationConsumer consumer;

    public JaxwsSubscription(String name) {
        super(name);
    }

    @Override
    protected void validateSubscription(Subscribe subscribeRequest)
        //CHECKSTYLE:OFF - WS-Notification spec throws a lot of faults
        throws InvalidFilterFault,
            InvalidMessageContentExpressionFault, InvalidProducerPropertiesExpressionFault,
            InvalidTopicExpressionFault, SubscribeCreationFailedFault, TopicExpressionDialectUnknownFault,
            TopicNotSupportedFault, UnacceptableInitialTerminationTimeFault,
            UnsupportedPolicyRequestFault, UnrecognizedPolicyRequestFault {
        //CHECKSTYLE:ON
        super.validateSubscription(subscribeRequest);
        // TODO: implement raw notifications
        if (useRaw) {
            SubscribeCreationFailedFaultType fault = new SubscribeCreationFailedFaultType();
            throw new SubscribeCreationFailedFault("Raw notifications are not supported", fault);
        }
        try {
            consumer = WSNHelper.getInstance().getPort(subscribeRequest.getConsumerReference(),
                                                       NotificationConsumer.class);
        } catch (Exception e) {
            SubscribeCreationFailedFaultType fault = new SubscribeCreationFailedFaultType();
            throw new SubscribeCreationFailedFault("Unable to resolve consumer reference endpoint", fault, e);
        }
    }

    @Override
    protected void doNotify(Notify notify) {
        // TODO: implement raw notifications
        consumer.notify(notify);
    }
}
