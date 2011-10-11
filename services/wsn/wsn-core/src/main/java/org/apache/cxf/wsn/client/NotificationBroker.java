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
package org.apache.cxf.wsn.client;

import java.util.Collections;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.apache.cxf.wsn.AbstractSubscription;
import org.apache.cxf.wsn.util.WSNHelper;
import org.oasis_open.docs.wsn.b_2.FilterType;
import org.oasis_open.docs.wsn.b_2.GetCurrentMessage;
import org.oasis_open.docs.wsn.b_2.GetCurrentMessageResponse;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.oasis_open.docs.wsn.b_2.QueryExpressionType;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.b_2.SubscribeResponse;
import org.oasis_open.docs.wsn.b_2.TopicExpressionType;
import org.oasis_open.docs.wsn.b_2.UseRaw;
import org.oasis_open.docs.wsn.br_2.RegisterPublisher;
import org.oasis_open.docs.wsn.br_2.RegisterPublisherResponse;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationFailedFault;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationRejectedFault;
import org.oasis_open.docs.wsn.bw_2.InvalidFilterFault;
import org.oasis_open.docs.wsn.bw_2.InvalidMessageContentExpressionFault;
import org.oasis_open.docs.wsn.bw_2.InvalidProducerPropertiesExpressionFault;
import org.oasis_open.docs.wsn.bw_2.InvalidTopicExpressionFault;
import org.oasis_open.docs.wsn.bw_2.MultipleTopicsSpecifiedFault;
import org.oasis_open.docs.wsn.bw_2.NoCurrentMessageOnTopicFault;
import org.oasis_open.docs.wsn.bw_2.NotifyMessageNotSupportedFault;
import org.oasis_open.docs.wsn.bw_2.SubscribeCreationFailedFault;
import org.oasis_open.docs.wsn.bw_2.TopicExpressionDialectUnknownFault;
import org.oasis_open.docs.wsn.bw_2.TopicNotSupportedFault;
import org.oasis_open.docs.wsn.bw_2.UnacceptableInitialTerminationTimeFault;
import org.oasis_open.docs.wsn.bw_2.UnrecognizedPolicyRequestFault;
import org.oasis_open.docs.wsn.bw_2.UnsupportedPolicyRequestFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

public class NotificationBroker implements Referencable {

    private final org.oasis_open.docs.wsn.brw_2.NotificationBroker broker;
    private final W3CEndpointReference epr;

    public NotificationBroker(String address) {
        this(WSNHelper.createWSA(address));
    }

    public NotificationBroker(W3CEndpointReference epr) {
        this.broker = WSNHelper.getPort(epr, org.oasis_open.docs.wsn.brw_2.NotificationBroker.class);
        this.epr = epr;
    }

    public org.oasis_open.docs.wsn.brw_2.NotificationBroker getBroker() {
        return broker;
    }

    public W3CEndpointReference getEpr() {
        return epr;
    }

    public void notify(String topic, Object msg) {
        notify(null, topic, msg);
    }

    public void notify(Referencable publisher, String topic, Object msg) {
        Notify notify = new Notify();
        NotificationMessageHolderType holder = new NotificationMessageHolderType();
        if (publisher != null) {
            holder.setProducerReference(publisher.getEpr());
        }
        if (topic != null) {
            TopicExpressionType topicExp = new TopicExpressionType();
            topicExp.getContent().add(topic);
            holder.setTopic(topicExp);
        }
        holder.setMessage(new NotificationMessageHolderType.Message());
        holder.getMessage().setAny(msg);
        notify.getNotificationMessage().add(holder);
        broker.notify(notify);
    }

    public Subscription subscribe(Referencable consumer, String topic) 
        throws TopicExpressionDialectUnknownFault, InvalidFilterFault, TopicNotSupportedFault,
        UnacceptableInitialTerminationTimeFault, SubscribeCreationFailedFault, 
        InvalidMessageContentExpressionFault, InvalidTopicExpressionFault, ResourceUnknownFault, 
        UnsupportedPolicyRequestFault, UnrecognizedPolicyRequestFault, 
        NotifyMessageNotSupportedFault, InvalidProducerPropertiesExpressionFault {
        
        return subscribe(consumer, topic, null, false);
    }

    public Subscription subscribe(Referencable consumer, String topic, String xpath) 
        throws TopicExpressionDialectUnknownFault, InvalidFilterFault, TopicNotSupportedFault, 
        UnacceptableInitialTerminationTimeFault, SubscribeCreationFailedFault, 
        InvalidMessageContentExpressionFault, InvalidTopicExpressionFault, ResourceUnknownFault, 
        UnsupportedPolicyRequestFault, UnrecognizedPolicyRequestFault, NotifyMessageNotSupportedFault, 
        InvalidProducerPropertiesExpressionFault {
        return subscribe(consumer, topic, xpath, false);
    }

