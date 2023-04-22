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

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMResult;

import org.w3c.dom.Document;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.ws.wsaddressing.W3CEndpointReference;
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
    public static final String WSN_URI = "http://docs.oasis-open.org/wsn/b-2";

    public static final String XPATH1_URI = "http://www.w3.org/TR/1999/REC-xpath-19991116";

    public static final QName QNAME_TOPIC_EXPRESSION = new QName(WSN_URI, "TopicExpression");

    public static final QName QNAME_MESSAGE_CONTENT = new QName(WSN_URI, "MessageContent");

    public static final QName QNAME_INITIAL_TERMINATION_TIME = new QName(WSN_URI, "InitialTerminationTime");

    public static final QName QNAME_PULLPOINT_QUEUE_NAME = new QName(WSN_URI, "pullPointQueueName");


    private org.oasis_open.docs.wsn.brw_2.NotificationBroker broker;
    private final W3CEndpointReference epr;
    private Class<?>[] extraClasses;
    private JAXBContext context;

    public NotificationBroker(String address, Class<?> ... cls) {
        this(WSNHelper.getInstance().createWSA(address), cls);
    }

    public NotificationBroker(W3CEndpointReference epr, Class<?> ... cls) {
        this.epr = epr;
        this.extraClasses = cls;
    }
    public void setExtraClasses(Class<?> ... c) {
        extraClasses = c;
    }

    public synchronized org.oasis_open.docs.wsn.brw_2.NotificationBroker getBroker() {
        if (broker == null) {
            WSNHelper helper = WSNHelper.getInstance();
            if (helper.supportsExtraClasses()) {
                this.broker = WSNHelper.getInstance()
                    .getPort(epr,
                         org.oasis_open.docs.wsn.brw_2.NotificationBroker.class,
                         extraClasses);
            } else {
                this.broker = WSNHelper.getInstance()
                    .getPort(epr,
                         org.oasis_open.docs.wsn.brw_2.NotificationBroker.class);
                if (extraClasses != null && extraClasses.length > 0) {
                    try {
                        this.context = JAXBContext.newInstance(extraClasses);
                    } catch (JAXBException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return broker;
    }

    public W3CEndpointReference getEpr() {
        return epr;
    }

    public void notify(String topic, Object msg) {
        notify(null, topic, msg);
    }

    public void notify(Referencable publisher, String topic, Object msg) {
        getBroker();
        if (this.context != null) {
            try {
                DOMResult result = new DOMResult();
                context.createMarshaller().marshal(msg, result);
                msg = result.getNode();
                if (msg instanceof Document) {
                    msg = ((Document)msg).getDocumentElement();
                }
            } catch (JAXBException e) {
                //ignore, we'll try and let the runtime handle it as is
            }
        }

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
        getBroker().notify(notify);
    }

    public Subscription subscribe(Referencable consumer, String topic)
        //CHECKSTYLE:OFF - WS-Notification spec throws a lot of faults
        throws TopicExpressionDialectUnknownFault, InvalidFilterFault, TopicNotSupportedFault,
        UnacceptableInitialTerminationTimeFault, SubscribeCreationFailedFault,
        InvalidMessageContentExpressionFault, InvalidTopicExpressionFault, ResourceUnknownFault,
        UnsupportedPolicyRequestFault, UnrecognizedPolicyRequestFault,
        NotifyMessageNotSupportedFault, InvalidProducerPropertiesExpressionFault {
        //CHECKSTYLE:ON

        return subscribe(consumer, topic, null, false, null);
    }



    public Subscription subscribe(Referencable consumer, String topic, String xpath)
        //CHECKSTYLE:OFF - WS-Notification spec throws a lot of faults
        throws TopicExpressionDialectUnknownFault, InvalidFilterFault, TopicNotSupportedFault,
        UnacceptableInitialTerminationTimeFault, SubscribeCreationFailedFault,
        InvalidMessageContentExpressionFault, InvalidTopicExpressionFault, ResourceUnknownFault,
        UnsupportedPolicyRequestFault, UnrecognizedPolicyRequestFault, NotifyMessageNotSupportedFault,
        InvalidProducerPropertiesExpressionFault {
        //CHECKSTYLE:ON
        return subscribe(consumer, topic, xpath, false, null);
    }

    public Subscription subscribe(Referencable consumer, String topic,
                                  String xpath, boolean raw, String initialTerminationTime)
        //CHECKSTYLE:OFF - WS-Notification spec throws a lot of faults
        throws TopicNotSupportedFault, InvalidFilterFault, TopicExpressionDialectUnknownFault,
        UnacceptableInitialTerminationTimeFault, SubscribeCreationFailedFault,
        InvalidMessageContentExpressionFault, InvalidTopicExpressionFault, UnrecognizedPolicyRequestFault,
        UnsupportedPolicyRequestFault, ResourceUnknownFault, NotifyMessageNotSupportedFault,
        InvalidProducerPropertiesExpressionFault {
        //CHECKSTYLE:ON

        Subscribe subscribeRequest = new Subscribe();
        if (initialTerminationTime != null) {
            subscribeRequest.setInitialTerminationTime(
                  new JAXBElement<String>(QNAME_INITIAL_TERMINATION_TIME,
                  String.class, initialTerminationTime));
        }
        subscribeRequest.setConsumerReference(consumer.getEpr());
        subscribeRequest.setFilter(new FilterType());
        if (topic != null) {
            TopicExpressionType topicExp = new TopicExpressionType();
            topicExp.getContent().add(topic);
            subscribeRequest.getFilter().getAny().add(
                    new JAXBElement<TopicExpressionType>(QNAME_TOPIC_EXPRESSION,
                            TopicExpressionType.class, topicExp));
        }
        if (xpath != null) {
            QueryExpressionType xpathExp = new QueryExpressionType();
            xpathExp.setDialect(XPATH1_URI);
            xpathExp.getContent().add(xpath);
            subscribeRequest.getFilter().getAny().add(
                    new JAXBElement<QueryExpressionType>(QNAME_MESSAGE_CONTENT,
                            QueryExpressionType.class, xpathExp));
        }
        if (raw) {
            subscribeRequest.setSubscriptionPolicy(new Subscribe.SubscriptionPolicy());
            subscribeRequest.getSubscriptionPolicy().getAny().add(new UseRaw());
        }
        SubscribeResponse response = getBroker().subscribe(subscribeRequest);
        return new Subscription(response.getSubscriptionReference());
    }

    public List<Object> getCurrentMessage(String topic)
        //CHECKSTYLE:OFF - WS-Notification spec throws a lot of faults
        throws TopicNotSupportedFault, TopicExpressionDialectUnknownFault, MultipleTopicsSpecifiedFault,
        InvalidTopicExpressionFault, ResourceUnknownFault, NoCurrentMessageOnTopicFault {
        //CHECKSTYLE:ON
        GetCurrentMessage getCurrentMessageRequest = new GetCurrentMessage();
        if (topic != null) {
            TopicExpressionType topicExp = new TopicExpressionType();
            topicExp.getContent().add(topic);
            getCurrentMessageRequest.setTopic(topicExp);
        }
        GetCurrentMessageResponse response = getBroker().getCurrentMessage(getCurrentMessageRequest);
        return response.getAny();
    }

    public Registration registerPublisher(Referencable publisher, String topic)
        //CHECKSTYLE:OFF - WS-Notification spec throws a lot of faults
        throws TopicNotSupportedFault, PublisherRegistrationFailedFault,
        UnacceptableInitialTerminationTimeFault, InvalidTopicExpressionFault,
        ResourceUnknownFault, PublisherRegistrationRejectedFault {
        //CHECKSTYLE:ON
        return registerPublisher(publisher, topic, false);
    }

    public Registration registerPublisher(Referencable publisher, String topic, boolean demand)
        //CHECKSTYLE:OFF - WS-Notification spec throws a lot of faults
        throws TopicNotSupportedFault, PublisherRegistrationFailedFault,
        UnacceptableInitialTerminationTimeFault, InvalidTopicExpressionFault, ResourceUnknownFault,
        PublisherRegistrationRejectedFault {
        //CHECKSTYLE:ON
        return registerPublisher(publisher, Collections.singletonList(topic), demand);
    }

    public Registration registerPublisher(Referencable publisher, List<String> topics, boolean demand)
        //CHECKSTYLE:OFF - WS-Notification spec throws a lot of faults
        throws TopicNotSupportedFault, PublisherRegistrationFailedFault,
        UnacceptableInitialTerminationTimeFault, InvalidTopicExpressionFault, ResourceUnknownFault,
        PublisherRegistrationRejectedFault {
        //CHECKSTYLE:ON

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
        RegisterPublisherResponse response = getBroker().registerPublisher(registerPublisherRequest);
        return new Registration(response.getPublisherRegistrationReference());
    }

}
