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
package org.apache.cxf.transport.jms.util;

import java.util.Enumeration;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.aries.transaction.internal.AriesTransactionManagerImpl;
import org.junit.Assert;
import org.junit.Test;

public class MessageListenerTest {

    private static final String FAIL = "fail";
    private static final String FAILFIRST = "failfirst";
    private static final String OK = "ok";

    @Test
    public void testWithJTA() throws JMSException, XAException, InterruptedException {
        Connection connection = createXAConnection("brokerJTA");
        Queue dest = createQueue(connection, "test");

        MessageListener listenerHandler = new TestMessageListener();
        MessageListenerContainer container = new MessageListenerContainer(connection, dest, listenerHandler);
        container.setTransacted(false);
        container.setAcknowledgeMode(Session.SESSION_TRANSACTED);
        TransactionManager transactionManager = new AriesTransactionManagerImpl();
        container.setTransactionManager(transactionManager);
        container.start();

        testTransactionalBehaviour(connection, dest);

        container.stop();
        connection.close();
    }

    @Test
    public void testNoTransaction() throws JMSException, XAException, InterruptedException {
        Connection connection = createConnection("brokerNoTransaction");
        Queue dest = createQueue(connection, "test");

        MessageListener listenerHandler = new TestMessageListener();
        MessageListenerContainer container = new MessageListenerContainer(connection, dest, listenerHandler);
        container.setTransacted(false);
        container.setAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
        container.start();

        assertNumMessagesInQueue("At the start the queue should be empty", connection, dest, 0, 0);

        sendMessage(connection, dest, OK);
        assertNumMessagesInQueue("This message should be committed", connection, dest, 0, 1000);

        sendMessage(connection, dest, FAIL);
        assertNumMessagesInQueue("Even when an exception occurs the message should be committed", connection,
                                 dest, 0, 1000);

        container.stop();
        connection.close();
    }

    @Test
    public void testLocalTransaction() throws JMSException, XAException, InterruptedException {
        Connection connection = createConnection("brokerLocalTransaction");
        Queue dest = createQueue(connection, "test");
        MessageListener listenerHandler = new TestMessageListener();
        MessageListenerContainer container = new MessageListenerContainer(connection, dest, listenerHandler);
        container.setTransacted(true);
        container.setAcknowledgeMode(Session.SESSION_TRANSACTED);
        container.start();

        testTransactionalBehaviour(connection, dest);
        container.stop();
        connection.close();
    }

    private void testTransactionalBehaviour(Connection connection, Queue dest) throws JMSException,
        InterruptedException {
        assertNumMessagesInQueue("At the start the queue should be empty", connection, dest, 0, 0);

        sendMessage(connection, dest, OK);
        assertNumMessagesInQueue("This message should be committed", connection, dest, 0, 1000);

        sendMessage(connection, dest, FAILFIRST);
        assertNumMessagesInQueue("Should be rolled back on first try", connection, dest, 1, 800);
        assertNumMessagesInQueue("Should succeed on second try", connection, dest, 0, 2000);

        sendMessage(connection, dest, "Fail");
        assertNumMessagesInQueue("Should be rolled back", connection, dest, 1, 1000);
    }

    private Connection createConnection(String name) throws JMSException {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://" + name
                                                                     + "?broker.persistent=false");
        cf.setRedeliveryPolicy(redeliveryPolicy());
        Connection connection = cf.createConnection();
        connection.start();
        return connection;
    }

    private Connection createXAConnection(String name) throws JMSException {
        ActiveMQXAConnectionFactory cf = new ActiveMQXAConnectionFactory("vm://" + name
                                                                         + "?broker.persistent=false");
        cf.setRedeliveryPolicy(redeliveryPolicy());
        Connection connection = cf.createXAConnection();
        connection.start();
        return connection;
    }

    private RedeliveryPolicy redeliveryPolicy() {
        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setRedeliveryDelay(1000);
        redeliveryPolicy.setMaximumRedeliveries(3);
        redeliveryPolicy.setUseExponentialBackOff(false);
        return redeliveryPolicy;
    }

    protected void drainQueue(Connection connection, Queue dest) throws JMSException, InterruptedException {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageConsumer consumer = session.createConsumer(dest);
        while (consumer.receiveNoWait() != null) {
            System.out.println("Consuming old message");
        }
        consumer.close();
        session.close();
        assertNumMessagesInQueue("", connection, dest, 0, 0);
    }

    private void assertNumMessagesInQueue(String message, Connection connection, Queue queue,
                                          int expectedNum, int timeout) throws JMSException,
        InterruptedException {
        long startTime = System.currentTimeMillis();
        int actualNum;
        do {
            actualNum = getNumMessages(connection, queue);
            System.out.println("Messages in queue: " + actualNum + ", expecting: " + expectedNum);
            Thread.sleep(100);
        } while ((System.currentTimeMillis() - startTime < timeout) && expectedNum != actualNum);
        Assert.assertEquals(message + " -> number of messages", expectedNum, actualNum);
    }

    private int getNumMessages(Connection connection, Queue queue) throws JMSException {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        QueueBrowser browser = session.createBrowser(queue);
        @SuppressWarnings("unchecked")
        Enumeration<Message> messages = browser.getEnumeration();
        int actualNum = 0;
        while (messages.hasMoreElements()) {
            actualNum++;
            messages.nextElement();
        }
        browser.close();
        session.close();
        return actualNum;
    }

    private void sendMessage(Connection connection, Destination dest, String content) throws JMSException,
        InterruptedException {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer prod = session.createProducer(dest);
        Message message = session.createTextMessage(content);
        prod.send(message);
        prod.close();
        session.close();
        Thread.sleep(500); // Give receiver some time to process
    }

    private Queue createQueue(Connection connection, String name) throws JMSException {
        Session session = null;
        try {
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            return session.createQueue(name);
        } finally {
            session.close();
        }
    }

    private static final class TestMessageListener implements MessageListener {
        @Override
        public void onMessage(Message message) {
            TextMessage textMessage = (TextMessage)message;
            try {
                String text = textMessage.getText();
                if (OK.equals(text)) {
                    System.out.println("Simulating Processing successful");
                } else if (FAIL.equals(text)) {
                    throw new RuntimeException("Simulating something went wrong. Expecting rollback");
                } else if (FAILFIRST.equals(text)) {
                    if (message.getJMSRedelivered()) {
                        System.out.println("Simulating processing worked on second try");
                    } else {
                        throw new RuntimeException("Simulating something went wrong. Expecting rollback");
                    }
                } else {
                    throw new IllegalArgumentException("Invalid message type");
                }
            } catch (JMSException e) {
                // Ignore
            }
        }
    }
}