    public Subscription subscribe(Referencable consumer, String topic,
                                  String xpath, boolean raw) 
        throws TopicNotSupportedFault, InvalidFilterFault, TopicExpressionDialectUnknownFault, 
        UnacceptableInitialTerminationTimeFault, SubscribeCreationFailedFault, 
        InvalidMessageContentExpressionFault, InvalidTopicExpressionFault, UnrecognizedPolicyRequestFault, 
        UnsupportedPolicyRequestFault, ResourceUnknownFault, NotifyMessageNotSupportedFault, 
        InvalidProducerPropertiesExpressionFault {

        Subscribe subscribeRequest = new Subscribe();
        subscribeRequest.setConsumerReference(consumer.getEpr());
        subscribeRequest.setFilter(new FilterType());
        if (topic != null) {
            TopicExpressionType topicExp = new TopicExpressionType();
            topicExp.getContent().add(topic);
            subscribeRequest.getFilter().getAny().add(
                    new JAXBElement<TopicExpressionType>(AbstractSubscription.QNAME_TOPIC_EXPRESSION,
                            TopicExpressionType.class, topicExp));
        }
        if (xpath != null) {
            QueryExpressionType xpathExp = new QueryExpressionType();
            xpathExp.setDialect(AbstractSubscription.XPATH1_URI);
            xpathExp.getContent().add(xpath);
            subscribeRequest.getFilter().getAny().add(
                    new JAXBElement<QueryExpressionType>(AbstractSubscription.QNAME_MESSAGE_CONTENT,
                            QueryExpressionType.class, xpathExp));
        }
        if (raw) {
            subscribeRequest.setSubscriptionPolicy(new Subscribe.SubscriptionPolicy());
            subscribeRequest.getSubscriptionPolicy().getAny().add(new UseRaw());
        }
        SubscribeResponse response = broker.subscribe(subscribeRequest);
        return new Subscription(response.getSubscriptionReference());
    }

    public List<Object> getCurrentMessage(String topic) 
        throws TopicNotSupportedFault, TopicExpressionDialectUnknownFault, MultipleTopicsSpecifiedFault, 
        InvalidTopicExpressionFault, ResourceUnknownFault, NoCurrentMessageOnTopicFault {
        GetCurrentMessage getCurrentMessageRequest = new GetCurrentMessage();
        if (topic != null) {
            TopicExpressionType topicExp = new TopicExpressionType();
            topicExp.getContent().add(topic);
            getCurrentMessageRequest.setTopic(topicExp);
        }
        GetCurrentMessageResponse response = broker.getCurrentMessage(getCurrentMessageRequest);
        return response.getAny();
    }

    public Registration registerPublisher(Referencable publisher, String topic) 
        throws TopicNotSupportedFault, PublisherRegistrationFailedFault, 
        UnacceptableInitialTerminationTimeFault, InvalidTopicExpressionFault, 
        ResourceUnknownFault, PublisherRegistrationRejectedFault {
        return registerPublisher(publisher, topic, false);
    }

    public Registration registerPublisher(Referencable publisher, String topic, boolean demand) 
        throws TopicNotSupportedFault, PublisherRegistrationFailedFault, 
        UnacceptableInitialTerminationTimeFault, InvalidTopicExpressionFault, ResourceUnknownFault, 
        PublisherRegistrationRejectedFault {
        return registerPublisher(publisher, Collections.singletonList(topic), demand);
    }

    public Registration registerPublisher(Referencable publisher, List<String> topics, boolean demand)
        throws TopicNotSupportedFault, PublisherRegistrationFailedFault, 
        UnacceptableInitialTerminationTimeFault, InvalidTopicExpressionFault, ResourceUnknownFault, 
        PublisherRegistrationRejectedFault {

        RegisterPublisher registerPublisherRequest = new RegisterPublisher();
        registerPublisherRequest.setPublisherReference(publisher.getEpr());
        if (topics != null) {
            for (String topic : topics) {
                TopicExpressionType topicExp = new TopicExpressionType();
                topicExp.getContent().add(topic);
                registerPublisherRequest.getTopic().add(topicExp);
            }
        }
        registerPublisherRequest.setDemand(demand);
        RegisterPublisherResponse response = broker.registerPublisher(registerPublisherRequest);
        return new Registration(response.getPublisherRegistrationReference());
    }

}
