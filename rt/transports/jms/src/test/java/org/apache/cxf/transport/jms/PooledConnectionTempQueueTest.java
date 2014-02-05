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

import java.util.concurrent.Executors;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;

import org.apache.activemq.pool.PooledConnectionFactory;
import org.junit.Test;

public class PooledConnectionTempQueueTest {

    protected static final String SERVICE_QUEUE = "queue1";

    @Test
    public void testTempQueueIssue() throws JMSException, InterruptedException {
        final PooledConnectionFactory cf = new PooledConnectionFactory("vm://localhost?broker.persistent=false");

        Connection con1 = cf.createConnection();
        con1.start();
        //Session session = con1.createSession(false, Session.AUTO_ACKNOWLEDGE);
        
        // This order seems to matter to reproduce the issue
        con1.close();
        
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            public void run() {
                try {
                    receiveAndRespondWithMessageIdAsCorrelationId(cf, SERVICE_QUEUE);
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });

        sendWithReplyToTemp(cf, SERVICE_QUEUE);        
    }

    private void sendWithReplyToTemp(ConnectionFactory cf, String serviceQueue) throws JMSException,
        InterruptedException {
        Connection con = cf.createConnection();
        con.start();
        Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
        TemporaryQueue tempQueue = session.createTemporaryQueue();
        TextMessage msg = session.createTextMessage("Request");
        msg.setJMSReplyTo(tempQueue);
        MessageProducer producer = session.createProducer(session.createQueue(serviceQueue));
        producer.send(msg);

        // This sleep also seems to matter
        Thread.sleep(500);
        
        MessageConsumer consumer = session.createConsumer(tempQueue);
        Message replyMsg = consumer.receive();
        System.out.println(replyMsg.getJMSCorrelationID());
        
        consumer.close();

        producer.close();
        session.close();
        con.close();
    }

    public void receiveAndRespondWithMessageIdAsCorrelationId(ConnectionFactory connectionFactory,
                                                              String queueName) throws JMSException {
        Connection con = connectionFactory.createConnection();
        Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageConsumer consumer = session.createConsumer(session.createQueue(queueName));
        final javax.jms.Message inMessage = consumer.receive();

        String requestMessageId = inMessage.getJMSMessageID();
        System.out.println("Received message " + requestMessageId);
        final TextMessage replyMessage = session.createTextMessage("Result");
        replyMessage.setJMSCorrelationID(inMessage.getJMSMessageID());
        final MessageProducer producer = session.createProducer(inMessage.getJMSReplyTo());
        System.out.println("Sending reply to " + inMessage.getJMSReplyTo());
        producer.send(replyMessage);
        
        producer.close();
        consumer.close();
        session.close();
        con.close();
    }

}
