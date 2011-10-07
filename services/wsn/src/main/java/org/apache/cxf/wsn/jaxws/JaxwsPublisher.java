/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cxf.wsn.jaxws;

import javax.jws.WebService;
import javax.xml.bind.JAXBElement;

import org.apache.cxf.wsn.AbstractSubscription;
import org.apache.cxf.wsn.jms.JmsPublisher;
import org.apache.cxf.wsn.util.WSNHelper;
import org.oasis_open.docs.wsn.b_2.FilterType;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.b_2.SubscribeResponse;
import org.oasis_open.docs.wsn.b_2.TopicExpressionType;
import org.oasis_open.docs.wsn.b_2.Unsubscribe;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationFailedFault;
import org.oasis_open.docs.wsn.bw_2.NotificationProducer;
import org.oasis_open.docs.wsn.bw_2.SubscriptionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebService(endpointInterface = "org.oasis_open.docs.wsn.brw_2.PublisherRegistrationManager")
public class JaxwsPublisher extends JmsPublisher {

    private final Logger logger = LoggerFactory.getLogger(JaxwsPublisher.class);

    protected JaxwsNotificationBroker notificationBroker;
    private NotificationProducer notificationProducer;

    public JaxwsPublisher(String name, JaxwsNotificationBroker notificationBroker) {
        super(name);
        this.notificationBroker = notificationBroker;
    }

    @Override
    protected void start() throws PublisherRegistrationFailedFault {
        super.start();
        if (demand) {
            notificationProducer = WSNHelper.getPort(publisherReference, NotificationProducer.class);
        }
    }

    @Override
    protected Object startSubscription(TopicExpressionType topic) {
        try {
            Subscribe subscribeRequest = new Subscribe();
            subscribeRequest.setConsumerReference(notificationBroker.getEpr());
            subscribeRequest.setFilter(new FilterType());
            subscribeRequest.getFilter().getAny().add(
                    new JAXBElement<TopicExpressionType>(AbstractSubscription.QNAME_TOPIC_EXPRESSION,
                            TopicExpressionType.class, topic));
            SubscribeResponse response = notificationProducer.subscribe(subscribeRequest);
            return WSNHelper.getPort(response.getSubscriptionReference(), SubscriptionManager.class);
        } catch (Exception e) {
            logger.info("Error while subscribing on-demand publisher", e);
            return null;
        }
    }

    @Override
    protected void stopSubscription(Object sub) {
        try {
            ((SubscriptionManager) sub).unsubscribe(new Unsubscribe());
        } catch (Exception e) {
            logger.info("Error while unsubscribing on-demand publisher", e);
        }
    }

}
