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

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.pool.XaPooledConnectionFactory;
import org.apache.geronimo.transaction.manager.GeronimoTransactionManager;
import org.awaitility.Awaitility;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MessageListenerTest {

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
        Awaitility.await().until(() -> !container.isRunning());
        JMSException ex = exListener.exception;
        assertNotNull(ex);
        assertEquals("The connection is already closed", ex.getMessage());
    }
    
    @Test
    public void testConnectionProblemXA() throws JMSException, XAException, InterruptedException {
        TransactionManager transactionManager = new GeronimoTransactionManager();
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
        Awaitility.await().until(() -> !container.isRunning());
        JMSException ex = exListener.exception;
        assertNotNull(ex);
        // Closing the pooled connection will result in a NPE when using it
        assertEquals("Wrapped exception. null", ex.getMessage());
    }

    @Test
    public void testWithJTA() throws JMSException, XAException, InterruptedException {
        TransactionManager transactionManager = new GeronimoTransactionManager();
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
        assertNumMessagesInQueue("This message should be committed", connection, dest, 0, 1000L);

        sendMessage(connection, dest, TestMessage.FAIL);
        assertNumMessagesInQueue("Even when an exception occurs the message should be committed", connection,
                                 dest, 0, 1000L);

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
        assertNumMessagesInQueue("This message should be committed", connection, dest, 0, 1000L);

        sendMessage(connection, dest, TestMessage.FAILFIRST);
        assertNumMessagesInQueue("Should succeed on second try", connection, dest, 0, 2000L);

        sendMessage(connection, dest, TestMessage.FAIL);
        assertNumMessagesInQueue("Should be rolled back", connection, dlq, 1, 2500L);
    }

    private static Connection createConnection(String name) throws JMSException {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://" + name
                                                                     + "?broker.persistent=false");
        cf.setRedeliveryPolicy(redeliveryPolicy());
        Connection connection = cf.createConnection();
        connection.start();
        return connection;
    }

    private static Connection createXAConnection(String name, TransactionManager tm) throws JMSException {
        ActiveMQXAConnectionFactory cf = new ActiveMQXAConnectionFactory("vm://" + name
                                                                         + "?broker.persistent=false");
        cf.setRedeliveryPolicy(redeliveryPolicy());
        XaPooledConnectionFactory cfp = new XaPooledConnectionFactory(cf);
        cfp.setTransactionManager(tm);
        cfp.setConnectionFactory(cf);
        Connection connection = cfp.createConnection();
        connection.start();
        return connection;
    }

    private static RedeliveryPolicy redeliveryPolicy() {
        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setRedeliveryDelay(500L);
        redeliveryPolicy.setMaximumRedeliveries(1);
        return redeliveryPolicy;
    }

    private static void assertNumMessagesInQueue(String message, Connection connection, Queue queue,
                                          int expectedNum, long timeout) throws JMSException,
        InterruptedException {
        long startTime = System.currentTimeMillis();
        int actualNum;
        do {
            actualNum = JMSUtil.getNumMessages(connection, queue);
            if (actualNum == expectedNum) {
                break;
            }
            //System.out.println("Messages in queue " + queue.getQueueName() + ": " + actualNum
            //                   + ", expecting: " + expectedNum);
            Thread.sleep(100L);
        } while ((System.currentTimeMillis() - startTime < timeout) && expectedNum != actualNum);
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
}
