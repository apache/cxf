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

import javax.transaction.xa.XAException;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.transaction.TransactionManager;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQXAConnectionFactory;
import org.apache.activemq.artemis.junit.EmbeddedActiveMQResource;
import org.awaitility.Awaitility;
import org.jboss.narayana.jta.jms.ConnectionFactoryProxy;
import org.jboss.narayana.jta.jms.TransactionHelperImpl;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class MessageListenerTest {
    @Rule public EmbeddedActiveMQResource server = new EmbeddedActiveMQResource(getConfiguration());

    enum TestMessage {
        OK, FAILFIRST, FAIL;
    }

    @Test
    public void testConnectionProblem() throws JMSException {
        Connection connection = createConnection("broker");
        Queue dest = JMSUtil.createQueue(connection, "test");

        MessageListener listenerHandler = new TestMessageListener();
        TestExceptionListener exListener = new TestExceptionListener();

        PollingMessageListenerContainer container = //
            new PollingMessageListenerContainer(connection, dest, listenerHandler, exListener);
        connection.close(); // Simulate connection problem
        container.start();
        Awaitility.await().until(() -> exListener.exception != null);
        JMSException ex = exListener.exception;
        assertNotNull(ex);
        assertEquals("Connection is closed", ex.getMessage());
    }
    
    @Test
    public void testConnectionProblemXA() throws JMSException, XAException, InterruptedException {
        TransactionManager transactionManager = com.arjuna.ats.jta.TransactionManager.transactionManager();
        Connection connection = createXAConnection("brokerJTA", transactionManager);
        Queue dest = JMSUtil.createQueue(connection, "test");

        MessageListener listenerHandler = new TestMessageListener();
        TestExceptionListener exListener = new TestExceptionListener();

        PollingMessageListenerContainer container = //
            new PollingMessageListenerContainer(connection, dest, listenerHandler, exListener);
        container.setTransacted(false);
        container.setAcknowledgeMode(Session.SESSION_TRANSACTED);
        container.setTransactionManager(transactionManager);

        connection.close(); // Simulate connection problem
        container.start();
        Awaitility.await().until(() -> exListener.exception != null);
        JMSException ex = exListener.exception;
        assertNotNull(ex);
        // Closing the pooled connection will result in a NPE when using it
        assertThat(ex.getMessage(), containsString("Wrapped exception.")); 
        assertThat(ex.getMessage(), containsString("null"));
    }

    @Test
    public void testWithJTA() throws JMSException, XAException, InterruptedException {
        TransactionManager transactionManager = com.arjuna.ats.jta.TransactionManager.transactionManager();
        Connection connection = createXAConnection("brokerJTA", transactionManager);
        Queue dest = JMSUtil.createQueue(connection, "test");

        MessageListener listenerHandler = new TestMessageListener();
        ExceptionListener exListener = new TestExceptionListener();
        PollingMessageListenerContainer container = new PollingMessageListenerContainer(connection, dest,
                                                                                        listenerHandler, exListener);
        container.setTransacted(false);
        container.setAcknowledgeMode(Session.SESSION_TRANSACTED);
        container.setTransactionManager(transactionManager);
        container.start();

        testTransactionalBehaviour(connection, dest);

        container.stop();
        connection.close();
    }

    @Test
    public void testNoTransaction() throws JMSException, XAException, InterruptedException {
        Connection connection = createConnection("brokerNoTransaction");
        Queue dest = JMSUtil.createQueue(connection, "test");

        MessageListener listenerHandler = new TestMessageListener();
        AbstractMessageListenerContainer container = new MessageListenerContainer(connection, dest,
                                                                                        listenerHandler);
        container.setTransacted(false);
        container.setAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
        container.start();

        assertNumMessagesInQueue("At the start the queue should be empty", connection, dest, 0, 0L);

        sendMessage(connection, dest, TestMessage.OK);
        assertNumMessagesInQueue("This message should be committed", connection, dest, 0, 3500L);

        sendMessage(connection, dest, TestMessage.FAIL);
        assertNumMessagesInQueue("Even when an exception occurs the message should be committed", connection,
                                 dest, 0, 3500L);

        container.stop();
        connection.close();
    }

    @Test
    public void testLocalTransaction() throws JMSException, XAException, InterruptedException {
        Connection connection = createConnection("brokerLocalTransaction");
        Queue dest = JMSUtil.createQueue(connection, "test");
        MessageListener listenerHandler = new TestMessageListener();
        AbstractMessageListenerContainer container = new MessageListenerContainer(connection, dest, listenerHandler);
        container.setTransacted(true);
        container.setAcknowledgeMode(Session.SESSION_TRANSACTED);
        container.start();

        testTransactionalBehaviour(connection, dest);
        container.stop();
        connection.close();
    }

    private void testTransactionalBehaviour(Connection connection, Queue dest) throws JMSException,
        InterruptedException {
        Queue dlq = JMSUtil.createQueue(connection, "ActiveMQ.DLQ");
        assertNumMessagesInQueue("At the start the queue should be empty", connection, dest, 0, 0L);
        assertNumMessagesInQueue("At the start the DLQ should be empty", connection, dlq, 0, 0L);

        sendMessage(connection, dest, TestMessage.OK);
        assertNumMessagesInQueue("This message should be committed", connection, dest, 0, 3500L);

        sendMessage(connection, dest, TestMessage.FAILFIRST);
        assertNumMessagesInQueue("Should succeed on second try", connection, dest, 0, 3500L);

        sendMessage(connection, dest, TestMessage.FAIL);
        assertNumMessagesInQueue("Should be rolled back", connection, dlq, 1, 3500L);
    }

    private static Connection createConnection(String name) throws JMSException {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://" + name);
        Connection connection = cf.createConnection();
        connection.start();
        return connection;
    }

    private static Connection createXAConnection(String name, TransactionManager tm) throws JMSException {
        ActiveMQXAConnectionFactory cf = new ActiveMQXAConnectionFactory("vm://" + name);
        ConnectionFactory cf1 = new ConnectionFactoryProxy(cf, new TransactionHelperImpl(tm));
        Connection connection = cf1.createConnection();
        connection.start();
        
        return connection;
    }

    private static void assertNumMessagesInQueue(String message, Connection connection, Queue queue,
                                          int expectedNum, long timeout) throws JMSException, InterruptedException {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        QueueBrowser browser = session.createBrowser(queue);
        int actualNum = 0;
        for (long startTime = System.currentTimeMillis(); System.currentTimeMillis() - startTime < timeout;
            Thread.sleep(100L)) {
            actualNum = 0;
            for (Enumeration<?> messages = browser.getEnumeration(); messages.hasMoreElements(); actualNum++) {
                messages.nextElement();
            }
            if (actualNum == expectedNum) {
                break;
            }
            //System.out.println("Messages in queue " + queue.getQueueName() + ": " + actualNum
            //                   + ", expecting: " + expectedNum);
        }
        browser.close();
        session.close();
        assertEquals(message + " -> number of messages on queue", expectedNum, actualNum);
    }

    private static void sendMessage(Connection connection, Destination dest, TestMessage content) throws JMSException {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer prod = session.createProducer(dest);
        Message message = session.createTextMessage(content.toString());
        prod.send(message);
        prod.close();
        session.close();
//        Thread.sleep(500L); // Give receiver some time to process
    }

    private static final class TestMessageListener implements MessageListener {
        @Override
        public void onMessage(Message message) {
            TextMessage textMessage = (TextMessage)message;
            try {
                switch (TestMessage.valueOf(textMessage.getText())) {
                case OK:
                    //System.out.println("Simulating Processing successful");
                    break;
                case FAILFIRST:
                    if (message.getJMSRedelivered()) {
                        //System.out.println("Simulating processing worked on second try");
                        break;
                    }
                    throw new RuntimeException("Simulating something went wrong. Expecting rollback");
                case FAIL:
                    throw new RuntimeException("Simulating something went wrong. Expecting rollback");
                default:
                    throw new IllegalArgumentException("Invalid message type");
                }
            } catch (JMSException e) {
                // Ignore
            }
        }
    }

    private static final class TestExceptionListener implements ExceptionListener {
        JMSException exception;
        @Override
        public void onException(JMSException ex) {
            exception = ex;
        }
    };
    
    private static Configuration getConfiguration() {
        try {
            return new ConfigurationImpl()
                .setSecurityEnabled(false)
                .setPersistenceEnabled(false)
                .setAddressQueueScanPeriod(1)
                .addAcceptorConfiguration("#", "vm://0")
                .addAddressesSetting("#",
                    new AddressSettings()
                        .setMaxDeliveryAttempts(1)
                        .setRedeliveryDelay(500L)
                        .setDeadLetterAddress(SimpleString.toSimpleString("ActiveMQ.DLQ")));
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
