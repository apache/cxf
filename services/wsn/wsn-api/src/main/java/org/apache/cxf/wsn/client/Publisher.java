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

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.apache.cxf.wsn.util.IdGenerator;
import org.oasis_open.docs.wsn.b_2.GetCurrentMessage;
import org.oasis_open.docs.wsn.b_2.GetCurrentMessageResponse;
import org.oasis_open.docs.wsn.b_2.InvalidFilterFaultType;
import org.oasis_open.docs.wsn.b_2.InvalidTopicExpressionFaultType;
import org.oasis_open.docs.wsn.b_2.NoCurrentMessageOnTopicFaultType;
import org.oasis_open.docs.wsn.b_2.Renew;
import org.oasis_open.docs.wsn.b_2.RenewResponse;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.b_2.SubscribeResponse;
import org.oasis_open.docs.wsn.b_2.TopicExpressionType;
import org.oasis_open.docs.wsn.b_2.Unsubscribe;
import org.oasis_open.docs.wsn.b_2.UnsubscribeResponse;
import org.oasis_open.docs.wsn.bw_2.InvalidFilterFault;
import org.oasis_open.docs.wsn.bw_2.InvalidMessageContentExpressionFault;
import org.oasis_open.docs.wsn.bw_2.InvalidProducerPropertiesExpressionFault;
import org.oasis_open.docs.wsn.bw_2.InvalidTopicExpressionFault;
import org.oasis_open.docs.wsn.bw_2.MultipleTopicsSpecifiedFault;
import org.oasis_open.docs.wsn.bw_2.NoCurrentMessageOnTopicFault;
import org.oasis_open.docs.wsn.bw_2.NotificationProducer;
import org.oasis_open.docs.wsn.bw_2.NotifyMessageNotSupportedFault;
import org.oasis_open.docs.wsn.bw_2.SubscribeCreationFailedFault;
import org.oasis_open.docs.wsn.bw_2.SubscriptionManager;
import org.oasis_open.docs.wsn.bw_2.TopicExpressionDialectUnknownFault;
import org.oasis_open.docs.wsn.bw_2.TopicNotSupportedFault;
import org.oasis_open.docs.wsn.bw_2.UnableToDestroySubscriptionFault;
import org.oasis_open.docs.wsn.bw_2.UnacceptableInitialTerminationTimeFault;
import org.oasis_open.docs.wsn.bw_2.UnacceptableTerminationTimeFault;
import org.oasis_open.docs.wsn.bw_2.UnrecognizedPolicyRequestFault;
import org.oasis_open.docs.wsn.bw_2.UnsupportedPolicyRequestFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

/**
 * Demand-based publisher.
 *
 */
@WebService(endpointInterface = "org.oasis_open.docs.wsn.bw_2.NotificationProducer")
public class Publisher implements NotificationProducer, Referencable {
    public static final String WSN_URI = "http://docs.oasis-open.org/wsn/b-2";
    public static final QName QNAME_TOPIC_EXPRESSION = new QName(WSN_URI, "TopicExpression");

    public interface Callback {
        void subscribe(TopicExpressionType topic);
        void unsubscribe(TopicExpressionType topic);
    }

    private final Callback callback;
    private final String address;
    private final Endpoint endpoint;
    private final IdGenerator idGenerator = new IdGenerator();

    public Publisher(Callback callback, String address) {
        this.callback = callback;
        this.address = address;
        if (callback == null || address == null) {
            this.endpoint = null;
        } else {
            this.endpoint = Endpoint.create(this);
            this.endpoint.publish(address);
        }
    }

    public void stop() {
        if (endpoint != null) {
            this.endpoint.stop();
        }
    }

    public W3CEndpointReference getEpr() {
        if (this.endpoint == null) {
            return null;
        }
        return this.endpoint.getEndpointReference(W3CEndpointReference.class);
    }

