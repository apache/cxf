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
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.wsn.AbstractPublisher;
import org.oasis_open.docs.wsn.b_2.InvalidTopicExpressionFaultType;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.oasis_open.docs.wsn.b_2.TopicExpressionType;
import org.oasis_open.docs.wsn.br_2.RegisterPublisher;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationFailedFault;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationRejectedFault;
import org.oasis_open.docs.wsn.bw_2.InvalidTopicExpressionFault;
import org.oasis_open.docs.wsn.bw_2.TopicNotSupportedFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

public abstract class JmsPublisher extends AbstractPublisher {

    private static final Logger LOGGER = LogUtils.getL7dLogger(JmsPublisher.class);

    private Connection connection;

    private JmsTopicExpressionConverter topicConverter;

    private JAXBContext jaxbContext;

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
            if (topic != null && !topic.isEmpty()) {
                int size = topic.size();
                ActiveMQTopic[] childrenDestinations = new ActiveMQTopic[size];
                for (int i = 0; i < size; i++) {
                    childrenDestinations[i] = topicConverter.toActiveMQTopic(topic.get(i));
                }
            }
        } catch (InvalidTopicException e) {
            InvalidTopicExpressionFaultType fault = new InvalidTopicExpressionFaultType();
            throw new InvalidTopicExpressionFault(e.getMessage(), fault);
        }
    }

    @Override
    protected void start() throws PublisherRegistrationFailedFault {
    }

    protected abstract Object startSubscription(TopicExpressionType topic);

    protected abstract void stopSubscription(Object sub);

}
