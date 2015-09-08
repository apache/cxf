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
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
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
import org.junit.Assert;
import org.junit.Test;

public class MessageListenerTest {

    private static final String FAIL = "fail";
    private static final String FAILFIRST = "failfirst";
    private static final String OK = "ok";

    @Test
    public void testWithJTA() throws JMSException, XAException, InterruptedException {
        TransactionManager transactionManager = new GeronimoTransactionManager();
        Connection connection = createXAConnection("brokerJTA", transactionManager);
        Queue dest = JMSUtil.createQueue(connection, "test");

        MessageListener listenerHandler = new TestMessageListener();
        PollingMessageListenerContainer container = new PollingMessageListenerContainer(connection, dest,
                                                                                        listenerHandler);
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
        assertNumMessagesInQueue("At the start the queue should be empty", connection, dest, 0, 0);
        assertNumMessagesInQueue("At the start the DLQ should be empty", connection, dlq, 0, 0);

        sendMessage(connection, dest, OK);
        assertNumMessagesInQueue("This message should be committed", connection, dest, 0, 1000);

        sendMessage(connection, dest, FAILFIRST);
        assertNumMessagesInQueue("Should succeed on second try", connection, dest, 0, 2000);

        sendMessage(connection, dest, FAIL);
        assertNumMessagesInQueue("Should be rolled back", connection, dlq, 1, 2500);
    }

    private Connection createConnection(String name) throws JMSException {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://" + name
                                                                     + "?broker.persistent=false");
        cf.setRedeliveryPolicy(redeliveryPolicy());
        Connection connection = cf.createConnection();
        connection.start();
        return connection;
    }

    private Connection createXAConnection(String name, TransactionManager tm) throws JMSException {
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

    private RedeliveryPolicy redeliveryPolicy() {
        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setRedeliveryDelay(1000);
        redeliveryPolicy.setMaximumRedeliveries(1);
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
            actualNum = JMSUtil.getNumMessages(connection, queue);
            
            //System.out.println("Messages in queue " + queue.getQueueName() + ": " + actualNum
            //                   + ", expecting: " + expectedNum);
            Thread.sleep(100);
        } while ((System.currentTimeMillis() - startTime < timeout) && expectedNum != actualNum);
        Assert.assertEquals(message + " -> number of messages on queue", expectedNum, actualNum);
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

    private static final class TestMessageListener implements MessageListener {
        @Override
        public void onMessage(Message message) {
            TextMessage textMessage = (TextMessage)message;
            try {
                String text = textMessage.getText();
                if (OK.equals(text)) {
                    //System.out.println("Simulating Processing successful");
                } else if (FAIL.equals(text)) {
                    throw new RuntimeException("Simulating something went wrong. Expecting rollback");
                } else if (FAILFIRST.equals(text)) {
                    if (message.getJMSRedelivered()) {
                        //System.out.println("Simulating processing worked on second try");
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
