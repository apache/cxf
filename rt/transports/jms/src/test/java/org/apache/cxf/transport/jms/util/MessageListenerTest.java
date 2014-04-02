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
import javax.jms.ConnectionFactory;
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
import javax.jms.XAConnectionFactory;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.apache.aries.transaction.internal.AriesTransactionManagerImpl;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class MessageListenerTest {

    private static final String OK = "ok";

    @Test
    @Ignore
    public void testWithJTA() throws JMSException, XAException, InterruptedException {
        Connection connection = createConnection();
        Queue dest = createQueue(connection, "test");
        
        MessageListener listenerHandler = new TestMessageListener();
        MessageListenerContainer container = new MessageListenerContainer(connection, dest, listenerHandler);
        container.setTransacted(false);
        TransactionManager transactionManager = new AriesTransactionManagerImpl();
        container.setTransactionManager(transactionManager);
        container.start();
        assertNumMessagesInQueue("At the start the queue should be empty", connection, dest, 0);
        synchronized (listenerHandler) {
            sendMessage(connection, dest, OK);
            listenerHandler.wait();
        }
        Thread.sleep(500);
        assertNumMessagesInQueue("This message should be committed", connection, dest, 0);
        synchronized (listenerHandler) {
            sendMessage(connection, dest, "Fail");
            listenerHandler.wait();
        }
        Thread.sleep(500);
        assertNumMessagesInQueue("First try should do rollback", connection, dest, 1);
        Thread.sleep(500);
        assertNumMessagesInQueue("Second try should work", connection, dest, 0);
        
        container.stop();
        connection.close();
    }
    
    @Test
    public void testNoTransaction() throws JMSException, XAException, InterruptedException {
        ConnectionFactory cf = new ActiveMQConnectionFactory("vm://broker1?broker.persistent=false");
        Connection connection = cf.createConnection();
        connection.start();
        Queue dest = createQueue(connection, "test");
       
        MessageListener listenerHandler = new TestMessageListener();
        MessageListenerContainer container = new MessageListenerContainer(connection, dest, listenerHandler);
        container.setTransacted(false);
        container.setAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
        container.start();
        assertNumMessagesInQueue("At the start the queue should be empty", connection, dest, 0);
        synchronized (listenerHandler) {
            sendMessage(connection, dest, OK);
            listenerHandler.wait();
        }
        Thread.sleep(500);
        assertNumMessagesInQueue("This message should be committed", connection, dest, 0);
        synchronized (listenerHandler) {
            sendMessage(connection, dest, "Fail");
            listenerHandler.wait();
        }
        Thread.sleep(500);
        assertNumMessagesInQueue("Even when an exception occurs the message should be committed", connection, dest, 0);
        container.stop();
        connection.close();
    }

    private Connection createConnection() throws JMSException {
        XAConnectionFactory cf = new ActiveMQXAConnectionFactory("vm://broker2?broker.persistent=false");
        Connection connection = cf.createXAConnection();
        connection.start();
        return connection;
    }

    protected void drainQueue(Connection connection, Queue dest) throws JMSException {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageConsumer consumer = session.createConsumer(dest);
        while (consumer.receiveNoWait() != null) {
            System.out.println("Consuming old message");
        }
        consumer.close();
        session.close();
        assertNumMessagesInQueue("", connection, dest, 0);
    }

    private void assertNumMessagesInQueue(String message, 
                                          Connection connection, 
                                          Queue queue, 
                                          int expectedNum) throws JMSException {
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
        Assert.assertEquals(message + " -> number of messages", expectedNum, actualNum);
    }

    private void sendMessage(Connection connection, Destination dest, String content) throws JMSException {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer prod = session.createProducer(dest);
        Message message = session.createTextMessage(content);
        prod.send(message);
        prod.close();
        session.close();
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
            TextMessage textMessage = (TextMessage) message;
            try {
                String text = textMessage.getText();
                if (MessageListenerTest.OK.equals(text)) {
                    System.out.println("Simulating Processing successful");
                } else {
                    if (message.getJMSRedelivered()) {
                        System.out.println("Simulating processing worked on second try");
                    } else {
                        throw new RuntimeException("Simulating something went wrong. Expecting rollback");
                    }
                }
            } catch (JMSException e) {
                // Ignore
            } finally {
                synchronized (this) {
                    this.notifyAll();
                }

            }
        }
    }
}