    public SubscribeResponse subscribe(
        @WebParam(partName = "SubscribeRequest", name = "Subscribe",
                  targetNamespace = "http://docs.oasis-open.org/wsn/b-2") Subscribe subscribeRequest)
        //CHECKSTYLE:OFF - WS-Notification spec throws a lot of faults
        throws InvalidTopicExpressionFault, ResourceUnknownFault, InvalidProducerPropertiesExpressionFault,
            UnrecognizedPolicyRequestFault, TopicExpressionDialectUnknownFault, NotifyMessageNotSupportedFault,
            InvalidFilterFault, UnsupportedPolicyRequestFault, InvalidMessageContentExpressionFault,
            SubscribeCreationFailedFault, TopicNotSupportedFault, UnacceptableInitialTerminationTimeFault {
        //CHECKSYTLE:ON
        
        TopicExpressionType topic = null;
        if (subscribeRequest.getFilter() != null) {
            for (Object f : subscribeRequest.getFilter().getAny()) {
                JAXBElement<?> e = null;
                if (f instanceof JAXBElement) {
                    e = (JAXBElement<?>) f;
                    f = e.getValue();
                }
                if (f instanceof TopicExpressionType) {
                    if (!e.getName().equals(QNAME_TOPIC_EXPRESSION)) {
                        InvalidTopicExpressionFaultType fault = new InvalidTopicExpressionFaultType();
                        throw new InvalidTopicExpressionFault("Unrecognized TopicExpression: " + e, fault);
                    }
                    topic = (TopicExpressionType) f;
                }
            }
        }
        if (topic == null) {
            InvalidFilterFaultType fault = new InvalidFilterFaultType();
            throw new InvalidFilterFault("Must specify a topic to subscribe on", fault);
        }
        PublisherSubscription pub = new PublisherSubscription(topic);
        SubscribeResponse response = new SubscribeResponse();
        response.setSubscriptionReference(pub.getEpr());
        callback.subscribe(topic);
        return response;
    }

    protected void unsubscribe(TopicExpressionType topic) {
        callback.unsubscribe(topic);
    }

    public GetCurrentMessageResponse getCurrentMessage(
            @WebParam(partName = "GetCurrentMessageRequest", name = "GetCurrentMessage", 
                      targetNamespace = "http://docs.oasis-open.org/wsn/b-2") 
                GetCurrentMessage getCurrentMessageRequest) 
        //CHECKSTYLE:OFF - WS-Notification spec throws a lot of faults
        throws InvalidTopicExpressionFault, ResourceUnknownFault, TopicExpressionDialectUnknownFault, 
            MultipleTopicsSpecifiedFault, NoCurrentMessageOnTopicFault, TopicNotSupportedFault {
        //CHECKSTYLE:ON
        
        NoCurrentMessageOnTopicFaultType fault = new NoCurrentMessageOnTopicFaultType();
        throw new NoCurrentMessageOnTopicFault("There is no current message on this topic.", fault);
    }

    @WebService(endpointInterface = "org.oasis_open.docs.wsn.bw_2.SubscriptionManager")
    protected class PublisherSubscription implements SubscriptionManager {

        private final String id;
        private final TopicExpressionType topic;
        private final Endpoint endpoint;

        public PublisherSubscription(TopicExpressionType topic) {
            this.topic = topic;
            this.id = idGenerator.generateSanitizedId();
            this.endpoint = Endpoint.create(this);
            this.endpoint.publish(address + "/subscriptions/" + this.id);
        }

        public W3CEndpointReference getEpr() {
            return endpoint.getEndpointReference(W3CEndpointReference.class);
        }

        public UnsubscribeResponse unsubscribe(
                @WebParam(partName = "UnsubscribeRequest", 
                          name = "Unsubscribe", 
                          targetNamespace = "http://docs.oasis-open.org/wsn/b-2")
                    Unsubscribe unsubscribeRequest)
            throws ResourceUnknownFault, UnableToDestroySubscriptionFault {
            Publisher.this.unsubscribe(topic);
            return new UnsubscribeResponse();
        }

        public RenewResponse renew(
                @WebParam(partName = "RenewRequest", name = "Renew",
                          targetNamespace = "http://docs.oasis-open.org/wsn/b-2") Renew renewRequest) 
            throws ResourceUnknownFault, UnacceptableTerminationTimeFault {
            throw new UnsupportedOperationException();
        }
    }

}
