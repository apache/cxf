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
package org.apache.cxf.wsn.jms;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.activemq.advisory.ConsumerEvent;
import org.apache.activemq.advisory.ConsumerEventSource;
import org.apache.activemq.advisory.ConsumerListener;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.wsn.AbstractPublisher;
import org.oasis_open.docs.wsn.b_2.InvalidTopicExpressionFaultType;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.oasis_open.docs.wsn.b_2.TopicExpressionType;
import org.oasis_open.docs.wsn.br_2.PublisherRegistrationFailedFaultType;
import org.oasis_open.docs.wsn.br_2.RegisterPublisher;
import org.oasis_open.docs.wsn.br_2.ResourceNotDestroyedFaultType;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationFailedFault;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationRejectedFault;
import org.oasis_open.docs.wsn.brw_2.ResourceNotDestroyedFault;
import org.oasis_open.docs.wsn.bw_2.InvalidTopicExpressionFault;
import org.oasis_open.docs.wsn.bw_2.TopicNotSupportedFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

public abstract class JmsPublisher extends AbstractPublisher implements ConsumerListener {

    private static final Logger LOGGER = LogUtils.getL7dLogger(JmsPublisher.class);

    private Connection connection;

    private JmsTopicExpressionConverter topicConverter;

    private JAXBContext jaxbContext;

    private List<ConsumerEventSource> advisories;

    private Map<Destination, Object> producers;

    public JmsPublisher(String name) {
        super(name);
        topicConverter = new JmsTopicExpressionConverter();
        try {
            jaxbContext = JAXBContext.newInstance(Notify.class);
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to create JAXB context", e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void notify(NotificationMessageHolderType messageHolder) {
        Session session = null;
        try {
            Topic topic = topicConverter.toActiveMQTopic(messageHolder.getTopic());
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(topic);
            Notify notify = new Notify();
            notify.getNotificationMessage().add(messageHolder);
            StringWriter writer = new StringWriter();
            jaxbContext.createMarshaller().marshal(notify, writer);
            Message message = session.createTextMessage(writer.toString());
            producer.send(message);
        } catch (JMSException | JAXBException | InvalidTopicException e) {
            LOGGER.log(Level.WARNING, "Error dispatching message", e);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (JMSException e) {
                    LOGGER.log(Level.FINE, "Error closing session", e);
                }
            }
        }
    }

    @Override
    protected void validatePublisher(RegisterPublisher registerPublisherRequest)
        throws InvalidTopicExpressionFault, PublisherRegistrationFailedFault,
            PublisherRegistrationRejectedFault, ResourceUnknownFault,
            TopicNotSupportedFault {
        super.validatePublisher(registerPublisherRequest);
        try {
            topicConverter.toActiveMQTopic(topic);
        } catch (InvalidTopicException e) {
            InvalidTopicExpressionFaultType fault = new InvalidTopicExpressionFaultType();
            throw new InvalidTopicExpressionFault(e.getMessage(), fault);
        }
    }

    @Override
    protected void start() throws PublisherRegistrationFailedFault {
        if (demand) {
            try {
                producers = new HashMap<>();
                advisories = new ArrayList<>();
                for (TopicExpressionType topic : this.topic) {
                    ConsumerEventSource advisory
                        = new ConsumerEventSource(connection, topicConverter.toActiveMQTopic(topic));
                    advisory.setConsumerListener(this);
                    advisory.start();
                    advisories.add(advisory);
                }
            } catch (Exception e) {
                PublisherRegistrationFailedFaultType fault = new PublisherRegistrationFailedFaultType();
                throw new PublisherRegistrationFailedFault("Error starting demand-based publisher", fault, e);
            }
        }
    }

    protected void destroy() throws ResourceNotDestroyedFault {
        try {
            if (advisories != null) {
                for (ConsumerEventSource advisory : advisories) {
                    advisory.stop();
                }
            }
        } catch (Exception e) {
            ResourceNotDestroyedFaultType fault = new ResourceNotDestroyedFaultType();
            throw new ResourceNotDestroyedFault("Error destroying publisher", fault, e);
        } finally {
            super.destroy();
        }
    }

    public synchronized void onConsumerEvent(ConsumerEvent event) {
        Object producer = producers.get(event.getDestination());
        if (event.getConsumerCount() > 0) {
            if (producer == null) {
                // start subscription
                producer = startSubscription(topicConverter.toTopicExpression((Topic)event.getDestination()));
                producers.put(event.getDestination(), producer);
            }
        } else {
            if (producer != null) {
                Object sub = producers.remove(event.getDestination());
                // destroy subscription
                stopSubscription(sub);
            }
        }
    }

    protected abstract Object startSubscription(TopicExpressionType topic);

    protected abstract void stopSubscription(Object sub);

}
