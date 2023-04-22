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
package org.apache.cxf.transport.jms;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TextMessage;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.junit.EmbeddedActiveMQResource;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class ConnectionTempQueueTest {
    protected static final String SERVICE_QUEUE = "queue1";

    @Rule public EmbeddedActiveMQResource server = new EmbeddedActiveMQResource(getConfiguration());

    @Test
    public void testTempQueueIssue() throws JMSException, InterruptedException {
        final ConnectionFactory cf = new ActiveMQConnectionFactory("vm://0");

        try (Connection con = cf.createConnection()) {
            con.start();
    
            new Thread(() -> {
                try {
                    receiveAndRespondWithMessageIdAsCorrelationId(con, SERVICE_QUEUE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
    
            sendWithReplyToTemp(con, SERVICE_QUEUE);
        }
    }

    private static void sendWithReplyToTemp(Connection con, String serviceQueue) throws JMSException,
        InterruptedException {
        Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
        TemporaryQueue tempQueue = session.createTemporaryQueue();
        TextMessage msg = session.createTextMessage("Request");
        msg.setJMSReplyTo(tempQueue);
        MessageProducer producer = session.createProducer(session.createQueue(serviceQueue));
        producer.send(msg);

        // This sleep also seems to matter
        Thread.sleep(500L);

        MessageConsumer consumer = session.createConsumer(tempQueue);
        Message replyMsg = consumer.receive(5000);
        assertThat(replyMsg, is(not(nullValue())));

        consumer.close();

        producer.close();
        session.close();
    }

    public static void receiveAndRespondWithMessageIdAsCorrelationId(Connection con,
                                                              String queueName) throws JMSException {
        Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageConsumer consumer = session.createConsumer(session.createQueue(queueName));
        final jakarta.jms.Message inMessage = consumer.receive(5000);
        assertThat(inMessage, is(not(nullValue())));

        final TextMessage replyMessage = session.createTextMessage("Result");
        replyMessage.setJMSCorrelationID(inMessage.getJMSMessageID());
        final MessageProducer producer = session.createProducer(inMessage.getJMSReplyTo());
        producer.send(replyMessage);

        producer.close();
        consumer.close();
        session.close();
    }

    private static Configuration getConfiguration() {
        try {
            return new ConfigurationImpl()
                .setSecurityEnabled(false)
                .setPersistenceEnabled(false)
                .addAcceptorConfiguration("vm", "vm://0");
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
