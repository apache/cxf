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

import java.util.concurrent.Executors;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

public class TestReceiver {
    private ConnectionFactory connectionFactory;
    private String receiveQueueName;
    private String requestMessageId;
    private String staticReplyQueue;
    
    public TestReceiver(ConnectionFactory connectionFactory, String receiveQueueName) {
        this.connectionFactory = connectionFactory;
        this.receiveQueueName = receiveQueueName;
        assert this.connectionFactory != null;
        assert this.receiveQueueName != null;
    }
    
    public String getRequestMessageId() {
        return requestMessageId;
    }

    public void setStaticReplyQueue(String staticReplyQueue) {
        this.staticReplyQueue = staticReplyQueue;
    }

    private void drainQueue() {
        ResourceCloser closer = new ResourceCloser();
        try {
            Session session = new SessionFactory(connectionFactory, closer).createSession();
            MessageConsumer consumer = closer.register(session.createConsumer(session.createQueue(receiveQueueName)));
            javax.jms.Message message = null;
            do {
                message = consumer.receive(100);
            } while (message != null);
        } catch (JMSException e) {
            throw JMSUtil.convertJmsException(e);
        } finally {
            closer.close();
        }
    }

    private void receiveAndRespondWithMessageIdAsCorrelationId() {
        ResourceCloser closer = new ResourceCloser();
        try {
            Session session = new SessionFactory(connectionFactory, closer).createSession();
            MessageConsumer consumer = closer.register(session.createConsumer(session
                .createQueue(receiveQueueName)));
            final javax.jms.Message inMessage = consumer.receive();
            requestMessageId = inMessage.getJMSMessageID();
            System.out.println("Received message " + requestMessageId);
            final TextMessage replyMessage = session.createTextMessage("Result");
            replyMessage.setJMSCorrelationID(inMessage.getJMSMessageID());
            Destination replyDest = staticReplyQueue != null 
                ? session.createQueue(staticReplyQueue) : inMessage.getJMSReplyTo();
            if (replyDest != null) {
                final MessageProducer producer = closer
                    .register(session.createProducer(replyDest));
                System.out.println("Sending reply to " + replyDest);
                producer.send(replyMessage);
            }
        } catch (JMSException e) {
            throw JMSUtil.convertJmsException(e);
        } finally {
            closer.close();
        }
    }
    
    public void runAsync() {
        drainQueue();
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            public void run() {
                receiveAndRespondWithMessageIdAsCorrelationId();
            }
        });
    }
}
